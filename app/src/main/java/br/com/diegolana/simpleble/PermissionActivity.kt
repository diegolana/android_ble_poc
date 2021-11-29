package br.com.diegolana.simpleble

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

open class PermissionActivity : AppCompatActivity() {

    private val permissionManager = BluetoothPermissionManager()

    fun requestPermission(): Boolean {
        if (permissionManager.getBluetoothState(this) == BluetoothPermissionManager.PermissionState.ALLOWED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionManager.requestLocationPermission(this)
                return false
            } else {
                return true
            }
        } else {
            permissionManager.enableBluetooth(this)
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == BluetoothPermissionManager.REQUEST_LOCATION &&
            grantResults[0] == -1)  {

            requestPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == BluetoothPermissionManager.REQUEST_ENABLE_BT &&
            resultCode == 0) {
            requestPermission()
        }
    }

}