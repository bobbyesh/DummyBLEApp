package com.example.bek.dummybleapp;

import java.nio.ByteBuffer;
import java.util.Arrays;


/*
    Refer to Figure 7.1: PB-GATT Advertising Data in the Mesh Profile specification (page 271)
 */
public class ServiceDataParser {
    private byte[] meshProvisioningUuid = new byte[2];
    private byte[] deviceUuid = new byte[16];
    private byte[] oobInfo = new byte[2];

    ServiceDataParser(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.get(meshProvisioningUuid);
        bb.get(deviceUuid);
        bb.get(oobInfo);
    }

    @Override
    public String toString() {
        return "ServiceDataParser{" +
                "meshProvisioningUuid=" + Arrays.toString(meshProvisioningUuid) +
                ", deviceUuid=" + Arrays.toString(deviceUuid) +
                ", oobInfo=" + Arrays.toString(oobInfo) +
                '}';
    }

    String getDeviceUuidHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b: getDeviceUuid()) {
            sb.append(String.format("%x", b & 0xFF));
        }

        return sb.toString();
    }

    public byte[] getMeshProvisioningUuid() {
        return meshProvisioningUuid;
    }

    public byte[] getDeviceUuid() {
        return deviceUuid;
    }

    public byte[] getOobInfo() {
        return oobInfo;
    }
}
