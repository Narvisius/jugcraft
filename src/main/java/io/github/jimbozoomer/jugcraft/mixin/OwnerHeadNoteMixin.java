package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerMobHeads;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
public abstract class OwnerHeadNoteMixin {
    @Inject(method="getCustomSoundId",at=@At("HEAD"),cancellable=true)
    private void jugcraft$ownerHead(Level level,BlockPos pos,CallbackInfoReturnable<Identifier> result){var sound=OwnerMobHeads.note(level,pos);if(sound!=null)result.setReturnValue(sound);}
}
