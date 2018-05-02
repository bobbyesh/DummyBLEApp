package com.example.bek.dummybleapp;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class ScannedDevice implements Comparable<ScannedDevice>, Parcelable {
    private final static String TAG = ScannedDevice.class.getSimpleName();
    private String macAddress;
    private ArrayList<ScanResult> results;

    private final static int LIMITED_DISCOVERABLE_MODE_BIT = (1);
    private final static int GENERAL_DISCOVERABLE_MODE_BIT = (1 << 1);

    ScannedDevice(String macAddress) {
        this.macAddress = macAddress;
        this.results = new ArrayList<>();
    }

    private ScannedDevice(Parcel in) {
        macAddress = in.readString();
        int size = in.readInt();
        results = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ScanResult result = in.readParcelable(ScanResult.class.getClassLoader());
            results.add(result);
        }
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

        boolean alreadyStored = false;
        for (ScanResult sr : results) {
            byte[] srBytes = sr.getScanRecord().getBytes();
            byte[] resultBytes = result.getScanRecord().getBytes();
            if (Arrays.equals(srBytes, resultBytes)) {
                alreadyStored = true;
                break;
            }
            ;
        }

        if (!alreadyStored) {
            this.results.add(result);
            ScanRecord record = result.getScanRecord();
        }

        if (macAddress.contains("BA:A9"))
            Log.d(TAG, "NAME== " + getName() + " " + logResults());
    }

    public String logResults() {
        StringBuilder sb = new StringBuilder();

        sb.append("\r\n\r\nScanResults:\r\n");
        int i = 0;
        for (ScanResult result : this.results) {
            sb.append("\t")
                    .append("(")
                    .append(i)
                    .append(")");

            ScanRecord record = result.getScanRecord();
            if (record != null) {
                for (byte b : record.getBytes()) {
                    sb.append(String.format("%02x", b & 0xFF));
                }

                sb.append("Advertising Flags: ");
                sb.append(Integer.toHexString(record.getAdvertiseFlags()));
                sb.append(discoverableBitIsSet(record.getAdvertiseFlags()) ? "Discoverable" : "Not Discoverable");
                sb.append("\r\n");
            }
            sb.append("\r\n");

            i++;
        }

        return sb.toString();
    }

    private boolean discoverableBitIsSet(int flags) {
        return (flags & LIMITED_DISCOVERABLE_MODE_BIT) != 0 ||
                (flags & GENERAL_DISCOVERABLE_MODE_BIT) != 0;
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

        for (ScanResult result : results) {
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                List<Pair<Integer, byte[]>> data = ScanRecordParser.packetize(record.getBytes());
                for (Pair<Integer, byte[]> packet : data) {
                    int type = packet.first;
                    byte[] payload = packet.second;
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

    static class ScannedDeviceComparator implements Comparator<ScannedDevice> {
        @Override
        public int compare(ScannedDevice lhs, ScannedDevice rhs) {
            int lhsRssi = Collections.max(lhs.getRssiList());
            int rhsRssi = Collections.max(rhs.getRssiList());
            return Integer.compare(rhsRssi, lhsRssi);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ScannedDevice> CREATOR = new Parcelable.Creator<ScannedDevice>() {
        @Override
        public ScannedDevice createFromParcel(Parcel in) {
            return new ScannedDevice(in);
        }

        @Override
        public ScannedDevice[] newArray(int size) {
            return new ScannedDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(macAddress);
        dest.writeInt(results.size());
        for (ScanResult result : results) {
            dest.writeParcelable(result, flags);
        }
    }

    BluetoothGatt getProvisioningGatt(Activity context, BluetoothGattCallback callback) {
        for (ScanResult result : results) {
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                byte[] meshServiceData = record.getServiceData(ProvoltGattAttributes.MESH_PROVISIONING_SERVICE);
                if (meshServiceData != null) {
                    BluetoothDevice device = result.getDevice();
                    Log.d(TAG, "found the correct device");
                    return device.connectGatt(context, true, callback);
                }
            }
        }

        Log.d(TAG, "failed to find device");
        return null;
    }

    BluetoothDevice getConnectableDevice() {
        for (ScanResult result : results) {
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                Log.d(TAG, "record found...");
                int flags = record.getAdvertiseFlags();
                if (discoverableBitIsSet(flags)) {
                    Log.d(TAG, "Discoverable device found...");
                    return result.getDevice();
                }
            }
        }

        return null;
    }

    BluetoothGattService getService(UUID uuid) {
        BluetoothGattService service = null;
        for (ScanRecord record : getRecords()) {
            //service = record.get
        }

        return service;
    }

    ArrayList<BluetoothGatt> getGatts() {
        ArrayList<BluetoothGatt> gatts = new ArrayList<>();


        for (BluetoothDevice device : getBluetoothDevices()) {
            ParcelUuid[] uuids = device.getUuids();
            if (uuids != null) {
                for (ParcelUuid uuid : uuids) {
                    Log.d(TAG, "UUID== " + uuid.toString());
                }
            }
        }

        return null;
    }

    ArrayList<ScanRecord> getRecords() {
        ArrayList<ScanRecord> records = new ArrayList<>();
        for (ScanResult result : results) {
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                records.add(record);
            }
        }

        return records;
    }

    ArrayList<BluetoothDevice> getBluetoothDevices() {
        ArrayList<BluetoothDevice> list = new ArrayList<>();
        for (ScanResult result : results) {
            BluetoothDevice device = result.getDevice();
            list.add(device);
        }

        return list;
    }
}
