package com.fearkov.zakuro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import java.io.File;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Atividade de emulação com controles externos em camada XML nativa Android.
 * Os controles e os menus são executados FORA da janela de renderização gráfica interna do jogo,
 * garantindo compatibilidade total com toque, estética moderna e visual cristalino das telas do 3DS.
 */
public class NativeActivity extends android.app.NativeActivity {

    private static final String TAG = "ZakuroNative";

    static {
        try {
            System.loadLibrary("main");
            Log.i(TAG, "libmain.so loaded successfully via System.loadLibrary");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to loadLibrary main: " + t.getMessage());
        }
    }

    private Vibrator vibrator;
    private View controllerOverlay;
    private WindowManager windowManager;
    private boolean overlayAttached = false;

    private boolean stickActive = false;
    private int currentStickKey = 0;

    public static native boolean isGameRunning();

    public static boolean checkGameRunning() {
        try {
            return isGameRunning();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "checkGameRunning UnsatisfiedLinkError: " + e.getMessage());
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private final android.os.Handler stateCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable stateCheckRunnable = new Runnable() {
        @Override
        public void run() {
            updateOverlayVisibility();
            stateCheckHandler.postDelayed(this, 150);
        }
    };

    private void updateOverlayVisibility() {
        if (controllerOverlay == null) return;
        boolean isRunning = checkGameRunning();
        int desiredVisibility = isRunning ? View.VISIBLE : View.GONE;
        if (controllerOverlay.getVisibility() != desiredVisibility) {
            controllerOverlay.setVisibility(desiredVisibility);
            Log.i(TAG, "Controller overlay visibility updated: " + (isRunning ? "VISIBLE (Game)" : "GONE (Recompiling / Library)"));
        }
    }

    private void startStateMonitoring() {
        stateCheckHandler.removeCallbacks(stateCheckRunnable);
        stateCheckHandler.post(stateCheckRunnable);
    }

    private void stopStateMonitoring() {
        stateCheckHandler.removeCallbacks(stateCheckRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "NativeActivity onCreate start");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        applyFullScreenImmersive();
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Configura variáveis de ambiente do compilador e caminhos de dados
        try {
            String nativeDir = getApplicationInfo().nativeLibraryDir;
            File ccFile = new File(nativeDir, "libcc.so");
            android.system.Os.setenv("CC", ccFile.getAbsolutePath(), true);
            android.system.Os.setenv("NATIVE_LIB_DIR", nativeDir, true);
            android.system.Os.setenv("ZAKURO_DATA_DIR", getFilesDir().getAbsolutePath(), true);
            android.system.Os.setenv("HOME", getFilesDir().getAbsolutePath(), true);
            android.system.Os.setenv("XDG_DATA_HOME", getFilesDir().getAbsolutePath(), true);
            File cacheDir = new File(getFilesDir(), "cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File recompDir = new File(getFilesDir(), "3dsrecomp");
            if (!recompDir.exists()) recompDir.mkdirs();
            android.system.Os.setenv("XDG_CACHE_HOME", cacheDir.getAbsolutePath(), true);
            android.system.Os.setenv("TMPDIR", cacheDir.getAbsolutePath(), true);

            String romPath = getIntent().getStringExtra("zakuro_selected_rom");
            String action = getIntent().getStringExtra("zakuro_action");
            File romsFolder = new File(getFilesDir(), "roms");
            if (!romsFolder.exists()) romsFolder.mkdirs();
            if (romPath != null && !romPath.isEmpty()) {
                File pathFile = new File(romsFolder, "selected_path.txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pathFile)) {
                    fos.write(romPath.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (action != null && !action.isEmpty()) {
                File actionFile = new File(romsFolder, "action.txt");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(actionFile)) {
                    fos.write(action.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {}

        // Adiciona a interface nativa em XML FORA da janela de renderização via WindowManager
        setupNativeControllerOverlay();
    }

    private void setupNativeControllerOverlay() {
        try {
            Log.i(TAG, "setupNativeControllerOverlay: inflating game_controller_overlay");
            controllerOverlay = getLayoutInflater().inflate(R.layout.game_controller_overlay, null);

            // Mapeamento dos botões de ombro (L e R)
            bindGamepadButton(R.id.btnL, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q);
            bindGamepadButton(R.id.btnR, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W);

            // Mapeamento dos botões de ação do 3DS (ABXY)
            bindGamepadButton(R.id.btnActionA, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_X);
            bindGamepadButton(R.id.btnActionB, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_Z);
            bindGamepadButton(R.id.btnActionX, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_S);
            bindGamepadButton(R.id.btnActionY, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_A);

            // Mapeamento do D-Pad (Direcional em Cruz)
            bindGamepadButton(R.id.btnDpadUp, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP);
            bindGamepadButton(R.id.btnDpadDown, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
            bindGamepadButton(R.id.btnDpadLeft, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT);
            bindGamepadButton(R.id.btnDpadRight, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT);

            // Mapeamento dos botões de sistema (START e SELECT)
            bindGamepadButton(R.id.btnStart, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
            bindGamepadButton(R.id.btnSelect, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_DEL);

            // Menu nativo externo ao compositor gráfico do jogo
            View btnMenu = controllerOverlay.findViewById(R.id.btnMenu);
            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> showInGameMenuDialog());
            }

            // Analógico Virtual (Circle Pad)
            setupCirclePad();

            // Adiciona a camada de controles sobre a janela nativa
            getWindow().getDecorView().post(this::attachOverlay);

            Log.i(TAG, "setupNativeControllerOverlay: successfully initialized!");
        } catch (Throwable t) {
            Log.e(TAG, "setupNativeControllerOverlay error", t);
        }
    }

    private void attachOverlay() {
        if (overlayAttached || controllerOverlay == null || isFinishing()) return;
        try {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            params.token = getWindow().getDecorView().getWindowToken();

            if (params.token != null) {
                boolean running = checkGameRunning();
                controllerOverlay.setVisibility(running ? View.VISIBLE : View.GONE);
                windowManager.addView(controllerOverlay, params);
                overlayAttached = true;
                Log.i(TAG, "Overlay attached via WindowManager successfully! initial visibility=" + (running ? "VISIBLE" : "GONE"));
                startStateMonitoring();
            } else {
                // Tenta novamente caso o token ainda esteja sendo criado pelo decorView
                getWindow().getDecorView().postDelayed(this::attachOverlay, 100);
            }
        } catch (Throwable t) {
            Log.e(TAG, "attachOverlay error", t);
        }
    }

    private void detachOverlay() {
        if (overlayAttached && controllerOverlay != null && windowManager != null) {
            try {
                windowManager.removeViewImmediate(controllerOverlay);
            } catch (Exception e1) {
                try {
                    windowManager.removeView(controllerOverlay);
                } catch (Exception ignored) {}
            }
            overlayAttached = false;
        }
    }

    private void bindGamepadButton(int buttonId, int gamePadKeyCode, int keyboardKeyCode) {
        View btn = controllerOverlay.findViewById(buttonId);
        if (btn == null) return;

        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    triggerHaptic();
                    sendKeyEvent(KeyEvent.ACTION_DOWN, gamePadKeyCode);
                    if (keyboardKeyCode != gamePadKeyCode) {
                        sendKeyEvent(KeyEvent.ACTION_DOWN, keyboardKeyCode);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    sendKeyEvent(KeyEvent.ACTION_UP, gamePadKeyCode);
                    if (keyboardKeyCode != gamePadKeyCode) {
                        sendKeyEvent(KeyEvent.ACTION_UP, keyboardKeyCode);
                    }
                    return true;
            }
            return false;
        });
    }

    private void setupCirclePad() {
        View container = controllerOverlay.findViewById(R.id.circlePadContainer);
        View thumb = controllerOverlay.findViewById(R.id.circlePadThumb);
        if (container == null || thumb == null) return;

        container.setOnTouchListener((v, event) -> {
            float width = v.getWidth();
            float height = v.getHeight();
            float centerX = width / 2.0f;
            float centerY = height / 2.0f;
            float maxRadius = (width / 2.0f) - (thumb.getWidth() / 2.0f);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    stickActive = true;
                    float dx = event.getX() - centerX;
                    float dy = event.getY() - centerY;
                    float distance = (float) Math.hypot(dx, dy);

                    if (distance > maxRadius && distance > 0) {
                        dx = (dx / distance) * maxRadius;
                        dy = (dy / distance) * maxRadius;
                    }

                    thumb.setTranslationX(dx);
                    thumb.setTranslationY(dy);

                    updateStickKeys(dx / maxRadius, dy / maxRadius);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stickActive = false;
                    thumb.animate().translationX(0).translationY(0).setDuration(80).start();
                    releaseStickKeys();
                    return true;
            }
            return false;
        });
    }

    private void updateStickKeys(float normX, float normY) {
        int targetKey = 0;
        float deadzone = 0.35f;

        if (Math.abs(normX) > Math.abs(normY)) {
            if (normX > deadzone) targetKey = KeyEvent.KEYCODE_L;      // Circle Right
            else if (normX < -deadzone) targetKey = KeyEvent.KEYCODE_J; // Circle Left
        } else {
            if (normY > deadzone) targetKey = KeyEvent.KEYCODE_K;       // Circle Down
            else if (normY < -deadzone) targetKey = KeyEvent.KEYCODE_I; // Circle Up
        }

        if (targetKey != currentStickKey) {
            if (currentStickKey != 0) {
                sendKeyEvent(KeyEvent.ACTION_UP, currentStickKey);
            }
            if (targetKey != 0) {
                sendKeyEvent(KeyEvent.ACTION_DOWN, targetKey);
            }
            currentStickKey = targetKey;
        }
    }

    private void releaseStickKeys() {
        if (currentStickKey != 0) {
            sendKeyEvent(KeyEvent.ACTION_UP, currentStickKey);
            currentStickKey = 0;
        }
    }

    private void sendKeyEvent(int action, int keyCode) {
        KeyEvent event = new KeyEvent(action, keyCode);
        dispatchKeyEvent(event);
    }

    private void triggerHaptic() {
        if (!hapticEnabled) return;
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(18);
            }
        }
    }

    /**
     * Menu nativo do Android fora do compositor gráfico do emulador.
     */
    private void showInGameMenuDialog() {
        triggerHaptic();
        String[] options = {
            "▶  Continuar Jogo",
            "⚙️  Opacidade dos Controles",
            "📳  Alternar Vibração ao Tocar",
            "🔄  Reiniciar Emulação",
            "🚪  Sair para a Biblioteca"
        };

        new AlertDialog.Builder(this)
            .setTitle("Zakuro 3DS Recomp")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        applyFullScreenImmersive();
                        dialog.dismiss();
                        break;
                    case 1:
                        showOpacityDialog();
                        break;
                    case 2:
                        toggleHapticFeedback();
                        break;
                    case 3:
                        recreate();
                        break;
                    case 4:
                        finish();
                        break;
                }
            })
            .setCancelable(true)
            .setOnDismissListener(d -> applyFullScreenImmersive())
            .show();
    }

