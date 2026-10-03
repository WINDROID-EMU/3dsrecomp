package com.fearkov.zakuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private ZakuroSettings settings;

    private Spinner spinnerRenderer;
    private Spinner spinnerGpuDriver;
    private Button btnInstallDriver;
    private Button btnDeleteDriver;
    private TextView tvDriverInfo;

    private Switch switchHwRaster;
    private Spinner spinnerResolution;
    private Switch switchShowFps;
    private Spinner spinnerVulkanPresentMode;
    private Switch switchDebugMetrics;
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

    private List<GpuDriverManager.DriverInfo> driverList = new ArrayList<>();
    private static final int REQUEST_PICK_DRIVER = 200;

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
        spinnerGpuDriver = findViewById(R.id.spinnerGpuDriver);
        btnInstallDriver = findViewById(R.id.btnInstallDriver);
        btnDeleteDriver = findViewById(R.id.btnDeleteDriver);
        tvDriverInfo = findViewById(R.id.tvDriverInfo);

        switchHwRaster = findViewById(R.id.switchHwRaster);
        spinnerResolution = findViewById(R.id.spinnerResolution);
        switchShowFps = findViewById(R.id.switchShowFps);
        spinnerVulkanPresentMode = findViewById(R.id.spinnerVulkanPresentMode);
        switchDebugMetrics = findViewById(R.id.switchDebugMetrics);
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
        btnInstallDriver.setOnClickListener(v -> pickDriverZip());
        btnDeleteDriver.setOnClickListener(v -> confirmDeleteDriver());
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

        // Custom GPU Driver
        refreshDriversList(settings.custom_driver);

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

        // Vulkan Present Mode / VSync
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

        // Debug Metrics
        switchDebugMetrics.setChecked(settings.debug_metrics);

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

    private void refreshDriversList(String selectId) {
        driverList = GpuDriverManager.getInstalledDrivers(this);
        List<String> displayNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < driverList.size(); i++) {
            GpuDriverManager.DriverInfo d = driverList.get(i);
            displayNames.add(d.name);
            if (d.id.equals(selectId)) {
                selectedIndex = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayNames);
        spinnerGpuDriver.setAdapter(adapter);
        spinnerGpuDriver.setSelection(selectedIndex);

        spinnerGpuDriver.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < driverList.size()) {
                    updateDriverDetails(driverList.get(position));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (selectedIndex >= 0 && selectedIndex < driverList.size()) {
            updateDriverDetails(driverList.get(selectedIndex));
        }
    }

    private void updateDriverDetails(GpuDriverManager.DriverInfo d) {
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

    private void pickDriverZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_PICK_DRIVER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_DRIVER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                GpuDriverManager.DriverInfo installed = GpuDriverManager.installDriverFromZip(this, data.getData());
                Toast.makeText(this, "✅ Driver '" + installed.name + "' instalado com sucesso!", Toast.LENGTH_LONG).show();
                refreshDriversList(installed.id);
            } catch (Exception e) {
                Toast.makeText(this, "❌ Erro ao instalar driver: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmDeleteDriver() {
        int pos = spinnerGpuDriver.getSelectedItemPosition();
        if (pos < 0 || pos >= driverList.size()) return;
        GpuDriverManager.DriverInfo current = driverList.get(pos);
        if (current.isSystem()) return;

        new AlertDialog.Builder(this)
            .setTitle("Excluir Driver")
            .setMessage("Deseja realmente remover o driver '" + current.name + "'?")
            .setPositiveButton("Excluir", (d, w) -> {
                if (GpuDriverManager.deleteDriver(this, current.id)) {
                    Toast.makeText(this, "Driver removido!", Toast.LENGTH_SHORT).show();
                    refreshDriversList(GpuDriverManager.DRIVER_SYSTEM);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void saveAndFinish() {
        // Collect UI data
        settings.renderer = (spinnerRenderer.getSelectedItemPosition() == 1) ? "opengl" : "vulkan";

        int driverPos = spinnerGpuDriver.getSelectedItemPosition();
        if (driverPos >= 0 && driverPos < driverList.size()) {
            GpuDriverManager.DriverInfo d = driverList.get(driverPos);
            settings.custom_driver = d.id;
            settings.custom_driver_name = d.name;
        } else {
            settings.custom_driver = GpuDriverManager.DRIVER_SYSTEM;
            settings.custom_driver_name = "Driver do Sistema (Padrão)";
        }

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

        GpuDriverManager.applyDriverEnv(this, settings);

        if (settings.save(this)) {
            Toast.makeText(this, "✅ Configurações salvas permanentemente em settings.toml!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Erro ao salvar configurações.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
