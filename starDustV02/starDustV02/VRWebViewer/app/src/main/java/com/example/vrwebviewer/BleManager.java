// BLE MANAGER - Bluetooth Low Energy gyro control
// Features: ESP32 connection, JSON parsing, gyro data processing
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
    
    // Nordic UART Service UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID TX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    
    private static final long SCAN_TIMEOUT = 10000; // 10 seconds
    
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
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available or not enabled");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_SCAN permission not granted");
            return;
        }
        
        if (isScanning) {
            return;
        }
        
        Log.d(TAG, "Starting BLE scan");
        isScanning = true;
        
        bluetoothLeScanner.startScan(scanCallback);
        
        // Stop scanning after timeout
        handler.postDelayed(() -> {
            if (isScanning) {
                stopScanning();
            }
        }, SCAN_TIMEOUT);
    }
    
    public void stopScanning() {
        if (!isScanning || bluetoothLeScanner == null) {
            return;
        }
        
        Log.d(TAG, "Stopping BLE scan");
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
            Log.d(TAG, "Found device: " + deviceName + " (" + device.getAddress() + ")");
            
            // Connect to first device with Nordic UART service
            if (deviceName != null && (deviceName.contains("ESP32") || deviceName.contains("VR"))) {
                stopScanning();
                connectToDevice(device);
            }
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with error: " + errorCode);
            isScanning = false;
        }
    };
    
    private void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to device: " + device.getAddress());
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }
    
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server");
                isConnected = true;
                
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
                
                if (listener != null) {
                    handler.post(() -> listener.onBleConnected());
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server");
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
                        // Enable notifications
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.setCharacteristicNotification(rxCharacteristic, true);
                        }
                        Log.d(TAG, "Nordic UART service configured");
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