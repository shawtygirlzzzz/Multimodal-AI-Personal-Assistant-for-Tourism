# System Design Document (SDD)
# HeyCyan Malacca Tourist Guide App

**Version:** 2.0  
**Date:** April 2026  
**Author:** [Your Name]  
**Status:** Draft  
**Related:** prd.md

---

## 1. System Overview

This document describes the technical architecture, folder structure, API contracts,
database schema, frontend screen design, and build order for the HeyCyan Malacca
Tourist Guide app.

The system has three layers:
- **HeyCyan smart glasses** — hardware input/output (camera, mic, speaker) via BLE
- **Android app (Kotlin)** — BLE bridge, STT, TTS, UI
- **Python backend (FastAPI)** — vision AI, web search, database writes

**Important build rule:** Backend must be fully tested before Android frontend begins.
Android frontend must be fully tested with phone camera before glasses BLE is added.

---

## 2. Tech Stack

### 2.1 Backend
| Component | Technology |
|---|---|
| Language | Python 3.11+ |
| Framework | FastAPI |
| Vision + LLM | Gemini 2.5 Flash (`gemini-2.5-flash`) |
| Live voice API | Gemini 3.1 Flash Live (`gemini-3.1-flash-live-preview`) |
| TTS | Gemini 3.1 Flash TTS (`gemini-3.1-flash-tts-preview`) |
| Web search | Google Custom Search API or Brave Search API |
| Database | Firebase Firestore |
| Image storage | Firebase Cloud Storage |
| Hosting | Google Cloud Run |
| Environment | Python venv + `.env` file |

### 2.2 Android App
| Component | Technology |
|---|---|
| Language | Kotlin |
| UI framework | Jetpack Compose |
| Navigation | Jetpack Navigation Compose |
| HTTP client | Retrofit 2 + OkHttp |
| Camera (Phase 2) | CameraX |
| BLE (Phase 3) | HeyCyan SDK (.aar) |
| STT | Android SpeechRecognizer → Gemini Live API (WebSocket) |
| TTS | Android TextToSpeech → Gemini TTS API |
| State management | ViewModel + StateFlow |
| Local cache | Room Database |
| Auth | Firebase Auth (anonymous) |
| Min SDK | API 26 (Android 8.0) |

### 2.3 External APIs
| API | Purpose | Key needed |
|---|---|---|
| Google Gemini API | Vision, LLM, STT, TTS | `GEMINI_API_KEY` |
| Google Custom Search | Web search for current info | `GOOGLE_SEARCH_API_KEY` + `SEARCH_ENGINE_ID` |
| Google Places API (New) | Restaurant rating, reviews, nearby search | `GOOGLE_PLACES_API_KEY` |
| Firebase | Firestore + Cloud Storage | Service account JSON |

---

## 3. Architecture Diagram (Text)

```
[HeyCyan Glasses]
    |  BLE (photo data, audio)
    v
[Android App — Kotlin]
    |  captures image + voice
    |  STT via Gemini Live API (WebSocket)
    |  sends HTTP POST to backend
    |
    v
[FastAPI Backend — Python]        <——> [Gemini 2.5 Flash] (vision + LLM)
    |                             <——> [Google Search API] (current info)
    |  saves session
    v
[Firebase Firestore]   [Firebase Cloud Storage]
    (session metadata)     (raw images)

[FastAPI Backend]
    |  returns JSON response text
    v
[Android App]
    |  TTS (speak response)
    v
[HeyCyan Glasses Speaker]
```

---

## 4. Folder Structure

### 4.1 Backend (`malacca-backend/`)
```
malacca-backend/
├── main.py                  # FastAPI app entry point
├── requirements.txt         # Python dependencies
├── .env                     # API keys (never commit to git)
├── .gitignore
├── Dockerfile               # For Cloud Run deployment
├── services/
│   ├── __init__.py
│   ├── vision.py            # Gemini vision call (landmark ID)
│   ├── search.py            # Google Search API wrapper
│   ├── places.py            # Google Places API wrapper (restaurant details + nearby search)
│   └── database.py          # Firestore read/write
├── models/
│   ├── __init__.py
│   └── schemas.py           # Pydantic request/response models
└── tests/
    ├── test_vision.py
    └── test_api.py
```

