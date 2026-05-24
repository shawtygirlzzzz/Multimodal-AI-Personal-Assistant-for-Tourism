import asyncio
import logging
from fastapi import APIRouter, UploadFile, File, Form
from google.genai.errors import ClientError, ServerError
from services.vision import analyze_landmark
from models.schemas import AnalyzeResponse

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze(
    image: UploadFile = File(...),
    query: str = Form(default="What is this building? Tell me about it."),
    language: str = Form(default="en"),
    landmark_context: str = Form(default=""),
):
    if image.content_type not in ("image/jpeg", "image/png", "image/jpg"):
        return AnalyzeResponse(
            status="error",
            message="Only JPEG and PNG images are supported.",
        )

    image_bytes = await image.read()
    if len(image_bytes) > 5 * 1024 * 1024:
        return AnalyzeResponse(
            status="error",
            message="Image must be under 5MB.",
        )

    try:
        result = await asyncio.to_thread(analyze_landmark, image_bytes, query, language, landmark_context)
        return AnalyzeResponse(
            status="success",
            landmark_name=result["landmark_name"],
            response=result["response"],
            session_id=None,
            confidence=result["confidence"],
        )
    except ClientError as e:
        logger.error("Gemini client error: %s", e)
        return AnalyzeResponse(
            status="error",
            message="Could not identify the landmark. Please try a closer angle.",
        )
    except ServerError as e:
        logger.error("Gemini server error: %s", e)
        return AnalyzeResponse(
            status="error",
            message="AI service is temporarily unavailable. Please try again.",
        )
    except Exception as e:
        logger.error("Unexpected error: %s", e)
        return AnalyzeResponse(
            status="error",
            message="Something went wrong. Please try again.",
        )