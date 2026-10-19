#!/system/bin/sh
# Only modifies box-local traffic. Never run on the car or install VpnService there.
set -eu
BASE=/data/local/tmp/krplay
ACTION=${1:-start}
[ "$(id -u)" = 0 ] || exit 1
cleanup() {
    iptables -t nat -D OUTPUT -p udp --dport 53 -j REDIRECT --to-ports 15353 2>/dev/null || true
    for PREF in 18080 18081 18082 18083 18084 18085; do ip rule del pref "$PREF" 2>/dev/null || true; done
    ip route flush table 18080 2>/dev/null || true
    if [ -f "$BASE/tunnel.pid" ]; then
        PID=$(cat "$BASE/tunnel.pid")
        if [ -r "/proc/$PID/cmdline" ] && tr '\000' ' ' < "/proc/$PID/cmdline" | grep -q "$BASE/hev-socks5-tunnel"; then kill "$PID" 2>/dev/null || true; fi
        rm -f "$BASE/tunnel.pid"
    fi
}
case "$ACTION" in stop) cleanup; exit 0;; start) ;; *) exit 2;; esac
[ -x "$BASE/hev-socks5-tunnel" ] || { echo 'Install a verified Android ARM hev-socks5-tunnel binary' >&2; exit 1; }
# Must have a running hub + DNS relay; prevents installing a blackhole route first.
ss -ltn | grep -q ':1080 ' || { echo 'Start KrPlay first' >&2; exit 1; }
ss -lun | grep -q ':15353 ' || { echo 'KrPlay DNS relay is not ready' >&2; exit 1; }
# Reserved rule/table ownership: refuse to overwrite unrelated policy.
if ip rule show | grep -Eq '^1808[0-5]:'; then echo 'Rule priorities 18080–18085 occupied; stop the old KrPlay tunnel first' >&2; exit 1; fi
if [ -n "$(ip route show table 18080 2>/dev/null)" ]; then echo 'Route table 18080 occupied' >&2; exit 1; fi
trap cleanup EXIT INT TERM
"$BASE/hev-socks5-tunnel" "$BASE/hev-socks5-tunnel.yml" > "$BASE/tunnel.log" 2>&1 &
echo $! > "$BASE/tunnel.pid"
COUNT=0
until ip link show krplay0 >/dev/null 2>&1; do COUNT=$((COUNT+1)); [ "$COUNT" -lt 30 ] || exit 1; sleep 1; done
ip link set krplay0 up
# Android's per-UID main-table rules are often near 19000. Override only on the box.
ip rule add pref 18080 to 192.168.50.0/24 lookup main
ip rule add pref 18081 to 100.96.0.1/32 lookup main
ip rule add pref 18082 to 77.17.3.0/24 lookup main
ip rule add pref 18083 to 127.0.0.0/8 lookup main
ip rule add pref 18084 to 224.0.0.0/4 lookup main
ip route add default dev krplay0 table 18080
ip rule add pref 18085 lookup 18080
iptables -t nat -A OUTPUT -p udp --dport 53 -j REDIRECT --to-ports 15353
# Supervise in foreground; EXIT always restores owned routing rules.
wait "$(cat "$BASE/tunnel.pid")"
