# twinmindx

`twinmindx` is an Android note-capture app that records meetings/voice notes, transcribes them, and generates smart summaries in a clean Jetpack Compose experience.

## Demo

- [Watch the app demo](https://drive.google.com/file/d/1QJVzvZvajwTBJ3RluAiVYQKQekiSZauR/view?usp=sharing)

## Highlights

- One-tap voice capture from the dashboard
- Live meeting timeline and recording states
- Transcript view for captured audio
- AI-powered summary generation
- Local persistence with Room + background processing

## Tech Stack

- Kotlin + Jetpack Compose
- MVVM + Hilt DI
- Room + WorkManager
- Retrofit/OkHttp + OpenAI API integration

## Quick Start

1. Clone the repo and open in Android Studio.
2. Add your API key in `local.properties`:

```properties
OPENAI_API_KEY=your_api_key_here
```

3. Run the app on an Android device/emulator (minSdk 24).

## Tiny Easter Egg 👀

On the dashboard, somewhere on top there’s a small easter egg there.
