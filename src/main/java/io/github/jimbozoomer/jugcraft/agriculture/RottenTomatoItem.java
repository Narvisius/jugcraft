package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Rotten Tomato (garden crops, tools/garden.py ROTTEN_TOMATO): from a tomato vine left too long ({@link TomatoVineBlock}).
 * Thrown like a snowball ({@link RottenTomato}), or composted.
 */
public class RottenTomatoItem extends Item {
	public static final String ID = TomatoVineBlock.ROTTEN;
	/** How hard it is thrown (blocks a tick), as a snowball. */
	public static final float SPEED = 1.5F;

	public RottenTomatoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
		if (level instanceof ServerLevel server) {
			RottenTomato tomato = new RottenTomato(server, player, stack);
			tomato.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SPEED, 1.0F);
			server.addFreshEntity(tomato);
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
