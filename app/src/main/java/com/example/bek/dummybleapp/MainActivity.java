package com.example.bek.dummybleapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    BluetoothManager bluetoothManager;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;
    private final ArrayList<ScannedDevice> scannedDevices = new ArrayList<>();
    ScanResultAdapter adapter;
    ListView listView;
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
        final ScanCallback callback = new UnprovisionedBeaconScanCallback(adapter, scannedDevices);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "----------------------> Start Scan <---------------------------");
                clearResultList();
                ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
                settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
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

        final MainActivity self = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScannedDevice device = scannedDevices.get(position);
                Bundle bundle = new Bundle();
                bundle.putParcelable(DeviceActivity.EXTRA_DEVICE, device);
                Intent intent = new Intent(self, DeviceActivity.class);
                intent.putExtras(bundle);
                bluetoothLeScanner.stopScan(callback);
                startActivity(intent);
            }
        });
    }

    private void clearResultList() {
        scannedDevices.clear();
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