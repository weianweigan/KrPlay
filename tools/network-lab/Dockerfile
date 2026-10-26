FROM alpine:3.22
RUN apk add --no-cache python3 iproute2 iptables
WORKDIR /lab
ENTRYPOINT ["python3", "/lab/zeekr_network_lab.py"]
