package com.yesclient.mixin;
import java.util.Map;

import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(FMLHandshakeMessage.ModList.class)
public class MixinFMLHandshakeMessage {

    @Shadow(remap = false)
    private Map<String, String> modTags;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true, remap = false)
    private void onToBytes(io.netty.buffer.ByteBuf buffer, CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getMinecraft().isSingleplayer()) {
            return;
        }
        boolean hideSelf = modTags.containsKey("yesclient");
        boolean hideLegacy = modTags.containsKey("yesclient");
        boolean hideChecker = modTags.containsKey("yesclientchecker");
        if (!hideSelf && !hideLegacy && !hideChecker) {
            return;
        }
        ci.cancel();
        int hidden = (hideSelf ? 1 : 0) + (hideLegacy ? 1 : 0) + (hideChecker ? 1 : 0);
        int size = modTags.size() - hidden;
        net.minecraftforge.fml.common.network.ByteBufUtils.writeVarInt(buffer, size, 2);
        for (Map.Entry<String, String> modTag : modTags.entrySet()) {
            String id = modTag.getKey();
            if ("yesclient".equals(id) || "yesclient".equals(id) || "yesclientchecker".equals(id)) {
                continue;
            }
            net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buffer, id);
            net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buffer, modTag.getValue());
        }
    }
}
