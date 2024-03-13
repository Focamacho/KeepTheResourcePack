package com.focamacho.keeptheresourcepack.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackSource;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KeepTheResourcePackClient implements ClientModInitializer {

    private static final File latestServerResourcePack = new File(FabricLoader.getInstance().getGameDir().toFile(), "latestServerResourcePack.json");
    public static File cacheResourcePackFile = null;

    @Override
    public void onInitializeClient() {
        if(latestServerResourcePack.exists()) {
            try {
                JsonObject jsonObject = new JsonParser().parse(FileUtils.readFileToString(latestServerResourcePack, StandardCharsets.UTF_8)).getAsJsonObject();
                File resourcePack = new File(jsonObject.get("file").getAsString());

                if(resourcePack.exists()) {
                    cacheResourcePackFile = resourcePack;
                    Minecraft.getInstance().getDownloadedPackSource().setServerPack(resourcePack, PackSource.SERVER);
                }
                else setLatestServerResourcePack(null);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public static void setLatestServerResourcePack(File file) {
        if(file == null) latestServerResourcePack.delete();
        else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("file", file.getPath());

            cacheResourcePackFile = file;
            try { FileUtils.writeStringToFile(latestServerResourcePack, jsonObject.toString(), StandardCharsets.UTF_8); } catch(IOException e) { e.printStackTrace(); }
        }
    }

}
