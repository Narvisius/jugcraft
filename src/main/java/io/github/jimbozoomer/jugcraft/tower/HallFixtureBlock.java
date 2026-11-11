package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Tri-Pillar control building's live hall fittings (tools/tripillar_hall.py; drawn by client/HallFixtureRenderer):
 * the touch consoles round the holo table, the info totem, the floor emitters of the floating holo panels, the drone
 * display stands and the service bay's robot arm. Each shows its tower's depot: the core pins it to the terminal when it
 * builds the foundation (one placed by a player links to the nearest terminal). No server ticking. A touch console opens
 * the depot's screen like a console desk.
 */
public class HallFixtureBlock extends FurnitureBlock implements EntityBlock, DepotDisplayBlockEntity.Display {
	/** How far a fitting placed by a player looks for its terminal (the hall to the supply yard is about 60 blocks). */
	public static final int LINK_RANGE = 72;
	private final boolean interactive;

	public HallFixtureBlock(Properties properties, boolean interactive, double[]... boxes) {
		super(properties, boxes);
		this.interactive = interactive;
	}

	/** True for the touch consoles: using one opens the depot's screen. */
	public boolean interactive() {
		return interactive;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.HALL_ENTITY, pos, state);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return true;
	}

	@Override
	public int linkRange() {
		return LINK_RANGE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var nearest = DroneDepots.nearest(level, pos, linkRange());
			if (nearest != null) {
				display.pin(nearest.getBlockPos());
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!interactive) {
			return super.useWithoutItem(state, level, pos, player, hit);
		}
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && display.terminal() != null
				&& level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) {
				ConsoleDeskBlock.openScreen.accept(display.terminal(), pos);
			} else {
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow();
				terminal.refreshClients();
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** What a holo emitter's floating panel shows. */
	public enum Panel implements StringRepresentable {
		FLEET, TOWER, SUPPLY;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** How a drone stand shows its drone: under glass on a display plinth, or up on the service bay's lifted cradle. */
	public enum Style implements StringRepresentable {
		DISPLAY, SERVICE;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The floor emitter of a floating holo panel (FLEET, TOWER or SUPPLY) over an operator area. */
	public static class Emitter extends HallFixtureBlock {
		public static final EnumProperty<Panel> PANEL = EnumProperty.create("panel", Panel.class);

		public Emitter(Properties properties, double[]... boxes) {
			super(properties, false, boxes);
			registerDefaultState(defaultBlockState().setValue(PANEL, Panel.FLEET));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(PANEL);
		}
	}

	/**
	 * A drone shown standing: on a display plinth under glass, or on the service bay's lifted cradle. The cradle
	 * (client/HallFixtureRenderer.stand: 2.2 blocks across, 2.5 long, its deck 1.15 up) collides where it is drawn,
	 * out over the cells round the stand (Justin could run through it).
	 */
	public static class Stand extends HallFixtureBlock {
		public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 9);
		public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
		private static final java.util.Map<Direction, VoxelShape> CRADLE = new java.util.EnumMap<>(Direction.class);

		static {
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				CRADLE.put(facing, FurnitureBlock.turned(new double[] {8 - 17.6, 0, 8 - 20, 8 + 17.6, 18.4, 8 + 20}, facing));
			}
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return state.getValue(STYLE) == Style.SERVICE ? CRADLE.get(state.getValue(FACING)) : super.getCollisionShape(state, level, pos, context);
		}

		public Stand(Properties properties, double[]... boxes) {
			super(properties, false, boxes);
			registerDefaultState(defaultBlockState().setValue(TIER, 1).setValue(STYLE, Style.DISPLAY));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(TIER, STYLE);
		}
	}
}
