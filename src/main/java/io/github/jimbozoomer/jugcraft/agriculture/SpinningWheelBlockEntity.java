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
 * so it is drawn spinning ({@link #spin}). While a Cursed Spindle's gate to the Spindle Loft is open here (lair/SpindleRite)
 * the wheel spins wild, {@value #WILD_SPEED} times as fast with nobody at the treadle; that is told to clients but never
 * saved, as a restart shuts the gate.
 */
public class SpinningWheelBlockEntity extends BlockEntity {
    public final local.peepo.CompanionRecipeFilter companionFilter = new local.peepo.CompanionRecipeFilter(java.util.List.of(net.minecraft.core.component.DataComponents.DYED_COLOR));
	public static final int TURNS = 4;
	/** How long (ticks) the wheel runs on after a turn of the treadle. */
	public static final int SPIN_TICKS = 20;
	public static final int UNRAVEL_LOSS = 1;
	/** How many times faster than the treadle drives it the wheel spins while it is a gate. */
	public static final float WILD_SPEED = 3.0F;

    /** One saved batch: companions never scatter yarn or overwrite another colour. */
    private ItemStack companionYarn = ItemStack.EMPTY;
    public final local.peepo.SpinningWheelJob companionJob = new local.peepo.SpinningWheelJob(this);
    private int companionTicks;
    private boolean companionBatch;
    private long lastCompanionTick = Long.MIN_VALUE;
    private record TransferState(DyeColor wool, int turns, int ticks, boolean managed, ItemStack yarn) {}
    private final net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<TransferState> transfers =
        new net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<>() {
            protected TransferState createSnapshot() { return new TransferState(wool, turns, companionTicks, companionBatch, companionYarn.copy()); }
            protected void readSnapshot(TransferState s) { wool=s.wool(); turns=s.turns(); companionTicks=s.ticks(); companionBatch=s.managed(); companionYarn=s.yarn(); }
            protected void onFinalCommit() { changed(); }
        };
    private final net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> companionInput =
        new net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage<>() {
            public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item, long amount,
                    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx) {
                net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions.notBlankNotNegative(item, amount);
                if(amount==0 || companionNeed(item.toStack(1))==0)return 0;
                transfers.updateSnapshots(tx); wool=Knitting.woolColor(item.toStack(1)); turns=0; companionTicks=0; companionBatch=true; return 1;
            }
        };
    private final net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> companionOutput =
        new net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage<>() {
            public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item,long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return 0;}
            public boolean supportsInsertion(){return false;}
            public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item,long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){
                net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions.notBlankNotNegative(item,amount);
                if(isRemoved() || companionYarn.isEmpty() || !item.equals(getResource()))return 0;
                int count=(int)Math.min(amount,companionYarn.getCount());
                if(count>0){transfers.updateSnapshots(tx);companionYarn.shrink(count);}return count;
            }
            public boolean isResourceBlank(){return companionYarn.isEmpty();}
            public net.fabricmc.fabric.api.transfer.v1.item.ItemVariant getResource(){return net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(companionYarn);}
            public long getAmount(){return companionYarn.getCount();}
            public long getCapacity(){return Knitting.YARN_PER_WOOL;}
        };
    public int companionNeed(ItemStack item){
        var color=Knitting.woolColor(item);
        return !isRemoved() && level!=null && !isWild(level.getGameTime()) && wool==null && companionYarn.isEmpty() && color!=null && companionFilter.allows(Knitting.yarn(color.getTextureDiffuseColor(),1))?1:0;
    }
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> companionInputs(){return companionInput;}
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> companionOutputs(){return companionOutput;}
    public local.peepo.CompanionStatus companionStatus(){
        if(isRemoved() || level==null)return local.peepo.CompanionStatus.MISSING;
        if(isWild(level.getGameTime()))return local.peepo.CompanionStatus.OCCUPIED;
        if(!companionYarn.isEmpty())return local.peepo.CompanionStatus.FULL;
        return wool==null?local.peepo.CompanionStatus.NO_INPUT:local.peepo.CompanionStatus.READY;
    }
    public local.peepo.CompanionStatus assist(local.peepo.PeepoEntity npc){
        if(!(level instanceof ServerLevel server) || !companionJob.isOccupant(npc) || !local.peepo.CompanionJobs.permitted(npc,worldPosition))return local.peepo.CompanionStatus.FORBIDDEN;
        var status=companionStatus();if(status!=local.peepo.CompanionStatus.READY)return status;
        long now=level.getGameTime();if(now==lastCompanionTick)return local.peepo.CompanionStatus.WORKING;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            if(npc.extractEnergy(4,tx)!=4)return local.peepo.CompanionStatus.RECOVERING;
            tx.commit();
        }
        lastCompanionTick=now;companionBatch=true;
        if(++companionTicks>=SPIN_TICKS){companionTicks=0;turn(server,true);}
        return local.peepo.CompanionStatus.WORKING;
    }
    public void resetCompanionStroke(){companionTicks=0;}
    /** Shift-empty-hand recovery, without requiring a companion or losing components. */
    public boolean collectCompanionOutput(Player player){
        if(companionYarn.isEmpty())return false;
        player.getInventory().add(companionYarn);changed();return true;
    }
    @Override public void setRemoved(){companionJob.removed();super.setRemoved();}

	private @Nullable DyeColor wool;
	private int turns;
	/** Ticks the wheel had spun before its latest turn, and the game time of that turn (for drawing it). */
	private long spun;
	private long turnedAt = Long.MIN_VALUE / 2;
	/** The game times the wheel began to spin wild and stops (never saved). */
	private long wildFrom = Long.MIN_VALUE / 2;
	private long wildUntil = Long.MIN_VALUE / 2;

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
        companionTicks=0;companionBatch=false;
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
		return spun + since + wildSpin(time, partialTick);
	}

	/** The ticks of spinning the wheel's wild spell has added by {@code time}. */
	private float wildSpin(long time, float partialTick) {
		long end = Math.min(time, wildUntil);
		if (end < wildFrom) {
			return 0.0F;
		}
		return ((end - wildFrom) + (time < wildUntil ? partialTick : 0.0F)) * WILD_SPEED;
	}

	/** Whether the wheel is spinning wild at {@code time}. */
	public boolean isWild(long time) {
		return time >= wildFrom && time < wildUntil;
	}

	/** Sets the wheel spinning wild from {@code now} for {@code ticks} (a Cursed Spindle's gate is open here). */
	public void spinWild(long now, int ticks) {
		spun += (long) wildSpin(now, 0.0F);
		wildFrom = now;
		wildUntil = now + ticks;
		changed();
	}

	/** The gate here has shut early: the wheel stops spinning wild. */
	public void calm() {
		if (level != null && isWild(level.getGameTime())) {
			wildUntil = level.getGameTime();
			changed();
		}
	}

	/**
	 * One turn of the treadle: spins the skein on, and on the last turn sets out its yarn in front of the wheel. Returns
	 * false (and does nothing) with no wool on the distaff.
	 */
	public boolean turn(ServerLevel level) { return turn(level,companionBatch); }

    private boolean turn(ServerLevel level, boolean collect) {
		if (wool == null || collect && (!companionYarn.isEmpty() || isWild(level.getGameTime()))) {
			return false;
		}
        companionTicks=0;
		long now = level.getGameTime();
		spun += Math.min(SPIN_TICKS, Math.max(0L, now - turnedAt));
		turnedAt = now;
		turns++;
		level.playSound(null, worldPosition, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.7F, 0.8F + 0.1F * turns);
		if (turns >= TURNS) {
			Direction front = getBlockState().hasProperty(SpinningWheelBlock.FACING) ? getBlockState().getValue(SpinningWheelBlock.FACING)
					: Direction.NORTH;
			ItemStack yarn=Knitting.yarn(wool.getTextureDiffuseColor(), Knitting.YARN_PER_WOOL);
            if(collect)companionYarn=yarn;else Block.popResourceFromFace(level, worldPosition, front, yarn);
			level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F, 1.0F);
			level.gameEvent(null, GameEvent.BLOCK_CHANGE, worldPosition);
			wool = null;
            companionBatch=false;
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
        if(level instanceof ServerLevel server && !companionYarn.isEmpty()){Block.popResource(server,pos,companionYarn);companionYarn=ItemStack.EMPTY;}
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
        local.peepo.CompanionReadiness.invalidate(this);
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
        companionFilter.load(input);
        companionYarn=input.read("CompanionYarn",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        companionTicks=0;companionBatch=input.getBooleanOr("CompanionBatch",false);
		int color = input.getIntOr("wool", -1);
		wool = color >= 0 && color < DyeColor.values().length ? DyeColor.values()[color] : null;
		turns = Math.clamp(input.getIntOr("turns", 0), 0, TURNS - 1);
		spun = input.getLongOr("spun", 0L);
		turnedAt = input.getLongOr("turned_at", Long.MIN_VALUE / 2);
		// Only in the update clients are sent: a saved wheel is never wild.
		wildFrom = input.getLongOr("wild_from", Long.MIN_VALUE / 2);
		wildUntil = input.getLongOr("wild_until", Long.MIN_VALUE / 2);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
        companionFilter.save(output);
        output.store("CompanionYarn",ItemStack.OPTIONAL_CODEC,companionYarn);
        output.putBoolean("CompanionBatch",companionBatch);
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
		CompoundTag tag = saveCustomOnly(registries);
		tag.putLong("wild_from", wildFrom);
		tag.putLong("wild_until", wildUntil);
		return tag;
	}
}
