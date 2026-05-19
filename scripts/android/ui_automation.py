#!/usr/bin/env python3
from __future__ import annotations

import argparse
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
import re
from typing import Iterable, Sequence


RETRYABLE_UI_ERRORS = (
    LookupError,
    RuntimeError,
    ValueError,
    ET.ParseError,
    subprocess.CalledProcessError,
    subprocess.TimeoutExpired,
    OSError,
)


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


def dump_xml_text(*, serial: str | None = None) -> str:
    dump_path = "/data/local/tmp/vibetap-window-dump.xml"
    adb("shell", "uiautomator", "dump", dump_path, serial=serial, timeout=10.0)
    dumped = adb("exec-out", "cat", dump_path, serial=serial, timeout=10.0)
    xml_text = dumped.stdout.strip()
    xml_start = xml_text.find("<")
    if xml_start == -1:
        raise RuntimeError("uiautomator dump did not produce XML output")
    return xml_text[xml_start:]


def dump_nodes(*, serial: str | None = None) -> list[UiNode]:
    return parse_nodes(dump_xml_text(serial=serial))


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
        except RETRYABLE_UI_ERRORS as exc:
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
    """Translate spaces for `adb shell input text` and reject unsupported literal `%`.

    This helper intentionally supports plain text plus spaces only. Spaces are encoded
    as `%s`, which matches adb's shell input contract. Literal percent characters are
    rejected so callers do not accidentally rely on ambiguous `%` handling.
    """
    if "%" in value:
        raise ValueError("Literal '%' is unsupported by set_text(); spaces are encoded as %s for adb input text")
    return "".join("%s" if character == " " else character for character in value)


def set_text(node: UiNode, value: str, *, serial: str | None = None) -> str:
    escaped_value = _escape_input_text(value)
    tap_node(node, serial=serial)
    adb("shell", "input", "text", escaped_value, serial=serial)
    return escaped_value


def get_text_by_content_desc(content_desc: str, *, serial: str | None = None) -> str:
    root = ET.fromstring(dump_xml_text(serial=serial))
    parents = {child: parent for parent in root.iter() for child in parent}

    for element in root.iter("node"):
        if element.attrib.get("content-desc", "") != content_desc:
            continue
        text = element.attrib.get("text", "")
        if text:
            return text
        parent = parents.get(element)
        if parent is not None:
            parent_text = parent.attrib.get("text", "")
            if parent_text:
                return parent_text
        return ""

    raise LookupError(f"No UI node matched content_desc={content_desc!r}")


def screen_bounds(nodes: Sequence[UiNode]) -> Rect:
    if not nodes:
        raise RuntimeError("UI hierarchy did not contain any nodes")
    return nodes[0].rect()


def device_screen_bounds(*, serial: str | None = None) -> Rect:
    wm_size = adb("shell", "wm", "size", serial=serial)
    override_match = re.search(r"Override size:\s*(\d+)x(\d+)", wm_size.stdout)
    physical_match = re.search(r"Physical size:\s*(\d+)x(\d+)", wm_size.stdout)
    match = override_match or physical_match
    if not match:
        raise RuntimeError(f"Could not parse device screen size from: {wm_size.stdout!r}")
    width, height = match.groups()
    return Rect(left=0, top=0, right=int(width), bottom=int(height))


def _rect_point(bounds: Rect, x_fraction: float, y_fraction: float) -> tuple[int, int]:
    width = bounds.right - bounds.left
    height = bounds.bottom - bounds.top
    return (
        bounds.left + round(width * x_fraction),
        bounds.top + round(height * y_fraction),
    )


def _keyboard_letter_point(letter: str, bounds: Rect) -> tuple[int, int]:
    rows: tuple[tuple[str, float, float, float], ...] = (
        ("QWERTYUIOP", 0.083, 0.917, 0.812),
        ("ASDFGHJKL", 0.092, 0.908, 0.867),
        ("ZXCVBNM", 0.123, 0.877, 0.922),
    )
    for letters, start_x, end_x, y_fraction in rows:
        if letter in letters:
            if len(letters) == 1:
                x_fraction = start_x
            else:
                step = (end_x - start_x) / (len(letters) - 1)
                x_fraction = start_x + step * letters.index(letter)
            return _rect_point(bounds, x_fraction, y_fraction)
    raise ValueError(f"Unsupported VibeTap keyboard letter: {letter!r}")