### 4.2 Android App (`malacca-android/`)
```
malacca-android/
├── app/
│   ├── libs/
│   │   └── QCBleSdk.aar              # HeyCyan SDK (add manually in Phase 3)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/malacca/guide/
│   │       ├── MainActivity.kt        # Single activity, hosts nav graph
│   │       ├── ui/
│   │       │   ├── theme/
│   │       │   │   ├── Color.kt       # App color palette
│   │       │   │   ├── Theme.kt       # MaterialTheme setup
│   │       │   │   └── Type.kt        # Typography
│   │       │   ├── navigation/
│   │       │   │   └── NavGraph.kt    # All screen routes
│   │       │   ├── screens/
│   │       │   │   ├── SplashScreen.kt     # Launch screen
│   │       │   │   ├── HomeScreen.kt       # Main tourist UI
│   │       │   │   ├── ListeningScreen.kt  # Active voice input UI
│   │       │   │   ├── LoadingScreen.kt    # Waiting for AI response
│   │       │   │   ├── ResultScreen.kt     # Show landmark + AI response
│   │       │   │   └── HistoryScreen.kt    # Past sessions list (Phase 4)
│   │       │   └── components/
│   │       │       ├── PulseButton.kt      # Animated mic button
│   │       │       ├── WaveformView.kt     # Audio waveform animation
│   │       │       ├── LandmarkCard.kt     # Result card component
│   │       │       └── ErrorBanner.kt      # Error message component
│   │       ├── ble/
│   │       │   ├── GlassesManager.kt       # HeyCyan BLE wrapper (Phase 3)
│   │       │   └── BleStateManager.kt      # BLE connection state
│   │       ├── api/
│   │       │   ├── ApiClient.kt            # Retrofit + OkHttp setup
│   │       │   ├── ApiService.kt           # Endpoint definitions
│   │       │   └── models/
│   │       │       └── ApiModels.kt        # Request/response data classes
│   │       ├── voice/
│   │       │   ├── SttManager.kt           # Speech-to-text manager
│   │       │   └── TtsManager.kt           # Text-to-speech manager
│   │       ├── camera/
│   │       │   └── CameraManager.kt        # CameraX wrapper (Phase 2)
│   │       ├── data/
│   │       │   ├── SessionRepository.kt    # Single source of truth
│   │       │   └── local/
│   │       │       ├── AppDatabase.kt      # Room DB setup
│   │       │       ├── SessionDao.kt       # Room queries
│   │       │       └── SessionEntity.kt    # Room table definition
│   │       └── viewmodel/
│   │           └── MainViewModel.kt        # All screen state + logic
│   └── build.gradle
├── build.gradle
└── google-services.json      # Firebase config (never commit to git)
```

---

## 5. API Endpoints

### 5.1 `POST /analyze`
Accepts an image and a text query. Returns an AI-generated tourist guide response.

**Request** (`multipart/form-data`):
```
image   : File    (JPEG or PNG, max 5MB)
query   : string  (default: "What is this building? Tell me about it.")
language: string  (default: "en")
```

**Response** (`application/json`):
```json
{
  "status": "success",
  "landmark_name": "Stadthuys",
  "response": "This is the Stadthuys, the oldest surviving Dutch building in Asia...",
  "session_id": "abc123",
  "confidence": "high"
}
```

**Error response:**
```json
{
  "status": "error",
  "message": "Could not identify the landmark. Please try a closer angle.",
  "session_id": null
}
```

---

### 5.2 `GET /health`
Health check endpoint for Cloud Run.

**Response:**
```json
{ "status": "ok", "version": "1.0.0" }
```

---

### 5.3 `POST /restaurant`
Accepts an image of a restaurant or cafe facade. Uses Gemini Vision to extract the restaurant name from signage, then fetches details from Google Places API.

**Request** (`multipart/form-data`):
```
image    : File    (JPEG or PNG, max 5MB)
lat      : float   (device GPS latitude)
lng      : float   (device GPS longitude)
language : string  (default: "en")
```

**Response** (`application/json`):
```json
{
  "status": "success",
  "type": "restaurant",
  "restaurant_name": "Jonker 88",
  "place_id": "ChIJxxxxxxxxxxxxxxxx",
  "rating": 4.3,
  "review_count": 1247,
  "top_review": "Must try the cendol and laksa here!",
  "price_level": 1,
  "opening_hours": "Open now · Closes 10 PM",
  "cuisine": "Malaysian, Local Food",
  "response": "This is Jonker 88, a popular local eatery rated 4.3 stars with over 1,200 reviews. Known for cendol and chicken rice ball. Currently open until 10 PM.",
  "session_id": "abc123"
}
```

**Error response:**
```json
{
  "status": "error",
  "message": "Could not identify a restaurant from this image. Try a closer angle on the signage.",
  "session_id": null
}
```

---

### 5.4 `POST /restaurant/nearby`
Returns up to 3 alternative restaurants or cafes near the tourist's current GPS location. Called when the tourist declines the currently identified restaurant.

**Request** (`application/json`):
```json
{
  "lat": 2.1944,
  "lng": 102.2501,
  "exclude_place_id": "ChIJxxxxxxxxxxxxxxxx",
  "language": "en"
}
```

