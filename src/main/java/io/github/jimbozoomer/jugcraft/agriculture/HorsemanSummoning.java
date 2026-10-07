package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Summoning the {@link HeadlessHorseman}: during the Halloween event, around midnight (within {@value #HOUR_WINDOW}
 * ticks of it on the overworld clock), sneak-use a Scarecrow whose head is a lit jack o'lantern or a lit hand-carved
 * pumpkin, under the open sky, in the overworld, not in peaceful. The Horseman claims the head (it is used up, with
 * a flash of harmless lightning) and rides in {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks away, hunting
 * whoever called him; the scarecrow becomes the centre of his arena. Only one Horseman rides within
 * {@value #ONE_AT_A_TIME} blocks at a time, and none while the {@code agriculture} feature is switched off. Everything is
 * checked on the server.
 */
public final class HorsemanSummoning {
	public static final long MIDNIGHT = 18000;
	public static final long HOUR_WINDOW = 1000;
	public static final int MIN_DISTANCE = 12;
	public static final int MAX_DISTANCE = 16;
	public static final int ONE_AT_A_TIME = 128;

	public enum Result {
		SUMMONED, DISABLED, OUT_OF_SEASON, WRONG_HOUR, WRONG_PLACE, PEACEFUL, NO_HEAD, ROOFED, ALREADY_RIDING
	}

	private HorsemanSummoning() {
	}

	/** Whether {@code head} (what a scarecrow wears) can be the Horseman's head: a jack o'lantern or lit hand-carved pumpkin. */
	public static boolean litPumpkin(ItemStack head) {
		return ScarecrowBlockEntity.lit(head);
	}

	/** Tries to summon the Horseman with the scarecrow whose lower half is at {@code scarecrow}. */
	public static Result summon(ServerPlayer player, BlockPos scarecrow) {
		return summon(player, scarecrow, player.level().getOverworldClockTime());
	}

	/** Tries to summon the Horseman as if the overworld clock read {@code dayTime}. */
	public static Result summon(ServerPlayer player, BlockPos scarecrow, long dayTime) {
		ServerLevel level = player.level();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
			return Result.DISABLED;
		}
		if (!HalloweenSeason.active()) {
			return Result.OUT_OF_SEASON;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return Result.WRONG_PLACE;
		}
		long hour = Math.floorMod(dayTime, TrickOrTreat.DAY);
		if (Math.abs(hour - MIDNIGHT) > HOUR_WINDOW) {
			return Result.WRONG_HOUR;
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return Result.PEACEFUL;
		}
		// The head is the one the scarecrow wears, on its upper half.
		BlockPos head = scarecrow.above();
		if (!(level.getBlockEntity(head) instanceof ScarecrowBlockEntity worn) || !litPumpkin(worn.head())) {
			return Result.NO_HEAD;
		}
		if (!Wisps.openSky(level, head.above())) {
			return Result.ROOFED;
		}
		if (!level.getEntitiesOfClass(HeadlessHorseman.class, new AABB(scarecrow).inflate(ONE_AT_A_TIME), HeadlessHorseman::isAlive).isEmpty()) {
			return Result.ALREADY_RIDING;
		}
		HeadlessHorseman horseman = JugcraftAgriculture.HEADLESS_HORSEMAN.create(level, EntitySpawnReason.TRIGGERED);
		if (horseman == null) {
			return Result.WRONG_PLACE;
		}
		worn.setHead(ItemStack.EMPTY);
		Entity bolt = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("lightning_bolt")).create(level, EntitySpawnReason.TRIGGERED);
		if (bolt instanceof LightningBolt lightning) {
			lightning.setVisualOnly(true);
			lightning.snapTo(Vec3.atBottomCenterOf(head.above()));
			level.addFreshEntity(lightning);
		}
		Vec3 spot = rideInSpot(level, scarecrow);
		horseman.snapTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
		horseman.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET, Vec3.atBottomCenterOf(scarecrow));
		horseman.setHome(scarecrow);
		horseman.setPersistenceRequired();
		level.addFreshEntity(horseman);
		horseman.hunt(player);
		for (ServerPlayer near : level.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(scarecrow)) < 64.0 * 64.0)) {
			near.sendSystemMessage(Component.translatable("message.jugcraft.horseman.summoned"));
		}
		return Result.SUMMONED;
	}

	/**
	 * A spot on the ground {@value #MIN_DISTANCE} to {@value #MAX_DISTANCE} blocks from the scarecrow where entities tick (a chunk can be
	 * loaded before its entities are, and a Horseman put there would stand frozen and unseen); beside it if none is found.
	 */
	static Vec3 rideInSpot(ServerLevel level, BlockPos scarecrow) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
			int distance = MIN_DISTANCE + level.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
			int x = scarecrow.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = scarecrow.getZ() + (int) Math.round(Math.sin(angle) * distance);
			if (level.isPositionEntityTicking(new BlockPos(x, scarecrow.getY(), z))) {
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (Math.abs(y - scarecrow.getY()) <= 8) {
					return new Vec3(x + 0.5, y, z + 0.5);
				}
			}
		}
		return Vec3.atBottomCenterOf(scarecrow.east(3));
	}
}