def vibetap_desc_tap_point(content_desc: str, bounds: Rect) -> tuple[int, int]:
    direct_points = {
        "Keyboard mic key": (0.167, 0.734),
        "Keyboard actions key": (0.500, 0.734),
        "Keyboard float key": (0.500, 0.734),
        "Keyboard backspace key": (0.833, 0.734),
        "Keyboard space key": (0.442, 0.972),
        "Keyboard enter key": (0.859, 0.972),
    }
    if content_desc in direct_points:
        x_fraction, y_fraction = direct_points[content_desc]
        return _rect_point(bounds, x_fraction, y_fraction)

    match = re.fullmatch(r"Keyboard letter ([A-Z]) key", content_desc)
    if match:
        return _keyboard_letter_point(match.group(1), bounds)

    raise ValueError(f"Unsupported VibeTap content description fallback: {content_desc!r}")


def _settings_keyboard_letter_point(letter: str, bounds: Rect) -> tuple[int, int]:
    rows: tuple[tuple[str, float, float, float], ...] = (
        ("QWERTYUIOP", 0.082, 0.916, 0.781),
        ("ASDFGHJKL", 0.080, 0.916, 0.838),
        ("ZXCVBNM", 0.076, 0.920, 0.895),
    )
    for letters, start_x, end_x, y_fraction in rows:
        if letter in letters:
            step = (end_x - start_x) / (len(letters) - 1)
            x_fraction = start_x + step * letters.index(letter)
            return _rect_point(bounds, x_fraction, y_fraction)
    raise ValueError(f"Unsupported VibeTap settings keyboard letter: {letter!r}")


def vibetap_settings_desc_tap_point(content_desc: str, bounds: Rect) -> tuple[int, int]:
    direct_points = {
        "Keyboard mic key": (0.141, 0.724),
        "Keyboard actions key": (0.409, 0.724),
        "Keyboard float key": (0.409, 0.724),
        "Keyboard backspace key": (0.702, 0.724),
        "Keyboard space key": (0.432, 0.931),
        "Keyboard enter key": (0.849, 0.931),
    }
    if content_desc in direct_points:
        x_fraction, y_fraction = direct_points[content_desc]
        return _rect_point(bounds, x_fraction, y_fraction)

    match = re.fullmatch(r"Keyboard letter ([A-Z]) key", content_desc)
    if match:
        return _settings_keyboard_letter_point(match.group(1), bounds)

    raise ValueError(f"Unsupported VibeTap settings content description fallback: {content_desc!r}")


def vibetap_floating_skill_tap_point(index: int, bounds: Rect) -> tuple[int, int]:
    if index not in (0, 1, 2):
        raise ValueError(f"Unsupported floating skill index: {index}; expected 0, 1, or 2")
    x_positions = (0.192, 0.500, 0.808)
    return _rect_point(bounds, x_positions[index], 0.170)


def tap_point(x: int, y: int, *, serial: str | None = None) -> tuple[int, int]:
    adb("shell", "input", "tap", str(x), str(y), serial=serial)
    return x, y


def tap_vibetap_desc(content_desc: str, *, serial: str | None = None) -> tuple[int, int]:
    try:
        node = find_first(dump_nodes(serial=serial), content_desc=content_desc)
        return tap_node(node, serial=serial)
    except RETRYABLE_UI_ERRORS:
        bounds = device_screen_bounds(serial=serial)
        x, y = vibetap_desc_tap_point(content_desc, bounds)
        return tap_point(x, y, serial=serial)


def tap_vibetap_settings_desc(content_desc: str, *, serial: str | None = None) -> tuple[int, int]:
    try:
        node = find_first(dump_nodes(serial=serial), content_desc=content_desc)
        return tap_node(node, serial=serial)
    except RETRYABLE_UI_ERRORS:
        bounds = device_screen_bounds(serial=serial)
        x, y = vibetap_settings_desc_tap_point(content_desc, bounds)
        return tap_point(x, y, serial=serial)


