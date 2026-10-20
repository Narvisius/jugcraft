package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Native encounters and utilities for the supplied fable creature resources. */
public final class FableCreatures {
    public static EntityType<Dart> DART;
    public static EntityType<Purse> PURSE;
    public static Holder<MobEffect> ROOT,CONVULSION;
    private FableCreatures(){}
    public static void register(){
        JsonObject catalog;
        try(var in=FableCreatures.class.getResourceAsStream("/jugcraft/menagerie/fable.json")){
            if(in==null)return;catalog=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException(e);}
        ROOT=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("fable_root"),new MobEffect(MobEffectCategory.HARMFUL,0x745534){}
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,Jugcraft.id("fable_root"),-.85,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CONVULSION=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Jugcraft.id("fable_convulsion"),new MobEffect(MobEffectCategory.HARMFUL,0x9D4C80){
            @Override public boolean shouldApplyEffectTickThisTick(int remaining,int amplifier){return remaining%20==0;}
            @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){
                // A brief stumble, never client camera control or forced long-distance movement.
                if(entity.onGround()&&!entity.isPassenger()){double angle=entity.getRandom().nextDouble()*Math.PI*2;entity.push(Math.cos(angle)*.08,0,Math.sin(angle)*.08);}
                return true;
            }
        });
        var dartKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("fable_dart"));
        DART=Registry.register(BuiltInRegistries.ENTITY_TYPE,dartKey,EntityType.Builder.<Dart>of(Dart::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(10).build(dartKey));
        var purseKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("fable_wisp_purse"));
        PURSE=Registry.register(BuiltInRegistries.ENTITY_TYPE,purseKey,EntityType.Builder.<Purse>of(Purse::new,MobCategory.MISC).sized(.35F,.35F).clientTrackingRange(6).build(purseKey));
        for(String name:List.of("hound_trap","doom_fire")){
            String id="fable_"+name;var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var props=BlockBehaviour.Properties.of().setId(key).noOcclusion().strength(name.equals("hound_trap")?2:0);
            Block block=name.equals("hound_trap")?new Trap(props):new DoomFire(props.noCollision().lightLevel(s->12));Registry.register(BuiltInRegistries.BLOCK,key,block);Menagerie.BLOCKS.put(id,block);
            if(name.equals("hound_trap"))Menagerie.item(id,p->new BlockItem(block,p.useBlockDescriptionPrefix()));else Menagerie.item(id,p->new BlockItem(block,p){
                @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer() instanceof ServerPlayer player&&!Authority.mayChange(player.level(),player,c.getClickedPos().relative(c.getClickedFace())))return InteractionResult.FAIL;return super.useOn(c);}
            });
        }
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey();var value=entry.getValue().getAsJsonObject();Menagerie.item(id,p->{
                if(value.get("kind").getAsString().equals("spawn"))return new MenagerieItems.Spawn(value.get("species").getAsString(),p,-1,value.has("baby")&&value.get("baby").getAsBoolean());
                return switch(value.get("source").getAsString()){
                    case "fertile_slimeball"->new Fertilizer(p);
                    case "blowgun"->new Blowgun(p.durability(240).repairable(Items.BAMBOO));
                    case "straw_hat"->new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.HEAD).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("menagerie/fable_straw_hat"))).build()));
                    default->new Item(p);
                };
            });
        }
    }
    public static boolean wards(MenagerieAnimal mob,Player player){return mob.species().id().equals("fable_scarecrow")&&player.getItemBySlot(EquipmentSlot.HEAD).is(Menagerie.ITEMS.get("fable_straw_hat"));}
    public static boolean spawnWarded(ServerLevelAccessor level,BlockPos at){
        var hat=Menagerie.ITEMS.get("fable_straw_hat");if(hat==null)return false;
        return level.getLevel().getPlayers(p->!p.isSpectator()&&p.distanceToSqr(Vec3.atCenterOf(at))<1024&&p.getItemBySlot(EquipmentSlot.HEAD).is(hat)).size()>0;
    }
    public static void tick(MenagerieAnimal mob,ServerLevel level){
        if(!mob.species().id().startsWith("fable_"))return;
        var target=mob.getTarget();
        if(mob.species().source().equals("jinx")&&mob.tickCount%60==0&&target!=null&&target.isAlive()&&mob.distanceToSqr(target)>9&&mob.distanceToSqr(target)<144&&mob.hasLineOfSight(target)){
            if(mob.owner()!=null){var keeper=Authority.present(level,mob.owner());if(keeper==null||!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(keeper,level,target))return;}
            var dart=new Dart(DART,level);dart.setOwner(mob);dart.setPos(mob.getX(),mob.getEyeY(),mob.getZ());dart.setBaseDamage(1);dart.pickup=AbstractArrow.Pickup.DISALLOWED;dart.addEffect(new MobEffectInstance(MobEffects.POISON,60));
            var direction=target.getEyePosition().subtract(dart.position());dart.shoot(direction.x,direction.y,direction.z,1.4F,3);level.addFreshEntity(dart);mob.triggerAnim("combat","strike");
        }
        if(mob.species().source().equals("butterfly")&&mob.owner()!=null&&!mob.isBaby()&&mob.tickCount%200==0){
            var keeper=Authority.present(level,mob.owner());if(keeper!=null&&mob.distanceToSqr(keeper)<36&&keeper.getHealth()<keeper.getMaxHealth()){
                // Regeneration does not stack with a swarm of butterflies.
                keeper.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,0,true,false));level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,mob.getX(),mob.getY()+.3,mob.getZ(),1,0,0,0,0);
            }
        }
    }
    public static final class Hostile extends MenagerieAnimal implements Enemy {
        public Hostile(EntityType<? extends MenagerieAnimal> type,Level level,Menagerie.Species species){super(type,level,species);}
    }
    private static final class Fertilizer extends Item {
        Fertilizer(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;boolean grew=false;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                var at=c.getClickedPos().offset(x,0,z);if(!p.level().hasChunkAt(at)||!Authority.mayChange(p.level(),p,at)||!(p.level().getBlockState(at).getBlock() instanceof CropBlock))continue;
                if(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),p.level(),at)){grew=true;p.level().levelEvent(1505,at,15);}
            }
            if(grew){c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;}return InteractionResult.PASS;
        }
    }
    private static final class Blowgun extends BowItem {
        Blowgun(Properties p){super(p);}
        @Override protected Projectile createProjectile(Level level,LivingEntity shooter,ItemStack weapon,ItemStack ammo,boolean critical){
            var dart=new Dart(DART,level);dart.setOwner(shooter);dart.setPos(shooter.getX(),shooter.getEyeY()-.1,shooter.getZ());dart.setAmmo(ammo.copyWithCount(1));dart.setBaseDamage(1.25);dart.addEffect(new MobEffectInstance(MobEffects.POISON,80,0));return dart;
        }
    }
    public static final class Dart extends Arrow {
        public Dart(EntityType<? extends Arrow> type,Level level){super(type,level);}
        public void setAmmo(ItemStack stack){setPickupItemStack(stack);}
        @Override protected boolean canHitEntity(Entity entity){
            if(entity instanceof LivingEntity target&&getOwner() instanceof ServerPlayer p&&!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(p,p.level(),target))return false;
            return super.canHitEntity(entity);
        }
    }
    /** A real item drop: normal pickup, ownership, expiry and save semantics. */
    public static final class Purse extends ItemEntity implements GeoEntity {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Purse(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
        public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<Purse>("purse",0,s->s.setAndContinue(RawAnimation.begin().thenLoop(onGround()?"ground":"fall"))));}
        public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    }
    public static void dropPurse(MenagerieAnimal mob,ServerLevel level){
        if(!mob.species().id().startsWith("fable_")||!mob.species().source().endsWith("wisp"))return;
        var purse=PURSE.create(level,EntitySpawnReason.TRIGGERED);if(purse==null)return;
        var loot=mob.species().source().equals("nether_wisp")?Items.GLOWSTONE_DUST:mob.species().source().equals("ender_wisp")?Items.ENDER_PEARL:Items.AMETHYST_SHARD;
        purse.setItem(new ItemStack(loot));purse.setPos(mob.getX(),mob.getY()+.3,mob.getZ());purse.setDefaultPickUpDelay();level.addFreshEntity(purse);
    }
    private static final class Trap extends Block {
        static final BooleanProperty ARMED=BooleanProperty.create("armed"),CLOSED=BooleanProperty.create("closed");
        Trap(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(ARMED,true).setValue(CLOSED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ARMED,CLOSED);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,s.getValue(CLOSED)?5:2,15);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player player,BlockHitResult hit){
            if(player instanceof ServerPlayer p){if(!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;l.setBlock(pos,s.setValue(CLOSED,false).setValue(ARMED,!s.getValue(ARMED)||s.getValue(CLOSED)),3);}return InteractionResult.SUCCESS;
        }
        @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity,InsideBlockEffectApplier effects,boolean moving){
            if(l instanceof ServerLevel server&&s.getValue(ARMED)&&!s.getValue(CLOSED)&&entity instanceof MenagerieAnimal a&&a.species().id().equals("fable_hound")&&a.owner()==null){a.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,4));l.setBlock(p,s.setValue(CLOSED,true),3);}
        }
    }
    private static final class DoomFire extends Block {
        DoomFire(Properties p){super(p);}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);l.scheduleTick(p,this,200);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){l.removeBlock(p,false);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
        @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity,InsideBlockEffectApplier effects,boolean moving){if(l instanceof ServerLevel server&&entity instanceof Enemy&&!entity.fireImmune())entity.hurtServer(server,l.damageSources().inFire(),1);}
    }
}
