package com.monitor.overlay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ThermalReader {

    public static int getBatteryTemp() {
        String[] batPaths = {
            "/sys/class/power_supply/battery/temp",
            "/sys/class/power_supply/Battery/temp",
            "/sys/class/power_supply/bms/temp",
        };
        for (String path : batPaths) {
            int raw = readInt(path);
            if (raw > 0) return raw / 10;
        }
        return findZoneTemp(new String[]{"battery", "Battery", "bat", "BAT"});
    }

    public static int getCpuTemp() {
        int t = findZoneTemp(new String[]{
            "cpu-0-0-usr", "cpu-0-1-usr", "cpu0",
            "cpu_thermal", "cpu-thermal",
            "mtktscpu", "mtktsAP",
            "cluster0", "CLUSTER0",
            "tsens_tz_sensor0",
            "x86_pkg_temp", "acpitz"
        });
        if (t >= 0) return t;
        return findZoneContains(new String[]{"cpu", "CPU", "tsens", "core"});
    }

    public static int getGpuTemp() {
        int t = findZoneTemp(new String[]{
            "gpu", "gpu0", "GPU",
            "gpu-step", "kgsl-adreno",
            "mtktsgpu", "mtktsGPU",
            "G3D", "gpu_thermal",
            "adreno", "Adreno",
            "mali", "Mali"
        });
        if (t >= 0) return t;
        return findZoneContains(new String[]{"gpu", "GPU", "adreno", "mali", "G3D"});
    }

    private static int findZoneTemp(String[] types) {
        File base = new File("/sys/class/thermal");
        File[] zones = base.listFiles();
        if (zones == null) return -1;
        for (File zone : zones) {
            if (!zone.getName().startsWith("thermal_zone")) continue;
            String zoneType = readString(zone.getAbsolutePath() + "/type");
            for (String t : types) {
                if (t.equals(zoneType)) {
                    return parseZoneTemp(zone.getAbsolutePath() + "/temp");
                }
            }
        }
        return -1;
    }

    private static int findZoneContains(String[] keywords) {
        File base = new File("/sys/class/thermal");
        File[] zones = base.listFiles();
        if (zones == null) return -1;
        for (File zone : zones) {
            if (!zone.getName().startsWith("thermal_zone")) continue;
            String zoneType = readString(zone.getAbsolutePath() + "/type");
            if (zoneType == null) continue;
            for (String kw : keywords) {
                if (zoneType.contains(kw)) {
                    return parseZoneTemp(zone.getAbsolutePath() + "/temp");
                }
            }
        }
        return -1;
    }

    private static int parseZoneTemp(String path) {
        int raw = readInt(path);
        if (raw <= 0) return -1;
        int celsius = raw / 1000;
        return (celsius > 0 && celsius < 120) ? celsius : -1;
    }

    private static int readInt(String path) {
        String s = readString(path);
        if (s == null) return -1;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String readString(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
