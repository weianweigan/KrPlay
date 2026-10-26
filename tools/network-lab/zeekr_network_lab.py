#!/usr/bin/env python3
"""Isolated Linux NAT/firewall experiments. Never configure the host network namespace."""
import argparse
import json
import os
import socket
import socketserver
import statistics
import struct
import subprocess
import sys
import time
import threading
from pathlib import Path


def exact(stream, size):
    data = bytearray()
    while len(data) < size:
        part = stream.recv(size - len(data))
        if not part:
            raise EOFError("Incomplete test payload")
        data.extend(part)
    return bytes(data)


class Echo(socketserver.BaseRequestHandler):
    def handle(self):
        sock = self.request
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 512 * 1024)
        try:
            while True:
                opcode = sock.recv(1)
                if opcode == b"P":
                    sock.sendall(exact(sock, 8))
                elif opcode == b"F":
                    size = struct.unpack("!I", exact(sock, 4))[0]
                    if size > 2 * 1024 * 1024:
                        return
                    sock.sendall(bytes(size))
                elif opcode == b"T":
                    prefix, size = struct.unpack("!II", exact(sock, 8))
                    if max(prefix, size) > 2 * 1024 * 1024:
                        return
                    sock.sendall(bytes(prefix))
                    # Same Linux monotonic clock across these containers. Like
                    # KrPlay's timestamp: before writing the measured frame.
                    started = time.monotonic_ns()
                    sock.sendall(struct.pack("!QI", started, size) + bytes(size))
                else:
                    return
        except (OSError, EOFError):
            pass


def probe(host, samples=20, frame=True):
    result = {"target": host, "port": 8084}
    try:
        with socket.create_connection((host, 8084), timeout=1.5) as sock:
            sock.settimeout(5)
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 512 * 1024)
            rtts = []
            for i in range(samples):
                start = time.monotonic_ns()
                payload = struct.pack("!Q", i)
                sock.sendall(b"P" + payload)
                if exact(sock, 8) != payload:
                    raise ValueError("Echo mismatch")
                rtts.append((time.monotonic_ns() - start) / 1e6)
            rtts.sort()
            result.update(connected=True, rtt_median_ms=statistics.median(rtts),
                          rtt_p95_ms=rtts[max(0, int(len(rtts) * .95 + .999) - 1)])
            if frame:
                size = 1024 * 1024
                start = time.monotonic_ns()
                sock.sendall(b"F" + struct.pack("!I", size))
                exact(sock, size)
                result.update(frame_bytes=size, frame_ms=(time.monotonic_ns() - start) / 1e6)
    except (OSError, EOFError, ValueError) as error:
        result.update(connected=False, error=type(error).__name__ + ": " + str(error))
    return result


def latency_probe(host, size, prefix=0, pause_ms=0, repeats=5):
    frames, busy_rtts = [], []
    stop = threading.Event()
    control_errors = []

    def control():
        try:
            with socket.create_connection((host, 8084), timeout=10) as sock:
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                while not stop.is_set():
                    start = time.monotonic_ns()
                    sock.sendall(b"P" + struct.pack("!Q", start))
                    if exact(sock, 8) != struct.pack("!Q", start):
                        raise ValueError("Control echo mismatch")
                    busy_rtts.append((time.monotonic_ns() - start) / 1e6)
                    stop.wait(.02)
        except (OSError, EOFError, ValueError) as error:
            control_errors.append(str(error))

    idle = probe(host, frame=False)
    worker = threading.Thread(target=control)
    worker.start()
    try:
        with socket.create_connection((host, 8084), timeout=10) as sock:
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 512 * 1024)
            for _ in range(repeats):
                sock.sendall(b"T" + struct.pack("!II", prefix, size))
                if pause_ms:
                    time.sleep(pause_ms / 1000)
                exact(sock, prefix)
                started, received_size = struct.unpack("!QI", exact(sock, 12))
                exact(sock, received_size)
                frames.append((time.monotonic_ns() - started) / 1e6)
                time.sleep(.05)
    finally:
        stop.set()
        worker.join(timeout=12)
    if worker.is_alive() or control_errors or not busy_rtts:
        raise RuntimeError("Concurrent control probe failed: " + repr(control_errors))

    def stats(values):
        values = sorted(values)
        return {"median_ms": statistics.median(values),
                "p95_ms": values[max(0, int(len(values) * .95 + .999) - 1)],
                "samples_ms": values}
    return {"frame_bytes": size, "preceding_bytes": prefix,
            "reader_pause_ms": pause_ms, "transfer": stats(frames),
            "idle_rtt": idle, "concurrent_control_rtt": stats(busy_rtts)}


