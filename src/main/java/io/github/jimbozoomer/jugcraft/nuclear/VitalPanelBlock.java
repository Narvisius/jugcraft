package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A 120 VAC vital instrument panel (round 26, 8e; Justin: "Side wall, under the tray", "Switchable breakers", "Door
 * that opens"): each vital inverter's distribution panelboard, hung on a DC room's wall, one block wide and two high,
 * labelled like the inverters (1A-1 and so on). Closed, it shows its nameplate, the POWER ON lamp (its inverter's
 * output alive and the main closed), the door with the circuit directory in its holder. Right-clicking the closed door
 * opens it (the block's {@link #OPEN} state, the door swung out on its hinges); open, right-clicking a breaker (or the
 * name beside it) switches it, anywhere else closes the door. The main breaker at the top, then twelve branch
 * breakers in two columns (odd on the left, even on the right), each carrying its share of the inverter's load
 * ({@link #LOADS}): the inverter's AC AMPS, and its regulating transformer's OUTPUT AMPS on bypass, read the closed
 * ones' sum (client/EdgTelemetry.java). The breakers are kept by its placing block (LivePanelBlockEntity breakers).
 *
 * <p>The layout matches tools/plant_panels.py vital_panel (model pixels on the piece facing north: x across, y up from
 * the floor 0..32, the wall behind it at z 0).
 */
public class VitalPanelBlock extends LivePanelBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	/** The main, then the branch circuits 1 to 12. */
	public static final int BREAKERS = 13;
	/** What each branch circuit draws (amps at 120 V), circuits 1 to 12; the spares nothing. */
	public static final int[] LOADS = {6, 7, 5, 4, 4, 4, 3, 5, 4, 4, 0, 0};
	/** The main and circuits 1 to 10 closed, the spares (11, 12) open. */
	public static final int DEFAULT_BREAKERS = (1 << 11) - 1;
	/** Each circuit's short name (as marked beside its breaker), its channel's numeral or detector put in for %s. */
	static final String[] NAMES = {"NIS %s", "PROT RACK %s", "SSPS CH %s", "SG LVL CH %s", "PZR P/L CH %s", "RCS FLOW %s", "SG PRESS %s",
			"RVLIS CH %s", "PAM CH %s", "AFW FLOW %s", "SPARE", "SPARE"};
	/** By label (1A-1, 1A-2, 1B-1, 1B-2): its channel, and its power range detector. */
	static final String[] CHANNELS = {"I", "III", "II", "IV"}, NIS = {"N41", "N43", "N42", "N44"};
	/** The breakers' rows (their middles, y) and their columns' click areas (x), the main's area: model pixels. */
	static final double ROW0 = 22.7, ROW_PITCH = 2.15, ROW_HALF = 1.05;
	static final double[][] COLUMNS = {{2.9, 8.0}, {8.0, 13.1}};
	static final double[] MAIN = {2.9, 24.3, 13.1, 27.6};
	/** The panel's outline, for its hitbox: x0, y0, x1, y1, its front (z). */
	static final double[] BOX = {2.5, 8.4, 13.5, 31.6, 5.6};

	public VitalPanelBlock(Properties properties) {
		super(properties, 1, 2, 4);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(OPEN, false);
	}

	/** A thin box on the wall: the enclosure (the open door is not in the way). */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean top = state.getValue(partProperty()) == 1;
		double y0 = top ? 0 : BOX[1], y1 = top ? BOX[3] - 16 : 16;
		return turned(new double[] {BOX[0], y0, 0, BOX[2], y1, BOX[4]}, state.getValue(FACING));
	}

	/** Breaker b at this point of the open panel's face (model pixels), or -1: 0 the main, 1 to 12 the circuits. */
	static int breakerAt(double x, double y) {
		if (x >= MAIN[0] && x <= MAIN[2] && y >= MAIN[1] && y <= MAIN[3]) {
			return 0;
		}
		for (int k = 0; k < 12; k++) {
			double[] col = COLUMNS[k % 2];
			double row = ROW0 - (k / 2) * ROW_PITCH;
			if (x >= col[0] && x <= col[1] && Math.abs(y - row) <= ROW_HALF) {
				return k + 1;
			}
		}
		return -1;
	}

	/** Circuit k's (1 to 12) name on the panel with label v. */
	public static String name(int k, int v) {
		String n = NAMES[k - 1];
		return n.contains("%s") ? String.format(n, k == 1 ? NIS[v % 4] : CHANNELS[v % 4]) : n;
	}

	/** What a panel's closed breakers draw (amps): the closed circuits' loads, nothing with the main open. */
	public static int load(int breakers) {
		if ((breakers & 1) == 0) {
			return 0;
		}
		int amps = 0;
		for (int k = 1; k <= 12; k++) {
			if ((breakers & 1 << k) != 0) {
				amps += LOADS[k - 1];
			}
		}
		return amps;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}
		boolean open = state.getValue(OPEN);
		if (!open) {
			setOpen(level, origin, facing, true);
			level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.3F);
			return InteractionResult.SUCCESS_SERVER;
		}
		// Where on the panel's face: the hit turned back into the piece's own frame (facing north), model pixels.
		double[] at = local(facing, origin, hit.getLocation());
		int b = breakerAt(at[0], at[1]);
		if (b < 0) {
			setOpen(level, origin, facing, false);
			level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.3F);
			return InteractionResult.SUCCESS_SERVER;
		}
		panel.breakers ^= 1 << b;
		boolean closed = (panel.breakers & 1 << b) != 0;
		panel.changedBreakers((ServerLevel) level);
		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, closed ? 1.6F : 1.3F);
		Component state1 = Component.translatable(closed ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open");
		int v = LivePanelBlockEntity.variant(state);
		player.sendOverlayMessage(b == 0 ? Component.translatable("message.jugcraft.nuclear.vital_main", state1)
				: Component.translatable("message.jugcraft.nuclear.vital_breaker", b, name(b, v), state1));
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Opens or closes the door: every part of the piece takes the state. */
	private void setOpen(Level level, BlockPos origin, Direction facing, boolean open) {
		for (int p = 0; p < width * rows; p++) {
			BlockPos at = origin.offset(offset(facing, p));
			BlockState there = level.getBlockState(at);
			if (there.is(this)) {
				level.setBlock(at, there.setValue(OPEN, open), Block.UPDATE_ALL);
			}
		}
	}
}
