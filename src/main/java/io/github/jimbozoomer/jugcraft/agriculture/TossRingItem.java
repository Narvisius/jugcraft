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

/** A Toss Ring (fall addition 26): tossed underhand, gently, at a Ring Toss crate ({@link TossRing}). */
public class TossRingItem extends Item {
	public static final String ID = "toss_ring";
	/** How hard it is tossed (blocks a tick): softer than a snowball, so it arcs. */
	public static final float SPEED = 0.75F;

	public TossRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.4F, 1.4F);
		if (level instanceof ServerLevel server) {
			TossRing ring = new TossRing(server, player, stack);
			ring.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SPEED, 0.5F);
			server.addFreshEntity(ring);
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
