package com.example.bek.dummybleapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public class DeviceActivity extends AppCompatActivity {
    private final static String TAG = "DeviceActivity";
    public final static String EXTRA_DEVICE = "EXTRA_DEVICE";

    ScannedDevice device;
    TextView deviceName;
    TextView macAddress;
    Button otaButton;
    OtaDataBluetoothGattCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        initializeUI();
        attachListeners();
    }

    private void initializeUI() {
        deviceName = findViewById(R.id.device_name);
        deviceName.setText(device.getName());

        macAddress = findViewById(R.id.mac_address);
        macAddress.setText(device.getMacAddress());
        otaButton = findViewById(R.id.ota_button);
    }

    private void attachListeners() {
        otaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeDummyOTA();
            }
        });
    }

    private void executeDummyOTA() {
        Log.d(TAG, "executeDummyOTA");
        ByteBuffer otaDataBytes = getOtaByteBuffer(1);
        BluetoothDevice btDevice = device.getConnectableDevice();
        callback = new OtaDataBluetoothGattCallback(this, btDevice, otaDataBytes, 20);
        callback.beginOtaUpdate();
    }

    private ByteBuffer getOtaByteBuffer(double kBytes) {
        byte[] data = new byte[(int) (kBytes * 1000)];
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) rand.nextInt();
        }

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.rewind();
        return bb;
    }
}

class OtaDataBluetoothGattCallback extends BluetoothGattCallback {
    private static final String TAG = OtaDataBluetoothGattCallback.class.getSimpleName();
    private ByteBuffer otaByteBuffer;
    private byte[] otaBytes;
    private boolean hasBegan;
    private CRC32 crc;
    private BluetoothGatt gatt;

    OtaDataBluetoothGattCallback(DeviceActivity context, BluetoothDevice device, ByteBuffer otaDataBytes, int mtuSize) {
        this.otaByteBuffer = otaDataBytes;
        this.otaBytes = new byte[mtuSize];
        this.hasBegan = false;
        this.crc = new CRC32();
        this.gatt = device.connectGatt(context, true, this);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch(newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.d(TAG, "Connected...");
                beginOtaUpdate();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.d(TAG, "Disconnected...");
                break;
        }
    }

    public void beginOtaUpdate() {
        Log.d(TAG, "beginOtaUpdate");
        gatt.discoverServices();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d(TAG, "onServicesedDiscvered == " + status);

        UUID uuid = ProvoltGattAttributes.OTA_SERVICE.getUuid();
        BluetoothGattService service = gatt.getService(uuid);
        if (service == null) {
            Log.d(TAG, "No service found with uuid: " + uuid.toString());
            Log.d(TAG, "Devices has uuids: " + Arrays.toString(gatt.getServices().toArray()));
            Log.d(TAG, "Device " + gatt.discoverServices());
            Log.d(TAG, "Aborting OTA update...");
            return;
        }

        uuid = ProvoltGattAttributes.OTA_DATA.getUuid();
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        writeOtaDataPDU(gatt, characteristic);
    }

    private void writeOtaDataPDU(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!hasBegan) {
            Log.d(TAG, "Starting OTA data transfer...");
            hasBegan = true;
        }

        if (otaByteBuffer.hasRemaining()) {
            loadBytes();
            characteristic.setValue(getBytes());
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            gatt.writeCharacteristic(characteristic);
        } else {
            Log.d(TAG, "OTA Transfer complete");
        }
    }

    private void loadBytes() {
        try {
            otaByteBuffer.get(otaBytes);
        } catch (BufferUnderflowException e) {
            // if otaBytes is too long, we shorten it to the length of the buffer.
            otaBytes = new byte[otaByteBuffer.remaining()];
            otaByteBuffer.get(otaBytes);
        }
    }

    private byte[] getBytes() {
        return otaBytes;
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            writeOtaDataPDU(gatt, characteristic);
        } else {
            Log.d(TAG, "Failed onCharacteristicWrite()");
        }
    }
}