def latency_experiments(ns, results):
    cases = [
        ("nat_only_1MiB", None, 1048576, 0, 0),
        ("nat_only_reader_pause", None, 262144, 0, 650),
        ("48mbit_1MiB", "48mbit", 1048576, 0, 0),
        ("24mbit_64KiB", "24mbit", 65536, 0, 0),
        ("24mbit_256KiB", "24mbit", 262144, 0, 0),
        ("24mbit_1MiB", "24mbit", 1048576, 0, 0),
        ("12mbit_1MiB", "12mbit", 1048576, 0, 0),
        ("24mbit_backlog_1MiB", "24mbit", 1048576, 1048576, 0),
    ]
    for name, rate, size, prefix, pause in cases:
        if rate:
            ns("host", "tc", "qdisc", "replace", "dev", "eth0", "root", "tbf",
               "rate", rate, "burst", "128kb", "latency", "2000ms")
        result = json.loads(ns("vm", "python3", "/lab/zeekr_network_lab.py", "latency-probe",
                               "192.168.15.38", "--size", str(size), "--prefix", str(prefix),
                               "--pause-ms", str(pause)))
        result.update(scenario=name, downlink_rate=rate or "unlimited",
                      qdisc=ns("host", "tc", "-s", "qdisc", "show", "dev", "eth0"))
        results.append(result)
        print(json.dumps(result), flush=True)


