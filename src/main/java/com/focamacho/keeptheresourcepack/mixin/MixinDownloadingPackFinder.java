package com.focamacho.keeptheresourcepack.mixin;

import com.focamacho.keeptheresourcepack.KeepTheResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(DownloadedPackSource.class)
public abstract class MixinDownloadingPackFinder {

    @Shadow @Nullable private Pack serverPack;

    @Shadow @Final private static Component SERVER_NAME;

    @Shadow @Nullable private CompletableFuture<?> currentDownload;

    @Shadow @Final private ReentrantLock downloadLock;

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public CompletableFuture<Void> clearServerPack() {
        this.downloadLock.lock();

        CompletableFuture<Void> completablefuture;
        try {
            if (this.currentDownload != null) {
                this.currentDownload.cancel(true);
            }

            this.currentDownload = null;
            completablefuture = CompletableFuture.completedFuture(null);
        } finally {
            this.downloadLock.unlock();
        }

        return completablefuture;
    }

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public CompletableFuture<Void> setServerPack(File fileIn, PackSource source) {
        Pack.ResourcesSupplier pack$resourcessupplier = (p_255464_) -> new FilePackResources(p_255464_, fileIn, false);
        Pack.Info pack$info = Pack.readPackInfo("server", pack$resourcessupplier);
        if (pack$info == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid pack metadata at " + fileIn));
        } else {
            LOGGER.info("Applying server pack {}", fileIn);

            Pack newServerPack = Pack.create("server", SERVER_NAME, true, pack$resourcessupplier, pack$info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, source);
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

}
