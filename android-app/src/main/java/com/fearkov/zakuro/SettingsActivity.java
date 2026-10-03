package com.fearkov.zakuro;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private ZakuroSettings settings;

    private Spinner spinnerRenderer;
    private Switch switchHwRaster;
    private Spinner spinnerResolution;
    private Switch switchShowFps;
    private Spinner spinnerLayout;
    private Switch switchRecompiled;

    private SeekBar seekVolume;
    private TextView tvVolumeVal;
    private Switch switchMute;

    private Switch switchTouchControls;
    private SeekBar seekOpacity;
    private TextView tvOpacityVal;
    private Switch switchHaptic;

    private TextView tvSettingsPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = ZakuroSettings.load(this);

        initViews();
        bindData();
    }

    private void initViews() {
        ImageButton backButton = findViewById(R.id.settingsBackButton);
        Button saveButton = findViewById(R.id.settingsSaveButton);

        spinnerRenderer = findViewById(R.id.spinnerRenderer);
        switchHwRaster = findViewById(R.id.switchHwRaster);
        spinnerResolution = findViewById(R.id.spinnerResolution);
        switchShowFps = findViewById(R.id.switchShowFps);
        spinnerLayout = findViewById(R.id.spinnerLayout);
        switchRecompiled = findViewById(R.id.switchRecompiled);

        seekVolume = findViewById(R.id.seekVolume);
        tvVolumeVal = findViewById(R.id.tvVolumeVal);
        switchMute = findViewById(R.id.switchMute);

        switchTouchControls = findViewById(R.id.switchTouchControls);
        seekOpacity = findViewById(R.id.seekOpacity);
        tvOpacityVal = findViewById(R.id.tvOpacityVal);
        switchHaptic = findViewById(R.id.switchHaptic);

        tvSettingsPath = findViewById(R.id.tvSettingsPath);

        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveAndFinish());
    }

    private void bindData() {
        // Renderer
        String[] renderers = {"Vulkan (Padrão e Recomendado no Android)", "OpenGL"};
        ArrayAdapter<String> rendererAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, renderers);
        spinnerRenderer.setAdapter(rendererAdapter);
        if ("opengl".equalsIgnoreCase(settings.renderer)) {
            spinnerRenderer.setSelection(1);
        } else {
            spinnerRenderer.setSelection(0);
        }

        // Hardware Rasterizer
        switchHwRaster.setChecked(settings.hardware_rasterizer);

        // Resolution
        String[] resolutions = {"1x (240p Original 3DS)", "2x (480p)", "3x (720p HD)", "4x (1080p FHD)"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, resolutions);
        spinnerResolution.setAdapter(resolutionAdapter);
        int resIndex = Math.max(0, Math.min(settings.resolution - 1, resolutions.length - 1));
        spinnerResolution.setSelection(resIndex);

        // Show FPS
        switchShowFps.setChecked(settings.show_fps);

        // Screens Layout
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

        // Recompiled
        switchRecompiled.setChecked(settings.recompiled);

        // Volume
        int volPercent = (int) Math.round(settings.volume * 100.0);
        seekVolume.setProgress(volPercent);
        tvVolumeVal.setText(volPercent + "%");
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVolumeVal.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Mute
        switchMute.setChecked(settings.mute);

        // Touch Controls
        switchTouchControls.setChecked(settings.touch_controls);

        // Touch Opacity
        int opPercent = (int) Math.round(settings.touch_controls_opacity * 100.0);
        seekOpacity.setProgress(opPercent);
        tvOpacityVal.setText(opPercent + "%");
        seekOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvOpacityVal.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Haptic Feedback
        switchHaptic.setChecked(settings.haptic_feedback);

        // Path
        tvSettingsPath.setText("Arquivo: " + ZakuroSettings.getSettingsFile(this).getAbsolutePath());
    }

    private void saveAndFinish() {
        // Collect UI data
        settings.renderer = (spinnerRenderer.getSelectedItemPosition() == 1) ? "opengl" : "vulkan";
        settings.hardware_rasterizer = switchHwRaster.isChecked();
        settings.resolution = spinnerResolution.getSelectedItemPosition() + 1;
        settings.show_fps = switchShowFps.isChecked();

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
        settings.touch_controls_opacity = Math.max(0.1f, seekOpacity.getProgress() / 100.0f);
        settings.haptic_feedback = switchHaptic.isChecked();

        if (settings.save(this)) {
            Toast.makeText(this, "✅ Configurações salvas permanentemente em settings.toml!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Erro ao salvar configurações.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
