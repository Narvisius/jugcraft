package io.github.jimbozoomer.jugcraft.agriculture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * What the wrought-iron candelabra (Halloween decorations batch 17, tools/decor17.py) share: the floor candelabrum, the
 * table candelabrum, the wall girandole and the branching chandelier. Their candles, drawn by the client where the
 * generated {@code /jugcraft/candelabra.json} puts them, take one of six {@link Wax}es (from a dye) and burn with one of
 * four {@link Flame}s (soul sand or soil, an amethyst shard, a glow ink sac). Flint and steel, a fire charge or a burning
 * arrow lights them; an empty hand snuffs them; a redstone signal lights them while it lasts. While they burn, wax drips
 * down them, a stage at a time ({@link #DRIPS}, one random tick in {@value #DRIP_CHANCE}); shears scrape the drips off and
 * give nothing, so a burning candle is no wax farm. Changing them needs build rights.
 */
public final class Candelabra {
	public static final int DRIP_STAGES = 3;
	public static final int DRIP_CHANCE = 18;
	public static final float CANDLE_WIDTH = 1.6F;
	public static final EnumProperty<Wax> WAX = EnumProperty.create("wax", Wax.class);
	public static final EnumProperty<Flame> FLAME = EnumProperty.create("flame", Flame.class);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final IntegerProperty DRIPS = IntegerProperty.create("drips", 0, DRIP_STAGES);
	/** Each fitting's candles, {x, y, z, height} in pixels for a fitting facing north (tools/decor17.py CANDELABRA). */
	private static final Map<String, float[][]> LAYOUT = load();

	private Candelabra() {
	}

	/** A candle's wax: its colour (RGB) for the client to tint. */
	public enum Wax implements StringRepresentable {
		IVORY(0xECE2C8), BLACK(0x342E3A), PURPLE(0x8042B0), GREEN(0x4C8E4A), ORANGE(0xE6802C), RED(0xAA2428);

		public final int rgb;

		Wax(int rgb) {
			this.rgb = rgb;
		}

		/** The wax a dye (or bone meal, for ivory) gives, or null. */
		public static Wax of(ItemStack stack) {
			if (stack.is(Items.BONE_MEAL)) {
				return IVORY;
			}
			DyeColor dye = ScarecrowBlock.dyeColor(stack);
			if (dye == null) {
				return null;
			}
			return switch (dye) {
				case WHITE -> IVORY;
				case BLACK -> BLACK;
				case PURPLE -> PURPLE;
				case GREEN -> GREEN;
				case ORANGE -> ORANGE;
				case RED -> RED;
				default -> null;
			};
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** A flame's colour (RGB) for the client to tint, and what turns flames to it. */
	public enum Flame implements StringRepresentable {
		ORDINARY(0xFFC460), SOUL(0x60E2FF), WITCHFIRE(0xCC70FF), GHOSTFIRE(0x78FF96);

		public final int rgb;

		Flame(int rgb) {
			this.rgb = rgb;
		}

		/** The flame a catalyst gives, or null. Ordinary flames come back by snuffing nothing: they are only the default. */
		public static Flame of(ItemStack stack) {
			if (stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL)) {
				return SOUL;
			}
			if (stack.is(Items.AMETHYST_SHARD)) {
				return WITCHFIRE;
			}
			return stack.is(Items.GLOW_INK_SAC) ? GHOSTFIRE : null;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private static Map<String, float[][]> load() {
		JsonObject root;
		try (InputStream stream = Candelabra.class.getResourceAsStream("/jugcraft/candelabra.json")) {
			if (stream == null) {
				throw new IOException("missing");
			}
			root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Could not read /jugcraft/candelabra.json", e);
		}
		Map<String, float[][]> out = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			JsonArray list = entry.getValue().getAsJsonArray();
			float[][] candles = new float[list.size()][];
			for (int i = 0; i < list.size(); i++) {
				JsonArray c = list.get(i).getAsJsonArray();
				candles[i] = new float[] {c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat(), c.get(3).getAsFloat()};
			}
			out.put(entry.getKey(), candles);
		}
		return out;
	}

	/** The candles of fitting {@code kind} (its block id), {x, y, z, height} in pixels for a fitting facing north. */
	public static float[][] candles(String kind) {
		return LAYOUT.getOrDefault(kind, new float[0][]);
	}

	/** Where candle {x, y, z} stands for a fitting facing {@code facing}, turned as its block model is. */
	public static float[] turned(float[] candle, Direction facing) {
		float x = candle[0];
		float z = candle[2];
		return switch (facing) {
			case EAST -> new float[] {16 - z, candle[1], x};
			case SOUTH -> new float[] {16 - x, candle[1], 16 - z};
			case WEST -> new float[] {z, candle[1], 16 - x};
			default -> new float[] {x, candle[1], z};
		};
	}

	/** The light a fitting giving {@code light} when lit gives in {@code state}. */
	public static int light(BlockState state, int light) {
		return state.getValue(LIT) ? light : 0;
	}

	/**
	 * What an item does to a fitting: flint and steel or a fire charge lights it, a dye changes its wax, a catalyst its
	 * flame, shears scrape its drips off. {@code set} changes the fitting (both halves of a tall one).
	 */
	public static InteractionResult use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			Consumer<BlockState> set) {
		boolean flint = stack.is(Items.FLINT_AND_STEEL);
		if (flint || stack.is(Items.FIRE_CHARGE)) {
			if (state.getValue(LIT)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				set.accept(state.setValue(LIT, true));
				level.playSound(null, pos, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F,
						level.getRandom().nextFloat() * 0.4F + 0.8F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				if (flint) {
					stack.hurtAndBreak(1, player, hand);
				} else {
					stack.consume(1, player);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (!player.mayBuild()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		Wax wax = Wax.of(stack);
		if (wax != null) {
			if (wax == state.getValue(WAX)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				set.accept(state.setValue(WAX, wax));
				stack.consume(1, player);
				level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
			return InteractionResult.SUCCESS;
		}
		Flame flame = Flame.of(stack);
		if (flame != null) {
			if (flame == state.getValue(FLAME)) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				set.accept(state.setValue(FLAME, flame));
				stack.consume(1, player);
				level.playSound(null, pos, flame == Flame.WITCHFIRE ? SoundEvents.AMETHYST_BLOCK_CHIME
						: flame == Flame.SOUL ? SoundEvents.SOUL_ESCAPE.value() : SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
				if (level instanceof ServerLevel server) {
					server.sendParticles(flame == Flame.SOUL ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.7,
							pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.01);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.SHEARS) && state.getValue(DRIPS) > 0) {
			if (!level.isClientSide()) {
				set.accept(state.setValue(DRIPS, 0));
				stack.hurtAndBreak(1, player, hand);
				level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.4F);
				level.gameEvent(player, GameEvent.SHEAR, pos);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/** An empty hand snuffs the candles (only an empty one: anything held does what it would on any block). */
	public static InteractionResult snuff(BlockState state, Level level, BlockPos pos, Player player, Consumer<BlockState> set) {
		if (!state.getValue(LIT) || !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			set.accept(state.setValue(LIT, false));
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** A redstone signal coming lights the candles, and its going snuffs them; returns the state to set, or null. */
	public static BlockState powered(BlockState state, boolean powered) {
		if (powered == state.getValue(POWERED)) {
			return null;
		}
		return state.setValue(POWERED, powered).setValue(LIT, powered);
	}

	/** Whether a fitting in {@code state} drips (only while it burns, until the drips are at their last stage). */
	public static boolean drips(BlockState state) {
		return state.getValue(LIT) && state.getValue(DRIPS) < DRIP_STAGES;
	}

	/** One random tick in {@value #DRIP_CHANCE}, the drips grow a stage; returns the state to set, or null. */
	public static BlockState drip(BlockState state, RandomSource random) {
		return drips(state) && random.nextInt(DRIP_CHANCE) == 0 ? state.setValue(DRIPS, state.getValue(DRIPS) + 1) : null;
	}

	/** Now and then a thread of smoke from one of the burning candles of fitting {@code kind}. */
	public static void smoke(String kind, BlockState state, Level level, BlockPos pos, Direction facing, RandomSource random) {
		float[][] candles = candles(kind);
		if (!state.getValue(LIT) || candles.length == 0 || random.nextInt(3) != 0) {
			return;
		}
		float[] candle = candles[random.nextInt(candles.length)];
		float[] at = turned(candle, facing);
		level.addParticle(ParticleTypes.SMOKE, pos.getX() + at[0] / 16, pos.getY() + (at[1] + candle[3] + 2.5) / 16, pos.getZ() + at[2] / 16,
				0.0, 0.01, 0.0);
	}
}