**Response** (`application/json`):
```json
{
  "status": "success",
  "alternatives": [
    {
      "name": "Nancy's Kitchen",
      "rating": 4.5,
      "price_level": 2,
      "distance_m": 87,
      "opening_hours": "Open now · Closes 9 PM",
      "cuisine": "Peranakan",
      "place_id": "ChIJyyyyyyyyyyyyyyyy"
    },
    {
      "name": "Hoe Kee Chicken Rice",
      "rating": 4.2,
      "price_level": 1,
      "distance_m": 134,
      "opening_hours": "Open now · Closes 8 PM",
      "cuisine": "Malaysian, Chinese",
      "place_id": "ChIJzzzzzzzzzzzzzzzz"
    }
  ],
  "response": "Here are 3 nearby alternatives. Nancy's Kitchen, rated 4.5 stars, about 87 metres away, serving Peranakan food. Hoe Kee Chicken Rice, rated 4.2 stars, 134 metres away..."
}
```

---

### 5.5 `GET /sessions/{tourist_id}`
Retrieve past sessions for a tourist (Phase 4).

**Response:**
```json
{
  "sessions": [
    {
      "session_id": "abc123",
      "timestamp": "2026-04-29T10:30:00Z",
      "landmark_name": "Stadthuys",
      "query": "What is this building?",
      "response": "This is the Stadthuys...",
      "image_url": "https://storage.googleapis.com/..."
    }
  ]
}
```

---

## 6. Database Schema (Firestore)

### Collection: `sessions`
```
sessions/
  {session_id}/
    tourist_id    : string   (anonymous Firebase UID)
    timestamp     : datetime
    landmark_name : string   (e.g. "Stadthuys")
    query         : string   (what the tourist asked)
    response      : string   (AI answer)
    image_url     : string   (Firebase Cloud Storage URL)
    confidence    : string   ("high" | "medium" | "low" | "unknown")
    language      : string   (e.g. "en")
    location      : geopoint (optional, if GPS available)
```

### Collection: `landmarks` (Phase 4 — pre-seeded reference data)
```
landmarks/
  {landmark_id}/
    name          : string
    aliases       : array<string>
    description   : string
    opening_hours : string
    ticket_price  : string
    image_ref     : string   (reference image URL for fallback matching)
    coordinates   : geopoint
```

---

## 7. Environment Variables

### Backend `.env`
```
GEMINI_API_KEY=your_gemini_api_key_here
GOOGLE_SEARCH_API_KEY=your_search_key_here
GOOGLE_SEARCH_ENGINE_ID=your_cx_id_here
FIREBASE_CREDENTIALS_PATH=./firebase-service-account.json
CLOUD_STORAGE_BUCKET=your-bucket-name.appspot.com
APP_VERSION=1.0.0
```

### Android `local.properties` (never commit)
```
BACKEND_BASE_URL=http://192.168.1.x:8000   # local dev (same WiFi as laptop)
# BACKEND_BASE_URL=https://your-app.run.app  # production
GEMINI_API_KEY=your_gemini_api_key_here
```

---

## 8. Gemini API Usage per Feature

| Feature | Model | API Type | Notes |
|---|---|---|---|
| Landmark identification | `gemini-2.5-flash` | REST (multimodal) | Stable, production-safe |
| LLM answer generation | `gemini-2.5-flash` | REST | Same call as vision |
| Voice input (STT) | `gemini-3.1-flash-live-preview` | WebSocket (Live API) | Preview — monitor deprecation |
| Voice output (TTS) | `gemini-3.1-flash-tts-preview` | REST | Preview — fallback to Android TTS |
| Web search grounding | Built into Gemini tool use | REST | Enable `google_search` tool |

---

## 9. Frontend Design (Android — Phase 2 & 3)

### 9.1 Screen Flow Diagram
```
App launch
    |
    v
[SplashScreen]  (2 seconds, logo + tagline)
    |
    v
[HomeScreen]  ← ← ← ← ← ← ← ← ← ← ←
    |                                  |
    | Tourist taps mic button          | Back / Done
    v                                  |
[ListeningScreen]                      |
    |                                  |
    | Voice captured, camera fires     |
    v                                  |
[LoadingScreen]  (AI is thinking...)   |
    |                                  |
    | Response received                |
    v                                  |
[ResultScreen]  ——————————————————————
    |
    | Tourist taps "History" (Phase 4)
    v
[HistoryScreen]
```

---

### 9.2 Screen Details

---

#### Screen 1 — SplashScreen
**File:** `ui/screens/SplashScreen.kt`  
**Purpose:** App launch screen shown for 2 seconds, then auto-navigates to HomeScreen.

**Components:**
- App logo (centred)
- App name: "HeyCyan Guide"
- Tagline: "Your AI guide to Malacca"
- Background: dark teal (`#0D3B33`)

**Behaviour:**
- Auto-navigate to HomeScreen after 2000ms using `LaunchedEffect`
- No back button

**Code notes:**
```kotlin
LaunchedEffect(Unit) {
    delay(2000)
    navController.navigate("home") {
        popUpTo("splash") { inclusive = true }
    }
}
```

---

#### Screen 2 — HomeScreen
**File:** `ui/screens/HomeScreen.kt`  
**Purpose:** Main screen the tourist sees. Central button to start interaction.

