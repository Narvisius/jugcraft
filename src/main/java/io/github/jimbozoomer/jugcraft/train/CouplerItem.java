package io.github.jimbozoomer.jugcraft.train;

import java.util.Comparator;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The Coupler (docs/features/trains.md): use it on a car standing on the line just behind a train to couple it on;
 * sneak and use it on a coupled car to uncouple it and every car behind it (on a locomotive, every car). The work is
 * done by {@link JugcraftTrains}' entity use callback, which calls {@link #use}.
 */
public class CouplerItem extends Item {
	/** How far away the train's locomotive may be, in blocks: the length of a full train. */
	private static final double SEARCH = 48.0;

	public CouplerItem(Properties properties) {
		super(properties);
	}

	/** Couples or uncouples {@code car}; the player is told what happened. */
	public static InteractionResult use(Player player, TrainCar car) {
		if (!(player.level() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		String message;
		if (player.isSecondaryUseActive()) {
			if (car instanceof Locomotive engine) {
				message = engine.cars().isEmpty() ? "message.jugcraft.train.nothing_coupled" : "message.jugcraft.train.uncoupled_all";
				engine.uncoupleAll();
			} else if (car.coupledTo() != null && level.getEntity(car.coupledTo()) instanceof Locomotive engine) {
				engine.uncouple(car);
				message = "message.jugcraft.train.uncoupled";
			} else {
				message = "message.jugcraft.train.nothing_coupled";
			}
		} else if (car instanceof Locomotive) {
			message = "message.jugcraft.train.couple_wagon";
		} else if (car.coupledTo() != null) {
			message = "message.jugcraft.train.already_coupled";
		} else {
			Locomotive engine = level.getEntitiesOfClass(Locomotive.class, car.getBoundingBox().inflate(SEARCH), Locomotive::isAlive).stream()
					.sorted(Comparator.comparingDouble(e -> e.distanceToSqr(car)))
					.filter(e -> e.couple(level, car)).findFirst().orElse(null);
			if (engine == null) {
				message = "message.jugcraft.train.couple_behind";
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.train.coupled", engine.cars().size() + 1));
				level.playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
				return InteractionResult.SUCCESS;
			}
		}
		player.sendOverlayMessage(Component.translatable(message));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable("tooltip.jugcraft.coupler").withStyle(ChatFormatting.GRAY));
	}
}
