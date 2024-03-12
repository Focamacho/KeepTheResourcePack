package com.focamacho.keeptheresourcepack.mixin;

import com.focamacho.keeptheresourcepack.KeepTheResourcePack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(ClientPackSource.class)
public abstract class MixinDownloadingPackFinder {

    @Shadow @Nullable private Pack serverPack;

    @Shadow @Final private ReentrantLock downloadLock;

    @Shadow @Nullable private CompletableFuture<?> currentDownload;

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public void clearServerPack() {
        this.downloadLock.lock();

        try {
            if (this.currentDownload != null) {
                this.currentDownload.cancel(true);
            }

            this.currentDownload = null;
        } finally {
            this.downloadLock.unlock();
        }
    }

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public CompletableFuture<Void> setServerPack(File fileIn, PackSource source) {
        PackMetadataSection packmetadatasection;
        try {
            FilePackResources filepackresources = new FilePackResources(fileIn);

            try {
                packmetadatasection = filepackresources.getMetadataSection(PackMetadataSection.SERIALIZER);
            } catch (Throwable throwable1) {
                try {
                    filepackresources.close();
                } catch (Throwable throwable) {
                    throwable1.addSuppressed(throwable);
                }

                throw throwable1;
            }

            filepackresources.close();
        } catch (IOException ioexception) {
            return Util.failedFuture(new IOException(String.format("Invalid resourcepack at %s", fileIn), ioexception));
        }

        LOGGER.info("Applying server pack {}", fileIn);

        Pack newServerPack = new Pack("server", true, () -> new FilePackResources(fileIn), new TranslatableComponent("resourcePack.server.name"), packmetadatasection.getDescription(), PackCompatibility.forMetadata(packmetadatasection, PackType.CLIENT_RESOURCES), Pack.Position.TOP, true, source);
        CompletableFuture<Void> returnValue = null;
        if(this.serverPack == null || !fileIn.equals(KeepTheResourcePack.cacheResourcePackFile)) {
            this.serverPack = newServerPack;
            if(!fileIn.equals(KeepTheResourcePack.cacheResourcePackFile)) {
                KeepTheResourcePack.setLatestServerResourcePack(fileIn);
                returnValue = Minecraft.getInstance().delayTextureReload();
            }
        }

        if(returnValue == null) {
            returnValue = new CompletableFuture<>();
            try { returnValue.complete(null); } catch(Exception ignored) {}
        }

        return returnValue;
    }

}
