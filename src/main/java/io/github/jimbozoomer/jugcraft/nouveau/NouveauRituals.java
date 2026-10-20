package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyArtifacts;
import io.github.jimbozoomer.jugcraft.weapons.ArmItem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Saved, bounded ritual jobs. Every change rechecks loaded chunks and the owner's current permission. */
public final class NouveauRituals {
    private static final Set<String> CONTINUOUS=Set.of("fertility","flight","gravity","harvest","overgrowth","restoration","sanctuary","disintegration","containment");
    private static final Map<NouveauWorkshop,Long> SANCTUARY_PAID=new WeakHashMap<>();
    private static final Set<NouveauWorkshop> SANCTUARIES=Collections.newSetFromMap(new WeakHashMap<>());
    private NouveauRituals(){}
    public static void register(){for(var e:NouveauCatalog.ITEM_ENTRIES.values())if(e.kind().equals("ritual"))NouveauCatalog.registerItem(e,p->new Tablet(e.source().substring(7),p));}
    public static final class Tablet extends Item {
        public final String ritual;
        public Tablet(String ritual,Properties p){super(p);this.ritual=ritual;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p)||!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof NouveauWorkshop brazier))return InteractionResult.PASS;
            if(!brazier.kind().equals("ritual_brazier")||!brazier.mayUse(p)||!brazier.ritual().isEmpty())return InteractionResult.FAIL;
            brazier.prepareRitual(ritual);c.getItemInHand().consume(1,p);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Add augments, then use the brazier with an empty hand to begin."));return InteractionResult.SUCCESS;
        }
    }
    public static void tick(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        String r=b.ritual();if(r.isEmpty())return;
        if(!b.ritualRunning()){b.status("Add augments; empty-hand use to begin");for(var drop:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(b.getBlockPos()).inflate(1.5),e->e.isAlive()).stream().limit(8).toList()){if(!Authority.mayChange(l,p,drop.blockPosition()))continue;for(int i=1;i<9;i++)if(b.getItem(i).isEmpty()){b.setItem(i,drop.getItem().split(1));if(drop.getItem().isEmpty())drop.discard();break;}}return;}
        if(r.equals("sanctuary")){SANCTUARIES.add(b);b.setActive(true);b.status("Preventing hostile natural spawns");return;}
        if(r.equals("overgrowth")&&NouveauRitualWork.count(b,Items.BONE_BLOCK)>0){if(NouveauSource.canPay(l,p,b,1)&&NouveauRitualWork.growBabies(l,p,b))NouveauSource.pay(l,p,b,1);return;}
        if(!NouveauSource.canPay(l,p,b,1)){b.setActive(false);b.status("Ritual needs Ley Charge");return;}
        int cursor=b.ritualCursor();boolean worked=false,done=false;BlockPos origin=NouveauRitualWork.center(b);
        switch(r){
            case "sunrise","moonfall" -> {
                var clock=l.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
                long time=l.getOverworldClockTime(),phase=r.equals("sunrise")?1000:13000;
                l.clockManager().setTotalTicks(clock,time+Math.floorMod(phase-time,24000));worked=done=true;
            }
            case "cloudshaping" -> {
                boolean rain=NouveauRitualWork.count(b,Items.GUNPOWDER)>0||NouveauRitualWork.count(b,Items.LAPIS_BLOCK)>0,storm=NouveauRitualWork.count(b,Items.LAPIS_BLOCK)>0;
                var weather=l.getWeatherData();weather.setRaining(rain);weather.setThundering(storm);weather.setRainTime(12000);weather.setThunderTime(12000);weather.setClearWeatherTime(rain?0:12000);worked=done=true;
            }
            case "challenge" -> {if(l.isVillage(origin)){int omen=Math.clamp(1+NouveauRitualWork.count(b,Items.EMERALD),1,5);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BAD_OMEN,1200,omen-1));worked=l.getRaids().createOrExtendRaid(p,origin)!=null;}done=true;}
            case "animal_summon" -> {worked=spawnAnimal(l,p,b);done=true;}
            case "wilden_summon" -> {worked=NouveauEncounters.summon(l,p,b);done=worked;}
            case "awakening" -> {worked=NouveauEncounters.awaken(l,p,b);done=worked;}
            case "binding" -> {worked=NouveauFamiliars.bind(l,p,b);done=worked;}
            case "warping" -> {
                worked=NouveauRitualWork.warp(l,p,b);
                done=true;
            }
            case "containment" -> {
                for(BlockPos at:BlockPos.betweenClosed(origin.offset(-3,-1,-3),origin.offset(3,1,3))){
                    if(!l.hasChunkAt(at)||!(l.getBlockEntity(at) instanceof NouveauWorkshop jar)||!jar.kind().equals("mob_jar")||jar.captured()!=null)continue;
                    if(NouveauContainment.lightning(l,p,jar)){worked=true;break;}
                    for(var mob:l.getEntitiesOfClass(Mob.class,new AABB(at).inflate(3),m->m.isAlive()).stream().limit(8).toList())if(NouveauContainment.capture(l,p,jar,mob)){worked=true;break;}
                    if(worked)break;
                }
            }
            case "fertility" -> {
                var animals=l.getEntitiesOfClass(Animal.class,new AABB(origin).inflate(7),Animal::isAlive);
                if(animals.size()<20)for(var a:animals.stream().limit(4).toList())if(a.canFallInLove()&&Authority.mayChange(l,p,a.blockPosition())){a.setInLove(p);worked=true;}
            }
            case "restoration" -> {
                for(var e:nearby(l,b,6))worked|=NouveauRitualWork.restore(l,p,e);
            }
            case "flight" -> {
                for(var e:nearby(l,b,8))if(e instanceof ServerPlayer player&&(player==p||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(p.getUUID(),player.getUUID()))){AlchemyArtifacts.grantRitualFlight(player,60);worked=true;}
            }
            case "gravity" -> {for(var e:nearby(l,b,8))if(ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)){NouveauStatuses.give(e,"gravity",60,0,p);worked=true;}}
            case "disintegration" -> {
                for(var e:nearby(l,b,5))if(e instanceof Enemy&&!(e instanceof NouveauBeast beast&&beast.boss())&&!Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian").contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())&&ConcordanceEffects.mayHarm(p,e)&&Authority.mayStrike(p,e)){
                    var gem=new ItemStack(NouveauCatalog.item("experience_gem"));gem.set(NouveauGear.EXPERIENCE,Math.clamp(e.getExperienceReward(l,p)*2,0,10000));
                    if(b.canOutput(gem)){e.discard();b.output(gem);worked=true;break;}
                }
            }
            case "sanctuary" -> {SANCTUARIES.add(b);worked=true;}
            case "burrowing" -> {
                int depth=cursor/4,side=cursor%4;var at=origin.offset(side%2+1,-depth,side/2+1);
                if(at.getY()<=l.getMinY())done=true;
                else if(l.hasChunkAt(at)&&Authority.mayChange(l,p,at)){
                    var s=l.getBlockState(at);if(s.getDestroySpeed(l,at)>=0&&!s.isAir()){l.destroyBlock(at,true,p);worked=true;}cursor++;
                }
            }
            case "conjure_island_desert","conjure_island_plains" -> {
                int radius=NouveauRitualWork.radius(b,7,16),width=radius*2+1;
                for(int i=0;i<8&&cursor<width*width*3;i++,cursor++){
                    int x=cursor%width-radius,z=(cursor/width)%width-radius,y=-(cursor/(width*width));if(x*x+z*z>radius*radius)continue;
                    BlockPos at=origin.offset(x,y,z);if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at)||!l.getBlockState(at).canBeReplaced())continue;
                    boolean mesa=NouveauRitualWork.count(b,Items.TERRACOTTA)>0,snow=NouveauRitualWork.count(b,NouveauCatalog.item("frostaya_pod"))>0;Block material=r.endsWith("desert")?(mesa?Blocks.TERRACOTTA:y==0?Blocks.SAND:Blocks.SANDSTONE):(snow&&y==0?Blocks.SNOW_BLOCK:y==0?Blocks.GRASS_BLOCK:Blocks.DIRT);l.setBlock(at,material.defaultBlockState(),Block.UPDATE_ALL);worked=true;
                }done=cursor>=width*width*3;if(done){NouveauRitualWork.islandBiome(l,p,b,r.endsWith("desert"));worked=true;}
            }
            case "scrying" -> {
                Block chosen=null;for(int i=0;i<8;i++)if(b.ritualAugment(i).getItem() instanceof BlockItem block){chosen=block.getBlock();break;}
                if(chosen!=null){int duration=NouveauRitualWork.count(b,NouveauCatalog.item("manipulation_essence"))>0?18000:6000;
                    for(var viewer:l.getEntitiesOfClass(ServerPlayer.class,new AABB(origin).inflate(8),v->v.isAlive()&&!v.isSpectator()).stream().limit(16).toList())if(viewer==p||io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(p.getUUID(),viewer.getUUID())){NouveauScrySight.begin(viewer,chosen,duration);worked=true;}
                }done=worked;
            }
            case "harvest","overgrowth","flowering","forestation" -> {
                for(int i=0;i<8;i++,cursor++){
                    int radius=NouveauRitualWork.radius(b,r.equals("forestation")?3:7,16),width=radius*2+1,n=Math.floorMod(cursor,width*width*3);BlockPos at=origin.offset(n%width-radius,(n/(width*width))%3-1,(n/width)%width-radius);
                    if(!l.hasChunkAt(at)||!Authority.mayChange(l,p,at))continue;var s=l.getBlockState(at);
                    if(r.equals("harvest")&&s.getBlock() instanceof CropBlock crop&&crop.isMaxAge(s)){worked|=NouveauRitualWork.harvest(l,p,b,at);}
                    if(r.equals("overgrowth")&&s.getBlock() instanceof BonemealableBlock grow&&grow.isValidBonemealTarget(l,at,s,net.minecraft.world.level.block.BonemealSource.INTERACTION)){grow.performBonemeal(l,l.getRandom(),at,s,net.minecraft.world.level.block.BonemealSource.INTERACTION);worked=true;}
                    if(r.equals("flowering"))worked|=NouveauRitualWork.flower(l,p,b,at,cursor);
                    if(r.equals("forestation"))worked|=NouveauRitualWork.forest(l,p,b,at,cursor);

                }done=!CONTINUOUS.contains(r)&&cursor>=Math.pow(NouveauRitualWork.radius(b,r.equals("forestation")?3:7,16)*2+1,2)*3;
            }
            default -> {b.status("Unknown ritual: "+r);return;}
        }
        if(worked){NouveauSource.pay(l,p,b,1);b.setActive(true);b.status("Ritual: "+r.replace('_',' '));}
        b.ritual(done?"":r,cursor);if(done)b.setActive(false);
    }
    private static List<LivingEntity> nearby(ServerLevel l,NouveauWorkshop b,int range){return l.getEntitiesOfClass(LivingEntity.class,new AABB(NouveauRitualWork.center(b)).inflate(range),e->e.isAlive()).stream().limit(16).toList();}
    private static boolean spawnAnimal(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        for(var recipe:NouveauRecipes.all())if(recipe.operation().equals("summon_ritual")){boolean match=false;for(int i=0;i<8;i++)match|=NouveauRecipes.ingredient(recipe.data().get("augment"),b.ritualAugment(i));if(!match)continue;boolean spawned=false;int count=Math.clamp(recipe.data().get("count").getAsInt(),1,8);var mobs=recipe.data().getAsJsonArray("mobs");int total=0;for(var mob:mobs)total+=Math.clamp(mob.getAsJsonObject().get("weight").getAsInt(),0,10000);if(total==0)return false;for(int i=0;i<count;i++){int roll=l.getRandom().nextInt(total);for(var mob:mobs){var value=mob.getAsJsonObject();roll-=Math.clamp(value.get("weight").getAsInt(),0,10000);if(roll>=0)continue;var type=BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(value.get("mob").getAsString()));var entity=type==null?null:type.create(l,EntitySpawnReason.MOB_SUMMONED);var at=NouveauRitualWork.center(b).offset(2+i%3,1,i/3);if(entity!=null&&l.hasChunkAt(at)&&Authority.mayChange(l,p,at)){entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);if(l.noCollision(entity))spawned|=l.addFreshEntity(entity);}break;}}return spawned;}
        var candidates=l.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,NouveauRitualWork.center(b)).getMobsToSpawn(MobCategory.CREATURE);var chosen=candidates.getRandom(l.getRandom());
        if(chosen.isEmpty())return false;var type=chosen.get().type();var entity=type.create(l,EntitySpawnReason.MOB_SUMMONED);if(entity==null)return false;
        BlockPos at=NouveauRitualWork.center(b).offset(2,1,0);if(!Authority.mayChange(l,p,at))return false;entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);return l.noCollision(entity)&&l.addFreshEntity(entity);
    }
    public static boolean deniesNaturalSpawn(ServerLevel level,BlockPos pos){
        for(var b:List.copyOf(SANCTUARIES))if(b.getLevel()==level&&!b.isRemoved()&&!b.paused()&&b.ritual().equals("sanctuary")&&b.ritualRunning()&&b.getBlockState().getValue(NouveauWorkshopBlock.ACTIVE)&&level.hasChunkAt(b.getBlockPos())&&NouveauRitualWork.center(b).distSqr(pos)<=Math.pow(Math.min(128,32+NouveauRitualWork.count(b,Items.ROTTEN_FLESH)),2)){var owner=Authority.answering(level,b.owner());if(owner==null)return false;long last=SANCTUARY_PAID.getOrDefault(b,Long.MIN_VALUE/2);if(level.getGameTime()-last>=1200){if(!NouveauSource.pay(level,owner,b,1))return false;SANCTUARY_PAID.put(b,level.getGameTime());}return true;}
        return false;
    }
}
