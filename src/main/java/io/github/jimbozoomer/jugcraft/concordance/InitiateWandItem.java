package io.github.jimbozoomer.jugcraft.concordance;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A Concordance instrument: the Initiate's Wand, and the Adept's Wand a ritual attunes it into (roadmap step 12),
 * which differ only in what they hold (data/jugcraft/concordance/instrument). The first Concordance instrument. Spell Engine resolves it as a caster for the {@code jugcraft:concordance} spell tag
 * (data/jugcraft/spell_assignments/initiate_wand.json), and {@link ConcordanceSpells} offers it the invocations its
 * holder has understood, so with one learned the use key casts. With none learned the use reaches this item, which says
 * how to begin instead of doing nothing.
 */
public class InitiateWandItem extends Item {
	public InitiateWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && ConcordanceProgress.knowledge(server).invocations().isEmpty()) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.no_invocations"));
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		// Each instrument names itself: tooltip.jugcraft.initiate_wand, tooltip.jugcraft.adept_wand.
		Identifier id = BuiltInRegistries.ITEM.getKey(this);
		tooltip.accept(Component.translatable("tooltip." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
	}
}
