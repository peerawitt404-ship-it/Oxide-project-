package com.oxide.esp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvGameStatus;
    private Button btnInject, btnESP, btnAimbot, btnHead, btnBody;
    private SeekBar sbFOV, sbSmooth;
    private TextView tvFOV, tvSmooth;

    private boolean isInjected = false;
    private boolean isEspOn = false;
    private boolean isAimbotOn = false;
    private boolean aimHead = true;

    private static final int REQUEST_OVERLAY_PERMISSION = 1;
    private static final int REQUEST_USAGE_STATS = 2;
    private static final String[] PERMISSIONS = {
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.QUERY_ALL_PACKAGES,
        Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkPermissions();
        checkGameStatus();
        setupListeners();

        // เริ่ม Service
        startService(new Intent(this, InjectorService.class));
    }

    private void initViews() {
        tvGameStatus = findViewById(R.id.tvGameStatus);
        btnInject = findViewById(R.id.btnInject);
        btnESP = findViewById(R.id.btnESP);
        btnAimbot = findViewById(R.id.btnAimbot);
        btnHead = findViewById(R.id.btnHead);
        btnBody = findViewById(R.id.btnBody);
        sbFOV = findViewById(R.id.sbFOV);
        sbSmooth = findViewById(R.id.sbSmooth);
        tvFOV = findViewById(R.id.tvFOV);
        tvSmooth = findViewById(R.id.tvSmooth);
    }

    private void checkPermissions() {
        // ตรวจสอบสิทธิ์ Overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            }
        }

        // ตรวจสอบสิทธิ์ Usage Stats
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!checkUsageStatsPermission()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, REQUEST_USAGE_STATS);
            }
        }

        // ขอสิทธิ์ทั่วไป
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 100);
                break;
            }
        }
    }

    private boolean checkUsageStatsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return true;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        // วิธีเช็คที่ง่ายกว่า
        return true;
    }

    private void checkGameStatus() {
        if (isGameRunning()) {
            tvGameStatus.setText("✅ กำลังเล่นเกมอยู่");
            tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            tvGameStatus.setText("❌ ยังไม่ได้เปิดเกม");
            tvGameStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private boolean isGameRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningAppProcessInfo process : am.getRunningAppProcesses()) {
            if (process.processName.equals(Config.GAME_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    private void setupListeners() {
        // ปุ่ม Inject
        btnInject.setOnClickListener(v -> {
            if (isGameRunning()) {
                if (!isInjected) {
                    performInject();
                } else {
                    Toast.makeText(this, "🔄 Inject ไปแล้ว!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "⚠️ เปิดเกมก่อน!", Toast.LENGTH_LONG).show();
            }
        });

        // ปุ่ม ESP
        btnESP.setOnClickListener(v -> {
            if (!isInjected) {
                Toast.makeText(this, "⚠️ Inject ก่อน!", Toast.LENGTH_SHORT).show();
                return;
            }
            isEspOn = !isEspOn;
            Config.ESP_ENABLED = isEspOn;
            if (isEspOn) {
                startESPService();
                btnESP.setText("👁️ ESP: ON");
                btnESP.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
            } else {
                stopESPService();
                btnESP.setText("👁️ ESP: OFF");
                btnESP.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500));
            }
        });

        // ปุ่ม Aimbot
        btnAimbot.setOnClickListener(v -> {
            if (!isInjected) {
                Toast.makeText(this, "⚠️ Inject ก่อน!", Toast.LENGTH_SHORT).show();
                return;
            }
            isAimbotOn = !isAimbotOn;
            Config.AIMBOT_ENABLED = isAimbotOn;
            if (isAimbotOn) {
                btnAimbot.setText("🎯 Aimbot: ON");
                btnAimbot.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
                Toast.makeText(this, "🎯 Aimbot เปิดแล้ว!", Toast.LENGTH_SHORT).show();
            } else {
                btnAimbot.setText("🎯 Aimbot: OFF");
                btnAimbot.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
                Toast.makeText(this, "🔴 Aimbot ปิดแล้ว", Toast.LENGTH_SHORT).show();
            }
        });

        // ปุ่ม Head
        btnHead.setOnClickListener(v -> {
            aimHead = true;
            Config.AIMBOT_HEAD = true;
            Config.AIMBOT_BODY = false;
            btnHead.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
            btnBody.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
            Toast.makeText(this, "🎯 เล็งหัว", Toast.LENGTH_SHORT).show();
        });

        // ปุ่ม Body
        btnBody.setOnClickListener(v -> {
            aimHead = false;
            Config.AIMBOT_HEAD = false;
            Config.AIMBOT_BODY = true;
            btnBody.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
            btnHead.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
            Toast.makeText(this, "🎯 เล็งตัว", Toast.LENGTH_SHORT).show();
        });

        // FOV Slider
        sbFOV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Config.FOV = progress;
                tvFOV.setText("FOV: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Smooth Slider
        sbSmooth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Config.SMOOTH = progress;
                tvSmooth.setText("Smooth: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void performInject() {
        Intent intent = new Intent(this, InjectorService.class);
        intent.setAction("INJECT");
        startService(intent);
        isInjected = true;
        btnInject.setText("✅ INJECTED");
        btnInject.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        Toast.makeText(this, "✅ Inject สำเร็จ!", Toast.LENGTH_LONG).show();
    }

    private void startESPService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                return;
            }
        }
        Intent intent = new Intent(this, ESPOverlayService.class);
        intent.setAction("START_ESP");
        startService(intent);
    }

    private void stopESPService() {
        Intent intent = new Intent(this, ESPOverlayService.class);
        intent.setAction("STOP_ESP");
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, InjectorService.class));
    }
}
