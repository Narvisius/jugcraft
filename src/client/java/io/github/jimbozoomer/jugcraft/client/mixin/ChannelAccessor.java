package io.github.jimbozoomer.jugcraft.client.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A channel's OpenAL source, for its low-pass filter (client/SoundOcclusion.java). */
@Mixin(Channel.class)
public interface ChannelAccessor {
	@Accessor("source")
	int jugcraft_getSource();
}
