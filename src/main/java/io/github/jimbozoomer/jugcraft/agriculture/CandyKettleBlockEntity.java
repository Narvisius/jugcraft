package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What is in a Candy Kettle, and how hot it is. A batch is a base ({@link CandyBase}), up to {@value #MAX_SUGAR} sugar
 * (two pieces of candy each), up to {@value #MAX_FLAVOURS} flavours and any dyes (mixed as dyes mix on leather), all added
 * below {@value #ADD_BELOW} degrees. Over a heat source ({@link CookingPotBlockEntity#isHeated}) a batch warms a degree
 * every {@value #HEAT_TICKS} ticks up to the boil, every {@value #BOIL_TICKS} while its water boils off (to
 * {@value #BOILED} degrees), and every {@value #COOK_TICKS} after that, up to {@value #MAX_TEMP}. Off the heat it cools a
 * degree every {@value #COOL_TICKS} ticks to {@value #ROOM}. The stage its candy sets at is that of the hottest it has
 * been ({@link #cooked}), and a bell rings as it reaches each. Saved, and sent to clients for the syrup and the
 * thermometer's needle to show.
 */
public class CandyKettleBlockEntity extends BlockEntity {
	public static final int MAX_SUGAR = 4;
	public static final int PIECES_PER_SUGAR = 2;
	public static final int MAX_FLAVOURS = 2;
	public static final int ROOM = 20;
	public static final int BOIL = 100;
	public static final int BOILED = 110;
	public static final int MAX_TEMP = 190;
	public static final int ADD_BELOW = 100;
	public static final int HEAT_TICKS = 4;
	public static final int BOIL_TICKS = 12;
	public static final int COOK_TICKS = 6;
	public static final int COOL_TICKS = 8;

	private @Nullable CandyBase base;
	private int sugar;
	private final List<CandyFlavour> flavours = new ArrayList<>();
	private int red;
	private int green;
	private int blue;
	private int peak;
	private int dyes;
	private int temperature = ROOM;
	private int cooked = ROOM;
	private int step;

	public CandyKettleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CANDY_KETTLE_ENTITY, pos, state);
	}

	public @Nullable CandyBase base() {
		return base;
	}

	public int sugar() {
		return sugar;
	}

	public List<CandyFlavour> flavours() {
		return List.copyOf(flavours);
	}

	public int temperature() {
		return temperature;
	}

	/** The hottest the batch has been: what sets its stage. */
	public int cooked() {
		return cooked;
	}

	public boolean empty() {
		return base == null && sugar == 0 && flavours.isEmpty() && dyes == 0;
	}

	/** A batch that can cook: a base and sugar. */
	public boolean batch() {
		return base != null && sugar > 0;
	}

	public CandyStage stage() {
		return CandyStage.at(cooked);
	}

	/** The candy it would make if poured now, or null if it is not ready (or holds nothing to pour). */
	public @Nullable CandyKind makes() {
		return batch() ? base.makes(stage()) : null;
	}

	public boolean dyed() {
		return dyes > 0;
	}

	/**
	 * The colour of the candy it pours: its dyes mixed, as dyes mix on leather, or else its first flavour's colour, or
	 * {@link Candies#NATURAL}.
	 */
	public int color() {
		if (dyes == 0) {
			return flavours.isEmpty() ? Candies.NATURAL : flavours.get(0).color;
		}
		int r = red / dyes;
		int g = green / dyes;
		int b = blue / dyes;
		int max = Math.max(r, Math.max(g, b));
		float scale = max == 0 ? 1.0F : (float) (peak / dyes) / max;
		return (Math.round(r * scale) << 16) | (Math.round(g * scale) << 8) | Math.round(b * scale);
	}

	/** Whether things can still be added: it is not yet near the boil. */
	public boolean cool() {
		return temperature < ADD_BELOW;
	}

	public boolean setBase(CandyBase added) {
		if (base != null) {
			return false;
		}
		base = added;
		changed();
		return true;
	}

	public boolean addSugar() {
		if (sugar >= MAX_SUGAR) {
			return false;
		}
		sugar++;
		changed();
		return true;
	}

	/** Whether {@code flavour} can go in: not already there, and room for another. */
	boolean canFlavour(CandyFlavour flavour) {
		return !flavours.contains(flavour) && flavours.size() < MAX_FLAVOURS;
	}

	public void addFlavour(CandyFlavour flavour) {
		flavours.add(flavour);
		changed();
	}

	public void addDye(DyeColor dye) {
		int rgb = dye.getTextureDiffuseColor();
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		red += r;
		green += g;
		blue += b;
		peak += Math.max(r, Math.max(g, b));
		dyes++;
		changed();
	}

	/** Empties the kettle (poured or tipped out); it stays as hot as it is. */
	void clear() {
		base = null;
		sugar = 0;
		flavours.clear();
		red = green = blue = peak = dyes = 0;
		cooked = temperature;
		step = 0;
		changed();
	}

	/** Sets the temperature at once, as if it had been heated (or cooled) to it: for tests and for commands. */
	public void setTemperature(int degrees) {
		temperature = Math.clamp(degrees, ROOM, MAX_TEMP);
		cooked = batch() ? Math.max(cooked, temperature) : temperature;
		changed();
	}

	/** Ticks a degree takes over heat at {@code degrees}: slow while its water boils off. */
	public static int ticksPerDegree(int degrees) {
		return degrees < BOIL ? HEAT_TICKS : degrees < BOILED ? BOIL_TICKS : COOK_TICKS;
	}

	void serverTick(ServerLevel level) {
		boolean heated = CookingPotBlockEntity.isHeated(level, worldPosition);
		int target;
		int every;
		if (heated && batch()) {
			target = MAX_TEMP;
			every = ticksPerDegree(temperature);
		} else {
			target = ROOM;
			every = COOL_TICKS;
		}
		if (temperature == target) {
			step = 0;
			return;
		}
		if (++step < every) {
			return;
		}
		step = 0;
		CandyStage before = stage();
		temperature += temperature < target ? 1 : -1;
		if (batch()) {
			cooked = Math.max(cooked, temperature);
		} else {
			cooked = temperature;
		}
		CandyStage after = stage();
		if (batch() && after != before) {
			// A bell rings a note higher at each stage; burnt sugar hisses instead.
			if (after == CandyStage.BURNT) {
				level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
			} else {
				level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.7F,
						(float) Math.pow(2.0, (after.ordinal() * 2 - 8) / 12.0));
			}
		}
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		base = input.read("base", CandyBase.CODEC).orElse(null);
		sugar = Math.clamp(input.getIntOr("sugar", 0), 0, MAX_SUGAR);
		flavours.clear();
		input.read("flavours", CandyFlavour.CODEC.listOf()).ifPresent(list -> list.stream().distinct().limit(MAX_FLAVOURS).forEach(flavours::add));
		red = input.getIntOr("red", 0);
		green = input.getIntOr("green", 0);
		blue = input.getIntOr("blue", 0);
		peak = input.getIntOr("peak", 0);
		dyes = Math.max(0, input.getIntOr("dyes", 0));
		temperature = Math.clamp(input.getIntOr("temperature", ROOM), ROOM, MAX_TEMP);
		cooked = Math.clamp(input.getIntOr("cooked", temperature), temperature, MAX_TEMP);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (base != null) {
			output.store("base", CandyBase.CODEC, base);
		}
		output.putInt("sugar", sugar);
		output.store("flavours", CandyFlavour.CODEC.listOf(), List.copyOf(flavours));
		output.putInt("red", red);
		output.putInt("green", green);
		output.putInt("blue", blue);
		output.putInt("peak", peak);
		output.putInt("dyes", dyes);
		output.putInt("temperature", temperature);
		output.putInt("cooked", cooked);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
