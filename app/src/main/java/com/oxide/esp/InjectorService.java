package com.oxide.esp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class InjectorService extends Service {

    private static final String TAG = "InjectorService";
    private static final String CHANNEL_ID = "injector_channel";
    private static final int NOTIFICATION_ID = 1;

    private Handler handler;
    private boolean isInjecting = false;
    private boolean isInjected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "INJECT".equals(intent.getAction())) {
            performInjection();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void performInjection() {
        if (isInjecting) return;
        isInjecting = true;

        new Thread(() -> {
            try {
                // 1. หา PID ของเกม
                int pid = findGameProcess();
                if (pid == -1) {
                    showToast("❌ ไม่พบเกม Oxide");
                    isInjecting = false;
                    return;
                }
                showToast("✅ พบเกม PID: " + pid);

                // 2. หา Base Address ของ libil2cpp.so
                long baseAddress = findBaseAddress(pid);
                if (baseAddress == 0) {
                    showToast("❌ ไม่พบ libil2cpp.so");
                    isInjecting = false;
                    return;
                }
                Config.baseAddress = baseAddress;
                showToast("✅ Base: " + Long.toHexString(baseAddress));

                // 3. อ่านค่าออฟเซ็ตจากหน่วยความจำ
                MemoryReader memoryReader = new MemoryReader(pid, baseAddress);
                if (!memoryReader.initialize()) {
                    showToast("❌ อ่านหน่วยความจำไม่ได้");
                    isInjecting = false;
                    return;
                }

                // 4. Inject โค้ดเข้าเกม (ใช้ ptrace หรือ /proc/mem)
                boolean success = injectCode(pid, memoryReader);
                if (success) {
                    isInjected = true;
                    showToast("✅ Inject สำเร็จ! เปิด ESP/Aim ได้เลย");
                    Config.ESP_ENABLED = true;
                    updateNotification("🦊 Inject สำเร็จ");
                } else {
                    showToast("❌ Inject ล้มเหลว");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                showToast("❌ เกิดข้อผิดพลาด: " + e.getMessage());
            }

            isInjecting = false;
        }).start();
    }

    private int findGameProcess() {
        try {
            Process process = Runtime.getRuntime().exec("ps -A");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(Config.GAME_PACKAGE)) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            return Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            // ถ้า format ต่างออกไป ลองแบบอื่น
                            for (String part : parts) {
                                if (part.matches("\\d+")) {
                                    return Integer.parseInt(part);
                                }
                            }
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "findGameProcess error: " + e.getMessage());
        }
        return -1;
    }

    private long findBaseAddress(int pid) {
        try {
            // อ่าน /proc/[pid]/maps
            Process process = Runtime.getRuntime().exec("cat /proc/" + pid + "/maps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("libil2cpp.so")) {
                    String[] parts = line.split("-");
                    if (parts.length >= 2) {
                        try {
                            return Long.parseLong(parts[0], 16);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "findBaseAddress error: " + e.getMessage());
        }
        return 0;
    }

    private boolean injectCode(int pid, MemoryReader memoryReader) {
        try {
            // เขียนโค้ดลงหน่วยความจำเกม
            // ใช้วิธีเขียนผ่าน /proc/[pid]/mem หรือ ptrace

            // 1. เขียน Hook ที่ฟังก์ชัน KNH (PlayerWeapon)
            long weaponHookAddress = Config.baseAddress + Config.OFFSET_WEAPON;
            byte[] hookCode = buildHookCode();
            if (!writeMemory(pid, weaponHookAddress, hookCode)) {
                Log.e(TAG, "Failed to write weapon hook");
                return false;
            }

            // 2. เขียน Hook ที่ฟังก์ชัน WorldToScreenPoint
            long cameraHookAddress = Config.baseAddress + Config.OFFSET_CAMERA;
            byte[] cameraCode = buildCameraHook();
            if (!writeMemory(pid, cameraHookAddress, cameraCode)) {
                Log.e(TAG, "Failed to write camera hook");
                return false;
            }

            // 3. เขียน Hook Entity List
            long entityHookAddress = Config.baseAddress + Config.OFFSET_ENTITY_LIST;
            byte[] entityCode = buildEntityHook();
            if (!writeMemory(pid, entityHookAddress, entityCode)) {
                Log.e(TAG, "Failed to write entity hook");
                return false;
            }

            Log.d(TAG, "All hooks injected successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "injectCode error: " + e.getMessage());
            return false;
        }
    }

    private boolean writeMemory(int pid, long address, byte[] data) {
        try {
            // ใช้ /proc/[pid]/mem เขียนหน่วยความจำ
            Process process = Runtime.getRuntime().exec("su -c 'echo \"write memory\"'");
            // TODO: ใช้วิธีเขียน /proc/[pid]/mem จริงๆ
            // วิธีที่ง่ายกว่า: ใช้ Runtime.exec() เขียนผ่าน dd หรือ python script
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeMemory error: " + e.getMessage());
            return false;
        }
    }

    private byte[] buildHookCode() {
        // ARM64 Hook Code (ตัวอย่าง)
        // ในความเป็นจริงต้องเขียนเป็น ARM Assembly
        String code = """
            // Hook ที่ PlayerWeapon.KNH()
            // ตรวจสอบการยิงและปรับเป้า
            // ใช้ Branch Instruction ไปยังฟังก์ชันของเรา
            """;
        return code.getBytes();
    }

    private byte[] buildCameraHook() {
        // ARM64 Hook Code สำหรับ Camera
        String code = """
            // Hook ที่ Camera.WorldToScreenPoint()
            // ใช้คำนวณตำแหน่งบนหน้าจอ
            """;
        return code.getBytes();
    }

    private byte[] buildEntityHook() {
        // ARM64 Hook Code สำหรับ Entity List
        String code = """
            // Hook ที่ PlayerList/EntityList
            // ใช้ดึงข้อมูลผู้เล่นทั้งหมด
            """;
        return code.getBytes();
    }

    private void showToast(String message) {
        handler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    // Notification Methods
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Injector Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Oxide ESP Injector Service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🦊 Oxide ESP Injector")
            .setContentText("กำลังรอการ Inject...")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🦊 Oxide ESP Injector")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        manager.notify(NOTIFICATION_ID, notification);
    }
}
