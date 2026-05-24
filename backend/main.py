import os
from fastapi import FastAPI
from dotenv import load_dotenv

load_dotenv(override=True)

app = FastAPI(title="HeyCyan Malacca Guide API", version=os.getenv("APP_VERSION", "1.0.0"))


@app.get("/health")
async def health():
    from models.schemas import HealthResponse
    return HealthResponse(status="ok", version=app.version)


# Placeholder — wired up in Step 1.3+
from routers import analyze  # noqa: E402
app.include_router(analyze.router)

from routers import restaurant  # noqa: E402
app.include_router(restaurant.router)