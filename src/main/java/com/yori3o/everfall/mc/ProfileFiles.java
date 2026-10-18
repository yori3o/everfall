package com.yori3o.everfall.mc;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Load in priority order: packaged defaults, active resource packs, then config overrides. */
final class ProfileFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger("everfall");

    @FunctionalInterface
    interface Loader { void load(String id, Reader reader) throws IOException; }

    static boolean validId(String id) { return id != null && id.matches("[a-z0-9_.-]+"); }

    static void load(Path config, ResourceManager resources, boolean additional, Loader loader) {
        String folder = additional ? "profiles/additional" : "profiles";
        if (resources != null) {
            try {
                var stacks = resources.listResourceStacks(folder, id -> id.getNamespace().equals("everfall")
                        && id.getPath().endsWith(".json")
                        && !id.getPath().substring(folder.length() + 1).contains("/"));
                for (var entry : stacks.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList()) {
                    String path = entry.getKey().getPath();
                    String id = path.substring(folder.length() + 1, path.length() - 5);
                    for (var resource : entry.getValue()) {
                        try (Reader reader = resource.openAsReader()) {
                            if (!validId(id)) throw new IllegalArgumentException("Invalid profile filename: " + id);
                            loader.load(id, reader);
                        } catch (IOException | RuntimeException exception) {
                            LOGGER.warn("[Everfall] Skipping profile {} from resource pack {}; retaining lower-priority definition if available",
                                    entry.getKey(), resource.sourcePackId(), exception);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("[Everfall] Could not scan resource-pack folder {}", folder, exception);
            }
        }
        Path directory = config.resolve("everfall").resolve(folder);
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .sorted().toList()) {
                    String filename = file.getFileName().toString();
                    String id = filename.substring(0, filename.length() - 5);
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        if (!validId(id)) throw new IllegalArgumentException("Invalid profile filename: " + id);
                        loader.load(id, reader);
                    } catch (IOException | RuntimeException exception) {
                        LOGGER.warn("[Everfall] Skipping profile {}; retaining lower-priority definition if available", file, exception);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Everfall] Could not scan profile directory {}; using resource profiles", directory, exception);
        }
    }
}
