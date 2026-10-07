package io.github.jimbozoomer.jugcraft.agriculture;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The mandrake's scream (the graveyard flora; tools/agriculture.py MANDRAKE): pulling up a ripe mandrake (the crop at
 * its last age) or a wild one makes it scream (shears take a wild one whole and quietly, root and all left in it). Every player
 * within {@value #SCREAM_RADIUS} blocks with nothing on their head is sickened for {@value #NAUSEA_SECONDS} seconds;
 * anything worn on the head (a helmet, a hat, a carved pumpkin) covers the ears. Pulling one with your own ears covered
 * earns Mind Your Ears. All of it happens on the server after the block is broken.
 */
public final class Mandrakes {
	public static final String CROP = "mandrake_crop";
	public static final String WILD = "wild_mandrake";
	public static final String ROOT = "mandrake_root";
	public static final int SCREAM_RADIUS = 8;
	public static final int NAUSEA_SECONDS = 8;
	public static final String ADVANCEMENT = "mind_your_ears";

	private Mandrakes() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer puller && screams(state)
					&& !(state.is(JugcraftAgriculture.block(WILD)) && player.getMainHandItem().is(Items.SHEARS))) {
				scream(server, pos, puller);
			}
		});
	}

	/** Whether pulling up a block in this state screams: a ripe mandrake crop or a wild mandrake. */
	public static boolean screams(BlockState state) {
		if (state.is(JugcraftAgriculture.block(WILD))) {
			return true;
		}
		return state.is(JugcraftAgriculture.block(CROP)) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
	}

	/** Whether this player's ears are covered: anything worn on the head. */
	public static boolean earsCovered(Player player) {
		return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
	}

	/** The scream at `pos`: the sound, a puff of spores, and nausea for every bare-headed player in range. */
	public static void scream(ServerLevel level, BlockPos pos, ServerPlayer puller) {
		level.playSound(null, pos, SoundEvents.FOX_SCREECH, SoundSource.BLOCKS, 1.6F, 1.7F);
		level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 0.5F, 1.9F);
		level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.0);
		AABB reach = new AABB(pos).inflate(SCREAM_RADIUS);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, reach,
				p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(pos)) <= SCREAM_RADIUS * SCREAM_RADIUS)) {
			if (!earsCovered(player)) {
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_SECONDS * 20, 0));
			}
		}
		if (earsCovered(puller)) {
			TrickOrTreat.award(puller, ADVANCEMENT);
		}
	}
}
