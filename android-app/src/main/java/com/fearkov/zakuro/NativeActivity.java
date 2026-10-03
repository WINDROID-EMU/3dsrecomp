package com.fearkov.zakuro;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Atividade de emulação com controles externos em camada XML nativa Android.
 * Os controles e os menus são executados FORA da janela de renderização gráfica interna do jogo,
 * garantindo compatibilidade total com toque, estética moderna e visual cristalino das telas do 3DS.
 */
public class NativeActivity extends android.app.NativeActivity {

    private static final String TAG = "ZakuroNative";

    static {
        try {
            android.system.Os.setenv("RUST_LOG", "info,zakuro=debug,zakuro_gpu=info,zakuro_core::services::dsp=warn", true);
            android.system.Os.setenv("RUST_BACKTRACE", "1", true);
        } catch (Throwable ignored) {}
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
    public static native byte consumeSettingsRequest();
    public static native void reloadSettings();
    public static native void nativeSetButtonState(int buttonMask, byte pressed);
    public static native void nativeSetCirclePad(float x, float y);
    public static native void nativeSetTouch(byte active, int x, int y);
    public static native float nativeGetFps();
    public static native float nativeGetShownFps();

    private TextView tvFpsOverlay;

    public static final int BUTTON_A = 1 << 0;
    public static final int BUTTON_B = 1 << 1;
    public static final int BUTTON_SELECT = 1 << 2;
    public static final int BUTTON_START = 1 << 3;
    public static final int BUTTON_RIGHT = 1 << 4;
    public static final int BUTTON_LEFT = 1 << 5;
    public static final int BUTTON_UP = 1 << 6;
    public static final int BUTTON_DOWN = 1 << 7;
    public static final int BUTTON_R = 1 << 8;
    public static final int BUTTON_L = 1 << 9;
    public static final int BUTTON_X = 1 << 10;
    public static final int BUTTON_Y = 1 << 11;

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
            updateFpsCounter();
            try {
                if (consumeSettingsRequest() != 0) {
                    showInGameSettingsDialog();
                }
            } catch (Throwable ignored) {}
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

    private void updateFpsCounter() {
        if (tvFpsOverlay == null) return;
        boolean isRunning = checkGameRunning();
        ZakuroSettings settings = ZakuroSettings.load(this);
        if (isRunning && settings.show_fps) {
            if (tvFpsOverlay.getVisibility() != View.VISIBLE) {
                tvFpsOverlay.setVisibility(View.VISIBLE);
            }
            try {
                float fps = nativeGetFps();
                if (fps > 0.0f) {
                    tvFpsOverlay.setText(String.format(java.util.Locale.US, "%.1f FPS", fps));
                } else {
                    tvFpsOverlay.setText("-- FPS");
                }
            } catch (Throwable t) {
                tvFpsOverlay.setText("-- FPS");
            }
        } else {
            if (tvFpsOverlay.getVisibility() != View.GONE) {
                tvFpsOverlay.setVisibility(View.GONE);
            }
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
        try {
            ZakuroSettings initSettings = ZakuroSettings.load(this);
            GpuDriverManager.applyDriverEnv(this, initSettings);
            if (initSettings.vulkan_present_mode != null) {
                android.system.Os.setenv("ZAKURO_VULKAN_PRESENT_MODE", initSettings.vulkan_present_mode, true);
            }
            android.system.Os.setenv("ZAKURO_DEBUG_METRICS", initSettings.debug_metrics ? "1" : "0", true);
        } catch (Exception e) {
            Log.w(TAG, "Could not set GPU driver env: " + e.getMessage());
        }
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
            android.system.Os.setenv("RUST_LOG", "info,zakuro=debug,zakuro_gpu=info,zakuro_core::services::dsp=warn", true);
            android.system.Os.setenv("RUST_BACKTRACE", "1", true);

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
            bindGamepadButton(R.id.btnL, BUTTON_L, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q);
            bindGamepadButton(R.id.btnR, BUTTON_R, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_W);

            // Mapeamento dos botões de ação do 3DS (ABXY)
            bindGamepadButton(R.id.btnActionA, BUTTON_A, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_X);
            bindGamepadButton(R.id.btnActionB, BUTTON_B, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_Z);
            bindGamepadButton(R.id.btnActionX, BUTTON_X, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_S);
            bindGamepadButton(R.id.btnActionY, BUTTON_Y, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_A);

            // Mapeamento do D-Pad (Direcional em Cruz)
            bindGamepadButton(R.id.btnDpadUp, BUTTON_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP);
            bindGamepadButton(R.id.btnDpadDown, BUTTON_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
            bindGamepadButton(R.id.btnDpadLeft, BUTTON_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT);
            bindGamepadButton(R.id.btnDpadRight, BUTTON_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT);

            // Mapeamento dos botões de sistema (START e SELECT)
            bindGamepadButton(R.id.btnStart, BUTTON_START, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
            bindGamepadButton(R.id.btnSelect, BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_DEL);

            // Menu nativo externo ao compositor gráfico do jogo
            View btnMenu = controllerOverlay.findViewById(R.id.btnMenu);
            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> showInGameMenuDialog());
            }

            tvFpsOverlay = controllerOverlay.findViewById(R.id.tvFpsOverlay);

            // Analógico Virtual (Circle Pad)
            setupCirclePad();

            // Adiciona a camada de controles sobre a janela nativa
            getWindow().getDecorView().post(this::attachOverlay);

            Log.i(TAG, "setupNativeControllerOverlay: successfully initialized with native JNI input!");
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

    private void bindGamepadButton(int buttonId, int buttonMask, int gamePadKeyCode, int keyboardKeyCode) {
        View btn = controllerOverlay.findViewById(buttonId);
        if (btn == null) return;

        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    triggerHaptic();
                    try {
                        nativeSetButtonState(buttonMask, (byte) 1);
                    } catch (Throwable t) {
                        Log.w(TAG, "nativeSetButtonState down failed: " + t.getMessage());
                    }
                    sendKeyEvent(KeyEvent.ACTION_DOWN, gamePadKeyCode);
                    if (keyboardKeyCode != gamePadKeyCode) {
                        sendKeyEvent(KeyEvent.ACTION_DOWN, keyboardKeyCode);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    try {
                        nativeSetButtonState(buttonMask, (byte) 0);
                    } catch (Throwable t) {
                        Log.w(TAG, "nativeSetButtonState up failed: " + t.getMessage());
                    }
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

                    float normX = dx / maxRadius;
                    float normY = -dy / maxRadius; // Up is positive in 3DS circle pad
                    try {
                        nativeSetCirclePad(normX, normY);
                    } catch (Throwable t) {
                        Log.w(TAG, "nativeSetCirclePad failed: " + t.getMessage());
                    }

                    updateStickKeys(normX, -normY);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stickActive = false;
                    thumb.animate().translationX(0).translationY(0).setDuration(80).start();
                    try {
                        nativeSetCirclePad(0f, 0f);
                    } catch (Throwable t) {
                        Log.w(TAG, "nativeSetCirclePad release failed: " + t.getMessage());
                    }
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
            "⚙️  Configurações do Emulador",
            "🎨  Opacidade dos Controles",
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
                        showInGameSettingsDialog();
                        break;
                    case 2:
                        showOpacityDialog();
                        break;
                    case 3:
                        toggleHapticFeedback();
                        break;
                    case 4:
                        recreate();
                        break;
                    case 5:
                        finish();
                        break;
                }
            })
            .setCancelable(true)
            .setOnDismissListener(d -> applyFullScreenImmersive())
            .show();
    }

    private static final int REQUEST_PICK_DRIVER = 300;
    private Runnable onDriverInstalledCallback = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_DRIVER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                GpuDriverManager.DriverInfo installed = GpuDriverManager.installDriverFromZip(this, data.getData());
                Toast.makeText(this, "✅ Driver '" + installed.name + "' instalado com sucesso!", Toast.LENGTH_LONG).show();
                if (onDriverInstalledCallback != null) {
                    onDriverInstalledCallback.run();
                }
            } catch (Exception e) {
                Toast.makeText(this, "❌ Erro ao instalar driver: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Dialog settingsDialog = null;

    private void showInGameSettingsDialog() {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) return;
        if (settingsDialog != null && settingsDialog.isShowing()) return;

        try {
            triggerHaptic();
            View settingsView = getLayoutInflater().inflate(R.layout.activity_settings, null);
            final ZakuroSettings settings = ZakuroSettings.load(this);

            ImageButton backButton = settingsView.findViewById(R.id.settingsBackButton);
            Button saveButton = settingsView.findViewById(R.id.settingsSaveButton);

            Spinner spinnerRenderer = settingsView.findViewById(R.id.spinnerRenderer);
            Spinner spinnerGpuDriver = settingsView.findViewById(R.id.spinnerGpuDriver);
            Button btnInstallDriver = settingsView.findViewById(R.id.btnInstallDriver);
            Button btnDeleteDriver = settingsView.findViewById(R.id.btnDeleteDriver);
            TextView tvDriverInfo = settingsView.findViewById(R.id.tvDriverInfo);

            Switch switchHwRaster = settingsView.findViewById(R.id.switchHwRaster);
            Spinner spinnerResolution = settingsView.findViewById(R.id.spinnerResolution);
            Switch switchShowFps = settingsView.findViewById(R.id.switchShowFps);
            Spinner spinnerVulkanPresentMode = settingsView.findViewById(R.id.spinnerVulkanPresentMode);
            Switch switchDebugMetrics = settingsView.findViewById(R.id.switchDebugMetrics);
            Spinner spinnerLayout = settingsView.findViewById(R.id.spinnerLayout);
            Switch switchRecompiled = settingsView.findViewById(R.id.switchRecompiled);

            SeekBar seekVolume = settingsView.findViewById(R.id.seekVolume);
            TextView tvVolumeVal = settingsView.findViewById(R.id.tvVolumeVal);
            Switch switchMute = settingsView.findViewById(R.id.switchMute);

            Switch switchTouchControls = settingsView.findViewById(R.id.switchTouchControls);
            SeekBar seekOpacity = settingsView.findViewById(R.id.seekOpacity);
            TextView tvOpacityVal = settingsView.findViewById(R.id.tvOpacityVal);
            Switch switchHaptic = settingsView.findViewById(R.id.switchHaptic);

            TextView tvSettingsPath = settingsView.findViewById(R.id.tvSettingsPath);

            // Bind values
            String[] renderers = {"Vulkan (Padrão e Recomendado no Android)", "OpenGL"};
            ArrayAdapter<String> rendererAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, renderers);
            spinnerRenderer.setAdapter(rendererAdapter);
            spinnerRenderer.setSelection("opengl".equalsIgnoreCase(settings.renderer) ? 1 : 0);

            // GPU Driver setup
            final List<GpuDriverManager.DriverInfo>[] driversRef = new List[]{ GpuDriverManager.getInstalledDrivers(this) };
            Runnable refreshDrivers = () -> {
                driversRef[0] = GpuDriverManager.getInstalledDrivers(NativeActivity.this);
                List<String> names = new ArrayList<>();
                int sel = 0;
                for (int i = 0; i < driversRef[0].size(); i++) {
                    GpuDriverManager.DriverInfo d = driversRef[0].get(i);
                    names.add(d.name);
                    if (d.id.equals(settings.custom_driver)) sel = i;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(NativeActivity.this, android.R.layout.simple_spinner_dropdown_item, names);
                spinnerGpuDriver.setAdapter(adapter);
                spinnerGpuDriver.setSelection(sel);
            };
            refreshDrivers.run();

            spinnerGpuDriver.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < driversRef[0].size()) {
                        GpuDriverManager.DriverInfo d = driversRef[0].get(position);
                        if (d.isSystem()) {
                            tvDriverInfo.setText("Driver do Sistema (Qualcomm/Mesa padrão do dispositivo)");
                            btnDeleteDriver.setVisibility(View.GONE);
                        } else {
                            String desc = d.name;
                            if (!d.description.isEmpty()) desc += "\n" + d.description;
                            if (!d.author.isEmpty()) desc += " | Autor: " + d.author;
                            if (!d.libName.isEmpty()) desc += " | Lib: " + d.libName;
                            tvDriverInfo.setText(desc);
                            btnDeleteDriver.setVisibility(View.VISIBLE);
                        }
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            btnInstallDriver.setOnClickListener(v -> {
                onDriverInstalledCallback = refreshDrivers;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(intent, REQUEST_PICK_DRIVER);
            });

            btnDeleteDriver.setOnClickListener(v -> {
                int pos = spinnerGpuDriver.getSelectedItemPosition();
                if (pos >= 0 && pos < driversRef[0].size()) {
                    GpuDriverManager.DriverInfo d = driversRef[0].get(pos);
                    if (!d.isSystem()) {
                        new AlertDialog.Builder(NativeActivity.this)
                            .setTitle("Excluir Driver")
                            .setMessage("Deseja realmente remover o driver '" + d.name + "'?")
                            .setPositiveButton("Excluir", (dlg, w) -> {
                                if (GpuDriverManager.deleteDriver(NativeActivity.this, d.id)) {
                                    Toast.makeText(NativeActivity.this, "Driver removido!", Toast.LENGTH_SHORT).show();
                                    settings.custom_driver = GpuDriverManager.DRIVER_SYSTEM;
                                    refreshDrivers.run();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                    }
                }
            });

            switchHwRaster.setChecked(settings.hardware_rasterizer);

            String[] resolutions = {"1x (240p Original 3DS)", "2x (480p)", "3x (720p HD)", "4x (1080p FHD)"};
            ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, resolutions);
            spinnerResolution.setAdapter(resolutionAdapter);
            int resIndex = Math.max(0, Math.min(settings.resolution - 1, resolutions.length - 1));
            spinnerResolution.setSelection(resIndex);

            switchShowFps.setChecked(settings.show_fps);

            String[] presentModes = {
                "Auto (Recomendado - Mailbox / Sem Bloqueio)",
                "Mailbox (Triple-Buffering Desbloqueado - Sem VSync)",
                "FIFO (VSync Ativo / 60Hz Travado)",
                "FIFO Relaxed (VSync Híbrido com Tearing)",
                "Immediate (Sem Sincronização / Baixa Latência)"
            };
            ArrayAdapter<String> presentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, presentModes);
            spinnerVulkanPresentMode.setAdapter(presentAdapter);
            if ("mailbox".equalsIgnoreCase(settings.vulkan_present_mode)) {
                spinnerVulkanPresentMode.setSelection(1);
            } else if ("fifo".equalsIgnoreCase(settings.vulkan_present_mode)) {
                spinnerVulkanPresentMode.setSelection(2);
            } else if ("fifo_relaxed".equalsIgnoreCase(settings.vulkan_present_mode)) {
                spinnerVulkanPresentMode.setSelection(3);
            } else if ("immediate".equalsIgnoreCase(settings.vulkan_present_mode)) {
                spinnerVulkanPresentMode.setSelection(4);
            } else {
                spinnerVulkanPresentMode.setSelection(0);
            }

            switchDebugMetrics.setChecked(settings.debug_metrics);

            String[] layouts = {"Lado a Lado (Side by Side - Ideal para Celular)", "Superior sobre Inferior (Stacked / Retrato)", "Apenas Tela Superior (Top Screen Only)"};
            ArrayAdapter<String> layoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, layouts);
            spinnerLayout.setAdapter(layoutAdapter);
            if ("stacked".equalsIgnoreCase(settings.layout)) {
                spinnerLayout.setSelection(1);
            } else if ("top_only".equalsIgnoreCase(settings.layout)) {
                spinnerLayout.setSelection(2);
            } else {
                spinnerLayout.setSelection(0);
            }

            switchRecompiled.setChecked(settings.recompiled);

            int volPercent = (int) Math.round(settings.volume * 100.0);
            seekVolume.setProgress(volPercent);
            tvVolumeVal.setText(volPercent + "%");
            seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean f) { tvVolumeVal.setText(p + "%"); }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });

