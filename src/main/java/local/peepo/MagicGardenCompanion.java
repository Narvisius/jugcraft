package local.peepo;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.garden.*;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Mulch;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Native garden items only: no growth pulses, nutrient creation, awakening or research credit. */
public final class MagicGardenCompanion implements CompanionLogistics.Port {
    public static final int MAW_STOCK=16;
    private final PeepoEntity npc;
    private final LivingDeviceBlockEntity device;
    private MagicGardenCompanion(PeepoEntity npc,LivingDeviceBlockEntity device){this.npc=npc;this.device=device;}
    public static boolean supports(BlockEntity block){return block instanceof MulchMawBlockEntity || block instanceof GleanerBlockEntity;}
    public static MagicGardenCompanion of(PeepoEntity npc,BlockEntity block){return supports(block)?new MagicGardenCompanion(npc,(LivingDeviceBlockEntity)block):null;}
    static boolean keeperAccess(PeepoEntity npc,java.util.UUID keeper){return npc.orders.tamed() && (keeper==null || npc.orders.foodAccess(keeper,true));}
    private static boolean ownerAvailable(PeepoEntity npc){return npc.level() instanceof ServerLevel level && (Authority.present(level,npc.orders.ownerId())!=null || Authority.absentOwnersAct());}
    /** Cheap checks used by discovery; protection callbacks run again at the actual mutation. */
    static boolean bedAvailable(PeepoEntity npc,BlockPos pos){
        return Garden.enabled() && npc.level().hasChunkAt(pos) && npc.level().getBlockEntity(pos) instanceof VerdantBedBlockEntity bed
            && bed.awake() && keeperAccess(npc,bed.keeper()) && ownerAvailable(npc);
    }
    static boolean mayChange(PeepoEntity npc,BlockPos pos){
        return npc.level() instanceof ServerLevel level && CompanionJobs.permitted(npc,pos) && Authority.mayChangeFor(level,npc.orders.ownerId(),pos);
    }
    private boolean available(){return !device.isRemoved() && npc.level().hasChunkAt(device.getBlockPos()) && npc.level().getBlockEntity(device.getBlockPos())==device && Garden.enabled()
        && device.awake() && keeperAccess(npc,device.keeper()) && ownerAvailable(npc) && CompanionJobs.permitted(npc,device.getBlockPos());}
    public Identifier plan(){return device instanceof MulchMawBlockEntity maw?PeepoMod.id("mulch_maw/"+maw.companionFilter.revision()):null;}
    public int needed(ItemStack stack){
        if(!(device instanceof MulchMawBlockEntity maw) || !available() || stack.isEmpty() || Garden.mulch(stack)<=0
            || maw.held()>=Mulch.QUARTERS || maw.status().equals("beds_full") || !maw.companionFilter.allows(stack))return 0;
        var held=maw.food();
        return held.isEmpty() || ItemStack.isSameItemSameComponents(held,stack)?Math.max(0,Math.min(MAW_STOCK,stack.getMaxStackSize())-held.getCount()):0;
    }
    public Storage<ItemVariant> inputs(){
        if(!(device instanceof MulchMawBlockEntity maw))return Storage.empty();
        return new InsertionOnlyStorage<>(){
            public long insert(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);
                long count=Math.min(amount,needed(item.toStack(1)));
                return count>0 && mayChange(npc,device.getBlockPos())?ContainerStorage.of(maw,Direction.UP).insert(item,count,tx):0;
            }
        };
    }
    public Storage<ItemVariant> outputs(){
        if(!(device instanceof GleanerBlockEntity gleaner))return Storage.empty();
        return new CombinedStorage<>(java.util.stream.IntStream.range(0,GleanerBlockEntity.SLOTS).mapToObj(slot->new SingleSlotStorage<ItemVariant>(){
            private ItemStack stack(){return available()?gleaner.getItem(slot):ItemStack.EMPTY;}
            public ItemVariant getResource(){return ItemVariant.of(stack());}
            public boolean isResourceBlank(){return stack().isEmpty();}
            public long getAmount(){return stack().getCount();}
            public long getCapacity(){return 64;}
            public boolean supportsInsertion(){return false;}
            public long insert(ItemVariant item,long amount,TransactionContext tx){return 0;}
            public long extract(ItemVariant item,long amount,TransactionContext tx){
                StoragePreconditions.notBlankNotNegative(item,amount);
                return available() && mayChange(npc,device.getBlockPos())?ContainerStorage.of(gleaner,Direction.DOWN).getSlot(slot).extract(item,amount,tx):0;
            }
        }).toList());
    }
    public CompanionStatus status(){
        if(!Garden.enabled())return CompanionStatus.MAGIC_DISABLED;
        if(!device.awake())return CompanionStatus.DORMANT_GARDEN;
        if(!available())return CompanionStatus.FORBIDDEN;
        if(device instanceof MulchMawBlockEntity maw)return maw.held()>=Mulch.QUARTERS || maw.status().equals("beds_full")?CompanionStatus.FULL:maw.food().isEmpty()?CompanionStatus.NO_INPUT:CompanionStatus.WORKING;
        var gleaner=(GleanerBlockEntity)device;
        if(!gleaner.isEmpty())return CompanionStatus.READY;
        return device.status().equals("no_verdance")?CompanionStatus.NEEDS_VERDANCE:device.status().equals("keeper_away")?CompanionStatus.FORBIDDEN:CompanionStatus.GROWING;
    }
}
