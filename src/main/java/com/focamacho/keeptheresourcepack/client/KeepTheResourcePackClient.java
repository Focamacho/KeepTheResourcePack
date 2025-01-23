package com.focamacho.keeptheresourcepack.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.util.Tuple;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeepTheResourcePackClient implements ClientModInitializer {

    private static final File latestServerResourcePack = new File(FabricLoader.getInstance().getGameDir().toFile(), "latestServerResourcePack.json");

    public static String cacheServer = null;
    public static ArrayList<Tuple<UUID, Path>> cacheResourcePacks = new ArrayList<>();

    public static boolean initialLoading = false;

    @Override
    public void onInitializeClient() {
        if(latestServerResourcePack.exists()) {
            try {
                JsonObject object = JsonParser.parseString(FileUtils.readFileToString(latestServerResourcePack, StandardCharsets.UTF_8)).getAsJsonObject();

                if(!object.has("server") || object.get("server").isJsonNull()) {
                    setLatestServerResourcePacks(null, new ArrayList<>());
                    return;
                }

                String server = object.get("server").getAsString();
                JsonArray array = object.getAsJsonArray("resourcePacks");
                ArrayList<Tuple<UUID, Path>> files = new ArrayList<>();

                for (JsonElement element : array) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    UUID uuid = UUID.fromString(jsonObject.get("uuid").getAsString());
                    Path resourcePack = Paths.get(jsonObject.get("file").getAsString());
                    files.add(new Tuple<>(uuid, resourcePack));
                }

                // Filter non-existent resourcepacks
                files.removeIf((tuple) -> !tuple.getB().toFile().exists());

                if(!files.isEmpty()) {
                    initialLoading = true;

                    DownloadedPackSource downloadedPackSource = Minecraft.getInstance().getDownloadedPackSource();
                    downloadedPackSource.allowServerPacks();
                    files.forEach((tuple) ->
                            downloadedPackSource.pushLocalPack(tuple.getA(), tuple.getB()));

                    for (ServerPackManager.ServerPackData pack : downloadedPackSource.manager.packs) {
                        pack.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                    }

                    downloadedPackSource.startReload(new PackReloadConfig.Callbacks() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onFailure(boolean bl) {}

                        @Override
                        public @NotNull List<PackReloadConfig.IdAndPath> packsToLoad() {
                            return downloadedPackSource.manager.packs.stream()
                                    .map(pack -> new PackReloadConfig.IdAndPath(pack.id, pack.path)).toList();
                        }
                    });

                    cacheServer = server;
                    cacheResourcePacks = files;
                    initialLoading = false;
                }

                if(array.size() != files.size()) setLatestServerResourcePacks(server, files);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public static void setLatestServerResourcePacks(String server, ArrayList<Tuple<UUID, Path>> packs) {
        if(server == null || packs.isEmpty()) {
            latestServerResourcePack.delete();
            cacheServer = null;
            cacheResourcePacks.clear();
        } else {
            JsonObject object = getJsonObject(server, packs);

            cacheServer = server;
            cacheResourcePacks = packs;
            try { FileUtils.writeStringToFile(latestServerResourcePack, object.toString(), StandardCharsets.UTF_8); } catch(IOException e) { e.printStackTrace(); }
        }
    }

    private static @NotNull JsonObject getJsonObject(String server, ArrayList<Tuple<UUID, Path>> packs) {
        JsonObject object = new JsonObject();
        object.addProperty("server", server);

        JsonArray serverPacks = new JsonArray();
        for (Tuple<UUID, Path> pack : packs) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("file", pack.getB().toString());
            jsonObject.addProperty("uuid", pack.getA().toString());
            serverPacks.add(jsonObject);
        }
        object.add("resourcePacks", serverPacks);
        return object;
    }

    public static void pushPack(String server, UUID uuid, Path path) {
        if(cacheResourcePacks.stream().noneMatch((tuple) -> tuple.getA().equals(uuid))) {
            cacheResourcePacks.add(new Tuple<>(uuid, path));
            setLatestServerResourcePacks(server, cacheResourcePacks);
        }
    }

}
