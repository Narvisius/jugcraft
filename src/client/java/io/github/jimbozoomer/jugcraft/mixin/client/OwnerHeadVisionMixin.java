package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementaries;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Native post-effect requests compose with other effects and automatically disappear when the head is removed. */
@Mixin(LocalPlayer.class)
public abstract class OwnerHeadVisionMixin {
 @Inject(method="getActivePostEffects",at=@At("RETURN"),cancellable=true)
 private void jugcraft$headVision(CallbackInfoReturnable<List<Identifier>> cir){
  var mc=Minecraft.getInstance();var player=(LocalPlayer)(Object)this;
  if(player.isSpectator()||mc.getCameraEntity()!=player||!mc.options.getCameraType().isFirstPerson()||Boolean.getBoolean("jugcraft.disableHeadVision"))return;
  var head=player.getItemBySlot(EquipmentSlot.HEAD);String effect=head.is(OwnerSupplementaries.ITEMS.get("enderman_head"))?"invert":head.is(OwnerSupplementaries.ITEMS.get("spider_head"))?"spider":null;
  if(effect==null)return;var id=Identifier.withDefaultNamespace(effect);var all=new ArrayList<>(cir.getReturnValue());if(!all.contains(id))all.add(id);cir.setReturnValue(List.copyOf(all));
 }
}
