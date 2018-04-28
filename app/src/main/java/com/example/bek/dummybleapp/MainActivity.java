package com.example.bek.dummybleapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    List<DeviceInfo> scannedDevices;

    BluetoothManager bluetoothManager;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        scannedDevices = new ArrayList<>();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        final ArrayList<Pair<String, ScanResult>> list = new ArrayList<>();

        final HashMap<String, ScanResult> resultForAddress = new HashMap<>();
        final ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ScanRecord record = result.getScanRecord();

                if (record != null) {
                    String address = result.getDevice().getAddress();
                    Pair<String, ScanResult> pair = new Pair<>(address, result);
                    list.add(pair);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(TAG, "Scan failed (stopScan): error " + errorCode);
            }
        };

        Button scanButton = findViewById(R.id.scan_button);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "----------------------> Start Scan <---------------------------");
                bluetoothLeScanner.startScan(callback);
            }
        });

        Button stopScanButton = findViewById(R.id.stop_scan_button);
        stopScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothLeScanner.stopScan(callback);
                Log.d(TAG, "----------------------> STOP Scan <---------------------------");

                Collections.sort(list, new Comparator<Pair<String, ScanResult>>() {
                    @Override
                    public int compare(Pair<String, ScanResult> lhs, Pair<String, ScanResult> rhs) {
                        int lhsRssi = lhs.second.getRssi();
                        int rhsRssi = rhs.second.getRssi();
                        return Integer.compare(lhsRssi, rhsRssi);
                    }
                });

                for (Pair<String, ScanResult> entry : list) {
                    String address = entry.first;
                    ScanResult result = entry.second;

                    ScanRecord record = result.getScanRecord();

                    if (record != null) {

                        byte[] bytes = result.getScanRecord().getBytes();
                        boolean isMaybeMesh = false;

                        ByteBuffer bb = ByteBuffer.wrap(bytes);

                        StringBuilder sb = new StringBuilder();
                        for(byte b: bytes) {
                            int val = b & 0xFF;
                            sb.append(String.format("%x, ", val));
                        }

                        Log.d(TAG, "\r\n\r\nDevice :" + address + " -> " + sb.toString());
                        while (bb.remaining() != 0) {
                            int len = bb.get() & 0xFF;
                            if (len == 0) {
                                break;
                            }
                            int type = bb.get() & 0xFF;
                            byte[] data = new byte[len - 1];
                            bb.get(data);

                            sb = new StringBuilder();
                            for(byte b: data) {
                                int val = b & 0xFF;
                                sb.append(String.format("%x", val));
                            }

                            if (type == ScanRecordParser.SERVICE_DATA_UUID_16_BIT)
                                Log.d(TAG, "Device UUID: " + new ServiceDataParser(data).getDeviceUuidHex());

                            Log.d(TAG, "type " + ScanRecordParser.getAdvType(type) + ", data = " + sb.toString());
                        }
                    } else {
                        Log.d(TAG, "Null record...");
                    }
                }

                list.clear();
            }
        });
    }
}
