package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hot-air balloon as an item (fall addition 29), packed with its fuel (the {@code balloon_fuel} component). Used on
 * the top of a block with room over it, it sets the balloon up there, its front towards its user.
 */
public class HotAirBalloonItem extends Item {
	/** Blocks of air it needs over the basket's floor: up to its throat, where the burner's flame goes. */
	public static final int CLEARANCE = 4;
	private final HotAirBalloon.Kind kind;

	public HotAirBalloonItem(Properties properties, HotAirBalloon.Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public HotAirBalloon.Kind kind() {
		return kind;
	}

	public static int fuel(ItemStack stack) {
		return stack.getOrDefault(JugcraftAgriculture.BALLOON_FUEL, 0);
	}

	/** Whether a balloon's basket fits on {@code ground}'s top, with {@link #CLEARANCE} blocks of air over it. */
	public static boolean roomFor(Level level, BlockPos ground) {
		Vec3 at = Vec3.atBottomCenterOf(ground.above());
		double half = HotAirBalloon.BASKET / 2;
		AABB basket = new AABB(at.x - half, at.y, at.z - half, at.x + half, at.y + CLEARANCE, at.z + half);
		return level.noCollision(basket) && !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty();
	}

	/** Sets up a balloon of {@code kind} on {@code ground}, facing {@code yaw}, with {@code fuel}; null if there is no room. */
	public static @Nullable HotAirBalloon setUp(ServerLevel level, BlockPos ground, HotAirBalloon.Kind kind, float yaw, int fuel) {
		if (!roomFor(level, ground)) {
			return null;
		}
		HotAirBalloon balloon = JugcraftAgriculture.HOT_AIR_BALLOON.create(level, EntitySpawnReason.TRIGGERED);
		if (balloon == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(ground.above());
		balloon.snapTo(at.x, at.y, at.z, yaw, 0.0F);
		balloon.setKind(kind);
		balloon.setFuel(fuel);
		level.addFreshEntity(balloon);
		return balloon;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos ground = context.getClickedPos();
		if (!roomFor(level, ground)) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.no_room"));
			}
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			ItemStack stack = context.getItemInHand();
			// Its front towards whoever set it up.
			float yaw = context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot() + 180.0F;
			if (context.getPlayer() != null && !server.mayInteract(context.getPlayer(), ground.above())
					|| setUp(server, ground, kind, yaw, fuel(stack)) == null) {
				return InteractionResult.FAIL;
			}
			server.playSound(null, ground.above(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
			stack.consume(1, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.hot_air_balloon.fuel", Broomstick.seconds(fuel(stack))).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.jugcraft.hot_air_balloon.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
