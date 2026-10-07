package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The wax in a Wax Melting Pot: one wax at a time, up to {@value #CAPACITY} measures, some set and some molten. Over a
 * heat source ({@link CookingPotBlockEntity#isHeated}) a set measure melts every {@value #MELT_TICKS} ticks; without
 * heat a molten one sets again every {@value #SET_TICKS}. Its mixture (dyes mixed as leather dyes mix, up to
 * {@value #MAX_SCENTS} scents, a brightener, an extender) lasts until the pot is empty. Saved, and sent to clients for
 * the wax to show.
 */
public class WaxPotBlockEntity extends BlockEntity {
	public static final int CAPACITY = 8;
	public static final int MELT_TICKS = 100;
	public static final int SET_TICKS = 300;
	public static final int COOL_TICKS = 40;
	public static final int MAX_SCENTS = 2;
	public static final float BRIGHT_BURN = 0.5F;
	public static final float LONG_BURN = 1.5F;
	/** Stirred in, they make the aura stronger, or the candle burn longer. */
	public static final TagKey<Item> BRIGHTENERS = TagKey.create(Registries.ITEM, Jugcraft.id("candle_brighteners"));
	public static final TagKey<Item> EXTENDERS = TagKey.create(Registries.ITEM, Jugcraft.id("candle_extenders"));

	private @Nullable CandleWax wax;
	private int solid;
	private int molten;
	private int progress;
	private int red;
	private int green;
	private int blue;
	private int peak;
	private int dyes;
	private final List<CandleScent> scents = new ArrayList<>();
	private boolean bright;
	private boolean lasting;

	public WaxPotBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.WAX_POT_ENTITY, pos, state);
	}

	public @Nullable CandleWax wax() {
		return wax;
	}

	public int molten() {
		return molten;
	}

	public int total() {
		return solid + molten;
	}

	public List<CandleScent> scents() {
		return List.copyOf(scents);
	}

	public boolean bright() {
		return bright;
	}

	public boolean lasting() {
		return lasting;
	}

	/** The wax's colour: its dyes mixed, as dyes mix on leather, or its natural colour. */
	public int color() {
		if (dyes == 0) {
			return wax == null ? 0xFFFFFF : wax.color;
		}
		int r = red / dyes;
		int g = green / dyes;
		int b = blue / dyes;
		int max = Math.max(r, Math.max(g, b));
		float scale = max == 0 ? 1.0F : (float) (peak / dyes) / max;
		return (Math.round(r * scale) << 16) | (Math.round(g * scale) << 8) | Math.round(b * scale);
	}

	/** Adds a measure-giving item of {@code kind}: refused if the pot holds another wax or has no room. */
	public boolean addWax(CandleWax kind) {
		if (wax != null && wax != kind || total() + kind.measures > CAPACITY) {
			return false;
		}
		wax = kind;
		solid += kind.measures;
		changed();
		return true;
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

	/** Whether {@code scent} can go in: not already there, and room for another. */
	boolean canScent(CandleScent scent) {
		return !scents.contains(scent) && scents.size() < MAX_SCENTS;
	}

	public void addScent(CandleScent scent) {
		scents.add(scent);
		changed();
	}

	void brighten() {
		bright = true;
		changed();
	}

	void extend() {
		lasting = true;
		changed();
	}

	/** Uses a molten measure (a dip); the mixture stays while any wax is left. */
	void useMeasure() {
		molten--;
		if (total() == 0) {
			empty();
		} else {
			changed();
		}
	}

	/** Pours everything away. */
	void empty() {
		wax = null;
		solid = 0;
		molten = 0;
		progress = 0;
		red = green = blue = peak = dyes = 0;
		scents.clear();
		bright = false;
		lasting = false;
		changed();
	}

	/** Melts whatever is set in the pot at once (a pot left long over the fire). */
	public void meltAll() {
		molten += solid;
		solid = 0;
		progress = 0;
		changed();
	}

	void serverTick(ServerLevel level) {
		if (wax == null) {
			return;
		}
		boolean heated = CookingPotBlockEntity.isHeated(level, worldPosition);
		if (heated && solid > 0) {
			if (++progress >= MELT_TICKS) {
				progress = 0;
				solid--;
				molten++;
				changed();
			}
		} else if (!heated && molten > 0) {
			if (++progress >= SET_TICKS) {
				progress = 0;
				molten--;
				solid++;
				changed();
			}
		} else {
			progress = 0;
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		wax = input.read("wax", CandleWax.CODEC).orElse(null);
		solid = input.getIntOr("solid", 0);
		molten = input.getIntOr("molten", 0);
		progress = input.getIntOr("progress", 0);
		red = input.getIntOr("red", 0);
		green = input.getIntOr("green", 0);
		blue = input.getIntOr("blue", 0);
		peak = input.getIntOr("peak", 0);
		dyes = input.getIntOr("dyes", 0);
		scents.clear();
		input.read("scents", CandleScent.CODEC.listOf()).ifPresent(list -> list.stream().distinct().limit(MAX_SCENTS).forEach(scents::add));
		bright = input.getBooleanOr("bright", false);
		lasting = input.getBooleanOr("lasting", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (wax != null) {
			output.store("wax", CandleWax.CODEC, wax);
		}
		output.putInt("solid", solid);
		output.putInt("molten", molten);
		output.putInt("progress", progress);
		output.putInt("red", red);
		output.putInt("green", green);
		output.putInt("blue", blue);
		output.putInt("peak", peak);
		output.putInt("dyes", dyes);
		output.store("scents", CandleScent.CODEC.listOf(), List.copyOf(scents));
		output.putBoolean("bright", bright);
		output.putBoolean("lasting", lasting);
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