            switchMute.setChecked(settings.mute);
            switchTouchControls.setChecked(settings.touch_controls);

            int opPercent = (int) Math.round(settings.touch_controls_opacity * 100.0);
            seekOpacity.setProgress(opPercent);
            tvOpacityVal.setText(opPercent + "%");
            seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean f) { tvOpacityVal.setText(p + "%"); }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });

            switchHaptic.setChecked(settings.haptic_feedback);
            tvSettingsPath.setText("Arquivo: " + ZakuroSettings.getSettingsFile(this).getAbsolutePath());

            final Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
            settingsDialog = dialog;
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(settingsView);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }

            backButton.setOnClickListener(v -> dialog.dismiss());

            saveButton.setOnClickListener(v -> {
                settings.renderer = (spinnerRenderer.getSelectedItemPosition() == 1) ? "opengl" : "vulkan";

                int driverPos = spinnerGpuDriver.getSelectedItemPosition();
                if (driverPos >= 0 && driverPos < driversRef[0].size()) {
                    GpuDriverManager.DriverInfo d = driversRef[0].get(driverPos);
                    settings.custom_driver = d.id;
                    settings.custom_driver_name = d.name;
                } else {
                    settings.custom_driver = GpuDriverManager.DRIVER_SYSTEM;
                    settings.custom_driver_name = "Driver do Sistema (Padrão)";
                }
                GpuDriverManager.applyDriverEnv(NativeActivity.this, settings);

                settings.hardware_rasterizer = switchHwRaster.isChecked();
                settings.resolution = spinnerResolution.getSelectedItemPosition() + 1;
                settings.show_fps = switchShowFps.isChecked();

                int presentIndex = spinnerVulkanPresentMode.getSelectedItemPosition();
                switch (presentIndex) {
                    case 1: settings.vulkan_present_mode = "mailbox"; break;
                    case 2: settings.vulkan_present_mode = "fifo"; break;
                    case 3: settings.vulkan_present_mode = "fifo_relaxed"; break;
                    case 4: settings.vulkan_present_mode = "immediate"; break;
                    default: settings.vulkan_present_mode = "auto"; break;
                }
                settings.debug_metrics = switchDebugMetrics.isChecked();
                try {
                    android.system.Os.setenv("ZAKURO_VULKAN_PRESENT_MODE", settings.vulkan_present_mode, true);
                    android.system.Os.setenv("ZAKURO_DEBUG_METRICS", settings.debug_metrics ? "1" : "0", true);
                } catch (Throwable ignored) {}

                int layoutPos = spinnerLayout.getSelectedItemPosition();
                if (layoutPos == 1) {
                    settings.layout = "stacked";
                } else if (layoutPos == 2) {
                    settings.layout = "top_only";
                } else {
                    settings.layout = "side_by_side";
                }

                settings.recompiled = switchRecompiled.isChecked();
                settings.volume = seekVolume.getProgress() / 100.0f;
                settings.mute = switchMute.isChecked();

                settings.touch_controls = switchTouchControls.isChecked();
                settings.touch_controls_opacity = Math.max(0.05f, seekOpacity.getProgress() / 100.0f);
                settings.haptic_feedback = switchHaptic.isChecked();

                if (settings.save(NativeActivity.this)) {
                    Toast.makeText(NativeActivity.this, "✅ Configurações salvas e aplicadas!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NativeActivity.this, "⚠️ Erro ao salvar configurações.", Toast.LENGTH_SHORT).show();
                }

                hapticEnabled = settings.haptic_feedback;
                if (controllerOverlay != null) {
                    controllerOverlay.setAlpha(settings.touch_controls_opacity);
                    if (!settings.touch_controls) {
                        controllerOverlay.setVisibility(View.GONE);
                    } else if (checkGameRunning()) {
                        controllerOverlay.setVisibility(View.VISIBLE);
                    }
                }

                try {
                    reloadSettings();
                } catch (Throwable ignored) {}

                dialog.dismiss();
            });

            dialog.setOnDismissListener(d -> {
                settingsDialog = null;
                applyFullScreenImmersive();
                if (getWindow() != null && getWindow().getDecorView() != null) {
                    getWindow().getDecorView().requestFocus();
                }
            });

            dialog.show();
            applyFullScreenImmersive();
        } catch (Throwable t) {
            Log.e(TAG, "showInGameSettingsDialog error", t);
        }
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
        try {
            reloadSettings();
        } catch (Throwable ignored) {}
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
