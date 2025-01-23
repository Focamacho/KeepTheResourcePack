package com.focamacho.keeptheresourcepack.client.mixin;

import com.focamacho.keeptheresourcepack.client.KeepTheResourcePackClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.server.packs.DownloadQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.UUID;

@Mixin(ServerPackManager.class)
public abstract class MixinServerPackManager {

    @Shadow @Final
    PackLoadFeedback packLoadFeedback;

    @Inject(method = "pushNewPack", at = @At("HEAD"), cancellable = true)
    public void pushNewPack(UUID uuid, ServerPackManager.ServerPackData serverPackData, CallbackInfo ci) {
        if(KeepTheResourcePackClient.cacheResourcePacks.stream().anyMatch(tuple -> tuple.getA().equals(uuid))) {
            this.packLoadFeedback.reportFinalResult(serverPackData.id, PackLoadFeedback.FinalResult.APPLIED);
            ci.cancel();
        }
    }

    @Inject(method = "registerForUpdate", at = @At("HEAD"), cancellable = true)
    public void registerForUpdate(CallbackInfo ci) {
        if(KeepTheResourcePackClient.initialLoading)
            ci.cancel();
    }

    @Inject(method = "triggerReloadIfNeeded", at = @At("HEAD"), cancellable = true)
    public void triggerReloadIfNeeded(CallbackInfo ci) {
        if(KeepTheResourcePackClient.initialLoading) {
            ci.cancel();
        }
    }

    @Inject(method = "onDownload", at = @At("RETURN"))
    public void onDownload(Collection<ServerPackManager.ServerPackData> collection, DownloadQueue.BatchResult batchResult, CallbackInfo ci) {
        for (ServerPackManager.ServerPackData serverPackData : collection) {
            ServerData data = Minecraft.getInstance().getCurrentServer();
            if(data == null) return;
            KeepTheResourcePackClient.pushPack(
                    data.ip, serverPackData.id, serverPackData.path
            );
        }
    }

}
