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

import com.example.bek.dummybleapp.utils.ByteStringBuilder;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public class DeviceActivity extends AppCompatActivity {
    private final static String TAG = "DeviceActivity";
    public final static String EXTRA_DEVICE = "EXTRA_DEVICE";
    private final int mtuSize = 250;
    private final int TEST_TRANSFER_SIZE = 10;

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
        ByteBuffer otaDataBytes = getOtaByteBuffer(TEST_TRANSFER_SIZE);
        BluetoothDevice btDevice = device.getConnectableDevice();
        callback = new OtaDataBluetoothGattCallback(this, btDevice, otaDataBytes, mtuSize);
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
    private int transferedByteCount;
    private ArrayList<Byte> successfullySentBytes;
    private long startTime;
    private BluetoothGattCharacteristic dataCharacteristic;
    private BluetoothGattCharacteristic controlCharacteristic;
    private BluetoothGattCharacteristic crcCharacteristic;

    private UUID otaServiceUuid;
    private final UUID dataCharacteristicUuid = ProvoltGattAttributes.OTA_DATA.getUuid();
    private final UUID controlCharacteristicUuid = ProvoltGattAttributes.OTA_CONTROL.getUuid();
    private final UUID crcCharacteristicUuid = ProvoltGattAttributes.OTA_CRC.getUuid();

    private static final byte OTA_CONTROL_START = 0;
    private static final byte OTA_CONTROL_END = 1;

    OtaDataBluetoothGattCallback(DeviceActivity context, BluetoothDevice device, ByteBuffer otaDataBytes, int mtuSize) {
        this.otaByteBuffer = otaDataBytes;
        this.otaBytes = new byte[mtuSize];
        this.hasBegan = false;
        this.crc = new CRC32();
        this.gatt = device.connectGatt(context, true, this);
        this.transferedByteCount = 0;
        this.successfullySentBytes = new ArrayList<>();
        otaServiceUuid = ProvoltGattAttributes.OTA_SERVICE.getUuid();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.d(TAG, "Connected...");
                gatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.d(TAG, "Disconnected...");
                break;
        }
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d(TAG, "onServicesDiscovered == " + status);

        BluetoothGattService service = gatt.getService(otaServiceUuid);
        if (service == null) {
            Log.d(TAG, "No service found with uuid: " + otaServiceUuid.toString());
            Log.d(TAG, "Devices has uuids: " + Arrays.toString(gatt.getServices().toArray()));
            Log.d(TAG, "Device " + gatt.discoverServices());
            Log.d(TAG, "Aborting OTA update...");
            return;
        }

        dataCharacteristic = service.getCharacteristic(dataCharacteristicUuid);
        if (dataCharacteristic == null) {
            Log.d(TAG, "No data characteristc found ..., aborting OTA");
            return;
        }

        controlCharacteristic = service.getCharacteristic(controlCharacteristicUuid);
        if (controlCharacteristic == null) {
            Log.d(TAG, "No control characteristic found..., aborting OTA");
            return;
        }

        crcCharacteristic = service.getCharacteristic(crcCharacteristicUuid);
        if (crcCharacteristic == null) {
            Log.d(TAG, "No crc characterstic found..., aborting OTA");
        }
        sendControlStart();
    }

    private void sendControlStart() {
        Log.d(TAG, "sendControlStart");
        byte[] packet = {OTA_CONTROL_START};
        controlCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        controlCharacteristic.setValue(packet);
        gatt.writeCharacteristic(controlCharacteristic);
    }

    private void sendControlEnd() {
        byte[] packet = {OTA_CONTROL_END};
        controlCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        controlCharacteristic.setValue(packet);
        gatt.writeCharacteristic(controlCharacteristic);
    }

    private void readCrc() {
        gatt.readCharacteristic(crcCharacteristic);
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
        byte[] bytes = characteristic.getValue();

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.getUuid().equals(dataCharacteristicUuid)) {
                transferedByteCount += bytes.length;
                crc.update(bytes);
                writeOtaDataPDU(gatt, characteristic);
            } else if (characteristic.getUuid().equals(controlCharacteristicUuid)) {
                Log.d(TAG, "Succesfful control characteristic write");
                byte[] controlValues = characteristic.getValue();
                if (controlValues[0] == OTA_CONTROL_START) {
                    writeOtaDataPDU(gatt, dataCharacteristic);
                } else if (controlValues[0] == OTA_CONTROL_END) {
                    readCrc();
                }
            }
        } else {
            ByteStringBuilder bsb = new ByteStringBuilder();
            bsb.append(bytes);
            Log.d(TAG, "Failed onCharacteristicWrite(): characteristic uuid == " + characteristic.getUuid().toString() + "\r\ndata == " + bsb.toString());
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log.d(TAG, "onReadCharacteristic");
        if (characteristic.getUuid().equals(crcCharacteristicUuid)) {
            Log.d(TAG, "characteristic == ota CRC");
            ByteStringBuilder bsb = new ByteStringBuilder();
            byte[] bytes = characteristic.getValue();
            bsb.append(bytes);
            Log.d(TAG, "remote == " + bsb.toString());

            long remoteCrc = (long) ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
            long localCrc = crc.getValue();
            byte[] localCrcBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                localCrcBytes[4 - 1 - i] = (byte) ((localCrc >> (i * 8)) & 0xFF);
            }

            bsb = new ByteStringBuilder();
            bsb.append(localCrcBytes);
            Log.d(TAG, "local == " + bsb.toString());
            Log.d(TAG, "CRC passed? " + (Arrays.equals(localCrcBytes, bytes)));

            /*
            Log.d(TAG, "CRC from local == " + crc.getValue());
            Log.d(TAG, "CRC (hex) from local == " + Long.toHexString(crc.getValue()));
            Log.d(TAG, "CRC from remote == " + remoteCrc);
            Log.d(TAG, "CRC (hex) from remote == " + Long.toHexString(remoteCrc));
            Log.d(TAG, "CRC (as bytes) from remote == " + bsb.toString());
            */
        }
    }

    private void writeOtaDataPDU(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!hasBegan) {
            Log.d(TAG, "Starting OTA data transfer");
            startTime = System.currentTimeMillis();
            hasBegan = true;
        }

        if (otaByteBuffer.hasRemaining()) {
            loadBytes();
            characteristic.setValue(getBytes());
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            gatt.writeCharacteristic(characteristic);
        } else {
            Log.d(TAG, "OTA Transfer complete");
            Log.d(TAG, "CRC == " + Long.toHexString(crc.getValue()));
            Log.d(TAG, "Transfered: " + transferedByteCount);
            Log.d(TAG, "Time elapsed " + Math.round((double) (System.currentTimeMillis() - startTime) / 1000.0 / 60.0 * 100) / 100.0 + "min");
            sendControlEnd();
        }
    }

}