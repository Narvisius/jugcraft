package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A pibal (fall addition 29): used, it is let go just over its user's head, to rise on the winds. */
public class PibalItem extends Item {
	public PibalItem(Properties properties) {
		super(properties);
	}

	/** Lets a pibal go at {@code at}. */
	public static @Nullable Pibal release(ServerLevel level, Vec3 at) {
		Pibal pibal = JugcraftAgriculture.PIBAL.create(level, EntitySpawnReason.TRIGGERED);
		if (pibal == null) {
			return null;
		}
		pibal.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		level.addFreshEntity(pibal);
		return pibal;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0.0, 0.5, 0.0);
			if (release(server, at) != null) {
				server.playSound(null, at.x, at.y, at.z, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6F, 1.6F);
				player.getItemInHand(hand).consume(1, player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.pibal.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
