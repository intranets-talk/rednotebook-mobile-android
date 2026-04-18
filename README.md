# A RedNotebook mobile companion client for Android <img src="https://github.com/intranets-talk/rednotebook-mobile-android/blob/main/logo.png" width="30">

## What this is

A simple Android/Kotlin companion app for the [RedNotebook](https://github.com/jendrikseipp/rednotebook) cross-platform journal app.

The Android app it only handles text journal entries (no images or file attachments).

<img src="https://github.com/intranets-talk/rednotebook-mobile-android/blob/main/screenshot.png" width="500">

## How and Why

Made vibe coding using Claude AI.

I've been using the RedNotebook app on my Linux desktop for years, and that works perfect for me. I am not a programmer, but I needed to update my journal on the go. A mobile app is currently not available, so Claude AI came to the rescue.

## How this works

- There a two components:

  1. The RedNotebook API service; This is a pre-requisite, you can get that from my [RedNotebook FastAPI](https://github.com/intranets-talk/RedNotebook-FastAPI-backend) repo. Based on FastAPI/Python, it reads and writes RedNotebook yyyy-mm.txt files, providing the API endpoints for the Android app. More info on the repo. I have mine running on a Orange Pi with Armbian, as a systemd service.

  2. This Android app - RedNotebook Mobile.

- The Android app or the API endpoints do not feature authentication. Exposing the API publicly to the Internet will make your journal entries available to anyone. Not a good idea.
- Within the Setting section, the app expects a local LAN IP address to reach the API URL for your journal entries, which it should be on the same local network range (ex: 192.x.x.x) as your Android phone.
- While outside of your local Network/WiFi, you can use the app, and it will sync back changes to your RedNotebook desktop app when the API URL is reachable again.

## Features

The app is fairly basic. Unlike RedNotebook for desktop - it does not handle images or attachments; only text journal entries.

- Calendar view with swipe navigation, year picker, Today FAB
- Entries list view with month navigation and Add/Today FABs  
- Search tab with offline support
- Settings tab with API URL configuration and dark/light theme toggle
- Offline-first architecture with Room local DB and WorkManager sync
- Syncing back all changes to your RedNotebook desktop app - when online again.
- Full initial sync on first connection

## Troubleshooting

- The app syncs all journal entries locally when first connecting to the the [RedNotebook FastAPI](https://github.com/intranets-talk/RedNotebook-FastAPI-backend) backend; if you have a lot of entries, it may take a minute.
- Only tested on a single Android device (TCL NxtPaper).
- Double check the IP address for the [RedNotebook FastAPI](https://github.com/intranets-talk/RedNotebook-FastAPI-backend) backend, entered in Settings. The API backend service runs on port 8000 by default, so this should be something like `http://192.168.1.10:8000`. The Settings screen will confirm if the IP is reachable or not.
- You may need to allow installing apps from unknown sources on your Android phone.
- The app is not optimised for landscape usage.

## Thanks to

- [Jendrik Seipp](https://github.com/jendrikseipp) - creator of RedNotebook and contributors.

## Contributing

- Can the app be improved? Of course, in many ways!
- Ex: improve UI, add image attachments, add markdown, better navigation, add authentication, optimise for usage in landscape.
- Feel free to submit a pull request for any improvements.
