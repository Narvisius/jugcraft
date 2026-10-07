package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * The Witchlight Lantern Path (Halloween decorations batch 19): what the Witchlight Lamp-Post, Path Stake and Hanging
 * Witchlight share. Asleep, a lamp glows dimly (light {@value #ASLEEP}) with a single wisp turning inside; when a player
 * comes within {@value #RANGE} blocks it flares awake (light {@value #AWAKE}) with a soft rising chime, and stays awake
 * {@value #LINGER_TICKS} ticks after the last player leaves, then goes back to sleep (the client fades its glow over
 * {@value #FADE_TICKS} ticks). A redstone signal keeps it awake. A dye on the glass changes its {@link Colour}. Each lamp
 * looks for players twice a second, near itself only (WitchlightBlockEntity).
 */
public final class Witchlights {
	public static final int ASLEEP = 3;
	public static final int AWAKE = 14;
	public static final double RANGE = 6.0;
	public static final int LINGER_TICKS = 200;
	public static final int FADE_TICKS = 40;
	public static final int CHECK_TICKS = 10;
	public static final BooleanProperty LIT = BooleanProperty.create("awake");
	public static final EnumProperty<Colour> COLOUR = EnumProperty.create("colour", Colour.class);

	/** The glass's colours, and the dye that gives each. */
	public enum Colour implements StringRepresentable {
		PURPLE(DyeColor.PURPLE, 0xB45CFF), GREEN(DyeColor.LIME, 0x7CFF5A), ORANGE(DyeColor.ORANGE, 0xFFA030),
		BLUE(DyeColor.LIGHT_BLUE, 0x5AC8FF), RED(DyeColor.RED, 0xFF4A3A);

		private final DyeColor dye;
		private final int rgb;

		Colour(DyeColor dye, int rgb) {
			this.dye = dye;
			this.rgb = rgb;
		}

		public int rgb() {
			return rgb;
		}

		/** The colour a dye gives the glass, or null if it gives none. */
		public static @Nullable Colour of(@Nullable DyeColor dye) {
			for (Colour colour : values()) {
				if (colour.dye == dye) {
					return colour;
				}
			}
			return null;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private Witchlights() {
	}

	/** The light of a lamp in {@code state} (the caller decides whether this block holds the lantern). */
	public static int light(BlockState state) {
		return state.getValue(LIT) ? AWAKE : ASLEEP;
	}

	/**
	 * Whether a lamp awake since it last saw a player at {@code lastSeen} should be awake at {@code now}: a player is near
	 * or it is powered, or the last one left less than {@value #LINGER_TICKS} ticks ago.
	 */
	public static boolean awake(boolean playerNear, boolean powered, long now, long lastSeen) {
		return playerNear || powered || now - lastSeen < LINGER_TICKS;
	}

	/** Recolours the glass of the lamp at {@code pos} with the dye in {@code stack}; PASS if it is no witchlight dye. */
	public static InteractionResult dye(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack) {
		Colour colour = Colour.of(ScarecrowBlock.dyeColor(stack));
		if (colour == null || colour == state.getValue(COLOUR)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (!level.mayInteract(player, pos)) {
				return InteractionResult.FAIL;
			}
			setLamp(level, pos, state.setValue(COLOUR, colour));
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Sets the lamp's state, on both halves of a lamp-post. */
	static void setLamp(Level level, BlockPos pos, BlockState changed) {
		if (changed.getBlock() instanceof WitchlightLampPostBlock) {
			WitchlightLampPostBlock.setPost(level, pos, changed);
		} else {
			level.setBlock(pos, changed, net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
	}

	/** Wakes or sleeps the lamp, with a rising chime as it wakes. */
	static void wake(Level level, BlockPos pos, BlockState state, boolean awake) {
		setLamp(level, pos, state.setValue(LIT, awake));
		if (awake) {
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
		}
	}
}
