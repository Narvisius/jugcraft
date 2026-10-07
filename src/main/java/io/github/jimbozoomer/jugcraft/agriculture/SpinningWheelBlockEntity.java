package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Spinning Wheel's skein of wool and how far it is spun. Each turn of the treadle spins it on; after
 * {@value #TURNS} turns the skein is {@value Knitting#YARN_PER_WOOL} balls of yarn in the wool's colour, set out in front of
 * the wheel (where a hopper can take them). Knitwear used on it is unravelled into a ball of yarn a row, less one
 * ({@value #UNRAVEL_LOSS}) for the yarn too worn to use again. Clients get the skein's colour and when the wheel last turned,
 * so it is drawn spinning ({@link #spin}).
 */
public class SpinningWheelBlockEntity extends BlockEntity {
	public static final int TURNS = 4;
	/** How long (ticks) the wheel runs on after a turn of the treadle. */
	public static final int SPIN_TICKS = 20;
	public static final int UNRAVEL_LOSS = 1;

	private @Nullable DyeColor wool;
	private int turns;
	/** Ticks the wheel had spun before its latest turn, and the game time of that turn (for drawing it). */
	private long spun;
	private long turnedAt = Long.MIN_VALUE / 2;

	public SpinningWheelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SPINNING_WHEEL_ENTITY, pos, state);
	}

	public @Nullable DyeColor wool() {
		return wool;
	}

	public int turns() {
		return turns;
	}

	/** Puts a skein of {@code color} wool on the distaff, if there is none; returns whether it did. */
	public boolean loadWool(DyeColor color) {
		if (wool != null) {
			return false;
		}
		wool = color;
		turns = 0;
		if (level != null) {
			level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		changed();
		return true;
	}

	/** How far round the wheel is at {@code time} (ticks of spinning, fractional), for drawing it. */
	public float spin(long time, float partialTick) {
		float since = Math.min(SPIN_TICKS, Math.max(0.0F, time - turnedAt + partialTick));
		return spun + since;
	}

	/**
	 * One turn of the treadle: spins the skein on, and on the last turn sets out its yarn in front of the wheel. Returns
	 * false (and does nothing) with no wool on the distaff.
	 */
	public boolean turn(ServerLevel level) {
		if (wool == null) {
			return false;
		}
		long now = level.getGameTime();
		spun += Math.min(SPIN_TICKS, Math.max(0L, now - turnedAt));
		turnedAt = now;
		turns++;
		level.playSound(null, worldPosition, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.7F, 0.8F + 0.1F * turns);
		if (turns >= TURNS) {
			Direction front = getBlockState().hasProperty(SpinningWheelBlock.FACING) ? getBlockState().getValue(SpinningWheelBlock.FACING)
					: Direction.NORTH;
			Block.popResourceFromFace(level, worldPosition, front, Knitting.yarn(wool.getTextureDiffuseColor(), Knitting.YARN_PER_WOOL));
			level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 1.0F);
			level.gameEvent(null, GameEvent.BLOCK_CHANGE, worldPosition);
			wool = null;
			turns = 0;
		}
		changed();
		return true;
	}

	/** Unravels {@code garment} (taken from {@code player}) into its yarn, a ball a row less {@value #UNRAVEL_LOSS}. */
	public int unravel(ServerLevel level, ItemStack garment, Player player) {
		Knitwear knit = Knitwear.of(garment.getItem());
		if (knit == null) {
			return 0;
		}
		int balls = knit.rows - UNRAVEL_LOSS;
		ItemStack yarn = Knitting.yarn(Knitting.color(garment), balls);
		garment.consume(1, player);
		if (!player.getInventory().add(yarn)) {
			Block.popResource(level, worldPosition.above(), yarn);
		}
		level.playSound(null, worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
		if (player instanceof ServerPlayer server) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.spinning_wheel.unravelled", balls));
		}
		return balls;
	}

	/** Broken, it drops the skein (as the wool it was). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server && wool != null) {
			ItemStack skein = woolItem(wool);
			if (!skein.isEmpty()) {
				Block.popResource(server, pos, skein);
			}
			wool = null;
		}
	}

	private static ItemStack woolItem(DyeColor color) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + "_wool")));
	}

	private void changed() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		int color = input.getIntOr("wool", -1);
		wool = color >= 0 && color < DyeColor.values().length ? DyeColor.values()[color] : null;
		turns = Math.clamp(input.getIntOr("turns", 0), 0, TURNS - 1);
		spun = input.getLongOr("spun", 0L);
		turnedAt = input.getLongOr("turned_at", Long.MIN_VALUE / 2);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("wool", wool == null ? -1 : wool.ordinal());
		output.putInt("turns", turns);
		output.putLong("spun", spun);
		output.putLong("turned_at", turnedAt);
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
