# A RedNotebook mobile companion client for Android.

## What this is

A simple Android/Kotlin companion app for the [RedNotebook](https://github.com/jendrikseipp/rednotebook) desktop journal app.

It only handles journal text entries (no images or attachments).

## How it's made

Vibe coding using Claude AI.

I've been using the RedNotebook app on my Linux desktop for years, and that works perfect for me. I am not a programmer, but I needed to update my journal on the go. A mobile app is currently not available, so Claude AI came to the rescue.

## How this works

- There a two components:

  1. The RedNotebook API service; This is a pre-requisite, you can get that from my repo [RedNotebook FastAPI](https://github.com/intranets-talk/RedNotebook-FastAPI-backend). Based on FastAPI/Python, it reads and writes RedNotebook yyyy-mm.txt files, providing the API endpoints for the Android app. More info on the repo. I have mine running on a Orange Pi with Armbian, as a systemd service.

  2. This Android app.

- The Android app or the API endpoints do not feature authentication. Exposing the API publicly to the Internet will make your journal entries available to anyone. Not a good idea.
- Within the Setting section, the app expects a local LAN IP address to reach the API URL for your journal entries, which it should be on the same local network range (ex: 192.x.x.x)
- While outside of your local Network/WiFi, you can still use the app, and it will sync back changes to your RedNotebook desktop app when the API URL is reachable again.

## Features

The app is fairly basic. It does not handle images or attachments, only text entries.

- View journal entries on a calendar or on a list;
- Edit, delete and add new journal entries;
- Search journal entries.
- Set dark/light mode for the app

## Troubleshooting

- If you have added the IP address in the Settings for the API endpoint - and no jurnal entries are showing, restarting the app may help. I've only tested it on a single phone model (TCL NextPaper).
- You may need to allow installing apps from unknown sources on your Android phone.


## Contributing

Feel free to submit a pull request for any improvements.
