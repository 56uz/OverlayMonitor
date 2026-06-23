package com.monitor.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "overlay_monitor";
    private static final int NOTIF_ID = 1;
    private static final long UPDATE_MS = 1000;

    private WindowManager windowManager;
    private View overlayView;
    private Handler handler;
    private Runnable updateTask;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private WindowManager.LayoutParams params;
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float currentFps = 0f;
    private android.view.Choreographer choreographer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        setupOverlayView();
        startFpsCounter();
        startDataUpdate();
    }

    private void setupOverlayView() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 120;
        overlayView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int)(event.getRawX() - initialTouchX);
                    params.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(overlayView, params);
                    return true;
            }
            return false;
        });
        windowManager.addView(overlayView, params);
    }

    private void startFpsCounter() {
        choreographer = android.view.Choreographer.getInstance();
        lastFrameTime = System.currentTimeMillis();
        frameCount = 0;
        android.view.Choreographer.FrameCallback callback = new android.view.Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                frameCount++;
                long now = System.currentTimeMillis();
                long elapsed = now - lastFrameTime;
                if (elapsed >= UPDATE_MS) {
                    currentFps = frameCount * 1000f / elapsed;
                    frameCount = 0;
                    lastFrameTime = now;
                }
                choreographer.postFrameCallback(this);
            }
        };
        choreographer.postFrameCallback(callback);
    }

    private void startDataUpdate() {
        updateTask = new Runnable() {
            @Override
            public void run() {
                updateOverlay();
                handler.postDelayed(this, UPDATE_MS);
            }
        };
        handler.post(updateTask);
    }

    private void updateOverlay() {
        if (overlayView == null) return;
        TextView tvFps = overlayView.findViewById(R.id.tv_fps);
        TextView tvCpu = overlayView.findViewById(R.id.tv_cpu);
        TextView tvGpu = overlayView.findViewById(R.id.tv_gpu);
        TextView tvBat = overlayView.findViewById(R.id.tv_bat);
        tvFps.setText(String.format("FPS  %d", (int) currentFps));
        colorFps(tvFps, currentFps);
        int cpuTemp = ThermalReader.getCpuTemp();
        int gpuTemp = ThermalReader.getGpuTemp();
        int batTemp = ThermalReader.getBatteryTemp();
        tvCpu.setText(cpuTemp >= 0 ? String.format("CPU  %d°C", cpuTemp) : "CPU  N/A");
        tvGpu.setText(gpuTemp >= 0 ? String.format("GPU  %d°C", gpuTemp) : "GPU  N/A");
        tvBat.setText(batTemp >= 0 ? String.format("BAT  %d°C", batTemp) : "BAT  N/A");
        colorTemp(tvCpu, cpuTemp);
        colorTemp(tvGpu, gpuTemp);
        colorTemp(tvBat, batTemp);
    }

    private void colorFps(TextView tv, float fps) {
        if (fps >= 55) tv.setTextColor(0xFF4CAF50);
        else if (fps >= 30) tv.setTextColor(0xFFFFEB3B);
        else tv.setTextColor(0xFFF44336);
    }

    private void colorTemp(TextView tv, int temp) {
        if (temp < 0) { tv.setTextColor(0xFF9E9E9E); return; }
        if (temp >= 55) tv.setTextColor(0xFFF44336);
        else if (temp >= 45) tv.setTextColor(0xFFFFEB3B);
        else tv.setTextColor(0xFF4CAF50);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "性能监控", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("性能监控运行中")
                .setContentText("点击管理")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateTask != null) handler.removeCallbacks(updateTask);
        if (overlayView != null) windowManager.removeView(overlayView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
