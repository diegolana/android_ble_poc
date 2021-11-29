package br.com.diegolana.simpleble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

/**
 * Class to manage the bluetooth permission
 * <p/>
 * Created by Diego Lana
 * 26/11/2021
 * <p/>
 */
public class BluetoothPermissionManager {

    /**
     * First time requesting location permission
     */
    private boolean firstTimeRequestingLocation = true;

    /**
     * Used to request bluetooth service activation
     */
    static public final int REQUEST_ENABLE_BT = 1111;

    /**
     * Request code to request location permission
     */
    static public final int REQUEST_LOCATION = 2222;

    /**
     * Enumerator to possible states in the permission:
     * ALLOWED: User already allowed the permission or the service is active
     * DENIED: User denied the permission or the service is inactive
     * UNKNOWN: User was not asked about this permission yet.
     */
    public enum PermissionState {
        ALLOWED,
        DENIED,
        UNKNOWN
    }

    /**
     * State of the bluetooth service
     * ALLOWED = Active
     * UNKNOW = not checked yet
     * DENIED = user denied access to bluetooth service
     */
    private PermissionState bluetoothEnabledState = PermissionState.UNKNOWN;


    /**
     * check if the bluetooth is active
     *
     * @param context context
     * @return true if the service is active
     */
    private boolean checkBluetoothEnabled(@NonNull Context context) {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.isEnabled();
        } else {
            return true;
        }
    }

    /**
     * Start activity for result to enable bluetooth
     *
     * @param activity activity
     */
    public void enableBluetooth(@NonNull Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * get bluetooth permission state
     *
     * @return bluetooth permission state
     */
    public PermissionState getBluetoothState(@NonNull Context context) {
        if (checkBluetoothEnabled(context)) {
            bluetoothEnabledState = PermissionState.ALLOWED;
        } else {
            //if the bluetooth is not active update the state from Allowed to unknown
            if (bluetoothEnabledState == PermissionState.ALLOWED) {
                bluetoothEnabledState = PermissionState.UNKNOWN;
            }
        }
        return bluetoothEnabledState;
    }

    /**
     * set bluetooth permission state
     *
     * @param bluetoothEnabledState bluetooth state
     */
    public void setBluetoothEnabledState(PermissionState bluetoothEnabledState) {
        this.bluetoothEnabledState = bluetoothEnabledState;
    }

    /**
     * init bluetooth enabled state to default: UNKNOWN
     */
    public void init() {
        bluetoothEnabledState = PermissionState.UNKNOWN;
    }

    /**
     * Request for permissions passed
     *
     * @param activity - Activity which is requesting the permission
     */
    public void requestLocationPermission(@NonNull final Activity activity) {
        if (firstTimeRequestingLocation ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            firstTimeRequestingLocation = false;
        } else {
            permissionAlert(activity);
        }
    }

    /**
     * Show dialog to inform the user that the settings will be opened
     *
     * @param activity - Activity which is requesting the permission
     */
    private void permissionAlert(@NonNull final Activity activity) {
        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setCancelable(false);
        alertDialog.setTitle("Permission Alert");
        alertDialog.setMessage("You should allow this app to access your Location");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> {
            openAndroidSettings(activity);
            dialog.dismiss();
        });
        alertDialog.show();
    }

    /**
     * Open Android Settings to allow the user to change permissions
     *
     * @param activity - Activity which is requesting the permission
     */
    private void openAndroidSettings(@NonNull Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, REQUEST_LOCATION);
    }

}
