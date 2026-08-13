package com.oxide.esp;

import android.util.Log;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MemoryReader {

    private static final String TAG = "MemoryReader";
    private int pid;
    private long baseAddress;
    private RandomAccessFile memFile;

    public MemoryReader(int pid, long baseAddress) {
        this.pid = pid;
        this.baseAddress = baseAddress;
    }

    public boolean initialize() {
        try {
            memFile = new RandomAccessFile("/proc/" + pid + "/mem", "rw");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to open mem file: " + e.getMessage());
            return false;
        }
    }

    public int readInt(long address) {
        try {
            memFile.seek(address);
            return memFile.readInt();
        } catch (IOException e) {
            Log.e(TAG, "readInt error: " + e.getMessage());
            return 0;
        }
    }

    public long readLong(long address) {
        try {
            memFile.seek(address);
            return memFile.readLong();
        } catch (IOException e) {
            Log.e(TAG, "readLong error: " + e.getMessage());
            return 0;
        }
    }

    public float readFloat(long address) {
        try {
            memFile.seek(address);
            return memFile.readFloat();
        } catch (IOException e) {
            Log.e(TAG, "readFloat error: " + e.getMessage());
            return 0;
        }
    }

    public double readDouble(long address) {
        try {
            memFile.seek(address);
            return memFile.readDouble();
        } catch (IOException e) {
            Log.e(TAG, "readDouble error: " + e.getMessage());
            return 0;
        }
    }

    public byte[] readBytes(long address, int length) {
        try {
            memFile.seek(address);
            byte[] buffer = new byte[length];
            memFile.read(buffer);
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "readBytes error: " + e.getMessage());
            return null;
        }
    }

    public boolean writeInt(long address, int value) {
        try {
            memFile.seek(address);
            memFile.writeInt(value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeInt error: " + e.getMessage());
            return false;
        }
    }

    public boolean writeLong(long address, long value) {
        try {
            memFile.seek(address);
            memFile.writeLong(value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeLong error: " + e.getMessage());
            return false;
        }
    }

    public boolean writeFloat(long address, float value) {
        try {
            memFile.seek(address);
            memFile.writeFloat(value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeFloat error: " + e.getMessage());
            return false;
        }
    }

    public boolean writeBytes(long address, byte[] data) {
        try {
            memFile.seek(address);
            memFile.write(data);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeBytes error: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (memFile != null) {
                memFile.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "close error: " + e.getMessage());
        }
    }

    // ฟังก์ชันอำนวยความสะดวก
    public float readVector3X(long address) {
        return readFloat(address);
    }

    public float readVector3Y(long address) {
        return readFloat(address + 4);
    }

    public float readVector3Z(long address) {
        return readFloat(address + 8);
    }

    public float[] readVector3(long address) {
        return new float[] {
            readFloat(address),
            readFloat(address + 4),
            readFloat(address + 8)
        };
    }

    public String readString(long address, int maxLength) {
        try {
            byte[] bytes = readBytes(address, maxLength);
            if (bytes == null) return "";
            int len = 0;
            while (len < bytes.length && bytes[len] != 0) len++;
            return new String(bytes, 0, len, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "readString error: " + e.getMessage());
            return "";
        }
    }
}
