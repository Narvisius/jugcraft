package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Hearth Oven's fire and its pie. It burns what generators burn, as long ({@link GeneratorFuels}: coal, charcoal, coke),
 * and logs ({@code jugcraft:hearth_oven_wood}), {@value #WOOD_BURN} ticks each, as in a furnace; up to {@value #MAX_BURN}
 * ticks banked.
 * While it burns the oven heats a degree every {@value #HEAT_TICKS} ticks to {@value #MAX_HEAT}; out, it cools a degree
 * every {@value #COOL_TICKS}. A pie bakes only while the oven is at {@value #BAKE_HEAT} or hotter: a point a tick, two when
 * it is at {@value #MAX_HEAT}. At {@value #BAKED} points it is baked; left in until {@value #BURNT} it burns. Clients get
 * the pie and how far it is baked (sent each time it passes another tenth), for drawing it.
 */
public class HearthOvenBlockEntity extends BlockEntity {
	public static final int MAX_BURN = 3200;
	public static final int WOOD_BURN = 300;
	public static final TagKey<Item> WOOD = TagKey.create(Registries.ITEM, Jugcraft.id("hearth_oven_wood"));
	public static final int MAX_HEAT = 100;
	public static final int HEAT_TICKS = 2;
	public static final int COOL_TICKS = 4;
	public static final int BAKE_HEAT = 50;
	public static final int BAKED = 600;
	public static final int BURNT = 1200;

	private int burn;
	private int heat;
	private @Nullable PieFilling pie;
	private int baked;

	public HearthOvenBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.HEARTH_OVEN_ENTITY, pos, state);
	}

	public int burn() {
		return burn;
	}

	public int heat() {
		return heat;
	}

	public @Nullable PieFilling pie() {
		return pie;
	}

	public int baked() {
		return baked;
	}

	/** Sets the oven's fire, heat, pie and baking at once (tests and screenshots). */
	public void set(int burnTicks, int degrees, @Nullable PieFilling filling, int points) {
		burn = Math.clamp(burnTicks, 0, MAX_BURN);
		heat = Math.clamp(degrees, 0, MAX_HEAT);
		pie = filling;
		baked = filling == null ? 0 : Math.clamp(points, 0, BURNT);
		changed(true);
	}

	/** The filling of the raw pie {@code stack} is, or null. */
	public static @Nullable PieFilling rawFilling(ItemStack stack) {
		return stack.isEmpty() ? null : PieFilling.ofRaw(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
	}

	/** How long one of {@code stack} burns in the oven, in ticks; 0 if it isn't fuel. */
	public static int burnTicks(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		return stack.is(WOOD) ? WOOD_BURN : GeneratorFuels.burnTicks(stack);
	}

	public static boolean isFuel(ItemStack stack) {
		return burnTicks(stack) > 0;
	}

	/** Puts one of {@code fuel} on the fire, if there is room for its burn; returns whether it did. */
	public boolean feed(ItemStack fuel, Player player) {
		if (level == null) {
			return false;
		}
		int duration = burnTicks(fuel);
		if (duration <= 0 || burn + duration > MAX_BURN) {
			message(player, "full");
			return false;
		}
		burn += duration;
		fuel.consume(1, player);
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
		changed(true);
		return true;
	}

	/** Puts the raw pie {@code stack} in, if the oven is empty; returns whether it did. */
	public boolean putIn(ItemStack stack, Player player) {
		PieFilling filling = rawFilling(stack);
		if (filling == null || pie != null || level == null) {
			message(player, "occupied");
			return false;
		}
		pie = filling;
		baked = 0;
		stack.consume(1, player);
		level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
		changed(true);
		return true;
	}

	/**
	 * Takes the pie out to {@code player}: baked, raw (as it went in) or burnt. With no pie, says how hot it is. Returns
	 * what came out.
	 */
	public ItemStack takeOut(Player player) {
		if (pie == null || level == null) {
			if (player instanceof ServerPlayer server) {
				server.sendOverlayMessage(Component.translatable("message.jugcraft.hearth_oven.status", heat, burn / 20));
			}
			return ItemStack.EMPTY;
		}
		ItemStack out;
		if (baked >= BURNT) {
			out = new ItemStack(JugcraftAgriculture.item("burnt_pie"));
		} else if (baked >= BAKED) {
			out = new ItemStack(JugcraftAgriculture.item(pie.pie()));
			if (player instanceof ServerPlayer server) {
				TrickOrTreat.award(server, "as_easy_as_pie");
			}
		} else {
			out = new ItemStack(JugcraftAgriculture.item(pie.rawPie()));
		}
		pie = null;
		baked = 0;
		if (!player.getInventory().add(out.copy())) {
			Block.popResource(level, worldPosition, out.copy());
		}
		level.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.2F);
		changed(true);
		return out;
	}

	/** Comparators: 0 empty; 1 to 13 baking; 15 baked; 1 burnt. */
	public int signal() {
		if (pie == null) {
			return 0;
		}
		if (baked >= BURNT) {
			return 1;
		}
		return baked >= BAKED ? 15 : 1 + 12 * baked / BAKED;
	}

	void serverTick(ServerLevel level) {
		long time = level.getGameTime();
		boolean was = burn > 0;
		if (burn > 0) {
			burn--;
			if (heat < MAX_HEAT && time % HEAT_TICKS == 0) {
				heat++;
			}
		} else if (heat > 0 && time % COOL_TICKS == 0) {
			heat--;
		}
		int tenth = baked * 10 / BAKED;
		if (pie != null && heat >= BAKE_HEAT && baked < BURNT) {
			baked = Math.min(BURNT, baked + (heat >= MAX_HEAT ? 2 : 1));
			if (baked >= BURNT) {
				level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
			} else if (baked >= BAKED && baked - (heat >= MAX_HEAT ? 2 : 1) < BAKED) {
				level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
			}
		}
		boolean lit = burn > 0;
		BlockState state = getBlockState();
		if (state.hasProperty(HearthOvenBlock.LIT) && state.getValue(HearthOvenBlock.LIT) != lit) {
			level.setBlock(worldPosition, state.setValue(HearthOvenBlock.LIT, lit), Block.UPDATE_ALL);
		}
		if (was != lit || baked * 10 / BAKED != tenth) {
			changed(true);
		} else if (time % 20 == 0) {
			changed(false);
		}
	}

	private static void message(Player player, String key) {
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.hearth_oven." + key));
		}
	}

	private void changed(boolean send) {
		setChanged();
		if (level != null && send) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	/** Broken, it drops its pie as it is. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server && pie != null) {
			String item = baked >= BURNT ? "burnt_pie" : baked >= BAKED ? pie.pie() : pie.rawPie();
			Block.popResource(server, pos, new ItemStack(JugcraftAgriculture.item(item)));
			pie = null;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		burn = Math.clamp(input.getIntOr("burn", 0), 0, MAX_BURN);
		heat = Math.clamp(input.getIntOr("heat", 0), 0, MAX_HEAT);
		int filling = input.getIntOr("pie", -1);
		pie = filling >= 0 && filling < PieFilling.values().length ? PieFilling.values()[filling] : null;
		baked = Math.clamp(input.getIntOr("baked", 0), 0, BURNT);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("burn", burn);
		output.putInt("heat", heat);
		output.putInt("pie", pie == null ? -1 : pie.ordinal());
		output.putInt("baked", baked);
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
