package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The bats roosting in a Bat House, and the guano under it. Bats roost by day and fly out at dusk; at dawn the bats
 * within {@value #RETURN_RANGE} blocks (its own, and wild ones) come in to roost, up to {@value #CAPACITY}, and each
 * leaves a guano (up to {@value #GUANO_CAP} under the house). At dusk, while mobs spawn, a house with room has a
 * {@value #MOVE_IN_CHANCE} chance of a new bat moving in, so a new house fills in a few days without any bats nearby.
 * The house looks at the Overworld clock every {@value #CHECK_TICKS} ticks for dusk and dawn. Saved, and sent to clients
 * for the status message's sake (the guano shows in the block state).
 */
public class BatHouseBlockEntity extends BlockEntity {
	public static final int CAPACITY = 4;
	public static final int GUANO_CAP = 16;
	public static final int RETURN_RANGE = 32;
	public static final float MOVE_IN_CHANCE = 0.5F;
	public static final int CHECK_TICKS = 20;
	/** Bats let out of a Bat House carry this entity tag. */
	public static final String TAG = "jugcraft.bat_house";

	private int residents;
	private int guano;
	private int night = -1;

	public BatHouseBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BAT_HOUSE_ENTITY, pos, state);
	}

	public int residents() {
		return residents;
	}

	public int guano() {
		return guano;
	}

	/** Sets the bats and guano at once (tests and commands). */
	public void set(int bats, int droppings) {
		residents = Math.clamp(bats, 0, CAPACITY);
		guano = Math.clamp(droppings, 0, GUANO_CAP);
		changed();
	}

	/** Takes the guano out; returns how much there was. */
	int takeGuano() {
		int taken = guano;
		guano = 0;
		changed();
		return taken;
	}

	/** Night on the Overworld clock: from dusk to dawn. */
	public static boolean night(long dayTime) {
		long hour = Math.floorMod(dayTime, (long) TrickOrTreat.DAY);
		return hour >= MooncakeItem.NIGHT_START && hour < MooncakeItem.NIGHT_END;
	}

	void serverTick(ServerLevel level) {
		if (level.getGameTime() % CHECK_TICKS != 0) {
			return;
		}
		int now = night(level.getOverworldClockTime()) ? 1 : 0;
		if (night == 0 && now == 1) {
			dusk(level, level.getRandom());
		} else if (night == 1 && now == 0) {
			dawn(level);
		}
		if (night != now) {
			night = now;
			setChanged();
		}
	}

	/** Dusk: perhaps a new bat moves in (while mobs spawn); then every bat flies out. Returns how many flew. */
	public int dusk(ServerLevel level, RandomSource random) {
		if (residents < CAPACITY && level.getGameRules().get(GameRules.SPAWN_MOBS) && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)
				&& random.nextFloat() < MOVE_IN_CHANCE) {
			residents++;
		}
		return release(level);
	}

	/** Lets every roosting bat out in front of the house; returns how many. */
	public int release(ServerLevel level) {
		int out = letOut(level);
		changed();
		return out;
	}

	private int letOut(ServerLevel level) {
		int out = 0;
		Direction front = getBlockState().hasProperty(BatHouseBlock.FACING) ? getBlockState().getValue(BatHouseBlock.FACING) : Direction.NORTH;
		for (int i = 0; i < residents; i++) {
			Entity bat = EntityTypes.BAT.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (bat == null) {
				continue;
			}
			bat.setPos(worldPosition.getX() + 0.5 + front.getStepX() * 0.8, worldPosition.getY() + 0.2 - i * 0.15,
					worldPosition.getZ() + 0.5 + front.getStepZ() * 0.8);
			bat.addTag(TAG);
			level.addFreshEntity(bat);
			out++;
		}
		residents = 0;
		if (out > 0) {
			level.playSound(null, worldPosition, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.8F, 1.0F);
			level.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5 + front.getStepX() * 0.6, worldPosition.getY() + 0.3,
					worldPosition.getZ() + 0.5 + front.getStepZ() * 0.6, 8, 0.2, 0.2, 0.2, 0.01);
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(16))) {
				TrickOrTreat.award(player, "night_shift");
			}
		}
		return out;
	}

	/** Dawn: the nearest bats come in to roost, up to the house's room; each leaves a guano. Returns how many came in. */
	public int dawn(ServerLevel level) {
		List<? extends Entity> bats = level.getEntities(EntityTypes.BAT, new AABB(worldPosition).inflate(RETURN_RANGE), Entity::isAlive);
		List<? extends Entity> nearest = bats.stream().sorted(Comparator.comparingDouble(bat -> bat.distanceToSqr(Vec3.atCenterOf(worldPosition))))
				.limit(CAPACITY - residents).toList();
		for (Entity bat : nearest) {
			bat.discard();
			residents++;
		}
		guano = Math.min(GUANO_CAP, guano + residents);
		changed();
		return nearest.size();
	}

	/** Broken, the house lets its bats out (they aren't lost) and drops its guano. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			letOut(server);
			if (guano > 0) {
				Block.popResource(server, pos, new net.minecraft.world.item.ItemStack(JugcraftAgriculture.item("bat_guano"), guano));
				guano = 0;
			}
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			int shown = BatHouseBlock.guanoLevel(guano);
			if (state.hasProperty(BatHouseBlock.GUANO) && state.getValue(BatHouseBlock.GUANO) != shown && !isRemoved()) {
				level.setBlock(worldPosition, state.setValue(BatHouseBlock.GUANO, shown), Block.UPDATE_ALL);
			} else {
				level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			}
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		residents = Math.clamp(input.getIntOr("residents", 0), 0, CAPACITY);
		guano = Math.clamp(input.getIntOr("guano", 0), 0, GUANO_CAP);
		night = Math.clamp(input.getIntOr("night", -1), -1, 1);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("residents", residents);
		output.putInt("guano", guano);
		output.putInt("night", night);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
