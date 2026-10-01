package io.github.jimbozoomer.jugcraft.tools;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tool that runs on JE instead of wearing out: each block it breaks costs {@link #energyPerBlock}.
 * With too little charge it mines like a bare hand and gets no drops. Subclasses break extra blocks
 * after the first ({@link #afterMining}); those go through the player's normal block breaking, so
 * protections, drops and the energy cost all apply to them too.
 */
public class PoweredToolItem extends Item implements Chargeable {
	/** Set while a tool breaks its extra blocks, so they do not start more. The server ticks on one thread. */
	private static boolean breakingExtra;

	private final long capacity;
	private final long energyPerBlock;

	public PoweredToolItem(Properties properties, long capacity, long energyPerBlock) {
		super(properties);
		this.capacity = capacity;
		this.energyPerBlock = energyPerBlock;
	}

	@Override
	public long baseCapacity() {
		return capacity;
	}

	/** JE per block: doubled, then tripled, by overclock modules. */
	public long energyPerBlock(ItemStack stack) {
		return energyPerBlock * (1 + ToolUpgrades.level(stack, ToolUpgrades.Kind.OVERCLOCK));
	}

	protected boolean charged(ItemStack stack) {
		return Chargeable.energy(stack) >= energyPerBlock(stack);
	}

	/** Each overclock module adds half the base speed again. */
	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (!charged(stack)) {
			return 1.0F;
		}
		float speed = super.getDestroySpeed(stack, state);
		return speed > 1.0F ? speed * (1 + 0.5F * ToolUpgrades.level(stack, ToolUpgrades.Kind.OVERCLOCK)) : speed;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return charged(stack) && super.isCorrectToolForDrops(stack, state);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
		if (level instanceof ServerLevel serverLevel && state.getDestroySpeed(level, pos) != 0.0F) {
			boolean paid = Chargeable.drain(stack, energyPerBlock(stack));
			if (paid && !breakingExtra && miner instanceof ServerPlayer player) {
				breakingExtra = true;
				try {
					afterMining(stack, serverLevel, state, pos, player);
				} finally {
					breakingExtra = false;
				}
			}
		}
		return true;
	}

	/** Called once after the player breaks a block with the tool (charged): break any extra blocks here. */
	protected void afterMining(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, ServerPlayer player) {
	}

	/** Breaks one extra block as the player would, if the tool still has the charge and is right for it. */
	protected boolean breakExtra(ItemStack stack, ServerLevel level, BlockPos pos, ServerPlayer player) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !charged(stack) || !isCorrectToolForDrops(stack, state)) {
			return false;
		}
		return player.gameMode.destroyBlock(pos);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(energyLine(stack));
		ToolUpgrades.appendTooltip(stack, tooltip);
	}

	/** Amber, like the machines' energy bars. */
	static final int BAR_COLOR = 0xFFB8740A;

	static int barWidth(ItemStack stack) {
		return (int) Math.round(13.0 * Chargeable.energy(stack) / Math.max(1, Chargeable.capacity(stack)));
	}

	static Component energyLine(ItemStack stack) {
		return Component.translatable("tooltip.jugcraft.energy", String.format("%,d", Chargeable.energy(stack)),
				String.format("%,d", Chargeable.capacity(stack))).withStyle(ChatFormatting.GOLD);
	}
}
