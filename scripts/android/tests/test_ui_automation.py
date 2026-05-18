import contextlib
import io
import subprocess
import unittest
from unittest import mock

from scripts.android import ui_automation
from scripts.android.ui_automation import (
    CompletedProcessResult,
    UiNode,
    device_screen_bounds,
    find_first,
    parse_bounds,
    parse_nodes,
    screen_bounds,
    vibetap_desc_tap_point,
    vibetap_floating_skill_tap_point,
    vibetap_settings_desc_tap_point,
)


SAMPLE_XML = """
<hierarchy rotation="0">
  <node text="Open keyboard settings" content-desc="" bounds="[0,0][200,80]" />
  <node text="" content-desc="OpenAI API Key input" bounds="[0,80][300,160]" />
  <node text="Mic" content-desc="Mic key" bounds="[0,160][120,240]" />
</hierarchy>
"""


class UiAutomationTests(unittest.TestCase):
    def test_parse_bounds_returns_tap_point(self):
        self.assertEqual((100, 40), parse_bounds("[0,0][200,80]").tap_point())

    def test_parse_nodes_extracts_text_and_content_desc(self):
        nodes = parse_nodes(SAMPLE_XML)
        self.assertEqual(
            [
                UiNode(text="Open keyboard settings", content_desc="", bounds="[0,0][200,80]"),
                UiNode(text="", content_desc="OpenAI API Key input", bounds="[0,80][300,160]"),
                UiNode(text="Mic", content_desc="Mic key", bounds="[0,160][120,240]"),
            ],
            nodes,
        )

    def test_find_first_matches_text(self):
        nodes = parse_nodes(SAMPLE_XML)
        match = find_first(nodes, text="Open keyboard settings")
        self.assertEqual("[0,0][200,80]", match.bounds)

    def test_find_first_matches_content_description(self):
        nodes = parse_nodes(SAMPLE_XML)
        match = find_first(nodes, content_desc="OpenAI API Key input")
        self.assertEqual("[0,80][300,160]", match.bounds)

    def test_find_first_matches_text_contains(self):
        nodes = parse_nodes(SAMPLE_XML)
        match = find_first(nodes, text_contains="keyboard")
        self.assertEqual("[0,0][200,80]", match.bounds)

    def test_adb_builds_command_with_serial_and_timeout(self):
        completed = subprocess.CompletedProcess(
            args=["adb", "devices"],
            returncode=0,
            stdout="device list",
            stderr="",
        )
        with mock.patch("scripts.android.ui_automation.subprocess.run", return_value=completed) as run_mock:
            result = ui_automation.adb("shell", "getprop", serial="emulator-5554", timeout=1.5)

        self.assertEqual("device list", result.stdout)
        run_mock.assert_called_once_with(
            ["adb", "-s", "emulator-5554", "shell", "getprop"],
            check=False,
            capture_output=True,
            text=True,
            timeout=1.5,
        )

    def test_dump_nodes_runs_uiautomator_dump_then_reads_xml(self):
        with mock.patch(
            "scripts.android.ui_automation.adb",
            side_effect=[
                CompletedProcessResult(stdout="UI hierchary dumped", stderr="", returncode=0),
                CompletedProcessResult(stdout=f"dumped\n{SAMPLE_XML}", stderr="", returncode=0),
            ],
        ) as adb_mock:
            nodes = ui_automation.dump_nodes(serial="emulator-5554")

        self.assertEqual("Mic", nodes[-1].text)
        self.assertEqual(
            [
                mock.call("shell", "uiautomator", "dump", "/data/local/tmp/vibetap-window-dump.xml", serial="emulator-5554", timeout=10.0),
                mock.call("exec-out", "cat", "/data/local/tmp/vibetap-window-dump.xml", serial="emulator-5554", timeout=10.0),
            ],
            adb_mock.call_args_list,
        )

    def test_wait_for_node_retries_lookup_and_transient_failures_until_success(self):
        target = UiNode(text="Ready", content_desc="Ready desc", bounds="[10,20][30,40]")
        with (
            mock.patch(
                "scripts.android.ui_automation.dump_nodes",
                side_effect=[
                    subprocess.CalledProcessError(1, ["adb"], stderr="device offline"),
                    RuntimeError("malformed dump"),
                    [UiNode(text="Other", content_desc="", bounds="[0,0][1,1]")],
                    [target],
                ],
            ) as dump_nodes_mock,
            mock.patch("scripts.android.ui_automation.time.sleep") as sleep_mock,
            mock.patch(
                "scripts.android.ui_automation.time.monotonic",
                side_effect=[0.0, 0.0, 0.1, 0.2, 0.3],
            ),
        ):
            match = ui_automation.wait_for_node(text="Ready", timeout_seconds=2.0, interval_seconds=0.01)

        self.assertEqual(target, match)
        self.assertEqual(4, dump_nodes_mock.call_count)
        self.assertEqual(3, sleep_mock.call_count)

    def test_wait_for_node_raises_timeout_with_last_retryable_error(self):
        with (
            mock.patch(
                "scripts.android.ui_automation.dump_nodes",
                side_effect=[RuntimeError("first dump failed"), RuntimeError("last dump failed")],
            ),
            mock.patch("scripts.android.ui_automation.time.sleep"),
            mock.patch(
                "scripts.android.ui_automation.time.monotonic",
                side_effect=[0.0, 0.0, 0.05, 0.2],
            ),
        ):
            with self.assertRaises(TimeoutError) as raised:
                ui_automation.wait_for_node(text="Ready", timeout_seconds=0.1, interval_seconds=0.01)

        self.assertIn("last dump failed", str(raised.exception))

    def test_set_text_taps_before_typing_and_escapes_spaces(self):
        target = UiNode(text="", content_desc="Input", bounds="[0,80][300,160]")
        events: list[tuple[str, str | None]] = []

        def record_tap(node, *, serial=None):
            events.append(("tap", serial))
            return (150, 120)

        def record_adb(*args, serial=None, check=True, timeout=None):
            events.append(("adb", serial))
            self.assertEqual(("shell", "input", "text", "hello%sworld"), args)
            self.assertTrue(check)
            self.assertIsNone(timeout)
            return CompletedProcessResult(stdout="", stderr="", returncode=0)

        with (
            mock.patch("scripts.android.ui_automation.tap_node", side_effect=record_tap) as tap_mock,
            mock.patch("scripts.android.ui_automation.adb", side_effect=record_adb) as adb_mock,
        ):
            escaped = ui_automation.set_text(target, "hello world", serial="emulator-5554")

        self.assertEqual("hello%sworld", escaped)
        self.assertEqual([("tap", "emulator-5554"), ("adb", "emulator-5554")], events)
        tap_mock.assert_called_once_with(target, serial="emulator-5554")
        adb_mock.assert_called_once()

    def test_set_text_rejects_percent_characters(self):
        with self.assertRaises(ValueError):
            ui_automation.set_text(UiNode(text="", content_desc="", bounds="[0,0][1,1]"), "100% ready")

    def test_screen_bounds_uses_root_node_bounds(self):
        nodes = parse_nodes(SAMPLE_XML)
        self.assertEqual(parse_bounds("[0,0][200,80]"), screen_bounds(nodes))

    def test_device_screen_bounds_prefers_override_size(self):
        with mock.patch(
            "scripts.android.ui_automation.adb",
            return_value=CompletedProcessResult(
                stdout="Physical size: 1080x2400\nOverride size: 720x1600\n",
                stderr="",
                returncode=0,
            ),
        ) as adb_mock:
            bounds = device_screen_bounds(serial="emulator-5554")

        self.assertEqual(parse_bounds("[0,0][720,1600]"), bounds)
        adb_mock.assert_called_once_with("shell", "wm", "size", serial="emulator-5554")

    def test_vibetap_desc_tap_point_maps_keyboard_controls(self):
        bounds = parse_bounds("[0,0][1080,2400]")
        self.assertEqual((540, 1762), vibetap_desc_tap_point("Keyboard float key", bounds))
        self.assertEqual((477, 2333), vibetap_desc_tap_point("Keyboard space key", bounds))
        self.assertEqual((990, 1949), vibetap_desc_tap_point("Keyboard letter P key", bounds))
        self.assertEqual((133, 2213), vibetap_desc_tap_point("Keyboard letter Z key", bounds))

    def test_vibetap_floating_skill_tap_point_maps_three_skill_slots(self):
        bounds = parse_bounds("[0,0][1080,2400]")
        self.assertEqual((207, 408), vibetap_floating_skill_tap_point(0, bounds))
        self.assertEqual((540, 408), vibetap_floating_skill_tap_point(1, bounds))
        self.assertEqual((873, 408), vibetap_floating_skill_tap_point(2, bounds))
        with self.assertRaises(ValueError):
            vibetap_floating_skill_tap_point(3, bounds)

    def test_vibetap_settings_desc_tap_point_maps_settings_keyboard_controls(self):
        bounds = parse_bounds("[0,0][1080,2400]")
        self.assertEqual((442, 1738), vibetap_settings_desc_tap_point("Keyboard float key", bounds))
        self.assertEqual((467, 2234), vibetap_settings_desc_tap_point("Keyboard space key", bounds))
        self.assertEqual((82, 2148), vibetap_settings_desc_tap_point("Keyboard letter Z key", bounds))
        self.assertEqual((690, 2148), vibetap_settings_desc_tap_point("Keyboard letter B key", bounds))

    def test_get_text_by_content_desc_reads_parent_text_when_desc_node_is_child(self):
        xml = '''
        <hierarchy>
          <node text="parent text" bounds="[0,0][10,10]">
            <node text="" content-desc="Input" bounds="[0,0][10,10]" />
          </node>
        </hierarchy>
        '''
        with mock.patch("scripts.android.ui_automation.dump_xml_text", return_value=xml):
            self.assertEqual("parent text", ui_automation.get_text_by_content_desc("Input"))

    def test_cli_subcommands_dispatch_and_exit_success(self):
        target = UiNode(text="Ready", content_desc="Input", bounds="[1,2][3,4]")
        cases = [
            (
                ["tap-text", "Ready"],
                [mock.call(serial=None, text="Ready")],
                [mock.call(target, serial=None)],
                None,
                0,
            ),
            (
                ["tap-desc", "Input"],
                [mock.call(serial=None, content_desc="Input")],
                [mock.call(target, serial=None)],
                None,
                0,
            ),
            (
                ["wait-text", "Ready", "--timeout", "1.5", "--interval", "0.25"],
                [mock.call(serial=None, timeout_seconds=1.5, interval_seconds=0.25, text="Ready")],
                [],
                None,
                0,
            ),
            (
                ["wait-text-contains", "ead"],
                [mock.call(serial=None, timeout_seconds=10.0, interval_seconds=0.5, text_contains="ead")],
                [],
                None,
                0,
            ),
            (
                ["wait-desc", "Input"],
                [mock.call(serial=None, timeout_seconds=10.0, interval_seconds=0.5, content_desc="Input")],
                [],
                None,
                0,
            ),
            (
                ["set-text-desc", "Input", "hello world"],
                [mock.call(serial=None, content_desc="Input")],
                [],
                [mock.call(target, "hello world", serial=None)],
                0,
            ),
        ]

        for argv, wait_calls, tap_calls, set_text_calls, expected_exit_code in cases:
            with self.subTest(argv=argv):
                stdout = io.StringIO()
                stderr = io.StringIO()
                with (
                    mock.patch("scripts.android.ui_automation.wait_for_node", return_value=target) as wait_mock,
                    mock.patch("scripts.android.ui_automation.tap_node", return_value=(2, 3)) as tap_mock,
                    mock.patch("scripts.android.ui_automation.set_text", return_value="hello%sworld") as set_text_mock,
                    contextlib.redirect_stdout(stdout),
                    contextlib.redirect_stderr(stderr),
                ):
                    exit_code = ui_automation.main(argv)

                self.assertEqual(expected_exit_code, exit_code)
                self.assertEqual(wait_calls, wait_mock.call_args_list)
                self.assertEqual(tap_calls, tap_mock.call_args_list)
                if set_text_calls is None:
                    self.assertEqual([], set_text_mock.call_args_list)
                else:
                    self.assertEqual(set_text_calls, set_text_mock.call_args_list)
                self.assertEqual("", stderr.getvalue())

    def test_cli_dispatches_vibetap_fallback_commands(self):
        stdout = io.StringIO()
        stderr = io.StringIO()
        with (
            mock.patch("scripts.android.ui_automation.tap_vibetap_desc", return_value=(111, 222)) as tap_desc_mock,
            mock.patch("scripts.android.ui_automation.tap_vibetap_settings_desc", return_value=(123, 234)) as tap_settings_desc_mock,
            mock.patch("scripts.android.ui_automation.tap_vibetap_floating_skill", return_value=(333, 444)) as tap_skill_mock,
            mock.patch("scripts.android.ui_automation.tap_relative", return_value=(555, 666)) as tap_relative_mock,
            mock.patch("scripts.android.ui_automation.set_text_relative", return_value="hello%sworld") as set_text_relative_mock,
            mock.patch("scripts.android.ui_automation.get_text_by_content_desc", return_value="hello world") as get_text_desc_mock,
            contextlib.redirect_stdout(stdout),
            contextlib.redirect_stderr(stderr),
        ):
            exit_code_desc = ui_automation.main(["tap-vibetap-desc", "Keyboard mic key"])
            exit_code_settings_desc = ui_automation.main(["tap-vibetap-settings-desc", "Keyboard mic key"])
            exit_code_skill = ui_automation.main(["tap-vibetap-floating-skill", "2"])
            exit_code_relative = ui_automation.main(["tap-relative", "0.16", "0.68"])
            exit_code_set_relative = ui_automation.main(["set-text-relative", "0.45", "0.33", "hello world"])
            exit_code_get_text_desc = ui_automation.main(["get-text-desc", "Input"])

        self.assertEqual(0, exit_code_desc)
        self.assertEqual(0, exit_code_settings_desc)
        self.assertEqual(0, exit_code_skill)
        self.assertEqual(0, exit_code_relative)
        self.assertEqual(0, exit_code_set_relative)
        self.assertEqual(0, exit_code_get_text_desc)
        tap_desc_mock.assert_called_once_with("Keyboard mic key", serial=None)
        tap_settings_desc_mock.assert_called_once_with("Keyboard mic key", serial=None)
        tap_skill_mock.assert_called_once_with(2, serial=None)
        tap_relative_mock.assert_called_once_with(0.16, 0.68, serial=None)
        set_text_relative_mock.assert_called_once_with(0.45, 0.33, "hello world", serial=None)
        get_text_desc_mock.assert_called_once_with("Input", serial=None)
        self.assertEqual("", stderr.getvalue())

    def test_cli_returns_error_exit_code_for_driver_failures(self):
        stderr = io.StringIO()
        with (
            mock.patch("scripts.android.ui_automation.wait_for_node", side_effect=TimeoutError("not found")),
            contextlib.redirect_stderr(stderr),
        ):
            exit_code = ui_automation.main(["wait-text", "Ready"])

        self.assertEqual(1, exit_code)
        self.assertIn("not found", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
