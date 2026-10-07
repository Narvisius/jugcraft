package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What is in a Cider Press: whole apples in its hopper, ground pulp in its basket (the "cheese"), how far its screw is
 * down on the cheese being pressed, and the juice in its trough. The crank grinds one apple at a time into pulp; the
 * screw presses a cheese in {@value #TURNS} turns, each letting out its share of the juice (one serving an apple); the
 * last turn knocks out the spent pomace (one for every {@value #APPLES_PER_POMACE} apples, rounded up). Work is paced:
 * the crank and the screw each move once every {@value #WORK_TICKS} ticks, however many players work them. Saved, and
 * sent to clients for the renderer.
 */
public class CiderPressBlockEntity extends BlockEntity {
	/** Apples and pulp together, and the juice the trough holds, in apples (servings). */
	public static final int CAPACITY = 8;
	public static final int TROUGH = 8;
	public static final int TURNS = 4;
	public static final int WORK_TICKS = 8;
	public static final int APPLES_PER_POMACE = 2;

	private int apples;
	private int pulp;
	private int pressing;
	private int turns;
	private int juice;
	/** When the crank or screw last moved (game time); never, at first. */
	private long lastWork = -WORK_TICKS;

	public CiderPressBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CIDER_PRESS_ENTITY, pos, state);
	}

	public int apples() {
		return apples;
	}

	public int pulp() {
		return pulp;
	}

	public int turns() {
		return turns;
	}

	public int juice() {
		return juice;
	}

	/** Whether the screw is down on a cheese: nothing goes in until it is pressed out. */
	public boolean pressing() {
		return turns > 0;
	}

	/** Whether another apple fits in the hopper. */
	boolean hasRoom() {
		return !pressing() && apples + pulp < CAPACITY;
	}

	public void addApple() {
		apples++;
		changed();
	}

	/** Whether the crank or screw may move again at {@code gameTime} (the work is paced, not the player). */
	boolean ready(long gameTime) {
		return gameTime - lastWork >= WORK_TICKS;
	}

	/** One turn of the crank: an apple from the hopper is ground into the basket. */
	public void grind(long gameTime) {
		apples--;
		pulp++;
		lastWork = gameTime;
		changed();
	}

	/** The juice the next turn of the screw lets out. */
	public int nextRelease() {
		int size = pressing() ? pressing : pulp;
		return size * (turns + 1) / TURNS - size * turns / TURNS;
	}

	/**
	 * One turn of the screw: its share of the cheese's juice runs into the trough. Returns the pomace knocked out when that
	 * was the last turn, else 0.
	 */
	public int turn(long gameTime) {
		if (!pressing()) {
			pressing = pulp;
		}
		juice += nextRelease();
		turns++;
		lastWork = gameTime;
		int pomace = 0;
		if (turns == TURNS) {
			pomace = (pressing + APPLES_PER_POMACE - 1) / APPLES_PER_POMACE;
			pulp = 0;
			pressing = 0;
			turns = 0;
		}
		changed();
		return pomace;
	}

	/** Draws a serving of juice into a bottle. */
	public void draw() {
		juice--;
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		apples = Math.clamp(input.getIntOr("apples", 0), 0, CAPACITY);
		pulp = Math.clamp(input.getIntOr("pulp", 0), 0, CAPACITY - apples);
		turns = Math.clamp(input.getIntOr("turns", 0), 0, TURNS - 1);
		pressing = turns == 0 ? 0 : Math.clamp(input.getIntOr("pressing", 0), 0, CAPACITY);
		juice = Math.clamp(input.getIntOr("juice", 0), 0, TROUGH);
		lastWork = -WORK_TICKS;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("apples", apples);
		output.putInt("pulp", pulp);
		output.putInt("turns", turns);
		output.putInt("pressing", pressing);
		output.putInt("juice", juice);
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
