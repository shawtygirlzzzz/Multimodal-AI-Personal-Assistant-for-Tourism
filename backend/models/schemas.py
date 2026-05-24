from pydantic import BaseModel, Field
from typing import Literal, Optional


class AnalyzeResponse(BaseModel):
    status: Literal["success", "error"]
    landmark_name: Optional[str] = None
    response: Optional[str] = None
    session_id: Optional[str] = None
    confidence: Optional[Literal["high", "medium", "low", "unknown"]] = None
    message: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    version: str


class SessionRecord(BaseModel):
    session_id: str
    timestamp: str
    landmark_name: str
    query: str
    response: str
    image_url: Optional[str] = None
    confidence: str = "unknown"
    language: str = "en"


class SessionListResponse(BaseModel):
    sessions: list[SessionRecord] = Field(default_factory=list)


# --- /restaurant ---

class RestaurantResponse(BaseModel):
    status: Literal["success", "error"]
    type: str = "restaurant"
    restaurant_name: Optional[str] = None
    place_id: Optional[str] = None
    rating: Optional[float] = None
    review_count: Optional[int] = None
    top_review: Optional[str] = None
    price_level: Optional[int] = None
    opening_hours: Optional[str] = None
    cuisine: Optional[str] = None
    response: Optional[str] = None
    session_id: Optional[str] = None
    message: Optional[str] = None


# --- /restaurant/nearby ---

class NearbyRequest(BaseModel):
    lat: float
    lng: float
    exclude_place_id: Optional[str] = None
    language: str = "en"


class PlaceAlternative(BaseModel):
    name: str
    rating: float
    price_level: int
    distance_m: int
    opening_hours: str
    cuisine: str
    place_id: str


class NearbyResponse(BaseModel):
    status: Literal["success", "error"]
    alternatives: list[PlaceAlternative] = Field(default_factory=list)
    response: Optional[str] = None
    message: Optional[str] = None
