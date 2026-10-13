package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.agriculture.SpiceGrinding;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Preparation;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The Mortar and Pestle (roadmap step 13): used with an alchemical ingredient in the other hand, grinds one into a
 * reagent of the preparation made with this tool (data: the preparation whose tool is {@code jugcraft:mortar}). Anyone
 * may grind; the crucible decides who may use what comes of it. The kitchen grinds its spices with it too: a Dried Chili
 * in the other hand becomes Paprika ({@link SpiceGrinding}).
 */
public class MortarItem extends Item {
	public static final String TOOL = "jugcraft:mortar";

	public MortarItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		ItemStack other = player.getItemInHand(InteractionHand.OFF_HAND);
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		if (SpiceGrinding.grinds(other)) {
			return grindSpice(level, server, other);
		}
		Preparation ground = Alchemy.catalog().madeWith(TOOL);
		String item = ConcordanceProgress.itemId(other);
		if (other.isEmpty() || other.has(JugcraftConcordance.REAGENT) || ground == null || Alchemy.catalog().ingredient(item) == null) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.cannot_grind"));
			return InteractionResult.FAIL;
		}
		if (!RateGate.allow(server, "mortar", 8)) {
			return InteractionResult.FAIL;
		}
		Component name = other.getHoverName();
		other.shrink(1);
		ItemStack reagent = new ItemStack(JugcraftConcordance.REAGENT_ITEM);
		reagent.set(JugcraftConcordance.REAGENT, new Reagent(item, ground.id()));
		server.getInventory().placeItemBackInInventory(reagent, Prediction.SERVER_ONLY);
		server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.ground", name));
		level.playSound(null, server.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.5F, 1.4F);
		return InteractionResult.SUCCESS;
	}

	/** The kitchen's use of the same mortar: a Dried Chili in the other hand is ground into Paprika ({@link SpiceGrinding}). */
	private static InteractionResult grindSpice(Level level, ServerPlayer server, ItemStack other) {
		if (!RateGate.allow(server, "mortar", 8)) {
			return InteractionResult.FAIL;
		}
		Component name = other.getHoverName();
		other.shrink(1);
		server.getInventory().placeItemBackInInventory(SpiceGrinding.result(), Prediction.SERVER_ONLY);
		server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.ground", name));
		level.playSound(null, server.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.5F, 1.4F);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.mortar").withStyle(ChatFormatting.GRAY));
	}
}
