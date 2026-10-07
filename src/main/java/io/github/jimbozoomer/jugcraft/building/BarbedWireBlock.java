package io.github.jimbozoomer.jugcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Barbed wire (batch 50): living things pushing through it are slowed to {@value Trenchworks#WIRE_SLOW} of their speed
 * and cut for {@value Trenchworks#WIRE_DAMAGE} damage (as often as they can be hurt). Someone sneaking picks their way
 * across: slower still, but unhurt. Items and other things pass freely.
 */
public class BarbedWireBlock extends Block {
	private static final Vec3 SNAG = new Vec3(Trenchworks.WIRE_SLOW, 0.8, Trenchworks.WIRE_SLOW);
	private static final Vec3 CAREFUL = new Vec3(Trenchworks.WIRE_SLOW / 2, 0.8, Trenchworks.WIRE_SLOW / 2);

	public BarbedWireBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
			boolean pastEdges) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		boolean careful = living.isShiftKeyDown();
		living.makeStuckInBlock(state, careful ? CAREFUL : SNAG);
		if (!careful && level instanceof ServerLevel server && living.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
			living.hurtServer(server, server.damageSources().sweetBerryBush(), Trenchworks.WIRE_DAMAGE);
		}
	}
}
