package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Knitting: yarn (one item, coloured by the vanilla {@code dyed_color} component), the garments, and keeping cosy. A
 * player wearing {@value #COZY_PIECES} or more pieces of knitwear (item tag {@code jugcraft:knitwear}) within
 * {@value #COZY_RANGE} blocks of a lit campfire is cosy: Regeneration I for {@value #COZY_EFFECT_TICKS} ticks, renewed
 * while they stay; wearing a beanie, a sweater and socks there earns Snug as a Bug. The server looks at each player every
 * {@value #COZY_TICKS} ticks, and looks for a campfire only round those wearing enough knitwear.
 */
public final class Knitting {
	public static final int COZY_TICKS = 40;
	public static final int COZY_PIECES = 2;
	public static final int COZY_RANGE = 4;
	public static final int COZY_EFFECT_TICKS = 60;
	/** Yarn a skein of wool spins into. */
	public static final int YARN_PER_WOOL = 4;
	/** Undyed yarn and knitwear: the cream of undyed wool. */
	public static final int UNDYED = 0xF0E6D2;
	public static final TagKey<Item> KNITWEAR = TagKey.create(Registries.ITEM, Jugcraft.id("knitwear"));

	private Knitting() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % COZY_TICKS != 0 || !JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE)) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!player.isSpectator()) {
					warm(player.level(), player);
				}
			}
		});
	}

	public static boolean isYarn(ItemStack stack) {
		return stack.is(JugcraftAgriculture.item("yarn"));
	}

	/** The colour of a ball of yarn or a garment. */
	public static int color(ItemStack stack) {
		DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
		return dyed == null ? UNDYED : dyed.rgb() & 0xFFFFFF;
	}

	/** {@code count} balls of yarn of colour {@code rgb}. */
	public static ItemStack yarn(int rgb, int count) {
		ItemStack yarn = new ItemStack(JugcraftAgriculture.item("yarn"), count);
		yarn.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb & 0xFFFFFF));
		return yarn;
	}

	/** A finished {@code knit} in colour {@code rgb}. */
	public static ItemStack garment(Knitwear knit, int rgb) {
		ItemStack garment = new ItemStack(JugcraftAgriculture.item(knit.item));
		garment.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb & 0xFFFFFF));
		return garment;
	}

	/** The colour of wool {@code stack} (the dye it was made with), or null if it isn't wool. */
	public static @Nullable DyeColor woolColor(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (!path.endsWith("_wool")) {
			return null;
		}
		String name = path.substring(0, path.length() - "_wool".length());
		for (DyeColor dye : DyeColor.values()) {
			if (dye.getSerializedName().equals(name)) {
				return dye;
			}
		}
		return null;
	}

	/** How many pieces of knitwear {@code player} wears. */
	public static int piecesWorn(ServerPlayer player) {
		int pieces = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			pieces += player.getItemBySlot(slot).is(KNITWEAR) ? 1 : 0;
		}
		return pieces;
	}

	/** Whether a lit campfire is within {@value #COZY_RANGE} blocks of {@code pos} (two up or down). */
	public static boolean nearFire(ServerLevel level, BlockPos pos) {
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-COZY_RANGE, -2, -COZY_RANGE), pos.offset(COZY_RANGE, 2, COZY_RANGE))) {
			BlockState state = level.getBlockState(at);
			if (state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT)) {
				return true;
			}
		}
		return false;
	}

	/** One look at {@code player}: cosy if wearing enough knitwear by a fire. Returns whether they were. */
	public static boolean warm(ServerLevel level, ServerPlayer player) {
		int pieces = piecesWorn(player);
		if (pieces < COZY_PIECES || !nearFire(level, player.blockPosition())) {
			return false;
		}
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COZY_EFFECT_TICKS, 0, true, true));
		if (player.getItemBySlot(EquipmentSlot.HEAD).is(KNITWEAR) && player.getItemBySlot(EquipmentSlot.CHEST).is(KNITWEAR)
				&& player.getItemBySlot(EquipmentSlot.FEET).is(KNITWEAR)) {
			TrickOrTreat.award(player, "snug_as_a_bug");
		}
		return true;
	}
}
