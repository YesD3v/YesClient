package com.yesclient.utils;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

 
public final class ClientUiSound extends PositionedSoundRecord {
    public ClientUiSound(ResourceLocation location, float volume, float pitch) {
        super(location, volume, pitch, 0.0F, 0.0F, 0.0F);
        this.attenuationType = ISound.AttenuationType.NONE;
    }
}
