package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Candy Kettle: a copper sugar pot with a candy thermometer clipped to its side, for boiling sugar into candy
 * ({@link CandyKettleBlockEntity}). Set it over a heat source and, before it nears the boil, use it with:
 * <ul>
 * <li>a water bottle (for sugar syrup) or a milk bucket (for cream): its base, one a batch;</li>
 * <li>sugar, up to four (two pieces of candy each);</li>
 * <li>flavours, up to two, and dyes, mixed as on leather.</li>
 * </ul>
 * Then watch the thermometer: each stage it reaches rings a bell, and the stage it is poured at decides the candy
 * ({@link CandyBase#makes}). Use it with an empty Candy Tray to pour (with a tray of candy corn, to pour another layer
 * on); with an empty hand, to read the thermometer; sneaking with an empty hand, to tip the batch out. Taking it off
 * the heat holds it at its stage while it cools. Comparators read the stage it has reached.
 */
public class CandyKettleBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final String MESSAGES = "message.jugcraft.candy_kettle.";
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

	public CandyKettleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CandyKettleBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.CANDY_KETTLE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((CandyKettleBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	/** A water bottle: a potion of plain water. */
	public static boolean waterBottle(ItemStack stack) {
		return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}

	private static boolean handles(ItemStack stack) {
		return waterBottle(stack) || stack.is(Items.MILK_BUCKET) || stack.is(Items.SUGAR) || CandyFlavour.of(stack) != null
				|| ScarecrowBlock.dyeColor(stack) != null || stack.getItem() instanceof CandyTrayItem;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!handles(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CandyKettleBlockEntity kettle)) {
			return InteractionResult.SUCCESS;
		}
		if (stack.getItem() instanceof CandyTrayItem) {
			pour(stack, level, pos, player, hand, kettle);
			return InteractionResult.SUCCESS;
		}
		if (!kettle.cool()) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "too_hot"));
			return InteractionResult.SUCCESS;
		}
		CandyBase base = waterBottle(stack) ? CandyBase.SYRUP : stack.is(Items.MILK_BUCKET) ? CandyBase.CREAM : null;
		DyeColor dye = ScarecrowBlock.dyeColor(stack);
		CandyFlavour flavour = CandyFlavour.of(stack);
		if (base != null) {
			if (kettle.setBase(base)) {
				ItemStack left = new ItemStack(base == CandyBase.SYRUP ? Items.GLASS_BOTTLE : Items.BUCKET);
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, left));
				level.playSound(null, pos, base == CandyBase.SYRUP ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "has_base", kettle.base().displayName()));
			}
		} else if (stack.is(Items.SUGAR)) {
			if (kettle.addSugar()) {
				stir(level, pos, player, stack, SoundEvents.SAND_PLACE);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "full_sugar", CandyKettleBlockEntity.MAX_SUGAR));
			}
		} else if (dye != null) {
			kettle.addDye(dye);
			stir(level, pos, player, stack, SoundEvents.DYE_USE);
		} else if (flavour != null) {
			if (kettle.canFlavour(flavour)) {
				kettle.addFlavour(flavour);
				stir(level, pos, player, stack, SoundEvents.BREWING_STAND_BREW);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + (kettle.flavours().contains(flavour) ? "already" : "two_flavours")));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Pours the batch onto an empty tray, or as another layer onto a tray of candy corn. */
	private static void pour(ItemStack tray, Level level, BlockPos pos, Player player, InteractionHand hand, CandyKettleBlockEntity kettle) {
		CandyKind kind = kettle.makes();
		if (kind == null) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + (kettle.batch() ? "not_ready" : "nothing")));
			return;
		}
		long now = level.getGameTime();
		int pieces = kettle.sugar() * CandyKettleBlockEntity.PIECES_PER_SUGAR;
		CandyBatch held = CandyTrayItem.batch(tray);
		ItemStack filled;
		if (held == null) {
			filled = new ItemStack(tray.getItem());
			CandyTrayItem.fill(filled, new CandyBatch(kind, kettle.flavours(), java.util.List.of(kettle.color()), pieces, now, 0));
			tray.consume(1, player);
			if (tray.isEmpty()) {
				player.setItemInHand(hand, filled);
			} else if (!player.getInventory().add(filled)) {
				Block.popResource(level, pos.above(), filled);
			}
		} else {
			String refusal = held.kind() != CandyKind.CANDY_CORN || kind != CandyKind.CANDY_CORN ? "tray_full"
					: held.colors().size() >= CandyTrayItem.MAX_LAYERS ? "layers_full"
					: held.flavoursWith(kettle.flavours()) > CandyKettleBlockEntity.MAX_FLAVOURS ? "two_flavours" : null;
			if (refusal != null) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + refusal, CandyTrayItem.MAX_LAYERS));
				return;
			}
			CandyTrayItem.fill(tray, held.layer(kettle.flavours(), kettle.color(), pieces, now));
			player.sendOverlayMessage(Component.translatable(MESSAGES + "layered", held.colors().size() + 1, CandyTrayItem.MAX_LAYERS));
		}
		kettle.clear();
		level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		if (player instanceof ServerPlayer server) {
			TrickOrTreat.award(server, "candy_maker");
		}
	}

	/** Stirs one of {@code stack} in, handing back what it leaves (a honey bottle leaves its bottle). */
	private static void stir(Level level, BlockPos pos, Player player, ItemStack stack, SoundEvent sound) {
		ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
		stack.consume(1, player);
		if (remainder != null && !player.getAbilities().instabuild) {
			ItemStack left = remainder.create();
			if (!player.getInventory().add(left)) {
				Block.popResource(level, pos.above(), left);
			}
		}
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
	}

	/** An empty hand reads the thermometer (and what the batch would make now); sneaking, it tips the batch out. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CandyKettleBlockEntity kettle)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive() && !kettle.empty()) {
			kettle.clear();
			level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.2F);
			player.sendOverlayMessage(Component.translatable(MESSAGES + "tipped"));
			return InteractionResult.SUCCESS;
		}
		player.sendOverlayMessage(status(kettle));
		return InteractionResult.SUCCESS;
	}

	/** "Candy Kettle: 117°C, soft ball. Sugar syrup, 3 sugar, Chocolate. Pour now for candy corn." */
	public static Component status(CandyKettleBlockEntity kettle) {
		MutableComponent status = Component.translatable(MESSAGES + "reading", kettle.temperature());
		if (kettle.empty()) {
			return status.append(Component.translatable(MESSAGES + "empty"));
		}
		if (kettle.batch()) {
			status.append(Component.translatable(MESSAGES + "stage", kettle.stage().displayName()));
		}
		status.append(Component.translatable(MESSAGES + "holds", kettle.base() == null ? Component.translatable(MESSAGES + "no_base")
				: kettle.base().displayName(), kettle.sugar()));
		for (CandyFlavour flavour : kettle.flavours()) {
			status.append(", ").append(flavour.displayName());
		}
		CandyKind kind = kettle.makes();
		if (kind != null) {
			status.append(Component.translatable(MESSAGES + "makes", kind.displayName()));
		}
		return status;
	}

	/** Boiling sugar bubbles; caramel steams and burnt sugar smokes (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof CandyKettleBlockEntity kettle) || !kettle.batch()
				|| kettle.temperature() < CandyKettleBlockEntity.BOIL) {
			return;
		}
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		double surface = pos.getY() + (3.0 + kettle.sugar() * 1.25) / 16.0;
		level.addParticle(ParticleTypes.BUBBLE_POP, x, surface, z, 0.0, 0.0, 0.0);
		CandyStage stage = kettle.stage();
		if (stage == CandyStage.BURNT) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, surface + 0.2, z, 0.0, 0.03, 0.0);
		} else if (stage.ordinal() >= CandyStage.CARAMEL.ordinal() || random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.WHITE_SMOKE, x, surface + 0.15, z, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** The stage the batch has reached: 0 for an empty kettle or one still syrup, up to 8 for burnt. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof CandyKettleBlockEntity kettle && kettle.batch() ? kettle.stage().ordinal() : 0;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
