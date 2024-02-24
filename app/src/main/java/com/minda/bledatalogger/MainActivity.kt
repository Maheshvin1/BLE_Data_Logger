package com.minda.bledatalogger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat

class ScanActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var scanButton: Button
    private lateinit var readButton: Button
    private lateinit var deviceListView: ListView
    private lateinit var progressBar: ProgressBar

    private val TAG = "BLEScanner"
//    private val SCAN_PERIOD: Long = 10000

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val devicesArrayList: ArrayList<String> = ArrayList()

    private val PERMISSION_REQUEST_CODE = 101

    private var isScanning = false

    private var selectedDeviceIndex: Int = -1
    private val selectedDeviceIndices: MutableList<Int> = mutableListOf()
    val selectedDevices: MutableList<String> = mutableListOf()
    private val deviceDataMap: MutableMap<String?, ByteArray?> = mutableMapOf()

    private val updatedList: HashMap<String, String> = HashMap()

    companion object {
        var deviceData: ByteArray? = null
        var deviceAddress: String? = null
        var totalSize: Int = 0
        var dataList = mutableListOf<String>()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanButton)
        readButton = findViewById(R.id.readButton)
        deviceListView = findViewById(R.id.deviceList)
        progressBar = findViewById(R.id.progress_bar)

        readButton.isEnabled = false

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BLE not supported")
//            finish()
        }

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if ((ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        "android.permission.BLUETOOTH_SCAN"
                    ),
                    1
                )
            } else {
                startActivityForResult(enableBtIntent, PERMISSION_REQUEST_CODE)
            }
        }

        listener()
        setupListView()
        setupItemClickListener()

        requestPermissions()
    }

    private fun requestPermissions() {
        if ((ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT"
                ),
                1
            )
        } else {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun listener() {
        scanButton.setOnClickListener(this)
        readButton.setOnClickListener(this)
    }

    private fun setupItemClickListener() {
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            if (deviceListView.isItemChecked(position)) {
                selectedDeviceIndices.add(position)
                readButton.isEnabled = true
            } else {
                //selectedDeviceIndex = -1
                selectedDeviceIndices.remove(position)
                readButton.isEnabled = false
            }
        }
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.scanButton -> {
                    when (isBluetoothOn()) {
                        true -> {
                            isScanningDevices()
                        }

                        false -> {
                            enableBluetooth()
                        }
                    }
                }

                R.id.readButton -> {
                    startIntentFun()
//                    startIntentFunTest()
                }
            }
        }
    }

    private fun isBluetoothOn(): Boolean {
        if (bluetoothAdapter == null) {
            print("Does not support")
        } else return bluetoothAdapter.isEnabled
        return true
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        "android.permission.BLUETOOTH_SCAN"
                    ),
                    1
                )
            } else {
                startActivityForResult(enableBtIntent, 1)
            }
        } else {
        }
    }

    private fun isScanningDevices() {
        if (!isScanning) {
            devicesArrayList.clear()
            arrayAdapter.notifyDataSetChanged()
            startScan()
            isScanning = true
            scanButton.text = "Stop Scan"
            progressBar.visibility = View.VISIBLE
            deviceListView.clearChoices()
        } else {
            stopScan()
            isScanning = false
            scanButton.text = "Start Scan"
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            result?.let {
                //val device = it.device
                val scanRecord = it.scanRecord
                deviceData = scanRecord?.bytes
                val hexString = deviceData?.joinToString(separator = " ") { byte ->
                    String.format(
                        "%02X",
                        byte
                    )
                }

                deviceAddress = it.device.address
                val deviceName = scanRecord?.deviceName ?: "unamed"
                val rssi = result.rssi
                val manufacturerData = scanRecord?.manufacturerSpecificData

                Log.e(TAG, "Mac Address of Device: $deviceName - $deviceAddress")
                Log.e(TAG, "Device_Data $deviceData")
                Log.e(TAG, "Hex_String $hexString")
                Log.e(TAG, "RSSI $rssi")
                Log.e(TAG, "Manufacturer_Data $manufacturerData")

//                Toast.makeText(this@MainActivity, "Data: $deviceData", Toast.LENGTH_SHORT).show()

                //val deviceAddressPrefix = "4B:00:15:00"

                //if (deviceAddress.startsWith(deviceAddressPrefix)) {
                //arrayAdapter.add("$deviceName - $deviceAddress")
                //}

                deviceDataMap[deviceAddress] = deviceData

                result?.let {
                    if (!devicesArrayList.any {
                            it.equals(
                                "$deviceName - $deviceAddress",
                                ignoreCase = true
                            )
                        }) {
                        arrayAdapter.add("$deviceName - $deviceAddress")
                    }
                }
//                arrayAdapter.notifyDataSetChanged()

//                arrayAdapter.add(deviceName)

//                arrayAdapter.add("$deviceName - $deviceAddress")

//                if (!devicesArrayList.contains(deviceName)) {
//                    arrayAdapter.add(deviceName)
//                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }

//    private val scanHandler = Handler(Looper.getMainLooper())
//    @SuppressLint("MissingPermission")
//    private val scanRunnable = Runnable {
//        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//
//        bluetoothLeScanner.stopScan(leScanCallback)
//    }

    private fun setupListView() {
        arrayAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, devicesArrayList)
        deviceListView.adapter = arrayAdapter
    }

    private fun startScan() {
        if (!isScanning) {
            isScanning = true
            val scanFilters: MutableList<ScanFilter> = mutableListOf()

            // On basis of uuid
//            val serviceUUID = ParcelUuid.fromString("f000180a-0451-40ff-b000-00ff00000000")
//            val serviceUuidFilter = ScanFilter.Builder().setServiceUuid(serviceUUID).build()
//            scanFilters.add(serviceUuidFilter)

            // On basis of device name
//            val deviceFilter = ScanFilter.Builder().setDeviceName("UMT").build()
//            scanFilters.add(deviceFilter)

            //val manufacturerId = 0x000D
//            val manufacturerDataFilter = ScanFilter.Builder().setManufacturerData(0, byteArrayOf()).build()
//            scanFilters.add(manufacturerDataFilter)

            val deviceFilter = ScanFilter.Builder().build()
            scanFilters.add(deviceFilter)

//            val deviceFilter = ScanFilter.Builder().setDeviceAddress("4B:00:15:00:F4:86").build()
//            scanFilters.add(deviceFilter)


            val settings =
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()


            if ((ActivityCompat.checkSelfPermission(
                    this@ScanActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                    this@ScanActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this@ScanActivity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        "android.permission.BLUETOOTH_SCAN"
                    ),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                bluetoothLeScanner.startScan(scanFilters, settings, leScanCallback)
//                lifecycleScope.launch(Dispatchers.IO) {
//                    bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//                    bluetoothLeScanner.startScan(scanFilters, settings, leScanCallback)
//                }
                //scanHandler.postDelayed(scanRunnable, SCAN_PERIOD) //stop scan after some period of time(seconds)
            }
        }
//        else {
//            stopScan()
//        }
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bluetoothLeScanner.stopScan(leScanCallback)

        if (progressBar.visibility == View.VISIBLE) {
            progressBar.visibility = View.GONE
        }
    }

    private fun getDataForDevice(deviceAddress: String): String {
        val deviceData = deviceDataMap[deviceAddress]
        return deviceData?.joinToString(separator = " ") { byte -> String.format("%02X", byte) }
            ?: ""
    }

    private fun startIntentFun() {
        var selectedDevice: String? = ""
        var data: String? = ""

        for (index in selectedDeviceIndices) {
            selectedDevice = devicesArrayList[index].substringAfterLast(" - ")
            val deviceAddress = selectedDevice.substringAfterLast(" - ")
            data = getDataForDevice(deviceAddress)
//            data = deviceDataMap[deviceAddress]?.joinToString(separator = " ") { byte -> String.format("%2X", byte)}
            selectedDevices.add("$selectedDevice - $data")
            totalSize++
        }

        Log.i("totalSize::::", totalSize.toString())
//        broadcastIntent.putExtra("device_data", selectedDevice + "-" + data.toString())
//        sendBroadcast(broadcastIntent)

//        DataStorage.storedDeviceData = selectedDevices.toString()
//        val deviceDataToSend = DataStorage.storedDeviceData

        var deviceDataToSend = selectedDevices

//        val deviceDataList = deviceDataToSend.substring(1, deviceDataToSend.length - 1).split(",")

        val deviceDataList = deviceDataToSend.toList()

        val iterator = dataList.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val addressData = item.split(",").map { it.trim() }
            val itemAddress = addressData[0]
            if (deviceAddress == itemAddress) {
                iterator.remove()
            }
        }

        // Add updated device data to the dataList
        for (data in deviceDataList) {
            val addressData = data.trim().split("-").map { it.trim() }
            val address = addressData[0]
            val deviceData = addressData[1]
            dataList.add("$address-$deviceData")
        }

        var intent = Intent("updated_device_details")
        intent.putExtra("device_data", ArrayList(selectedDevices))
        sendBroadcast(intent)

//        var intent = Intent("updated_device_details")
//        intent.putStringArrayListExtra("device_data", ArrayList(selectedDevices))
//        intent.component = ComponentName("com.minda.bledatalogger", "com.minda.bledatalogger.DeviceDetailsActivity")
//        sendBroadcast(intent)

        intentFun()
    }

//    private fun startIntentFunTest() {
//        var intent = Intent("updated_device_details")
//        intent.putExtra("device_data", "sending data")
//        sendBroadcast(intent)
//
//        intentFun()
//    }

    private fun intentFun() {
//        intent.putStringArrayListExtra("deviceList", ArrayList(selectedDevices))
//        intent.putExtra("device_data", selectedDevice+"-"+data.toString())

        var intent1 = Intent(this@ScanActivity, DeviceDetailsActivity::class.java)

//        devicesArrayList.clear()
//        deviceListView.clearChoices()
        startActivity(intent1)
    }

    private fun readDataFromSelectedDevice() {
        val intent1 = Intent(this@ScanActivity, DeviceDetailsActivity::class.java)
        var selectedDevice: String? = ""
        var data: String? = ""
        val intent = Intent("device_data_details")

        for (index in selectedDeviceIndices) {
            selectedDevice = devicesArrayList[index]
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val deviceAddress = selectedDevice.substringAfterLast(" - ")

            data = deviceDataMap[deviceAddress]?.joinToString(separator = " ") { byte ->
                String.format(
                    "%2X",
                    byte
                )
            }


//            intent.putExtra("device_data", "$selectedDevice-$data")
//            intent.putParcelableArrayListExtra(DeviceDetailsActivity.CONNECTED_DEVICES, ArrayList(selectedDevices))
//
//            intent1.putExtra("selectedDevices", "$selectedDevice - $data")
//            intent.getStringExtra("selectedDevices")?.let { Log.i("selected Device data", it) }

        }
//        sendBroadcast(intent)
//        startActivity(intent1)
    }

    override fun onResume() {
        super.onResume()
//        startScan()

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if ((ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        "android.permission.BLUETOOTH_SCAN"
                    ),
                    1
                )
            } else {
                startActivityForResult(enableBtIntent, PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        stopScan()
//        unregisterReceiver(dataReceiver)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        bluetoothLeScanner.stopScan(leScanCallback)
    }
}
