package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.Enemy;
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
 * Keeps the town as it was built. In its protected area (inside the wall and {@code protect} blocks round it, from
 * {@link Town#PROTECT_BELOW} under the ground up) players cannot break or place blocks, use items on blocks, empty or
 * fill buckets, or harm its decorations; they can still open doors and gates, press buttons, and use the town's ATMs,
 * crafting tables and beds (the blocks in {@code #jugcraft:town_usable}) and its shops. Townsfolk cannot be harmed by
 * anyone. Hostile mobs do not spawn naturally inside the wall. Explosions, fire and pistons cannot change protected
 * blocks (the mixins {@code TownExplosionCalculatorMixin}, {@code TownEntityExplosionMixin}, {@code TownFireBlockMixin}
 * and {@code TownPistonMixin} ask {@link #shieldsBlock}).
 *
 * <p>Operators in creative mode are exempt, so a server's staff can repair or change the town; {@code town.protection=off}
 * switches all of it off. Everything is decided on the server.
 */
public final class TownProtection {
	public static final TagKey<Block> USABLE = TagKey.create(Registries.BLOCK, io.github.jimbozoomer.jugcraft.Jugcraft.id("town_usable"));

	private TownProtection() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (denies(player, level, pos)) {
				tell(player);
				return false;
			}
			return true;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				!level.isClientSide() && denies(player, level, pos) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register(TownProtection::useBlock);
		UseItemCallback.EVENT.register(TownProtection::useItem);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> attack(player, level, entity));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> useEntity(player, level, hand, entity));
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) ->
				fromDisk || reason != EntitySpawnReason.NATURAL || !(entity instanceof Enemy) || !Town.isInside(level, entity.blockPosition()));
	}

	public static boolean enabled() {
		return JugcraftConfig.textOption("town.protection").trim().equalsIgnoreCase("on");
	}

	/** Operators in creative mode may change the town. */
	public static boolean exempt(Player player) {
		return player.isCreative() && player instanceof ServerPlayer server
				&& Commands.LEVEL_GAMEMASTERS.check(server.createCommandSourceStack().permissions());
	}

	/** Whether the town stops this player changing this block. */
	public static boolean denies(Player player, Level level, BlockPos pos) {
		return !level.isClientSide() && enabled() && Town.isProtected(level, pos) && !exempt(player);
	}

	/** Whether the town shields a block from explosions, fire and pistons. */
	public static boolean shieldsBlock(Level level, BlockPos pos) {
		return !level.isClientSide() && enabled() && Town.isProtected(level, pos);
	}

	private static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockPos beside = pos.relative(hit.getDirection());
		if (!denies(player, level, pos) && !denies(player, level, beside)) {
			return InteractionResult.PASS;
		}
		BlockState state = level.getBlockState(pos);
		boolean holding = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
		// Doors, gates, buttons and the town's own machines work; sneaking with something in hand would skip them and use
		// the item instead, so that is refused.
		if (state.is(USABLE) && !(player.isSecondaryUseActive() && holding)) {
			return InteractionResult.PASS;
		}
		if (holding) {
			tell(player);
		}
		return InteractionResult.FAIL;
	}

	private static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		// Items whose plain use (not on a block) changes the world: buckets, spawn eggs, and lily pads and frogspawn on
		// water. Block items used on blocks are refused in useBlock; berries and seeds can still be eaten.
		boolean changesBlocks = stack.getItem() instanceof BucketItem || stack.getItem() instanceof SpawnEggItem
				|| stack.is(Items.LILY_PAD) || stack.is(Items.FROGSPAWN);
		if (changesBlocks && denies(player, level, player.blockPosition())) {
			tell(player);
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	private static InteractionResult attack(Player player, Level level, Entity entity) {
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}
		if (entity instanceof Townsfolk && !exempt(player)) {
			return InteractionResult.FAIL;
		}
		if ((entity instanceof HangingEntity || entity instanceof ArmorStand) && denies(player, level, entity.blockPosition())) {
			tell(player);
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	private static InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity) {
		if (entity instanceof Townsfolk townsfolk) {
			// All of a townsperson's interactions go through them (no leads, name tags or anything else).
			return level.isClientSide() ? InteractionResult.SUCCESS : townsfolk.talkTo(player, hand);
		}
		if (!level.isClientSide() && (entity instanceof HangingEntity || entity instanceof ArmorStand)
				&& denies(player, level, entity.blockPosition())) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.PASS;
	}

	private static void tell(Player player) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.town.protected"));
	}
}
