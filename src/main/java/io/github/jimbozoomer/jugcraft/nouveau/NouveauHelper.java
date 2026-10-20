package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animation.RawAnimation;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.spirits.*;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import org.jspecify.annotations.Nullable;

/** Each helper performs a different bounded workshop job and retains real produced cargo while blocked. */
public final class NouveauHelper extends WorkerEntity<NouveauHelper> {
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> VARIANT=net.minecraft.network.syncher.SynchedEntityData.defineId(NouveauHelper.class,net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private final String species;
    private @Nullable BlockPos home;
    private int scan;
    private final List<ItemStack> cargo=new ArrayList<>();
    private long nextJob;
    private @Nullable BlockPos watchedSapling;
    public NouveauHelper(EntityType<? extends PathfinderMob> t,Level l,String name){super(t,l);species=name;}
    public String species(){return species;}
    public String texture(){return "entity/"+species+switch(species){case "drygmy"->"_"+(entityData.get(VARIANT).isEmpty()?"brown":entityData.get(VARIANT));case "wixie"->"_"+(entityData.get(VARIANT).isEmpty()?"black":entityData.get(VARIANT));case "whirlisprig"->"_"+(entityData.get(VARIANT).isEmpty()?"spring":entityData.get(VARIANT));default->"";};}
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(VARIANT,"");}
    public @Nullable BlockPos home(){return home;}
    public void bind(BlockPos p){home=p.immutable();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,24);}
    @Override public String kind(){return "nouveau_"+species;}
    @Override public Status think(ServerLevel level,long time,long gameTime){
        Status result=work(level,gameTime);if(result!=Status.TRAVELLING)stop();setStatus(result);return result;
    }
    private Status work(ServerLevel level,long now){
        if(!JugNouveau.enabled())return Status.DISABLED;
        if(owner==null)return wild(level,now);
        ServerPlayer actor=Authority.answering(level,owner);if(actor==null)return Status.OWNER_OFFLINE;
        if(home==null||!level.hasChunkAt(home))return Status.DESTINATION_UNLOADED;
        if(!Authority.mayChange(level,actor,home))return Status.BLOCKED_BY_ACCESS;
        NouveauWorkshop station=level.getBlockEntity(home) instanceof NouveauWorkshop n&&n.permitted(actor)?n:null;
        NouveauWorkshop source=station;
        if(source==null)for(Direction direction:Direction.values())if(level.hasChunkAt(home.relative(direction))&&level.getBlockEntity(home.relative(direction)) instanceof NouveauWorkshop n&&n.permitted(actor)){source=n;break;}
        if(!cargo.isEmpty()){
            if(!near(centre(home),2))return walkTo(centre(home),1,1,now)?Status.TRAVELLING:Status.CANNOT_NAVIGATE;
            Iterator<ItemStack> it=cargo.iterator();while(it.hasNext()){var s=it.next();if(!deposit(level,actor,s,station))return Status.FULL;it.remove();}return Status.FINISHED;
        }
        if(now<nextJob)return Status.IDLE;
        boolean free=species.equals("amethyst_golem");if(!free&&(source==null||!NouveauSource.canPay(level,actor,source,2)))return Status.NO_ENERGY;
        boolean worked=switch(species){
            case "drygmy" -> harvestCreatures(level,actor);
            case "whirlisprig" -> tendGarden(level,actor);
            case "amethyst_golem" -> tendAmethyst(level,actor);
            case "wixie" -> station!=null&&craftForWixie(level,actor,station);
            case "alakarkinos" -> collectShards(level,actor);
            default -> false;
        };
        if(worked){if(!free)NouveauSource.pay(level,actor,source,2);nextJob=Math.max(nextJob,now+100);return Status.WORKING;}
        nextJob=now+20;return Status.WAITING_FOR_RESOURCES;
    }
    private boolean deposit(ServerLevel l,ServerPlayer p,ItemStack stack,@Nullable NouveauWorkshop station){
        List<BlockPos> spots=new ArrayList<>();spots.add(home);for(Direction d:Direction.values())spots.add(home.relative(d));
        for(var pos:spots){var container=NouveauStorage.accessible(l,p,pos);if(container==null)continue;
            if(NouveauFilters.frames(l,pos,stack)&&NouveauCrafting.deposit(container,stack,false))return true;
        }return station!=null&&station.accepts(stack)&&station.output(stack);
    }
    private Status wild(ServerLevel l,long now){
        if(now<nextJob)return Status.IDLE;nextJob=now+40;
        for(var drop:l.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(4),i->i.isAlive()).stream().limit(8).toList()){
            boolean offer=species.equals("drygmy")&&drop.getItem().is(NouveauCatalog.item("wilden_horn"))||species.equals("alakarkinos")&&drop.getItem().is(net.minecraft.tags.ItemTags.DECORATED_POT_SHERDS);
            if(!offer)continue;if(!near(drop.position(),1.5))return walkTo(drop.position(),1,1,now)?Status.TRAVELLING:Status.IDLE;
            if(drop.getOwner() instanceof ServerPlayer giver)NouveauAdvancements.award(giver,"poof_mob");
            drop.getItem().shrink(1);if(drop.getItem().isEmpty())drop.discard();spawnAtLocation(l,new ItemStack(NouveauCatalog.item(species.equals("drygmy")?"drygmy_shard":"alakarkinos_token")));discard();return Status.FINISHED;
        }
        if(species.equals("whirlisprig")){
            if(watchedSapling!=null&&l.hasChunkAt(watchedSapling)&&l.getBlockState(watchedSapling).is(net.minecraft.tags.BlockTags.LOGS)){
                for(var witness:l.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(8),p->p.isAlive()&&!p.isSpectator()).stream().limit(16).toList())NouveauAdvancements.award(witness,"poof_mob");
                spawnAtLocation(l,new ItemStack(NouveauCatalog.item("whirlisprig_shards")));discard();return Status.FINISHED;
            }
            if(watchedSapling==null)for(BlockPos at:BlockPos.betweenClosed(blockPosition().offset(-4,-1,-4),blockPosition().offset(4,2,4)))if(l.hasChunkAt(at)&&l.getBlockState(at).getBlock() instanceof SaplingBlock){watchedSapling=at.immutable();break;}
            // Wildlife observes growth. It never changes blocks in someone else's claim.
            if(watchedSapling!=null){if(watchedSapling.distSqr(blockPosition())>100)watchedSapling=null;else return Status.IDLE;}
        }
        var wander=blockPosition().offset(l.getRandom().nextInt(9)-4,0,l.getRandom().nextInt(9)-4);if(l.hasChunkAt(wander))walkTo(centre(wander),.7F,1,now);return Status.TRAVELLING;
    }
    private boolean harvestCreatures(ServerLevel l,ServerPlayer actor){
        var nearby=new ArrayList<Mob>(l.getEntitiesOfClass(Mob.class,new AABB(home).inflate(8),m->m.isAlive()&&!(m instanceof NouveauHelper)&&!(m instanceof io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity<?>)));
        for(var at:BlockPos.betweenClosed(home.offset(-5,-2,-5),home.offset(5,2,5)))if(l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof NouveauWorkshop jar&&jar.kind().equals("mob_jar")&&jar.permitted(actor)){var mob=NouveauContainment.specimen(l,jar);if(mob!=null)nearby.add(mob);if(nearby.size()>=64)break;}
        if(nearby.isEmpty())return false;
        long diversity=nearby.stream().map(Mob::getType).distinct().count();
        for(int i=0;i<Math.min(8,nearby.size());i++){
            var mob=nearby.get(Math.floorMod(scan++,nearby.size()));if(!Authority.mayChange(l,actor,mob.blockPosition()))continue;String type=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            if(!type.startsWith("minecraft:")||mob.isBaby()||Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian","minecraft:villager","minecraft:wandering_trader","minecraft:armor_stand","minecraft:mannequin").contains(type))continue;
            ResourceKey<LootTable> key=ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse(type.replace(":",":entities/")));
            mob.dropFromLootTable(l,l.damageSources().magic(),false,key,item->{if(cargo.size()<15)cargo.add(item.copy());});
            if(cargo.size()<16&&l.getRandom().nextInt(4)==0){var gem=new ItemStack(NouveauCatalog.item("experience_gem"));gem.set(NouveauGear.EXPERIENCE,(int)Math.min(20,2+diversity));cargo.add(gem);}
            nextJob=l.getGameTime()+Math.max(100,400-diversity*20);return true;
        }return false;
    }
    private boolean tendGarden(ServerLevel l,ServerPlayer actor){
        // A Whirlisprig draws inspiration from its garden. It does not destroy the display plants.
        Map<Block,List<BlockPos>> garden=new LinkedHashMap<>();
        for(BlockPos p:BlockPos.betweenClosed(home.offset(-4,-1,-4),home.offset(4,2,4)))if(l.hasChunkAt(p)&&Authority.mayChange(l,actor,p)){
            var state=l.getBlockState(p);var block=state.getBlock();
            if(block instanceof CropBlock crop&&!crop.isMaxAge(state))continue;
            if(block instanceof CropBlock||block instanceof FlowerBlock||block instanceof SaplingBlock||state.is(net.minecraft.tags.BlockTags.LOGS))
                garden.computeIfAbsent(block,k->new ArrayList<>()).add(p.immutable());
        }
        if(garden.size()<3)return false;
        List<BlockPos> choices=garden.values().stream().flatMap(List::stream).toList();
        BlockPos chosen=choices.get(l.getRandom().nextInt(choices.size()));var state=l.getBlockState(chosen);
        var drops=Block.getDrops(state,l,chosen,null,this,ItemStack.EMPTY);
        if(drops.isEmpty())return false;cargo.addAll(drops.stream().limit(16).toList());
        // Diversity lowers the wait; quantity influences which represented plant is chosen.
        nextJob=l.getGameTime()+Math.max(80,400-garden.size()*20);return true;
    }
    private boolean tendAmethyst(ServerLevel l,ServerPlayer actor){
        for(var drop:l.getEntitiesOfClass(ItemEntity.class,new AABB(home).inflate(10),e->e.isAlive()&&e.getItem().is(Items.AMETHYST_SHARD)).stream().limit(8).toList())if(Authority.mayChange(l,actor,drop.blockPosition())){cargo.add(drop.getItem().copy());drop.discard();return true;}
        for(int i=0;i<16;i++){
            int n=Math.floorMod(scan++,9261);BlockPos p=home.offset(n%21-10,(n/441)%21-10,(n/21)%21-10);
            if(!l.hasChunkAt(p)||!Authority.mayChange(l,actor,p))continue;var state=l.getBlockState(p);
            for(var recipe:NouveauRecipes.all())if(recipe.operation().equals("budding_conversion")&&NouveauRecipes.ingredient(recipe.data().get("input"),new ItemStack(state.getBlock()))){var converted=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(recipe.data().get("result").getAsString()));if(converted!=null&&converted!=Blocks.AIR){l.setBlock(p,converted.defaultBlockState(),Block.UPDATE_ALL);return true;}}
            if(state.is(Blocks.AMETHYST_CLUSTER)){cargo.add(new ItemStack(Items.AMETHYST_SHARD,4));l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return true;}
            if(state.is(Blocks.BUDDING_AMETHYST))for(Direction d:Direction.values()){
                var at=p.relative(d);if(!l.hasChunkAt(at)||!Authority.mayChange(l,actor,at))continue;var bud=l.getBlockState(at);Block next=bud.isAir()?Blocks.SMALL_AMETHYST_BUD:bud.is(Blocks.SMALL_AMETHYST_BUD)?Blocks.MEDIUM_AMETHYST_BUD:bud.is(Blocks.MEDIUM_AMETHYST_BUD)?Blocks.LARGE_AMETHYST_BUD:bud.is(Blocks.LARGE_AMETHYST_BUD)?Blocks.AMETHYST_CLUSTER:null;
                if(next!=null){l.setBlock(at,next.defaultBlockState().setValue(AmethystClusterBlock.FACING,d),Block.UPDATE_ALL);return true;}
            }
        }return false;
    }
    private boolean craftForWixie(ServerLevel level,ServerPlayer actor,NouveauWorkshop station){
        // A taught sample in slot 0 selects a normal crafting output. Ingredients are real linked inventories.
        if(level.hasNeighborSignal(station.getBlockPos()))return false;
        return station.sample().has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)?NouveauAlchemy.brew(level,actor,station):NouveauCrafting.craftOne(level,actor,station);
    }
    private boolean collectShards(ServerLevel level,ServerPlayer actor){
        for(int i=0;i<16;i++){
            int n=Math.floorMod(scan++,147);BlockPos at=home.offset(n%7-3,(n/49)%3-1,(n/7)%7-3);
            if(!level.hasChunkAt(at)||!Authority.mayChange(level,actor,at))continue;var state=level.getBlockState(at);
            if(!state.is(Blocks.SAND)&&!state.is(Blocks.GRAVEL))continue;
            var conversions=NouveauRecipes.all().stream().filter(r->r.operation().equals("alakarkinos_conversion")&&NouveauRecipes.ingredient(r.data().get("input"),new ItemStack(state.getBlock()))).toList();
            int total=conversions.stream().mapToInt(r->Math.clamp(r.data().get("weight").getAsInt(),0,10000)).sum();if(total==0)continue;int roll=level.getRandom().nextInt(total);String table="";for(var recipe:conversions){roll-=Math.clamp(recipe.data().get("weight").getAsInt(),0,10000);if(roll<0){table=recipe.data().get("table").getAsString();break;}}
            ResourceKey<LootTable> key=ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse(table));
            var params=new LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(at)).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ARCHAEOLOGY);
            var loot=level.getServer().reloadableRegistries().getLootTable(key).getRandomItems(params);
            if(loot.size()>16)return false;
            level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);cargo.addAll(loot);return true;
        }return false;
    }
    @Override protected RawAnimation animation(Status s){return RawAnimation.begin().thenLoop(s==Status.TRAVELLING?(species.equals("whirlisprig")?"fly":species.equals("wixie")?"idle":"run"):s==Status.WORKING?switch(species){case "drygmy"->"channel";case "amethyst_golem"->"harvest";case "wixie"->"cast";default->"idle";}:species.equals("amethyst_golem")?"carry_idle":species.equals("drygmy")?"channel":"idle");}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        if(p.getItemInHand(hand).is(JugNouveau.WAND))return JugNouveau.WAND.interactLivingEntity(p.getItemInHand(hand),p,this,hand);
        if(!(p instanceof ServerPlayer actor))return InteractionResult.SUCCESS;
        if(owner==null||!(owner.equals(p.getUUID())||JugcraftParties.sameParty(owner,p.getUUID()))||!Authority.mayChange(actor.level(),actor,blockPosition()))return InteractionResult.FAIL;
        var color=io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock.dyeColor(p.getItemInHand(hand));
        if(color!=null){String variant=color.getName();if(species.equals("whirlisprig"))variant=switch(variant){case "orange"->"autumn";case "yellow"->"summer";case "white"->"winter";default->"spring";};
            boolean valid=species.equals("drygmy")&&Set.of("brown","cyan","orange").contains(variant)||species.equals("wixie")&&Set.of("black","white","red","blue","green").contains(variant)||species.equals("whirlisprig");
            if(valid){entityData.set(VARIANT,variant);p.getItemInHand(hand).consume(1,p);return InteractionResult.SUCCESS;}}
        if(p.isShiftKeyDown()&&p.getItemInHand(hand).isEmpty()){discard();return InteractionResult.SUCCESS;}
        return super.mobInteract(p,hand);
    }
    @Override public void remove(RemovalReason reason){
        if(reason.shouldDestroy()&&!NouveauContainment.capturing(this)&&!isRemoved()&&level() instanceof ServerLevel l){
            cargo.forEach(s->spawnAtLocation(l,s));cargo.clear();if(owner!=null){spawnAtLocation(l,new ItemStack(NouveauCatalog.item(species+"_charm")));WorkerRoster.of(l.getServer()).remove(owner,getUUID());owner=null;}
        }super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(home!=null)out.store("home",BlockPos.CODEC,home);out.store("cargo",ItemStack.CODEC.listOf(),cargo);out.putInt("scan",scan);out.putLong("next_job",nextJob);out.putString("variant",entityData.get(VARIANT));}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);home=in.read("home",BlockPos.CODEC).orElse(null);cargo.clear();cargo.addAll(in.read("cargo",ItemStack.CODEC.listOf()).orElse(List.of()).stream().limit(16).toList());scan=Math.floorMod(in.getIntOr("scan",0),9261);nextJob=in.getLongOr("next_job",0);entityData.set(VARIANT,in.getStringOr("variant",""));}
}
