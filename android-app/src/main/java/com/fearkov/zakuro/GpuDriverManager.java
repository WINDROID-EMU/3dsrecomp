package com.fearkov.zakuro;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Gerenciador de Drivers Vulkan Customizados (AdrenoTools / Mesa Turnip).
 * Permite a qualquer usuário instalar drivers em formato .ZIP (ex: Turnip Mesa),
 * listar drivers disponíveis, alternar entre o driver do sistema e customizados,
 * e aplicar os caminhos de carregamento via variáveis de ambiente JNI.
 */
public class GpuDriverManager {
    private static final String TAG = "GpuDriverManager";

    public static final String DRIVER_SYSTEM = "system";

    public static class DriverInfo {
        public final String id;
        public final String name;
        public final String description;
        public final String author;
        public final String driverVersion;
        public final String libName;
        public final String dirPath;

        public DriverInfo(String id, String name, String description, String author, String driverVersion, String libName, String dirPath) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.author = author;
            this.driverVersion = driverVersion;
            this.libName = libName;
            this.dirPath = dirPath;
        }

        public boolean isSystem() {
            return DRIVER_SYSTEM.equalsIgnoreCase(id);
        }

        public boolean isValid() {
            if (isSystem()) return true;
            if (dirPath == null || libName == null) return false;
            File so = new File(dirPath, libName);
            return so.exists() && so.length() > 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static File getDriversDir(Context context) {
        File dir = new File(context.getFilesDir(), "custom_drivers");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Retorna a lista de todos os drivers instalados, sempre iniciando com o Driver do Sistema.
     */
    public static List<DriverInfo> getInstalledDrivers(Context context) {
        List<DriverInfo> list = new ArrayList<>();

        // 1. Driver padrão do sistema
        list.add(new DriverInfo(
            DRIVER_SYSTEM,
            "Driver do Sistema (Padrão Android)",
            "Driver Vulkan nativo do fabricante (Qualcomm/Mesa padrão do aparelho)",
            "Fabricante do Dispositivo",
            "Nativo",
            "",
            ""
        ));

        // 2. Migra/registra driver legado em getFilesDir()/driver se existir
        migrateLegacyDriver(context);

        // 3. Lê subdiretórios de custom_drivers
        File baseDir = getDriversDir(context);
        File[] folders = baseDir.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                DriverInfo info = loadDriverFromFolder(folder);
                if (info != null && info.isValid()) {
                    list.add(info);
                }
            }
        }

        return list;
    }

    public static DriverInfo getDriverById(Context context, String id) {
        if (id == null || id.isEmpty() || DRIVER_SYSTEM.equalsIgnoreCase(id)) {
            return new DriverInfo(
                DRIVER_SYSTEM,
                "Driver do Sistema (Padrão Android)",
                "Driver Vulkan nativo do fabricante",
                "Fabricante",
                "Nativo",
                "",
                ""
            );
        }
        for (DriverInfo d : getInstalledDrivers(context)) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    private static DriverInfo loadDriverFromFolder(File folder) {
        File metaFile = new File(folder, "meta.json");
        String name = folder.getName();
        String description = "Driver customizado instalado";
        String author = "Desconhecido";
        String driverVersion = "";
        String libName = "libvulkan_freedreno.so";

        if (metaFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(metaFile), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject obj = new JSONObject(sb.toString());

                if (obj.has("name")) name = obj.getString("name");
                if (obj.has("description")) description = obj.getString("description");
                if (obj.has("author")) author = obj.getString("author");
                if (obj.has("driverVersion")) driverVersion = obj.getString("driverVersion");
                if (obj.has("libraryName")) libName = obj.getString("libraryName");
            } catch (Exception e) {
                Log.w(TAG, "Error reading meta.json in " + folder.getName(), e);
            }
        } else {
            // Procura arquivo .so
            File[] soFiles = folder.listFiles((dir, f) -> f.endsWith(".so"));
            if (soFiles != null && soFiles.length > 0) {
                for (File so : soFiles) {
                    if (so.getName().contains("freedreno") || so.getName().contains("vulkan")) {
                        libName = so.getName();
                        break;
                    }
                }
                if (libName.isEmpty()) libName = soFiles[0].getName();
            }
        }

        return new DriverInfo(folder.getName(), name, description, author, driverVersion, libName, folder.getAbsolutePath());
    }

    private static void migrateLegacyDriver(Context context) {
        try {
            File legacyDir = new File(context.getFilesDir(), "driver");
            File legacySo = new File(legacyDir, "libvulkan_freedreno.so");
            if (legacySo.exists() && legacySo.length() > 0) {
                File targetFolder = new File(getDriversDir(context), "turnip_default");
                if (!targetFolder.exists()) targetFolder.mkdirs();
                File targetSo = new File(targetFolder, "libvulkan_freedreno.so");
                if (!targetSo.exists() || targetSo.length() != legacySo.length()) {
                    copyFile(legacySo, targetSo);
                }
                File targetMeta = new File(targetFolder, "meta.json");
                if (!targetMeta.exists()) {
                    JSONObject obj = new JSONObject();
                    obj.put("schemaVersion", 1);
                    obj.put("name", "Mesa Turnip v26.3.0 - R5");
                    obj.put("description", "Driver Turnip Vulkan de alta performance para Adreno 6xx/7xx");
                    obj.put("author", "K11MCH1 / Mesa");
                    obj.put("packageVersion", "1");
                    obj.put("vendor", "Mesa");
                    obj.put("driverVersion", "26.3.0");
                    obj.put("minApi", 28);
                    obj.put("libraryName", "libvulkan_freedreno.so");
                    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetMeta), StandardCharsets.UTF_8)) {
                        writer.write(obj.toString(2));
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Legacy driver migration ignored: " + t.getMessage());
        }
    }

