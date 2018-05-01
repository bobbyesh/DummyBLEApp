package com.example.bek.dummybleapp;

import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class ScanRecordParser {
    private static final String TAG = ScanRecordParser.class.getSimpleName();

    public static final int FLAG = 0x01;
    public static final int INCOMPLETE_SERVICE_CLASS_UUIDS = 0x02;
    public static final int COMPLETE_SERVICE_CLASS_UUIDS = 0x03;
    public static final int INCOMPLETE_32_BIT_SERVICE_CLASS_UUID = 0x04;
    public static final int COMPLETE_32_BIT_SERVICE_CLASS_UUID = 0x05;
    public static final int INCOMPLETE_128_BIT_SERVICE_CLASS_UUID = 0x06;
    public static final int COMPLETE_128_BIT_SERVICE_CLASS_UUID = 0x07;
    public static final int SHORTENED_LOCAL_NAME = 0x08;
    public static final int COMPLETE_LOCAL_NAME = 0x09;
    public static final int TX_POWER_LEVEL = 0x0A;
    public static final int CLASS_OF_DEVICE = 0x0D;
    public static final int SIMPLE_PAIRING_HASH = 0x0E;
    public static final int SIMPLE_PAIRING_RANDOMIZER = 0x0F;
    public static final int DEVICE_ID_SM_TK_VALUE = 0x10;
    public static final int SM_OOB_FLAGS = 0x11;
    public static final int SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;
    public static final int SERVICE_SOLICITATION_UUIDS_16_BIT = 0x14;
    public static final int SERVICE_SOLICITATION_UUIDS_32_BIT = 0x1F;
    public static final int SERVICE_SOLICATION_UUIDS_128_BIT = 0x15;
    public static final int SERVICE_DATA_UUID_16_BIT = 0x16;
    public static final int SERVICE_DATA_UUID_32_BIT = 0x20;
    public static final int SERVICE_DATA_128_BIT = 0x21;
    public static final int LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE = 0x22;
    public static final int LE_SECURE_CONNECTIONS_RANDOM_VALUE = 0x23;
    public static final int URI = 0x24;
    public static final int INDOOR_POSITIONING = 0x25;
    public static final int TRANSPORT_DISCOVERY_DATA = 0x26;
    public static final int PUBLIC_TARGET_ADDRESS = 0x17;
    public static final int RANDOM_TARGET_ADDRESS = 0x18;
    public static final int APPEARANCE = 0x19;
    public static final int ADVERTISING_INTERVAL = 0x1A;
    public static final int LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;
    public static final int LE_ROLE = 0x1C;
    public static final int SIMPLE_PAIRING_HASH_C256 = 0x1D;
    public static final int SIMPLE_PAIRING_RANDOMIZER_R256 = 0x1E;
    public static final int MANUFACTURER_SPECIFIC_DATA = 0xFF;
    public static final int PB_ADV = 0x29;
    public static final int MESH_MESSAGE = 0x2A;
    public static final int MESH_BEACON = 0x2B;
    public static final int THREED_INFORMATION_DATA = 0x3D;

    static String getAdvType(byte type) {
        return getAdvType(type & 0xFF);
    }

    static String getAdvType(int type) {
        switch (type)
        {
            case FLAG: return "Flags";
            case INCOMPLETE_SERVICE_CLASS_UUIDS: return "Incomplete List of 16-bit Service Class UUIDs";
            case COMPLETE_SERVICE_CLASS_UUIDS: return "Complete List of 16-bit Service Class UUIDs";
            case INCOMPLETE_32_BIT_SERVICE_CLASS_UUID: return "Incomplete List of 32-bit Service Class UUIDs";
            case COMPLETE_32_BIT_SERVICE_CLASS_UUID: return "Complete List of 32-bit Service Class UUIDs";
            case INCOMPLETE_128_BIT_SERVICE_CLASS_UUID: return "Incomplete List of 128-bit Service Class UUIDs";
            case COMPLETE_128_BIT_SERVICE_CLASS_UUID: return "Complete List of 128-bit Service Class UUIDs";
            case SHORTENED_LOCAL_NAME: return "Shortened Local Name";
            case COMPLETE_LOCAL_NAME: return "Complete Local Name";
            case TX_POWER_LEVEL: return "Tx Power Level";
            case CLASS_OF_DEVICE: return "Class of Device";
            case SIMPLE_PAIRING_HASH: return "Simple Pairing Hash C/C-192";
            case SIMPLE_PAIRING_RANDOMIZER: return "Simple Pairing Randomizer R/R-192";
            case DEVICE_ID_SM_TK_VALUE: return "Device ID/Security Manager TK Value";
            case SM_OOB_FLAGS: return "Security Manager Out of Band Flags";
            case SLAVE_CONNECTION_INTERVAL_RANGE: return "Slave Connection Interval Range";
            case SERVICE_SOLICITATION_UUIDS_16_BIT: return "List of 16-bit Service Solicitation UUIDs";
            case SERVICE_SOLICITATION_UUIDS_32_BIT: return "List of 32-bit Service Solicitation UUIDs";
            case SERVICE_SOLICATION_UUIDS_128_BIT: return "List of 128-bit Service Solicitation UUIDs";
            case SERVICE_DATA_UUID_16_BIT: return "Service Data - 16-bit UUID";
            case SERVICE_DATA_UUID_32_BIT: return "Service Data - 32-bit UUID";
            case SERVICE_DATA_128_BIT: return "Service Data - 128-bit UUID";
            case LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE: return "LE Secure Connections Confirmation Value";
            case LE_SECURE_CONNECTIONS_RANDOM_VALUE: return "LE Secure Connections Random Value";
            case URI: return "URI";
            case INDOOR_POSITIONING: return "Indoor Positioning";
            case TRANSPORT_DISCOVERY_DATA: return "Transport Discovery Data";
            case PUBLIC_TARGET_ADDRESS: return "Public Target Address";
            case RANDOM_TARGET_ADDRESS: return "Random Target Address";
            case APPEARANCE: return "Appearance";
            case ADVERTISING_INTERVAL: return "Advertising Interval";
            case LE_BLUETOOTH_DEVICE_ADDRESS: return "LE Bluetooth Device Address";
            case LE_ROLE: return "LE Role";
            case SIMPLE_PAIRING_HASH_C256: return "Simple Pairing Hash C-256";
            case SIMPLE_PAIRING_RANDOMIZER_R256: return "Simple Pairing Randomizer R-256";
            case PB_ADV: return "PB-ADV";
            case MESH_MESSAGE: return "Mesh Message";
            case MESH_BEACON: return "Mesh Beacon";
            case THREED_INFORMATION_DATA: return "3D Information Data";
            case MANUFACTURER_SPECIFIC_DATA: return "Manufacturer Specific Data";

            default:
                return "Unknown type: " + String.format("0x%02x", type);
        }
    }

    static String deviceNameFromManufacturerData(byte[] manufacturerSpecificData) {
        /*
        Example Scan Record:
        216 3327 181516271853696c6162734465762da9ba199ffd9000000000000000000000000000000000000
         */
        StringBuilder sb = new StringBuilder();
        for (byte b: manufacturerSpecificData) {
            sb.append(Byte.toString(b));

        }
        return sb.toString();
    }

    static ArrayList<Pair<Integer, byte[]>> packetize(byte[] bytes) {
        ArrayList<Pair<Integer, byte[]>> list = new ArrayList<>();

        ByteBuffer bb = ByteBuffer.wrap(bytes);

        while(bb.position() < bytes.length) {
            int length = bb.get() & 0xFF;
            if (length <= 1) {
                break;
            }
            int type = bb.get() & 0xFF;

            byte[] payload = new byte[length-1];
            bb.get(payload);

            list.add(new Pair<>(type & 0xFF, payload));
        }

        return list;
    }
}