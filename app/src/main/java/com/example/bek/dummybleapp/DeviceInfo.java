package com.example.bek.dummybleapp;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.util.SparseArray;

import java.util.List;
import java.util.Map;

public class DeviceInfo {
    public BluetoothDevice device;
    public String device_name;
    public String device_address;
    public int rssi;
    public String flag;
    public List<ParcelUuid> uuid;

    public SparseArray<byte[]> manuf;
    public Map<ParcelUuid, byte[]> service_data;
    public int tx;
    public long timestamp;
    public long deltaT = 0;
    public int position = 0;
    public byte[] scanRecord;
}