    /**
     * Instala um novo driver a partir de um arquivo ZIP escolhido pelo usuário.
     */
    public static DriverInfo installDriverFromZip(Context context, Uri zipUri) throws Exception {
        String baseName = "driver_" + System.currentTimeMillis();
        // Tenta obter o nome original do arquivo
        try {
            String path = zipUri.getLastPathSegment();
            if (path != null) {
                if (path.contains("/")) path = path.substring(path.lastIndexOf('/') + 1);
                if (path.toLowerCase().endsWith(".zip")) {
                    path = path.substring(0, path.length() - 4);
                }
                String sanitized = path.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
                if (!sanitized.isEmpty()) baseName = sanitized;
            }
        } catch (Exception ignored) {}

        File destDir = new File(getDriversDir(context), baseName);
        if (destDir.exists()) {
            deleteRecursive(destDir);
        }
        destDir.mkdirs();

        // Extrai o conteúdo do ZIP
        try (InputStream is = context.getContentResolver().openInputStream(zipUri);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                // Remove caminhos de pastas internas para manter plano ou estruturado
                String filename = new File(entryName).getName();
                if (filename.isEmpty()) continue;

                File outFile = new File(destDir, filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }

        // Verifica se meta.json existe, ou cria um se não existir
        File metaFile = new File(destDir, "meta.json");
        File[] soFiles = destDir.listFiles((dir, name) -> name.endsWith(".so"));
        if (soFiles == null || soFiles.length == 0) {
            deleteRecursive(destDir);
            throw new IllegalArgumentException("O arquivo ZIP selecionado não contém bibliotecas .so de driver Vulkan válidas!");
        }

        String primaryLib = "libvulkan_freedreno.so";
        boolean foundPrimary = false;
        for (File so : soFiles) {
            if (so.getName().equalsIgnoreCase("libvulkan_freedreno.so")) {
                primaryLib = so.getName();
                foundPrimary = true;
                break;
            }
        }
        if (!foundPrimary) {
            for (File so : soFiles) {
                if (so.getName().contains("vulkan") || so.getName().contains("adreno")) {
                    primaryLib = so.getName();
                    foundPrimary = true;
                    break;
                }
            }
            if (!foundPrimary) primaryLib = soFiles[0].getName();
        }

        if (!metaFile.exists()) {
            JSONObject obj = new JSONObject();
            obj.put("schemaVersion", 1);
            obj.put("name", baseName.replace('_', ' '));
            obj.put("description", "Driver importado pelo usuário");
            obj.put("author", "Personalizado");
            obj.put("packageVersion", "1");
            obj.put("vendor", "Custom");
            obj.put("driverVersion", "Custom");
            obj.put("minApi", 28);
            obj.put("libraryName", primaryLib);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(metaFile), StandardCharsets.UTF_8)) {
                writer.write(obj.toString(2));
            }
        }

        DriverInfo info = loadDriverFromFolder(destDir);
        if (info == null || !info.isValid()) {
            deleteRecursive(destDir);
            throw new IllegalStateException("Falha ao validar os arquivos do driver instalado.");
        }

        Log.i(TAG, "Driver instalado com sucesso: " + info.name + " (" + info.libName + ") em " + info.dirPath);
        return info;
    }

    public static boolean deleteDriver(Context context, String driverId) {
        if (driverId == null || DRIVER_SYSTEM.equalsIgnoreCase(driverId)) return false;
        File dir = new File(getDriversDir(context), driverId);
        if (dir.exists()) {
            return deleteRecursive(dir);
        }
        return false;
    }

    private static boolean deleteRecursive(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDir.delete();
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Aplica as variáveis de ambiente necessárias para carregar o driver selecionado.
     */
    public static void applyDriverEnv(Context context, ZakuroSettings settings) {
        try {
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
            android.system.Os.setenv("ZAKURO_HOOK_LIB_DIR", nativeLibDir, true);
            android.system.Os.setenv("ZAKURO_GPU_SHADERS", "1", true);

            String driverId = (settings != null && settings.custom_driver != null) ? settings.custom_driver : DRIVER_SYSTEM;

            if (DRIVER_SYSTEM.equalsIgnoreCase(driverId) || driverId.isEmpty()) {
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_DIR", "", true);
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_LIB", "", true);
                Log.i(TAG, "Driver ativo: Driver do Sistema (AdrenoTools desativado)");
                return;
            }

            DriverInfo driver = getDriverById(context, driverId);
            if (driver != null && driver.isValid()) {
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_DIR", driver.dirPath + "/", true);
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_LIB", driver.libName, true);
                Log.i(TAG, "Driver ativo: " + driver.name + " (" + driver.libName + ") em " + driver.dirPath);
            } else {
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_DIR", "", true);
                android.system.Os.setenv("ZAKURO_CUSTOM_DRIVER_LIB", "", true);
                Log.w(TAG, "Driver customizado '" + driverId + "' não encontrado ou inválido, usando driver do sistema.");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao definir variáveis de ambiente do driver GPU", t);
        }
    }
}
