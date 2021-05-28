*Please be aware that this application / sample is provided as-is for demonstration purposes without any guarantee of support*
=========================================================

# App Actions Demo using the Android Shortcuts framework
Demo to show how to use App Actions (Shortcuts) on Zebra Mobile Computers

Google IO 2021 announced the beta release of the [App Actions](https://developers.google.com/assistant/app/overview) using the Android [Shortcuts framework](https://developer.android.com/guide/topics/ui/shortcuts).  This is an enhancement to the existing [Google Assistant integration](https://developers.google.com/assistant/app/legacy/build-overview) that used [actions.xml](https://developers.google.com/assistant/app/legacy/action-schema).  The announcement centred around using [Built-in Intents](https://developers.google.com/assistant/app/reference/built-in-intents/bii-index), first introduced in 2020 and the new [Custom Intents](https://developers.google.com/assistant/app/custom-intents).  Things honestly got a bit confusing, especially when the official "[fitness tracker](https://github.com/actions-on-google/appactions-fitness-kotlin)" sample app had not received an update in nearly a year.

I noticed that one of the built-in Intents was "[Get Barcode](https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode)" so I thought it made sense to experiment with App Actions on a Zebra device, especially since the IO announcement included one of our major retail customers.  I have previously written about how you can [add voice recognition to your app](https://github.com/darryncampbell/RetailAssistant) using Google DialogFlow, [using DataWedge](https://github.com/darryncampbell/DataWedge-VoiceRecognition-Sample) and upgrading to [V2 of the DialogFlow engine](https://github.com/darryncampbell/AndroidV2DialogFlow).  App Actions appear to achieve the same use cases (responding to user requests) without integrating with a backend server. 

## Setup

A new [Code Lab for App Actions](https://codelabs.developers.google.com/codelabs/appactions-beta#0) with Shortcuts was released alongside Google IO 2021 so I followed that.  There is also a [webinar](https://www.youtube.com/watch?v=PMinB3VZ9x4).  This code lab covers the basic setup & prerequisites such as uploading your app to an Internal Play Store test track, signing in with the same Google account everywhere and configuring the (very Alpha) [Assistant test tool for Android Studio](https://developers.google.com/assistant/app/test-tool)

I previously wrote an application to demonstrate [barcode capture on Zebra Android devices using Kotlin](https://github.com/darryncampbell/DataWedgeKotlin), which I used as a base app to add App Actions to. 

I am testing with a Zebra TC52 running Android 10, though any Zebra Android device running Lollipop (for App Actions) and DataWedge 6.4 or higher should work.  I also installed the separate [Assistant app from the Play Store](https://play.google.com/store/apps/details?id=com.google.android.apps.googleassistant) to provide a better user experience but this was not necessary.

## Built-In Intent

Did you know, if you ask Google assistant to "Scan a Barcode" it will bring up Google Lens?

![OK Google Scan Barcode](https://raw.githubusercontent.com/darryncampbell/App-Actions-Demo/main/media/ok_google_scan_barcode_01.png)

There is obviously a better way to scan barcodes on Zebra devices and you can add additional capabilities to the Assistant by implementing one of the built-in Intents; built-in Intents use pre-defined machine learning models on the device to recognise a user's intention, for example [Get Barcode](https://developers.google.com/assistant/app/reference/built-in-intents/common/get-barcode) will be recognised when the user says something like:

- Scan to pay on Example App
- Example App scan barcode

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

When the Google assistant recognises that 'GET BARCODE' desire, an explicit Android Intent is sent to the sample application.  Because 'GET BARCODE' does not have any associated parameters there is no easy way to differentiate the Intent from other Intents (such as barcode scans).  In a production app, the recommendation is to segregate these capability handlers into separate [modules](https://developer.android.com/studio/projects/add-app-module) of your app but for this demo, I just differentiate from other Intents by the presence of the "token", which is present in all Assistants calls.

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

Another consideration for applications that use DataWedge is to be aware that the Google Assistant may now pop-up on top of your app and send it to the background.  To avoid DataWedge switching profiles (disabling the scanner & potentially introducing delays), then add the Google assistant to your associated apps when you define the DataWedge profile.  Obviously take care that there are not multiple apps all adding the Assistant to their DW profile.

```kotlin
private fun createDataWedgeProfile() {
    ...
    val appConfig = Bundle()
    appConfig.putString("PACKAGE_NAME", packageName)
    appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
    val appConfigAssistant = Bundle()
    appConfigAssistant.putString("PACKAGE_NAME", "com.google.android.googlequicksearchbox")  //  Assistant
    appConfigAssistant.putStringArray("ACTIVITY_LIST", arrayOf("*"))
    profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig, appConfigAssistant))
    ...
}
```

## Testing the Built-In Intent

The 'GET BARCODE' functionality can now be tested.  Google recommends using the Google Assistant plugin in Android Studio to simulate the Assistant invoking the capability and you can see that in the video below:

[![Video Demo](https://img.youtube.com/vi/O22BUzpJzwg/0.jpg)](https://www.youtube.com/watch?v=O22BUzpJzwg)

Ideally, I would like to show you this running a real device and respoding to a real voice activation, "OK Google, Scan a Barcode with the App Actions Demo".  On-device testing with the Assistant is not as simple and the app must first be approved according to the [Play Store Policy Asssitant review process](https://developers.google.com/assistant/app/get-started#request-review).

A few things to bear in mind:

- When you upload your application you will to define a privacy policy and accept the Google Actions terms & conditions under the advanced settings.
- In my experience, this review process is manual.  Obviously manual review processes take longer and demo apps are less likely to be approved.
- The review process for Assistant actions does not hold up the overall review process for the app. 

The way the sample app is written, a scan with the hardware trigger is indistinguishable from a scan invoked with 'GET BARCODE'

![Application](https://raw.githubusercontent.com/darryncampbell/App-Actions-Demo/main/media/device01.png)

## Custom Intent

I implemented a [Custom Intent](https://developers.google.com/assistant/app/custom-intents) to return the quantity of a scanned product at a particular store.  The use case might be that the user asks, 'Is this product available at the *London* store?' and Android will recognise that you are trying to find the product availability at a defined store (in this case, London).  Custom Intents are similar to built-in Intents with the following additions:

You need to define the queries that a user might ask.  These take the form of a String array with placeholder text.  You can define multiple queries for the same capability and the more you define, the better the Assistant will be able to determine that the user is trying to invoke your app, though there is an upper limit of 100.  The type of expected placeholder data is also included, in this case 'text'.  You can also specify 'number' or 'date' but you cannot define your own types.

```xml
<string-array name="StockAvailabilityQueries">
    <item>Lookup stock in $text1</item>
    <item>Find this item at $text1</item>
    <item>Stock check for $text1</item>
    <item>Is this item in stock at $text1</item>
    <item>Find this item at $text1</item>
</string-array>
```

Then define the custom Intent capability in your `shortcuts.xml` file and reference the user queries under the `app:queryPatterns` property.  Just like my built-in Intents sample discussed earlier, I had this capability fulfilled by sending an explicit Intent to my application but notice how this custom intent contains a parameter which is the location of the store.  Per the documentation, the `android:name` cannot start with `actions.intent`.

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

Note that I had a build error when I added the `app:queryPatterns` property to my capability and I needed to update the `minSdkVersion` of my project to `25` to fix it.  *I'm not sure if `25` was the actual number, it was just the first value I tried*

I also defined a new shortcut to invoke the new, custom intent for stock availability:

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

I saw some strange behaviour when invoking the custom intent, even though I had defined the same target package and class to fulfill the capability the Android Intent received by my app came through in `onCreate` and not `onNewIntent` like I expected.  I am unsure whether this was something I had configured incorrectly in my app (my activity has its `launchMode` set to `singleTask`) or whether this is an issue with custom Intents.  

I know my custom Intent example will always have a `'store_location'` extra, so I use that in my generic Intent handler.  As I mentioned earlier, Google recommend you segregate these capability handlers into separate [modules](https://developer.android.com/studio/projects/add-app-module) but this is just a demo.

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

The application will then perform a (mocked) product availability lookup on the next scan:

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
        ...
    }
}
```
## Testing the Custom Intent

Please see the Video below to show the custom intent being invoked from the Android Studio Google Assistant plugin

[![Video Demo](https://img.youtube.com/vi/ICJdzGESAvE/0.jpg)](https://www.youtube.com/watch?v=ICJdzGESAvE)

I mentioned previously that to show App Actions running in response to a real invocation from the Google Assistant, it must first be approved according to the [Play Store Policy Asssitant review process](https://developers.google.com/assistant/app/get-started#request-review).  

The limitation of not being able to test with the real Google Assistant is even more evident for custom Intents because you want to test the effectiveness of the model Google created from your specified `queryPackages`, but to do that you need to install the (reviewed) app from the app store.  Also, the Android Studio plugin appears to test the `<capability/>`, not the `<shortcut/>` from your `shortcuts.xml` file.

Google also provides the user with the requirement to manually enable your voice shortcut.  You can suggest these shortcuts to your users using the [In-App Promo SDK](https://developers.google.com/assistant/app/in-app-promo-sdk) but it would require manual intervention from the user to enable your shortcut.

The dialog shown to the user looks similar to what is shown below:

![Shortcuts](https://raw.githubusercontent.com/darryncampbell/App-Actions-Demo/main/media/shortcuts_tc52_01.png)

## Conclusions
The App Actions feature is interesting and potentially powerful but I would not personally recommend it to any Enterprise app developers today.

The required integration with Google Assistant and manual approval of shortcuts would not work on a shared, dedicated enterprise device.  I am unsure whether App Actions would work with Android Enterprise Google accounts, though I suspect the dependency on the Google Assistant would preclude that.

There would be definite advantages to using App Actions in the enterprise, notably the ability to harness natural language processing on-device through a simple and approachable API is compelling but the technology is today definitely focused on consumer end users.  If you are looking for on-device speech recognition today, I would probably direct my customers towards the [DataWedge Voice Input feature](https://developer.zebra.com/blog/giving-context-speech-datawedge-and-voice-input) 

