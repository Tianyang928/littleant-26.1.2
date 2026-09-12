package net.tianyang928.littleant.gui;

import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.tianyang928.littleant.LittleAnt;
import net.tianyang928.littleant.entity.ai.brain.BrainBlock;
import net.tianyang928.littleant.entity.ai.brain.JsonToModuleConverter;
import net.tianyang928.littleant.entity.ai.brain.ModuleToJsonConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Client-side brain file storage. Disk operations always run on Minecraft's IO pool. */
public final class BrainFileRepository {
    private BrainFileRepository() {}
    private static final Path DIRECTORY = Minecraft.getInstance().gameDirectory.toPath().resolve("littleant_brains");

    public record BrainFile(String name, boolean preset, Path path) {}

    public static CompletableFuture<List<BrainFile>> list() {
        return CompletableFuture.supplyAsync(() -> {
            try { Files.createDirectories(DIRECTORY); } catch (IOException e) { LittleAnt.LOGGER.warn("Unable to create brain directory", e); }
            List<BrainFile> result = new ArrayList<>();
            Minecraft.getInstance().getResourceManager().listResources("brain_presets", p -> p.getPath().endsWith(".json"))
                    .keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> result.add(new BrainFile(strip(id.getPath()), true, null)));
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORY, "*.json")) {
                for (Path p : stream) result.add(new BrainFile(strip(p.getFileName().toString()), false, p));
            } catch (IOException e) { LittleAnt.LOGGER.warn("Unable to list brain files", e); }
            result.sort(Comparator.comparing(BrainFile::name, String.CASE_INSENSITIVE_ORDER));
            return result;
        }, Util.ioPool());
    }

    public static CompletableFuture<Map<UUID, BrainBlock>> load(BrainFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String source;
                if (file.preset()) {
                    var id = ResourceLocation.fromNamespaceAndPath(LittleAnt.MOD_ID, "brain_presets/" + file.name() + ".json");
                    try (var in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).open()) {
                        source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                } else source = Files.readString(file.path(), StandardCharsets.UTF_8);
                return new JsonToModuleConverter().convert(JsonParser.parseString(source).getAsJsonObject());
            } catch (Exception e) { throw new RuntimeException("Unable to load brain " + file.name(), e); }
        }, Util.ioPool());
    }

    public static CompletableFuture<Path> save(String name, Map<UUID, BrainBlock> blocks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(DIRECTORY);
                String safe = sanitize(name);
                if (safe.isEmpty()) throw new IllegalArgumentException("名称不能为空");
                Path path = DIRECTORY.resolve(safe + ".json");
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(new ModuleToJsonConverter().convert(blocks));
                Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return path;
            } catch (IOException e) { throw new RuntimeException("Unable to save brain", e); }
        }, Util.ioPool());
    }

    public static String sanitize(String name) {
        return name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9._ -]", "_");
    }
    private static String strip(String name) { return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name; }
}
