package com.example.bek.dummybleapp;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import com.example.bek.dummybleapp.utils.ByteStringBuilder;

import java.util.ArrayList;
import java.util.Collections;

class UnprovisionedBeaconScanCallback extends ScanCallback {
    private static final String TAG = UnprovisionedBeaconScanCallback.class.getSimpleName();

    private ScanResultAdapter adapter;
    private ArrayList<ScannedDevice> previouslyScannedDevices;

    UnprovisionedBeaconScanCallback(ScanResultAdapter adapter, ArrayList<ScannedDevice> previouslyScannedDevices) {
        this.adapter = adapter;
        this.previouslyScannedDevices = previouslyScannedDevices;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);
        String address = result.getDevice().getAddress();
        addScanResult(address, result);
        adapter.notifyDataSetChanged();
    }

    static private void logScanResult(ScanResult result) {
        ScanRecord record = result.getScanRecord();

        if (record != null) {
            ByteStringBuilder bsb = new ByteStringBuilder()
                    .append("onScanResult")
                    .append("(" + result.getDevice().getAddress() + ") -> ")
                    .append(record.getBytes());
            Log.d(TAG, bsb.toString());
        }
    }

    private void addScanResult(String macAddress, ScanResult scanResult) {
        boolean hasPreviousScanRecordFromSameDevice = false;
        for (ScannedDevice device : previouslyScannedDevices) {
            if (device.getMacAddress().equals(macAddress)) {
                device.addScanResult(scanResult);
                hasPreviousScanRecordFromSameDevice = true;
                break;
            }
        }

        if (!hasPreviousScanRecordFromSameDevice) {

            ScannedDevice device = new ScannedDevice(macAddress);
            device.addScanResult(scanResult);
            previouslyScannedDevices.add(device);
        }

        Collections.sort(previouslyScannedDevices, new ScannedDevice.ScannedDeviceComparator());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.d(TAG, "Scan failed (stopScan): error " + errorCode);
    }
}


