package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.town.TownProtection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The foam sprayer (batch 32): aimed at a block up to {@link ConstructionChemistry#SPRAY_RANGE} away, it fills the open
 * space in front of that face with construction foam, spreading through air, water, lava and plants to at most
 * {@link ConstructionChemistry#SPRAY_BLOCKS} blocks within {@link ConstructionChemistry#SPRAY_RADIUS} of where it lands.
 * Each block uses one point of a foam canister from the inventory. It never replaces a solid block, never fills a space
 * a mob or player stands in, and respects spawn protection and the walled town.
 */
public class FoamSprayerItem extends ConstructionChemistry.Described {
	public FoamSprayerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack sprayer = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		HitResult hit = player.pick(ConstructionChemistry.SPRAY_RANGE, 1.0F, true);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		boolean free = player.hasInfiniteMaterials();
		int available = free ? ConstructionChemistry.SPRAY_BLOCKS : foamLeft(player);
		if (available <= 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.foam_sprayer.empty"));
			return InteractionResult.FAIL;
		}
		BlockPos start = server.getBlockState(block.getBlockPos()).canBeReplaced() ? block.getBlockPos()
				: block.getBlockPos().relative(block.getDirection());
		List<BlockPos> filled = fill(server, player, start, Math.min(available, ConstructionChemistry.SPRAY_BLOCKS));
		if (filled.isEmpty()) {
			return InteractionResult.FAIL;
		}
		for (BlockPos pos : filled) {
			server.setBlockAndUpdate(pos, ConstructionChemistry.CONSTRUCTION_FOAM.defaultBlockState());
			server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
		}
		if (!free) {
			useFoam(player, filled.size());
		}
		Vec3 at = Vec3.atCenterOf(start);
		server.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.6F);
		player.getCooldowns().addCooldown(sprayer, ConstructionChemistry.SPRAY_COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The open blocks the foam fills from {@code start}, nearest first: through replaceable blocks (air, liquids, plants)
	 * that the player may build in and nobody stands in, within the spray's radius, at most {@code limit}.
	 */
	public static List<BlockPos> fill(ServerLevel level, Player player, BlockPos start, int limit) {
		List<BlockPos> out = new ArrayList<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start);
		seen.add(start);
		double reach = ConstructionChemistry.SPRAY_RADIUS * ConstructionChemistry.SPRAY_RADIUS;
		BlockState foam = ConstructionChemistry.CONSTRUCTION_FOAM.defaultBlockState();
		while (!queue.isEmpty() && out.size() < limit) {
			BlockPos pos = queue.poll();
			if (!level.getBlockState(pos).canBeReplaced() || !mayBuild(level, player, pos)) {
				continue;
			}
			if (level.isUnobstructed(foam, pos, CollisionContext.empty())) {
				out.add(pos);
			}
			for (Direction direction : Direction.values()) {
				BlockPos next = pos.relative(direction);
				if (next.distSqr(start) <= reach && seen.add(next)) {
					queue.add(next);
				}
			}
		}
		return out;
	}

	private static boolean mayBuild(ServerLevel level, Player player, BlockPos pos) {
		return level.isInWorldBounds(pos) && level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY)
				&& !TownProtection.denies(player, level, pos);
	}

	/** Foam left in every canister the player carries. */
	public static int foamLeft(Player player) {
		int total = 0;
		for (ItemStack stack : canisters(player)) {
			total += stack.getMaxDamage() - stack.getDamageValue();
		}
		return total;
	}

	/** Uses {@code blocks} points of foam from the player's canisters; an emptied canister is used up. */
	public static void useFoam(Player player, int blocks) {
		for (ItemStack stack : canisters(player)) {
			if (blocks <= 0) {
				return;
			}
			// Canisters have durability, so they do not stack: one canister per stack.
			int take = Math.min(blocks, stack.getMaxDamage() - stack.getDamageValue());
			blocks -= take;
			if (stack.getDamageValue() + take >= stack.getMaxDamage()) {
				stack.shrink(1);
			} else {
				stack.setDamageValue(stack.getDamageValue() + take);
			}
		}
	}

	private static List<ItemStack> canisters(Player player) {
		List<ItemStack> out = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ConstructionChemistry.FOAM_CANISTER)) {
				out.add(stack);
			}
		}
		return out;
	}
}
