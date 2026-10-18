package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import org.jspecify.annotations.Nullable;

/** Seven real slots, one staggered second pulse and no work for absent owners or unloaded chunks. */
public final class AlchemyMachinesBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOTS=7, STAR=0, FIRST_INPUT=1, FIRST_OUTPUT=4;
    private static final int[] INPUT={1,2,3},OUTPUT={4,5,6},NONE={};
    public static final int READY=0,PAUSED=1,OWNER_AWAY=2,DENIED=3,RESEARCH=4,DISABLED=5,UNBALANCED=6,
        WORKING=7,EMPTY=8,FULL=9,NO_ESSENCE=10,BAD_ARTIFACT=11,ARMED=12;
    private NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private @Nullable UUID owner;
    private int essence,status,progress,scanCursor,range=6,lastSpent,lastTargets;
    private float experience;
    private boolean paused, authorized;
    private long lastBonusSecond=Long.MIN_VALUE;
    public boolean bonusWork(){if(level==null||lastBonusSecond==level.getGameTime()/20||!readyOwner())return false;lastBonusSecond=level.getGameTime()/20;return furnaceStep();}
    private ItemStack displayedArtifact=ItemStack.EMPTY,lastSentArtifact=ItemStack.EMPTY;
    public AlchemyMachinesBlockEntity(BlockPos pos,BlockState state){super(AlchemyMachines.ENTITY,pos,state);}
    public AlchemyMachines.Kind kind(){return ((AlchemyMachinesBlock)getBlockState().getBlock()).kind;}
    public int tier(){return ((AlchemyMachinesBlock)getBlockState().getBlock()).tier;}
    public int capacity(){return switch(kind()){case RELAY->new int[]{16384,65536,262144}[tier()-1];case FURNACE->tier()==1?65536:262144;default->16384;};}
    public int essence(){return essence;}
    public int status(){return status;}
    public int progress(){return progress;}
    public int range(){return range;} public int lastTargets(){return lastTargets;} public void recordTargets(int n){lastTargets+=n;} public int lastSpent(){return lastSpent;}
    public void cycleRange(){range=range>=6?2:range+2;setChanged();if(level instanceof ServerLevel server)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    public String artifactId(){return BuiltInRegistries.ITEM.getKey(items.get(1).getItem()).getPath();}
    public boolean paused(){return paused;}
    public @Nullable UUID owner(){return owner;}
    /** Public pedestal appearance only: no inventory payload, private item data, charge or owner UUID. */
    public ItemStack displayArtifact(){return (level!=null&&level.isClientSide()?displayedArtifact:visualArtifact()).copy();}
    private ItemStack visualArtifact(){return kind()==AlchemyMachines.Kind.PEDESTAL&&pedestalArtifact(items.get(1))?new ItemStack(items.get(1).getItem()):ItemStack.EMPTY;}
    public void setEssence(int value){essence=Math.clamp(value,0,capacity());setChanged();}
    public void setPaused(boolean value){paused=value;status=value?PAUSED:READY;if(level instanceof ServerLevel server&&getBlockState().getValue(AlchemyMachinesBlock.ACTIVE))server.setBlock(worldPosition,getBlockState().setValue(AlchemyMachinesBlock.ACTIVE,false),Block.UPDATE_CLIENTS);setChanged();}
    public void placedBy(ServerPlayer player){owner=player.getUUID();authorized=true;setChanged();}
    private boolean related(UUID by){return owner!=null&&(owner.equals(by)||JugcraftParties.sameParty(owner,by));}
    public boolean mayUse(ServerPlayer player){return related(player.getUUID())&&player.isAlive()&&!player.isSpectator()&&level instanceof ServerLevel server&&Authority.mayChange(server,player,worldPosition);}
    @Override public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3.atCenterOf(worldPosition))<=64&&(!(p instanceof ServerPlayer player)||mayUse(player));}
    private int gate(@Nullable ServerPlayer player){
        if(!Assaying.enabled())return DISABLED;
        if(paused||level!=null&&level.hasNeighborSignal(worldPosition))return PAUSED;
        if(player==null||owner==null||!owner.equals(player.getUUID())||!player.isAlive()||player.isSpectator())return OWNER_AWAY;
        // Possessing/placing the device is enough; research is optional guidance.
        if(!Authority.isStandIn(player))authorized=true;
        if(!(level instanceof ServerLevel server)||!Assaying.problems(server).isEmpty())return UNBALANCED;
        return Authority.mayChange(server,player,worldPosition)?READY:DENIED;
    }
    private ServerPlayer actor(ServerLevel server){
        return kind()==AlchemyMachines.Kind.FURNACE||kind()==AlchemyMachines.Kind.RELAY?AlchemyOwner.factory(server,owner,authorized):Authority.present(server,owner);
    }
    public boolean readyOwner(){return level instanceof ServerLevel server&&gate(actor(server))==READY;}
    /** Called only by a touching workshop, with both end-point permissions checked. Never draws an unloaded owner. */
    public int acceptEssence(ServerPlayer from,int offered){
        if(offered<=0||!related(from.getUUID())||!(level instanceof ServerLevel server)||!Authority.mayChange(server,from,worldPosition)
                ||gate(owner.equals(from.getUUID())?from:actor(server))!=READY||kind()==AlchemyMachines.Kind.NOVA)return 0;
        int accepted=Math.min(offered,capacity()-essence);setEssence(essence+accepted);return accepted;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,AlchemyMachinesBlockEntity be){
        if(level instanceof ServerLevel server&&Math.floorMod(level.getGameTime()+pos.asLong(),20)==0)be.pulse(be.actor(server));
    }
    public void pulse(@Nullable ServerPlayer player){
        if(!(level instanceof ServerLevel server))return;
        if(player==null)player=actor(server);
        status=gate(player);boolean worked=false;
        if(status==READY){
            if(kind()==AlchemyMachines.Kind.RELAY)worked=relay(player);
            else if(kind()==AlchemyMachines.Kind.NOVA)status=ARMED;
            else{
                // The supply socket never dissolves its star and never draws beyond available buffer capacity.
                int moved=Math.min(Math.min(KleinStars.stored(items.get(STAR)),tier()==1?4096:32768),capacity()-essence);
                if(moved>0){KleinStars.setStored(items.get(STAR),KleinStars.stored(items.get(STAR))-moved);essence+=moved;}
                worked=switch(kind()){case FURNACE->furnaceStep();case PEDESTAL->pedestal(player);case TORCH->ward(player);default->false;};
            }
        }
        if(getBlockState().getValue(AlchemyMachinesBlock.ACTIVE)!=worked)server.setBlock(worldPosition,getBlockState().setValue(AlchemyMachinesBlock.ACTIVE,worked),Block.UPDATE_CLIENTS);
        setChanged();
    }
    private boolean relay(ServerPlayer player){
        ServerLevel server=(ServerLevel)level;boolean worked=false;int budget=8<<(tier()-1);
        for(int i=1;i<4&&budget>0;i++){
            var in=items.get(i);var m=AlchemicalWorkshopBlockEntity.material(in);if(m==null||!m.dissolvable())continue;
            int n=Math.min(in.getCount(),budget);while(n>0&&Assay.dissolve(m,n)>capacity()-essence)n--;
            long gain=Assay.dissolve(m,n);if(gain<=0)continue;in.shrink(n);essence+=(int)gain;budget-=n;worked=true;
        }
        budget=Math.min(essence,256<<(2*(tier()-1)));
        // A relay has one clearly modelled output: the face with its front texture. This prevents network search/loops.
        BlockPos to=worldPosition.relative(getBlockState().getValue(AlchemyMachinesBlock.FACING));int moved=0;
        if(budget>0&&server.hasChunkAt(to)&&Authority.mayChange(server,player,to)){
            if(server.getBlockEntity(to) instanceof AlchemicalWorkshopBlockEntity other)moved=other.acceptEssence(player,budget);
            else if(server.getBlockEntity(to) instanceof AlchemyMachinesBlockEntity other)moved=other.acceptEssence(player,budget);
        }
        essence-=moved;budget-=moved;worked|=moved>0;
        var star=items.get(STAR);moved=Math.min(budget,KleinStars.capacity(star)-KleinStars.stored(star));
        if(moved>0){KleinStars.setStored(star,KleinStars.stored(star)+moved);essence-=moved;worked=true;}
        status=worked?WORKING:essence==capacity()?FULL:EMPTY;return worked;
    }
    private ItemStack cooked(ItemStack in){
        if(!(level instanceof ServerLevel server)||in.isEmpty()||!ItemStack.isSameItemSameComponents(in,new ItemStack(in.getItem())))return ItemStack.EMPTY;
        return server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(in),server)
            .map(r->r.value().assemble(new SingleRecipeInput(in))).orElse(ItemStack.EMPTY);
    }
    private int outputSlot(ItemStack out){
        if(out.isEmpty()||out.getCount()>out.getMaxStackSize())return -1;
        for(int i=4;i<SLOTS;i++){var existing=items.get(i);if(existing.isEmpty()||ItemStack.isSameItemSameComponents(existing,out)&&existing.getCount()+out.getCount()<=existing.getMaxStackSize())return i;}return -1;
    }
    /** One normal processing pulse. Exposed for the bounded Watch; caller already checks owner authority. */
    public boolean furnaceStep(){
        if(!(level instanceof ServerLevel server)||kind()!=AlchemyMachines.Kind.FURNACE)return false;
        int first=-1;boolean hasInput=false;
        for(int i=1;i<4;i++){var result=cooked(items.get(i));if(result.isEmpty())continue;hasInput=true;if(outputSlot(result)>=0){first=i;break;}}
        if(!hasInput){progress=0;status=EMPTY;return false;}
        if(first<0){status=FULL;return false;}
        if(essence<16){status=NO_ESSENCE;return false;}
        progress+=20;int duration=tier()==1?40:20;
        if(progress<duration){status=WORKING;setChanged();return true;}
        progress=0;boolean worked=false;
        for(int work=0;work<(tier()==1?1:4)&&essence>=16;work++){
            int input=-1,outSlot=-1;ItemStack out=ItemStack.EMPTY;
            for(int i=1;i<4;i++){var result=cooked(items.get(i));int slot=result.isEmpty()?-1:outputSlot(result);if(slot>=0){input=i;outSlot=slot;out=result;break;}}
            if(input<0)break;
            var recipe=server.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(items.get(input)),server);
            if(recipe.isEmpty())break;
            if(items.get(outSlot).isEmpty())items.set(outSlot,out.copy());else items.get(outSlot).grow(out.getCount());
            items.get(input).shrink(1);essence-=16;experience+=recipe.get().value().experience();worked=true;
        }
        status=worked?WORKING:NO_ESSENCE;setChanged();return worked;
    }
    public void claimExperience(ServerPlayer p){if(!mayUse(p)||!(level instanceof ServerLevel server))return;int whole=(int)experience;if(whole>0){experience-=whole;ExperienceOrb.award(server,p.position(),whole);setChanged();}}
    public static boolean pedestalArtifact(ItemStack stack){return Set.of("black_hole_band","archangel_smite","ignition_ring","zero_ring","watch_of_flowing_time","mind_stone","repair_talisman","harvest_goddess_band","body_stone","soul_stone","life_stone","swiftwolf_rending_gale").contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())&&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("jugcraft");}
    private boolean pedestal(ServerPlayer player){
        var artifact=items.get(1);if(!pedestalArtifact(artifact)){status=BAD_ARTIFACT;return false;}
        String id=BuiltInRegistries.ITEM.getKey(artifact.getItem()).getPath();boolean worked=false;int before=essence;lastTargets=0;lastSpent=0;
        if(AlchemyPedestalEffects.EXTRA.contains(id))worked=AlchemyPedestalEffects.run(this,player,id);
        else if(id.equals("harvest_goddess_band")){
            ServerLevel server=(ServerLevel)level;
            // Persisted rolling cursor: at most49 cells each second, at most4 growths, no full farm scan.
            for(int n=0,changed=0;n<49&&changed<4;n++){
                int cell=scanCursor;scanCursor=(scanCursor+1)%147;BlockPos pos=worldPosition.offset(cell%7-3,cell/49-1,(cell/7)%7-3);
                if(essence<16||!server.hasChunkAt(pos)||Math.abs(pos.getX()-worldPosition.getX())>range||Math.abs(pos.getZ()-worldPosition.getZ())>range)continue;var state=server.getBlockState(pos);
                if(AlchemyPedestalEffects.harvest(this,player,pos,state)){worked=true;changed++;lastTargets++;continue;}
                if(state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state)&&state.canSurvive(server,pos)&&Authority.mayChange(server,player,pos)){
                    if(crop instanceof io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock organism){
                        if(!Authority.mayChange(server,player,pos.below())||organism.hasten(server,pos,state,server.getRandom())!=io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock.Growth.GREW)continue;
                    }else server.setBlock(pos,crop.getStateForAge(crop.getAge(state)+1),Block.UPDATE_ALL);
                    essence-=16;worked=true;changed++;lastTargets++;
                }
            }
        }else if(id.equals("swiftwolf_rending_gale"))worked=ward(player);
        else{
            List<ServerPlayer> nearby=new ArrayList<>();
            ((ServerLevel)level).getEntities(EntityTypeTest.forClass(ServerPlayer.class),new AABB(worldPosition).inflate(range),
                target->related(target.getUUID())&&target.isAlive()&&!target.isSpectator()&&target.distanceToSqr(Vec3.atCenterOf(worldPosition))<=range*range,nearby,8);
            for(var target:nearby){
                int beforeTarget=essence;
                if(!Authority.mayChange((ServerLevel)level,player,target.blockPosition()))continue;
                if(id.equals("repair_talisman")){
                    for(var slot:new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                        var gear=target.getItemBySlot(slot);if(!gear.isDamageableItem()||!gear.isDamaged()||!gear.has(DataComponents.REPAIRABLE)||gear.has(DataComponents.UNBREAKABLE))continue;
                        int repaired=Math.min(Math.min(4,gear.getDamageValue()),essence);if(repaired>0){gear.setDamageValue(gear.getDamageValue()-repaired);essence-=repaired;target.getInventory().setChanged();worked=true;}break;
                    }
                }else{
                    if(!id.equals("body_stone")&&essence>=32&&target.getHealth()<target.getMaxHealth()){target.heal(1);essence-=32;worked=true;}
                    if(!id.equals("soul_stone")&&essence>=32&&target.getFoodData().needsFood()){target.getFoodData().eat(1,0);essence-=32;worked=true;}
                }
                if(essence<beforeTarget)lastTargets++;
            }
        }
        lastSpent=Math.max(0,before-essence);status=worked?WORKING:essence<4?NO_ESSENCE:READY;return worked;
    }
    private boolean ward(ServerPlayer player){
        if(essence<4){status=NO_ESSENCE;return false;}ServerLevel server=(ServerLevel)level;
        List<LivingEntity> mobs=new ArrayList<>();server.getEntities(EntityTypeTest.forClass(LivingEntity.class),new AABB(worldPosition).inflate(range),m->m instanceof Enemy&&m.isAlive(),mobs,16);
        boolean worked=false;int count=0;
        var cause=Cause.of(player.getUUID(),Cause.Origin.SHRINE,BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString(),ConcordanceEffects.nextSerial());
        var ledger=new Ledger(new Ledger.Limits(8,8*EffectKind.MOVEMENT.work,0));
        var context=new ConcordanceEffects.Context(server,cause,player,ledger,"ward_push",Vec3.atCenterOf(worldPosition));
        for(var mob:mobs){
            if(count>=8||essence<4)break;Vec3 delta=mob.position().subtract(Vec3.atCenterOf(worldPosition));
            if(delta.lengthSqr()>range*range||!Authority.mayChange(server,player,mob.blockPosition())||!Authority.mayStrike(player,mob)
                ||server.clip(new ClipContext(Vec3.atCenterOf(worldPosition).add(0,.75,0),mob.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS)continue;
            if(ConcordanceEffects.apply(context,EffectSpec.of(EffectKind.MOVEMENT,Intent.HARMFUL,5,0),mob).applied()){
                essence-=4;worked=true;count++;lastTargets++;
            }
        }
        status=worked?WORKING:READY;return worked;
    }
    /** Explicit owner action only. No fuse, fire, entity damage, chain reaction, foreign inventories or claim bypass. */
    public boolean activateNova(ServerPlayer player){
        if(kind()!=AlchemyMachines.Kind.NOVA||!stillValid(player)||!owner.equals(player.getUUID())||gate(player)!=READY||!(level instanceof ServerLevel server))return false;
        int price=tier()==1?4096:16384;if(Assaying.balance(player)<price){status=NO_ESSENCE;return false;}
        int radius=tier()==1?2:3,limit=tier()==1?64:192;List<BlockPos> targets=new ArrayList<>();var pick=new ItemStack(Items.NETHERITE_PICKAXE);
        for(BlockPos cursor:BlockPos.betweenClosed(worldPosition.offset(-radius,-radius,-radius),worldPosition.offset(radius,radius,radius))){
            if(targets.size()>=limit)break;BlockPos pos=cursor.immutable();if(pos.equals(worldPosition)||!server.hasChunkAt(pos))continue;
            var state=server.getBlockState(pos);float hardness=state.getDestroySpeed(server,pos);
            if(state.isAir()||state.hasBlockEntity()||!state.getFluidState().isEmpty()||hardness<0||hardness>20||state.getBlock() instanceof AlchemyMachinesBlock||!Authority.mayChange(server,player,pos)
                ||state.requiresCorrectToolForDrops()&&!pick.isCorrectToolForDrops(state))continue;
            targets.add(pos);
        }
        if(targets.isEmpty())return false;
        Assaying.setBalance(player,Assaying.balance(player)-price);
        int cleared=0;
        for(var pos:targets){var state=server.getBlockState(pos);var drops=Block.getDrops(state,server,pos,null,player,pick);if(server.removeBlock(pos,false)){for(var drop:drops)Block.popResource(server,pos,drop);if(cleared++<16)server.levelEvent(2001,pos,Block.getId(state));}}
        AlchemyArtifacts.sound(player,"nova");
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.nova.cleared",cleared,price));
        // Consume the actual charge block, dropping its inventory (normally empty) exactly once through vanilla removal.
        server.removeBlock(worldPosition,false);return true;
    }
    private boolean automationAllowed(@Nullable Direction side){
        if(!(level instanceof ServerLevel server)||(kind()!=AlchemyMachines.Kind.FURNACE&&kind()!=AlchemyMachines.Kind.RELAY))return false;
        ServerPlayer p=actor(server);BlockPos neighbour=side==null?worldPosition:worldPosition.relative(side);
        return gate(p)==READY&&server.hasChunkAt(neighbour)&&Authority.mayChange(server,p,neighbour);
    }
    @Override public int[] getSlotsForFace(Direction side){return kind()==AlchemyMachines.Kind.FURNACE?(side==Direction.DOWN?OUTPUT:INPUT):kind()==AlchemyMachines.Kind.RELAY&&side!=Direction.DOWN?INPUT:NONE;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){return slot>=1&&slot<4&&side!=Direction.DOWN&&automationAllowed(side)&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return kind()==AlchemyMachines.Kind.FURNACE&&slot>=4&&side==Direction.DOWN&&automationAllowed(side);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        if(kind()==AlchemyMachines.Kind.NOVA)return false;if(slot==0)return KleinStars.capacity(stack)>0;
        if(kind()==AlchemyMachines.Kind.PEDESTAL)return slot==1&&pedestalArtifact(stack);
        if(slot<1||slot>=4)return false;
        if(kind()==AlchemyMachines.Kind.FURNACE)return !cooked(stack).isEmpty();
        var material=AlchemicalWorkshopBlockEntity.material(stack);return kind()==AlchemyMachines.Kind.RELAY&&material!=null&&material.dissolvable();
    }
    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){var result=ContainerHelper.takeItem(items,slot);if(!result.isEmpty())setChanged();return result;}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){if(level instanceof ServerLevel server){Containers.dropContents(server,pos,this);clearContent();int whole=(int)experience;if(whole>0)ExperienceOrb.award(server,Vec3.atCenterOf(pos),whole);experience=0;}}
    @Override protected void loadAdditional(ValueInput input){
        if(input.getBooleanOr("display_only",false)){range=Math.clamp(input.getIntOr("effect_range",6),2,6);displayedArtifact=input.read("display_artifact",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);return;}
        super.loadAdditional(input);items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);ContainerHelper.loadAllItems(input,items);owner=input.read("owner",UUIDUtil.CODEC).orElse(null);authorized=owner!=null /* Upgrade old research-locked factories without changing their owner. */;range=Math.clamp(input.getIntOr("effect_range",6),2,6);essence=Math.clamp(input.getIntOr("essence",0),0,capacity());paused=input.getBooleanOr("paused",false);progress=Math.clamp(input.getIntOr("progress",0),0,40);experience=Math.max(0,input.getFloatOr("experience",0));scanCursor=Math.floorMod(input.getIntOr("scan_cursor",0),147);displayedArtifact=visualArtifact();lastSentArtifact=displayedArtifact.copy();
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);Saved.stamp(out,1);ContainerHelper.saveAllItems(out,items);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("effect_range",range);out.putBoolean("factory_authorized",authorized);out.putInt("essence",essence);out.putBoolean("paused",paused);out.putInt("progress",progress);out.putFloat("experience",experience);out.putInt("scan_cursor",scanCursor%147);}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder c){super.collectImplicitComponents(c);c.set(AlchemicalWorkshops.ESSENCE,essence);}
    @Override protected void applyImplicitComponents(DataComponentGetter c){super.applyImplicitComponents(c);essence=Math.clamp(c.getOrDefault(AlchemicalWorkshops.ESSENCE,0),0,capacity());}
    @Override public void setChanged(){
        super.setChanged();if(!(level instanceof ServerLevel server)||kind()!=AlchemyMachines.Kind.PEDESTAL)return;
        var visual=visualArtifact();if(!ItemStack.matches(visual,lastSentArtifact)){lastSentArtifact=visual;server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,registries);out.putBoolean("display_only",true);out.putInt("effect_range",range);out.store("display_artifact",ItemStack.OPTIONAL_CODEC,visualArtifact());return out.buildResult();
    }
}
