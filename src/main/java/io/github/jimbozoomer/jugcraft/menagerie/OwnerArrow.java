package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native bow ammunition, retaining normal bow/crossbow pickup and enchantment behavior. */
public final class OwnerArrow extends AbstractArrow {
    public static final Map<String,EntityType<OwnerArrow>> TYPES=new LinkedHashMap<>();
    private boolean excavated;
    public OwnerArrow(EntityType<? extends OwnerArrow> type,Level level){super(type,level);}
    private OwnerArrow(EntityType<? extends OwnerArrow> type,Level level,LivingEntity owner,ItemStack ammo,ItemStack weapon){super(type,owner,level,ammo,weapon);setBaseDamage(kind().contains("shark_tooth")?3:2);}
    static void register(){for(String name:List.of("cavern_burrowing_arrow","cavern_seeking_arrow","beast_ancient_dart","beast_shark_tooth_arrow")){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id(name));TYPES.put(name,Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<OwnerArrow>of(OwnerArrow::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build(key)));}}
    public String kind(){return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();}
    public static Item item(String name,Item.Properties properties){return new ArrowItem(properties){@Override public AbstractArrow createArrow(Level level,ItemStack ammo,LivingEntity owner,ItemStack weapon){return new OwnerArrow(TYPES.get(name),level,owner,ammo,weapon);}};}
    @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(Menagerie.item(kind()));}
    @Override protected boolean canHitEntity(Entity entity){return !(entity instanceof LivingEntity target&&getOwner() instanceof ServerPlayer p&&!TwoHanded.allowed(p,p.level(),target))&&super.canHitEntity(entity);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||isRemoved()||isInGround()||!kind().equals("cavern_seeking_arrow")||tickCount>60||!(getOwner() instanceof ServerPlayer p))return;
        var target=l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(8),m->m instanceof Enemy&&m.isAlive()&&canHitEntity(m)&&m.hasLineOfSight(this)).stream().min(Comparator.comparingDouble(this::distanceToSqr));
        if(target.isPresent()){var velocity=getDeltaMovement();var aim=target.get().getEyePosition().subtract(position()).normalize();if(velocity.normalize().dot(aim)>.4)setDeltaMovement(velocity.scale(.92).add(aim.scale(velocity.length()*.08)));}
    }
    @Override protected void doPostHurtEffects(LivingEntity target){super.doPostHurtEffects(target);if(!(getOwner() instanceof ServerPlayer p)||!TwoHanded.allowed(p,p.level(),target))return;if(kind().equals("beast_ancient_dart"))target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0),p);}
    @Override protected void onHitBlock(BlockHitResult hit){
        if(!excavated&&kind().equals("cavern_burrowing_arrow")&&level() instanceof ServerLevel l&&getOwner() instanceof ServerPlayer p){
            var at=hit.getBlockPos();var state=l.getBlockState(at);var tool=new ItemStack(Items.IRON_PICKAXE);
            if(l.hasChunkAt(at)&&Authority.mayChange(l,p,at)&&l.getBlockEntity(at)==null&&(state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(BlockTags.DIRT)||state.is(BlockTags.SAND))&&state.getDestroySpeed(l,at)>=0&&state.getDestroySpeed(l,at)<=2&&(!state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state))){
                var drops=Block.getDrops(state,l,at,null,p,tool);if(l.removeBlock(at,false)){for(var stack:drops)Block.popResource(l,at,stack);excavated=true;setPos(hit.getLocation().add(getDeltaMovement().normalize().scale(.1)));setDeltaMovement(getDeltaMovement().scale(.7));return;}
            }
        }super.onHitBlock(hit);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("Excavated",excavated);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);excavated=in.getBooleanOr("Excavated",false);}
}
