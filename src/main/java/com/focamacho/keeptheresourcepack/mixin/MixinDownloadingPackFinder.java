package com.focamacho.keeptheresourcepack.mixin;

import com.focamacho.keeptheresourcepack.KeepTheResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DownloadingPackFinder;
import net.minecraft.resources.FilePack;
import net.minecraft.resources.IPackNameDecorator;
import net.minecraft.resources.PackCompatibility;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.data.PackMetadataSection;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(DownloadingPackFinder.class)
public abstract class MixinDownloadingPackFinder {

    @Shadow @Final private ReentrantLock lockDownload;

    @Shadow @Nullable private CompletableFuture<?> currentDownload;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable private ResourcePackInfo serverPack;

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public void clearResourcePack() {
        this.lockDownload.lock();

        try {
            if (this.currentDownload != null) {
                this.currentDownload.cancel(true);
            }

            this.currentDownload = null;
        } finally {
            this.lockDownload.unlock();
        }
    }

    /**
     * @author KeepTheResourcePack
     * @reason Keep the Resource Pack loaded even after leaving the server.
     */
    @Overwrite
    public CompletableFuture<Void> setServerPack(File fileIn, IPackNameDecorator p_217816_2_) {
        PackMetadataSection packmetadatasection;
        try (FilePack filepack = new FilePack(fileIn)) {
            packmetadatasection = filepack.getMetadata(PackMetadataSection.SERIALIZER);
        } catch (IOException ioexception) {
            return Util.completedExceptionallyFuture(new IOException(String.format("Invalid resourcepack at %s", fileIn), ioexception));
        }

        LOGGER.info("Applying server pack {}", fileIn);
        ResourcePackInfo newServerPack = new ResourcePackInfo("server", true, () -> new FilePack(fileIn), new TranslationTextComponent("resourcePack.server.name"), packmetadatasection.getDescription(), PackCompatibility.getCompatibility(packmetadatasection.getPackFormat()), ResourcePackInfo.Priority.TOP, true, p_217816_2_);

        CompletableFuture<Void> returnValue = null;
        if(this.serverPack == null || !fileIn.equals(KeepTheResourcePack.cacheResourcePackFile)) {
            this.serverPack = newServerPack;
            if(!fileIn.equals(KeepTheResourcePack.cacheResourcePackFile)) {
                KeepTheResourcePack.setLatestServerResourcePack(fileIn);
                returnValue = Minecraft.getInstance().scheduleResourcesRefresh();
            }
        }

        if(returnValue == null) {
            returnValue = new CompletableFuture<>();
            try { returnValue.complete(null); } catch(Exception ignored) {}
        }

        return returnValue;
    }

}