**Components:**
```
┌─────────────────────────────────┐
│  [Battery icon]    [History icon]│  ← top bar
│                                 │
│  ┌──────────┐  ┌──────────────┐ │
│  │ LANDMARK │  │  RESTAURANT  │ │  ← mode toggle (segmented control)
│  └──────────┘  └──────────────┘ │     default: LANDMARK
│                                 │
│      "Point your glasses        │
│       at a landmark"            │  ← instruction text (changes per mode)
│                                 │
│         ┌─────────┐             │
│         │   MIC   │             │  ← large pulsing circle button
│         │  BUTTON │             │     (PulseButton component)
│         └─────────┘             │
│                                 │
│   "Tap and ask HeyCyan          │
│    about what you see"          │  ← sub-instruction
│                                 │
│  [BLE status indicator]         │  ← bottom: "Glasses connected ✓"
│                                 │     or "Using phone camera"
└─────────────────────────────────┘
```

**State shown from ViewModel:**
- `bleConnected: Boolean` → show glasses status at bottom
- `isLoading: Boolean` → disable button while loading
- `appMode: AppMode` → `LANDMARK` or `RESTAURANT` (controls which endpoint is called)

**On mic button tap:**
- Navigate to `ListeningScreen`
- Start `SttManager.startListening()`
- Trigger camera capture (phone CameraX in Phase 2, glasses BLE in Phase 3)
- If `appMode == LANDMARK` → calls `POST /analyze`
- If `appMode == RESTAURANT` → calls `POST /restaurant` with GPS coordinates

**Error states:**
- No internet → show `ErrorBanner("No internet connection")`
- Camera permission denied → show `ErrorBanner("Camera permission needed")`

---

#### Screen 3 — ListeningScreen
**File:** `ui/screens/ListeningScreen.kt`  
**Purpose:** Shown while the tourist is speaking. Gives visual feedback that voice is being recorded.

**Components:**
```
┌─────────────────────────────────┐
│                                 │
│                                 │
│       "I'm listening..."        │  ← animated text (fade in/out)
│                                 │
│    ~~~~~~~~~~~~~~~~~~~~         │
│    ~~~~ [WAVEFORM] ~~~~         │  ← WaveformView component
│    ~~~~~~~~~~~~~~~~~~~~         │     (animated audio bars)
│                                 │
│       [STOP button]             │  ← tourist can tap to stop early
│                                 │
│                                 │
└─────────────────────────────────┘
```

**Behaviour:**
- Auto-navigate to LoadingScreen when STT detects end of speech
- Tourist can tap STOP to manually end recording
- Waveform animates based on microphone amplitude
- If STT returns empty string → show toast "I didn't catch that, try again"
  and navigate back to HomeScreen

**Code notes:**
```kotlin
// WaveformView: use Canvas in Compose, animate bar heights with LaunchedEffect
// listening state managed in MainViewModel.uiState
```

---

#### Screen 4 — LoadingScreen
**File:** `ui/screens/LoadingScreen.kt`  
**Purpose:** Shown while the backend is processing the image + query. Keeps tourist engaged.

**Components:**
```
┌─────────────────────────────────┐
│                                 │
│                                 │
│      [Rotating logo / spinner]  │  ← animated
│                                 │
│    "Identifying landmark..."    │  ← cycling messages every 1.5s:
│                                 │     1. "Identifying landmark..."
│                                 │     2. "Searching for details..."
│                                 │     3. "Almost there..."
│                                 │
│      [thumbnail of photo]       │  ← small preview of captured image
│                                 │
└─────────────────────────────────┘
```

**Behaviour:**
- Not dismissible (tourist must wait)
- If backend takes > 10 seconds → auto-navigate back to HomeScreen with error:
  `ErrorBanner("Connection timeout. Please try again.")`
- Loading messages rotate every 1500ms using `LaunchedEffect` + `rememberInfiniteTransition`

---

#### Screen 5 — ResultScreen
**File:** `ui/screens/ResultScreen.kt`  
**Purpose:** Show the landmark name and AI response. TTS plays automatically on arrival.

**Components:**
```
┌─────────────────────────────────┐
│  [Back arrow]                   │  ← back to HomeScreen
│                                 │
│  ┌───────────────────────────┐  │
│  │  [Captured image preview] │  │  ← LandmarkCard component
│  │                           │  │
│  │  Stadthuys                │  ← landmark name (large, bold)
│  │  ★ High confidence        │  ← confidence badge
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ "This is the Stadthuys,   │  │  ← AI response text
│  │  the oldest surviving     │  │     (scrollable)
│  │  Dutch building in Asia.  │  │
│  │  Built in 1650..."        │  │
│  └───────────────────────────┘  │
│                                 │
│  [🔊 Replay]  [Ask follow-up]  │  ← action buttons
│                                 │
│  [Ask another landmark]         │  ← back to HomeScreen
└─────────────────────────────────┘
```

