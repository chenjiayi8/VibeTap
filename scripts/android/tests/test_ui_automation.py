import unittest

from scripts.android.ui_automation import UiNode, find_first, parse_bounds, parse_nodes


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


if __name__ == "__main__":
    unittest.main()
