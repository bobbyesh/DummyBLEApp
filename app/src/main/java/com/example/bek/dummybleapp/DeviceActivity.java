package com.example.bek.dummybleapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {
    public final static String EXTRA_DEVICE = "EXTRA_DEVICE";

    ScannedDevice device;
    TextView deviceName;
    TextView macAddress;
    Button otaButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        initializeUI();
    }

    private void initializeUI(){
        deviceName = findViewById(R.id.device_name);
        deviceName.setText(device.getName());

        macAddress = findViewById(R.id.mac_address);
        macAddress.setText(device.getMacAddress());
        otaButton = findViewById(R.id.ota_button);
    }
}