**Behaviour (Landmark mode):**
- TTS starts automatically when screen appears (`LaunchedEffect` on screen entry)
- "Replay" button → re-trigger TTS
- "Ask follow-up" button → return to ListeningScreen with same image context
- "Ask another landmark" → navigate back to HomeScreen
- Confidence badge colours:
  - high → green
  - medium → amber
  - low / unknown → red with message "Try a clearer angle next time"

**Behaviour (Restaurant mode):**
```
┌─────────────────────────────────┐
│  [Back arrow]                   │
│                                 │
│  ┌───────────────────────────┐  │
│  │  [Captured image preview] │  │
│  │                           │  │
│  │  Jonker 88           ★4.3 │  ← restaurant name + rating
│  │  $ · Malaysian            │  ← price level + cuisine
│  │  Open now · Closes 10 PM  │  ← opening hours
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ "Jonker 88 is rated 4.3   │  │  ← AI spoken response (scrollable)
│  │  stars. Known for cendol  │  │
│  │  and chicken rice ball..."│  │
│  └───────────────────────────┘  │
│                                 │
│  [🔊 Replay]  [Find Nearby]    │  ← "Find Nearby" calls POST /restaurant/nearby
│                                 │
│  [Scan another restaurant]      │  ← back to HomeScreen (restaurant mode)
└─────────────────────────────────┘
```
- "Find Nearby" button → calls `POST /restaurant/nearby` with current GPS + `exclude_place_id`
- Nearby results replace the current result card with a list of 3 alternatives
- TTS speaks the alternatives response automatically

**Data received from ViewModel:**
```kotlin
data class LandmarkResult(
    val landmarkName: String,
    val response: String,
    val confidence: String,   // "high" | "medium" | "low" | "unknown"
    val imageBitmap: Bitmap?,
    val sessionId: String
) : AppUiState()

data class RestaurantResult(
    val restaurantName: String,
    val rating: Float,
    val reviewCount: Int,
    val topReview: String,
    val priceLevel: Int,        // 1 = $, 2 = $$, 3 = $$$
    val openingHours: String,
    val cuisine: String,
    val response: String,
    val placeId: String,
    val imageBitmap: Bitmap?,
    val sessionId: String
) : AppUiState()

data class NearbyResult(
    val alternatives: List<PlaceAlternative>,
    val response: String
) : AppUiState()
```

---

#### Screen 6 — HistoryScreen (Phase 4)
**File:** `ui/screens/HistoryScreen.kt`  
**Purpose:** List of all past landmark interactions for this tourist.

**Components:**
```
┌─────────────────────────────────┐
│  [Back]   "My Discoveries"      │
│                                 │
│  ┌───────────────────────────┐  │
│  │ [thumb] Stadthuys         │  │  ← session card
│  │         29 Apr · 10:30am  │  │
│  │         "What is this..." │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ [thumb] A Famosa          │  │
│  │         29 Apr · 11:15am  │  │
│  │         "Tell me about..." │ │
│  └───────────────────────────┘  │
│                                 │
│  (scrollable list)              │
└─────────────────────────────────┘
```

**Behaviour:**
- Loaded from Room DB (offline-first)
- Synced from Firestore on app open
- Tap a session card → navigate to ResultScreen with saved data (read-only)
- Empty state: "No discoveries yet. Go explore Malacca!"

---

### 9.3 ViewModel — State Management

**File:** `viewmodel/MainViewModel.kt`  
All screen state lives in one ViewModel to avoid prop-drilling across screens.

```kotlin
// App mode enum
enum class AppMode { LANDMARK, RESTAURANT }

// UI state sealed class
sealed class AppUiState {
    object Idle : AppUiState()
    object Listening : AppUiState()
    object Loading : AppUiState()
    data class LandmarkResult(
        val landmarkName: String,
        val response: String,
        val confidence: String,
        val imageBitmap: Bitmap?,
        val sessionId: String
    ) : AppUiState()
    data class RestaurantResult(
        val restaurantName: String,
        val rating: Float,
        val reviewCount: Int,
        val topReview: String,
        val priceLevel: Int,
        val openingHours: String,
        val cuisine: String,
        val response: String,
        val placeId: String,
        val imageBitmap: Bitmap?,
        val sessionId: String
    ) : AppUiState()
    data class NearbyResult(
        val alternatives: List<PlaceAlternative>,
        val response: String
    ) : AppUiState()
    data class Error(val message: String) : AppUiState()
}

// ViewModel exposes:
val uiState: StateFlow<AppUiState>
val bleConnected: StateFlow<Boolean>
val appMode: StateFlow<AppMode>               // current mode toggle state
val sessions: StateFlow<List<SessionEntity>>  // for HistoryScreen

// Key functions:
fun startListening()                          // triggers STT + camera
fun stopListening()                           // manual stop
fun setMode(mode: AppMode)                    // switch landmark / restaurant mode
fun analyzeImage(bitmap: Bitmap, query: String)           // calls POST /analyze
fun analyzeRestaurant(bitmap: Bitmap, lat: Double, lng: Double)  // calls POST /restaurant
fun findNearbyRestaurants(lat: Double, lng: Double, excludePlaceId: String)  // calls POST /restaurant/nearby
fun replayTts()                               // re-speaks last response
fun loadHistory()                             // loads Room DB sessions
```

