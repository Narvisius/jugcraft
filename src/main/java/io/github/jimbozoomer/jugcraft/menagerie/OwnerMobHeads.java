package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Original enderman gaze detector and both head note-block voices. */
public final class OwnerMobHeads {
    private OwnerMobHeads(){}
    static void register(){net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registries)->{
        if(!key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.CHARGED_CREEPER))return;
        for(var type:java.util.List.of(net.minecraft.world.entity.EntityTypes.ENDERMAN,net.minecraft.world.entity.EntityTypes.SPIDER,net.minecraft.world.entity.EntityTypes.CAVE_SPIDER)){
            var condition=net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition.hasProperties(net.minecraft.world.level.storage.loot.LootContext.EntityTarget.THIS,net.minecraft.advancements.predicates.entity.EntityPredicate.Builder.entity().of(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE),type));
            var entry=net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(OwnerSupplementaries.ITEMS.get(type==net.minecraft.world.entity.EntityTypes.ENDERMAN?"enderman_head":"spider_head")).when(condition);
            table.withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1))).add(entry));
        }
    });}
    public static boolean head(OwnerSupplementData d){return d.block().function.equals("mob_head");}
    public static boolean ender(OwnerSupplementData d){return d.block().name.startsWith("enderman");}
    public static Identifier note(Level level,BlockPos note){
        return level.getBlockEntity(note.above()) instanceof OwnerSupplementData d&&head(d)?Identifier.withDefaultNamespace(ender(d)?"entity.enderman.teleport":"entity.spider.ambient"):null;
    }
    public static net.minecraft.world.phys.shapes.VoxelShape shape(OwnerSupplementData data){
        double x=.25,z=.25,y=0;if(data.block().name.contains("wall")){var facing=HouseholdBlock.facing(data.getBlockState());x-=facing.getStepX()*.25;z-=facing.getStepZ()*.25;y=.25;}
        double age=data.getLevel()==null?1:Math.clamp((data.getLevel().getGameTime()-data.pulseUntil)*.5,0,1),mouth=data.power>0?age:1-age;
        double extra=ender(data)?mouth*.375:0;return net.minecraft.world.phys.shapes.Shapes.box(x,y,z,x+.5,y+.5+extra,z+.5);
    }
    static void tick(ServerLevel level,OwnerSupplementData data){
        if(!ender(data))return;
        boolean watched=false;var pos=data.getBlockPos();
        for(var player:level.players()){
            if(player.isSpectator()||!player.isAlive()||player.distanceToSqr(Vec3.atCenterOf(pos))>400||player.getItemBySlot(EquipmentSlot.HEAD).is(net.minecraft.tags.ItemTags.GAZE_DISGUISE_EQUIPMENT))continue;
            var hit=player.pick(21,1,false);if(!(hit instanceof BlockHitResult block)||!block.getBlockPos().equals(pos)||!block.getDirection().getAxis().isHorizontal())continue;
            if(data.block().name.contains("wall")){if(block.getDirection()!=HouseholdBlock.facing(data.getBlockState()))continue;}
            else{float angle=Float.parseFloat(HouseholdBlock.value(data.getBlockState(),"rotation"))*22.5F;var relative=hit.getLocation().subtract(Vec3.atCenterOf(pos)).yRot((float)Math.toRadians(angle));if(relative.z>0||relative.x<-.25||relative.x>.25)continue;}
            watched=true;break;
        }
        data.headWatch=watched?Math.min(225,data.headWatch+1):0;int signal=watched?Math.min(15,1+data.headWatch/15):0;
        if(signal!=data.power){if((signal>0)!=(data.power>0))data.pulseUntil=level.getGameTime();data.power=signal;data.changed();level.updateNeighborsAt(pos,data.block());}
        data.setChanged();
        if(watched&&level.getGameTime()%4==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,.3,.3,.3,0);
    }
}
