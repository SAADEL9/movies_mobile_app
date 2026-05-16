# Movies Mobile App - Cinematic Experience with AI Avatar

Welcome to the **Movies Mobile App**, a premium Android application designed for movie enthusiasts who want a cinematic experience at their fingertips. This app goes beyond simple movie browsing by integrating a state-of-the-art **3D AI Avatar (CineBot)** that you can talk to!

## 🚀 Key Features

### 🎬 Movie Discovery & Details
- **Dynamic Browsing**: Explore "Now Playing", "Top Rated" movies, and "Top Rated" series.
- **Category Filtering**: Quickly filter movies by genres like Action, Romance, Comedy, Horror, Sci-Fi, and Animation.
- **Deep Search**: Find any movie or series using the real-time search engine powered by the TMDB API.
- **Rich Content**: View high-quality posters, backdrops, ratings, and detailed overviews for every title.

### 🤖 CineBot - Your AI Movie Assistant
- **3D Animated Avatar**: Interact with a fully animated 3D avatar that waves and talks to you, built using **Google Filament** and **SceneView**.
- **Voice Interaction**: Use your voice to ask questions about movies. The app features integrated **Speech Recognition** and **Text-to-Speech (TTS)**.
- **Masculine Voice Profile**: Optimized with a deep, masculine voice for a premium persona.
- **Dual AI Core**: Powered by **Ollama** (Llama 3.2) for local processing, with an automatic fallback to **OpenRouter** for cloud-based intelligence.

### 👤 User Experience & Backend
- **Secure Authentication**: User registration and login powered by **Supabase Auth**.
- **Personalized Watchlist**: Save your favorite movies to a cloud-synced watchlist.
- **Profile Management**: Customize your profile with a display name and avatar storage.
- **Cinematic UI**: A stunning dark-mode interface with glassmorphism effects, smooth transitions, and a gold-accented premium color palette.

## 🛠️ Technology Stack

| Category | Tools & Libraries |
| :--- | :--- |
| **Language** | Java |
| **Framework** | Android SDK (Min API 24, Target API 36) |
| **3D Engine** | Google Filament, SceneView |
| **Networking** | Retrofit 2, OkHttp, GSON |
| **Backend / DB** | Supabase (PostgreSQL, Storage, GoTrue) |
| **AI Processing** | Ollama (Llama 3.2), OpenRouter API |
| **Data Source** | TMDB API |
| **UI / Design** | Material Design 3, Glide, ConstraintLayout |
| **Audio** | Android TTS, SpeechRecognizer |

## ⚙️ Setup & Configuration

To run this project, you need to configure the following in your `local.properties` file:

```properties
# TMDB Configuration
TMDB_API_KEY=your_tmdb_api_key
TMDB_BASE_URL=https://api.themoviedb.org/3/
TMDB_IMAGE_BASE_URL=https://image.tmdb.org/t/p/w500

# Supabase Configuration
SUPABASE_URL=your_supabase_project_url
SUPABASE_PUBLISHABLE_KEY=your_supabase_anon_key

# AI Configuration
# For physical devices, use your PC's local IP (e.g., 192.168.x.x)
OLLAMA_BASE_URL=http://your_pc_ip:11434/
OPENROUTER_API_KEY=your_openrouter_api_key
OPENROUTER_BASE_URL=https://openrouter.ai/
```

### 💡 Connecting to Ollama from a Real Phone
1. Find your PC's local IP address using `ipconfig`.
2. Update `OLLAMA_BASE_URL` in `local.properties` with this IP.
3. On your PC, set the environment variable `OLLAMA_HOST=0.0.0.0` and restart Ollama to allow external connections.

## 📜 License
This project is for educational and portfolio purposes.
