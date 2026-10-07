package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A bigger Specimen Jar (Halloween decorations batch 17, the bigger jars): the Specimen Jar made {@link #scale()} times
 * bigger, its glowing green fluid, glass, iron fittings and specimen and all, several blocks placed and broken as one
 * ({@link MultiDecorationBlock}); each block's shape is its piece of the jar's box made as much bigger. It holds one of
 * the jar's specimens ({@link #SPECIMEN}, on every block). Sneak-use any block of it to put in the next; broken, it keeps
 * its specimen. The specimen bobs and turns slowly among rising bubbles as in the Specimen Jar, drawn by the client from
 * the block entity on its first block.
 */
public abstract class SpecimenVesselBlock extends MultiDecorationBlock implements EntityBlock {
	public static final EnumProperty<SpecimenJarBlock.Specimen> SPECIMEN = SpecimenJarBlock.SPECIMEN;
	private final int scale;
	private final Map<Direction, VoxelShape[]> shapes;

	/** {@code scale}: how many times bigger than the Specimen Jar it is; {@code cells}: its blocks, as {@link #cells} gives them. */
	protected SpecimenVesselBlock(Properties properties, int scale, int[][] cells) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(SPECIMEN, SpecimenJarBlock.Specimen.EYE));
		this.scale = scale;
		int across = 1;
		int deep = 1;
		for (int[] cell : cells) {
			across = Math.max(across, cell[0] + 1);
			deep = Math.max(deep, cell[2] + 1);
		}
		shapes = partShapes(grown(SpecimenJarBlock.BOX, scale, across, deep), cells);
	}

	/** How many times bigger than the Specimen Jar it is. */
	public int scale() {
		return scale;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[part(state)];
	}

	/** Its glass and fluid let the light through. */
	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	/** Sneak-use any block of it to put in the next specimen, on every block. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			BlockPos master = masterPos(pos, state);
			Direction facing = state.getValue(FACING);
			BlockState first = level.getBlockState(master);
			if (!first.is(this) || part(first) != MASTER) {
				return InteractionResult.PASS;
			}
			SpecimenJarBlock.Specimen next = first.getValue(SPECIMEN).next();
			for (int part = 0; part < cells().length; part++) {
				BlockPos at = partPos(master, facing, part);
				BlockState there = level.getBlockState(at);
				if (there.is(this) && part(there) == part) {
					level.setBlock(at, there.setValue(SPECIMEN, next), Block.UPDATE_ALL);
				}
			}
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 0.6F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, master);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.specimen_jar." + next.getSerializedName()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == MASTER ? new DecorationBlockEntity(JugcraftAgriculture.SPECIMEN_VESSEL_ENTITY, pos, state) : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SPECIMEN);
	}
}
