*Please be aware that this application / sample is provided as-is for demonstration purposes without any guarantee of support*
=========================================================

# App-Actions-Demo
Demo to show how to use App Actions on Zebra Mobile Computers

## ToDo:
- Get application working with Custom Intents (Removed Shortcuts tags and re-added the parameter mime type to the capability - re-uploaded to the Play Store.  I don't know why onNewIntent() is no longer being called.  It's coming through in onCreate)
- Video of app working with custom Intents
- Document BII (including code changes and YouTube video)
- Document custom Intents

## Links:
- https://developers.google.com/assistant/app/overview
- https://github.com/actions-on-google/appactions-fitness-kotlin
- https://developers.google.com/assistant/app/reference/built-in-intents
- https://developers.google.com/assistant/app/reference/built-in-intents/bii-index
- https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode
- https://developers.google.com/assistant/app/legacy/action-schema
- https://developers.google.com/assistant/app/test-tool


- https://developers.google.com/assistant/app/custom-intents#actions.xml
- https://developers.google.com/assistant/app/in-app-promo-sdk
- https://codelabs.developers.google.com/codelabs/appactions-beta#0
- https://www.youtube.com/watch?v=PMinB3VZ9x4

- Approval process: https://developers.google.com/assistant/app/get-started#request-review

Other apps:
- https://github.com/darryncampbell/DataWedge-VoiceRecognition-Sample
- https://developer.zebra.com/blog/giving-context-speech-datawedge-and-voice-input
- https://github.com/darryncampbell/RetailAssistant
- https://developer.zebra.com/blog/using-dialogflow-zebra-mobile-devices
- https://github.com/darryncampbell/AndroidV2DialogFlow

Notes:
- Actions.xml is for production apps (current handler for Google Assistant), shortcuts.xml is for the new App Shortcuts feature and is currently beta.
- Install assistant (not required)
- Put app in Play Store
- Signed into my Google account (same as Play Store account)
- WMT Demo during Google IO
- When uploading to the Play Store to roll out, needed to define a privacy policy and accept the Google Actions terms & conditions under the advanced settings.
- Will this work in Android Enterprise?  
- Can't test this without a review (https://developers.google.com/assistant/app/get-started#request-review)
- LP or higher
- Suggested voice commands are given under under Assistant Settings (get on this list with in-app-promo-sdk)
- Needed sdk 25 for custom Intents (queryPackages was not recognised with sdk 21)
- shortcut logic in shortcuts.xml - final shortcut is currently commented out.