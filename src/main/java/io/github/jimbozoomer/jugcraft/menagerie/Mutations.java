package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Summoned native encounters and their equipment. Area abilities do not alter terrain. */
public final class Mutations {
    private Mutations(){}
    static MenagerieAnimal create(EntityType<? extends MenagerieAnimal> type,Level level,Menagerie.Species species){return species.mood().equals("hostile")?new Hostile(type,level,species):new Mutant(type,level,species);}
    static void register(){
        JsonObject catalog;
        try(var in=Mutations.class.getResourceAsStream("/jugcraft/menagerie/mutant.json")){catalog=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%20!=0)return;for(var player:server.getPlayerList().getPlayers()){
            int pieces=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){String id=BuiltInRegistries.ITEM.getKey(player.getItemBySlot(slot).getItem()).toString();if(id.startsWith("jugcraft:mutant_mutant_skeleton_"))pieces++;}
            if(pieces>=2)player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,40,0,true,false));
            if(pieces==4)player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,0,true,false));
        }});
        var bone=new ArmorMaterial(28,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,7,ArmorType.LEGGINGS,6,ArmorType.BOOTS,3,ArmorType.BODY,7),15,SoundEvents.ARMOR_EQUIP_CHAIN,1,0,TagKey.create(Registries.ITEM,Jugcraft.id("mutant_bones")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("mutant_skeleton")));
        var skull=registerSkull("mutant_mutant_skeleton_skull",false);var wall=registerSkull("mutant_mutant_skeleton_wall_skull",true);
        for(var entry:catalog.getAsJsonObject("items").entrySet()){
            String id=entry.getKey(),source=entry.getValue().getAsJsonObject().get("source").getAsString();
            Menagerie.item(id,p->{
                if(source.endsWith("_spawn_egg"))return new MenagerieItems.Spawn("mutant_"+source.replace("_spawn_egg",""),p);
                if(source.equals("chemical_x"))return new Chemical(p.stacksTo(16));
                if(source.equals("mutant_skeleton_skull"))return new StandingAndWallBlockItem(skull,wall,Direction.DOWN,p.humanoidArmor(bone,ArmorType.HELMET));
                if(source.equals("mutant_skeleton_chestplate"))return new Item(p.humanoidArmor(bone,ArmorType.CHESTPLATE));
                if(source.equals("mutant_skeleton_leggings"))return new Item(p.humanoidArmor(bone,ArmorType.LEGGINGS));
                if(source.equals("mutant_skeleton_boots"))return new Item(p.humanoidArmor(bone,ArmorType.BOOTS));
                if(source.equals("creeper_minion_tracker"))return new Tracker(p.stacksTo(1));
                if(Set.of("hulk_hammer","endersoul_hand","creeper_shard").contains(source))return new Relic(source,source.equals("hulk_hammer")?p.sword(ToolMaterial.DIAMOND,5,-3.2F):p.durability(256));
                return new Item(p);
            });
        }
    }
    private static Block registerSkull(String id,boolean wall){
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var p=BlockBehaviour.Properties.of().setId(key).strength(1).noOcclusion();
        Block b=wall?new WallSkullBlock(SkullBlock.Types.SKELETON,p){
            @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos at,BlockState state){return null;}
            @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
        }:new SkullBlock(SkullBlock.Types.SKELETON,p){
            @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos at,BlockState state){return null;}
            @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
        };
        Registry.register(BuiltInRegistries.BLOCK,key,b);Menagerie.BLOCKS.put(id,b);return b;
    }
    private static final class Chemical extends Item {
        Chemical(Properties p){super(p);}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity original,InteractionHand hand){
            String source=BuiltInRegistries.ENTITY_TYPE.getKey(original.getType()).toString();String form=switch(source){case "minecraft:zombie","minecraft:husk","minecraft:drowned"->"mutant_zombie";case "minecraft:skeleton","minecraft:stray","minecraft:bogged"->"mutant_skeleton";case "minecraft:creeper"->player.isShiftKeyDown()?"creeper_minion":"mutant_creeper";case "minecraft:enderman"->"mutant_enderman";case "minecraft:snow_golem"->"mutant_snow_golem";case "minecraft:pig","minecraft:spider"->"spider_pig";default->"";};
            if(form.isEmpty()||!original.isAlive()||original.isVehicle()||original.isPassenger())return InteractionResult.PASS;
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!Menagerie.mayChange(p.level(),p,original.blockPosition())||!TwoHanded.allowed(p,p.level(),original))return InteractionResult.FAIL;
            var spec=Menagerie.SPECIES.get("mutant_"+form);if(spec.mood().equals("hostile")&&p.level().getDifficulty()==Difficulty.PEACEFUL)return InteractionResult.FAIL;
            var mutant=Menagerie.TYPES.get(spec.id()).create(p.level(),EntitySpawnReason.CONVERSION);if(mutant==null)return InteractionResult.FAIL;
            mutant.snapTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),0);
            if(p.level().getBlockCollisions(mutant,mutant.getBoundingBox()).iterator().hasNext()){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("This mutation needs more open space."));return InteractionResult.FAIL;}
            mutant.setCustomName(original.getCustomName());mutant.setPersistenceRequired();if(!spec.mood().equals("hostile"))mutant.tame(p.getUUID());
            for(var slot:EquipmentSlot.values())mutant.equipSaved(slot,original.getItemBySlot(slot).copy());
            if(!p.level().addFreshEntity(mutant))return InteractionResult.FAIL;
            original.discard();stack.consume(1,p);p.level().sendParticles(ParticleTypes.WITCH,mutant.getX(),mutant.getY()+1,mutant.getZ(),24,.7,1,.7,.02);return InteractionResult.SUCCESS;
        }
    }
    public static class Mutant extends MenagerieAnimal {
        private int recharge=80,windup;
        private final ServerBossEvent bar;
        private boolean risen;
        Mutant(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){
            super(t,l,s);bar=new ServerBossEvent(UUID.randomUUID(),getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
            if(!l.isClientSide()){
                getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(s.source().equals("creeper_minion")?3:10);
                getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.65);
                if(!s.mood().equals("hostile"))targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Mob.class,20,true,false,(e,level)->owner()!=null&&e instanceof Enemy));
            }
        }
        @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(species().mood().equals("hostile"))bar.addPlayer(p);}
        @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
        @Override protected void customServerAiStep(ServerLevel l){
            super.customServerAiStep(l);bar.setName(getDisplayName());bar.setProgress(getHealth()/getMaxHealth());
            if(recharge>0)recharge--;
            var target=getTarget();
            if(windup>0){
                getNavigation().stop();l.sendParticles(species().source().contains("creeper")?ParticleTypes.SMOKE:ParticleTypes.CRIT,getX(),getY()+1,getZ(),2,.5,.5,.5,0);
                if(--windup==0&&target!=null&&target.isAlive()&&hasLineOfSight(target))special(l,target);
                return;
            }
            if(target!=null&&target.isAlive()&&distanceToSqr(target)<196&&hasLineOfSight(target)&&recharge==0&&!sitting()){
                windup=species().source().equals("mutant_zombie")?12:24;recharge=120;triggerAnim("combat","strike");
                RecoveredCreatureActions.play(this,"slam_ground","shoot","attack","bite");
                playSound(SoundEvents.IRON_GOLEM_ATTACK,1,.65F);
            }
        }
        private boolean permitted(ServerLevel l,LivingEntity target){if(owner()==null)return target instanceof Player&&!((Player)target).isCreative()&&!((Player)target).isSpectator()||target==getTarget();var p=Authority.present(l,owner());return p!=null&&TwoHanded.allowed(p,l,target);}
        private void special(ServerLevel l,LivingEntity target){
            if(!permitted(l,target))return;
            String kind=species().source();
            if(kind.equals("mutant_skeleton")){
                for(int i=-1;i<=1;i++){var arrow=new Arrow(l,this,new ItemStack(Items.ARROW),ItemStack.EMPTY);arrow.pickup=AbstractArrow.Pickup.DISALLOWED;arrow.setBaseDamage(3);var direction=target.getEyePosition().subtract(arrow.position());arrow.shoot(direction.x+i*.35,direction.y+.15,direction.z,1.5F,2);l.addFreshEntity(arrow);}return;
            }
            if(kind.equals("mutant_snow_golem")){
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,1),this);target.hurtServer(l,damageSources().mobAttack(this),5);l.sendParticles(ParticleTypes.SNOWFLAKE,target.getX(),target.getY()+.6,target.getZ(),16,.5,.7,.5,.02);return;
            }
            if(kind.equals("mutant_enderman")){
                for(int i=0;i<8;i++){var pos=target.blockPosition().offset(random.nextInt(9)-4,0,random.nextInt(9)-4);var box=getBoundingBox().move(pos.getX()+.5-getX(),pos.getY()-getY(),pos.getZ()+.5-getZ());
                    if(l.hasChunkAt(pos)&&l.getWorldBorder().isWithinBounds(pos)&&l.getBlockState(pos.below()).isSolidRender()&&l.noCollision(this,box)){setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);l.sendParticles(ParticleTypes.PORTAL,getX(),getY()+1,getZ(),24,.5,1,.5,.02);break;}}
            }
            double radius=kind.equals("creeper_minion")?2:4;
            for(var victim:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius),v->v!=this&&v.isAlive()).stream().limit(24).toList()){
                if(distanceToSqr(victim)>radius*radius||!permitted(l,victim)||!hasLineOfSight(victim))continue;
                victim.hurtServer(l,damageSources().mobAttack(this),kind.equals("creeper_minion")?4:kind.equals("mutant_creeper")?12:8);
                var away=victim.position().subtract(position()).normalize();victim.push(away.x*.6,.35,away.z*.6);
            }
            l.sendParticles(kind.contains("creeper")?ParticleTypes.EXPLOSION:ParticleTypes.POOF,getX(),getY()+.5,getZ(),12,1,.3,1,.02);
        }
        @Override public boolean hurtServer(ServerLevel l,DamageSource source,float amount){
            if(species().source().equals("mutant_zombie")&&!risen&&amount>=getHealth()&&!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)){
                risen=true;setHealth(getMaxHealth()*.25F);windup=40;recharge=120;l.sendParticles(ParticleTypes.SMOKE,getX(),getY()+1,getZ(),28,1,1,1,0);return true;
            }return super.hurtServer(l,source,amount);
        }
        @Override public boolean doHurtTarget(ServerLevel l,Entity target){return windup==0&&super.doHurtTarget(l,target);}
        @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other){return species().source().equals("spider_pig")&&super.canMate(other);}
        @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("MutantRisen",risen);out.putInt("MutantRecharge",recharge);}
        @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);risen=in.getBooleanOr("MutantRisen",false);recharge=Math.clamp(in.getIntOr("MutantRecharge",80),0,120);windup=0;}
    }
    private static final class Hostile extends Mutant implements Enemy {Hostile(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){super(t,l,s);}}
    private static final class Tracker extends Item {
        Tracker(Properties p){super(p);}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(who instanceof ServerPlayer p){int count=0;for(var a:l.getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(48),a->a.species().source().equals("creeper_minion")&&a.ownedBy(p)).stream().limit(16).toList()){a.sit(false);a.getNavigation().moveTo(p,1.2);count++;}p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Called "+count+" nearby minions. Crouch and interact with a minion to make it stay."));}return InteractionResult.SUCCESS;
        }
    }
    private static final class Relic extends Item {
        private final String kind;Relic(String kind,Properties p){super(p);this.kind=kind;}
        @Override public InteractionResult use(Level l,Player who,InteractionHand hand){
            if(!(who instanceof ServerPlayer p))return InteractionResult.SUCCESS;var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held))return InteractionResult.PASS;
            if(kind.equals("endersoul_hand")){
                var hit=p.pick(16,0,false);if(!(hit instanceof BlockHitResult blockHit))return InteractionResult.PASS;var at=blockHit.getBlockPos().relative(blockHit.getDirection());
                if(!l.hasChunkAt(at)||!Menagerie.mayChange(l,p,p.blockPosition())||!Menagerie.mayChange(l,p,at)||!l.getWorldBorder().isWithinBounds(at)||!l.noCollision(p,p.getBoundingBox().move(at.getX()+.5-p.getX(),at.getY()-p.getY(),at.getZ()+.5-p.getZ())))return InteractionResult.FAIL;
                int pearl=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.ENDER_PEARL)){pearl=i;break;}if(pearl<0)return InteractionResult.FAIL;
                if(!p.teleportTo(p.level(),at.getX()+.5,at.getY(),at.getZ()+.5,Set.of(),p.getYRot(),p.getXRot(),true))return InteractionResult.FAIL;p.getInventory().getItem(pearl).consume(1,p);
            }else{
                if(!Menagerie.mayChange(l,p,p.blockPosition()))return InteractionResult.FAIL;
                if(kind.equals("creeper_shard")){int powder=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.GUNPOWDER)){powder=i;break;}if(powder<0)return InteractionResult.FAIL;p.getInventory().getItem(powder).consume(1,p);}
                for(var victim:l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(4),v->v!=p&&v.isAlive()).stream().limit(24).toList())if(p.distanceToSqr(victim)<=16&&p.hasLineOfSight(victim)&&TwoHanded.allowed(p,p.level(),victim)){
                    victim.hurtServer(p.level(),p.damageSources().playerAttack(p),kind.equals("hulk_hammer")?10:8);var away=victim.position().subtract(p.position()).normalize();victim.push(away.x*.7,.4,away.z*.7);
                }p.level().sendParticles(kind.equals("creeper_shard")?ParticleTypes.EXPLOSION:ParticleTypes.POOF,p.getX(),p.getY()+.5,p.getZ(),16,1,.2,1,.03);
            }
            held.hurtAndBreak(1,p,hand);p.getCooldowns().addCooldown(held,100);return InteractionResult.SUCCESS;
        }
    }
}
