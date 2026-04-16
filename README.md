# A RedNotebook mobile companion client for Android.

## What this is

A simple Android/Kotlin companion app for the [RedNotebook](https://github.com/jendrikseipp/rednotebook) desktop journal app.

It only handles journal text entries (no images or attachments).

<img src="https://github.com/intranets-talk/rednotebook-mobile-android/blob/main/sreenshot.png" width="200">

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

- View journal entries on a calendar or on a list;
- Edit, delete and add new journal entries;
- Search journal entries;
- Date in header screens can be used to jump through the years;
- Dark and light themes available in Settings;
- Works offline, syncing back all changes when online again.

## Troubleshooting

- If you have added the IP address in the Settings for the API endpoint - and no journal entries are showing, restarting the app may help.
- The app syncs all journal entries locally when first connecting; if you have a lot of entries, it may take a minute or two.
- I've only tested it on a single phone model (TCL NxtPaper).
- Double check the IP address for the [RedNotebook FastAPI](https://github.com/intranets-talk/RedNotebook-FastAPI-backend) backend, entered in Settings. The API backend service runs on port 8000 by default, so this should be something like `http:192.168.1.10:8000`. The Settings screen will confirm if the IP is reachable or not.
- You may need to allow installing apps from unknown sources on your Android phone.
- The app is not optimised for landscape usage.

## Contributing

- Can the app be improved? Of course, in many ways!
- Ex: improve UI, add image attachments, add markdown, better navigation, add authentication, optimise for usage in landscape.
- Feel free to submit a pull request for any improvements.
