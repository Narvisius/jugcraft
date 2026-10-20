package io.github.jimbozoomer.jugcraft.menagerie;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** The added seasonal visuals reuse the established spirit, crop-guard, engraving and spinning behavior. */
public final class MenagerieSeasonal {
    public static EntityType<HerbalPumpkling> PUMPKLING;
    public static EntityType<VeilGhost> GHOST;
    private MenagerieSeasonal(){}
    public static void register(){
        var pk=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("herbal_pumpkling"));
        PUMPKLING=Registry.register(BuiltInRegistries.ENTITY_TYPE,pk,EntityType.Builder.<HerbalPumpkling>of(HerbalPumpkling::new,MobCategory.MISC).sized(.6F,.9F).build(pk));
        FabricDefaultAttributeRegistry.register(PUMPKLING,Pumpkling.createAttributes());
        var gk=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("veil_ghost"));
        GHOST=Registry.register(BuiltInRegistries.ENTITY_TYPE,gk,EntityType.Builder.<VeilGhost>of(VeilGhost::new,MobCategory.AMBIENT).sized(.6F,1.4F).fireImmune().build(gk));
        FabricDefaultAttributeRegistry.register(GHOST,RestlessSpirit.createAttributes());
        for(String name:List.of("ghost_ambient","ghost_ambient_1","ghost_attack","ghost_death","ghost_hurt","ghost_weep"))Menagerie.SOUNDS.put("herbal_"+name,Registry.register(BuiltInRegistries.SOUND_EVENT,Jugcraft.id("herbal_"+name),SoundEvent.createVariableRangeEvent(Jugcraft.id("herbal_"+name))));
        Menagerie.item("herbal_ghost_spawn_egg",p->new Summon(false,p));Menagerie.item("herbal_pumpkling_spawn_egg",p->new Summon(true,p));
        for(String style:List.of("cross","upright"))for(String material:List.of("stone","mossy")){
            String id="herbal_gravestone_"+style+"_"+material;
            var b=block(id,p->new GravestoneBlock(p,style.equals("cross")?GravestoneBlock.Style.CROSS:GravestoneBlock.Style.ROUNDED));
            ((FabricBlockEntityType)JugcraftAgriculture.GRAVESTONE_ENTITY).addValidBlock(b);
        }
        var wheel=block("herbal_spinning_wheel",SpinningWheelBlock::new);
        ((FabricBlockEntityType)JugcraftAgriculture.SPINNING_WHEEL_ENTITY).addValidBlock(wheel);
        Block crop=block("herbal_pumpkling_crop",p->new PumpkinCrop(p.noCollision().randomTicks().strength(0).sound(SoundType.CROP)));
        Menagerie.item("herbal_pumpkling_seeds",p->new BlockItem(crop,p));
        for(DyeColor color:DyeColor.values())Menagerie.item("herbal_"+color.getName()+"_cloth",Item::new);
        for(String set:List.of("witch","murder"))for(String part:List.of("head","chest","legs","feet")){
            String name=set+"_"+switch(part){case "head"->set.equals("witch")?"hat":"mask";case "chest"->set.equals("witch")?"robe":"coat";case "legs"->"pants";default->"boots";};
            ArmorType type=switch(part){case "head"->ArmorType.HELMET;case "chest"->ArmorType.CHESTPLATE;case "legs"->ArmorType.LEGGINGS;default->ArmorType.BOOTS;};
            var mat=new ArmorMaterial(16,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,5,ArmorType.LEGGINGS,4,ArmorType.BOOTS,1,ArmorType.BODY,5),18,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,Jugcraft.id("herbal_cloth")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("herbal/"+set)));
            Menagerie.item("herbal_"+name,p->new Item(p.humanoidArmor(mat,type)));
        }
    }
    private static Block block(String id,java.util.function.Function<BlockBehaviour.Properties,Block> factory){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var b=Registry.register(BuiltInRegistries.BLOCK,key,factory.apply(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).strength(1.5F).noOcclusion()));
        Menagerie.BLOCKS.put(id,b);Menagerie.item(id,p->new BlockItem(b,p.useBlockDescriptionPrefix()));return b;
    }
    public static final class HerbalPumpkling extends Pumpkling implements GeoEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public HerbalPumpkling(EntityType<? extends Pumpkling> t,Level l){super(t,l);}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<HerbalPumpkling>("movement",5,s->s.setAndContinue(RawAnimation.begin().thenLoop(s.isMoving()?"walk":"idle"))));}
    }
    public static final class VeilGhost extends RestlessSpirit implements GeoEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public VeilGhost(EntityType<? extends RestlessSpirit> t,Level l){super(t,l);}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<VeilGhost>("drift",5,s->s.setAndContinue(RawAnimation.begin().thenLoop("idle"))));}
        @Override protected SoundEvent getAmbientSound(){return Menagerie.sound("herbal_ghost_ambient");}
        @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource d){return Menagerie.sound("herbal_ghost_hurt");}
        @Override protected SoundEvent getDeathSound(){return Menagerie.sound("herbal_ghost_death");}
    }
    private static final class Summon extends Item {
        private final boolean pumpkin;Summon(boolean pumpkin,Properties p){super(p);this.pumpkin=pumpkin;}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=c.getClickedPos().relative(c.getClickedFace());
            if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
            Entity entity=pumpkin?PUMPKLING.create(p.level(),EntitySpawnReason.SPAWN_ITEM_USE):GHOST.create(p.level(),EntitySpawnReason.SPAWN_ITEM_USE);
            if(entity==null)return InteractionResult.FAIL;entity.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
            if(entity instanceof HerbalPumpkling companion){companion.setOwner(p.getUUID());companion.setHead(new ItemStack(Items.CARVED_PUMPKIN));}
            if(entity instanceof VeilGhost ghost){ghost.setHome(at);ghost.reveal(p.level().getGameTime()+1200);}
            if(!p.level().noCollision(entity)||!p.level().addFreshEntity(entity))return InteractionResult.FAIL;c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
        }
    }
    private static final class PumpkinCrop extends CropBlock {
        PumpkinCrop(Properties p){super(p);}
        @Override protected ItemLike getBaseSeedId(){return Menagerie.item("herbal_pumpkling_seeds");}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level l,BlockPos at,Player player,BlockHitResult hit){
            if(!isMaxAge(state))return InteractionResult.PASS;
            if(player instanceof ServerPlayer p&&Authority.mayChange(p.level(),p,at)){
                if(p.level().getEntitiesOfClass(HerbalPumpkling.class,new net.minecraft.world.phys.AABB(at).inflate(24)).size()>=12)return InteractionResult.FAIL;
                var a=PUMPKLING.create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(a==null)return InteractionResult.FAIL;
                a.snapTo(at.getX()+.5,at.getY()+1,at.getZ()+.5,0,0);a.setOwner(p.getUUID());a.setHead(new ItemStack(Items.CARVED_PUMPKIN));
                if(!l.noCollision(a)||!l.addFreshEntity(a))return InteractionResult.FAIL;l.setBlock(at,getStateForAge(0),3);
            }return InteractionResult.SUCCESS;
        }
    }
}

