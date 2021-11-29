package br.com.diegolana.simpleble

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.bluetooth.BluetoothGattDescriptor
import androidx.lifecycle.Transformations
import android.bluetooth.BluetoothDevice


class ViewModelBLE(application: Application) : AndroidViewModel(application) {

    private val TAG = "BLE-TAG"
    private val SCAN_PERIOD: Long = 5000 // Stops scanning after 5 seconds.
    private val SERVICE_UUID = ParcelUuid.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHARACTERISTIC_UUID = ParcelUuid.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CLIENT_CHARAC_CONFIG = ParcelUuid.fromString("00002902-0000-1000-8000-00805f9b34fb") // Client Characteristic Configuration
    private val DEVICE_NAME = "HALL ESP32"

    private val buttonTextMutable: MutableLiveData<String> = MutableLiveData<String>("SCAN")
    val buttonText: LiveData<String> = buttonTextMutable

    private val scanContentMutable: MutableLiveData<String> = MutableLiveData<String>("...")
    val scanContent: LiveData<String> = scanContentMutable

    private val buttonVisibilityMutable: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val buttonVisibility: LiveData<Boolean> = buttonVisibilityMutable

    private val deviceMutable: MutableLiveData<BluetoothDevice?> = MutableLiveData(null)
    val isBonded: LiveData<Boolean> = Transformations.map(deviceMutable) { it != null }

    private var bluetoothManager: BluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothLeScanner: BluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
    private val filterServiceUUID = ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build()
    private val filterDeviceName = ScanFilter.Builder().setDeviceName(DEVICE_NAME).build()
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()

    private var scanning = false

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                when (state) {
                    BluetoothDevice.BOND_BONDING -> {
                        Log.i(TAG, "BOND_BONDING")
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        Log.i(TAG, "BOND_BONDED")
                        application.unregisterReceiver(this)
                        verifyBond()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Log.i(TAG, "BOND_NONE")
                        verifyBond()
                    }
                }
            }
        }
    }

    private var scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            Log.i(TAG, "$result.device.name")
            scanContentMutable.value = device.name
            Log.d(TAG, "result.device.uuids ${result.scanRecord}")
            Log.d(TAG, "device.bondState ${device.bondState}")
            device.createBond()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i(TAG, "error")
            scanContentMutable.value = "FAIL"
        }
    }

    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        application.registerReceiver(mReceiver, filter)

        verifyBond()
    }

    private fun verifyBond() {
        bluetoothManager.adapter.bondedDevices.find { it.name == DEVICE_NAME }?.let {
            deviceMutable.postValue(it)
        }
    }

    fun scanLeDevice() {
        if (!scanning) {
            Handler(Looper.getMainLooper()).postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(scanCallback)
                buttonTextMutable.value = "START SCAN"
                buttonVisibilityMutable.value = false
            }, SCAN_PERIOD)
            buttonVisibilityMutable.value = true
            buttonTextMutable.value = "STOP SCAN"
            scanning = true
            startScan()
        } else {
            buttonTextMutable.value = "START SCAN"
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
            buttonVisibilityMutable.value = false
        }
    }

    private fun startScan() {
        //bluetoothLeScanner.startScan(scanCallback) // simple scan without filter
        bluetoothLeScanner.startScan(listOf(filterDeviceName),scanSettings,scanCallback)
    }

    fun connect() {
        deviceMutable.value?.connectGatt(getApplication(), false, gattCallback)
    }

    private val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i(TAG, "onConnectionStateChange")
            gatt?.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG, "onServicesDiscovered")
            gatt?.getService(SERVICE_UUID.uuid)?.let { gattService ->
                gattService.getCharacteristic(CHARACTERISTIC_UUID.uuid)?.let { gattCharacteristic ->
                    readDescriptor(gatt, gattCharacteristic) // read changes
                    //readCharacteristic(gatt, gattCharacteristic) // only once
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicRead")
            characteristic?.let{ showValue(characteristic) }
            readDescriptor(gatt, characteristic)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.i(TAG, "onCharacteristicChanged")
            characteristic?.let{ showValue(characteristic) }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicWrite")
        }

        /**
         * Read characteristic only once
         *
         * @param gatt
         */
        private fun readCharacteristic(gatt: BluetoothGatt, gattCharacteristic: BluetoothGattCharacteristic) {
            gatt.readCharacteristic(gattCharacteristic)
        }

        /**
         * Register to characteristic
         * You will get notifications through onCharacteristicChanged callback every time the characteristic changes.
         *
         * @param gatt
         */
        private fun readDescriptor(gatt: BluetoothGatt?, gattCharacteristic: BluetoothGattCharacteristic?) {
            Log.i(TAG, "registerDescriptor")
            gatt?.let {
                gattCharacteristic?.let {
                    val clientConfig: BluetoothGattDescriptor = gattCharacteristic.getDescriptor(CLIENT_CHARAC_CONFIG.uuid)
                    if (gatt.setCharacteristicNotification(gattCharacteristic, true)) {
                        clientConfig.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(clientConfig)
                        Log.i(TAG, "registerDescriptor success")
                    }
                }
            }
        }

        private fun showValue(gattCharacteristic: BluetoothGattCharacteristic) {
            val value = gattCharacteristic?.value
            val intValue = Util.byteToInt(value)
            Log.i(TAG, "value = $intValue")
            scanContentMutable.postValue(intValue.toString())
        }
    }

}