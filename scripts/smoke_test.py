#!/usr/bin/env python3
import argparse
import base64
import select
import socket
import sys
import time
from typing import Callable, List, Optional, Tuple


class ChatTestClient:
    def __init__(self, host: str, port: int, name: str) -> None:
        self.name = name
        self.sock = socket.create_connection((host, port), timeout=3)
        self.sock.setblocking(False)
        self._buffer = b""

    def send(self, line: str) -> None:
        self.sock.sendall((line + "\n").encode("utf-8"))

    def poll(self, timeout: float = 0.2) -> List[str]:
        messages: List[str] = []
        end_time = time.time() + timeout
        while True:
            remaining = end_time - time.time()
            if remaining <= 0:
                break
            readable, _, _ = select.select([self.sock], [], [], remaining)
            if not readable:
                break
            chunk = self.sock.recv(4096)
            if not chunk:
                break
            self._buffer += chunk
            while b"\n" in self._buffer:
                raw, self._buffer = self._buffer.split(b"\n", 1)
                text = raw.decode("utf-8", errors="replace").rstrip("\r")
                if text:
                    messages.append(text)
        return messages

    def wait_for(
        self, predicate: Callable[[str], bool], timeout: float = 3.0
    ) -> Tuple[Optional[str], List[str]]:
        seen: List[str] = []
        deadline = time.time() + timeout
        while time.time() < deadline:
            chunk = self.poll(0.25)
            seen.extend(chunk)
            for line in chunk:
                if predicate(line):
                    return line, seen
        return None, seen

    def close(self) -> None:
        try:
            self.sock.close()
        except OSError:
            pass


def assert_true(ok: bool, message: str) -> None:
    if not ok:
        print(f"[FAIL] {message}")
        raise AssertionError(message)
    print(f"[PASS] {message}")


def run_smoke(host: str, port: int) -> None:
    alice = ChatTestClient(host, port, "alice_smoke")
    bob = ChatTestClient(host, port, "bob_smoke")
    try:
        alice.send("LOGIN|alice_smoke")
        bob.send("LOGIN|bob_smoke")

        def userlist_has_both(line: str) -> bool:
            return (
                line.startswith("USERLIST|")
                and "alice_smoke" in line
                and "bob_smoke" in line
            )

        _, alice_seen = alice.wait_for(userlist_has_both, timeout=4)
        _, bob_seen = bob.wait_for(userlist_has_both, timeout=4)
        assert_true(
            any(userlist_has_both(x) for x in alice_seen)
            and any(userlist_has_both(x) for x in bob_seen),
            "登录后双方都收到了完整 USERLIST",
        )

        chat_line = "CHAT|alice_smoke|hello-group"
        alice.send(chat_line)
        alice_recv, _ = alice.wait_for(lambda x: x == chat_line, timeout=3)
        bob_recv, _ = bob.wait_for(lambda x: x == chat_line, timeout=3)
        assert_true(
            alice_recv == chat_line and bob_recv == chat_line,
            "群聊消息被广播到发送方和接收方",
        )

        private_line = "PRIVATE|alice_smoke|bob_smoke|hello-private"
        alice.poll(0.2)
        bob.poll(0.2)
        alice.send(private_line)
        bob_private, _ = bob.wait_for(lambda x: x == private_line, timeout=3)
        assert_true(bob_private == private_line, "私聊消息只发到目标用户")
        alice_private = any(x == private_line for x in alice.poll(1.2))
        assert_true(not alice_private, "私聊消息未回发给发送方")

        file_payload = base64.b64encode(b"smoke-file").decode("utf-8")
        file_prefix = "FILE|alice_smoke|smoke.txt|"
        file_line = file_prefix + file_payload
        alice.send(file_line)
        alice_file, _ = alice.wait_for(lambda x: x.startswith(file_prefix), timeout=3)
        bob_file, _ = bob.wait_for(lambda x: x.startswith(file_prefix), timeout=3)
        assert_true(
            alice_file is not None and bob_file is not None,
            "文件消息被广播到所有在线用户",
        )

        print("\nSmoke test completed successfully.")
    finally:
        alice.close()
        bob.close()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="TCPChatRoom protocol smoke test (requires running chat server)."
    )
    parser.add_argument("--host", default="127.0.0.1", help="Server host")
    parser.add_argument("--port", type=int, default=8888, help="Server port")
    args = parser.parse_args()

    try:
        run_smoke(args.host, args.port)
        return 0
    except (OSError, AssertionError) as ex:
        print(f"\nSmoke test failed: {ex}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
