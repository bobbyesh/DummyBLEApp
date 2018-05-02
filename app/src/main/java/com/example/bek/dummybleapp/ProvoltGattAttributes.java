package com.example.bek.dummybleapp;

import android.os.ParcelUuid;

import java.util.HashMap;
import java.util.UUID;

public class ProvoltGattAttributes {
    public static ParcelUuid OTA_SERVICE = ParcelUuid.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
    public static ParcelUuid OTA_CONTROL = ParcelUuid.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");
    public static ParcelUuid OTA_DATA    = ParcelUuid.fromString("984227F3-34FC-4045-A5D0-2C581F81A153");
    public static ParcelUuid MESH_PROVISIONING_SERVICE = ParcelUuid.fromString(("00001827-0000-1000-8000-00805f9b34fb"));
}
