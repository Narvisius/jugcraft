package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Native ecology, equipment and encounter loops for the larger owner creature collection. */
public final class Beasts {
    private Beasts(){}
    static MenagerieAnimal create(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){return s.mood().equals("hostile")?new Hostile(t,l,s):new Creature(t,l,s);}
    static void register(){
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("beast_bison_upgrade"),BeastUpgradeRecipe.FUR_SERIALIZER);Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("beast_mimicream_repair"),BeastUpgradeRecipe.REPAIR_SERIALIZER);
        JsonObject root;
        try(var in=Beasts.class.getResourceAsStream("/jugcraft/menagerie/beast.json")){root=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new IllegalStateException(e);}
        for(var entry:root.getAsJsonObject("blocks").entrySet()){
            String id=entry.getKey(),name=entry.getValue().getAsJsonObject().get("source").getAsString();
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id(id));var p=BlockBehaviour.Properties.of().setId(key).strength(2).noOcclusion();
            Block block=switch(name){
                case "purpur_planks_stairs"->new StairBlock(Blocks.PURPUR_BLOCK.defaultBlockState(),p);
                case "purpur_planks_slab"->new SlabBlock(p);
                case "purpur_planks_wall"->new WallBlock(p);
                case "end_pirate_door"->new DoorBlock(BlockSetType.OAK,p);
                case "end_pirate_trapdoor"->new TrapDoorBlock(BlockSetType.OAK,p);
                case "banana_slug_slime_block"->new SlimeBlock(p.bounceRestitution(1));
                case "rainbow_glass"->new OwnerBuildingStates.ConnectedGlass(p.sound(SoundType.GLASS));
                default->HouseholdBlock.create(p.randomTicks(),entry.getValue().getAsJsonObject().getAsJsonObject("definition"));
            };
            Registry.register(BuiltInRegistries.BLOCK,key,block);Menagerie.BLOCKS.put(id,block);Menagerie.item(id,ip->new BlockItem(block,ip.useBlockDescriptionPrefix()));
        }
        for(var entry:root.getAsJsonObject("items").entrySet()){
            String id=entry.getKey();var v=entry.getValue().getAsJsonObject();String source=v.get("source").getAsString();
            if(Menagerie.ITEMS.containsKey(id))continue;
            Menagerie.item(id,p->{
                if(v.get("kind").getAsString().equals("spawn"))return new MenagerieItems.Spawn(v.get("species").getAsString(),p);
                if(source.endsWith("_bucket"))return new MenagerieItems.Captured(source,p.stacksTo(1));
                if(v.has("food")&&v.get("food").getAsBoolean())p.food(new FoodProperties.Builder().nutrition(source.startsWith("cooked_")?7:source.contains("stew")||source.contains("rice")?8:3).saturationModifier(.5F).build());
                if(v.has("armor")&&!v.get("armor").getAsString().isEmpty()){
                    var material=new ArmorMaterial(22,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,6,ArmorType.LEGGINGS,5,ArmorType.BOOTS,2,ArmorType.BODY,6),15,SoundEvents.ARMOR_EQUIP_LEATHER,0,0,TagKey.create(Registries.ITEM,Jugcraft.id("beast_armor_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,Jugcraft.id("beast/"+source)));
                    p.humanoidArmor(material,ArmorType.valueOf(v.get("armor").getAsString().toUpperCase(Locale.ROOT)));
                    if(source.equals("tarantula_hawk_elytra"))p.component(net.minecraft.core.component.DataComponents.GLIDER,net.minecraft.util.Unit.INSTANCE);
                }
                if(source.startsWith("music_disc_"))p.stacksTo(1).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Jugcraft.id("beast_"+source.substring(11))));
                if(source.equals("shield_of_the_deep"))return new ShieldItem(p.durability(640));
                if(source.equals("enderiophage_rocket"))return new FireworkRocketItem(p.component(net.minecraft.core.component.DataComponents.FIREWORKS,new net.minecraft.world.item.component.Fireworks(2,List.of())));
                if(source.equals("shark_tooth_arrow")||source.equals("ancient_dart"))return OwnerArrow.item("beast_"+source,p);
                if(source.endsWith("stew")||source.endsWith("rice")||source.equals("sopa_de_macaco"))p.usingConvertsTo(Items.BOWL).stacksTo(16);
                if(source.equals("fish_oil"))p.usingConvertsTo(Items.GLASS_BOTTLE).stacksTo(16);
                if(Set.of("dimensional_carver","shattered_dimensional_carver","falconry_glove","falconry_hood","echolocator","endolocator","pupfish_locator","maraca","chorus_on_a_stick").contains(source))p.stacksTo(1);
                if(source.equals("ghostly_pickaxe"))p.pickaxe(ToolMaterial.DIAMOND,1,-2.8F);
                if(source.equals("mungal_spores"))return new BoneMealItem(p);
                if(Set.of("skelewag_sword","tendon_whip").contains(source))p.sword(ToolMaterial.DIAMOND,3,-2.2F);
                if(source.equals("straddleboard"))return new CavernSubmarine.Kit(p,true);
                if(source.equals("animal_dictionary"))return new MenagerieItems.Guide(p.stacksTo(1));
                if(source.equals("squid_grapple")||source.equals("vine_lasso"))return new CompanionCreatures.SilkGrapple(p.durability(200));
                return new Relic(source,p);
            });
        }
        BeastCapsid.register(root.getAsJsonArray("capsid"));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%10!=0)return;
            for(var p:server.getPlayerList().getPlayers())gear(p);
        });
    }
    private static boolean wearing(Player p,EquipmentSlot slot,String source){return p.getItemBySlot(slot).is(Menagerie.ITEMS.get("beast_"+source));}
    private static void gear(ServerPlayer p){
        if(p.isRemoved()||!p.level().hasChunkAt(p.blockPosition()))return;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(p.getItemBySlot(slot).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBooleanOr("BisonInsulated",false)){p.setTicksFrozen(0);break;}
        if(wearing(p,EquipmentSlot.FEET,"roadrunner_boots")&&p.level().getBlockState(p.blockPosition().below()).is(BlockTags.SAND))effect(p,MobEffects.SPEED,0);
        if(wearing(p,EquipmentSlot.FEET,"flying_fish_boots")&&!p.onGround()&&!p.isInWater())effect(p,MobEffects.SLOW_FALLING,0);
        if(wearing(p,EquipmentSlot.CHEST,"crocodile_chestplate")&&p.isInWater())effect(p,MobEffects.DOLPHINS_GRACE,0);
        if(wearing(p,EquipmentSlot.HEAD,"spiked_turtle_shell")&&p.isInWater())effect(p,MobEffects.WATER_BREATHING,0);
        if(wearing(p,EquipmentSlot.HEAD,"frontier_cap")&&p.isShiftKeyDown())effect(p,MobEffects.SPEED,0);
        if(wearing(p,EquipmentSlot.HEAD,"froststalker_helmet"))p.setTicksFrozen(0);
        if(wearing(p,EquipmentSlot.LEGS,"centipede_leggings")&&p.horizontalCollision&&!p.isShiftKeyDown())p.setDeltaMovement(p.getDeltaMovement().x,.2,p.getDeltaMovement().z);
        if(wearing(p,EquipmentSlot.CHEST,"rocky_chestplate")&&p.isSprinting())effect(p,MobEffects.RESISTANCE,0);
        if(wearing(p,EquipmentSlot.HEAD,"sombrero")&&p.level().canSeeSky(p.blockPosition())&&p.level().isBrightOutside())effect(p,MobEffects.LUCK,0);
        if(wearing(p,EquipmentSlot.LEGS,"emu_leggings")&&p.isSprinting())effect(p,MobEffects.JUMP_BOOST,0);
        if(p.getOffhandItem().is(Menagerie.ITEMS.get("beast_halo")))effect(p,MobEffects.LUCK,0);
        if(wearing(p,EquipmentSlot.CHEST,"unsettling_kimono")&&p.tickCount%20==0)for(var enemy:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(12),m->m.getTarget()==p&&m.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)).stream().limit(16).toList())if(enemy.getLastHurtByMob()!=p)enemy.setTarget(null);
    }
    private static void effect(LivingEntity p,Holder<MobEffect> effect,int level){p.addEffect(new MobEffectInstance(effect,30,level,true,false));}
    static final class Hostile extends Creature implements Enemy {Hostile(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){super(t,l,s);}}
    public static class Creature extends MenagerieAnimal {
        private int abilityTicks=80,windup;
        private final ServerBossEvent bar;
        Creature(EntityType<? extends MenagerieAnimal> t,Level l,Menagerie.Species s){
            super(t,l,s);bar=s.health()>=120&&s.mood().equals("hostile")?new ServerBossEvent(UUID.randomUUID(),Component.literal(s.name()),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS):null;
            if(s.habitat().equals("nether"))setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.LAVA,0);
        }
        @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(bar!=null)bar.addPlayer(p);}
        @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);if(bar!=null)bar.removePlayer(p);}
        @Override public boolean fireImmune(){return species().habitat().equals("nether")||super.fireImmune();}
        @Override protected void customServerAiStep(ServerLevel l){
            super.customServerAiStep(l);if(bar!=null)bar.setProgress(getHealth()/getMaxHealth());
            if(windup>0){getNavigation().stop();recoveredCharge(windup-1);if(--windup==0)special(l);return;}
            if(--abilityTicks<=0){abilityTicks=100+random.nextInt(40);if(getTarget()!=null&&hasLineOfSight(getTarget())&&distanceToSqr(getTarget())<144){windup=20;recoveredCharge(20);triggerAnim("combat","strike");RecoveredCreatureActions.play(this,"attack","bite","melee","punch","roar","shoot");l.sendParticles(ParticleTypes.CRIT,getX(),getEyeY(),getZ(),10,.4,.3,.4,.02);}}
            if(tickCount%40==0&&!sitting())utility(l);
        }
        protected boolean allowed(ServerLevel l,LivingEntity target){
            if(owner()!=null){var p=Authority.present(l,owner());return p!=null&&TwoHanded.allowed(p,l,target);}
            return target==getTarget()&&(!(target instanceof Player p)||!p.isCreative()&&!p.isSpectator());
        }
        protected void special(ServerLevel l){
            var target=getTarget();if(target==null||!allowed(l,target)||!hasLineOfSight(target)||distanceToSqr(target)>144)return;
            String n=species().source();
            if(Set.of("guster","farseer","warped_mosco","crimson_mosquito","straddler","void_worm","murmur").contains(n)){
                float damage=n.equals("farseer")||n.equals("void_worm")?9:5;target.hurtServer(l,damageSources().mobAttack(this),damage);
                if(n.equals("guster"))target.push(0,.5,0);
                if(n.equals("crimson_mosquito"))heal(3);
                l.sendParticles(n.equals("guster")?ParticleTypes.CLOUD:ParticleTypes.PORTAL,target.getX(),target.getY()+.7,target.getZ(),12,.3,.3,.3,.03);
            }else if(distanceToSqr(target)<16){
                doHurtTarget(l,target);
                if(Set.of("elephant","rhinoceros","bison","moose","tusklin","bunfungus").contains(n))target.knockback(1,getX()-target.getX(),getZ()-target.getZ(),damageSources().mobAttack(this),5);
            }
        }
        private void utility(ServerLevel l){
            if(getItemBySlot(EquipmentSlot.FEET).is(Menagerie.ITEMS.get("beast_pigshoes"))&&isVehicle())addEffect(new MobEffectInstance(MobEffects.SPEED,60,0,true,false));
            String n=species().source();
            if(n.equals("sunbird"))for(var p:l.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(8),p->!p.onGround()).stream().limit(8).toList())p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100,0));
            if(n.equals("skunk")&&getLastHurtByMob()!=null&&distanceToSqr(getLastHurtByMob())<16&&allowed(l,getLastHurtByMob()))getLastHurtByMob().addEffect(new MobEffectInstance(MobEffects.NAUSEA,100,0),this);
            if(owner()!=null&&readyToHarvest()){
                String product=switch(n){case "moose"->"moose_antler";case "anaconda"->"shed_snake_skin";case "banana_slug"->"banana_slug_slime";case "emu"->"emu_egg";case "platypus"->"gongylidia";case "cachalot_whale"->"ambergris";default->"";};
                if(!product.isEmpty()&&l.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(4)).size()<8){spawnAtLocation(l,new ItemStack(Menagerie.item("beast_"+product)));harvested();}
            }
            if(n.equals("crow")&&ownedByPresent(l)&&chested()){
                for(var item:l.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(3),i->i.isAlive()&&!i.hasPickUpDelay()).stream().limit(4).toList()){
                    var p=Authority.present(l,owner());if(p==null||item.getOwner()!=null&&!item.getOwner().getUUID().equals(owner())||!Menagerie.mayChange(l,p,item.blockPosition()))continue;
                    var remaining=cargo.addItem(item.getItem().copy());if(remaining.isEmpty())item.discard();else item.setItem(remaining);
                }
            }
        }
        private boolean ownedByPresent(ServerLevel l){return owner()!=null&&Authority.present(l,owner())!=null;}
        @Override public boolean carriesCargo(){return super.carriesCargo()||Set.of("crow","kangaroo","raccoon","capuchin_monkey").contains(species().source());}
        @Override public boolean doHurtTarget(ServerLevel l,Entity target){
            if(windup>0)return false;boolean hit=super.doHurtTarget(l,target);
            if(hit&&target instanceof LivingEntity a){String n=species().source();
                if(Set.of("rattlesnake","komodo_dragon","centipede_head","tarantula_hawk").contains(n))a.addEffect(new MobEffectInstance(MobEffects.POISON,100,0),this);
                if(n.equals("froststalker"))a.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,80,0),this);
                if(n.equals("soul_vulture"))a.addEffect(new MobEffectInstance(MobEffects.WITHER,60,0),this);
            }return hit;
        }
        @Override public InteractionResult mobInteract(Player p,InteractionHand hand){
            if(!Menagerie.mayChange(p.level(),p,blockPosition()))return InteractionResult.FAIL;
            var held=p.getItemInHand(hand);String n=species().source();
            if(n.equals("bison")&&held.is(Items.SHEARS)&&readyToHarvest()&&!isBaby()&&(owner()==null||ownedBy(p))){
                if(p instanceof ServerPlayer sp){spawnAtLocation(sp.level(),new ItemStack(Menagerie.item("beast_bison_fur"),2));harvested();held.hurtAndBreak(1,p,hand);}return InteractionResult.SUCCESS;
            }
            if(n.equals("skunk")&&held.is(Items.GLASS_BOTTLE)&&readyToHarvest()&&ownedBy(p)){if(p instanceof ServerPlayer){p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Menagerie.item("beast_stink_bottle"))));harvested();}return InteractionResult.SUCCESS;}
            if(n.equals("bald_eagle")&&held.is(Menagerie.ITEMS.get("beast_falconry_hood"))&&ownedBy(p)){if(p instanceof ServerPlayer sp){sit(!sitting());sp.sendOverlayMessage(Component.literal(sitting()?"Falcon hooded: staying here":"Falcon unhooded: following"));}return InteractionResult.SUCCESS;}
            if(n.equals("komodo_dragon")&&held.is(Items.GLASS_BOTTLE)&&readyToHarvest()&&ownedBy(p)){
                if(p instanceof ServerPlayer){p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Menagerie.item("beast_komodo_spit_bottle"))));harvested();}return InteractionResult.SUCCESS;
            }
            if(owner()==null&&!species().mood().equals("hostile")&&isFood(held)){
                if(p instanceof ServerPlayer sp){held.consume(1,p);if(random.nextInt(species().health()>35?4:2)==0){tame(p.getUUID());sp.level().broadcastEntityEvent(this,(byte)7);}}
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(p,hand);
        }
        @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putInt("BeastAbility",Math.max(20,abilityTicks));}
        @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);abilityTicks=Math.clamp(i.getIntOr("BeastAbility",80),20,200);windup=0;}
    }
    static final class Relic extends Item {
        private final String name;
        Relic(String name,Properties p){super(p);this.name=name;}
        @Override public InteractionResult interactLivingEntity(ItemStack held,Player player,LivingEntity target,InteractionHand hand){
            if(!(target instanceof MenagerieAnimal animal)||!animal.ownedBy(player)||!Menagerie.mayChange(player.level(),player,animal.blockPosition()))return InteractionResult.PASS;
            if(name.equals("straddle_saddle")&&animal.species().mount()&&!animal.saddled()){if(!player.level().isClientSide()){animal.saddle();held.consume(1,player);}return InteractionResult.SUCCESS;}
            if(name.equals("pigshoes")||name.equals("straddle_helmet")){
                if(!animal.species().mount())return InteractionResult.FAIL;
                if(player instanceof ServerPlayer p){var slot=name.equals("pigshoes")?EquipmentSlot.FEET:EquipmentSlot.BODY;var old=animal.getItemBySlot(slot);animal.equipSaved(slot,held.copyWithCount(1));held.consume(1,p);if(!old.isEmpty()&&!p.getInventory().add(old))animal.spawnAtLocation(p.level(),old);if(slot==EquipmentSlot.BODY)animal.getAttribute(Attributes.ARMOR).setBaseValue(8);}return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            String hatch=switch(name){case "emu_egg"->"emu";case "leafcutter_ant_pupa"->"leafcutter_ant";case "cockroach_ootheca"->"cockroach";case "mosquito_larva"->"crimson_mosquito";case "potted_flutter"->"flutter";default->"";};
            if(!hatch.isEmpty()){
                var at=c.getClickedPos().relative(c.getClickedFace());if(!Menagerie.mayChange(p.level(),p,at)||p.level().getEntitiesOfClass(MenagerieAnimal.class,new AABB(at).inflate(16)).size()>=12)return InteractionResult.FAIL;
                var mob=Menagerie.TYPES.get("beast_"+hatch).create(p.level(),EntitySpawnReason.BREEDING);if(mob==null)return InteractionResult.FAIL;mob.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);mob.setBaby(!hatch.equals("flutter"));mob.tame(p.getUUID());if(!p.level().noCollision(mob)||!p.level().addFreshEntity(mob))return InteractionResult.FAIL;
                if(hatch.equals("flutter"))p.setItemInHand(c.getHand(),ItemUtils.createFilledResult(c.getItemInHand(),p,new ItemStack(Items.FLOWER_POT)));else c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
            }
            String summoned=switch(name){case "mysterious_worm"->"void_worm";case "farseer_arm"->"farseer";case "fish_oil"->"sea_bear";default->"";};
            if(!summoned.isEmpty()){
                var at=c.getClickedPos().relative(c.getClickedFace());if(!summoned.equals("sea_bear")&&!p.level().dimension().equals(Level.END)||p.level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||!Menagerie.mayChange(p.level(),p,at))return InteractionResult.FAIL;
                if(!p.level().getEntitiesOfClass(Creature.class,new AABB(at).inflate(96),m->m.species().source().equals(summoned)).isEmpty())return InteractionResult.FAIL;
                if(summoned.equals("sea_bear")&&!p.level().getFluidState(at).is(FluidTags.WATER))return InteractionResult.FAIL;
                var mob=Menagerie.TYPES.get("beast_"+summoned).create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(mob==null)return InteractionResult.FAIL;
                mob.snapTo(at.getX()+.5,at.getY()+1,at.getZ()+.5,0,0);if(!p.level().noCollision(mob)||!p.level().addFreshEntity(mob))return InteractionResult.FAIL;
                mob.setPersistenceRequired();mob.setTarget(p);if(summoned.equals("sea_bear"))p.setItemInHand(c.getHand(),ItemUtils.createFilledResult(c.getItemInHand(),p,new ItemStack(Items.GLASS_BOTTLE)));else c.getItemInHand().consume(1,p);return InteractionResult.SUCCESS;
            }
            return super.useOn(c);
        }
        @Override public void inventoryTick(ItemStack stack,ServerLevel l,Entity holder,EquipmentSlot slot){
            super.inventoryTick(stack,l,holder,slot);if(name.startsWith("banner_pattern_")&&holder.tickCount%20==0)l.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).get(TagKey.create(Registries.BANNER_PATTERN,Jugcraft.id("beast_pattern/"+name.substring(15)))).ifPresent(patterns->stack.set(net.minecraft.core.component.DataComponents.PROVIDES_BANNER_PATTERNS,patterns));
        }
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity eater){if(!l.isClientSide()&&name.equals("mosquito_repellent_stew")){eater.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,0));for(var mob:l.getEntitiesOfClass(Mob.class,eater.getBoundingBox().inflate(12),m->m.getTarget()==eater&&m instanceof MenagerieAnimal a&&a.species().source().contains("mosquito")))mob.setTarget(null);}return super.finishUsingItem(stack,l,eater);}
        @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){super.hurtEnemy(stack,target,attacker);if(attacker instanceof ServerPlayer p&&TwoHanded.allowed(p,p.level(),target)){if(name.equals("tendon_whip"))target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40,1),p);if(name.equals("skelewag_sword")&&target.isInWater())target.hurtServer(p.level(),p.damageSources().playerAttack(p),2);}}
        @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            var held=p.getItemInHand(hand);if(p.getCooldowns().isOnCooldown(held))return InteractionResult.FAIL;
            if(name.equals("soul_heart")){if(p.getHealth()>=p.getMaxHealth())return InteractionResult.PASS;p.heal(8);held.consume(1,p);p.getCooldowns().addCooldown(held,100);return InteractionResult.SUCCESS;}
            if(Set.of("lava_bottle","poison_bottle","komodo_spit_bottle").contains(name)){
                var target=Caverns.aim(p,12);if(target==null||!TwoHanded.allowed(p,p.level(),target))return InteractionResult.FAIL;
                if(name.equals("lava_bottle")){target.igniteForSeconds(4);target.hurtServer(p.level(),p.damageSources().playerAttack(p),3);}else target.addEffect(new MobEffectInstance(MobEffects.POISON,100,0),p);
                p.level().sendParticles(name.equals("lava_bottle")?ParticleTypes.FLAME:ParticleTypes.WITCH,target.getX(),target.getEyeY(),target.getZ(),10,.2,.3,.2,.02);p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Items.GLASS_BOTTLE)));p.getCooldowns().addCooldown(held,20);return InteractionResult.SUCCESS;
            }
            if(Set.of("echolocator","endolocator","pupfish_locator").contains(name)){
                var candidates=l.getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(96),a->name.equals("pupfish_locator")?a.species().source().equals("devils_hole_pupfish"):name.equals("endolocator")?a.species().habitat().equals("end"):a.species().habitat().equals("cave"));
                var closest=candidates.stream().min(Comparator.comparingDouble(p::distanceToSqr));p.sendOverlayMessage(Component.literal(closest.map(a->a.getName().getString()+": "+a.blockPosition().toShortString()).orElse("No matching creature in the nearby loaded area.")));p.getCooldowns().addCooldown(held,60);return InteractionResult.SUCCESS;
            }
            if(name.endsWith("dimensional_carver")){
                var hit=p.pick(name.startsWith("shattered")?12:32,0,false);if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.FAIL;var at=block.getBlockPos().relative(block.getDirection());if(!l.hasChunkAt(at)||!Menagerie.mayChange(l,p,p.blockPosition())||!Menagerie.mayChange(l,p,at)||!l.getWorldBorder().isWithinBounds(at)||!l.noCollision(p,p.getBoundingBox().move(Vec3.atBottomCenterOf(at).subtract(p.position()))))return InteractionResult.FAIL;
                int pearl=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.ENDER_PEARL)){pearl=i;break;}if(pearl<0&&!p.isCreative())return InteractionResult.FAIL;
                if(!p.teleportTo(p.level(),at.getX()+.5,at.getY(),at.getZ()+.5,Set.of(),p.getYRot(),p.getXRot(),true))return InteractionResult.FAIL;if(!p.isCreative())p.getInventory().getItem(pearl).shrink(1);p.getCooldowns().addCooldown(held,100);return InteractionResult.SUCCESS;
            }
            if(name.equals("maraca")){
                for(var animal:l.getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(12),a->Set.of("cockroach","capuchin_monkey","gorilla").contains(a.species().source())).stream().limit(16).toList()){if(animal.owner()!=null&&!animal.ownedBy(p))continue;animal.getNavigation().stop();animal.setDeltaMovement(0,.2,0);p.level().sendParticles(ParticleTypes.NOTE,animal.getX(),animal.getEyeY()+.2,animal.getZ(),2,.2,.2,.2,0);}l.playSound(null,p.blockPosition(),SoundEvents.NOTE_BLOCK_HAT.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8F,1);p.getCooldowns().addCooldown(held,10);return InteractionResult.SUCCESS;
            }
            if(name.equals("falconry_glove")){
                var target=Caverns.aim(p,32);for(var bird:l.getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(48),a->a.ownedBy(p)&&a.species().source().equals("bald_eagle")).stream().limit(4).toList()){bird.sit(false);if(target!=null&&TwoHanded.allowed(p,p.level(),target))bird.setTarget(target);else{bird.setTarget(null);bird.getNavigation().moveTo(p,1.5);}}return InteractionResult.SUCCESS;
            }
            if(name.equals("chorus_on_a_stick")&&p.getVehicle() instanceof MenagerieAnimal mount&&mount.ownedBy(p)){effect(mount,MobEffects.SPEED,1);p.getCooldowns().addCooldown(held,20);return InteractionResult.SUCCESS;}

            if(name.equals("mimicream")){
                var tool=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(!tool.isDamaged())return InteractionResult.PASS;
                tool.setDamageValue(Math.max(0,tool.getDamageValue()-Math.max(1,tool.getMaxDamage()/4)));held.consume(1,p);return InteractionResult.SUCCESS;
            }
            if(Set.of("blood_sprayer","hemolymph_blaster","stink_ray","pocket_sand").contains(name)){
                if(p.getCooldowns().isOnCooldown(held))return InteractionResult.FAIL;
                String ammo=switch(name){case "blood_sprayer"->"beast_blood_sac";case "hemolymph_blaster"->"beast_hemolymph_sac";case "stink_ray"->"beast_stink_bottle";default->"minecraft:sand";};
                var item=ammo.startsWith("minecraft:")?Items.SAND:Menagerie.item(ammo);int slot=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item)){slot=i;break;}if(slot<0&&!p.isCreative())return InteractionResult.FAIL;
                Vec3 from=p.getEyePosition(),direction=p.getLookAngle();var block=p.level().clip(new ClipContext(from,from.add(direction.scale(16)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));double max=from.distanceTo(block.getLocation());
                LivingEntity target=null;double nearest=max;
                for(var e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(direction.scale(max)).inflate(1),e->e!=p&&e.isAlive()).stream().limit(64).toList()){
                    var hit=e.getBoundingBox().inflate(.2).clip(from,from.add(direction.scale(max)));if(hit.isPresent()&&from.distanceTo(hit.get())<nearest){target=e;nearest=from.distanceTo(hit.get());}
                }
                if(target==null||!TwoHanded.allowed(p,p.level(),target))return InteractionResult.FAIL;
                if(!p.isCreative())p.getInventory().getItem(slot).shrink(1);
                if(name.equals("pocket_sand"))target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,50,0),p);
                else if(name.equals("stink_ray"))target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,100,0),p);
                else target.hurtServer(p.level(),p.damageSources().playerAttack(p),name.equals("hemolymph_blaster")?8:5);
                p.level().sendParticles(NaturalCollections.particle(name.equals("stink_ray")?"beast_smelly":"beast_hemolymph",ParticleTypes.CRIT),target.getX(),target.getEyeY(),target.getZ(),8,.2,.2,.2,.02);p.getCooldowns().addCooldown(held,20);return InteractionResult.SUCCESS;
            }
            return super.use(l,p,hand);
        }
    }
}
