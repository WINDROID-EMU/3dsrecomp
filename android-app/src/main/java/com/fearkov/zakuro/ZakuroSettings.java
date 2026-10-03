package com.fearkov.zakuro;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZakuroSettings {
    private static final String TAG = "ZakuroSettings";

    public String games = "";
    public String renderer = "vulkan";
    public boolean hardware_rasterizer = true;
    public int resolution = 1;
    public boolean recompiled = true;
    public int scale = 1;
    public String layout = "side_by_side"; // "side_by_side", "stacked", "top_only"
    public float volume = 1.0f;
    public boolean mute = false;
    public boolean show_fps = true;
    public float background_opacity = 0.35f;
    public boolean touch_controls = true;
    public float touch_controls_opacity = 0.65f;
    public boolean haptic_feedback = true;
    public String custom_driver = "system";
    public String custom_driver_name = "Driver do Sistema (Padrão)";

    // Preserva seções [keys] e [pad] intactas
    private final List<String> rawKeysLines = new ArrayList<>();
    private final List<String> rawPadLines = new ArrayList<>();

    public static File getSettingsFile(Context context) {
        return new File(context.getFilesDir(), "settings.toml");
    }

    public static ZakuroSettings load(Context context) {
        ZakuroSettings s = new ZakuroSettings();
        File file = getSettingsFile(context);
        if (!file.exists()) {
            s.games = new File(context.getFilesDir(), "roms").getAbsolutePath();
            s.save(context);
            return s;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            String currentSection = "";
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed;
                    continue;
                }

                if ("[keys]".equalsIgnoreCase(currentSection)) {
                    s.rawKeysLines.add(line);
                    continue;
                } else if ("[pad]".equalsIgnoreCase(currentSection)) {
                    s.rawPadLines.add(line);
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq < 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String val = trimmed.substring(eq + 1).trim();

                switch (key) {
                    case "games":
                        s.games = unquote(val);
                        break;
                    case "renderer":
                        s.renderer = unquote(val).toLowerCase();
                        break;
                    case "hardware_rasterizer":
                        s.hardware_rasterizer = Boolean.parseBoolean(val);
                        break;
                    case "resolution":
                        try { s.resolution = Integer.parseInt(val); } catch (Exception ignored) {}
                        break;
                    case "recompiled":
                        s.recompiled = Boolean.parseBoolean(val);
                        break;
                    case "scale":
                        try { s.scale = Integer.parseInt(val); } catch (Exception ignored) {}
                        break;
                    case "layout":
                        s.layout = unquote(val).toLowerCase();
                        break;
                    case "volume":
                        try { s.volume = Float.parseFloat(val); } catch (Exception ignored) {}
                        break;
                    case "mute":
                        s.mute = Boolean.parseBoolean(val);
                        break;
                    case "show_fps":
                        s.show_fps = Boolean.parseBoolean(val);
                        break;
                    case "background_opacity":
                        try { s.background_opacity = Float.parseFloat(val); } catch (Exception ignored) {}
                        break;
                    case "touch_controls":
                        s.touch_controls = Boolean.parseBoolean(val);
                        break;
                    case "touch_controls_opacity":
                        try { s.touch_controls_opacity = Float.parseFloat(val); } catch (Exception ignored) {}
                        break;
                    case "haptic_feedback":
                        s.haptic_feedback = Boolean.parseBoolean(val);
                        break;
                    case "custom_driver":
                        s.custom_driver = unquote(val);
                        break;
                    case "custom_driver_name":
                        s.custom_driver_name = unquote(val);
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings.toml", e);
        }
        if ("system".equalsIgnoreCase(s.custom_driver)) {
            File turnip = new File(context.getFilesDir(), "custom_drivers/turnip_default");
            if (turnip.exists()) {
                s.custom_driver = "turnip_default";
                s.custom_driver_name = "Mesa Turnip Adreno";
            }
        }
        return s;
    }

    public boolean save(Context context) {
        File file = getSettingsFile(context);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            if (games != null && !games.isEmpty()) {
                pw.println("games = \"" + games + "\"");
            }
            pw.println("renderer = \"" + renderer + "\"");
            pw.println("hardware_rasterizer = " + hardware_rasterizer);
            pw.println("resolution = " + resolution);
            pw.println("recompiled = " + recompiled);
            pw.println("scale = " + scale);
            pw.println("layout = \"" + layout + "\"");
            pw.printf(java.util.Locale.US, "volume = %.2f\n", volume);
            pw.println("mute = " + mute);
            pw.println("show_fps = " + show_fps);
            pw.printf(java.util.Locale.US, "background_opacity = %.2f\n", background_opacity);
            pw.println("touch_controls = " + touch_controls);
            pw.printf(java.util.Locale.US, "touch_controls_opacity = %.2f\n", touch_controls_opacity);
            pw.println("haptic_feedback = " + haptic_feedback);
            pw.println("custom_driver = \"" + (custom_driver != null ? custom_driver : "system") + "\"");
            pw.println("custom_driver_name = \"" + (custom_driver_name != null ? custom_driver_name : "Driver do Sistema (Padrão)") + "\"");
            pw.println();

            pw.println("[keys]");
            if (rawKeysLines.isEmpty()) {
                pw.println("a = \"KeyX\"");
                pw.println("b = \"KeyZ\"");
                pw.println("x = \"KeyS\"");
                pw.println("y = \"KeyA\"");
                pw.println("l = \"KeyQ\"");
                pw.println("r = \"KeyW\"");
                pw.println("start = \"Enter\"");
                pw.println("select = \"Backspace\"");
                pw.println("up = \"ArrowUp\"");
                pw.println("down = \"ArrowDown\"");
                pw.println("left = \"ArrowLeft\"");
                pw.println("right = \"ArrowRight\"");
                pw.println("circle_up = \"KeyI\"");
                pw.println("circle_down = \"KeyK\"");
                pw.println("circle_left = \"KeyJ\"");
                pw.println("circle_right = \"KeyL\"");
            } else {
                for (String k : rawKeysLines) pw.println(k);
            }
            pw.println();

            pw.println("[pad]");
            if (rawPadLines.isEmpty()) {
                pw.println("a = \"East\"");
                pw.println("b = \"South\"");
                pw.println("x = \"North\"");
                pw.println("y = \"West\"");
                pw.println("l = \"LeftTrigger\"");
                pw.println("r = \"RightTrigger\"");
                pw.println("start = \"Start\"");
                pw.println("select = \"Select\"");
                pw.println("up = \"DPadUp\"");
                pw.println("down = \"DPadDown\"");
                pw.println("left = \"DPadLeft\"");
                pw.println("right = \"DPadRight\"");
            } else {
                for (String p : rawPadLines) pw.println(p);
            }

            Log.i(TAG, "Settings successfully saved to " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings.toml", e);
            return false;
        }
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
