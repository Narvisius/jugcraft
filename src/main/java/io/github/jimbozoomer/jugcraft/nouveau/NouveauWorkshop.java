package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Real owned inventories; recipes reserve first and commit on the server only when outputs and power fit. */
public final class NouveauWorkshop extends BlockEntity implements WorldlyContainer,GeoBlockEntity {
    public static final int SLOTS=54,INPUTS=9,OUTPUT=9,OUTPUT_END=18;
    private NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private @Nullable UUID owner;
    private long ley;
    private boolean paused,locked;
    private String selected="",job="",status="Ready",customName="";
    public String customName(){return customName;}
    public void customName(String text){customName=text.strip().substring(0,Math.min(64,text.strip().length()));setChanged();}
    private boolean clientDirty;
    @Override public void setChanged(){super.setChanged();clientDirty=true;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){
        // Nearby clients need the visible specimen and display slots, not the full storage network.
        var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,lookup);
        var display=NonNullList.withSize(SLOTS,ItemStack.EMPTY);display.set(0,items.get(0).copy());display.set(9,items.get(9).copy());ContainerHelper.saveAllItems(out,display);
        out.putLong("ley",ley);out.putString("custom_name",customName);out.putInt("doses",doses);out.store("potion",net.minecraft.world.item.alchemy.PotionContents.CODEC,potion);
        if(captured!=null)out.store("captured",net.minecraft.world.item.component.CustomData.CODEC,captured);
        if(!preview.isEmpty())out.store("preview",NouveauPlanarium.Mini.CODEC.listOf(),preview);return out.buildResult();
    }
    private List<NouveauPlanarium.Mini> preview=List.of();
    public List<NouveauPlanarium.Mini> preview(){return preview;}
    public void preview(List<NouveauPlanarium.Mini> next){if(!preview.equals(next)){preview=List.copyOf(next);setChanged();}}
    private int progress;
    private int cycles;
    private ItemStack filter=ItemStack.EMPTY;
    private int threshold=1,signal;
    private long signalUntil;
    private boolean inverted,lastPowered;
    private List<NouveauWorkshop> repositories=List.of();private long repositoryRefresh;
    private List<NouveauWorkshop> repositories(){if(!kind().equals("repository_controller")||level==null)return List.of();if(level.getGameTime()>=repositoryRefresh){repositories=NouveauRepositories.connected(this);repositoryRefresh=level.getGameTime()+40;}return repositories;}
    public List<NouveauWorkshop> connectedRepositories(){return repositories();}
    private NouveauWorkshop repository(int slot){int n=slot/54-1;var connected=repositories();if(n<0||n>=connected.size())return null;var be=connected.get(n);if(be.isRemoved()||!(level instanceof ServerLevel s)||!s.hasChunkAt(be.getBlockPos()))return null;var owner=Authority.answering(s,this.owner);return owner!=null&&be.permitted(owner)?be:null;}
    public ItemStack filter(){return filter;}
    public ItemStack sample(){return !filter.isEmpty()?filter:getItem(0);}
    public void filter(ItemStack value){filter=value.copyWithCount(value.isEmpty()?0:1);setChanged();}
    public int threshold(){return threshold;}
    public void threshold(int value){threshold=Math.clamp(value,1,4096);setChanged();}
    public boolean inverted(){return inverted;}
    public void invert(){inverted=!inverted;setChanged();}
    public int signal(){return signal;}
    public long signalUntil(){return signalUntil;}
    public void signal(int power,long until){signalUntil=until;if(signal==power)return;signal=Math.clamp(power,0,15);setChanged();if(level!=null)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}
    public boolean rising(boolean powered){boolean rising=powered&&!lastPowered;lastPowered=powered;return rising;}
    public boolean accepts(ItemStack stack){return NouveauFilters.allows(filter,stack)&&(!(level instanceof ServerLevel s)||NouveauFilters.frames(s,worldPosition,stack));}
    public void clearLinks(){links.clear();setChanged();}
    public boolean every(int seconds){return cycles%seconds==0;}
    private net.minecraft.world.item.alchemy.PotionContents potion=net.minecraft.world.item.alchemy.PotionContents.EMPTY;
    private int doses, diffuserRemaining;
    public net.minecraft.world.item.alchemy.PotionContents potion(){return potion;}
    public int doses(){return doses;}
    public boolean acceptsPotion(net.minecraft.world.item.alchemy.PotionContents value,int count){return kind().equals("potion_jar")&&count>0&&doses+count<=100&&((doses==0&&!locked)||potion.equals(value));}
    public boolean fillPotion(net.minecraft.world.item.alchemy.PotionContents value,int count){if(!acceptsPotion(value,count))return false;potion=value;doses+=count;setChanged();syncFill();return true;}
    public boolean drainPotion(int count){if(count<=0||doses<count)return false;doses-=count;setChanged();syncFill();return true;}
    public void diffusion(net.minecraft.world.item.alchemy.PotionContents value,int seconds){potion=value;diffuserRemaining=Math.clamp(seconds,0,600);setChanged();}
    public boolean diffusing(){return diffuserRemaining>0;}
    public void advanceDiffuser(){if(diffuserRemaining>0){diffuserRemaining--;setChanged();}}
    private String ritual="";
    private int ritualCursor;
    private boolean ritualRunning;
    private List<ItemStack> ritualAugments=List.of();
    public boolean ritualRunning(){return ritualRunning;}
    public ItemStack ritualAugment(int index){return index>=0&&index<ritualAugments.size()?ritualAugments.get(index):ItemStack.EMPTY;}
    public void prepareRitual(String id){ritual=id;ritualCursor=0;ritualRunning=false;ritualAugments=List.of();setChanged();}
    public void startRitual(){var augments=new ArrayList<ItemStack>();for(int i=1;i<9;i++){augments.add(items.get(i).copy());items.set(i,ItemStack.EMPTY);}ritualAugments=List.copyOf(augments);ritualRunning=true;setChanged();}
    private net.minecraft.world.item.component.@Nullable CustomData captured;
    public net.minecraft.world.item.component.@Nullable CustomData captured(){return captured;}
    public void captured(net.minecraft.world.item.component.@Nullable CustomData value){captured=value;setChanged();}
    private final List<BlockPos> links=new ArrayList<>();
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public NouveauWorkshop(BlockPos p,BlockState s){super(NouveauWorkshops.ENTITY,p,s);}
    public NouveauCatalog.Entry entry(){return ((NouveauWorkshopBlock)getBlockState().getBlock()).entry;}
    public String kind(){return entry().source();}
    public @Nullable UUID owner(){return owner;}
    public void placedBy(ServerPlayer player){owner=player.getUUID();setChanged();}
    public boolean belongs(UUID who){return owner!=null&&(owner.equals(who)||JugcraftParties.sameParty(owner,who));}
    public boolean mayUse(ServerPlayer p){
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||level!=p.level()||!Authority.mayChange(p.level(),p,worldPosition))return false;
        // Natural structure stations have no placer. The first nearby permitted visitor adopts them.
        if(owner==null&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64)placedBy(p);
        return belongs(p.getUUID());
    }
    public boolean permitted(ServerPlayer p){return belongs(p.getUUID())&&level==p.level()&&level.hasChunkAt(worldPosition)&&Authority.mayChange(p.level(),p,worldPosition);}
    public long capacity(){return kind().equals("creative_source_jar")?1000000:kind().contains("source_jar")?1000:128;}
    public long ley(){return ley;}
    public Reservoir reservoir(){return new Reservoir(LeyPylonBlockEntity.LEY,ley,capacity());}
    public long fill(long amount){long n=Math.clamp(amount,0,capacity()-ley);ley+=n;if(n>0){setChanged();syncFill();}return n;}
    public boolean draw(long amount){if(amount<0||ley<amount)return false;ley-=amount;if(amount>0){setChanged();syncFill();}return true;}
    private void syncFill(){if(level==null)return;long amount=kind().equals("potion_jar")?doses:ley,max=kind().equals("potion_jar")?100:capacity();int fill=amount==0?0:1+(int)Math.min(10,amount*10/max);if(getBlockState().getValue(NouveauWorkshopBlock.FILL)!=fill)level.setBlock(worldPosition,getBlockState().setValue(NouveauWorkshopBlock.FILL,fill),Block.UPDATE_CLIENTS);}
    public String ritual(){return ritual;}
    public int ritualCursor(){return ritualCursor;}
    public void ritual(String id,int cursor){ritual=id;ritualCursor=cursor;if(id.isEmpty()){ritualRunning=false;ritualAugments=List.of();}setChanged();}
    public String status(){return status;}
    public void status(String text){status=text;}
    public int progress(){return progress;}
    public boolean paused(){return paused;}
    public boolean locked(){return locked;}
    public String selected(){return selected;}
    public List<BlockPos> links(){return List.copyOf(links);}
    public boolean link(ServerPlayer p,BlockPos target){
        if(kind().equals("ritual_brazier")&&p.level().getBlockEntity(target) instanceof NouveauWorkshop relay&&relay.kind().equals("brazier_relay")&&(target.distSqr(worldPosition)>225||!relay.permitted(p)))return false;
        boolean warp=kind().equals("relay_warp")&&p.level().hasChunkAt(target)&&p.level().getBlockEntity(target) instanceof NouveauWorkshop other&&other.kind().equals("relay_warp")&&other.permitted(p);
        if(!mayUse(p)||target.equals(worldPosition)||!warp&&target.distSqr(worldPosition)>32*32||!p.level().hasChunkAt(target)||!Authority.mayChange(p.level(),p,target))return false;
        if(links.remove(target)){setChanged();return true;}if(links.size()>=8)return false;links.add(target.immutable());setChanged();return true;
    }
    public void select(String id){if(locked&&!id.equals(selected))return;selected=id;job="";progress=0;setChanged();}
    public void togglePause(){paused=!paused;setChanged();}
    public void toggleLock(){locked=!locked;setChanged();}
    public boolean pedestal(){return kind().equals("arcane_pedestal")||kind().equals("arcane_platform");}
    public boolean storage(){return Set.of("repository","repository_controller","archwood_chest").contains(kind());}
    public static void tick(Level level,BlockPos pos,BlockState state,NouveauWorkshop be){
        if(!(level instanceof ServerLevel server)||(server.getGameTime()+pos.asLong())%20!=0)return;
        if(be.clientDirty){be.clientDirty=false;level.sendBlockUpdated(pos,state,state,Block.UPDATE_CLIENTS);}
        if(!JugNouveau.enabled()||be.paused){be.setActive(false);be.signal(0,0);be.status="Paused";return;}
        ServerPlayer actor=Authority.answering(server,be.owner);
        if(actor==null||!be.permitted(actor)){be.setActive(false);be.status="Waiting for owner / access";return;}
        be.cycles++;if(be.kind().equals("planarium")&&be.every(10))NouveauPlanarium.refresh(be);
        NouveauSource.tick(server,actor,be);
        if(!NouveauRecipes.forStation(be.kind()).isEmpty())be.craft(server,actor);
        NouveauDevices.tick(server,actor,be);
        NouveauSignals.tick(server,actor,be);
    }
    private record Input(Container container,int slot){}
    private List<Input> inputs(ServerLevel server,ServerPlayer actor){
        List<Input> found=new ArrayList<>();for(int i=0;i<INPUTS;i++)found.add(new Input(this,i));
        if(!Set.of("enchanting_apparatus","imbuement_chamber","alteration_table").contains(kind()))return found;
        for(BlockPos at:BlockPos.betweenClosed(worldPosition.offset(-3,0,-3),worldPosition.offset(3,0,3))){
            if(found.size()>=17)break;
            if(server.hasChunkAt(at)&&server.getBlockEntity(at) instanceof NouveauWorkshop other&&other.pedestal()&&other.permitted(actor))found.add(new Input(other,0));
        }return found;
    }
    private void craft(ServerLevel server,ServerPlayer actor){
        if(kind().equals("enchanting_apparatus")&&(!(server.getBlockEntity(worldPosition.below()) instanceof NouveauWorkshop core)||!core.kind().equals("arcane_core")||!core.permitted(actor))){progress=0;job="";status="Place an Arcane Core beneath the apparatus";setActive(false);return;}
        var available=NouveauRecipes.forStation(kind());List<Input> refs=inputs(server,actor);
        List<ItemStack> stacks=refs.stream().map(r->r.container.getItem(r.slot).copy()).toList();
        NouveauRecipes.Match found=null;
        for(var recipe:available){
            if(!selected.isEmpty()&&!selected.equals(recipe.id()))continue;
            found=NouveauRecipes.match(recipe,stacks,actor);if(found!=null)break;
        }
        if(found==null){progress=0;job="";setActive(false);status="Waiting for recipe inputs";return;}
        if(!job.equals(found.recipe().id())){job=found.recipe().id();progress=0;}
        if(!canOutput(found.output())){status="Output full";setActive(false);return;}
        boolean powered=NouveauSource.canPay(server,actor,this,found.recipe().ley()),passive=found.recipe().operation().equals("imbuement");
        if(!powered&&!passive){status="Needs Ley Charge";setActive(false);return;}
        status=powered?"Working":"Imbuing slowly • Ley Charge speeds this up";setActive(true);progress+=powered?20:2;setChanged();if(progress<100)return;
        // The match, access checks and payment all occur in this one server tick.
        if(powered&&!NouveauSource.pay(server,actor,this,found.recipe().ley()))return;
        for(int i=0;i<refs.size();i++)if(found.used()[i]>0)refs.get(i).container.setItem(refs.get(i).slot,found.after().get(i));
        if(!actor.getAbilities().instabuild&&found.recipe().experience()>0)actor.giveExperiencePoints(-found.recipe().experience());
        output(found.output());progress=0;setChanged();
    }
    public boolean canOutput(ItemStack stack){int remaining=stack.getCount();for(int i=OUTPUT;i<OUTPUT_END;i++){
        var held=items.get(i);if(held.isEmpty())remaining-=stack.getMaxStackSize();
        else if(ItemStack.isSameItemSameComponents(held,stack))remaining-=Math.max(0,held.getMaxStackSize()-held.getCount());
    }return remaining<=0;}
    public boolean output(ItemStack stack){if(!canOutput(stack))return false;ItemStack rest=stack.copy();for(int i=OUTPUT;i<OUTPUT_END&&!rest.isEmpty();i++){
        var held=items.get(i);if(held.isEmpty()){items.set(i,rest.split(Math.min(rest.getCount(),rest.getMaxStackSize())));}
        else if(ItemStack.isSameItemSameComponents(held,rest)){int n=Math.min(rest.getCount(),held.getMaxStackSize()-held.getCount());held.grow(n);rest.shrink(n);}
    }setChanged();return true;}
    public void setActive(boolean active){if(level!=null&&getBlockState().getValue(NouveauWorkshopBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(NouveauWorkshopBlock.ACTIVE,active),Block.UPDATE_CLIENTS);}
    @Override public int[] getSlotsForFace(Direction d){return java.util.stream.IntStream.range(storage()?0:d==Direction.DOWN?OUTPUT:0,storage()?getContainerSize():d==Direction.DOWN?OUTPUT_END:INPUTS).toArray();}
    private boolean automation(@Nullable Direction d){if(!(level instanceof ServerLevel s))return false;var p=Authority.answering(s,owner);return p!=null&&permitted(p)&&(d==null||s.hasChunkAt(worldPosition.relative(d))&&Authority.mayChange(s,p,worldPosition.relative(d)));}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction d){return canPlaceItem(slot,stack)&&(storage()||slot<INPUTS)&&automation(d);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){if(slot>=SLOTS){var b=repository(slot);return accepts(stack)&&b!=null&&b.accepts(stack);}return !storage()||accepts(stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction d){return (storage()||slot>=OUTPUT&&slot<OUTPUT_END)&&automation(d);}
    @Override public int getContainerSize(){return SLOTS*(1+repositories().size());}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty)&&repositories().stream().allMatch(NouveauWorkshop::isEmpty);}
    @Override public ItemStack getItem(int slot){if(slot>=SLOTS){var b=repository(slot);return b==null?ItemStack.EMPTY:b.getItem(slot%SLOTS);}return slot<0?ItemStack.EMPTY:items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){if(slot>=SLOTS){var b=repository(slot);return b==null?ItemStack.EMPTY:b.removeItem(slot%SLOTS,count);}var out=ContainerHelper.removeItem(items,slot,count);if(!out.isEmpty())setChanged();return out;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(slot>=SLOTS){var b=repository(slot);return b==null?ItemStack.EMPTY:b.removeItemNoUpdate(slot%SLOTS);}return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){if(slot>=SLOTS){var b=repository(slot);if(b!=null)b.setItem(slot%SLOTS,stack);return;}items.set(slot,stack);setChanged();}
    @Override public void clearContent(){items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player p){return p instanceof ServerPlayer sp&&mayUse(sp)&&!isRemoved()&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&p.level().getBlockEntity(worldPosition)==this;}
    @Override public void preRemoveSideEffects(BlockPos p,BlockState s){if(level instanceof ServerLevel server){Containers.dropContents(server,p,new SimpleContainer(items.toArray(ItemStack[]::new)));clearContent();}}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);preview=in.read("preview",NouveauPlanarium.Mini.CODEC.listOf()).orElse(List.of()).stream().limit(768).toList();items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);
        owner=in.read("owner",UUIDUtil.CODEC).orElse(null);ley=Math.clamp(in.getLongOr("ley",0),0,capacity());paused=in.getBooleanOr("paused",false);locked=in.getBooleanOr("locked",false);
        selected=in.getStringOr("selected","");customName(in.getStringOr("custom_name",""));job="";progress=0;links.clear();in.read("links",BlockPos.CODEC.listOf()).orElse(List.of()).stream().distinct().filter(p->kind().equals("relay_warp")||p.distSqr(worldPosition)<=1024).limit(8).forEach(links::add);
        ritual=in.getStringOr("ritual","");ritualCursor=Math.clamp(in.getIntOr("ritual_cursor",0),0,1000000);
        ritualRunning=in.getBooleanOr("ritual_running",false);ritualAugments=in.read("ritual_augments",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()).stream().limit(8).toList();
        captured=in.read("captured",net.minecraft.world.item.component.CustomData.CODEC).orElse(null);
        potion=in.read("potion",net.minecraft.world.item.alchemy.PotionContents.CODEC).orElse(net.minecraft.world.item.alchemy.PotionContents.EMPTY);doses=Math.clamp(in.getIntOr("doses",0),0,100);diffuserRemaining=Math.clamp(in.getIntOr("diffusion",0),0,600);
        filter=in.read("filter",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);threshold=Math.clamp(in.getIntOr("threshold",1),1,4096);inverted=in.getBooleanOr("inverted",false);signal=0;signalUntil=0;
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("nouveau_schema",1);ContainerHelper.saveAllItems(out,items);out.store("preview",NouveauPlanarium.Mini.CODEC.listOf(),preview);
        if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putLong("ley",ley);out.putBoolean("paused",paused);out.putBoolean("locked",locked);out.putString("selected",selected);out.store("links",BlockPos.CODEC.listOf(),links);
        out.putString("ritual",ritual);out.putInt("ritual_cursor",ritualCursor);out.putString("custom_name",customName);
        out.putBoolean("ritual_running",ritualRunning);out.store("ritual_augments",ItemStack.OPTIONAL_CODEC.listOf(),ritualAugments);
        if(captured!=null)out.store("captured",net.minecraft.world.item.component.CustomData.CODEC,captured);
        out.store("potion",net.minecraft.world.item.alchemy.PotionContents.CODEC,potion);out.putInt("doses",doses);out.putInt("diffusion",diffuserRemaining);
        out.store("filter",ItemStack.OPTIONAL_CODEC,filter);out.putInt("threshold",threshold);out.putBoolean("inverted",inverted);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder b){super.collectImplicitComponents(b);if(!customName.isEmpty())b.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(customName));b.set(JugcraftConcordance.LEY_CHARGE,(int)ley);if(captured!=null)b.set(NouveauContainment.CAPTURE,captured);if(doses>0){b.set(DataComponents.POTION_CONTENTS,potion);b.set(NouveauPotions.JAR_DOSES,doses);}}
    @Override protected void applyImplicitComponents(DataComponentGetter c){super.applyImplicitComponents(c);ley=Math.clamp(c.getOrDefault(JugcraftConcordance.LEY_CHARGE,0),0,capacity());captured=c.get(NouveauContainment.CAPTURE);potion=c.getOrDefault(DataComponents.POTION_CONTENTS,net.minecraft.world.item.alchemy.PotionContents.EMPTY);doses=Math.clamp(c.getOrDefault(NouveauPotions.JAR_DOSES,0),0,100);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<NouveauWorkshop>("work",4,s->{
        String clip=NouveauPresentation.animation(entry().geo(),getBlockState().getValue(NouveauWorkshopBlock.ACTIVE));
        return clip.isEmpty()?com.geckolib.animation.object.PlayState.STOP:s.setAndContinue(RawAnimation.begin().thenLoop(clip));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
