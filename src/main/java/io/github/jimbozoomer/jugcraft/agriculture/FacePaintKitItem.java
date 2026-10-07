package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A Face Paint Kit: a tin palette of greasepaints and a brush, good for {@value #USES} faces. Its dial is set to one of
 * the {@link FacePaint.Design designs} (kept on the kit):
 * <ul>
 * <li>sneak and use it to turn the dial to the next design;</li>
 * <li>use it on another player to paint their face at once;</li>
 * <li>use it and hold to paint your own face, which takes {@value #USE_TICKS} ticks (a mirror would help).</li>
 * </ul>
 * Each face painted wears the kit by one use. Painting over a painted face replaces the design.
 */
public class FacePaintKitItem extends Item {
	public static final int USES = 16;
	public static final int USE_TICKS = 32;

	public FacePaintKitItem(Properties properties) {
		super(properties);
	}

	/** The design the kit's dial is set to. */
	public static FacePaint.Design design(ItemStack kit) {
		return kit.getOrDefault(JugcraftAgriculture.FACE_PAINT_DESIGN, FacePaint.Design.SKULL);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack kit = player.getItemInHand(hand);
		if (player.isSecondaryUseActive()) {
			FacePaint.Design next = design(kit).next();
			kit.set(JugcraftAgriculture.FACE_PAINT_DESIGN, next);
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.face_paint.design", next.displayName()));
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 0.3F, 1.7F);
			}
			return InteractionResult.SUCCESS;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BRUSH;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_TICKS;
	}

	/** Held to the end of its use, the kit paints its holder's own face. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			paintFace(player, player, stack, player.getUsedItemHand());
		}
		return stack;
	}

	/** Used on another player, the kit paints their face. */
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof Player friend) || friend == player) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer painter) {
			paintFace(painter, friend, stack, hand);
		}
		return InteractionResult.SUCCESS;
	}

	/** {@code painter} paints the kit's design on {@code face}, wearing the kit by one use. */
	public static void paintFace(ServerPlayer painter, Player face, ItemStack kit, InteractionHand hand) {
		FacePaint.Design design = design(kit);
		FacePaint.paint(face, design);
		face.level().playSound(null, face.getX(), face.getY(), face.getZ(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (face instanceof ServerPlayer painted) {
			painted.sendOverlayMessage(Component.translatable("message.jugcraft.face_paint.painted", design.displayName()));
		}
		if (face != painter) {
			TrickOrTreat.award(painter, "face_painter");
		}
		kit.hurtAndBreak(1, painter, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.face_paint_kit.design", design(stack).displayName()).withStyle(ChatFormatting.GOLD));
		tooltip.accept(Component.translatable("item.jugcraft.face_paint_kit.hint").withStyle(ChatFormatting.GRAY));
	}
}
