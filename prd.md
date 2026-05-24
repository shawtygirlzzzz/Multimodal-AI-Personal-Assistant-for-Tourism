# Product Requirements Document (PRD)
# HeyCyan Malacca Tourist Guide App

**Version:** 1.0  
**Date:** April 2026  
**Author:** [Your Name]  
**Status:** Draft

---

## 1. Overview

### 1.1 Product Summary
A mobile Android app that pairs with HeyCyan smart glasses to act as an AI-powered
tourist guide in Malacca (Melaka), Malaysia. Tourists wear the glasses, point the
camera at a landmark, ask a question by voice, and receive a spoken response — all
hands-free.

### 1.2 Problem Statement
Tourists in Malacca often encounter historical buildings, monuments, and cultural
sites without knowing their significance. Existing solutions (brochures, tour guides,
Google Maps) require stopping, looking at a screen, or hiring a human guide. There is
no seamless, hands-free way to get instant, accurate information about what you are
looking at.

### 1.3 Solution
Use HeyCyan smart glasses as the input device (camera + microphone + speaker) paired
with an Android app that:
1. Captures an image of the landmark via the glasses camera
2. Records and transcribes the tourist's voice question
3. Sends both to an AI backend for landmark identification and information retrieval
4. Speaks the AI response back through the glasses speaker

---

## 2. Target Users

| User | Description |
|---|---|
| **Primary** | International and domestic tourists visiting Malacca |
| **Secondary** | Tour operators who want to offer tech-enhanced experiences |
| **Tertiary** | Students on educational field trips |

### 2.1 User Persona
**Name:** Amir, 34, tourist from Japan  
**Situation:** Visiting Malacca for 2 days, does not speak Malay or know local history  
**Goal:** Understand the buildings he passes without stopping to look at his phone  
**Pain point:** Google searches are slow, generic, and require screen interaction  

---

## 3. Features

### 3.1 Must Have (MVP — Phase 1, 2 & 3)

| # | Feature | Description |
|---|---|---|
| F01 | Landmark identification | Capture image via glasses, identify building using Gemini Vision |
| F02 | Voice input (STT) | Tourist speaks a question, converted to text via Gemini Live API |
| F03 | AI response | LLM generates a tourist-friendly answer with historical info |
| F04 | Voice output (TTS) | AI response is spoken back through glasses speaker |
| F05 | Web search grounding | LLM searches the web for ticket prices, opening hours, current info |
| F06 | Restaurant/cafe discovery | When tourist photographs a restaurant or cafe (Jonker Street / Bandar Melaka area), identify it via signage, fetch rating, reviews, price level, and opening hours from Google Places API. Response delivered via TTS. |
| F07 | Nearby restaurant recommendation | If tourist declines the current restaurant, suggest up to 3 alternatives within 100–200m radius using GPS + Google Places Nearby Search. Delivered via TTS. |
| F08 | BLE glasses connection | Android app connects to HeyCyan glasses via Bluetooth LE |
| F09 | Session saving | Each interaction (image + query + response) is saved to database |

### 3.2 Nice to Have (Phase 4+)

| # | Feature | Description |
|---|---|---|
| F10 | Multilingual support | Respond in tourist's preferred language (EN, ZH, JA, AR) |
| F11 | History log screen | Tourist can review past interactions in the app |
| F12 | Offline mode | Cache top 20 Malacca landmarks for use without internet |
| F13 | Map integration | Show landmark location on map after identification |
| F14 | Audio tour mode | Pre-recorded walking tour triggered by GPS location |
| F15 | Feedback system | Tourist rates the response (thumbs up/down) to improve accuracy |

### 3.3 Out of Scope (v1.0)
- iOS version
- Web dashboard
- Social sharing
- Multi-user / group tour mode

---

## 4. User Stories

### Core Flow
```
As a tourist wearing HeyCyan glasses,
I want to look at a building and ask "What is this place?",
So that I can learn about it without stopping or looking at my phone.
```