---

### 9.4 App Theme & Colors

**File:** `ui/theme/Color.kt`

```kotlin
// Primary palette — inspired by Malacca heritage colours
val MalaccaRed    = Color(0xFFB22222)   // Dutch colonial red (Stadthuys)
val MalaccaTeal   = Color(0xFF0D6E6E)   // Peranakan tile teal
val MalaccaGold   = Color(0xFFD4A017)   // Sultanic gold accent
val BackgroundDark = Color(0xFF121212)  // Dark background (easy outdoors)
val SurfaceDark   = Color(0xFF1E1E1E)
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val SuccessGreen  = Color(0xFF4CAF50)
val WarningAmber  = Color(0xFFFFC107)
val ErrorRed      = Color(0xFFE53935)
```

**Design decisions:**
- Dark theme by default — easier to read outdoors in Malacca sun
- Large tap targets (minimum 56dp) — tourists walking and tapping
- High contrast text — readability in bright outdoor light
- Font: use default Material3 Roboto (no custom font needed for MVP)

---

### 9.5 Navigation Setup

**File:** `ui/navigation/NavGraph.kt`

```kotlin
// Routes
const val ROUTE_SPLASH   = "splash"
const val ROUTE_HOME     = "home"
const val ROUTE_LISTENING = "listening"
const val ROUTE_LOADING  = "loading"
const val ROUTE_RESULT   = "result"
const val ROUTE_HISTORY  = "history"

// NavGraph composable connects all screens
// Single NavController passed from MainActivity
// ViewModel shared across all screens via viewModel()
```

---

### 9.6 Error States Reference

Every screen must handle these gracefully (never crash):

| Error | Screen | What tourist sees |
|---|---|---|
| No internet | HomeScreen | ErrorBanner at bottom: "No internet. Check your connection." |
| Mic permission denied | HomeScreen | ErrorBanner: "Microphone permission needed. Go to Settings." |
| Camera permission denied | HomeScreen | ErrorBanner: "Camera permission needed. Go to Settings." |
| STT heard nothing | ListeningScreen | Toast: "I didn't catch that. Try again." → back to Home |
| Backend timeout (>10s) | LoadingScreen | Auto-navigate Home: "Connection timeout. Try again." |
| Low confidence landmark | ResultScreen | Amber badge + "Try a clearer angle next time" |
| Unknown landmark | ResultScreen | Red badge + "I couldn't identify this landmark." |
| BLE disconnected | HomeScreen | Bottom indicator updates to "Glasses disconnected" |
| Backend 500 error | LoadingScreen | Navigate Home: "Something went wrong. Try again." |
| Restaurant not found in Places | ResultScreen | ErrorBanner: "Couldn't find this restaurant online. Try a closer shot of the signage." |
| Location permission denied | HomeScreen (restaurant mode) | ErrorBanner: "Location access needed to find nearby restaurants." |
| No nearby results | ResultScreen | TTS: "I couldn't find any open restaurants nearby right now." |

---

## 10. BLE Communication (HeyCyan SDK — Phase 3 only)

### Connection flow
```
1. App scans for BLE devices (QCCentralManager)
2. User selects glasses from list
3. App connects (QCCentralManager.connect)
4. App triggers photo: setDeviceMode(Photo)
5. SDK callback fires with photo data
6. App sends photo bytes to /analyze
7. Backend returns text response
8. TtsManager speaks response through phone speaker
```

### Key SDK classes (Android)
```kotlin
QCCentralManager      // BLE scan + connect
QCSDKManager          // Singleton, receives callbacks
QCSDKManagerDelegate  // Interface: onPhotoReceived, onBatteryUpdate, etc.
QCSDKCmdCreator       // Send commands: photo, video, audio mode
```

### Required Android permissions (`AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 11. Build Order (Step-by-Step)

Follow this order strictly. Do not move to the next phase until ALL success checks
in the current phase pass.

---

### Phase 1 — Backend Foundation
**Tool:** VS Code  
**Glasses needed:** No  
**Goal:** Working `/analyze` endpoint that correctly identifies a Malacca landmark

