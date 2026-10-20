package io.github.jimbozoomer.jugcraft.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.JugcraftKeyBindings;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Defaults apply to Controls > Reset too; saved invalid Jugcraft keys are repaired during options loading. */
@Mixin(KeyMapping.class)
public abstract class JugcraftKeyMappingMixin {
    @Shadow @Final private String name;

    @ModifyVariable(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;I)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int jugcraft$packDefault(int value, String binding, InputConstants.Type type, int key,
            KeyMapping.Category category, int order) {
        return JugcraftKeyBindings.defaultKey(binding, type, value);
    }

    @ModifyVariable(method = "setKey", at = @At("HEAD"), argsOnly = true)
    private InputConstants.Key jugcraft$repairSavedKey(InputConstants.Key key) {
        return JugcraftKeyBindings.safeSavedKey(name, key);
    }
}
