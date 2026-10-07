package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * The scuba tank (batch 27, docs/features/gear-and-plastic.md), worn in the chest slot with the scuba mask on. It holds
 * {@link #CAPACITY} mB of oxygen, filled by using it on anything that holds oxygen (a tank, a gas holder, a machine's
 * output). Under water it keeps the wearer's air full for {@link #OXYGEN_PER_TICK} mB a tick: 8,000 mB is 400 seconds.
 */
public class ScubaTankItem extends Item {
	public static final int CAPACITY = 8_000;
	public static final int OXYGEN_PER_TICK = 1;
	/** Fabric counts fluids in droplets: 81,000 to the bucket, 81 to the millibucket. */
	private static final long DROPLETS_PER_MB = 81;
	/** Light blue, like oxygen's gauge. */
	private static final int BAR_COLOR = 0xFF78B4E0;

	public ScubaTankItem(Properties properties) {
		super(properties);
	}

	public static int oxygen(ItemStack stack) {
		return stack.getOrDefault(JugcraftGear.OXYGEN, 0);
	}

	public static void setOxygen(ItemStack stack, int amount) {
		stack.set(JugcraftGear.OXYGEN, Math.max(0, Math.min(CAPACITY, amount)));
	}

	/** Fills from the oxygen in whatever was clicked, in whole millibuckets. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		int room = CAPACITY - oxygen(stack);
		if (room <= 0) {
			return InteractionResult.PASS;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, context.getClickedPos(), context.getClickedFace());
		if (storage == null) {
			return InteractionResult.PASS;
		}
		FluidVariant oxygen = FluidVariant.of(PetroFluids.OXYGEN.fluid());
		long millibuckets;
		try (Transaction simulation = Transaction.openOuter()) {
			millibuckets = storage.extract(oxygen, room * DROPLETS_PER_MB, simulation) / DROPLETS_PER_MB;
		}
		if (millibuckets <= 0) {
			return InteractionResult.PASS;
		}
		// Take only whole millibuckets, so no droplets are lost in the rounding.
		try (Transaction transaction = Transaction.openOuter()) {
			if (storage.extract(oxygen, millibuckets * DROPLETS_PER_MB, transaction) != millibuckets * DROPLETS_PER_MB) {
				return InteractionResult.PASS;
			}
			transaction.commit();
		}
		setOxygen(stack, oxygen(stack) + (int) millibuckets);
		level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6F, 1.4F);
		return InteractionResult.SUCCESS;
	}

	/** Worn with the mask, eyes under water and short of air: top the air up from the tank. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (slot != EquipmentSlot.CHEST || !(entity instanceof LivingEntity wearer)
				|| !wearer.getItemBySlot(EquipmentSlot.HEAD).is(JugcraftGear.SCUBA_MASK)
				|| !wearer.isEyeInFluid(FluidTags.WATER) || wearer.getAirSupply() >= wearer.getMaxAirSupply()) {
			return;
		}
		int stored = oxygen(stack);
		if (stored >= OXYGEN_PER_TICK) {
			setOxygen(stack, stored - OXYGEN_PER_TICK);
			wearer.setAirSupply(wearer.getMaxAirSupply());
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * oxygen(stack) / CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.oxygen", String.format("%,d", oxygen(stack)),
				String.format("%,d", CAPACITY)).withStyle(ChatFormatting.AQUA));
		tooltip.accept(Component.translatable("tooltip.jugcraft.scuba_tank").withStyle(ChatFormatting.GRAY));
	}
}