def tap_vibetap_floating_skill(index: int, *, serial: str | None = None) -> tuple[int, int]:
    bounds = device_screen_bounds(serial=serial)
    x, y = vibetap_floating_skill_tap_point(index, bounds)
    return tap_point(x, y, serial=serial)


def tap_relative(x_fraction: float, y_fraction: float, *, serial: str | None = None) -> tuple[int, int]:
    if not (0.0 <= x_fraction <= 1.0 and 0.0 <= y_fraction <= 1.0):
        raise ValueError("tap-relative fractions must be between 0.0 and 1.0")
    bounds = device_screen_bounds(serial=serial)
    x, y = _rect_point(bounds, x_fraction, y_fraction)
    return tap_point(x, y, serial=serial)


def set_text_relative(x_fraction: float, y_fraction: float, value: str, *, serial: str | None = None) -> str:
    tap_relative(x_fraction, y_fraction, serial=serial)
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

    tap_vibetap_desc = subparsers.add_parser(
        "tap-vibetap-desc",
        help="Tap a VibeTap IME control by content description, with coordinate fallback when the node is unavailable",
    )
    tap_vibetap_desc.add_argument("content_desc")

    tap_vibetap_settings_desc = subparsers.add_parser(
        "tap-vibetap-settings-desc",
        help="Tap a settings-screen VibeTap IME control by content description, with coordinate fallback when the node is unavailable",
    )
    tap_vibetap_settings_desc.add_argument("content_desc")

    tap_vibetap_skill = subparsers.add_parser(
        "tap-vibetap-floating-skill",
        help="Tap one of the three visible VibeTap floating skill buttons by zero-based index",
    )
    tap_vibetap_skill.add_argument("index", type=int)

    tap_relative = subparsers.add_parser(
        "tap-relative",
        help="Tap a normalized screen coordinate using fractions between 0.0 and 1.0",
    )
    tap_relative.add_argument("x_fraction", type=float)
    tap_relative.add_argument("y_fraction", type=float)

    set_text_relative = subparsers.add_parser(
        "set-text-relative",
        help="Tap a normalized screen coordinate and type text into it",
    )
    set_text_relative.add_argument("x_fraction", type=float)
    set_text_relative.add_argument("y_fraction", type=float)
    set_text_relative.add_argument("value")

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

    get_text_desc = subparsers.add_parser("get-text-desc", help="Read the current text value of a node by content description")
    get_text_desc.add_argument("content_desc")

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

        if args.command == "tap-vibetap-desc":
            x, y = tap_vibetap_desc(args.content_desc, serial=args.serial)
            print(f"Tapped VibeTap content description {args.content_desc!r} at ({x}, {y})")
            return 0

        if args.command == "tap-vibetap-settings-desc":
            x, y = tap_vibetap_settings_desc(args.content_desc, serial=args.serial)
            print(f"Tapped VibeTap settings content description {args.content_desc!r} at ({x}, {y})")
            return 0

        if args.command == "tap-vibetap-floating-skill":
            x, y = tap_vibetap_floating_skill(args.index, serial=args.serial)
            print(f"Tapped VibeTap floating skill index {args.index} at ({x}, {y})")
            return 0

        if args.command == "tap-relative":
            x, y = tap_relative(args.x_fraction, args.y_fraction, serial=args.serial)
            print(f"Tapped relative coordinate ({args.x_fraction}, {args.y_fraction}) at ({x}, {y})")
            return 0

        if args.command == "set-text-relative":
            escaped_value = set_text_relative(args.x_fraction, args.y_fraction, args.value, serial=args.serial)
            print(f"Set text via relative coordinate ({args.x_fraction}, {args.y_fraction}): {escaped_value}")
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

        if args.command == "get-text-desc":
            print(get_text_by_content_desc(args.content_desc, serial=args.serial))
            return 0
    except (LookupError, RuntimeError, TimeoutError, ValueError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as exc:
        print(str(exc), file=sys.stderr)
        return 1

    parser.error(f"Unsupported command: {args.command}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
