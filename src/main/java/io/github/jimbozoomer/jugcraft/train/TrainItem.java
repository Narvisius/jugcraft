package io.github.jimbozoomer.jugcraft.train;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A locomotive or car as an item (docs/features/trains.md): use it on a rail to set it on the line there, facing
 * along the rail the way you face. It needs the line clear of other trains for its length.
 */
public class TrainItem extends Item {
	private final Function<Level, TrainCar> factory;
	private final String tooltip;

	public TrainItem(Properties properties, Function<Level, TrainCar> factory, String tooltip) {
		super(properties);
		this.factory = factory;
		this.tooltip = tooltip;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Vec3 look = player != null ? Vec3.directionFromRotation(0, player.getYRot()) : new Vec3(0, 0, 1);
		TrackPoint point = TrackPoint.onRail(context.getLevel(), context.getClickedPos(), look);
		if (point == null) {
			if (player != null && !context.getLevel().isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.train.on_rail"));
			}
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		TrainCar car = factory.apply(level);
		car.place(point);
		// Its length of line must be clear of other trains: no car nearer than their two half-lengths.
		AABB near = car.getBoundingBox().inflate(car.length());
		if (level.getEntitiesOfClass(TrainCar.class, near, TrainCar::isAlive).stream()
				.anyMatch(other -> other.position().distanceTo(car.position()) < (other.length() + car.length()) / 2)) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.train.no_room"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(car);
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
	}
}
