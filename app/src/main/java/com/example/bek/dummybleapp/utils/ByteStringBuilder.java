package com.example.bek.dummybleapp.utils;

public class ByteStringBuilder {

    private final StringBuilder sb;

    public ByteStringBuilder() {
        sb = new StringBuilder();
    }

    public ByteStringBuilder append(byte[] bytes) {
        for(byte b: bytes) {
            String hex = String.format("%x", b & 0xFF);
            sb.append(hex);
        }

        return this;
    }

    public ByteStringBuilder append(byte b) {
        String hex = String.format("%x", b & 0xFF);
        sb.append(hex);

        return this;
    }

    public ByteStringBuilder append(String s) {
        sb.append(s);
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
