# MoviesSaad - 2026 Movie Assistant App

MoviesSaad is a next-generation Android application designed to provide a personalized and interactive movie discovery experience. Combining advanced AI, 3D visualization, and real-time movie data, it offers users a unique way to explore the world of cinema.

## 🚀 Key Features

### 1. 🛡️ Smart Age Scanning (Onboarding)
- **AI-Powered Entry:** Before entering the app, users are scanned via the front camera.
- **TFLite Face Analysis:** Uses a TensorFlow Lite model (version 2.16.1) to detect whether the user is a **Kid** or an **Adult**.
- **Dynamic Content Filtering:** The app automatically adjusts its movie recommendations based on the detected age group (e.g., animation and family movies for kids).

### 2. 🤖 CineBot: 3D AI Assistant
- **Interactive 3D Avatar:** Powered by **SceneView (Filament)**, a 3D avatar (CineBot) interacts with users through animations (waving, talking, listening).
- **Voice & Chat Interaction:** Users can chat with CineBot via text or voice (Speech-to-Text). The bot responds with localized Text-to-Speech (TTS) using a deep male voice.
- **Intelligent Movie Knowledge:** CineBot uses **Ollama** (local LLM) with a fallback to **OpenRouter** to answer questions about specific movies, genres, or trivia.

### 3. 🎬 Comprehensive Movie Exploration
- **TMDB Integration:** Fetches real-time movie data, including now playing, top-rated, and genre-specific titles from The Movie Database (TMDB).
- **Detailed Insights:** View movie overviews, ratings, release dates, and full cast lists.
- **Multimedia Experience:** Watch trailers directly via YouTube integration.

### 4. 📍 Cinema Discovery
- **OpenStreetMap (OSMdroid):** View nearby cinemas on an interactive map.
- **Location Awareness:** Uses GPS to center the map on the user's current location and highlights local theaters.

### 5. 👤 Personalized Profiles & Watchlists
- **Supabase Backend:** Secure authentication (Login/Register) and real-time database management via Supabase.
- **Customizable Profiles:** Users can set their bio and upload custom avatars (stored in Supabase Storage).
- **Personalized Lists:**
    - **Watchlist:** Save movies you want to see later.
    - **Watched History:** Mark movies as seen.
    - **User Ratings:** Give movies your own star ratings (1-5 stars) and sync them to the cloud.

## 🛠️ Technical Stack

- **Platform:** Android (Java & Kotlin)
- **Backend & Database:** Supabase (Auth, DB, Storage)
- **AI & ML:** TensorFlow Lite, ML Kit, Ollama/OpenRouter
- **3D Graphics:** SceneView (Avatar rendering)
- **Movie Data:** TMDB API (The Movie Database)
- **Images:** Glide (Posters & Avatars)
- **Maps:** OSMdroid (Cinema Discovery)
- **Architecture:** MVVM with Service-based data layers

## 📦 Recent Updates
- **Stability Fix:** Optimized Age Scanner dependencies by using TFLite 2.16.1 to resolve class duplication conflicts while maintaining support for required model operations.
- **Performance Fixes:** Implemented a grace period for face scanning to prevent flickering and improved consecutive frame detection logic.

---
*Created as a prototype for advanced AI integration in mobile applications.*