def run_lab(output, latency=False):
    # Each node is its own restricted Docker network namespace. Forwarding is set
    # at container creation; /proc/sys stays read-only and SYS_ADMIN is not needed.
    prefix = "krplaylab-" + str(os.getpid())
    nodes = {name: prefix + "-" + name for name in ("vm", "host", "router", "receiver")}
    networks = {name: prefix + "-" + name for name in ("front", "middle", "rear")}
    created, containers, results = [], [], []

    def command(*args):
        result = subprocess.run(args, text=True, capture_output=True)
        if result.returncode:
            raise RuntimeError(" ".join(args) + ": " + result.stderr.strip())
        return result.stdout

    def ns(name, *args):
        return command("docker", "exec", nodes[name], *args)

    def container(name, network, address, forwarding=False):
        args = ["docker", "run", "-d", "--name", nodes[name],
                "--network", networks[network], "--ip", address,
                "--cap-drop", "ALL", "--cap-add", "NET_ADMIN",
                "--security-opt", "no-new-privileges",
                "--mount", "type=bind,src=" + str(Path(__file__).resolve().parent) + ",dst=/lab,readonly",
                "--entrypoint", "python3"]
        if forwarding:
            args += ["--sysctl", "net.ipv4.ip_forward=1"]
        args += ["krplay-network-lab:local"]
        args += ["/lab/zeekr_network_lab.py", "serve"] if name in ("vm", "receiver") else ["-c", "import time; time.sleep(86400)"]
        command(*args)
        containers.append(nodes[name])

    def scenario(name, origin, target, expected):
        result = json.loads(ns(origin, "python3", "/lab/zeekr_network_lab.py", "probe", target))
        result.update(scenario=name, origin=origin, expected_connected=expected)
        results.append(result)
        print(json.dumps(result), flush=True)
        if result["connected"] != expected:
            raise AssertionError("Unexpected reachability: " + name)

    try:
        for name, subnet, gateway in (("front", "77.17.3.0/24", "77.17.3.254"),
                                      ("middle", "172.30.0.0/24", "172.30.0.254"),
                                      ("rear", "192.168.15.0/24", "192.168.15.254")):
            command("docker", "network", "create", "--internal", "--subnet", subnet,
                    "--gateway", gateway, networks[name])
            created.append(networks[name])
        container("vm", "front", "77.17.3.10")
        container("host", "front", "77.17.3.1", forwarding=True)
        command("docker", "network", "connect", "--ip", "172.30.0.2", networks["middle"], nodes["host"])
        container("router", "middle", "172.30.0.1", forwarding=True)
        command("docker", "network", "connect", "--ip", "192.168.15.1", networks["rear"], nodes["router"])
        container("receiver", "rear", "192.168.15.38")
        for node, gateway in (("vm", "77.17.3.1"), ("host", "172.30.0.1"), ("receiver", "192.168.15.1")):
            ns(node, "ip", "route", "replace", "default", "via", gateway)
        ns("router", "ip", "route", "add", "77.17.3.0/24", "via", "172.30.0.2")
        ns("receiver", "ip", "addr", "add", "6.6.6.6/32", "dev", "lo")
        for node, source in (("host", "77.17.3.0/24"), ("router", "172.30.0.0/24")):
            ns(node, "iptables", "-t", "nat", "-A", "POSTROUTING", "-s", source, "-o", "eth1", "-j", "MASQUERADE")
            ns(node, "iptables", "-P", "FORWARD", "DROP")
            ns(node, "iptables", "-A", "FORWARD", "-i", "eth0", "-o", "eth1", "-j", "ACCEPT")
            ns(node, "iptables", "-A", "FORWARD", "-i", "eth1", "-o", "eth0", "-m", "conntrack",
               "--ctstate", "ESTABLISHED,RELATED", "-j", "ACCEPT")
        for node, address in (("receiver", "192.168.15.38"), ("vm", "77.17.3.10")):
            for attempt in range(30):
                if json.loads(ns(node, "python3", "/lab/zeekr_network_lab.py", "probe", address))["connected"]:
                    break
                time.sleep(.05)
            else:
                raise RuntimeError("Probe server did not start: " + node)

        if latency:
            latency_experiments(ns, results)
            return

        scenario("double_nat_outbound", "vm", "192.168.15.38", True)
        scenario("receiver_cannot_initiate_into_vm", "receiver", "77.17.3.10", False)
        ns("host", "iptables", "-I", "FORWARD", "1", "-d", "192.168.15.0/24", "-j", "DROP")
        scenario("host_blocks_hotspot_subnet", "vm", "192.168.15.38", False)
        scenario("alias_without_host_path", "vm", "6.6.6.6", False)
        ns("router", "ip", "route", "add", "6.6.6.6/32", "via", "192.168.15.38")
        scenario("alias_with_explicit_route", "vm", "6.6.6.6", True)
        ns("host", "iptables", "-I", "FORWARD", "1", "-i", "eth0", "-j", "DROP")
        scenario("host_blocks_all_vm_forwarding", "vm", "6.6.6.6", False)
        ns("host", "iptables", "-D", "FORWARD", "-i", "eth0", "-j", "DROP")
        ns("host", "iptables", "-D", "FORWARD", "-d", "192.168.15.0/24", "-j", "DROP")
        # Explicit impairment, not an estimate of Zeekr's real latency or bandwidth.
        for interface in ("eth0", "eth1"):
            ns("host", "tc", "qdisc", "add", "dev", interface, "root", "netem",
               "delay", "50ms", "10ms", "rate", "12mbit", "limit", "1000")
        scenario("injected_50ms_each_way_12mbit", "vm", "6.6.6.6", True)
    finally:
        for name in reversed(containers):
            subprocess.run(["docker", "rm", "-f", name], capture_output=True)
        for name in reversed(created):
            subprocess.run(["docker", "network", "rm", name], capture_output=True)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps({"model": "hypothetical multi-NAT VM; not a Zeekr firmware emulator",
                                     "results": results}, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("serve", "probe", "run", "latency", "latency-probe"))
    parser.add_argument("target", nargs="?")
    parser.add_argument("--output", type=Path, default=Path("logs/network-lab-20261020/results.json"))
    parser.add_argument("--size", type=int, default=1048576)
    parser.add_argument("--prefix", type=int, default=0)
    parser.add_argument("--pause-ms", type=int, default=0)
    args = parser.parse_args()
    if args.mode == "serve":
        socketserver.ThreadingTCPServer.allow_reuse_address = True
        with socketserver.ThreadingTCPServer(("0.0.0.0", 8084), Echo) as server:
            server.serve_forever()
    elif args.mode == "probe":
        print(json.dumps(probe(args.target)))
    elif args.mode == "latency-probe":
        print(json.dumps(latency_probe(args.target, args.size, args.prefix, args.pause_ms)))
    else:
        run_lab(args.output, latency=args.mode == "latency")


if __name__ == "__main__":
    main()
