package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Nothing in a lair can be built or broken (docs/features/witching-season.md, the lairs' shared rules). On the server,
 * for everyone but an operator in creative mode: breaking is refused before the block cracks; using an item on a block
 * or using a block is refused, except the lair's own fixtures ({@code #jugcraft:lair_fixtures}: its exits and braziers);
 * buckets, spawn eggs and the like are refused; hanging things and armour stands cannot be used or struck. With the build
 * ability taken away inside ({@link Lairs#restrict}) the client does not try either. Ender pearls, chorus fruit, food,
 * potions and weapons work as anywhere.
 */
public final class LairRules {
	public static final TagKey<Block> FIXTURES = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));

	private LairRules() {
	}

	static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (denies(player, level)) {
				tell(player);
				return false;
			}
			return true;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				denies(player, level) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register(LairRules::useBlock);
		UseItemCallback.EVENT.register(LairRules::useItem);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> protectedThing(player, level, entity)
				? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> protectedThing(player, level, entity)
				? InteractionResult.FAIL : InteractionResult.PASS);
	}

	/** Whether a lair stops this player changing it here. */
	public static boolean denies(Player player, Level level) {
		return !level.isClientSide() && Lairs.isLair(level) && !Lairs.exempt(player);
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (!denies(player, level)) {
			return InteractionResult.PASS;
		}
		BlockState state = level.getBlockState(hit.getBlockPos());
		boolean holding = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
		// The lair's own fixtures work; sneaking with something in hand would skip them and use the item instead.
		if (state.is(FIXTURES) && !(player.isSecondaryUseActive() && holding)) {
			return InteractionResult.PASS;
		}
		if (holding) {
			tell(player);
		}
		return InteractionResult.FAIL;
	}

	public static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		if (!denies(player, level)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		boolean changesBlocks = stack.getItem() instanceof BucketItem || stack.getItem() instanceof SpawnEggItem
				|| stack.is(Items.LILY_PAD) || stack.is(Items.FROGSPAWN) || stack.is(Items.END_CRYSTAL);
		if (changesBlocks) {
			tell(player);
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	private static boolean protectedThing(Player player, Level level, Entity entity) {
		return (entity instanceof HangingEntity || entity instanceof ArmorStand) && denies(player, level);
	}

	private static void tell(Player player) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.lair.protected"));
	}
}
