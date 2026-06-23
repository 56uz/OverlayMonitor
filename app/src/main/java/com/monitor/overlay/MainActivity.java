package com.monitor.overlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tv_status);
        Button btnStart  = findViewById(R.id.btn_start);
        Button btnStop   = findViewById(R.id.btn_stop);

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
            } else {
                startOverlay();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            tvStatus.setText("状态：已停止");
        });

        updateStatus(tvStatus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvStatus = findViewById(R.id.tv_status);
        updateStatus(tvStatus);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startOverlay();
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能运行", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startOverlay() {
        Intent service = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        TextView tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText("状态：运行中 ✓");
    }

    private void updateStatus(TextView tv) {
        boolean hasPermission = Settings.canDrawOverlays(this);
        tv.setText(hasPermission ? "悬浮窗权限：已授权" : "悬浮窗权限：未授权");
    }
}
