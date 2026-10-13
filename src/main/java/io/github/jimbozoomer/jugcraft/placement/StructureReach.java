package io.github.jimbozoomer.jugcraft.placement;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.nuclear.ConferenceTableBlock;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentItem;
import io.github.jimbozoomer.jugcraft.nuclear.MultiFurnitureBlock;
import io.github.jimbozoomer.jugcraft.nuclear.TankKitItem;
import io.github.jimbozoomer.jugcraft.powerline.LineItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * How far away a structure can be placed (Justin, October 8 2026): as far as any block can be placed, plus twice the
 * structure's longest side. It replaces the blueprints' old fixed 80 blocks, and goes for every MBS that is placed whole
 * from one item: blueprints, storage tank kits, the nuclear plant's equipment, power line supports and boxes, the
 * plant's multi-block furniture (wall screens, tables, racks, panels) and multi-block machines.
 *
 * <p>While such an item is in the main hand the player's block interaction range carries a modifier of twice its
 * longest side ({@link #MODIFIER}), set every tick on the server and on the client alike, so the crosshair outlines
 * the far block and placing there passes the server's own reach check. The game holds that range to at most 64 blocks,
 * so the modifier is only as much as fits under that; further out (a turbine-generator reaches 100 blocks further than
 * a block), aiming at the ground with nothing under the crosshair and using the item places it where the player looks,
 * up to the full reach ({@link #farTarget}; the server works out the spot itself, as blueprints always have). The
 * longer reach is for placing only: beyond the ordinary reach a click places the item without using the block clicked
 * (no far doors, levers or chests) and no block can be broken.
 */
public final class StructureReach {
	public static final Identifier MODIFIER = Jugcraft.id("structure_reach");

	private StructureReach() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				update(player);
			}
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || bonus(player) <= 0 || withinOrdinaryReach(player, hit.getBlockPos())) {
				return InteractionResult.PASS;
			}
			// Beyond the ordinary reach: place the structure, never use the block that was clicked.
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!level.mayInteract(player, hit.getBlockPos())) {
				return InteractionResult.FAIL;
			}
			InteractionResult placed = player.getItemInHand(hand).useOn(new UseOnContext(player, hand, hit));
			return placed == InteractionResult.PASS ? InteractionResult.FAIL : placed;
		});
		// Beyond what the crosshair reaches (the game's 64 blocks): the item used with nothing under the crosshair places
		// the structure on the block the player looks at, up to its full reach. Blueprints do this themselves.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || level.isClientSide()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof BlueprintItem || longestSide(stack, false) <= 0) {
				return InteractionResult.PASS;
			}
			BlockHitResult hit = farTarget(player, stack, 1.0F);
			if (hit == null) {
				return InteractionResult.PASS;
			}
			if (!level.mayInteract(player, hit.getBlockPos())) {
				return InteractionResult.FAIL;
			}
			InteractionResult placed = stack.useOn(new UseOnContext(player, hand, hit));
			return placed == InteractionResult.PASS ? InteractionResult.FAIL : placed;
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				bonus(player) > 0 && !withinOrdinaryReach(player, pos) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> bonus(player) <= 0 || withinOrdinaryReach(player, pos));
	}

	/** The most the game lets a block interaction range be (64 blocks). */
	public static double maxRange() {
		return Attributes.BLOCK_INTERACTION_RANGE.value() instanceof RangedAttribute ranged ? ranged.getMaxValue() : 64.0;
	}

	/**
	 * Sets (or clears) the reach modifier for what the player holds in the main hand: twice the structure's longest side,
	 * or as much of it as fits under {@link #maxRange()}. Called every tick on both sides.
	 */
	public static void update(Player player) {
		AttributeInstance range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
		if (range == null) {
			return;
		}
		double wanted = 2.0 * longestSide(player.getMainHandItem(), player.level().isClientSide());
		AttributeModifier current = range.getModifier(MODIFIER);
		double ordinary;
		if (current == null) {
			ordinary = range.getValue();
		} else if (range.getValue() < maxRange()) {
			ordinary = range.getValue() - current.amount();
		} else {
			// Held at the limit: what it is without the modifier can't be read off the total.
			range.removeModifier(MODIFIER);
			ordinary = range.getValue();
			range.addTransientModifier(current);
		}
		double amount = Math.max(0, Math.min(wanted, maxRange() - ordinary));
		if (current != null && current.amount() == amount) {
			return;
		}
		if (current != null) {
			range.removeModifier(MODIFIER);
		}
		if (amount > 0) {
			range.addTransientModifier(new AttributeModifier(MODIFIER, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/** The reach modifier the player has now (0 without one): how much further than a block the crosshair reaches. */
	public static double bonus(Player player) {
		AttributeInstance range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
		AttributeModifier modifier = range == null ? null : range.getModifier(MODIFIER);
		return modifier == null ? 0 : modifier.amount();
	}

	/** Whether {@code pos} is within the reach the player has without the modifier (how far any block can be placed). */
	public static boolean withinOrdinaryReach(Player player, BlockPos pos) {
		return player.isWithinBlockInteractionRange(pos, -bonus(player));
	}

	/**
	 * The block the player looks at within {@code stack}'s full placing reach ({@link #range}), or null: where the
	 * structure goes when it is further than the crosshair reaches (the server's use of the item, the client's preview).
	 */
	public static @Nullable BlockHitResult farTarget(Player player, ItemStack stack, float partialTick) {
		HitResult hit = player.pick(range(player, stack), partialTick, false);
		return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
	}

	/**
	 * How far {@code stack} can be placed from: the ordinary block reach plus twice the structure's longest side. Worked
	 * out from the stack itself (not the modifier), so it holds the moment the item comes into the hand.
	 */
	public static double range(Player player, ItemStack stack) {
		return player.blockInteractionRange() - bonus(player) + 2.0 * longestSide(stack, player.level().isClientSide());
	}

	/** The longest side, in blocks, of the structure {@code stack} places; 0 for anything that is not an MBS. */
	public static int longestSide(ItemStack stack, boolean client) {
		if (stack.isEmpty()) {
			return 0;
		}
		int side = 0;
		if (stack.getItem() instanceof BlueprintItem) {
			Blueprint blueprint = Blueprint.get(BlueprintItem.idOf(stack), client);
			side = blueprint == null ? 0 : longest(blueprint);
		} else if (stack.getItem() instanceof TankKitItem kit) {
			Blueprint blueprint = TankKitItem.blueprint(kit.kit);
			side = blueprint == null ? 0 : longest(blueprint);
		} else if (stack.getItem() instanceof EquipmentItem equipment) {
			side = Math.max(equipment.kind.wide, Math.max(equipment.kind.deep, equipment.kind.tall));
		} else if (stack.getItem() instanceof LineItem line) {
			side = Math.max(line.kind.height, 2 * line.kind.halfWidth() + 1);
		} else if (stack.getItem() instanceof BlockItem item) {
			Block block = item.getBlock();
			if (block instanceof io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock station) {
				side = station.longestSide();
			} else if (block instanceof io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock) {
				side = 3;
			} else if (block instanceof MultiFurnitureBlock furniture) {
				side = furniture.longestSide();
			} else if (block instanceof ConferenceTableBlock) {
				side = 3;
			} else if (block instanceof LargeMachineBlock machine) {
				// As its item places it: an enlarged machine's default is the one-block copy from before it grew.
				side = longest(machine.footprint(machine.formed(machine.defaultBlockState())).offsets());
			} else if (block instanceof MachineBlock machine) {
				side = longest(machine.footprint(machine.defaultBlockState()).offsets());
			}
		}
		return side > 1 ? side : 0;
	}

	private static int longest(Blueprint blueprint) {
		return Math.max(blueprint.sizeX, Math.max(blueprint.sizeY, blueprint.sizeZ));
	}

	private static int longest(java.util.List<Vec3i> offsets) {
		int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
		for (Vec3i o : offsets) {
			minX = Math.min(minX, o.getX());
			minY = Math.min(minY, o.getY());
			minZ = Math.min(minZ, o.getZ());
			maxX = Math.max(maxX, o.getX());
			maxY = Math.max(maxY, o.getY());
			maxZ = Math.max(maxZ, o.getZ());
		}
		return 1 + Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
	}
}
