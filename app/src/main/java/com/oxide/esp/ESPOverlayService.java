package com.oxide.esp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.List;

public class ESPOverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private Handler handler;
    private Runnable updateRunnable;

    // ESP Canvas
    private Paint boxPaint;
    private Paint healthPaint;
    private Paint linePaint;
    private Paint textPaint;
    private Paint healthBgPaint;

    // FOV Circle
    private Paint fovPaint;
    private boolean showFOV = true;

    // ข้อมูลเป้าหมาย
    private List<TargetData> targets = new ArrayList<>();
    private AimAssist aimAssist;

    private static class TargetData {
        float screenX, screenY;
        float health;
        float distance;
        String name;
        boolean isEnemy;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        initPaints();
        createOverlayView();

        // เริ่มอัปเดตข้อมูล
        startUpdating();
    }

    private void initPaints() {
        // Box
        boxPaint = new Paint();
        boxPaint.setColor(Color.argb(255, 255, 165, 0));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(2f);

        // Health
        healthPaint = new Paint();
        healthPaint.setColor(Color.GREEN);
        healthPaint.setStyle(Paint.Style.FILL);
        healthPaint.setStrokeWidth(4f);

        healthBgPaint = new Paint();
        healthBgPaint.setColor(Color.argb(150, 0, 0, 0));
        healthBgPaint.setStyle(Paint.Style.FILL);

        // Line
        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);

        // Text
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);
        textPaint.setAntiAlias(true);

        // FOV
        fovPaint = new Paint();
        fovPaint.setColor(Color.argb(80, 255, 255, 255));
        fovPaint.setStyle(Paint.Style.STROKE);
        fovPaint.setStrokeWidth(1f);
    }

    private void createOverlayView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_esp, null);

        // ตั้งค่า WindowManager
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        // เพิ่ม View เข้าสู่ระบบ
        windowManager.addView(overlayView, params);

        // ตั้งค่าให้วาด Canvas
        overlayView.setWillNotDraw(false);
        overlayView.postInvalidate();

        // ปิด/เปิด FOV
        Button btnFOV = overlayView.findViewById(R.id.btnFOV);
        if (btnFOV != null) {
            btnFOV.setOnClickListener(v -> {
                showFOV = !showFOV;
                Toast.makeText(this, showFOV ? "FOV ON" : "FOV OFF", Toast.LENGTH_SHORT).show();
            });
        }

        // ตั้งค่า Drag
        View dragView = overlayView.findViewById(R.id.esp_drag_area);
        if (dragView != null) {
            dragView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void startUpdating() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTargets();
                overlayView.postInvalidate();
                handler.postDelayed(this, 16); // ~60 FPS
            }
        };
        handler.post(updateRunnable);
    }

    private void updateTargets() {
        if (aimAssist == null) {
            // สร้าง MemoryReader ใหม่
            // TODO: ใช้ MemoryReader ที่ InjectorService สร้างไว้
        }

        // อัปเดตข้อมูลเป้าหมายจาก AimAssist
        if (aimAssist != null) {
            // aimAssist.updateTargets();
            // List<AimAssist.Target> rawTargets = aimAssist.getTargets();
            // แปลงเป็น TargetData
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ("START_ESP".equals(intent.getAction())) {
                // เริ่ม ESP
            } else if ("STOP_ESP".equals(intent.getAction())) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // View ที่วาด ESP
    public class ESPView extends View {

        public ESPView() {
            super(ESPOverlayService.this);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // วาด FOV Circle
            if (showFOV) {
                float centerX = getWidth() / 2f;
                float centerY = getHeight() / 2f;
                float radius = Config.FOV / 2f;
                canvas.drawCircle(centerX, centerY, radius, fovPaint);
            }

            // วาด ESP Box
            for (TargetData target : targets) {
                if (!isOnScreen(target.screenX, target.screenY)) continue;

                float boxSize = 80 - (target.distance / 10f);
                float left = target.screenX - boxSize / 2;
                float top = target.screenY - boxSize;
                float right = target.screenX + boxSize / 2;
                float bottom = target.screenY;

                // กล่อง
                int boxColor = target.isEnemy ? Color.RED : Color.GREEN;
                boxPaint.setColor(boxColor);
                canvas.drawRect(left, top, right, bottom, boxPaint);

                // Health Bar
                float healthWidth = boxSize;
                float healthHeight = 4;
                float healthX = left;
                float healthY = bottom + 4;

                canvas.drawRect(healthX, healthY, healthX + healthWidth, healthY + healthHeight, healthBgPaint);
                healthPaint.setColor(getHealthColor(target.health));
                canvas.drawRect(healthX, healthY, healthX + (healthWidth * target.health / 100f), healthY + healthHeight, healthPaint);

                // ระยะทาง
                textPaint.setTextSize(16);
                String distanceText = Math.round(target.distance) + "m";
                canvas.drawText(distanceText, left, bottom + 24, textPaint);

                // ชื่อ
                textPaint.setTextSize(18);
                canvas.drawText(target.name != null ? target.name : "Enemy", left, top - 8, textPaint);
            }

            // วาดเส้นเชื่อม
            if (Config.ESP_ENABLED) {
                float centerX = getWidth() / 2f;
                float centerY = getHeight() / 2f;
                for (TargetData target : targets) {
                    if (!isOnScreen(target.screenX, target.screenY)) continue;
                    canvas.drawLine(centerX, centerY, target.screenX, target.screenY, linePaint);
                }
            }
        }

        private boolean isOnScreen(float x, float y) {
            return x > 0 && x < getWidth() && y > 0 && y < getHeight();
        }

        private int getHealthColor(float health) {
            if (health > 70) return Color.GREEN;
            if (health > 40) return Color.YELLOW;
            return Color.RED;
        }
    }
}
