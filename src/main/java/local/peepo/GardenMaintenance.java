package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Opt-in maintenance, bounded to learned farmland and permitted crops inside an assigned zone. */
final class GardenMaintenance {
    static final int CARRY_LIMIT=4,ZONE_COOLDOWN=400,CROP_COOLDOWN=1200;
    private final PeepoEntity npc;
    private final Map<CompanionAssignments.Target,LinkedHashMap<BlockPos,BlockState>> soils=new HashMap<>();
    private final Map<CompanionAssignments.Target,Long> nextUse=new HashMap<>();
    // Shared across companions/overlapping zones. Weak level keys; no entity references, no forced loads.
    private static final Map<Level,LinkedHashMap<BlockPos,Long>> FED=new WeakHashMap<>();
    GardenMaintenance(PeepoEntity npc){this.npc=npc;}
    private record Soil(BlockPos pos,BlockState state){
        static final com.mojang.serialization.Codec<Soil> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
            BlockPos.CODEC.fieldOf("Pos").forGetter(Soil::pos),BlockState.CODEC.fieldOf("Soil").forGetter(Soil::state)).apply(i,Soil::new));
    }
    void removed(CompanionAssignments.Target target){soils.remove(target);nextUse.remove(target);}
    void observe(CompanionAssignments.Target target,BlockPos pos){
        if(target.gardenZone()==null || target.orchard() || !target.gardenZone().ground(pos) || !npc.level().hasChunkAt(pos))return;
        var state=npc.level().getBlockState(pos);
        if(state.is(Blocks.FARMLAND) || state.getBlock() instanceof RichFarmlandBlock){
            var memory=soils.computeIfAbsent(target,t->new LinkedHashMap<>());
            if(memory.containsKey(pos) || memory.size()<256)memory.put(pos.immutable(),state.getBlock().defaultBlockState());
        }else if(!state.is(Blocks.DIRT) && !(state.getBlock() instanceof RichSoilBlock)){
            var memory=soils.get(target);if(memory!=null)memory.remove(pos);
        }
    }
    private BlockState repairState(CompanionAssignments.Target target,BlockPos pos){var memory=soils.get(target);return memory==null?null:memory.get(pos);}
    boolean repairable(CompanionAssignments.Target target,BlockPos pos){
        if(!npc.assignments.gardenMaintenance(target,CompanionAssignments.REPAIR) || !npc.level().hasChunkAt(pos) || !npc.level().hasChunkAt(pos.above()))return false;
        var remembered=repairState(target,pos);if(remembered==null || !npc.level().getBlockState(pos.above()).isAir())return false;
        var actual=npc.level().getBlockState(pos);
        return remembered.is(Blocks.FARMLAND)?actual.is(Blocks.DIRT):remembered.getBlock() instanceof RichFarmlandBlock && actual.getBlock() instanceof RichSoilBlock;
    }
    boolean ready(CompanionAssignments.Target target){long now=npc.level().getGameTime(),until=nextUse.getOrDefault(target,0L);return until<=now || until>now+ZONE_COOLDOWN;}
    boolean repair(CompanionAssignments.Target target,BlockPos pos){
        if(!repairable(target,pos) || !ready(target) || !CompanionJobs.permitted(npc,pos) || !CompanionJobs.permitted(npc,pos.above()))return false;
        if(!npc.level().setBlock(pos,repairState(target,pos),Block.UPDATE_ALL))return false;
        nextUse.put(target,npc.level().getGameTime()+ZONE_COOLDOWN);
        npc.level().playSound(null,pos,SoundEvents.HOE_TILL.value(),SoundSource.BLOCKS,.7F,1);return true;
    }
    static boolean fertilizer(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return stack.is(Items.BONE_MEAL)
            || stack.getItem() instanceof io.github.jimbozoomer.jugcraft.chemistry.SpecializedFertilizerItem
            || stack.is(io.github.jimbozoomer.jugcraft.chemistry.PetroItems.FERTILIZER)
            || id.getNamespace().equals("jugcraft") && id.getPath().equals("bat_guano");
    }
    private static int radius(ItemStack stack){return stack.is(Items.BONE_MEAL)?0:stack.getItem() instanceof io.github.jimbozoomer.jugcraft.chemistry.SpecializedFertilizerItem || stack.is(io.github.jimbozoomer.jugcraft.chemistry.PetroItems.FERTILIZER)?2:1;}
    private static int doses(ItemStack stack,BlockState crop){return stack.getItem() instanceof io.github.jimbozoomer.jugcraft.chemistry.SpecializedFertilizerItem blend?blend.doses(crop):radius(stack)==2?2:1;}
    private boolean eligible(ItemStack stack,BlockPos pos){return radius(stack)==0 || npc.level().getBlockState(pos).is(BlockTags.CROPS)
        || stack.getItem() instanceof io.github.jimbozoomer.jugcraft.chemistry.SpecializedFertilizerItem && npc.level().getBlockState(pos).getBlock() instanceof TallCropBlock;}
    int carried(){int n=0;for(int i=0;i<8;i++)if(!npc.transport.reserved(i) && fertilizer(npc.belongings.getItem(i)))n+=npc.belongings.getItem(i).getCount();return n;}
    private boolean cooled(BlockPos pos){var times=FED.get(npc.level());long until=times==null?0:times.getOrDefault(pos,0L),now=npc.level().getGameTime();return until<=now || until>now+CROP_COOLDOWN;}
    boolean crop(CompanionAssignments.Target target,BlockPos pos){
        var zone=target.gardenZone();if(zone==null || zone.orchard() || !zone.contains(pos) || !zone.ground(pos.below()) || !npc.level().hasChunkAt(pos) || !cooled(pos))return false;
        if(!CompanionJobs.permitted(npc,pos) || !CompanionJobs.permitted(npc,pos.below()))return false;
        var state=npc.level().getBlockState(pos);var block=state.getBlock();
        // Explicit crop set: never fertilize trees, flowers, magic beds, or fruit-producing stems across an edge.
        if(block instanceof OrganismCropBlock || !(block instanceof CropBlock || block instanceof TallCropBlock || block instanceof CranberryBushBlock || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock))return false;
        if(block instanceof TallCropBlock tall){
            if(state.getValue(TallCropBlock.SECTION)!=0 || TallCropBlock.isRipe(state))return false;
            for(int y=1;y<tall.crop().height(TallCropBlock.MAX_AGE);y++){var at=pos.above(y);if(!zone.contains(at.below()) || !CompanionJobs.permitted(npc,at))return false;}
        }
        return block instanceof BonemealableBlock grow && grow.isValidBonemealTarget(npc.level(),pos,state,BonemealSource.INTERACTION);
    }
    boolean fertilizable(CompanionAssignments.Target target,BlockPos pos){return npc.assignments.gardenMaintenance(target,CompanionAssignments.FERTILIZE) && ready(target) && crop(target,pos);}
    boolean useful(CompanionAssignments.Target target,BlockPos pos,ItemStack stack){return fertilizer(stack) && fertilizable(target,pos) && eligible(stack,pos);}
    int slot(CompanionAssignments.Target target,BlockPos pos){
        if(!fertilizable(target,pos))return -1;
        int best=-1,score=-1;
        for(int i=0;i<8;i++){var stack=npc.belongings.getItem(i);if(!npc.transport.reserved(i) && useful(target,pos,stack)){
            int strength=doses(stack,npc.level().getBlockState(pos));if(strength>score){best=i;score=strength;}
        }}
        return best;
    }
    private LinkedHashMap<BlockPos,Long> cooldowns(){
        var times=FED.computeIfAbsent(npc.level(),l->new LinkedHashMap<>());long now=npc.level().getGameTime();
        if(times.size()>=4000){var retained=new LinkedHashMap<BlockPos,Long>();var it=times.entrySet().iterator();int read=0;
            while(it.hasNext() && read++<128){var e=it.next();if(e.getValue()>now && e.getValue()<=now+CROP_COOLDOWN)retained.put(e.getKey(),e.getValue());it.remove();}times.putAll(retained);}
        return times;
    }
    boolean fertilize(CompanionAssignments.Target target,BlockPos center){
        int slot=slot(target,center);if(slot<0)return false;
        var stack=npc.belongings.getItem(slot);int radius=radius(stack);var server=(ServerLevel)npc.level();var times=cooldowns();
        // At most 75 cells and four native growth doses per crop. Exclude unauthorized/unloaded/out-of-zone cells.
        var candidates=new ArrayList<BlockPos>();
        for(var at:BlockPos.betweenClosed(center.offset(-radius,radius==0?0:-1,-radius),center.offset(radius,radius==0?0:1,radius)))
            if(crop(target,at) && eligible(stack,at))candidates.add(at.immutable());
        int added=0;for(var at:candidates)if(!times.containsKey(at))added++;
        if(candidates.isEmpty() || times.size()+added>4096){nextUse.put(target,server.getGameTime()+ZONE_COOLDOWN);return false;}
        boolean grew=false;
        try(var tx=Transaction.openOuter()){
            if(npc.food.takeCargo(slot,stack,1,tx)!=1)return false;
            for(var at:candidates){boolean any=false;
                for(int dose=0,doses=doses(stack,server.getBlockState(at));dose<doses;dose++)if(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),server,at))any=true;
                if(any){grew=true;times.put(at,server.getGameTime()+CROP_COOLDOWN);server.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH,at,15);}
            }
            if(grew)tx.commit();
        }
        // Failed native attempts also back off rather than retrying every work cycle.
        nextUse.put(target,server.getGameTime()+ZONE_COOLDOWN);return grew;
    }
    void save(ValueOutput out){for(int row=1;row<=4;row++){
        var target=npc.assignments.get(row);var memory=soils.get(target);if(memory!=null)out.store("GardenSoils"+row,Soil.CODEC.listOf(),memory.entrySet().stream().map(e->new Soil(e.getKey(),e.getValue())).toList());
        if(target!=null)out.putLong("GardenMaintainAfter"+row,nextUse.getOrDefault(target,0L));
    }}
    void load(ValueInput in){soils.clear();nextUse.clear();for(int row=1;row<=4;row++){
        var target=npc.assignments.get(row);if(target==null || target.gardenZone()==null || target.orchard())continue;
        var memory=new LinkedHashMap<BlockPos,BlockState>();
        for(var soil:in.read("GardenSoils"+row,Soil.CODEC.listOf()).orElse(List.of())){
            if(memory.size()>=256)break;
            if(target.gardenZone().ground(soil.pos()) && (soil.state().is(Blocks.FARMLAND) || soil.state().getBlock() instanceof RichFarmlandBlock))memory.put(soil.pos(),soil.state().getBlock().defaultBlockState());
        }
        if(!memory.isEmpty())soils.put(target,memory);nextUse.put(target,in.getLongOr("GardenMaintainAfter"+row,0));
    }}
}
