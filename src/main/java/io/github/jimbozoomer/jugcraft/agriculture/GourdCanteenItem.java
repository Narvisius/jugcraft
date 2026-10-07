package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Gourd Canteen: a dried bottle gourd that holds {@link #CAPACITY} sips of water
 * ({@link JugcraftAgriculture#CANTEEN_WATER}). Use it on a water source to fill it. Each sip, used on a
 * block, does one thing: waters a giant pumpkin (it grows a point faster per tick for a day), moistens
 * farmland, puts out fire, or adds a level to a cauldron (as a water bottle does). Water is free in the
 * world already, so the canteen saves walking, nothing more. Every change is made on the server, where the
 * player must be allowed to build.
 */
public class GourdCanteenItem extends Item {
	public static final int CAPACITY = 3;
	private static final int WATER_BLUE = 0x3F76E4;

	public GourdCanteenItem(Properties properties) {
		super(properties);
	}

	public static int water(ItemStack stack) {
		return Math.clamp(stack.getOrDefault(JugcraftAgriculture.CANTEEN_WATER, 0), 0, CAPACITY);
	}

	/** Fills the canteen from the water source the player looks at. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (water(stack) >= CAPACITY || hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.mayInteract(player, pos)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			stack.set(JugcraftAgriculture.CANTEEN_WATER, CAPACITY);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 0.9F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResult.SUCCESS;
	}

	/** Pours one sip on the block used on, if it is something water does anything to. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (water(stack) <= 0) {
			return InteractionResult.PASS;
		}
		BlockState state = level.getBlockState(pos);
		BlockPos firePos = pos.relative(context.getClickedFace());
		Pour pour;
		if (state.getBlock() instanceof GiantPumpkinBlock) {
			pour = Pour.PUMPKIN;
		} else if (state.is(Blocks.FARMLAND) && state.getValue(BlockStateProperties.MOISTURE) < 7) {
			pour = Pour.FARMLAND;
		} else if (state.getBlock() instanceof BaseFireBlock) {
			pour = Pour.FIRE;
			firePos = pos;
		} else if (level.getBlockState(firePos).getBlock() instanceof BaseFireBlock) {
			pour = Pour.FIRE;
		} else if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3) {
			pour = Pour.CAULDRON;
		} else {
			return InteractionResult.PASS;
		}
		if (player != null && (!player.mayUseItemAt(pos, context.getClickedFace(), stack) || !level.mayInteract(player, pos))) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		SoundEvent sound = SoundEvents.BOTTLE_EMPTY;
		switch (pour) {
			case PUMPKIN -> {
				GiantPumpkinBlockEntity master = GiantPumpkinBlock.master(level, pos, state);
				if (master == null) {
					return InteractionResult.PASS;
				}
				master.water(level);
			}
			case FARMLAND -> level.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, 7), Block.UPDATE_CLIENTS);
			case FIRE -> {
				level.removeBlock(firePos, false);
				sound = SoundEvents.FIRE_EXTINGUISH;
			}
			case CAULDRON -> level.setBlockAndUpdate(pos, state.is(Blocks.CAULDRON) ? Blocks.WATER_CAULDRON.defaultBlockState()
					: state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1));
		}
		stack.set(JugcraftAgriculture.CANTEEN_WATER, water(stack) - 1);
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
		if (player != null) {
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResult.SUCCESS;
	}

	private enum Pour {
		PUMPKIN, FARMLAND, FIRE, CAULDRON
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * water(stack) / CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return WATER_BLUE;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.gourd_canteen.water", water(stack), CAPACITY).withStyle(ChatFormatting.BLUE));
	}
}
