package io.github.jimbozoomer.jugcraft.concordance;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A draught or a salve (roadmap step 13): the two delivery forms a dose is bottled into, carrying a {@link Brew}.
 * A draught is drunk (its container is a glass bottle); a salve is used on a creature, or on oneself while sneaking,
 * and leaves its bowl. Either way the effects go through the shared effect executor ({@link Alchemy#apply}): someone
 * applying a salve to another is its actor (friendly-fire rules apply); a draught drunk, or a salve on oneself, has no
 * actor, so a harmful dose reaches the one who takes it, as a potion would.
 */
public class BrewItem extends Item {
	private final boolean salve;

	public BrewItem(Properties properties, boolean salve) {
		super(properties);
		this.salve = salve;
	}

	/** Drinking a draught. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		Brew brew = stack.get(JugcraftConcordance.BREW);
		if (!salve && brew != null && level instanceof ServerLevel server) {
			report(entity, Alchemy.apply(server, brew, entity, null));
		}
		return super.finishUsingItem(stack, level, entity);
	}

	/** A salve used on a creature. */
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!salve) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && server.level() instanceof ServerLevel level) {
			applySalve(level, server, stack, target, target == player ? null : player);
		}
		return InteractionResult.SUCCESS;
	}

	/** A salve used on oneself, sneaking. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!salve) {
			return super.use(level, player, hand);
		}
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
			applySalve(serverLevel, server, player.getItemInHand(hand), server, null);
		}
		return InteractionResult.SUCCESS;
	}

	private static void applySalve(ServerLevel level, ServerPlayer user, ItemStack stack, LivingEntity target, Player actor) {
		Brew brew = stack.get(JugcraftConcordance.BREW);
		if (brew == null || !RateGate.allow(user, "salve", 10)) {
			return;
		}
		int applied = Alchemy.apply(level, brew, target, actor);
		stack.shrink(1);
		user.getInventory().placeItemBackInInventory(new ItemStack(Items.BOWL), Prediction.SERVER_ONLY);
		level.playSound(null, target.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.6F, 1.2F);
		user.sendOverlayMessage(Component.translatable(applied > 0 ? "message.jugcraft.concordance.alchemy.applied"
				: "message.jugcraft.concordance.alchemy.no_effect"));
	}

	private static void report(LivingEntity entity, int applied) {
		if (applied == 0 && entity instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.alchemy.no_effect"));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		Brew brew = stack.get(JugcraftConcordance.BREW);
		if (brew == null || brew.effects().isEmpty()) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.brew.none").withStyle(ChatFormatting.GRAY));
		} else {
			for (Brew.Dose dose : brew.effects()) {
				Identifier id = Identifier.tryParse(dose.status());
				Component name = id == null ? Component.literal(dose.status())
						: BuiltInRegistries.MOB_EFFECT.getOptional(id).map(effect -> effect.getDisplayName()).orElse(Component.literal(dose.status()));
				tooltip.accept(Component.translatable("tooltip.jugcraft.concordance.brew.effect", name, dose.amplifier() + 1,
						io.github.jimbozoomer.jugcraft.concordance.compose.Text.seconds(dose.ticks()))
						.withStyle(dose.harmful() ? ChatFormatting.RED : ChatFormatting.BLUE));
			}
		}
		tooltip.accept(Component.translatable(salve ? "tooltip.jugcraft.salve" : "tooltip.jugcraft.draught").withStyle(ChatFormatting.GRAY));
	}
}
