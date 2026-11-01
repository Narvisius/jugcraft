package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauFilters;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

final class BotanicalLogistics {
    private static AABB bounds(BlockPos a,BlockPos b){return new AABB(a.getX(),a.getY(),a.getZ(),b.getX(),b.getY(),b.getZ());}

    private final BotanicalWorkshop flower;private int cursor;private boolean exactState;
    private BlockPos destination;
    BotanicalLogistics(BotanicalWorkshop flower){this.flower=flower;}
    void toggle(ServerPlayer actor){exactState=!exactState;flower.setChanged();actor.sendOverlayMessage(Component.literal(exactState?"Placement sample: exact block state":"Placement sample: block type"));}
    boolean bind(ServerPlayer actor,BlockPos target){
        var at=target.above();var level=actor.level();
        if(!flower.permitted(actor)||!level.hasChunkAt(at)||at.distSqr(flower.getBlockPos())>128*128||!Authority.mayChange(level,actor,at)||!ClaimTransfers.allowed(level,flower.getBlockPos(),at))return false;
        destination=at.immutable();flower.setChanged();actor.sendOverlayMessage(Component.literal("Item destination: "+destination.toShortString()));return true;
    }
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%10!=0)return;
        if(flower.kind().equals("spectranthemum")){teleport(level,actor);return;}
        if(flower.kind().equals("labellia")){label(level,actor);return;}
        place(level,actor);
    }
    private List<ItemEntity> drops(ServerLevel level,ServerPlayer actor,AABB bounds){return level.getEntitiesOfClass(ItemEntity.class,bounds,d->d.isAlive()&&d.getAge()>=20&&!d.hasPickUpDelay()&&BotanicalDropCooldown.ready(d)&&!d.getItem().isEmpty()&&Authority.mayChange(level,actor,d.blockPosition())&&ClaimTransfers.allowed(level,flower.getBlockPos(),d.blockPosition())).stream().limit(32).toList();}
    private void teleport(ServerLevel level,ServerPlayer actor){
        if(destination==null||!level.hasChunkAt(destination)||!Authority.mayChange(level,actor,destination)||!ClaimTransfers.allowed(level,flower.getBlockPos(),destination))return;
        var to=Vec3.atCenterOf(destination);
        for(var drop:drops(level,actor,new AABB(flower.effectivePos()).inflate(2))){
            if(drop.getItem().has(BotanicalTools.MANA)||drop.getItem().is(Jugtania.item("mana_tablet"))||!ClaimTransfers.allowed(level,drop.blockPosition(),destination))continue;
            int cost=(int)Math.ceil(2*drop.getItem().getCount()*drop.position().distanceTo(to));if(cost<1||cost>flower.mana())continue;
            var box=drop.getBoundingBox().move(to.subtract(drop.position()));if(!level.noCollision(drop,box)||!flower.draw(cost))continue;
            var old=drop.position();drop.setPos(to.x,to.y,to.z);drop.setDeltaMovement(Vec3.ZERO);drop.setPickUpDelay(20);drop.needsSync=true;
            level.sendParticles(ParticleTypes.PORTAL,old.x,old.y,old.z,8,.2,.2,.2,.02);level.sendParticles(ParticleTypes.PORTAL,to.x,to.y,to.z,8,.2,.2,.2,.02);
        }
    }
    private void place(ServerLevel level,ServerPlayer actor){
        var origin=flower.effectivePos();boolean mini=flower.kind().endsWith("chibi");int range=mini?(flower.mana()>0?3:2):(flower.mana()>0?8:6),vertical=mini?2:6;
        if(!level.hasChunkAt(origin.below(2)))return;var sample=level.getBlockState(origin.below(2));if(sample.isAir())return;
        var filter=BotanicalCollection.frames(level,origin.below(2));
        var drops=drops(level,actor,new AABB(origin).inflate(2,3,2)).stream().filter(d->d.getItem().getItem() instanceof BlockItem&&(filter.isEmpty()||filter.stream().anyMatch(f->NouveauFilters.allows(f,d.getItem())))).toList();if(drops.isEmpty())return;
        int width=range*2+1,height=vertical*2+1,total=width*width*height;
        for(int scanned=0;scanned<128;scanned++){
            int i=Math.floorMod(cursor++,total);var base=origin.offset(i%width-range,(i/width)%height-vertical,i/(width*height)-range);var at=base.above();
            if(!level.hasChunkAt(base)||!level.hasChunkAt(at))continue;var state=level.getBlockState(base);
            if(exactState?!state.equals(sample):!state.is(sample.getBlock()))continue;
            if(!level.isEmptyBlock(at)||!safeArea(level,actor,at))continue;
            for(var drop:drops){
                if(!drop.isAlive()||drop.getItem().isEmpty())continue;var copy=drop.getItem().copyWithCount(1);var item=(BlockItem)copy.getItem();
                var hit=new BlockHitResult(Vec3.atCenterOf(base).add(0,.5,0),Direction.UP,base,false);
                var context=new BlockPlaceContext(level,actor,InteractionHand.MAIN_HAND,copy,hit);
                if(!context.getClickedPos().equals(at)||!item.place(context).consumesAction())continue;
                drop.getItem().shrink(1);if(drop.getItem().isEmpty())drop.discard();else drop.setItem(drop.getItem().copy());if(flower.mana()>0)flower.draw(1);flower.setChanged();return;
            }
        }flower.setChanged();
    }
    private boolean safeArea(ServerLevel level,ServerPlayer actor,BlockPos at){
        for(var p:BlockPos.betweenClosed(at.offset(-1,-1,-1),at.offset(1,1,1)))if(!level.hasChunkAt(p)||!Authority.mayChange(level,actor,p)||!ClaimTransfers.allowed(level,flower.getBlockPos(),p))return false;return true;
    }
    private void label(ServerLevel level,ServerPlayer actor){
        if(flower.mana()<500)return;var origin=flower.effectivePos();var area=bounds(origin.offset(-2,0,-2),origin.offset(3,1,3));
        for(var tag:drops(level,actor,new AABB(origin))){
            var stack=tag.getItem();var name=stack.get(DataComponents.CUSTOM_NAME);if(!stack.is(Items.NAME_TAG)||name==null)continue;
            var mobs=level.getEntitiesOfClass(LivingEntity.class,area,e->!(e instanceof Player)&&!name.equals(e.getCustomName())&&Authority.mayChange(level,actor,e.blockPosition())&&Authority.mayStrike(actor,e)).stream().limit(32).toList();
            var items=drops(level,actor,area).stream().filter(d->d!=tag&&!name.equals(d.getItem().get(DataComponents.CUSTOM_NAME))).toList();
            if(mobs.isEmpty()&&items.isEmpty()||!flower.draw(500))continue;
            for(var mob:mobs){mob.setCustomName(name);if(mob instanceof Mob m)m.setPersistenceRequired();}
            for(var item:items){var renamed=item.getItem().copy();renamed.set(DataComponents.CUSTOM_NAME,name);item.setItem(renamed);}
            stack.shrink(1);if(stack.isEmpty())tag.discard();return;
        }
    }
    void save(ValueOutput out){out.putInt("logistics_cursor",cursor);out.putBoolean("placement_exact_state",exactState);if(destination!=null)out.store("flower_item_destination",BlockPos.CODEC,destination);}
    void load(ValueInput in){cursor=Math.floorMod(in.getIntOr("logistics_cursor",0),65536);exactState=in.getBooleanOr("placement_exact_state",false);destination=in.read("flower_item_destination",BlockPos.CODEC).filter(p->p.distSqr(flower.getBlockPos())<=128*128).orElse(null);}
}
