package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cider in a Cider Barrel: up to {@value #CAPACITY} servings of one batch and the game time it started ageing. Fresh
 * juice (Sweet Cider) ferments into Sparkling Cider after {@value #SPARKLING_TICKS} ticks (a Minecraft day) and matures
 * into Aged Cider after {@value #AGED_TICKS} (three days). Topping up a batch that is still sweet starts its clock again;
 * a batch that has begun to ferment takes no fresh juice. Ageing is counted from the start time, so it needs no ticking;
 * the barrel only looks every {@value #CHECK_TICKS} ticks to change its chalk mark ({@link CiderBarrelBlock#CIDER}).
 */
public class CiderBarrelBlockEntity extends BlockEntity {
	public static final int CAPACITY = 16;
	public static final int SPARKLING_TICKS = 24000;
	public static final int AGED_TICKS = 72000;
	static final int CHECK_TICKS = 20;
	/** The drink a serving is at each stage. */
	public static final List<String> STAGES = List.of("sweet_cider", "sparkling_cider", "aged_cider");

	private int servings;
	private long started;

	public CiderBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CIDER_BARREL_ENTITY, pos, state);
	}

	public int servings() {
		return servings;
	}

	public long started() {
		return started;
	}

	/** Sets the batch's start time (game tests age a barrel this way). */
	public void setStarted(long started) {
		this.started = started;
		changed();
	}

	/** The batch's stage at {@code now}: 0 sweet, 1 sparkling, 2 aged; -1 when empty. */
	public int stage(long now) {
		return servings == 0 ? -1 : stageAt(now - started);
	}

	public static int stageAt(long age) {
		return age >= AGED_TICKS ? 2 : age >= SPARKLING_TICKS ? 1 : 0;
	}

	/** Ticks until the batch reaches its next stage at {@code now}, or 0 when aged (or empty). */
	public long untilNext(long now) {
		int stage = stage(now);
		return stage < 0 || stage == 2 ? 0 : (stage == 0 ? SPARKLING_TICKS : AGED_TICKS) - (now - started);
	}

	/** Whether a serving of fresh juice can go in at {@code now}: room, and a batch still sweet (or none). */
	public boolean canFill(long now) {
		return servings < CAPACITY && stage(now) <= 0;
	}

	/** Pours in a serving of fresh juice; the batch's clock starts again. */
	public void fill(long now) {
		servings++;
		started = now;
		changed();
	}

	/** Draws a serving off; returns the drink it is (an item ID under jugcraft). */
	public String draw(long now) {
		String drink = STAGES.get(stage(now));
		servings--;
		if (servings == 0) {
			started = 0;
		}
		changed();
		return drink;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.getGameTime() % CHECK_TICKS == 0) {
			int mark = stage(level.getGameTime()) + 1;
			if (state.getValue(CiderBarrelBlock.CIDER) != mark) {
				level.setBlock(pos, state.setValue(CiderBarrelBlock.CIDER, mark), Block.UPDATE_CLIENTS);
			}
		}
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = getBlockState();
			int mark = stage(level.getGameTime()) + 1;
			if (state.hasProperty(CiderBarrelBlock.CIDER) && state.getValue(CiderBarrelBlock.CIDER) != mark) {
				level.setBlock(worldPosition, state.setValue(CiderBarrelBlock.CIDER, mark), Block.UPDATE_CLIENTS);
			}
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		BarrelCider cider = components.get(JugcraftAgriculture.BARREL_CIDER);
		if (cider != null) {
			servings = cider.servings();
			started = cider.started();
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (servings > 0) {
			components.set(JugcraftAgriculture.BARREL_CIDER, new BarrelCider(servings, started));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		servings = Math.clamp(input.getIntOr("servings", 0), 0, CAPACITY);
		started = input.getLongOr("started", 0L);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("servings", servings);
		output.putLong("started", started);
	}
}
