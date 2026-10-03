package com.fearkov.zakuro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;

/** XML launcher & painel de recompilação para o Zakuro 3DS. */
public final class MainActivity extends Activity {
    private static final int PICK_ROM = 40;
    private static final int REQUEST_MANAGE_STORAGE = 101;
    private static final int REQUEST_LEGACY_STORAGE = 102;

    private TextView romName;
    private TextView romDetails;
    private TextView formatBadge;
    private TextView cryptoBadge;
    private TextView modeBadge;
    private ImageView gameIcon;
    private TextView progressLabel;
    private ProgressBar progress;
    private Button playButton;
    private Button startButton;
    private Button selectRomButton;
    private Button grantPermissionButton;
    private Button debugButton;
    private Button modsButton;
    private Button settingsButton;
    private File selectedRom;
    private RomDiagnosis currentDiag;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        romName = findViewById(R.id.romName);
        romDetails = findViewById(R.id.romDetails);
        formatBadge = findViewById(R.id.formatBadge);
        cryptoBadge = findViewById(R.id.cryptoBadge);
        modeBadge = findViewById(R.id.modeBadge);
        gameIcon = findViewById(R.id.gameIcon);
        progressLabel = findViewById(R.id.progressLabel);
        progress = findViewById(R.id.recompileProgress);
        playButton = findViewById(R.id.playButton);
        startButton = findViewById(R.id.startButton);
        selectRomButton = findViewById(R.id.selectRomButton);
        grantPermissionButton = findViewById(R.id.grantPermissionButton);
        debugButton = findViewById(R.id.debugButton);
        modsButton = findViewById(R.id.modsButton);
        settingsButton = findViewById(R.id.settingsButton);

        if (selectRomButton != null) selectRomButton.setOnClickListener(v -> chooseRom());
        if (grantPermissionButton != null) grantPermissionButton.setOnClickListener(v -> requestStoragePermission());
        if (debugButton != null) debugButton.setOnClickListener(v -> showDebugDialog());
        if (settingsButton != null) settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        if (modsButton != null) modsButton.setOnClickListener(v -> openModsManager());
        if (playButton != null) playButton.setOnClickListener(v -> launchGame(false));
        if (startButton != null) startButton.setOnClickListener(v -> launchGame(true));

        updatePermissionBanner();

        File folder = new File(getFilesDir(), "roms");
        File existing = new File(folder, "selected.3ds");
        if (existing.exists() && existing.length() > 0) {
            selectedRom = existing;
            romName.setText(existing.getName());
            RomDiagnosis diag = inspectRom(existing);
            applyDiagnosis(diag);
        }

        setupToolchain();
        setupTurnipDriver();

