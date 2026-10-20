package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.effect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Turrets cast from their muzzle through the same plans, claims, PvP checks and work accounting as players. */
public final class NouveauDevices {
    private NouveauDevices(){}
    public record Supply(net.minecraft.world.Container inventory,int slot){
        public net.minecraft.world.item.ItemStack stack(){return inventory.getItem(slot);}
        public void changed(){inventory.setChanged();}
    }
    public static NouveauWorkshop source(ConcordanceEffects.Context c){
        if(!c.cause().source().startsWith("jugcraft:nouveau_device/"))return null;
        try{var at=BlockPos.of(Long.parseLong(c.cause().source().substring(c.cause().source().lastIndexOf('/')+1)));var p=ConcordanceEffects.person(c);return p!=null&&c.level().hasChunkAt(at)&&c.level().getBlockEntity(at) instanceof NouveauWorkshop b&&b.permitted(p)?b:null;}catch(NumberFormatException ignored){return null;}
    }
    public static boolean automated(ConcordanceEffects.Context c){return c.cause().source().startsWith("jugcraft:nouveau_device/");}
    public static List<net.minecraft.world.Container> inventories(ServerLevel l,ServerPlayer p,NouveauWorkshop b){
        var positions=new LinkedHashSet<BlockPos>(b.links());if(!b.kind().equals("wixie_cauldron")||positions.isEmpty())for(var d:Direction.values())positions.add(b.getBlockPos().relative(d));
        var result=new ArrayList<net.minecraft.world.Container>();for(var at:positions){var storage=NouveauStorage.accessible(l,p,at);if(storage!=null&&!result.contains(storage))result.add(storage);}return result;
    }
    public static List<Supply> supplies(ConcordanceEffects.Context c,ServerPlayer p){
        var result=new ArrayList<Supply>();if(automated(c)){var b=source(c);if(b!=null)for(var inventory:inventories(c.level(),p,b))for(int i=0;i<Math.min(54,inventory.getContainerSize());i++)result.add(new Supply(inventory,i));}
        else {for(int i=0;i<36;i++)result.add(new Supply(p.getInventory(),i));}return result;
    }
    public static boolean collect(ConcordanceEffects.Context c,ServerPlayer p,net.minecraft.world.item.ItemStack stack){
        int before=stack.getCount();if(!automated(c)){p.getInventory().add(stack);return stack.getCount()!=before;}
        var b=source(c);if(b==null)return false;for(var inventory:inventories(c.level(),p,b))for(int i=0;i<Math.min(54,inventory.getContainerSize())&&!stack.isEmpty();i++){
            if(!inventory.canPlaceItem(i,stack))continue;var old=inventory.getItem(i);if(old.isEmpty())inventory.setItem(i,stack.split(Math.min(stack.getCount(),stack.getMaxStackSize())));
            else if(net.minecraft.world.item.ItemStack.isSameItemSameComponents(old,stack)){int n=Math.min(stack.getCount(),old.getMaxStackSize()-old.getCount());if(n>0){old.grow(n);stack.shrink(n);inventory.setChanged();}}
        }return before!=stack.getCount();
    }
    public static void tick(ServerLevel level,ServerPlayer owner,NouveauWorkshop device){
        switch(device.kind()){
            case "mob_jar" -> NouveauContainment.tick(level,owner,device);
            case "potion_diffuser" -> NouveauAlchemy.diffuse(level,owner,device);
            case "potion_melder" -> NouveauAlchemy.meld(level,owner,device);
            case "basic_spell_turret","spell_turret","rotating_spell_turret","timer_spell_turret","rune" -> cast(level,owner,device);
            case "ritual_brazier" -> NouveauRituals.tick(level,owner,device);
            default -> {}
        }
    }
    private static void cast(ServerLevel l,ServerPlayer owner,NouveauWorkshop b){
        boolean timer=b.kind().equals("timer_spell_turret"),rune=b.kind().equals("rune"),powered=l.hasNeighborSignal(b.getBlockPos());
        boolean rising=b.rising(powered);b.setActive(false);
        if(timer?!b.every(b.threshold()):!rune&&!rising)return;
        var inscription=b.getItem(0).get(JugcraftConcordance.INSCRIPTION);if(inscription==null)return;
        var instrument=new Instrument("jugcraft:nouveau_device","jugcraft:nouveau_spell_turret",32,12,160,2,2400);
        var compiled=io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compileAuthored(inscription.text(),ConcordanceData.rules().catalog(),instrument);
        if(compiled.plan()==null)return;var plan=compiled.plan();int cost=Math.max(1,plan.focus());
        if(!NouveauSource.canPay(l,owner,b,cost))return;
        var facing=b.getBlockState().getValue(NouveauWorkshopBlock.FACING);Vec3 direction=new Vec3(facing.getStepX(),facing.getStepY(),facing.getStepZ());
        Vec3 origin=Vec3.atCenterOf(b.getBlockPos()).add(0,.25,0).add(direction.scale(.6));
        LivingEntity target=null;
        if(rune||b.kind().equals("rotating_spell_turret")){
            var targets=l.getEntitiesOfClass(LivingEntity.class,new AABB(b.getBlockPos()).inflate(rune?1:16),e->e instanceof Enemy&&e.isAlive());
            targets.sort(Comparator.comparingDouble(e->e.distanceToSqr(Vec3.atCenterOf(b.getBlockPos()))));if(targets.isEmpty())return;target=targets.getFirst();direction=target.getEyePosition().subtract(origin).normalize();
        }
        Vec3 end=origin.add(direction.scale(Math.max(1,plan.root().range())));var hit=l.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        Vec3 stop=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():end;
        var entity=ProjectileUtil.getEntityHitResult(owner,origin,stop,new AABB(origin,stop).inflate(.3),e->e instanceof LivingEntity&&e.isAlive()&&!e.isSpectator(),origin.distanceToSqr(stop));
        var at=hit.getBlockPos();var impact=entity==null?new ComposedSpells.Impact(stop,null,at.relative(hit.getDirection()),at,origin):new ComposedSpells.Impact(entity.getLocation(),entity.getEntity().getUUID(),entity.getEntity().blockPosition(),entity.getEntity().blockPosition(),origin);
        if(rune&&target!=null)impact=new ComposedSpells.Impact(target.position(),target.getUUID(),target.blockPosition(),target.blockPosition(),origin);
        var cause=Cause.of(owner.getUUID(),Cause.Origin.SPELL,"jugcraft:nouveau_device/"+b.getBlockPos().asLong(),ConcordanceEffects.nextSerial());Ledger ledger=new Ledger(plan.limits());
        if(!NouveauSource.pay(l,owner,b,cost))return;
        boolean applied=NouveauSpellDelivery.device(l,owner,plan,cause,ledger,impact,direction);
        if(!applied){b.fill(cost);return;}b.setActive(true);b.status("Casting");NouveauSignals.spell(l,owner,origin,plan.text(),plan.capacity(),false);
    }
}
