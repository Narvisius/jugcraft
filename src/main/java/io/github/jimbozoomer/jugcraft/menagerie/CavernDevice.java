package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Saved cave machinery. Every output is reserved before inputs, fuel or terrain are changed. */
public final class CavernDevice extends BaseContainerBlockEntity implements WorldlyContainer {
    static final Set<String> KINDS=Set.of("nuclear_furnace_component","quarry","confection_oven","conversion_crucible","hologram_projector");
    public static BlockEntityType<CavernDevice> TYPE;
    private NonNullList<ItemStack> items=NonNullList.withSize(13,ItemStack.EMPTY);
    private UUID owner;
    private int fuel,progress,total=100,cursor,status;
    private String active="";
    private boolean clientDirty;
    private boolean working;
    public boolean projector(){return kind().equals("hologram_projector");}
    final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->fuel;case 1->progress;case 2->total;case 3->status;default->kind().equals("nuclear_furnace_component")?0:kind().equals("quarry")?1:kind().equals("confection_oven")?2:projector()?4:3;};}public void set(int i,int v){}public int getCount(){return 5;}};
    static void register(){TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("cavern_device"),FabricBlockEntityTypeBuilder.create(CavernDevice::new,KINDS.stream().map(n->Menagerie.BLOCKS.get("cavern_"+n)).toArray(Block[]::new)).build());CavernDeviceMenu.register();}
    CavernDevice(BlockPos p,BlockState s){super(TYPE,p,s);}
    String kind(){return ((HouseholdBlock)getBlockState().getBlock()).name;}
    void claimIfUnowned(Player p){if(owner==null)owner(p);}
    void owner(Player p){owner=p.getUUID();setChanged();}
    public static void tick(ServerLevel l,BlockPos p,BlockState s,CavernDevice be){
        if(l.getGameTime()%5!=0)return;
        if(be.projector()&&be.clientDirty){be.clientDirty=false;l.sendBlockUpdated(p,s,s,Block.UPDATE_CLIENTS);}
        be.working=false;
        var actor=Authority.answering(l,be.owner);if(actor==null||!Authority.mayChange(l,actor,p)){be.status=1;be.updateActive(l,p);return;}
        be.status=0;
        switch(be.kind()){
            case "nuclear_furnace_component"->be.nuclear(l,p);
            case "quarry"->be.quarry(l,p,actor);
            case "confection_oven"->be.confection(l,p,actor);
            case "conversion_crucible"->be.convert(l,p,actor);
            default->{}
        }
        be.updateActive(l,p);
    }
    private void updateActive(ServerLevel l,BlockPos p){if(kind().equals("nuclear_furnace_component")){var state=getBlockState();var next=HouseholdBlock.set(state,"active",Boolean.toString(working));if(!next.equals(state))l.setBlock(p,next,Block.UPDATE_CLIENTS);}}
    private boolean is(int slot,String name){return items.get(slot).is(Menagerie.ITEMS.get("cavern_"+name));}
    private boolean reserve(List<ItemStack> output,boolean commit){
        var staged=new SimpleContainer(9);for(int i=0;i<9;i++)staged.setItem(i,items.get(i+2).copy());
        for(var item:output)if(!staged.addItem(item.copy()).isEmpty())return false;
        if(commit)for(int i=0;i<9;i++)items.set(i+2,staged.getItem(i));return true;
    }
    private static BlockPos reactorOrigin(ServerLevel l,BlockPos p){
        var block=Menagerie.BLOCKS.get("cavern_nuclear_furnace_component");
        for(int dx=-1;dx<=0;dx++)for(int dy=-1;dy<=0;dy++)for(int dz=-1;dz<=0;dz++){
            var base=p.offset(dx,dy,dz);boolean complete=true;
            for(var at:BlockPos.betweenClosed(base,base.offset(1,1,1)))if(!l.hasChunkAt(at)||!l.getBlockState(at).is(block)){complete=false;break;}
            if(complete)return base;
        }return null;
    }
    static CavernDevice controller(ServerLevel l,BlockPos p){var at=reactorOrigin(l,p);return at!=null&&l.getBlockEntity(at) instanceof CavernDevice b?b:null;}
    private void nuclear(ServerLevel l,BlockPos p){
        var base=reactorOrigin(l,p);if(base==null){status=2;return;}if(!base.equals(p)){status=3;return;}
        var input=new SingleRecipeInput(items.get(0));var found=l.recipeAccess().getRecipeFor(RecipeType.BLASTING,input,l);if(found.isEmpty()){progress=0;active="";return;}
        var recipe=found.get();String key=recipe.id().identifier().toString();if(!active.equals(key)){active=key;progress=0;}
        var result=recipe.value().assemble(input);if(!reserve(List.of(result),false)){status=4;return;}
        if(fuel==0){
            var waste=new ItemStack(Menagerie.item("cavern_waste_drum"));var stored=items.get(12);
            if(!is(1,"uranium_rod")||!is(11,"metal_barrel")){status=5;return;}
            if(!stored.isEmpty()&&(!ItemStack.isSameItemSameComponents(stored,waste)||stored.getCount()>=stored.getMaxStackSize())){status=6;return;}
            items.get(1).shrink(1);items.get(11).shrink(1);if(stored.isEmpty())items.set(12,waste);else stored.grow(1);fuel=64;setChanged();
        }
        total=Math.max(5,recipe.value().cookingTime()/5);progress+=5;
        if(progress>=total){reserve(List.of(result),true);items.get(0).shrink(1);fuel--;progress=0;}
        working=true;setChanged();
    }
    private void quarry(ServerLevel l,BlockPos p,ServerPlayer actor){
        if(!is(0,"quarry_smasher")){status=7;return;}
        Direction forward=HouseholdBlock.facing(getBlockState()).getOpposite(),side=forward.getClockWise();
        for(int a:new int[]{-4,4})for(int b:new int[]{1,9}){var corner=p.relative(forward,b).relative(side,a);if(!l.hasChunkAt(corner)||!l.getBlockState(corner).is(Menagerie.BLOCKS.get("cavern_magnetic_light"))||!Authority.mayChange(l,actor,corner)){status=8;return;}}
        int maxDepth=Math.min(64,p.getY()-l.getMinY());if(cursor>=49*maxDepth){status=9;return;}
        if(fuel==0){if(!items.get(1).is(Items.REDSTONE)){status=5;return;}items.get(1).shrink(1);fuel=8;setChanged();}
        total=49*maxDepth;progress=cursor;
        for(int scan=0;scan<8&&cursor<total;scan++){
            int cell=cursor%49;var at=p.relative(forward,2+cell/7).relative(side,-3+cell%7).below(1+cursor/49);
            if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at)){status=1;return;}
            var state=l.getBlockState(at);float hardness=state.getDestroySpeed(l,at);
            if(state.isAir()||hardness<0||hardness>50||l.getBlockEntity(at)!=null||!state.getFluidState().isEmpty()){cursor++;setChanged();continue;}
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);if(state.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(state)){cursor++;setChanged();continue;}
            var drops=Block.getDrops(state,l,at,null,actor,tool);if(!reserve(drops,false)){status=4;return;}
            if(!l.removeBlock(at,false))return;reserve(drops,true);fuel--;cursor++;setChanged();return;
        }
    }
    private void confection(ServerLevel l,BlockPos p,ServerPlayer actor){
        if(!l.hasNeighborSignal(p)){status=10;return;}if(!is(0,"gingerbread_crumbs")||!items.get(1).is(Items.SUGAR)){progress=0;status=5;return;}
        if(l.getEntitiesOfClass(MenagerieAnimal.class,new AABB(p).inflate(24),a->a.species().id().equals("cavern_gingerbread_man")).size()>=8){status=11;return;}
        total=200;progress+=5;setChanged();if(progress<total)return;
        var at=p.relative(HouseholdBlock.facing(getBlockState()));if(!Authority.mayChange(l,actor,at))return;
        var mob=Menagerie.TYPES.get("cavern_gingerbread_man").create(l,EntitySpawnReason.MOB_SUMMONED);if(mob==null)return;mob.tame(owner);mob.setPersistenceRequired();mob.snapTo(at.getX()+.5,at.getY()+.1,at.getZ()+.5,0,0);
        if(!l.noCollision(mob)||!l.addFreshEntity(mob))return;items.get(0).shrink(1);items.get(1).shrink(1);progress=0;setChanged();
    }
    private void convert(ServerLevel l,BlockPos p,ServerPlayer actor){
        if(!is(0,"biome_treat")||!items.get(1).is(Items.DIAMOND)){progress=0;status=5;return;}
        String id=items.get(0).getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("BoundBiome","");
        var key=Identifier.tryParse(id);if(key==null){status=12;return;}var biome=l.registryAccess().lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME,key));if(biome.isEmpty()){status=12;return;}
        if(!active.equals(id)){active=id;progress=0;}total=200;progress+=5;setChanged();if(progress<total)return;
        // Player-triggered recolouring of one loaded biome cell region; never regenerates blocks or imports terrain.
        var min=new BlockPos(Math.floorDiv(p.getX()-4,4)*4,Math.floorDiv(p.getY()-4,4)*4,Math.floorDiv(p.getZ()-4,4)*4);var max=min.offset(11,11,11);
        if(min.getY()<l.getMinY()||max.getY()>=l.getMaxY()){status=12;progress=0;return;}
        for(var at:BlockPos.betweenClosed(min,max))if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at)){status=1;progress=0;return;}
        var changed=new ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        for(int cx=min.getX()>>4;cx<=max.getX()>>4;cx++)for(int cz=min.getZ()>>4;cz<=max.getZ()>>4;cz++){
            var chunk=l.getChunkAt(new BlockPos(cx*16,p.getY(),cz*16));
            for(int sy=min.getY()>>4;sy<=max.getY()>>4;sy++){
                var section=chunk.getSection(chunk.getSectionIndex(sy*16));
                section.fillBiomesFromNoise((x,y,z)->x*4>=min.getX()&&x*4<=max.getX()&&y*4>=min.getY()&&y*4<=max.getY()&&z*4>=min.getZ()&&z*4<=max.getZ()?biome.get():section.getNoiseBiome(x&3,y&3,z&3),cx*4,sy*4,cz*4);
            }chunk.markUnsaved();changed.add(chunk);
        }
        l.getChunkSource().chunkMap.resendBiomesForChunks(changed);items.get(0).shrink(1);items.get(1).shrink(1);progress=0;setChanged();
    }
    @Override public void setChanged(){super.setChanged();clientDirty=true;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){
        var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,lookup);var display=NonNullList.withSize(13,ItemStack.EMPTY);if(projector())display.set(0,items.get(0).copy());ContainerHelper.saveAllItems(out,display);return out.buildResult();
    }
    @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new CavernDeviceMenu(id,inv,this,data);}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> v){items=v;}
    @Override public int getContainerSize(){return 13;}
    @Override public boolean canPlaceItem(int i,ItemStack s){return i==0?(!projector()||s.is(Menagerie.ITEMS.get("cavern_holocoder"))):i==1&&!projector()||i==11&&kind().equals("nuclear_furnace_component")&&s.is(Menagerie.ITEMS.get("cavern_metal_barrel"));}
    @Override public int[] getSlotsForFace(Direction d){return d==Direction.DOWN?new int[]{2,3,4,5,6,7,8,9,10,12}:d==Direction.UP?new int[]{0}:new int[]{1,11};}
    @Override public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return canPlaceItem(i,s);}
    @Override public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return i>=2&&i!=11;}
    @Override protected void saveAdditional(ValueOutput o){super.saveAdditional(o);ContainerHelper.saveAllItems(o,items);if(owner!=null)o.putString("Owner",owner.toString());o.putInt("Fuel",fuel);o.putInt("Progress",progress);o.putInt("Cursor",cursor);o.putString("Active",active);}
    @Override protected void loadAdditional(ValueInput i){super.loadAdditional(i);items=NonNullList.withSize(13,ItemStack.EMPTY);ContainerHelper.loadAllItems(i,items);try{owner=UUID.fromString(i.getStringOr("Owner",""));}catch(IllegalArgumentException ignored){owner=null;}fuel=Math.clamp(i.getIntOr("Fuel",0),0,64);progress=Math.clamp(i.getIntOr("Progress",0),0,10000);cursor=Math.clamp(i.getIntOr("Cursor",0),0,49*64);active=i.getStringOr("Active","");}
}
