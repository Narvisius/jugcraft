package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What is in a Cider Press: whole apples in its hopper, ground pulp in its basket (the "cheese"), how far its screw is
 * down on the cheese being pressed, and the juice in its trough. The crank grinds one apple at a time into pulp; the
 * screw presses a cheese in {@value #TURNS} turns, each letting out its share of the juice (one serving an apple); the
 * last turn knocks out the spent pomace (one for every {@value #APPLES_PER_POMACE} apples, rounded up). Work is paced:
 * the crank and the screw each move once every {@value #WORK_TICKS} ticks, however many players work them. Saved, and
 * sent to clients for the renderer.
 */
public class CiderPressBlockEntity extends BlockEntity {
	/** Apples and pulp together, and the juice the trough holds, in apples (servings). */
	public static final int CAPACITY = 8;
	public static final int TROUGH = 8;
	public static final int TURNS = 4;
	public static final int WORK_TICKS = 8;
	public static final int APPLES_PER_POMACE = 2;
	/** Small companion output trays; no per-tick inventory scanning or loose pomace entities. */
	public static final int OUTPUT_CAPACITY = 8;

	private int apples;
	private int pulp;
	private int pressing;
	private int turns;
	private int juice;
	private int bottled;
	private int pomaceStored;
	private final TransferState transfers = new TransferState();
	private final Storage<ItemVariant> companionInputs = new Storage<>() {
		@Override public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
			if (resource.isBlank() || maxAmount <= 0 || isRemoved()) return 0;
			ItemStack stack = resource.toStack(1);
			int count = (int) Math.min(maxAmount, companionNeed(stack));
			if (count <= 0) return 0;
			transfers.updateSnapshots(tx);
			if (stack.is(Items.GLASS_BOTTLE)) { juice -= count; bottled += count; }
			else apples += count;
			return count;
		}
		@Override public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) { return 0; }
		@Override public boolean supportsExtraction() { return false; }
		@Override public Iterator<StorageView<ItemVariant>> iterator() { return Collections.emptyIterator(); }
	};
	private final Storage<ItemVariant> companionOutputs = new CombinedStorage<>(List.of(output(true), output(false)));
	/** When the crank or screw last moved (game time); never, at first. */
	private long lastWork = -WORK_TICKS;
	public final local.peepo.CiderPressJob companionJob=new local.peepo.CiderPressJob(this);
	public local.peepo.CompanionStatus companionStatus(){
		if(apples<=0 && pulp<=0)return local.peepo.CompanionStatus.NO_INPUT;
		if(juice>=TROUGH || (apples<=0 || pressing()) && juice+nextRelease()>TROUGH)return local.peepo.CompanionStatus.FULL;
		if(pomaceStored + (pulp + APPLES_PER_POMACE - 1) / APPLES_PER_POMACE > OUTPUT_CAPACITY)return local.peepo.CompanionStatus.FULL;
		return local.peepo.CompanionStatus.READY;
	}
	public local.peepo.CompanionStatus assist(local.peepo.PeepoEntity npc){
		if(!companionJob.isOccupant(npc) || !local.peepo.CompanionJobs.permitted(npc,worldPosition))return local.peepo.CompanionStatus.FORBIDDEN;
		var status=companionStatus();if(status!=local.peepo.CompanionStatus.READY)return status;
		long now=level.getGameTime();if(!ready(now))return local.peepo.CompanionStatus.WORKING;
		try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
			if(npc.extractEnergy(64,tx)<=0)return local.peepo.CompanionStatus.RECOVERING;
			tx.commit();
		}
		if(apples>0 && !pressing())grind(now);
		else {
			int pomace=turn(now);
			if(pomace>0){pomaceStored+=pomace;changed();}
		}
		return local.peepo.CompanionStatus.WORKING;
	}
	@Override public void setRemoved(){companionJob.removed();super.setRemoved();}

	public CiderPressBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CIDER_PRESS_ENTITY, pos, state);
	}

	/** Bottle existing juice before fetching another batch. Pending output must leave first. */
	public int companionNeed(ItemStack stack) {
		if (stack.is(Items.GLASS_BOTTLE)) return Math.min(juice, OUTPUT_CAPACITY - bottled);
		if (stack.is(CiderPressBlock.APPLES) && !pressing() && juice == 0 && bottled == 0 && pomaceStored == 0)
			return CAPACITY - apples - pulp;
		return 0;
	}
	public Storage<ItemVariant> companionInputs() { return companionInputs; }
	public Storage<ItemVariant> companionOutputs() { return companionOutputs; }

	/** Both sides of a transport operation roll back together, including bottle-to-cider conversion. */
	private final class TransferState extends SnapshotParticipant<int[]> {
		@Override protected int[] createSnapshot() { return new int[] {apples, juice, bottled, pomaceStored}; }
		@Override protected void readSnapshot(int[] s) { apples=s[0]; juice=s[1]; bottled=s[2]; pomaceStored=s[3]; }
		@Override protected void onFinalCommit() { changed(); }
	}
	private SingleSlotStorage<ItemVariant> output(boolean cider) {
		return new SingleSlotStorage<>() {
			private ItemVariant variant() { return ItemVariant.of(JugcraftAgriculture.item(cider ? "sweet_cider" : "apple_pomace")); }
			@Override public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) { return 0; }
			@Override public boolean supportsInsertion() { return false; }
			@Override public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
				if (maxAmount <= 0 || isRemoved() || !resource.equals(variant())) return 0;
				int count = (int) Math.min(maxAmount, getAmount());
				if (count > 0) { transfers.updateSnapshots(tx); if (cider) bottled-=count; else pomaceStored-=count; }
				return count;
			}
			@Override public boolean isResourceBlank() { return getAmount() == 0; }
			@Override public ItemVariant getResource() { return isResourceBlank() ? ItemVariant.blank() : variant(); }
			@Override public long getAmount() { return cider ? bottled : pomaceStored; }
			@Override public long getCapacity() { return OUTPUT_CAPACITY; }
		};
	}

	/** Shift-empty-hand recovery works without a helper. Leave anything that does not fit in the tray. */
	public void collectCompanionOutput(Player player) {
		for (boolean cider : new boolean[] {true, false}) {
			int count = cider ? bottled : pomaceStored;
			if (count == 0) continue;
			ItemStack stack = new ItemStack(JugcraftAgriculture.item(cider ? "sweet_cider" : "apple_pomace"), count);
			player.getInventory().add(stack);
			if (cider) bottled = stack.getCount(); else pomaceStored = stack.getCount();
		}
		changed();
	}
	@Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			if (bottled > 0) Block.popResource(level, pos, new ItemStack(JugcraftAgriculture.item("sweet_cider"), bottled));
			if (pomaceStored > 0) Block.popResource(level, pos, new ItemStack(JugcraftAgriculture.item("apple_pomace"), pomaceStored));
			bottled = pomaceStored = 0;
		}
		super.preRemoveSideEffects(pos, state);
	}

	public int apples() {
		return apples;
	}

	public int pulp() {
		return pulp;
	}

	public int turns() {
		return turns;
	}

	public int juice() {
		return juice;
	}

	/** Whether the screw is down on a cheese: nothing goes in until it is pressed out. */
	public boolean pressing() {
		return turns > 0;
	}

	/** Whether another apple fits in the hopper. */
	boolean hasRoom() {
		return !pressing() && apples + pulp < CAPACITY;
	}

	public void addApple() {
		apples++;
		changed();
	}

	/** Whether the crank or screw may move again at {@code gameTime} (the work is paced, not the player). */
	boolean ready(long gameTime) {
		return gameTime - lastWork >= WORK_TICKS;
	}

	/** One turn of the crank: an apple from the hopper is ground into the basket. */
	public void grind(long gameTime) {
		apples--;
		pulp++;
		lastWork = gameTime;
		changed();
	}

	/** The juice the next turn of the screw lets out. */
	public int nextRelease() {
		int size = pressing() ? pressing : pulp;
		return size * (turns + 1) / TURNS - size * turns / TURNS;
	}

	/**
	 * One turn of the screw: its share of the cheese's juice runs into the trough. Returns the pomace knocked out when that
	 * was the last turn, else 0.
	 */
	public int turn(long gameTime) {
		if (!pressing()) {
			pressing = pulp;
		}
		juice += nextRelease();
		turns++;
		lastWork = gameTime;
		int pomace = 0;
		if (turns == TURNS) {
			pomace = (pressing + APPLES_PER_POMACE - 1) / APPLES_PER_POMACE;
			pulp = 0;
			pressing = 0;
			turns = 0;
		}
		changed();
		return pomace;
	}

	/** Draws a serving of juice into a bottle. */
	public void draw() {
		juice--;
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
		apples = Math.clamp(input.getIntOr("apples", 0), 0, CAPACITY);
		pulp = Math.clamp(input.getIntOr("pulp", 0), 0, CAPACITY - apples);
		turns = Math.clamp(input.getIntOr("turns", 0), 0, TURNS - 1);
		pressing = turns == 0 ? 0 : Math.clamp(input.getIntOr("pressing", 0), 0, CAPACITY);
		juice = Math.clamp(input.getIntOr("juice", 0), 0, TROUGH);
		bottled = Math.clamp(input.getIntOr("companionBottled", 0), 0, OUTPUT_CAPACITY);
		pomaceStored = Math.clamp(input.getIntOr("companionPomace", 0), 0, OUTPUT_CAPACITY);
		lastWork = -WORK_TICKS;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("apples", apples);
		output.putInt("pulp", pulp);
		output.putInt("turns", turns);
		output.putInt("pressing", pressing);
		output.putInt("juice", juice);
		output.putInt("companionBottled", bottled);
		output.putInt("companionPomace", pomaceStored);
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
