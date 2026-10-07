package io.github.jimbozoomer.jugcraft.rocketry;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A rocket fired by hand (batch 38, docs/features/rocketry.md). It needs open sky above the player. It rises as a
 * firework (one that only bursts for the flares, high up, so it hurts no one) and does its work at the top,
 * {@link JugcraftRocketry#LAUNCH_DELAY} ticks later ({@link JugcraftRocketry#arrive}).
 */
public class RocketItem extends Item {
	public enum Kind {
		SURVEY, RAIN, CLEAR, SIGNAL, ILLUMINATION;

		boolean weather() {
			return this == RAIN || this == CLEAR;
		}
	}

	private final Kind kind;

	public RocketItem(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos head = BlockPos.containing(player.getEyePosition());
		if (server.getHeight(Heightmap.Types.MOTION_BLOCKING, head.getX(), head.getZ()) > head.getY()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.rocket.indoors"));
			return InteractionResult.FAIL;
		}
		if (kind.weather()) {
			if (!JugcraftRocketry.hasWeather(server)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.rocket.no_weather"));
				return InteractionResult.FAIL;
			}
			long wait = JugcraftRocketry.weatherWait(server);
			if (wait > 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.rocket.weather_cooldown", (wait + 19) / 20));
				return InteractionResult.FAIL;
			}
		}
		launch(server, player, kind);
		stack.consume(1, player);
		player.getCooldowns().addCooldown(stack, JugcraftRocketry.COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	/** Sends a rocket of {@code kind} up from beside {@code player} and books its arrival. */
	public static void launch(ServerLevel level, Player player, Kind kind) {
		ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
		firework.set(DataComponents.FIREWORKS, new Fireworks(2, explosions(kind)));
		FireworkRocketEntity rocket = new FireworkRocketEntity(level, player.getX(), player.getEyeY() - 0.3, player.getZ(), firework);
		level.addFreshEntity(rocket);
		level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0F, 0.7F);
		JugcraftRocketry.book(level, player, kind);
	}

	private static List<FireworkExplosion> explosions(Kind kind) {
		return switch (kind) {
			case SIGNAL -> List.of(new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(0xB02E26, 0xE04030),
					IntList.of(0xFF8060), true, true));
			case ILLUMINATION -> List.of(new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(0xFFFFF0),
					IntList.of(0xFFF0A0), true, true));
			default -> List.of();
		};
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft." + JugcraftRocketry.id(this)).withStyle(ChatFormatting.GRAY));
	}
}
