#!/system/bin/sh
# Run after the box's 5 GHz AP is up. Never guess which UP interface is the AP.
set -eu
IFACE=${1:?Usage: krplay-box-net.sh AP_INTERFACE [start|stop]}
ACTION=${2:-start}
BASE=/data/local/tmp/krplay
case "$IFACE" in *[!a-zA-Z0-9_.-]*|'') echo 'Invalid interface' >&2; exit 1;; esac
[ "$(id -u)" = 0 ] || { echo 'Requires box root' >&2; exit 1; }
mkdir -p "$BASE"
PIDFILE="$BASE/dnsmasq.pid"
stop_owned() {
    if [ -f "$PIDFILE" ]; then
        PID=$(cat "$PIDFILE")
        if [ -r "/proc/$PID/cmdline" ] && tr '\000' ' ' < "/proc/$PID/cmdline" | grep -q "$BASE/dnsmasq.conf"; then kill "$PID"; fi
        rm -f "$PIDFILE"
    fi
    iptables -D INPUT -i "$IFACE" -j KRPLAY_IN 2>/dev/null || true
    iptables -F KRPLAY_IN 2>/dev/null || true
    iptables -X KRPLAY_IN 2>/dev/null || true
    if [ -f "$BASE/alias-owned" ]; then ip addr del 100.96.0.1/32 dev "$IFACE" 2>/dev/null || true; rm -f "$BASE/alias-owned"; fi
}
case "$ACTION" in stop) stop_owned; exit 0;; start) ;; *) exit 2;; esac
ip link show "$IFACE" >/dev/null
ip -4 addr show dev "$IFACE" | grep -q '192.168.50.1/24' || { echo 'Configure the AP subnet as 192.168.50.1/24 first; existing addresses are preserved.' >&2; exit 1; }
[ -x /system/bin/dnsmasq ] || { echo 'dnsmasq missing' >&2; exit 1; }
if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then echo 'KrPlay DHCP already running'; exit 0; fi
# Stop ONLY the ROM DHCP instance serving THIS AP before running. Never killall dnsmasq.
if ss -lun 2>/dev/null | grep -q ':67 '; then echo 'UDP 67 is already occupied. Stop the AP DHCP service before taking ownership.' >&2; exit 1; fi
if ! ip -4 addr show dev "$IFACE" | grep -q '100.96.0.1/32'; then ip addr add 100.96.0.1/32 dev "$IFACE"; touch "$BASE/alias-owned"; fi
cat > "$BASE/dnsmasq.conf" <<CONFIG
interface=$IFACE
bind-interfaces
listen-address=192.168.50.1
port=0
pid-file=$PIDFILE
dhcp-leasefile=$BASE/dnsmasq.leases
dhcp-range=192.168.50.10,192.168.50.100,255.255.255.0,12h
dhcp-authoritative
dhcp-option=3
dhcp-option=6
dhcp-option=121,100.96.0.1/32,192.168.50.1,192.168.50.0/24,192.168.50.1
dhcp-option=249,100.96.0.1/32,192.168.50.1,192.168.50.0/24,192.168.50.1
CONFIG
iptables -N KRPLAY_IN 2>/dev/null || true
iptables -F KRPLAY_IN
iptables -A KRPLAY_IN -s 192.168.50.0/24 -p tcp -m multiport --dports 7000,8081,8082,8083,8084 -j ACCEPT
iptables -A KRPLAY_IN -p tcp -m multiport --dports 7000,8081,8082,8083,8084 -j DROP
iptables -C INPUT -i "$IFACE" -j KRPLAY_IN 2>/dev/null || iptables -I INPUT -i "$IFACE" -j KRPLAY_IN
if ! /system/bin/dnsmasq -C "$BASE/dnsmasq.conf"; then stop_owned; exit 1; fi
echo 'KrPlay DHCP active. Reconnect car and iPhone to renew leases; verify their default cellular routes.'
