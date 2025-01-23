package com.focamacho.keeptheresourcepack.client.mixin;

import com.focamacho.keeptheresourcepack.client.KeepTheResourcePackClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {

    @Inject(method = "Lnet/minecraft/client/gui/screens/ConnectScreen;connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V", at = @At("HEAD"))
    public void connect(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData, TransferState transferState, CallbackInfo ci) {
        if(KeepTheResourcePackClient.cacheServer != null && !KeepTheResourcePackClient.cacheServer.equalsIgnoreCase(serverData.ip)) {
            Minecraft.getInstance().getDownloadedPackSource().popAll();
            KeepTheResourcePackClient.setLatestServerResourcePacks(null, new ArrayList<>());
        }
    }

}
