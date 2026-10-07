package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/**
 * A spooky firework: used on a block, it goes up as a {@link SpookyRocket} and bursts into its {@link FireworkShape}.
 * Its flight level is the vanilla {@code minecraft:fireworks} component's (1 to 3, from the gunpowder it was made with);
 * glowstone dust in the recipe makes it twinkle ({@code jugcraft:twinkle}). It can't boost elytra flight. Dispensers
 * and the Show Launcher fire it too.
 */
public class SpookyFireworkItem extends Item {
	private static final String TOOLTIP = "tooltip.jugcraft.spooky_firework.";
	private final FireworkShape shape;

	public SpookyFireworkItem(FireworkShape shape, Properties properties) {
		super(properties);
		this.shape = shape;
	}

	public FireworkShape shape() {
		return shape;
	}

	/** Whether {@code stack} is a rocket the Show Launcher and dispensers can fire: a spooky firework or a vanilla one. */
	public static boolean rocket(ItemStack stack) {
		return stack.getItem() instanceof SpookyFireworkItem || stack.is(Items.FIREWORK_ROCKET);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level) {
			ItemStack stack = context.getItemInHand();
			Direction face = context.getClickedFace();
			Vec3 at = context.getClickLocation().add(face.getStepX() * 0.15, face.getStepY() * 0.15, face.getStepZ() * 0.15);
			Vec3 velocity = new Vec3(level.getRandom().nextGaussian() * 0.001, 0.05, level.getRandom().nextGaussian() * 0.001);
			SpookyRocket rocket = new SpookyRocket(level, at.x, at.y, at.z, stack, velocity, false);
			rocket.setOwner(context.getPlayer());
			level.addFreshEntity(rocket);
			stack.consume(1, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Dispensers fire spooky fireworks: facing up, the rocket climbs as if set off by hand; facing any other way, it
	 * flies straight out and bursts where it hits something or at the end of its flight.
	 */
	static void registerDispensing() {
		DefaultDispenseItemBehavior launch = new DefaultDispenseItemBehavior() {
			@Override
			protected ItemStack execute(BlockSource source, ItemStack stack) {
				Direction facing = source.state().getValue(DispenserBlock.FACING);
				Vec3 at = source.center().add(facing.getStepX() * 0.7, facing.getStepY() * 0.7, facing.getStepZ() * 0.7);
				boolean up = facing == Direction.UP;
				Vec3 velocity = up ? new Vec3(0.0, 0.05, 0.0) : new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(0.5);
				source.level().addFreshEntity(new SpookyRocket(source.level(), at.x, at.y, at.z, stack, velocity, !up));
				stack.shrink(1);
				return stack;
			}
		};
		for (FireworkShape shape : FireworkShape.values()) {
			DispenserBlock.registerBehavior(JugcraftAgriculture.item(shape.item()), launch);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable(TOOLTIP + shape.id()).withStyle(ChatFormatting.GRAY));
		if (Boolean.TRUE.equals(stack.get(JugcraftAgriculture.TWINKLE))) {
			tooltip.accept(Component.translatable(TOOLTIP + "twinkle").withStyle(ChatFormatting.GRAY));
		}
	}
}
