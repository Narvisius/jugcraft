package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Bowling Scoreboard's game: the pins its lane had when the game began, how many each roll knocked down, and how many
 * were standing before the roll now coming. A roll counts the Skeleton Pins within
 * {@link BowlingScoreboardBlock#LANE_REACH} blocks (a bounded look, once a roll); a finished game starts again with the
 * next roll. Saved, and sent to clients for the board to show.
 */
public class BowlingScoreboardBlockEntity extends BlockEntity {
	private final List<Integer> rolls = new ArrayList<>();
	private int pins;
	private int standingBefore;

	public BowlingScoreboardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BOWLING_SCOREBOARD_ENTITY, pos, state);
	}

	public List<Integer> rolls() {
		return List.copyOf(rolls);
	}

	/** How many pins the lane had when this game began (0 before the first roll). */
	public int pins() {
		return pins;
	}

	/** {standing, total} Skeleton Pins within reach. */
	public int[] countPins(ServerLevel level) {
		int standing = 0;
		int total = 0;
		int reach = BowlingScoreboardBlock.LANE_REACH;
		for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-reach, -1, -reach), worldPosition.offset(reach, 1, reach))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof SkeletonPinBlock) {
				total++;
				standing += state.getValue(SkeletonPinBlock.DOWN) ? 0 : 1;
			}
		}
		return new int[] {standing, total};
	}

	/** Scores a roll that has just come to rest: how many pins went down since the last. */
	public void rolled(ServerLevel level) {
		int[] count = countPins(level);
		if (count[1] == 0) {
			return;
		}
		if (rolls.isEmpty() || BowlingScore.over(rolls, pins)) {
			rolls.clear();
			pins = count[1];
			standingBefore = pins;
		}
		int knocked = Math.max(0, standingBefore - count[0]);
		rolls.add(knocked);
		standingBefore = count[0];
		if (knocked >= pins || count[0] == 0 && knocked > 0) {
			level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);
		}
		if (BowlingScore.standPins(rolls, pins, count[0])) {
			level.scheduleTick(worldPosition, getBlockState().getBlock(), BowlingScoreboardBlock.RESET_TICKS);
		}
		changed();
	}

	/** Stands up every pin within reach. */
	public void standPins(ServerLevel level) {
		int reach = BowlingScoreboardBlock.LANE_REACH;
		for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-reach, -1, -reach), worldPosition.offset(reach, 1, reach))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof SkeletonPinBlock && state.getValue(SkeletonPinBlock.DOWN)) {
				level.setBlock(pos, state.setValue(SkeletonPinBlock.DOWN, false), Block.UPDATE_ALL);
			}
		}
		standingBefore = countPins(level)[0];
		changed();
	}

	/** Wipes the score and stands the pins up. */
	public void newGame(ServerLevel level) {
		rolls.clear();
		pins = 0;
		standPins(level);
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		rolls.clear();
		rolls.addAll(input.read("rolls", Codec.INT.listOf()).orElse(List.of()));
		pins = input.getIntOr("pins", 0);
		standingBefore = input.getIntOr("standing", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("rolls", Codec.INT.listOf(), List.copyOf(rolls));
		output.putInt("pins", pins);
		output.putInt("standing", standingBefore);
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
