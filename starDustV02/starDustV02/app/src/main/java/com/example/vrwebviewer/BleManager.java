// BLE MANAGER - Bluetooth Low Energy gyro control
package com.example.vrwebviewer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class BleManager {
    private static final String TAG = "BleManager";
    
    private static final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final long SCAN_TIMEOUT = 10000;
    
    public interface BleListener {
        void onBleConnected();
        void onBleDisconnected();
        void onGyroData(float gx, float gy, float gz);
        void onButtonData(boolean left, boolean right, boolean up, boolean down);
    }
    
    private Context context;
    private BleListener listener;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic rxCharacteristic;
    
    private Handler handler;
    private boolean isScanning = false;
    private boolean isConnected = false;
    
    public BleManager(Context context, BleListener listener) {
        this.context = context;
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        if (isScanning) return;
        
        isScanning = true;
        bluetoothLeScanner.startScan(scanCallback);
        
        handler.postDelayed(() -> {
            if (isScanning) stopScanning();
        }, SCAN_TIMEOUT);
    }
    
    public void stopScanning() {
        if (!isScanning || bluetoothLeScanner == null) return;
        
        isScanning = false;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }
    
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            String deviceName = device.getName();
            if (deviceName != null && (deviceName.contains("ESP32") || deviceName.contains("VR"))) {
                stopScanning();
                connectToDevice(device);
            }
        }
    };
    
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }
    
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                isConnected = true;
                
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
                
                if (listener != null) {
                    handler.post(() -> listener.onBleConnected());
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false;
                if (listener != null) {
                    handler.post(() -> listener.onBleDisconnected());
                }
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID);
                    if (rxCharacteristic != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.setCharacteristicNotification(rxCharacteristic, true);
                        }
                    }
                }
            }
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RX_CHAR_UUID)) {
                byte[] data = characteristic.getValue();
                String jsonString = new String(data);
                parseGyroData(jsonString);
            }
        }
    };
    
    private void parseGyroData(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            
            float gx = (float) json.optDouble("gx", 0.0);
            float gy = (float) json.optDouble("gy", 0.0);
            float gz = (float) json.optDouble("gz", 0.0);
            
            boolean left = json.optInt("l", 0) == 1;
            boolean right = json.optInt("r", 0) == 1;
            boolean up = json.optInt("su", 0) == 1;
            boolean down = json.optInt("sd", 0) == 1;
            
            if (listener != null) {
                handler.post(() -> {
                    listener.onGyroData(gx, gy, gz);
                    listener.onButtonData(left, right, up, down);
                });
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing gyro data: " + jsonString, e);
        }
    }
    
    public void disconnect() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
        isConnected = false;
        stopScanning();
    }
}