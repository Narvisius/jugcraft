package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A storage tank plate: the shell and roof of a storage tank MBS ({@link StorageTankPortBlockEntity}). Loose plates
 * and an unfinished tank show grey primer; when the tank's outlet forms the tank it paints every plate by where it
 * is: the white wall, the checker band round the top course, the dark rim at the roof's edge, the roof, in the colours
 * of the tank's rating (TankRating: a flammable tank's red product band, a corrosive tank's darker shell and yellow band,
 * a cryogenic tank's plain jacket).
 */
public class StorageTankPlateBlock extends Block {
	public enum Band implements StringRepresentable {
		WALL("wall"), BAND("band"), RIM("rim"), ROOF("roof"),
		/** The course under the band: a flammable tank's red product band, a corrosive tank's yellow one (wall otherwise). */
		STRIPE("stripe");

		private final String name;

		Band(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	public static final EnumProperty<Band> BAND = EnumProperty.create("band", Band.class);
	/** The formed tank's rating (its outlet's): the paint (TankRating). */
	public static final EnumProperty<TankRating> RATING = TankRating.PROPERTY;

	public StorageTankPlateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(BAND, Band.WALL).setValue(RATING, TankRating.GENERAL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, BAND, RATING);
	}

	/** A name tag, a flame arrester or a rubber lining used on the shell (TankUpgrades). */
	@Override
	protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		InteractionResult upgraded = state.getValue(FORMED) ? TankUpgrades.use(stack, level, pos, player) : null;
		return upgraded != null ? upgraded : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	/** A formed tank's shell, right-clicked: the tank's screen (what it holds, how fast it fills and empties). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(FORMED)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			BlockPos outlet = StorageTanks.outletOf(level, pos);
			if (outlet != null) {
				StorageTanks.openScreen.accept(outlet);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
