package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * The Candy Bag: knocks on villagers' doors ({@link TrickOrTreat}) and holds treats like a bundle, and only treats
 * (item tag {@code jugcraft:candy_bag_treats}). Treats handed out at a door go straight into the bag the player
 * carries. Its tooltip shows how many homes gave its owner a treat tonight: the server writes that on the bag
 * ({@link Night}) when a treat is given and wipes it once the night is over, so the client only displays it.
 */
public class CandyBagItem extends BundleItem {
	public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, Jugcraft.id("candy_bag_treats"));

	/** Which night ({@link TrickOrTreat#night}) the bag went out, and how many homes gave a treat that night. */
	public record Night(long night, int homes) {
		public static final Codec<Night> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.LONG.fieldOf("night").forGetter(Night::night),
				Codec.intRange(0, TrickOrTreat.MAX_HOMES).fieldOf("homes").forGetter(Night::homes)).apply(i, Night::new));
		public static final StreamCodec<ByteBuf, Night> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG, Night::night, ByteBufCodecs.VAR_INT, Night::homes, Night::new);
	}

	public CandyBagItem(Properties properties) {
		super(properties);
	}

	/** Only treats go in; anything else is swapped as usual. */
	@Override
	public boolean overrideStackedOnOther(ItemStack bag, Slot slot, ClickAction action, Player player) {
		ItemStack other = slot.getItem();
		return (other.isEmpty() || other.is(TREATS)) && super.overrideStackedOnOther(bag, slot, action, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack bag, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
		return (other.isEmpty() || other.is(TREATS)) && super.overrideOtherStackedOnMe(bag, other, slot, action, player, access);
	}

	/** Puts as much of {@code treat} as fits into the bag (treats only); {@code treat} keeps what did not fit. */
	public static void fill(ItemStack bag, ItemStack treat) {
		BundleContents contents = bag.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null || treat.isEmpty() || !treat.is(TREATS)) {
			return;
		}
		BundleContents.Mutable mutable = contents.asMutable();
		int added = mutable.tryInsert(treat.copy());
		if (added > 0) {
			treat.shrink(added);
			bag.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
		}
	}

	/** Writes tonight's count of homes on the bag (server side). */
	public static void recordHomes(ItemStack bag, long night, int homes) {
		bag.set(JugcraftAgriculture.CANDY_BAG_NIGHT, new Night(night, Math.min(homes, TrickOrTreat.MAX_HOMES)));
	}

	/** Once a second, a bag carried into a new night forgets the old one's count. */
	@Override
	public void inventoryTick(ItemStack bag, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(bag, level, owner, slot);
		if (level.getGameTime() % 20 == 0) {
			forget(bag, TrickOrTreat.night(level.getOverworldClockTime()));
		}
	}

	/** If the bag's count belongs to another night than {@code night}, forgets it. Returns whether it did. */
	public static boolean forget(ItemStack bag, long night) {
		Night counted = bag.get(JugcraftAgriculture.CANDY_BAG_NIGHT);
		if (counted != null && counted.night() != night) {
			bag.remove(JugcraftAgriculture.CANDY_BAG_NIGHT);
			return true;
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack bag, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(bag, context, display, tooltip, flag);
		Night night = bag.get(JugcraftAgriculture.CANDY_BAG_NIGHT);
		tooltip.accept((night == null || night.homes() == 0 ? Component.translatable("item.jugcraft.candy_bag.no_homes")
				: Component.translatable("item.jugcraft.candy_bag.homes", night.homes(), TrickOrTreat.FULL_BAG)).withStyle(ChatFormatting.GRAY));
	}
}
