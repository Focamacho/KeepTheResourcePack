package com.focamacho.keeptheresourcepack.client.mixin;

import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundResourcePackPopPacket.class)
public class MixinClientboundResourcePackPopPacket {

    @Inject(method = "handle(Lnet/minecraft/network/protocol/common/ClientCommonPacketListener;)V", at = @At("HEAD"), cancellable = true)
    public void onHandle(ClientCommonPacketListener clientCommonPacketListener, CallbackInfo ci) {
        ci.cancel();
    }

}
