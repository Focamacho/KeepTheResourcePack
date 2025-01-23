package com.focamacho.keeptheresourcepack.client.mixin;

import com.focamacho.keeptheresourcepack.client.KeepTheResourcePackClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(DownloadedPackSource.class)
public abstract class MixinDownloadedPackSource {

    @Inject(method = "cleanupAfterDisconnect", at = @At("HEAD"), cancellable = true)
    public void cleanupAfterDisconnect(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "startReload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<Void> reloadResourcePacks(Minecraft instance) {
        if(KeepTheResourcePackClient.initialLoading) {
            return CompletableFuture.completedFuture(null);
        }

        return instance.reloadResourcePacks();
    }

}