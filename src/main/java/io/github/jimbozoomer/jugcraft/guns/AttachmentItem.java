package io.github.jimbozoomer.jugcraft.guns;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A gun attachment ({@link JugcraftGuns#ATTACHMENTS}): fitted to a gun that takes it in a crafting grid
 * ({@link GunAttachmentRecipe}), where it changes the gun's numbers and shows on its model as the gun's own part.
 */
public class AttachmentItem extends Item {
	private final String name;

	public AttachmentItem(String name, Properties properties) {
		super(properties);
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		GunAttachment attachment = JugcraftGuns.ATTACHMENTS.get(name);
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.slot." + attachment.slot()).withStyle(ChatFormatting.GOLD));
		effects(attachment, tooltip);
		if (attachment.bayonet()) {
			// Half hearts, shown as the game shows attack damage; and the key that stabs.
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.stab", attachment.stab(), Component.keybind("key.jugcraft.stab"))
					.withStyle(ChatFormatting.GREEN));
		}
		MutableComponent guns = Component.empty();
		JugcraftGuns.ACCEPTS.forEach((gun, takes) -> {
			if (takes.contains(name)) {
				if (!guns.getSiblings().isEmpty()) {
					guns.append(", ");
				}
				guns.append(Component.translatable(JugcraftGuns.GUNS.get(gun).getDescriptionId()));
			}
		});
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.fits", guns).withStyle(ChatFormatting.BLUE));
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns." + name).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns.fitting").withStyle(ChatFormatting.DARK_GRAY));
	}

	/** A line for each number the attachment changes, as a percentage: green where it helps, red where it costs. */
	static void effects(GunAttachment attachment, Consumer<Component> tooltip) {
		effect(tooltip, "damage", attachment.damage(), true);
		effect(tooltip, "range", attachment.range(), true);
		effect(tooltip, "hip_spread", attachment.hipSpread(), false);
		effect(tooltip, "aim_spread", attachment.aimSpread(), false);
		effect(tooltip, "capacity", attachment.capacity(), true);
		effect(tooltip, "reload", attachment.reload(), false);
		effect(tooltip, "kick", attachment.kick(), false);
		effect(tooltip, "volume", attachment.volume(), false);
	}

	private static void effect(Consumer<Component> tooltip, String key, float factor, boolean moreIsBetter) {
		int percent = Math.round((factor - 1.0F) * 100.0F);
		if (percent != 0) {
			tooltip.accept(Component.translatable("tooltip.jugcraft.guns.effect." + key, (percent > 0 ? "+" : "") + percent + "%")
					.withStyle(percent > 0 == moreIsBetter ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}
}