### Supporting Stories
```
As a tourist, I want the response spoken aloud through the glasses,
so that I can keep walking while listening.

As a tourist, I want to ask follow-up questions like "How much is the ticket?",
so that I can plan my visit without searching the web manually.

As a tourist, I want my session history saved,
so that I can review what I learned at the end of the day.

As a tourist, I want the app to work even on slow mobile data,
so that I can use it throughout Malacca without WiFi.
```

### Restaurant & Cafe Flow
```
As a tourist walking on Jonker Street,
I want to photograph a restaurant and ask what it serves,
So that I can decide whether to eat there without searching my phone.

As a tourist, I want to hear the restaurant's rating and reviews spoken aloud,
So that I can make a quick decision while still walking.

As a tourist, if I decide not to eat at the photographed restaurant,
I want the app to suggest up to 3 nearby alternatives,
So that I can find a good place to eat without manual searching.
```

---

## 5. Key Landmarks (Initial Test Dataset)

| Landmark | Type |
|---|---|
| Stadthuys (Red Building) | Dutch colonial building |
| A Famosa (Porta de Santiago) | Portuguese fortress |
| St. Paul's Church | Historical ruins |
| Jonker Street (Jalan Hang Jebat) | Heritage street |
| Cheng Hoon Teng Temple | Oldest Chinese temple in Malaysia |
| Kampung Morten | Traditional Malay village |
| Malacca Sultanate Palace Museum | Cultural museum |
| Christ Church Melaka | Dutch Reformed church |
| The Shore Sky Tower | Modern landmark |
| Menara Taming Sari | Revolving tower |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Latency** | Voice-to-response under 4 seconds on 4G connection |
| **Accuracy** | Correctly identifies top 20 Malacca landmarks ≥ 90% of the time |
| **Availability** | Backend uptime ≥ 99% (Cloud Run auto-scaling) |
| **Language** | Default response in English; multilingual in Phase 5 |
| **Privacy** | Images not stored permanently without user consent |
| **Offline** | App does not crash without internet; shows graceful error message |
| **Battery** | BLE connection should not drain phone battery more than 10%/hour |
| **Android** | Minimum Android API 26 (Android 8.0) |

---

## 7. Success Metrics (MVP)

| Metric | Target |
|---|---|
| Landmark identification accuracy | ≥ 90% for top 20 landmarks |
| End-to-end response time | ≤ 4 seconds |
| BLE connection stability | < 1 drop per 30-minute session |
| Session save success rate | ≥ 99% |
| Crash-free sessions | ≥ 95% |

---

## 8. Assumptions & Risks

| | Description |
|---|---|
| **Assumption** | Tourist has Android phone (API 26+) and HeyCyan glasses |
| **Assumption** | Tourist has mobile data (4G minimum) while walking in Malacca |
| **Risk** | HeyCyan SDK BLE errors may delay glasses integration |
| **Mitigation** | Build and validate backend + app with phone camera first; add glasses last |
| **Risk** | Gemini preview models may have rate limits or be deprecated |
| **Mitigation** | Use stable `gemini-2.5-flash` for vision; upgrade Live model when stable |
| **Risk** | Landmark recognition may fail at odd angles or at night |
| **Mitigation** | Add fallback prompt: "I could not identify this clearly, try a closer angle" |

---

## 9. Build Phases Summary

| Phase | Scope | Deliverable |
|---|---|---|
| **Phase 1** | Python FastAPI backend | `/analyze` endpoint working with Gemini Vision + web search |
| **Phase 2** | Android app (phone camera) | Full STT → Vision → TTS loop + restaurant/cafe discovery (F06, F07) |
| **Phase 3** | HeyCyan BLE integration | Replace phone camera with glasses camera trigger |
| **Phase 4** | Database + session history | Firestore save, history screen, error handling |
| **Phase 5** | Polish + nice-to-have | Multilingual, offline cache, map, feedback rating |
