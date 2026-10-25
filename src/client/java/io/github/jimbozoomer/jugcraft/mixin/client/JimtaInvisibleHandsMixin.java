package io.github.jimbozoomer.jugcraft.mixin.client;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaWardSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class JimtaInvisibleHandsMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void jimta$hideHands(float partial,PoseStack pose,SubmitNodeCollector collector,PlayerRenderState state,FirstPersonHandsAndItemsRenderState hands,CallbackInfo info){var p=Minecraft.getInstance().player;if(p!=null&&JimtaWardSpells.has(p,"true_invisibility"))info.cancel();}
}
