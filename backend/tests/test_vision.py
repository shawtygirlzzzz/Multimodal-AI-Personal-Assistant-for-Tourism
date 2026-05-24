"""
Quick test for services/vision.py — run from backend/ folder:
  venv/Scripts/python tests/test_vision.py

Place any .jpg photo of a Malacca landmark at:
  backend/tests/test_landmark.jpg
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from dotenv import load_dotenv
load_dotenv(override=True)

from services.vision import analyze_landmark

TEST_IMAGE_PATH = os.path.join(os.path.dirname(__file__), "test_landmarks.jpg")


def test_vision():
    if not os.path.exists(TEST_IMAGE_PATH):
        print("FAIL: No test image found.")
        print(f"   Save any Malacca landmark photo to: {TEST_IMAGE_PATH}")
        sys.exit(1)

    print(f"Loading test image: {TEST_IMAGE_PATH}")
    with open(TEST_IMAGE_PATH, "rb") as f:
        image_bytes = f.read()
    print(f"Image loaded: {len(image_bytes)} bytes")

    print("Calling Gemini Vision...")
    result = analyze_landmark(image_bytes, "What is this landmark? Tell me about it.")

    print("\n--- RESULT ---")
    print(f"Landmark : {result['landmark_name']}")
    print(f"Confidence: {result['confidence']}")
    print(f"Response  :\n{result['response']}")
    print("--------------")

    assert result["confidence"] in ("high", "medium", "low"), "Invalid confidence value"
    assert len(result["response"]) > 50, "Response too short"
    print("\nPASSED: test_vision OK")


if __name__ == "__main__":
    test_vision()