package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A piece of nuclear equipment in item form. Placing it builds the whole thing: it stands with its front left corner
 * where it is placed and reaches to the player's right and away from them.
 */
public class EquipmentItem extends BlockItem {
	public final EquipmentKind kind;

	public EquipmentItem(Block anchor, EquipmentKind kind, Properties properties) {
		super(anchor, properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		if (EquipmentRegistry.of(level).anchorOf(clicked) != null && context.getPlayer() != null && !context.getPlayer().isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		BlockPos at = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = context.getHorizontalDirection();
		if (!EquipmentPlacement.place(server, at, kind, facing, context.getPlayer())) {
			return InteractionResult.FAIL;
		}
		var sound = JugcraftNuclear.EQUIPMENT.get(kind).defaultBlockState().getSoundType();
		level.playSound(null, at, sound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.nuclear.size", kind.wide, kind.deep, kind.tall).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.jugcraft.nuclear." + kind.id).withStyle(ChatFormatting.DARK_GRAY));
	}
}
