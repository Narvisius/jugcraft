package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.botany.BotanicalFields;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SoundEngine.class)
public abstract class BotanicalSoundMixin {
    @Inject(method="calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",at=@At("RETURN"),cancellable=true)
    private void jugcraft$muffle(SoundInstance sound,CallbackInfoReturnable<Float> cir){
        var level=Minecraft.getInstance().level;
        if(level==null||sound.isRelative()||sound.getSource()==SoundSource.MUSIC||sound.getSource()==SoundSource.RECORDS||sound.getSource()==SoundSource.MASTER)return;
        int count=BotanicalFields.mufflers(level,new Vec3(sound.getX(),sound.getY(),sound.getZ()));if(count>0)cir.setReturnValue(cir.getReturnValue()*(float)Math.pow(.5,count));
    }
}
