package io.github.jimbozoomer.jugcraft.placement;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.BlueprintItem;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * How far away a structure can be placed (Justin, October 8 2026): as far as any block can be placed, plus twice the
 * structure's longest side. It replaces the blueprints' old fixed 80 blocks, and goes for every MBS that is placed whole
 * from one item: blueprints, storage tank kits, the nuclear plant's equipment, power line supports and boxes, the
 * plant's multi-block furniture (wall screens, tables, racks, panels) and multi-block machines.
 *
 * <p>While such an item is in the main hand the player's block interaction range carries a modifier of twice its
 * longest side ({@link #MODIFIER}), set every tick on the server and on the client alike, so the crosshair outlines
 * the far block and placing there passes the server's own reach check. The longer reach is for placing only: beyond
 * the ordinary reach a click places the item without using the block clicked (no far doors, levers or chests) and
 * no block can be broken.
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
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				bonus(player) > 0 && !withinOrdinaryReach(player, pos) ? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> bonus(player) <= 0 || withinOrdinaryReach(player, pos));
	}

	/** Sets (or clears) the reach modifier for what the player holds in the main hand. Called every tick on both sides. */
	public static void update(Player player) {
		AttributeInstance range = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
		if (range == null) {
			return;
		}
		double wanted = 2.0 * longestSide(player.getMainHandItem(), player.level().isClientSide());
		AttributeModifier current = range.getModifier(MODIFIER);
		if (current != null && current.amount() == wanted) {
			return;
		}
		if (current != null) {
			range.removeModifier(MODIFIER);
		}
		if (wanted > 0) {
			range.addTransientModifier(new AttributeModifier(MODIFIER, wanted, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/** The reach modifier the player has now (0 without one). */
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
			if (block instanceof MultiFurnitureBlock furniture) {
				side = furniture.longestSide();
			} else if (block instanceof ConferenceTableBlock) {
				side = 3;
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