| Step | Task | Success Check |
|---|---|---|
| 1.1 | Set up Python venv, install dependencies | `pip install` runs without error |
| 1.2 | Create `main.py` with FastAPI skeleton | `GET /health` returns `{"status":"ok"}` |
| 1.3 | Create `services/vision.py` with Gemini 2.5 Flash | Returns text for a test image |
| 1.4 | Add system prompt for Malacca tourist context | Response mentions correct landmark name |
| 1.5 | Create `services/search.py` for web search | Returns ticket prices when asked |
| 1.6 | Wire search into vision (Gemini tool use) | Single `/analyze` call handles vision + search |
| 1.7 | Create `models/schemas.py` Pydantic models | Request/response validated correctly |
| 1.8 | Test `/analyze` via FastAPI `/docs` UI | Upload Stadthuys photo → correct response |
| 1.9 | Add error handling + fallback messages | Bad image returns graceful error JSON |
| ✅ | **Phase 1 complete when:** | All 9 steps pass. Backend runs locally on port 8000. |

---

### Phase 2 — Android App with Phone Camera
**Tool:** Android Studio  
**Glasses needed:** No  
**Goal:** Full tourist flow working on phone — voice in, AI response, spoken back

#### 2A — Project Setup & API Connection
| Step | Task | Success Check |
|---|---|---|
| 2.1 | Create Android project (Kotlin, Compose, API 26) | App builds and runs on physical device |
| 2.2 | Add all dependencies to `build.gradle` | Gradle sync succeeds, no errors |
| 2.3 | Create `ApiClient.kt` + `ApiService.kt` | `GET /health` returns 200 from device on same WiFi |
| 2.4 | Create `ApiModels.kt` data classes | Request/response models match backend schema |

#### 2B — Navigation & Screens (UI Shell)
| Step | Task | Success Check |
|---|---|---|
| 2.5 | Set up `NavGraph.kt` with all 5 routes | Can navigate between screens manually |
| 2.6 | Build `SplashScreen.kt` | Logo shows, auto-navigates to Home after 2s |
| 2.7 | Build `HomeScreen.kt` shell | Mic button visible, BLE status shown at bottom |
| 2.8 | Build `ListeningScreen.kt` shell | Waveform animation plays |
| 2.9 | Build `LoadingScreen.kt` shell | Spinner + rotating messages animate correctly |
| 2.10 | Build `ResultScreen.kt` shell | Landmark name + response text + buttons render |
| 2.11 | Apply theme colours in `Color.kt` + `Theme.kt` | Dark theme, Malacca colours applied to all screens |

#### 2C — Camera + API Call
| Step | Task | Success Check |
|---|---|---|
| 2.12 | Create `CameraManager.kt` with CameraX | Tapping button takes a photo on the device |
| 2.13 | Wire camera photo → `POST /analyze` | Response JSON received in Android app |
| 2.14 | Display response on ResultScreen | Landmark name + AI text shows correctly |
| 2.15 | Show captured image thumbnail on ResultScreen | Image preview visible in LandmarkCard |

#### 2D — Voice Input (STT)
| Step | Task | Success Check |
|---|---|---|
| 2.16 | Create `SttManager.kt` with Android SpeechRecognizer | Voice transcribed to text on device |
| 2.17 | Wire STT result → query field in `/analyze` call | Spoken question sent as query to backend |
| 2.18 | Navigate Listening → Loading on speech end | Automatic transition when tourist stops speaking |
| 2.19 | Handle empty STT result gracefully | Toast shown, navigate back to Home |

#### 2E — Voice Output (TTS)
| Step | Task | Success Check |
|---|---|---|
| 2.20 | Create `TtsManager.kt` with Android TextToSpeech | Response text spoken aloud on arrival at ResultScreen |
| 2.21 | Wire "Replay" button to re-trigger TTS | Tapping Replay speaks response again |
| 2.22 | Stop TTS when navigating away from ResultScreen | No audio leak when going back to Home |

#### 2F — ViewModel & State
| Step | Task | Success Check |
|---|---|---|
| 2.23 | Create `MainViewModel.kt` with `AppUiState` sealed class | State transitions: Idle → Listening → Loading → Result |
| 2.24 | Wire all error states to `ErrorBanner` component | Each error shows correct message on correct screen |
| 2.25 | Add timeout handling (10s backend timeout) | App navigates back to Home on timeout |

#### 2H — Restaurant & Cafe Discovery (F06, F07)
| Step | Task | Success Check |
|---|---|---|
| 2.29 | Add `ACCESS_FINE_LOCATION` runtime permission request to Android | Device prompts user for location on first launch |
| 2.30 | Add GPS coordinate fetch to `MainViewModel` | `lat`/`lng` values available before restaurant call |
| 2.31 | Add `RestaurantRequest` + `RestaurantResponse` + `PlaceAlternative` to `ApiModels.kt` | Data classes match `POST /restaurant` and `POST /restaurant/nearby` schemas |
| 2.32 | Add `/restaurant` and `/restaurant/nearby` endpoints to `ApiService.kt` | Retrofit definitions compile without error |
| 2.33 | Add mode toggle (Landmark / Restaurant) to `HomeScreen.kt` | Toggling changes `appMode` state in ViewModel |
| 2.34 | Wire restaurant mode → `POST /restaurant` call with GPS | Restaurant name, rating, hours returned and shown on ResultScreen |
| 2.35 | Build restaurant variant of `ResultScreen.kt` | Rating, price level, opening hours displayed correctly |
| 2.36 | Wire "Find Nearby" button → `POST /restaurant/nearby` | 3 alternative restaurants listed and spoken via TTS |
| 2.37 | Test restaurant flow end-to-end on Jonker Street photo | Correct restaurant identified, details accurate, TTS speaks response |
| 2.38 | Test "Find Nearby" with a known location | 3 alternatives returned within ~200m, spoken aloud |

