# SA8295 Android VM streaming recovery

The supplied measurements show intermittent stalls and recovery, not proof that VPU
preemption, Wi-Fi scans, or IDR size caused each stall. The application changes below
reduce amplification while retaining enough counters to test that hypothesis.

## Implemented behavior

- Client defaults to 720p for new preferences; existing explicit choices remain.
  Receiver HEVC preference already defaults to enabled. The upstream iPhone still
  chooses the actual negotiated stream; the client cannot turn AVC bytes into HEVC.
- Decoder requests realtime priority, operating rate 120 and standard low latency.
  Qualcomm decoders also receive the existing low-latency vendor hint. The proposed
  picture-order key is used only if advertised through Android 12's vendor parameter
  API. Unsupported tuning falls back to baseline configuration, shown in diagnostics.
  Neither the priority nor operating-rate hint guarantees VPU frequency or QNX scheduling.
- Input wait >=60 ms is counted separately. Input retries drain output and retain the
  pending access unit. Rebuild requires >=750 ms without output progress and at least
  eight failed polls. Surface/connection changes are handled separately. This bounds
  genuine hangs without classifying every 20–40 ms scheduling bubble as a failure.
- Client has a 12-entry FIFO ring with a bounded 14-packet pool. Wire frames retain
  their existing access-unit boundaries. Full queues first discard only positively
  identified non-reference AVC AUs (all VCL NALs must be non-reference type 1).
  HEVC and ambiguous AVC are retained. A 250 ms enqueue grace absorbs short bursts.
  If that fails, the reference chain is abandoned explicitly and a keyframe is required;
  the existing decoder is retained until a real failure/configuration change.
- Catch-up feeds reference frames to the decoder and releases obsolete decoded output
  without presentation. The final available output is presented immediately once the
  queued video backlog is consumed. SurfaceView/direct MediaCodec Surface binding was
  already in use; this does not promise a physical HWC overlay or zero display latency.
- Client IDR retries remain <=1/s, extended to <=1/2s when latest control RTT >100 ms.
  Retry is not disabled permanently when congested or when the source is static.
- Server uses nonblocking SocketChannel writes, 16 KiB chunks and selector readiness.
  A partially written frame always finishes, or the connection is closed after persistent
  lack of progress. No frame is spliced out of an active TCP byte stream. The existing
  two-second watchdog now tracks last write progress rather than total frame duration.
- Server application queue is bounded (12 entries / 16 preallocated packets), and queued
  video older than 250 ms is discarded as a dependent group before writing. New video
  waits for a fresh keyframe. A proven non-reference AVC AU can be shed without that
  recovery. 15 ms / two-frame recovery was deliberately avoided: it would recreate
  recovery storms during ordinary 60 fps scheduling fluctuations.
- All bridge/client/source keyframe requests pass through the server congestion gate.
  They are merged while a frame is being written or the application queue is nonempty,
  retried after drain, and suppressed for 500 ms after an IDR is sent. A one-second
  upstream request cooldown remains. A successful keyframe satisfies pending requests.
- Video sockets keep TCP_NODELAY and 512 KiB send/receive buffer requests. Android/Linux
  additionally attempts TCP_NOTSENT_LOWAT=16 KiB using a duplicated descriptor; failure
  is logged and tolerated. CS4 is requested, not assumed to be honored by the AP or host.
  Readiness-driven chunking provides backpressure; it is not a bandwidth-paced shaper.
- Client audio network reads and AudioTrack playback use separate threads. A bounded
  PCM mailbox keeps at most approximately 120 ms of queued audio, preserving stop/mic
  control messages. AudioTrack writes are nonblocking; expired PCM is flushed with a
  short ramp down/up. Audio errors close only the audio socket. Server audio produces
  <=20 ms PCM chunks and evicts old PCM rather than disconnecting on PCM overflow.
  Age is local queue age: the legacy audio protocol carries no capture timestamp, so
  absolute age of PCM already in flight cannot be measured by this change.
- The owned Android virtual-display AVC encoder uses supported CBR, resolution-scaled
  4/8 Mbps targets clamped to capabilities, baseline where supported, and no B frames
  on API 29+. This is distinct from iPhone CarPlay encoding.

## Not available in this repository

The CarPlay receiver forwards compressed iPhone video; it does not own a VideoToolbox
session. VT DataRateLimits, IDR QP caps, GDR, slice encoding and iPhone B-frame settings
cannot be implemented here. No arbitrary byte-level NAL splitting or fake encoder
settings are substituted. TCP_NOTSENT_LOWAT limits unsent bytes, not all bytes in flight,
and cannot retract data already accepted into TCP. Buffer-full on a blocking socket
is not itself proof of the cause of a SocketException.

## Verification and device acceptance

Automated coverage includes normal and timed HEVC framing, full-size frames, temporary
receiver pauses, bounded reference-preserving client backpressure, exhausted queues,
100 coalesced IDR requests during a partial large-frame write, recovery frame boundaries,
static-source retry, PCM eviction/control preservation, and audio/microphone roundtrips.
Android compilation and unit tests cannot validate Qualcomm VPU, actual overlay use,
DSCP/WMM, QNX scheduling or the Linux socket option on the target kernel.

On the 8295, compare equal-duration dynamic scenes at actual 720p HEVC and 1080p AVC:

1. Record baseline and end values for input waits, sustained stalls, decoder rebuilds,
   overflow, non-reference drops, catch-up outputs, reconnects and keyframe requests.
2. Record server queue depth, staleFrames, lastWriteMs, maxFrameBytes, sendFailures,
   lastSendFailure and slowDisconnects. Socket option availability is in KrPlay-Video logcat.
3. Measure control RTT and video latency P50/P95/P99 alongside a timeline of visible
   freezes. Use the same sampling duration; dropped outputs do not enter render P95,
   so P95 alone cannot prove an improvement. Do not run throughput saturation during it.
4. Exercise temporary 100/300/600 ms receiver pauses and repeated resolution changes;
   verify resumed reference correctness, final static frame, audio recovery and controls.
5. Treat 5 ms transfer, 8–12 ms decoder output, <=5 requests and P95 <=250 ms as device
   experiment targets, not software guarantees. A 600 ms host/network pause cannot be
   eliminated by an application, and render callbacks are not physical display times.

References:
- https://developer.android.com/reference/android/media/MediaFormat
- https://developer.android.com/reference/android/media/MediaCodec
- https://source.android.com/docs/core/media/low-latency-media
- https://www.kernel.org/doc/html/next/networking/ip-sysctl.html#tcp-notsent-lowat-unsigned-integer

Local verification (2026-10-27): client 51 unit tests passed; server ClientAudioRouteTest
2 tests passed; protocol/HTTP/TCP integration suite passed, including a 600 ms receiver
pause and 100 requests during a partially sent large frame. Both debug APKs built.
No SA8295/QNX hardware performance run or installation was performed.

Reproduce with the repository-local JDK/SDK (`eval "$(scripts/android.sh env)"`):

```sh
cd krplay-client
./gradlew --no-daemon -Dorg.gradle.java.home="$JAVA_HOME" testDebugUnitTest assembleDebug
```

From `krplay/`, use `:common:testDebugUnitTest --tests '*ClientAudioRouteTest'
:mobile:assembleDebug`. The independent TCP suite is `scripts/test.py`, with `--jdk`
pointing at the same JDK and `--kotlin-lib-dir` at the Gradle distribution's `lib/`.
