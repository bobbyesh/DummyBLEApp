package com.example.bek.dummybleapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    BluetoothManager bluetoothManager;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;
    private final ArrayList<ScannedDevice> scannedDevices = new ArrayList<>();
    ScanResultAdapter adapter;
    ListView listView;
    final ScanCallback callback = getCallback();
    private Button scanButton;
    private Button stopScanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        adapter = new ScanResultAdapter(this, scannedDevices);

        getBluetoothPermissions();
        initializeUI();
        attachListeners();
    }

    private void initializeUI() {
        scanButton = findViewById(R.id.scan_button);
        stopScanButton = findViewById(R.id.stop_scan_button);
        listView = findViewById(R.id.list_view);
        listView.setAdapter(adapter);
    }

    private void attachListeners() {
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "----------------------> Start Scan <---------------------------");
                clearResultList();
                bluetoothLeScanner.startScan(callback);
            }
        });

        stopScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothLeScanner.stopScan(callback);
                Log.d(TAG, "----------------------> STOP Scan <---------------------------");
            }
        });
    }

    private void logScanRecord(Pair<String, ScanResult> entry) {
        String address = entry.first;
        ScanResult result = entry.second;
        ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord == null)
            return;

        byte[] bytes = scanRecord.getBytes();
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
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
            for (byte b : data) {
                int val = b & 0xFF;
                sb.append(String.format("%x", val));
            }

            if (type == ScanRecordParser.SERVICE_DATA_UUID_16_BIT)
                Log.d(TAG, "Device UUID: " + new ServiceDataParser(data).getDeviceUuidHex());

            Log.d(TAG, "type " + ScanRecordParser.getAdvType(type) + ", data = " + sb.toString());
        }
    }

    @NonNull
    private ScanCallback getCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, "onScanResult");
                ScanRecord record = result.getScanRecord();

                if (record != null) {
                    Log.d(TAG, "onScanResult: record != null");
                    String address = result.getDevice().getAddress();
                    Pair<String, ScanResult> pair = new Pair<>(address, result);
                    addScanResult(address, result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(TAG, "Scan failed (stopScan): error " + errorCode);
            }
        };
    }

    private void clearResultList() {
        scannedDevices.clear();
        adapter.notifyDataSetChanged();
    }

    private void addScanResult(String macAddress, ScanResult scanResult) {
        Log.d(TAG, "addScanResult");

        boolean hasPreviousScanRecordFromSameDevice = false;
        for (ScannedDevice device : scannedDevices) {
            if (device.getMacAddress().equals(macAddress)) {
                device.addScanResult(scanResult);
                hasPreviousScanRecordFromSameDevice = true;
                break;
            }
        }

        if (!hasPreviousScanRecordFromSameDevice) {

            ScannedDevice device = new ScannedDevice(macAddress);
            device.addScanResult(scanResult);
            scannedDevices.add(device);
        }

        Log.d(TAG, "addScanResult: scannedDevices.size() == " + scannedDevices.size());
        Collections.sort(scannedDevices, new ScannedDeviceComparator());
        adapter.notifyDataSetChanged();
    }

    private void getBluetoothPermissions() {
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
    }

    private String listItemString(Pair<String, ScanResult> item) {
        StringBuilder sb = new StringBuilder();
        String mac_address = item.first;
        String rssi = Integer.toString(item.second.getRssi());

        ScanRecord record = item.second.getScanRecord();
        String name = record != null ? record.getDeviceName() : "Unknown";

        sb.append(name);
        sb.append("\r\n");
        sb.append(mac_address);
        sb.append(", rssi=");
        sb.append(rssi);

        return sb.toString();
    }

}

class ScanResultAdapter extends BaseAdapter {
    private final static String TAG = ScanResultAdapter.class.getSimpleName();

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<ScannedDevice> devices;

    ScanResultAdapter(Context context, ArrayList<ScannedDevice> devices) {
        this.context = context;
        this.devices = devices;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.scan_record_list_item, parent, false);

        TextView name = view.findViewById(R.id.device_name);
        TextView rssi = view.findViewById(R.id.rssi);
        TextView mac = view.findViewById(R.id.mac_address);

        ScannedDevice device = this.devices.get(position);
        ArrayList<ScanResult> results = device.getScanResults();
        String rssiString = Integer.toString(Collections.max(device.getRssiList()));

        name.setText(device.getName());
        mac.setText(device.getMacAddress());
        rssi.setText(rssiString);
        return view;
    }
}

class ScannedDevice implements Comparable<ScannedDevice> {
    private final static String TAG = ScannedDevice.class.getSimpleName();
    private String macAddress;
    private ArrayList<ScanResult> results;

    ScannedDevice(String macAddress) {
        this.macAddress = macAddress;
        this.results = new ArrayList<>();
    }

    public ArrayList<ScanResult> getScanResults() {
        return results;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void addScanResult(ScanResult result) {

        if (results.isEmpty()) {
            results.add(result);
            return;
        }

        for (ScanResult sr: results) {
            byte[] srBytes = sr.getScanRecord().getBytes();
            byte[] resultBytes = result.getScanRecord().getBytes();
            if(!Arrays.equals(srBytes, resultBytes)) {
                this.results.add(result);
                break;
            };
        }
    }

    public int getRssi() {
        return Collections.max(getRssiList());
    }

    public ArrayList<Integer> getRssiList() {
        ArrayList<Integer> rssiList = new ArrayList<>();
        for (ScanResult result : results) {
            rssiList.add(result.getRssi());
        }

        return rssiList;
    }

    public String getName() {
        String name = "Unknown Device";
        boolean isPrc = macAddress.contains("BA:A9");


        for (ScanResult result : results) {
            ScanRecord record = result.getScanRecord();
            if (isPrc) {
                int[] values = new int[record.getBytes().length];
                for (int i=0; i<values.length; i++) {
                    values[i] = record.getBytes()[i] & 0xFF;
                }
            }
            if (record != null) {
                List<Pair<Integer, byte[]>> data = ScanRecordParser.packetize(record.getBytes());
                for (Pair<Integer, byte[]> packet : data) {
                    int type = packet.first;
                    byte[] payload = packet.second;
                    if (isPrc) {
                        Log.d(TAG, String.format("type == %x", type));
                    }
                    if (type == ScanRecordParser.COMPLETE_LOCAL_NAME) {
                        name = new String(payload, Charset.forName("UTF-8"));
                    }
                }
            }
        }

        return name;
    }

    @Override
    public int compareTo(@NonNull ScannedDevice o) {
        return new ScannedDeviceComparator().compare(this, o);
    }

    @Override
    public String toString() {
        return "ScannedDevice{" +
                "macAddress='" + macAddress + '\'' +
                ", results=" + results +
                '}';
    }
}

class ScannedDeviceComparator implements Comparator<ScannedDevice> {
    @Override
    public int compare(ScannedDevice lhs, ScannedDevice rhs) {
        int lhsRssi = Collections.max(lhs.getRssiList());
        int rhsRssi = Collections.max(rhs.getRssiList());
        return Integer.compare(rhsRssi, lhsRssi);
    }
}