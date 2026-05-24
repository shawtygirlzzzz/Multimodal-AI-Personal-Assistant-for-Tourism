from google.genai import types


def build_search_tool() -> types.Tool:
    """Returns the Gemini built-in Google Search grounding tool."""
    return types.Tool(google_search=types.GoogleSearch())
