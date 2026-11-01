package io.github.jimbozoomer.jugcraft.backpack;

import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Saved living creatures reserve actual bag slots. Capture removes once; release consumes only after spawn. */
public final class OwnerBackpackMobs {
    private OwnerBackpackMobs(){}
    static List<CustomData> entries(ItemStack pack){return pack.getOrDefault(OwnerBackpacks.CAPTURES,List.of());}
    public static boolean occupied(ItemStack pack,int slot){return entries(pack).stream().anyMatch(e->contains(e.copyTag(),slot));}
    static boolean contains(CompoundTag entry,int slot){int start=entry.getIntOr("Slot",-999);return slot/9>=start/9&&slot/9<start/9+entry.getIntOr("Height",0)&&slot%9>=start%9&&slot%9<start%9+entry.getIntOr("Width",0);}
    public static boolean capture(ServerPlayer player,ItemStack pack,LivingEntity target){
        if(!OwnerBackpackInfinity.mayEdit(player,pack)||!OwnerBackpackUpgrades.accessible(player,pack)||!player.isShiftKeyDown())return false;
        boolean advanced=OwnerBackpacks.has(pack,"advanced_mob_catcher_upgrade");
        if(!advanced&&!OwnerBackpacks.has(pack,"mob_catcher_upgrade"))return false;
        if(!(target instanceof Mob mob)||!mob.isAlive()||mob.isPassenger()||mob.isVehicle()||mob.getLeashData()!=null||mob.distanceToSqr(player)>36||!player.hasLineOfSight(mob)||!Authority.mayChange(player.level(),player,mob.blockPosition()))return fail(player,"That creature cannot be captured here.");
        String type=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        // Town residents, summoned workers and encounter entities must retain their own lifecycle/ownership.
        boolean ordinary=type.startsWith("minecraft:")||mob instanceof io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
        if(!ordinary||Set.of("minecraft:ender_dragon","minecraft:wither","minecraft:warden","minecraft:elder_guardian","minecraft:villager","minecraft:wandering_trader").contains(type)||mob.entityTags().contains("jugcraft_boss")||mob.getMaxHealth()>100)return fail(player,"Bosses and protected residents cannot be captured.");
        if(mob instanceof OwnableEntity owned&&owned.getOwnerReference()!=null&&!owned.getOwnerReference().getUUID().equals(player.getUUID()))return fail(player,"Only its owner can capture this creature.");
        if(mob instanceof io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal animal&&animal.owner()!=null&&!animal.ownedBy(player))return fail(player,"Only its owner can capture this creature.");
        boolean hostile=mob instanceof Enemy||mob instanceof io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal animal&&animal.species().mood().equals("hostile");
        if(hostile&&!advanced)return fail(player,"Hostile creatures need an advanced mob catcher.");
        int cost=Math.max(1,(int)Math.ceil((mob.getMaxHealth()+Math.min(mob.getHealth(),mob.getMaxHealth()))*.5*(hostile?2:1)));
        if(cost>(advanced?72:18))return fail(player,"This creature needs "+cost+" slots; this catcher allows "+(advanced?72:18)+".");
        var bag=new OwnerBackpackInventory(pack,player.getInventory()::setChanged);
        int width=1,height=cost;double aspect=Math.max(.25,mob.getBbWidth())/Math.max(.25,mob.getBbHeight())*1.4,best=Double.MAX_VALUE;
        for(int w=1;w<=9;w++)for(int h=1;h<=14;h++){int area=w*h;if(area<cost)continue;double score=Math.abs(Math.log((double)w/h/aspect))*10+(area-cost)*.75;if(score<best){best=score;width=w;height=h;}}
        int chosen=-1;
        for(int start=0;start<bag.getContainerSize()&&chosen<0;start++){
            if(start%9+width>9)continue;boolean free=true;
            for(int y=0;y<height&&free;y++)for(int x=0;x<width;x++){int slot=start+y*9+x;if(slot>=bag.getContainerSize()||!bag.getItem(slot).isEmpty()||occupied(pack,slot)){free=false;break;}}
            if(free)chosen=start;
        }
        if(chosen<0)return fail(player,"No empty "+width+" by "+height+" slot area is available.");
        var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,player.registryAccess());if(!mob.save(saved))return false;
        var entity=saved.buildResult();var existing=entries(pack);
        if(existing.size()>=16||entity.toString().length()+existing.stream().mapToInt(e->e.copyTag().toString().length()).sum()>65536)return fail(player,"The pack cannot safely hold more creature data.");
        var tag=new CompoundTag();tag.put("Entity",entity);tag.putInt("Slot",chosen);tag.putInt("Width",width);tag.putInt("Height",height);tag.putString("Name",mob.getName().getString());tag.putBoolean("Hostile",hostile);
        var next=new ArrayList<>(existing);next.add(CustomData.of(tag));pack.set(OwnerBackpacks.CAPTURES,List.copyOf(next));mob.discard();player.getInventory().setChanged();
        player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,mob.getX(),mob.getY()+mob.getBbHeight()*.5,mob.getZ(),16,.3,.3,.3,.05);
        player.sendOverlayMessage(Component.literal("Captured "+tag.getStringOr("Name","creature")+". Sneak-use on open ground to release."));return true;
    }
    public static boolean release(ServerPlayer player,ItemStack pack,BlockPos at,int slot,Runnable changed){
        if(!OwnerBackpackInfinity.mayEdit(player,pack)||!Authority.mayChange(player.level(),player,at)||!player.level().hasChunkAt(at)||player.distanceToSqr(Vec3.atCenterOf(at))>64)return false;
        var entries=new ArrayList<>(entries(pack));int chosen=-1;for(int i=0;i<entries.size();i++)if(slot<0||contains(entries.get(i).copyTag(),slot)){chosen=i;break;}if(chosen<0)return false;
        var record=entries.get(chosen).copyTag();var data=record.getCompound("Entity");if(data.isEmpty())return false;
        var input=TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),data.get());
        var entity=EntityType.loadEntityRecursive(input,player.level(),EntitySpawnReason.MOB_SUMMONED,EntityProcessor.NOP);
        if(!(entity instanceof Mob)||entity.isVehicle()||entity.isPassenger())return false;
        for(var level:player.level().getServer().getAllLevels())if(level.getEntity(entity.getUUID())!=null)return fail(player,"That creature is already present in the world.");
        entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,player.getYRot(),0);
        if(!player.level().noCollision(entity)||!player.level().getWorldBorder().isWithinBounds(entity.getBoundingBox()))return fail(player,"The creature needs more clear space to be released.");
        if(!player.level().addFreshEntity(entity))return false;
        entries.remove(chosen);pack.set(OwnerBackpacks.CAPTURES,List.copyOf(entries));changed.run();
        player.sendOverlayMessage(Component.literal("Released "+record.getStringOr("Name","creature")));return true;
    }
    private static boolean fail(ServerPlayer player,String message){player.sendOverlayMessage(Component.literal(message));return false;}
}
