#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Iterable, Sequence


@dataclass(frozen=True)
class Rect:
    left: int
    top: int
    right: int
    bottom: int

    def tap_point(self) -> tuple[int, int]:
        return ((self.left + self.right) // 2, (self.top + self.bottom) // 2)


@dataclass(frozen=True)
class UiNode:
    text: str
    content_desc: str
    bounds: str

    def rect(self) -> Rect:
        return parse_bounds(self.bounds)


@dataclass(frozen=True)
class CompletedProcessResult:
    stdout: str
    stderr: str
    returncode: int


def parse_bounds(raw_bounds: str) -> Rect:
    try:
        start, end = raw_bounds.split("][")
        left, top = start.lstrip("[").split(",")
        right, bottom = end.rstrip("]").split(",")
    except ValueError as exc:
        raise ValueError(f"Invalid bounds string: {raw_bounds!r}") from exc
    return Rect(left=int(left), top=int(top), right=int(right), bottom=int(bottom))


def parse_nodes(xml_text: str) -> list[UiNode]:
    root = ET.fromstring(xml_text)
    nodes: list[UiNode] = []
    for element in root.iter("node"):
        nodes.append(
            UiNode(
                text=element.attrib.get("text", ""),
                content_desc=element.attrib.get("content-desc", ""),
                bounds=element.attrib["bounds"],
            )
        )
    return nodes


def find_first(
    nodes: Iterable[UiNode],
    *,
    text: str | None = None,
    content_desc: str | None = None,
    text_contains: str | None = None,
) -> UiNode:
    if text is None and content_desc is None and text_contains is None:
        raise ValueError("At least one selector is required")

    for node in nodes:
        if text is not None and node.text != text:
            continue
        if content_desc is not None and node.content_desc != content_desc:
            continue
        if text_contains is not None and text_contains not in node.text:
            continue
        return node

    selector_parts = []
    if text is not None:
        selector_parts.append(f"text={text!r}")
    if content_desc is not None:
        selector_parts.append(f"content_desc={content_desc!r}")
    if text_contains is not None:
        selector_parts.append(f"text_contains={text_contains!r}")
    raise LookupError(f"No UI node matched {', '.join(selector_parts)}")


def adb(
    *args: str,
    serial: str | None = None,
    check: bool = True,
    timeout: float | None = None,
) -> CompletedProcessResult:
    command = ["adb"]
    if serial:
        command.extend(["-s", serial])
    command.extend(args)
    completed = subprocess.run(
        command,
        check=False,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    if check and completed.returncode != 0:
        raise subprocess.CalledProcessError(
            completed.returncode,
            command,
            output=completed.stdout,
            stderr=completed.stderr,
        )
    return CompletedProcessResult(
        stdout=completed.stdout,
        stderr=completed.stderr,
        returncode=completed.returncode,
    )


def dump_nodes(*, serial: str | None = None) -> list[UiNode]:
    dump_path = "/data/local/tmp/vibetap-window-dump.xml"
    adb("shell", "uiautomator", "dump", dump_path, serial=serial)
    dumped = adb("exec-out", "cat", dump_path, serial=serial)
    xml_text = dumped.stdout.strip()
    xml_start = xml_text.find("<")
    if xml_start == -1:
        raise RuntimeError("uiautomator dump did not produce XML output")
    return parse_nodes(xml_text[xml_start:])


def wait_for_node(
    *,
    serial: str | None = None,
    timeout_seconds: float = 10.0,
    interval_seconds: float = 0.5,
    text: str | None = None,
    content_desc: str | None = None,
    text_contains: str | None = None,
) -> UiNode:
    deadline = time.monotonic() + timeout_seconds
    last_error: Exception | None = None

    while time.monotonic() < deadline:
        try:
            return find_first(
                dump_nodes(serial=serial),
                text=text,
                content_desc=content_desc,
                text_contains=text_contains,
            )
        except LookupError as exc:
            last_error = exc
            time.sleep(interval_seconds)

    if last_error is None:
        raise TimeoutError("Timed out before the first UI hierarchy check")
    raise TimeoutError(str(last_error)) from last_error


def tap_node(node: UiNode, *, serial: str | None = None) -> tuple[int, int]:
    x, y = node.rect().tap_point()
    adb("shell", "input", "tap", str(x), str(y), serial=serial)
    return x, y


def _escape_input_text(value: str) -> str:
    return "".join("%s" if character == " " else character for character in value)


def set_text(node: UiNode, value: str, *, serial: str | None = None) -> str:
    tap_node(node, serial=serial)
    escaped_value = _escape_input_text(value)
    adb("shell", "input", "text", escaped_value, serial=serial)
    return escaped_value


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="ADB-backed UIAutomator helper for the VibeTap proof flow.")
    parser.add_argument("--serial", help="ADB device serial to target")

    subparsers = parser.add_subparsers(dest="command", required=True)

    tap_text = subparsers.add_parser("tap-text", help="Tap the first visible node matching exact text")
    tap_text.add_argument("text")

    tap_desc = subparsers.add_parser("tap-desc", help="Tap the first visible node matching exact content description")
    tap_desc.add_argument("content_desc")

    wait_text = subparsers.add_parser("wait-text", help="Wait for a node with exact text to appear")
    wait_text.add_argument("text")
    _add_wait_args(wait_text)

    wait_text_contains = subparsers.add_parser("wait-text-contains", help="Wait for a node whose text contains the given substring")
    wait_text_contains.add_argument("text_contains")
    _add_wait_args(wait_text_contains)

    wait_desc = subparsers.add_parser("wait-desc", help="Wait for a node with exact content description to appear")
    wait_desc.add_argument("content_desc")
    _add_wait_args(wait_desc)

    set_text_desc = subparsers.add_parser("set-text-desc", help="Tap a node by content description and type text into it")
    set_text_desc.add_argument("content_desc")
    set_text_desc.add_argument("value")

    return parser


def _add_wait_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--timeout", type=float, default=10.0, help="Maximum seconds to wait")
    parser.add_argument("--interval", type=float, default=0.5, help="Polling interval in seconds")


def main(argv: Sequence[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)

    try:
        if args.command == "tap-text":
            node = wait_for_node(serial=args.serial, text=args.text)
            x, y = tap_node(node, serial=args.serial)
            print(f"Tapped text {args.text!r} at ({x}, {y})")
            return 0

        if args.command == "tap-desc":
            node = wait_for_node(serial=args.serial, content_desc=args.content_desc)
            x, y = tap_node(node, serial=args.serial)
            print(f"Tapped content description {args.content_desc!r} at ({x}, {y})")
            return 0

        if args.command == "wait-text":
            node = wait_for_node(
                serial=args.serial,
                timeout_seconds=args.timeout,
                interval_seconds=args.interval,
                text=args.text,
            )
            print(node.bounds)
            return 0

        if args.command == "wait-text-contains":
            node = wait_for_node(
                serial=args.serial,
                timeout_seconds=args.timeout,
                interval_seconds=args.interval,
                text_contains=args.text_contains,
            )
            print(node.bounds)
            return 0

        if args.command == "wait-desc":
            node = wait_for_node(
                serial=args.serial,
                timeout_seconds=args.timeout,
                interval_seconds=args.interval,
                content_desc=args.content_desc,
            )
            print(node.bounds)
            return 0

        if args.command == "set-text-desc":
            node = wait_for_node(serial=args.serial, content_desc=args.content_desc)
            escaped_value = set_text(node, args.value, serial=args.serial)
            print(f"Set text via content description {args.content_desc!r}: {escaped_value}")
            return 0
    except (LookupError, RuntimeError, TimeoutError, ValueError, subprocess.CalledProcessError) as exc:
        print(str(exc), file=sys.stderr)
        return 1

    parser.error(f"Unsupported command: {args.command}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
