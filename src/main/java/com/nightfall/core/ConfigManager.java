package com.nightfall.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightfall.NightfallClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and loads module settings to config/nightfall.json.
 * Modules always start disabled; only their settings are persisted.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nightfall.json");
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.INSTANCE.all()) {
            JsonObject obj = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                obj.add(s.name, s.toJson());
            }
            root.add(m.name, obj);
        }
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(root));
        } catch (IOException e) {
            NightfallClient.LOGGER.error("Could not save nightfall.json", e);
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(f)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Module m : ModuleManager.INSTANCE.all()) {
                JsonElement element = root.get(m.name);
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                for (Setting<?> s : m.getSettings()) {
                    JsonElement v = obj.get(s.name);
                    if (v == null) {
                        continue;
                    }
                    try {
                        s.fromJson(v);
                    } catch (RuntimeException ignored) {
                        // bad value in the file: keep the default
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            NightfallClient.LOGGER.warn("Could not read nightfall.json, using defaults", e);
        }
    }
}