    private boolean hapticEnabled = true;

    private void toggleHapticFeedback() {
        hapticEnabled = !hapticEnabled;
        android.widget.Toast.makeText(this, hapticEnabled ? "Vibração ativada" : "Vibração desativada", android.widget.Toast.LENGTH_SHORT).show();
        applyFullScreenImmersive();
    }

    private void showOpacityDialog() {
        String[] levels = {"100% (Padrão)", "75%", "50%", "30%", "Invisível (Toque Ativo)"};
        final float[] opacities = {1.0f, 0.75f, 0.50f, 0.30f, 0.05f};
        new AlertDialog.Builder(this)
            .setTitle("Opacidade dos Controles")
            .setItems(levels, (d, w) -> {
                if (controllerOverlay != null && w >= 0 && w < opacities.length) {
                    controllerOverlay.setAlpha(opacities[w]);
                }
                applyFullScreenImmersive();
            })
            .setOnDismissListener(d -> applyFullScreenImmersive())
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        applyFullScreenImmersive();
        if (!overlayAttached) {
            getWindow().getDecorView().post(this::attachOverlay);
        }
        startStateMonitoring();
    }

    @Override
    protected void onPause() {
        stopStateMonitoring();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyFullScreenImmersive();
            if (!overlayAttached) {
                getWindow().getDecorView().post(this::attachOverlay);
            }
            startStateMonitoring();
        }
    }

    @Override
    protected void onDestroy() {
        stopStateMonitoring();
        detachOverlay();
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyFullScreenImmersive();
    }

    private void applyFullScreenImmersive() {
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(flags);
        }
    }
}
