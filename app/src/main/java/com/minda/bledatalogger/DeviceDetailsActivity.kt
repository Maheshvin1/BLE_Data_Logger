package com.minda.bledatalogger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileWriter

class DeviceDetailsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var deviceListAdapter: ArrayAdapter<String>

    private lateinit var dataListView: ListView
    private lateinit var exportDataBtn: Button
    private lateinit var refreshDataBtn: Button
    private lateinit var frontTyrePressure: TextView
    private lateinit var frontTyreTemp: TextView
    private lateinit var frontTyreBattery: TextView
    private lateinit var rearTyrePressure: TextView
    private lateinit var rearTyreTemp: TextView
    private lateinit var rearTyreBattery: TextView

    private lateinit var frontDeviceMac: TextView
    private lateinit var rearDeviceMac: TextView

    private var frontDeviceAddress: String = ""
    private var rearDeviceAddress: String = ""

    companion object {
        private const val FILE_MAX_SIZE = 100 * 1024
        private const val FILE_NAME = "Data_Logs.txt"
        private var device_Data = MainActivity.deviceData
        private var deviceAddress = MainActivity.deviceAddress
    }

   // var convertedData = device_Data?.map {it.toInt() and 0xFF}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        exportDataBtn = findViewById(R.id.exportBtn)
        refreshDataBtn = findViewById(R.id.refreshDataBtn)
        frontTyrePressure = findViewById(R.id.frontTyrePressure)
        frontTyreTemp = findViewById(R.id.frontTyreTemp)
        frontTyreBattery = findViewById(R.id.frontTyreBattery)
        rearTyrePressure = findViewById(R.id.rearTyrePressure)
        rearTyreTemp = findViewById(R.id.rearTyreTemp)
        rearTyreBattery = findViewById(R.id.rearTyreBattery)


        dataListView = findViewById(R.id.dataListView)
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        val selectedDevice = intent.getStringExtra("selectedDevices")

        //val convert = device_Data?.joinToString(separator = " ") { byte -> String.format("%2X", byte)}
        deviceListAdapter.add(selectedDevice)
        dataListView.adapter = deviceListAdapter

        listener()
    }

    private fun listener() {
        exportDataBtn.setOnClickListener(this)
        refreshDataBtn.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (view != null) {
            when(view.id) {
                R.id.refreshDataBtn -> {
                    onRefresh()
                }
                R.id.exportBtn -> {
                    exportDataInTextfile()
                }
            }
        }
    }

    private fun onRefresh() {
        //deviceListAdapter.notifyDataSetChanged()

        if (deviceAddress == frontDeviceAddress) {
            updateFrontTyreDetails()
        } else if (deviceAddress == rearDeviceAddress) {
            updateRearTyreDetails()
        } else {
            // Set as the first device if it's the only one or the first received device
            if (frontDeviceAddress.isEmpty()) {
                frontDeviceAddress = deviceAddress.toString()
                updateFrontTyreDetails()
//                frontDeviceMac.text = "mac: $deviceAddress"
            } else {
                rearDeviceAddress = deviceAddress.toString()
                updateRearTyreDetails()
//                frontDeviceMac.text = "mac: $deviceAddress"
            }
        }


//        val binaryStrings = device_Data?.run {
//                //byte -> String.format("%8s", byte.toInt().and(0xFF).toString(2)).replace(' ', '0')
//            listOf(
//                get(20).toInt().and(0xFF).toString(2).padStart(8, '0'),
//                get(21).toInt().and(0xFF).toString(2).padStart(8, '0'),
//                get(22).toInt().and(0xFF).toString(2).padStart(8, '0')
//            )
//        }
//
//        val binaryData = binaryStrings?.joinToString(" ")
//
//        var pressure = 0
//        var temperature = 0
//        var batteryPercentage = 0
//        var isNegative = 0
//
//        if (binaryData?.length!! >= 22) {
//            val pressureBits = binaryData.substring(2, 8)
//            val batteryBits = binaryData.substring(0, 2)
//            val temperatureBits = binaryData.substring(10, 17)
//            val signBits = binaryData.substring(9, 10)
//
//            pressure = Integer.parseInt(pressureBits, 2)
//            temperature = Integer.parseInt(temperatureBits, 2)
//
//            batteryPercentage = when (batteryBits) {
//                "00" -> 100
//                "01" -> 75
//                "10" -> 50
//                else -> 25
//            }
//
//            isNegative = if (signBits == "1") 1 else 0
//        }
//
//        frontTyrePressure.text = "$tyrePressure1 PSI"
//        frontTyreTemp.text = "$temperature1 C"
//        frontTyreBattery.text = "$battery1 %"
//
//        rearTyrePressure.text = "$tyrePressure2 PSI"
//        rearTyreTemp.text = "$temperature2 C"
//        rearTyreBattery.text = "$battery2 %"
//
//        Toast.makeText(this, "$device_Data", Toast.LENGTH_SHORT).show()
    }

    private fun updateFrontTyreDetails() {
        //val data = device_Data?.joinToString(separator = " ") { byte -> String.format("%2X", byte)}

        val binaryStrings = device_Data?.run {
            listOf(
                get(12).toInt().and(0xFF).toString(2).padStart(8, '0'),
                get(13).toInt().and(0xFF).toString(2).padStart(8, '0'),
                get(14).toInt().and(0xFF).toString(2).padStart(8, '0')
            )
        }

        val binaryData = binaryStrings?.joinToString(" ")

        var pressure = 0
        var temperature = 0
        var batteryPercentage = 0
        var isNegative = 0

        if (binaryData?.length!! >= 24) {
            val pressureBits = binaryData.substring(2, 8)
            Log.i("PressureBits", pressureBits)
            val batteryBits = binaryData.substring(0, 2)
            val temperatureBits = binaryData.substring(10, 17)
            val signBits = binaryData[9]

            pressure = Integer.parseInt(pressureBits, 2)
            temperature = Integer.parseInt(temperatureBits, 2)

            batteryPercentage = when (batteryBits) {
                "00" -> 100
                "01" -> 75
                "10" -> 50
                else -> 25
            }

            isNegative = if (signBits == '1') 1 else 0
        }


        frontTyrePressure.text = "$pressure PSI"
        frontTyreTemp.text = "$temperature C"
        frontTyreBattery.text = "$batteryPercentage %"

    }

    private fun updateRearTyreDetails() {

       // val data = MainActivity.deviceData?.joinToString(separator = " ") { byte -> String.format("%2X", byte)}

        val binaryStrings = device_Data?.run {
            listOf(
                get(12).toInt().and(0xFF).toString(2).padStart(8, '0'),
                get(13).toInt().and(0xFF).toString(2).padStart(8, '0'),
                get(14).toInt().and(0xFF).toString(2).padStart(8, '0')
            )
        }

        val binaryData = binaryStrings?.joinToString(" ")

        var pressure = 0
        var temperature = 0
        var batteryPercentage = 0
        var isNegative = 0

        if (binaryData?.length!! >= 22) {
            val pressureBits = binaryData.substring(2, 8)
            val batteryBits = binaryData.substring(0, 2)
            val temperatureBits = binaryData.substring(10, 17)
            val signBits = binaryData[9]

            pressure = Integer.parseInt(pressureBits, 2)
            temperature = Integer.parseInt(temperatureBits, 2)

            batteryPercentage = when (batteryBits) {
                "00" -> 100
                "01" -> 75
                "10" -> 50
                else -> 25
            }

            isNegative = if (signBits == '1') 1 else 0
        }

        rearTyrePressure.text = "$pressure PSI"
        rearTyreTemp.text = "$temperature C"
        rearTyreBattery.text = "$batteryPercentage %"
    }

    private fun exportDataInTextfile() {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = File(dir, FILE_NAME)
            val writer = FileWriter(file, true)

            for (i in 0 until dataListView.adapter.count) {
                val item = dataListView.getItemAtPosition(i)
                writer.write(item.toString())
                writer.write("\n")
            }

            writer.flush()
            writer.close()

            if (file.length() > FILE_MAX_SIZE) {
                truncateFile(dir, file)
            }
            Toast.makeText(this, "Data Exported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            //e.printStackTrace()
            Toast.makeText(this, "$e.message", Toast.LENGTH_SHORT).show()
        }
    }
    private fun truncateFile(directory: File, file: File) {
        val content = file.readText()
        val newData = content.substringAfter("\n")
        val writer = FileWriter(file, false)

        writer.write(newData)
        writer.flush()
        writer.close()
    }
}