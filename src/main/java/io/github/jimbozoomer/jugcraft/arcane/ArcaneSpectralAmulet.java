package io.github.jimbozoomer.jugcraft.arcane;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Source toggle is available immediately; no research requirement. */
public final class ArcaneSpectralAmulet extends Item {
    ArcaneSpectralAmulet(Properties p){super(p.stacksTo(1));}
    public static boolean active(ItemStack s){return s.getItem() instanceof ArcaneSpectralAmulet&&s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBooleanOr("ArcaneSpectralVision",false);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){if(!level.isClientSide()){var s=player.getItemInHand(hand);boolean enabled=!active(s);CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putBoolean("ArcaneSpectralVision",enabled));player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Spectral vision "+(enabled?"enabled":"disabled")));}return InteractionResult.SUCCESS;}
}
