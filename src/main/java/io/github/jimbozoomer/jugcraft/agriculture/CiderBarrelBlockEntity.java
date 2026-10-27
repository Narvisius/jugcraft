package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import local.peepo.CompanionRecipeFilter;
import local.peepo.CompanionStatus;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cider in a Cider Barrel: up to {@value #CAPACITY} servings of one batch and the game time it started ageing. Fresh
 * juice (Sweet Cider) ferments into Sparkling Cider after {@value #SPARKLING_TICKS} ticks (a Minecraft day) and matures
 * into Aged Cider after {@value #AGED_TICKS} (three days). Topping up a batch that is still sweet starts its clock again;
 * a batch that has begun to ferment takes no fresh juice. Ageing is counted from the start time, so it needs no ticking;
 * the barrel only looks every {@value #CHECK_TICKS} ticks to change its chalk mark ({@link CiderBarrelBlock#CIDER}).
 */
public class CiderBarrelBlockEntity extends BlockEntity {
	public static final int CAPACITY = 16;
	public static final int SPARKLING_TICKS = 24000;
	public static final int AGED_TICKS = 72000;
	static final int CHECK_TICKS = 20;
	/** The drink a serving is at each stage. */
	public static final List<String> STAGES = List.of("sweet_cider", "sparkling_cider", "aged_cider");

	private int servings;
	private long started;

    private int companionBottles;
    /** Brief unloading window for an eight-slot delivery; topping up never resets this batch's clock. */
    private long companionFillUntil;
    private final CompanionRecipeFilter filter = new CompanionRecipeFilter();
    public CompanionRecipeFilter filter() { return filter; }
    private int stockStage;
    public void stockStage(int stage){if(servings==0 && stockStage!=stage){stockStage=stage;changed();}}
    public int companionStage() {
        if(stockStage>0 && filter.allows(new ItemStack(JugcraftAgriculture.item(STAGES.get(stockStage)))))return stockStage;
        return filter.allows(new ItemStack(JugcraftAgriculture.item("sparkling_cider"))) ? 1 : 2;
    }
    public void selectCompanionStage(boolean aged) {
        stockStage=0;
        for(int i=0;i<9;i++)filter.set(i,ItemStack.EMPTY);
        filter.set(0,new ItemStack(JugcraftAgriculture.item(aged?"aged_cider":"sparkling_cider")));
        changed();
    }
    private record TransferState(int servings, long started, int bottles, long fillUntil) {}
    private final SnapshotParticipant<TransferState> transfers = new SnapshotParticipant<>() {
        protected TransferState createSnapshot() { return new TransferState(servings,started,companionBottles,companionFillUntil); }
        protected void readSnapshot(TransferState s) { servings=s.servings();started=s.started();companionBottles=s.bottles();companionFillUntil=s.fillUntil(); }
        protected void onFinalCommit() { changed(); }
    };
    private final Storage<ItemVariant> companionInput = new InsertionOnlyStorage<>() {
        public long insert(ItemVariant item,long amount,TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(item,amount);
            int count=(int)Math.min(amount,companionNeed(item.toStack(1)));
            if(count<=0)return 0;
            transfers.updateSnapshots(tx);
            if(item.isOf(JugcraftAgriculture.item("sweet_cider"))) {
                if(servings==0){started=level.getGameTime();companionFillUntil=started+100;}
                servings+=count;
            }
            // Sweet cider is emptied into the cask; retain its actual bottles for this batch.
            companionBottles+=count;
            return count;
        }
    };
    private final Storage<ItemVariant> companionOutput = new CombinedStorage<>(List.of(new BarrelOutput(false),new BarrelOutput(true)));
    private class BarrelOutput implements SingleSlotStorage<ItemVariant> {
        private final boolean spareBottles;
        BarrelOutput(boolean spareBottles){this.spareBottles=spareBottles;}
        public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
        public boolean supportsInsertion(){return false;}
        public long extract(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);
            if(!item.equals(getResource()))return 0;
            int count=(int)Math.min(amount,getAmount());
            if(count<=0)return 0;
            transfers.updateSnapshots(tx);companionBottles-=count;
            if(!spareBottles){servings-=count;if(servings==0){started=0;companionFillUntil=0;}}
            return count;
        }
        public boolean isResourceBlank(){return getAmount()==0;}
        public ItemVariant getResource(){
            if(getAmount()==0)return ItemVariant.blank();
            return ItemVariant.of(spareBottles?Items.GLASS_BOTTLE:JugcraftAgriculture.item(STAGES.get(stage(level.getGameTime()))));
        }
        public long getAmount(){
            if(isRemoved() || level==null || level.isClientSide())return 0;
            if(spareBottles)return Math.max(0,companionBottles-servings);
            return stage(level.getGameTime())>=companionStage()?Math.min(servings,companionBottles):0;
        }
        public long getCapacity(){return CAPACITY;}
    }
    public int companionNeed(ItemStack item){
        if(isRemoved() || level==null || level.isClientSide())return 0;
        long now=level.getGameTime();
        if(item.is(JugcraftAgriculture.item("sweet_cider")) && (servings==0 || now<companionFillUntil && stage(now)==0))
            return Math.min(CAPACITY-servings,CAPACITY-companionBottles);
        return item.is(Items.GLASS_BOTTLE)?Math.max(0,servings-companionBottles):0;
    }
    public Storage<ItemVariant> companionInputs(){return companionInput;}
    public Storage<ItemVariant> companionOutputs(){return companionOutput;}
    public CompanionStatus companionStatus(){
        if(isRemoved() || level==null)return CompanionStatus.MISSING;
        if(companionBottles>servings)return CompanionStatus.FULL;
        if(servings==0)return CompanionStatus.NO_INPUT;
        if(stage(level.getGameTime())<companionStage())return CompanionStatus.AGING;
        return companionBottles==0?CompanionStatus.NEEDS_BOTTLES:CompanionStatus.FULL;
    }
    /** Recover reserved bottles by hand, including after manually drawing the batch. */
    public boolean collectCompanionBottles(Player player){
        if(companionBottles==0)return false;
        var bottles=new ItemStack(Items.GLASS_BOTTLE,companionBottles);
        player.getInventory().add(bottles);companionBottles=bottles.getCount();changed();return true;
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel server && companionBottles>0){
            Block.popResource(server,pos,new ItemStack(Items.GLASS_BOTTLE,companionBottles));companionBottles=0;
        }
    }


	public CiderBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.CIDER_BARREL_ENTITY, pos, state);
	}

	public int servings() {
		return servings;
	}

	public long started() {
		return started;
	}

	/** Sets the batch's start time (game tests age a barrel this way). */
	public void setStarted(long started) {
		this.started = started;
		changed();
	}

	/** The batch's stage at {@code now}: 0 sweet, 1 sparkling, 2 aged; -1 when empty. */
	public int stage(long now) {
		return servings == 0 ? -1 : stageAt(now - started);
	}

	public static int stageAt(long age) {
		return age >= AGED_TICKS ? 2 : age >= SPARKLING_TICKS ? 1 : 0;
	}

	/** Ticks until the batch reaches its next stage at {@code now}, or 0 when aged (or empty). */
	public long untilNext(long now) {
		int stage = stage(now);
		return stage < 0 || stage == 2 ? 0 : (stage == 0 ? SPARKLING_TICKS : AGED_TICKS) - (now - started);
	}

	/** Whether a serving of fresh juice can go in at {@code now}: room, and a batch still sweet (or none). */
	public boolean canFill(long now) {
		return servings < CAPACITY && stage(now) <= 0;
	}

	/** Pours in a serving of fresh juice; the batch's clock starts again. */
	public void fill(long now) {
		servings++;
        companionFillUntil=0;
		started = now;
		changed();
	}

	/** Draws a serving off; returns the drink it is (an item ID under jugcraft). */
	public String draw(long now) {
		String drink = STAGES.get(stage(now));
		servings--;
		if (servings == 0) {
			started = 0;
		}
		changed();
		return drink;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (level.getGameTime() % CHECK_TICKS == 0) {
			int mark = stage(level.getGameTime()) + 1;
			if (state.getValue(CiderBarrelBlock.CIDER) != mark) {
				level.setBlock(pos, state.setValue(CiderBarrelBlock.CIDER, mark), Block.UPDATE_CLIENTS);
			}
		}
	}

	private void changed() {
        local.peepo.CompanionReadiness.invalidate(this);
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = getBlockState();
			int mark = stage(level.getGameTime()) + 1;
			if (state.hasProperty(CiderBarrelBlock.CIDER) && state.getValue(CiderBarrelBlock.CIDER) != mark) {
				level.setBlock(worldPosition, state.setValue(CiderBarrelBlock.CIDER, mark), Block.UPDATE_CLIENTS);
			}
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		BarrelCider cider = components.get(JugcraftAgriculture.BARREL_CIDER);
		if (cider != null) {
			servings = cider.servings();
			started = cider.started();
		}
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (servings > 0) {
			components.set(JugcraftAgriculture.BARREL_CIDER, new BarrelCider(servings, started));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		servings = Math.clamp(input.getIntOr("servings", 0), 0, CAPACITY);
		started = input.getLongOr("started", 0L);
        companionBottles=Math.clamp(input.getIntOr("CompanionBottles",0),0,CAPACITY);
        companionFillUntil=input.getLongOr("CompanionFillUntil",0L);
        filter.load(input);stockStage=Math.clamp(input.getIntOr("CompanionStockStage",0),0,2);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("servings", servings);
		output.putLong("started", started);
        output.putInt("CompanionBottles",companionBottles);
        output.putLong("CompanionFillUntil",companionFillUntil);
        filter.save(output);output.putInt("CompanionStockStage",stockStage);
	}
}