#### 2G — Full End-to-End Test (Phone Camera)
| Step | Task | Success Check |
|---|---|---|
| 2.26 | Full flow test: voice → camera → AI → speak | Complete tourist experience works on phone |
| 2.27 | Test with 3 different Malacca landmark photos | All 3 identified correctly |
| 2.28 | Test all error states manually | Every error shows correct banner, no crashes |
| ✅ | **Phase 2 complete when:** | Tourist can use the full app with phone camera. Zero crashes. |

---

### Phase 3 — HeyCyan BLE Glasses Integration
**Tool:** Android Studio  
**Glasses needed:** Yes (physical HeyCyan glasses required)  
**Goal:** Replace phone camera with glasses camera. Everything else stays the same.

| Step | Task | Success Check |
|---|---|---|
| 3.1 | Copy `QCBleSdk.aar` into `app/libs/` | File exists in correct folder |
| 3.2 | Add AAR dependency in `build.gradle` | Project builds without AAR errors |
| 3.3 | Add all BLE permissions to `AndroidManifest.xml` | No permission-related build errors |
| 3.4 | Create `GlassesManager.kt` — BLE scan | Nearby glasses appear in a list |
| 3.5 | Implement connect + disconnect logic | App connects to glasses, HomeScreen shows "Glasses connected ✓" |
| 3.6 | Implement `QCSDKManagerDelegate` photo callback | Callback fires when glasses shutter pressed |
| 3.7 | Replace `CameraManager` with glasses photo bytes | Photo from glasses sent to `/analyze` |
| 3.8 | Test full flow with glasses | Tourist speaks → glasses capture → AI response spoken back |
| 3.9 | Test BLE drop recovery | Glasses disconnect → HomeScreen shows "Glasses disconnected" gracefully |
| ✅ | **Phase 3 complete when:** | Full flow works with glasses. App handles BLE drops without crashing. |

---

### Phase 4 — Database + Session History
**Tool:** VS Code + Android Studio  
**Glasses needed:** Optional  
**Goal:** Every interaction saved, tourist can review past discoveries

| Step | Task | Success Check |
|---|---|---|
| 4.1 | Set up Firebase project + Firestore + Cloud Storage | Firebase console shows project active |
| 4.2 | Add `services/database.py` — Firestore session save | Session document appears in Firestore console |
| 4.3 | Add image upload to Cloud Storage | Image URL stored in session document |
| 4.4 | Add `GET /sessions/{tourist_id}` endpoint | Returns correct session list as JSON |
| 4.5 | Add Firebase Auth (anonymous) to Android | Each install gets unique anonymous UID |
| 4.6 | Create `SessionEntity.kt` + `SessionDao.kt` (Room) | Sessions persist after app restart |
| 4.7 | Create `SessionRepository.kt` — syncs Firestore → Room | Local DB updated on app open |
| 4.8 | Wire session save into result flow | After ResultScreen loads, session auto-saved |
| 4.9 | Build `HistoryScreen.kt` with session list | Past sessions visible, scrollable |
| 4.10 | Tap session → ResultScreen (read-only) | Can review past landmarks |
| ✅ | **Phase 4 complete when:** | Sessions save correctly. HistoryScreen shows all past discoveries. |

---

### Phase 5 — Polish + Nice-to-Have
| Step | Feature | Notes |
|---|---|---|
| 5.1 | Upgrade STT to Gemini Live API (WebSocket) | Replace Android SpeechRecognizer — lower latency |
| 5.2 | Upgrade TTS to Gemini TTS API | Replace Android TextToSpeech — more natural voice |
| 5.3 | Multilingual response | Pass `language` param; detect from device locale |
| 5.4 | Offline landmark cache | Pre-load top 20 landmarks into Room DB |
| 5.5 | Map integration | Show Google Maps pin after landmark identified |
| 5.6 | Feedback (thumbs up/down) | Save rating to Firestore session document |
| 5.7 | Cloud Run deployment | Dockerize backend, deploy to GCP, update Android URL |

---

## 12. Key Rules for Claude Code Sessions

When starting a new Claude Code session, always begin with:

> "Read `prd.md` and `sdd.md` first.  
> We are on **Phase X, Step Y**.  
> Current task: [describe task]  
> Current error (if any): [paste full error message]"

**Never skip a phase's success checks before moving to the next phase.**  
**Never add glasses BLE code before Phase 2 is fully complete.**  
**Never deploy to Cloud Run before Phase 4 is complete.**
