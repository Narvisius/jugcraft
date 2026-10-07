package io.github.jimbozoomer.jugcraft.client.mixin;

import io.github.jimbozoomer.jugcraft.client.SoundOcclusion;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sound through walls (client/SoundOcclusion.java): a sound is muffled for the walls between it and you as it starts. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Shadow
	@Final
	private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

	@Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("RETURN"))
	private void jugcraft$occlude(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> result) {
		SoundOcclusion.started(sound, instanceToChannel.get(sound));
	}
}
