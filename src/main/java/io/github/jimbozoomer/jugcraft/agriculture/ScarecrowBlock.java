package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Scarecrow: a straw figure on a post, two blocks tall, in a flannel shirt. Use any dye on it to change the
 * shirt's colour ({@link #SHIRT}). Give it a pumpkin of any kind (the {@code jugcraft:scarecrow_heads} item tag) and
 * it wears it for a head, on its shoulders the way an armor stand wears a pumpkin, carving and all
 * ({@link ScarecrowBlockEntity}, on the upper half); an empty hand takes the head back, and a torch or soul torch
 * lights a hand-carved one. A lit head lights the scarecrow ({@link #LIGHT}). Wearing a lit head, sneak-used at midnight during the
 * Halloween event, it summons the Headless Horseman ({@link HorsemanSummoning}). It keeps crows ({@link Crow}) off the
 * crops round it, the farther the better it is dressed ({@link Scarecrows}).
 */
public class ScarecrowBlock extends TallDecorationBlock implements EntityBlock {
	public static final EnumProperty<DyeColor> SHIRT = EnumProperty.create("shirt", DyeColor.class);
	public static final DyeColor DEFAULT_SHIRT = DyeColor.RED;
	/** The light the head gives (kept on both halves; only the upper half, where the head is, shines). */
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

	public ScarecrowBlock(Properties properties) {
		super(properties, Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0), Block.box(2.0, 0.0, 5.0, 14.0, 16.0, 11.0));
		registerDefaultState(defaultBlockState().setValue(SHIRT, DEFAULT_SHIRT).setValue(LIGHT, 0));
	}

	/** The light a scarecrow block gives: its head's, from the upper half. */
	public static int light(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? state.getValue(LIGHT) : 0;
	}

	/** Sets the light of the scarecrow whose upper half is at {@code upper} (both halves). */
	static void setLight(Level level, BlockPos upper, BlockState state, int light) {
		setBoth(level, upper, state.setValue(LIGHT, light));
	}

	/** The colour of a vanilla dye, or null if the stack is not one. */
	public static @Nullable DyeColor dyeColor(ItemStack stack) {
		for (DyeColor color : DyeColor.values()) {
			if (stack.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", color.getSerializedName() + "_dye")))) {
				return color;
			}
		}
		return null;
	}

	/** The upper half of the scarecrow with a half at {@code pos}, which wears the head. */
	public static BlockPos upper(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos;
	}

	/** What the scarecrow with a half at {@code pos} wears for a head (empty if nothing). */
	public static ItemStack head(Level level, BlockPos pos, BlockState state) {
		return level.getBlockEntity(upper(pos, state)) instanceof ScarecrowBlockEntity scarecrow ? scarecrow.head() : ItemStack.EMPTY;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? new ScarecrowBlockEntity(pos, state) : null;
	}

	/**
	 * A dye recolours the shirt (both halves), using up one dye; a pumpkin goes on as the head (giving back the one it
	 * wore); a torch lights a hand-carved head that has holes for the light to come out of.
	 */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		DyeColor color = dyeColor(stack);
		boolean head = stack.is(JugcraftAgriculture.SCARECROW_HEADS);
		boolean torch = CarvedPumpkinBlock.isTorch(stack) && canLight(head(level, pos, state));
		if ((color == null || state.getValue(SHIRT) == color) && !head && !torch) {
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!player.mayBuild() || !level.mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		if (!(level.getBlockEntity(upper(pos, state)) instanceof ScarecrowBlockEntity scarecrow)) {
			return InteractionResult.PASS;
		}
		if (head) {
			ItemStack old = scarecrow.setHead(stack);
			stack.consume(1, player);
			give(player, old, level, pos);
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
		} else if (torch) {
			ItemStack lit = scarecrow.head().copy();
			lit.set(DataComponents.BLOCK_STATE, lit.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
					.with(CarvedPumpkinBlock.LIT, true).with(CarvedPumpkinBlock.SOUL, stack.is(Items.SOUL_TORCH)));
			scarecrow.setHead(lit);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
		} else {
			setBoth(level, pos, state.setValue(SHIRT, color));
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/**
	 * An empty hand takes the head off. Sneak-used with an empty hand at midnight during the Halloween event, wearing a
	 * lit head, the scarecrow summons the Headless Horseman ({@link HorsemanSummoning}).
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			if (player instanceof ServerPlayer caller) {
				BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
				HorsemanSummoning.Result result = HorsemanSummoning.summon(caller, lower);
				if (result != HorsemanSummoning.Result.SUMMONED) {
					caller.sendOverlayMessage(Component.translatable("message.jugcraft.horseman." + result.name().toLowerCase(Locale.ROOT)));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (head(level, pos, state).isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			if (!player.mayBuild() || !level.mayInteract(player, pos)
					|| !(level.getBlockEntity(upper(pos, state)) instanceof ScarecrowBlockEntity scarecrow)) {
				return InteractionResult.FAIL;
			}
			give(player, scarecrow.setHead(ItemStack.EMPTY), level, pos);
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Whether a torch would light {@code head}: an unlit hand-carved pumpkin with something carved out. */
	private static boolean canLight(ItemStack head) {
		return head.getItem() instanceof BlockItem item && item.getBlock() instanceof CarvedPumpkinBlock && !ScarecrowBlockEntity.lit(head)
				&& head.getOrDefault(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK).glow() > 0;
	}

	/** Gives {@code stack} to the player, or drops it at the scarecrow if their inventory is full. */
	private static void give(Player player, ItemStack stack, Level level, BlockPos pos) {
		if (!stack.isEmpty() && !player.getInventory().add(stack)) {
			Block.popResource(level, pos, stack);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SHIRT, LIGHT);
	}
}
