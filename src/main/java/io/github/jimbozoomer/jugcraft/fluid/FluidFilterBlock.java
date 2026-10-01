package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A fluid filter: a steel pipe segment with a strainer housing. Fluid passes along it like any pipe, but the tanks and
 * machines it touches only receive its chosen fluid (and nothing until one is chosen). Use a filled bucket on it to
 * choose that fluid; right-click it with an empty hand to copy the fluid from a tank or machine beside it (the way to
 * choose a gas, which has no bucket); sneak and right-click to clear it. Its lamp lights while a fluid is set.
 */
public class FluidFilterBlock extends FluidPipeBlock implements EntityBlock {
	public static final BooleanProperty SET = BooleanProperty.create("set");

	public FluidFilterBlock(Properties properties, long rateMb) {
		super(properties, rateMb);
		registerDefaultState(defaultBlockState().setValue(SET, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SET);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidFilterBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		Storage<FluidVariant> held = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
		FluidVariant fluid = held == null ? null : StorageUtil.findStoredResource(held);
		if (fluid == null || fluid.isBlank()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			set(level, pos, state, fluid, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (player.isShiftKeyDown()) {
				set(level, pos, state, FluidVariant.blank(), player);
			} else {
				FluidVariant beside = fluidBeside(level, pos);
				if (beside != null) {
					set(level, pos, state, beside, player);
				} else {
					player.sendOverlayMessage(describe(filter(level, pos)));
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** The fluid in the first tank or machine beside the filter that holds one (pipes are skipped), or null. */
	private static @Nullable FluidVariant fluidBeside(Level level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = pos.relative(direction);
			if (level.getBlockState(neighbor).getBlock() instanceof FluidPipeBlock) {
				continue;
			}
			Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, neighbor, direction.getOpposite());
			FluidVariant stored = storage == null ? null : StorageUtil.findStoredResource(storage);
			if (stored != null && !stored.isBlank()) {
				return stored;
			}
		}
		return null;
	}

	private static void set(Level level, BlockPos pos, BlockState state, FluidVariant fluid, Player player) {
		if (level.getBlockEntity(pos) instanceof FluidFilterBlockEntity filter) {
			filter.setFilter(fluid);
			level.setBlock(pos, state.setValue(SET, !fluid.isBlank()), Block.UPDATE_ALL);
			player.sendOverlayMessage(describe(fluid));
		}
	}

	/** The fluid this filter lets out, or blank (nothing) when it is not set or there is no filter at {@code pos}. */
	public static FluidVariant filter(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof FluidFilterBlockEntity filter ? filter.filter() : FluidVariant.blank();
	}

	private static Component describe(FluidVariant fluid) {
		return fluid.isBlank()
				? Component.translatable("message.jugcraft.fluid_filter.none")
				: Component.translatable("message.jugcraft.fluid_filter", FluidVariantAttributes.getName(fluid));
	}
}