        if (!hasStoragePermission()) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_title)
                .setMessage(R.string.storage_permission_msg)
                .setPositiveButton(R.string.grant_storage_permission, (d, w) -> requestStoragePermission())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        }
    }

    private void setupToolchain() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File toolchainDir = new File(getFilesDir(), "toolchain");
                File marker = new File(toolchainDir, "version_installed.txt");
                String currentVer = "0.2.11_tcc_v2";
                boolean needExtract = !marker.exists();
                if (marker.exists()) {
                    try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(marker))) {
                        String v = r.readLine();
                        if (v == null || !v.trim().equals(currentVer)) {
                            needExtract = true;
                        }
                    } catch (Exception ignored) {
                        needExtract = true;
                    }
                }

                if (needExtract) {
                    android.util.Log.i("ZakuroToolchain", "Extracting toolchain.zip into " + toolchainDir);
                    if (!toolchainDir.exists()) toolchainDir.mkdirs();
                    try (InputStream is = getAssets().open("toolchain.zip");
                         java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {
                        java.util.zip.ZipEntry entry;
                        byte[] buffer = new byte[8192];
                        while ((entry = zis.getNextEntry()) != null) {
                            File outFile = new File(toolchainDir, entry.getName());
                            if (entry.isDirectory()) {
                                outFile.mkdirs();
                            } else {
                                if (outFile.getParentFile() != null) outFile.getParentFile().mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                            }
                            zis.closeEntry();
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(marker)) {
                        fos.write(currentVer.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    android.util.Log.i("ZakuroToolchain", "Toolchain extraction complete!");
                }

                String nativeDir = getApplicationInfo().nativeLibraryDir;
                File ccFile = new File(nativeDir, "libcc.so");
                File ccPathTxt = new File(toolchainDir, "cc_path.txt");
                try (FileOutputStream fos = new FileOutputStream(ccPathTxt)) {
                    fos.write(ccFile.getAbsolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

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
                android.util.Log.i("ZakuroToolchain", "Compiler ready at: " + ccFile.getAbsolutePath());
            } catch (Exception e) {
                android.util.Log.e("ZakuroToolchain", "Error setting up toolchain", e);
            }
        });
    }

    private void setupTurnipDriver() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ZakuroSettings settings = ZakuroSettings.load(this);
                // Migra driver legado e detecta drivers instalados
                GpuDriverManager.getInstalledDrivers(this);
                // Se o usuário ainda não escolheu um driver específico e o Turnip integrado existe, define-o como padrão
                if (settings.custom_driver == null || settings.custom_driver.isEmpty() || "system".equalsIgnoreCase(settings.custom_driver)) {
                    GpuDriverManager.DriverInfo turnip = GpuDriverManager.getDriverById(this, "turnip_default");
                    if (turnip != null && turnip.isValid()) {
                        settings.custom_driver = turnip.id;
                        settings.custom_driver_name = turnip.name;
                        settings.save(this);
                    }
                }
                GpuDriverManager.applyDriverEnv(this, settings);
            } catch (Exception e) {
                android.util.Log.w("ZakuroDriver", "Error initializing GPU driver manager", e);
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        updatePermissionBanner();
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updatePermissionBanner() {
        if (grantPermissionButton == null) return;
        grantPermissionButton.setVisibility(hasStoragePermission() ? View.GONE : View.VISIBLE);
    }

    private void requestStoragePermission() {
        if (hasStoragePermission()) {
            Toast.makeText(this, R.string.storage_permission_granted, Toast.LENGTH_SHORT).show();
            updatePermissionBanner();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                } catch (Exception e2) {
                    Toast.makeText(this, "Não foi possível abrir as configurações de permissão", Toast.LENGTH_LONG).show();
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_LEGACY_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LEGACY_STORAGE) {
            updatePermissionBanner();
            if (hasStoragePermission()) {
                Toast.makeText(this, R.string.storage_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.storage_permission_denied_warning, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void chooseRom() {
        if (!hasStoragePermission()) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_title)
                .setMessage(R.string.storage_permission_msg)
                .setPositiveButton(R.string.grant_storage_permission, (d, w) -> requestStoragePermission())
                .setNegativeButton("Continuar", (d, w) -> openRomPicker())
                .show();
            return;
        }
        openRomPicker();
    }

    private void openRomPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_ROM);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQUEST_MANAGE_STORAGE) {
            updatePermissionBanner();
            if (hasStoragePermission()) {
                Toast.makeText(this, R.string.storage_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.storage_permission_denied_warning, Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (request != PICK_ROM || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
        }
        prepareRomAsync(uri);
    }

    private void prepareRomAsync(Uri uri) {
        progress.setProgress(0);
        progressLabel.setText(getString(R.string.rom_copying));
        if (startButton != null) startButton.setEnabled(false);
        if (playButton != null) playButton.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long totalSize = -1;
                try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            totalSize = cursor.getLong(sizeIndex);
                        }
                    }
                } catch (Exception ignored) {}

                File folder = new File(getFilesDir(), "roms");
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new IllegalStateException("não foi possível criar a pasta de ROMs");
                }
                File output = new File(folder, "selected.3ds");

                final long finalTotal = totalSize;
                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(output)) {
                    if (input == null) throw new IllegalStateException("URI sem conteúdo");
                    byte[] buffer = new byte[1024 * 1024];
                    int read;
                    long copied = 0;
                    while ((read = input.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        copied += read;
                        if (finalTotal > 0) {
                            final int pct = (int) (copied * 100 / finalTotal);
                            runOnUiThread(() -> progress.setProgress(pct));
                        }
                    }
                }

                File pathFile = new File(folder, "selected_path.txt");
                try (FileOutputStream pathOut = new FileOutputStream(pathFile)) {
                    pathOut.write(output.getAbsolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                RomDiagnosis diag = inspectRom(output);
                runOnUiThread(() -> {
                    selectedRom = output;
                    romName.setText(output.getName());
                    applyDiagnosis(diag);
                    if (diag.isNcch && !diag.isDecrypted) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.rom_diagnosis_title)
                            .setMessage("• Produto: " + diag.productCode +
                                "\n• Title ID: " + diag.titleId +
                                "\n• Formato: " + diag.format +
                                "\n• Criptografia: Criptografada (Crypto 0x" + Integer.toHexString(diag.cryptoMethod) + ")" +
                                "\n\n⚠️ Atenção: Esta ROM é criptografada. O Zakuro e o motor de recompilação (3dsrecomp) necessitam de ROMs descriptografadas (formato .3ds decriptado ou .cxi).\n\nVocê pode abrir o emulador para visualizar os detalhes no frontend.")
                            .setPositiveButton("Entendido", null)
                            .show();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progress.setProgress(0);
                    progressLabel.setText(getString(R.string.ready_to_recompile));
                    Toast.makeText(MainActivity.this, "Falha ao preparar a ROM: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void applyDiagnosis(RomDiagnosis diag) {
        currentDiag = diag;
        progress.setProgress(100);
        if (playButton != null) playButton.setEnabled(true);
        if (startButton != null) startButton.setEnabled(true);

        if (romName != null) {
            if (diag.title != null && !diag.title.isEmpty()) {
                romName.setText(diag.title);
            } else if (selectedRom != null) {
                romName.setText(selectedRom.getName());
            }
        }

        if (romDetails != null) {
            StringBuilder details = new StringBuilder();
            if (diag.publisher != null && !diag.publisher.isEmpty()) {
                details.append(diag.publisher);
            } else {
                details.append("Nintendo 3DS");
            }
            if (!diag.productCode.isEmpty()) {
                details.append(" • ").append(diag.productCode);
            }
            if (!diag.titleId.isEmpty()) {
                details.append(" (").append(diag.titleId).append(")");
            }
            romDetails.setText(details.toString());
        }

        if (formatBadge != null) {
            formatBadge.setText(diag.format);
        }

        if (diag.isNcch) {
            if (cryptoBadge != null) {
                cryptoBadge.setText(diag.isDecrypted ? R.string.status_decrypted : R.string.status_encrypted);
                cryptoBadge.setBackgroundResource(diag.isDecrypted ? R.drawable.badge_success : R.drawable.badge_warning);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cryptoBadge.setTextColor(getColor(diag.isDecrypted ? R.color.zakuro_success : R.color.zakuro_warning));
                }
            }
            if (modeBadge != null) {
                modeBadge.setText(diag.isDecrypted ? "Pronta para Recompilar AOT" : "Interpretador Ativo");
            }
            if (progressLabel != null) {
                if (diag.isDecrypted) {
                    progressLabel.setText("✅ ROM Descriptografada (" + diag.productCode + ") pronta para execução e recompilação!");
                } else {
                    progressLabel.setText("⚠️ ROM Criptografada (" + diag.productCode + "). Toque em Jogar (Interpretador).");
                }
            }
        } else if (progressLabel != null) {
            progressLabel.setText(getString(R.string.rom_ready));
        }
    }

    private void launchGame(boolean recompile) {
        if (selectedRom == null) {
            Toast.makeText(this, R.string.select_rom_first, Toast.LENGTH_SHORT).show();
            return;
        }

        // Garante transição imediata para orientação Landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        File folder = new File(getFilesDir(), "roms");
        if (!folder.exists()) folder.mkdirs();

        File pathFile = new File(folder, "selected_path.txt");
        try (FileOutputStream pathOut = new FileOutputStream(pathFile)) {
            pathOut.write(selectedRom.getAbsolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        File actionFile = new File(folder, "action.txt");
        try (FileOutputStream actionOut = new FileOutputStream(actionFile)) {
            actionOut.write((recompile ? "recompile" : "play").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, NativeActivity.class);
        intent.putExtra("zakuro_selected_rom", selectedRom.getAbsolutePath());
        intent.putExtra("zakuro_action", recompile ? "recompile" : "play");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.native_start_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void openModsManager() {
        Intent intent = new Intent(this, CModActivity.class);
        if (currentDiag != null && currentDiag.titleId != null && !currentDiag.titleId.isEmpty()) {
            intent.putExtra(CModActivity.EXTRA_TITLE_ID, currentDiag.titleId);
            intent.putExtra(CModActivity.EXTRA_GAME_NAME, (currentDiag.title != null && !currentDiag.title.isEmpty()) ? currentDiag.title : "Jogo 3DS");
        }
        if (selectedRom != null) {
            intent.putExtra(CModActivity.EXTRA_ROM_PATH, selectedRom.getAbsolutePath());
        }
        startActivity(intent);
    }

    private void showDebugDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Diagnóstico Zakuro 3DS Recomp ===\n\n");
        if (selectedRom != null && selectedRom.exists()) {
            RomDiagnosis diag = inspectRom(selectedRom);
            sb.append("Arquivo: ").append(selectedRom.getName()).append(" (")
              .append(selectedRom.length() / (1024 * 1024)).append(" MB)\n");
            sb.append("Formato: ").append(diag.format).append("\n");
            if (!diag.title.isEmpty()) sb.append("Título: ").append(diag.title).append("\n");
            if (!diag.publisher.isEmpty()) sb.append("Publicador: ").append(diag.publisher).append("\n");
            if (!diag.productCode.isEmpty()) sb.append("Código do Produto: ").append(diag.productCode).append("\n");
            if (!diag.titleId.isEmpty()) sb.append("Title ID: ").append(diag.titleId).append("\n");
            sb.append("Status Criptografia: ")
              .append(diag.isDecrypted ? "✅ Descriptografada (compatível)" : "⚠️ Criptografada (Crypto 0x" + Integer.toHexString(diag.cryptoMethod) + ")\n   -> Zakuro & 3dsrecomp requerem dump descriptografado")
              .append("\n\n");
        } else {
            sb.append("Nenhuma ROM selecionada no momento.\n\n");
        }

        sb.append("=== Logs Recentes (logcat) ===\n");
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-t", "80"});
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("zakuro") || line.contains("fearkov") || line.contains("RustStdoutStderr") || line.contains("recomp")) {
                        sb.append(line).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("Não foi possível ler logs: ").append(e.getMessage()).append("\n");
        }

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setTextSize(12);
        tv.setPadding(32, 24, 32, 24);
        tv.setTextIsSelectable(true);
        sv.addView(tv);

        new AlertDialog.Builder(this)
            .setTitle(R.string.rom_diagnosis_title)
            .setView(sv)
            .setPositiveButton("Fechar", null)
            .show();
    }

    public static class RomDiagnosis {
        public String format = "Desconhecido";
        public String title = "";
        public String publisher = "";
        public String productCode = "";
        public String titleId = "";
        public boolean isNcch = false;
        public boolean isDecrypted = false;
        public int cryptoMethod = -1;
    }

    private static RomDiagnosis inspectRom(File rom) {
        RomDiagnosis d = new RomDiagnosis();
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(rom, "r")) {
            byte[] header = new byte[0x200];
            raf.seek(0x100);
            raf.readFully(header);
            String magic = new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
            long ncchOffset = -1;
            if ("NCSD".equals(magic)) {
                d.format = "NCSD (.3DS / .CCI)";
                long partition0MediaUnit = ((long)(header[0x20] & 0xFF)) |
                        (((long)(header[0x21] & 0xFF)) << 8) |
                        (((long)(header[0x22] & 0xFF)) << 16) |
                        (((long)(header[0x23] & 0xFF)) << 24);
                ncchOffset = partition0MediaUnit * 512L;
            } else if ("NCCH".equals(magic)) {
                d.format = "NCCH (.CXI)";
                ncchOffset = 0;
            }

            if (ncchOffset >= 0 && raf.length() > ncchOffset + 0x200) {
                raf.seek(ncchOffset + 0x100);
                byte[] ncchHeader = new byte[0x100];
                raf.readFully(ncchHeader);
                String ncchMagic = new String(ncchHeader, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if ("NCCH".equals(ncchMagic)) {
                    d.isNcch = true;
                    StringBuilder tid = new StringBuilder();
                    for (int i = 15; i >= 8; i--) {
                        tid.append(String.format("%02X", ncchHeader[i]));
                    }
                    d.titleId = tid.toString();

                    d.productCode = new String(ncchHeader, 0x50, 16, java.nio.charset.StandardCharsets.US_ASCII).trim();

                    int flags7 = ncchHeader[0x8F] & 0xFF;
                    int cryptoMethod = ncchHeader[0x8B] & 0xFF;
                    d.cryptoMethod = cryptoMethod;
                    d.isDecrypted = (flags7 & 0x04) != 0 || (flags7 == 0 && cryptoMethod == 0);

                    // Tentar ler ExeFS para extrair nome oficial e publisher do SMDH
                    try {
                        long exefsMediaUnit = ((long)(ncchHeader[0x60] & 0xFF)) |
                                (((long)(ncchHeader[0x61] & 0xFF)) << 8) |
                                (((long)(ncchHeader[0x62] & 0xFF)) << 16) |
                                (((long)(ncchHeader[0x63] & 0xFF)) << 24);
                        long exefsOffset = ncchOffset + exefsMediaUnit * 512L;
                        if (exefsOffset > 0 && raf.length() > exefsOffset + 0x200) {
                            raf.seek(exefsOffset);
                            byte[] exefsHeader = new byte[10 * 16];
                            raf.readFully(exefsHeader);
                            for (int e = 0; e < 10; e++) {
                                int entryBase = e * 16;
                                String entryName = new String(exefsHeader, entryBase, 8, java.nio.charset.StandardCharsets.US_ASCII).trim();
                                if ("icon".equals(entryName)) {
                                    long iconFileOffset = ((long)(exefsHeader[entryBase + 8] & 0xFF)) |
                                            (((long)(exefsHeader[entryBase + 9] & 0xFF)) << 8) |
                                            (((long)(exefsHeader[entryBase + 10] & 0xFF)) << 16) |
                                            (((long)(exefsHeader[entryBase + 11] & 0xFF)) << 24);
                                    long smdhOffset = exefsOffset + 512L + iconFileOffset;
                                    if (raf.length() > smdhOffset + 0x400) {
                                        raf.seek(smdhOffset);
                                        byte[] smdhMagicBytes = new byte[4];
                                        raf.readFully(smdhMagicBytes);
                                        if ("SMDH".equals(new String(smdhMagicBytes, java.nio.charset.StandardCharsets.US_ASCII))) {
                                            raf.seek(smdhOffset + 0x208);
                                            byte[] shortTitleBytes = new byte[0x80];
                                            raf.readFully(shortTitleBytes);
                                            String shortTitle = new String(shortTitleBytes, java.nio.charset.StandardCharsets.UTF_16LE).trim();

                                            byte[] longTitleBytes = new byte[0x100];
                                            raf.readFully(longTitleBytes);
                                            String longTitle = new String(longTitleBytes, java.nio.charset.StandardCharsets.UTF_16LE).trim();

                                            byte[] pubBytes = new byte[0x80];
                                            raf.readFully(pubBytes);
                                            String pub = new String(pubBytes, java.nio.charset.StandardCharsets.UTF_16LE).trim();

                                            if (!longTitle.isEmpty()) d.title = longTitle;
                                            else if (!shortTitle.isEmpty()) d.title = shortTitle;

                                            if (!pub.isEmpty()) d.publisher = pub;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return d;
    }
}
