HeyCyan — AI Tourist Guide for Malacca
HeyCyan is an AI-powered tourist guide app designed for visitors exploring Malacca (Melaka), Malaysia. It uses computer vision and voice interaction to identify landmarks and restaurants in real time, delivering spoken descriptions directly to the user.

Features
Landmark Identification
Point your camera at any historical site and HeyCyan will identify it — distinguishing visually similar landmarks such as A Famosa (Porta de Santiago) vs. St. Paul's Church — and narrate a concise, tourist-friendly description including history, who built it, and a surprising fact. Follow-up questions are supported in context without re-identifying the landmark.

Restaurant Mode
Capture a restaurant signage photo and the app extracts the name via AI, then queries Google Places to retrieve real-time data: star rating, review count, price level, cuisine type, and opening hours. A top review snippet is read aloud. A "Find Nearby" button surfaces the top 3 currently-open restaurants within 200 metres, ranked by rating.

Multilingual
Full support for English, Bahasa Melayu, and Mandarin Chinese — UI labels, spoken responses, and TTS output all adapt to the selected language.

HeyCyan Smart Glasses
Optionally pairs with HeyCyan BLE smart glasses. When connected, photos are captured from the glasses rather than the phone camera, allowing hands-free exploration.

Tech Stack
Layer	Technology
Backend	Python, FastAPI, Google Gemini 2.5 Flash, Google Places API v1
Android	Kotlin, Jetpack Compose, Retrofit, CameraX
AI	Gemini 2.5 Flash (vision + Google Search grounding)
Connectivity	Bluetooth LE (glasses), Tailscale (backend networking)
