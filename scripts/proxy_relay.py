#!/usr/bin/env python3
"""
Local proxy relay for Claude Code Web sandbox.

The Claude Code Web sandbox routes all outbound traffic through an
authenticated proxy. Many tools (Java's HttpClient, .NET's HttpClient,
etc.) don't properly extract credentials from the HTTPS_PROXY URL to
send the required Proxy-Authorization header, resulting in HTTP 407
errors.

This script runs a lightweight local proxy on 127.0.0.1 that:
  1. Accepts connections with NO authentication required
  2. Injects the Proxy-Authorization header from the upstream proxy URL
  3. Forwards the request to the real sandbox proxy

Usage:
    # Reads HTTPS_PROXY env var for upstream credentials
    LOCAL_PROXY_PORT=18080 python3 scripts/proxy_relay.py

See also: https://github.com/anthropics/claude-code/issues/11897
"""

import base64
import os
import socket
import threading
from urllib.parse import urlparse

upstream_url = os.environ.get("HTTPS_PROXY", "")
parsed = urlparse(upstream_url)

UPSTREAM_HOST = parsed.hostname
UPSTREAM_PORT = parsed.port or 8080
PROXY_USER = parsed.username or ""
PROXY_PASS = parsed.password or ""

auth_b64 = base64.b64encode(f"{PROXY_USER}:{PROXY_PASS}".encode()).decode()
LOCAL_PORT = int(os.environ.get("LOCAL_PROXY_PORT", 18080))


def handle_client(client_socket):
    try:
        request = client_socket.recv(65536)
        if not request:
            client_socket.close()
            return

        upstream_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        upstream_socket.connect((UPSTREAM_HOST, UPSTREAM_PORT))

        lines = request.decode("utf-8", errors="ignore").split("\r\n")
        new_lines = [lines[0], f"Proxy-Authorization: Basic {auth_b64}"] + lines[1:]
        upstream_socket.send("\r\n".join(new_lines).encode())

        def forward(src, dst):
            try:
                while True:
                    data = src.recv(65536)
                    if not data:
                        break
                    dst.send(data)
            except Exception:
                pass
            finally:
                src.close()
                dst.close()

        t1 = threading.Thread(
            target=forward, args=(upstream_socket, client_socket), daemon=True
        )
        t2 = threading.Thread(
            target=forward, args=(client_socket, upstream_socket), daemon=True
        )
        t1.start()
        t2.start()
        t1.join()
        t2.join()
    except Exception as e:
        print(f"Error: {e}")
        client_socket.close()


def main():
    if not UPSTREAM_HOST:
        print("No HTTPS_PROXY set — proxy relay not needed outside sandbox.")
        raise SystemExit(0)

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("127.0.0.1", LOCAL_PORT))
    server.listen(100)
    print(f"Proxy relay listening on 127.0.0.1:{LOCAL_PORT} -> {UPSTREAM_HOST}:{UPSTREAM_PORT}")

    while True:
        client_socket, _ = server.accept()
        threading.Thread(target=handle_client, args=(client_socket,), daemon=True).start()


if __name__ == "__main__":
    main()
