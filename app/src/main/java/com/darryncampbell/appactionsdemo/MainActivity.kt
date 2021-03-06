package com.darryncampbell.appactionsdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), Observer, View.OnTouchListener {

    private val LOG_TAG: String = "AppActionsDemo"
    private var scans: ArrayList<Scan> = arrayListOf()
    private var adapter = ScanAdapter(this, scans)
    private val dwInterface = DWInterface()
    private val receiver = DWReceiver()
    private var initialized = false
    private var version65OrOver = false
    private var storeLocation = ""
    private var stockAvailabilityCheck = false

    companion object {
        const val PROFILE_NAME = "AppActionsDemo"
        const val PROFILE_INTENT_ACTION = "com.darryncampbell.appactionsdemo.SCAN"
        const val PROFILE_INTENT_START_ACTIVITY = "0"
        const val SCAN_HISTORY_FILE_NAME = "ScanHistory"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        listView?.adapter = adapter
        ObservableObject.instance.addObserver(this)
        btnScan.setOnTouchListener(this)

        //  Register broadcast receiver to listen for responses from DW API
        val intentFilter = IntentFilter()
        intentFilter.addAction(DWInterface.DATAWEDGE_RETURN_ACTION)
        intentFilter.addCategory(DWInterface.DATAWEDGE_RETURN_CATEGORY)
        registerReceiver(receiver, intentFilter)

        handleIntent(intent)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent)
    {
        Log.v(LOG_TAG, intent.action.toString())
        val bundleExtras = intent.extras
        if (bundleExtras != null) {
            for (key in bundleExtras.keySet()) {
                Log.v(LOG_TAG, key)
            }
        }
        if (intent.action.toString().equals("android.intent.action.MAIN"))
        {
            return
        }
        else if (intent.hasExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING)) {
            //  DataWedge intents received here
            if (stockAvailabilityCheck)
            {
                //  For demo purposes only(!)
                val date = Calendar.getInstance().time
                val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                val dateTimeString = df.format(date)
                val availabilityLookup = Scan(storeLocation + " stock: " +
                        Random().nextInt(10), "Stock Availability", dateTimeString)
                scans.add(0, availabilityLookup)
                stockAvailabilityCheck = false;
            }
            else
            {
                //  Handle scan intent received from DataWedge, add it to the list of scans
                val scanData = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING)
                val symbology = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_LABEL_TYPE)
                val date = Calendar.getInstance().time
                val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                val dateTimeString = df.format(date)
                val currentScan = Scan(scanData, symbology, dateTimeString)
                scans.add(0, currentScan)
            }
        }
        else if (intent.hasExtra("store_location"))
        {
            //  STOCK_AVAILABILITY Intent entry point (App Actions)
            storeLocation = intent.getStringExtra("store_location").toString()
            Log.d(LOG_TAG, "Looking up stock availability in $storeLocation")
            stockAvailabilityCheck = true;
            dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN,
                "START_SCANNING")

        }
        else if (intent.hasExtra("actions.fulfillment.extra.ACTION_TOKEN"))
        {
            //  GET_BARCODE Intent entry point (App Actions)
            val token = intent.getStringExtra("actions.fulfillment.extra.ACTION_TOKEN").toString()
            Log.d(LOG_TAG, token)
            dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN,
                "START_SCANNING")
        }
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, ConfigurationActivity::class.java)
                settingsIntent.putExtra(ConfigurationActivity.SETTINGS_KEY_VERSION, version65OrOver)
                startActivity(settingsIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        //  initialized variable is a bit clunky but onResume() is called on each newIntent()
        if (!initialized) {
            //  Create profile to be associated with this application
            dwInterface.sendCommandString(this, DWInterface.DATAWEDGE_SEND_GET_VERSION, "")
            initialized = true
        }
    }

    override fun onStart()
    {
        super.onStart()
        //  Persist the scanned barcodes
        readFile()
    }

    override fun onStop()
    {
        super.onStop()
        //  Persist the scanned barcodes
        writeFile()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun update(p0: Observable?, p1: Any?) {
        //  Invoked in response to the DWReceiver broadcast receiver
        val receivedIntent = p1 as Intent
        //  This activity will only receive DataWedge version since that is all we ask for, the
        //  configuration activity is responsible for other return values such as enumerated scanners
        //  If the version is <= 6.5 we reduce the amount of configuration available.  There are
        //  smarter ways to do this, e.g. DW 6.4 introduces profile creation (without profile
        //  configuration) but to keep it simple, we just define a minimum of 6.5 for configuration
        //  functionality
        if (receivedIntent.hasExtra(DWInterface.DATAWEDGE_RETURN_VERSION)) {
            val version = receivedIntent.getBundleExtra(DWInterface.DATAWEDGE_RETURN_VERSION)
            val dataWedgeVersion = version?.getString(DWInterface.DATAWEDGE_RETURN_VERSION_DATAWEDGE)
            if (dataWedgeVersion != null && dataWedgeVersion >= "6.5" && !version65OrOver) {
                version65OrOver = true
                createDataWedgeProfile()
            }
        }
    }

    private fun createDataWedgeProfile() {
        //  Create and configure the DataWedge profile associated with this application
        //  For readability's sake, I have not defined each of the keys in the DWInterface file
        dwInterface.sendCommandString(this, DWInterface.DATAWEDGE_SEND_CREATE_PROFILE, PROFILE_NAME)
        val profileConfig = Bundle()
        profileConfig.putString("PROFILE_NAME", PROFILE_NAME)
        profileConfig.putString("PROFILE_ENABLED", "true") //  These are all strings
        profileConfig.putString("CONFIG_MODE", "UPDATE")
        val barcodeConfig = Bundle()
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
        barcodeConfig.putString("RESET_CONFIG", "true") //  This is the default but never hurts to specify
        val barcodeProps = Bundle()
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps)
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig)
        val appConfig = Bundle()
        appConfig.putString("PACKAGE_NAME", packageName)      //  Associate the profile with this app
        appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
        val appConfigAssistant = Bundle()
        appConfigAssistant.putString("PACKAGE_NAME", "com.google.android.googlequicksearchbox")      //  Associate the profile with the Google assistant
        appConfigAssistant.putStringArray("ACTIVITY_LIST", arrayOf("*"))
        profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig, appConfigAssistant))
        dwInterface.sendCommandBundle(this, DWInterface.DATAWEDGE_SEND_SET_CONFIG, profileConfig)
        //  You can only configure one plugin at a time in some versions of DW, now do the intent output
        profileConfig.remove("PLUGIN_CONFIG")
        val intentConfig = Bundle()
        intentConfig.putString("PLUGIN_NAME", "INTENT")
        intentConfig.putString("RESET_CONFIG", "true")
        val intentProps = Bundle()
        intentProps.putString("intent_output_enabled", "true")
        intentProps.putString("intent_action", PROFILE_INTENT_ACTION)
        intentProps.putString("intent_delivery", PROFILE_INTENT_START_ACTIVITY)  //  "0"
        intentConfig.putBundle("PARAM_LIST", intentProps)
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig)
        dwInterface.sendCommandBundle(this, DWInterface.DATAWEDGE_SEND_SET_CONFIG, profileConfig)
    }

    //  Credit: https://stackoverflow.com/questions/5816695/android-sharedpreferences-with-serializable-object
    private fun writeFile() {
        //  Persist the scans array to a file
        var objectOut: ObjectOutputStream? = null
        try {
            val fileOut = applicationContext.openFileOutput(SCAN_HISTORY_FILE_NAME, Activity.MODE_PRIVATE)
            objectOut = ObjectOutputStream(fileOut)
            objectOut.writeObject(scans)
            fileOut.fd.sync()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    //  Credit: https://stackoverflow.com/questions/5816695/android-sharedpreferences-with-serializable-object
    private fun readFile()
    {
        //  Read in the previously persisted scans array, else create a new array if one does not exist
        var objectIn: ObjectInputStream? = null
        try {
            val fileIn = applicationContext.openFileInput(SCAN_HISTORY_FILE_NAME)
            objectIn = ObjectInputStream(fileIn)
            @Suppress("UNCHECKED_CAST")
            scans = objectIn.readObject() as ArrayList<Scan>
        } catch (e: FileNotFoundException) {
            scans = arrayListOf()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: ClassCastException) {
            e.printStackTrace()
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close()
                } catch (e: IOException) {
                    // do nowt
                }

            }
        }

        adapter = ScanAdapter(this, scans)
        listView?.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(button: View?, event: MotionEvent?): Boolean {
        when (button?.id) {
            R.id.btnScan -> {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->
                    {
                        dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN,
                                "START_SCANNING")
                        return true
                    }
                    MotionEvent.ACTION_UP ->
                    {
                        dwInterface.sendCommandString(applicationContext, DWInterface.DATAWEDGE_SEND_SET_SOFT_SCAN,
                                "STOP_SCANNING")
                        return true
                    }
                }
            }
        }
        return false
    }

}
