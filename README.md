*Please be aware that this application / sample is provided as-is for demonstration purposes without any guarantee of support*
=========================================================

# App Actions Demo using the Android Shortcuts framework
Demo to show how to use App Actions (Shortcuts) on Zebra Mobile Computers

Google IO 2021 announced the beta release of the [App Actions](https://developers.google.com/assistant/app/overview) using the Android [Shortcuts framework](https://developer.android.com/guide/topics/ui/shortcuts).  This is an enhancement to the existing [Google Assistant integration](https://developers.google.com/assistant/app/legacy/build-overview) that used [actions.xml](https://developers.google.com/assistant/app/legacy/action-schema).  The announcement centred around using [Built-in Intents](https://developers.google.com/assistant/app/reference/built-in-intents/bii-index), first introduced in 2020 and the new [Custom Intents](https://developers.google.com/assistant/app/custom-intents).  Things honestly got a bit confusing, especially when the official "[fitness tracker](https://github.com/actions-on-google/appactions-fitness-kotlin)" sample app had not received an update in nearly a year.

I noticed that one of the built-in Intents was "[Get Barcode](https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode)" so I thought it made sense to experiment with App Actions on a Zebra device.  I have previously written about how you can [add voice recognition to your app](https://github.com/darryncampbell/RetailAssistant) using Google DialogFlow, [using DataWedge](https://github.com/darryncampbell/DataWedge-VoiceRecognition-Sample) and upgrading to [V2 of the DialogFlow engine](https://github.com/darryncampbell/AndroidV2DialogFlow).  App Actions appear to achieve the same use cases (responding to user requests) without integrating with a backend server. 

## Setup

A new [Code Lab for App Actions](https://codelabs.developers.google.com/codelabs/appactions-beta#0) with Shortcuts was released alongside Google IO 2021 so I followed that.  There is also a [webinar](https://www.youtube.com/watch?v=PMinB3VZ9x4).  This code lab covers the basic setup & prerequisites such as uploading your app to an Internal Play Store test track and configuring the (very Alpha) [Assistant test tool for Android Studio](https://developers.google.com/assistant/app/test-tool)

I previously wrote an application to demonstrate [barcode capture on Zebra Android devices using Kotlin](https://github.com/darryncampbell/DataWedgeKotlin), which I used as a base app to add App Actions to. 

I am testing with a Zebra TC52 running Android 10, though any Zebra Android device running Lollipop (for App Actions) and DataWedge 6.4 or higher should work. 

## Built-In Intent

In order to implement [Get Barcode](https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode) into my existing app, I needed to make the following changes:

Create a `shortcuts.xml` file and define a built-in Intent capability.  I just declared an explicit Intent to be sent to my application to fulfill the capability but the mechanism is very flexible and more detail is available in the [schema](https://developers.google.com/assistant/app/action-schema)

```xml
<capability android:name="actions.intent.GET_BARCODE">
    <intent
        android:action="android.intent.action.VIEW"
        android:targetPackage="com.darryncampbell.appactionsdemo"
            android:targetClass="com.darryncampbell.appactionsdemo.MainActivity">
    </intent>
</capability>
```

I only defined a single shortcut that would invoke the 'GET BARCODE' capability.  These are the kind of application capabilities that your app would surface to the Google assistant so it can in turn surface these to your end users:

```xml
<shortcut
    android:shortcutId="PRICE_LOOKUP"
    android:shortcutShortLabel="@string/pluShort">
    <capability-binding android:key="actions.intent.GET_BARCODE">
    </capability-binding>
</shortcut>
```
You also need to point to your `shortcuts.xml` file from your Android manifest

```xml
<meta-data
    android:name="android.app.shortcuts"
    android:resource="@xml/shortcuts" />
```

token is just something all responses have
```kotlin
fun handleIntent(intent: Intent)
{
    ...
    else if (intent.hasExtra("actions.fulfillment.extra.ACTION_TOKEN"))
    {
        //  GET_BARCODE Intent entry point (App Actions)
        dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN, "START_SCANNING")
    }
}
```


```kotlin
private fun createDataWedgeProfile() {
    val appConfig = Bundle()
    appConfig.putString("PACKAGE_NAME", packageName)      //  Associate the profile with this app
    appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
    val appConfigAssistant = Bundle()
    appConfigAssistant.putString("PACKAGE_NAME", "com.google.android.googlequicksearchbox")      //  Associate the profile with the Google assistant
    appConfigAssistant.putStringArray("ACTIVITY_LIST", arrayOf("*"))
    profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig, appConfigAssistant))
}
```





[![Video Demo](https://img.youtube.com/vi/O22BUzpJzwg/0.jpg)](https://www.youtube.com/watch?v=O22BUzpJzwg)

## Custom Intent

I implemented a [Custom Intent](https://developers.google.com/assistant/app/custom-intents) to return the quantity of a scanned product at a particular store as follows:

I had a build error when adding the `app:queryPatterns` property to my capability Intent, I needed to update the `minSdkVersion` to `25` to fix that.

```xml
<capability
    android:name="custom.actions.intent.STOCK_AVAILABILITY"
    app:queryPatterns="@array/StockAvailabilityQueries">
    <intent
        android:action="android.intent.action.VIEW"
        android:targetPackage="com.darryncampbell.appactionsdemo"
        android:targetClass="com.darryncampbell.appactionsdemo.MainActivity">
        <parameter
            android:name="store_location"
            android:key="store_location" />
    </intent>
</capability>
```


```xml
<shortcut
    android:shortcutId="STOCK_AVAILABILITY"
    android:shortcutShortLabel="@string/stockAvailabilityShort">
    <capability-binding android:key="custom.actions.intent.STOCK_AVAILABILITY">
        <parameter-binding
            android:key="store_location"
            android:value="@string/storeLocation"/>
    </capability-binding>
</shortcut>
```

New entries in `Strings.xml`

```xml
<string name="stockAvailabilityShort">Stock Availability</string>
<string name="pluShort">Price Lookup</string>
<string name="storeLocation">Store Location</string>

<string-array name="StockAvailabilityQueries">
    <item>Lookup stock in $text1</item>
    <item>Find this item at $text1</item>
    <item>Stock check for $text1</item>
    <item>Is this item in stock at $text1</item>
    <item>Find this item at $text1</item>
</string-array>
```

comes through in onCreate.  Will perform a lookup on the next scan
```kotlin
fun handleIntent(intent: Intent)
{
    ...
    else if (intent.hasExtra("store_location"))
    {
        //  STOCK_AVAILABILITY Intent entry point (App Actions)
        storeLocation = intent.getStringExtra("store_location").toString()
        Log.d(LOG_TAG, "Looking up stock availability in $storeLocation")
        stockAvailabilityCheck = true;
        dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN, "START_SCANNING")
    }
}
```

```kotlin
fun handleIntent(intent: Intent)
{
    ...
    else if (intent.hasExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING)) {
        if (stockAvailabilityCheck)
        {
            //  For demo purposes only(!)
            val date = Calendar.getInstance().time
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
            val dateTimeString = df.format(date)
            val availabilityLookup = Scan(storeLocation + " stock: " + Random().nextInt(10), "Stock Availability", dateTimeString)
            scans.add(0, availabilityLookup)
            stockAvailabilityCheck = false;
        }
    }
}
```




[![Video Demo](https://img.youtube.com/vi/ICJdzGESAvE/0.jpg)](https://www.youtube.com/watch?v=ICJdzGESAvE)

## Conclusions
Assistant only
In-app promotion (Assistant Settings screenshot) https://developers.google.com/assistant/app/in-app-promo-sdk

## ToDo:
- Get application working with Custom Intents (Removed Shortcuts tags and re-added the parameter mime type to the capability - re-uploaded to the Play Store.  I don't know why onNewIntent() is no longer being called.  It's coming through in onCreate)
- Video of app working with custom Intents
- Document BII (including code changes and YouTube video)
- Document custom Intents

## Links:
- ~~https://developers.google.com/assistant/app/overview~~
- ~~https://github.com/actions-on-google/appactions-fitness-kotlin~~
- ~~https://developers.google.com/assistant/app/reference/built-in-intents/bii-index~~
- ~~https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode~~
- ~~https://developers.google.com/assistant/app/legacy/action-schema~~
- ~~https://developers.google.com/assistant/app/test-tool~~


- ~~https://developers.google.com/assistant/app/custom-intents~~
- https://developers.google.com/assistant/app/in-app-promo-sdk
- ~~https://codelabs.developers.google.com/codelabs/appactions-beta#0~~
- ~~https://www.youtube.com/watch?v=PMinB3VZ9x4~~

- Approval process: https://developers.google.com/assistant/app/get-started#request-review

Other apps:
- ~~https://github.com/darryncampbell/DataWedge-VoiceRecognition-Sample~~
- https://developer.zebra.com/blog/giving-context-speech-datawedge-and-voice-input
- ~~https://github.com/darryncampbell/RetailAssistant~~
- https://developer.zebra.com/blog/using-dialogflow-zebra-mobile-devices
- ~~https://github.com/darryncampbell/AndroidV2DialogFlow~~

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

YouTube videos: https://youtu.be/O22BUzpJzwg, https://youtu.be/ICJdzGESAvE
