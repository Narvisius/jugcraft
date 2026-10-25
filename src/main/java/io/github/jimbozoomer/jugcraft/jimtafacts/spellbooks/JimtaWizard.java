package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Source mage identities, school-specific attacks and merchant roles on native server AI. */
public final class JimtaWizard extends PathfinderMob implements GeoEntity,Merchant {
    public static final Map<String,EntityType<JimtaWizard>> TYPES=new LinkedHashMap<>();
    public static final List<String> KINDS=List.of("pyromancer","cryomancer","archevoker","cultist","priest","apothecarist","necromancer","cursed_armor_stand","dead_king","fire_boss","dead_king_corpse");
    private static final EntityDataAccessor<Integer> CAST=SynchedEntityData.defineId(JimtaWizard.class,EntityDataSerializers.INT),PHASE=SynchedEntityData.defineId(JimtaWizard.class,EntityDataSerializers.INT);
    private final String kind;private int cooldown=40,castTicks,spellCycle,transition;private Player trading;private MerchantOffers offers;private boolean outfitted;private long restockDay=-1;
    private final Set<UUID> servants=new HashSet<>();
    private final net.minecraft.server.level.ServerBossEvent bossBar=new ServerBossEvent(java.util.UUID.randomUUID(),net.minecraft.network.chat.Component.empty(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public JimtaWizard(EntityType<? extends PathfinderMob> type,Level level,String kind){super(type,level);this.kind=kind;this.xpReward=boss()?150:15;}
    public String kind(){return kind;}public int castPose(){return entityData.get(CAST);}public int phase(){return entityData.get(PHASE);}public boolean boss(){return kind.equals("dead_king")||kind.equals("fire_boss");}
    public static void register(){JimtaWizardBolt.register();for(var kind:KINDS){var key=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("jimtafacts_"+kind));var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<JimtaWizard>of((t,l)->new JimtaWizard(t,l,kind),MobCategory.CREATURE).sized(.6F,kind.equals("dead_king")?2.3F:1.95F).clientTrackingRange(12).build(key));TYPES.put(kind,type);var eggKey=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+kind+"_spawn_egg"));JimtaSpellbooks.ITEMS.put(kind+"_spawn_egg",Registry.register(BuiltInRegistries.ITEM,eggKey,new SpawnEggItem(new Item.Properties().setId(eggKey).spawnEgg(type))));boolean boss=kind.equals("dead_king")||kind.equals("fire_boss");FabricDefaultAttributeRegistry.register(type,Mob.createMobAttributes().add(Attributes.MAX_HEALTH,kind.equals("dead_king")?500:kind.equals("fire_boss")?1000:60).add(Attributes.MOVEMENT_SPEED,boss?.21:.25).add(Attributes.ATTACK_DAMAGE,boss?10:3).add(Attributes.FOLLOW_RANGE,boss?48:24).add(Attributes.ARMOR,boss?15:0).add(Attributes.KNOCKBACK_RESISTANCE,boss?.8:0));}}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CAST,0);b.define(PHASE,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(7,new RandomStrollGoal(this,.7));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));targetSelector.addGoal(1,new HurtByTargetGoal(this));}
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel level)||!JimtaSpellbooks.enabled())return;
        if(!outfitted){outfitted=true;outfit();}
        if(kind.equals("dead_king_corpse")){getNavigation().stop();setDeltaMovement(Vec3.ZERO);if(transition>0&&--transition==0){var king=TYPES.get("dead_king").create(level,EntitySpawnReason.TRIGGERED);if(king!=null){king.snapTo(getX(),getY()+1,getZ(),getYRot(),0);king.setPersistenceRequired();if(level.addFreshEntity(king))discard();}}return;}
        if(offers!=null&&level.getDefaultClockTime()/24000>restockDay){restockDay=level.getDefaultClockTime()/24000;for(var offer:offers)offer.resetUses();}
        if(boss()){bossBar.setName(getDisplayName());bossBar.setProgress(getHealth()/getMaxHealth());if(getHealth()<=getMaxHealth()*.5&&phase()==0){entityData.set(PHASE,1);transition=60;entityData.set(CAST,3);getNavigation().stop();}if(transition>0){transition--;if(transition==0){entityData.set(PHASE,2);entityData.set(CAST,0);}return;}}
        if(tickCount%20==0){servants.removeIf(id->!(level.getEntity(id) instanceof LivingEntity living)||!living.isAlive());if(trading!=null&&(!trading.isAlive()||distanceToSqr(trading)>64))trading=null;}
        if(kind.equals("priest")&&tickCount%80==0)for(var ally:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(8),e->e.isAlive()&&(e instanceof JimtaWizard||e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager)).stream().limit(8).toList())ally.heal(3);
        if(getTarget()==null&&(boss()||kind.equals("necromancer")||kind.equals("archevoker")||kind.equals("cultist"))&&tickCount%20==0){var p=level.getNearestPlayer(this,24);if(p!=null&&!p.isCreative()&&!p.isSpectator())setTarget(p);}
        var target=getTarget();if(target==null||!target.isAlive()||target instanceof Player player&&(player.isCreative()||player.isSpectator())||distanceToSqr(target)>64*64){setTarget(null);if(tickCount%100==0&&getHealth()<getMaxHealth())heal(2);return;}
        if(trading!=null){getNavigation().stop();return;}getLookControl().setLookAt(target,30,30);
        if(castTicks>0){castTicks--;if(kind.equals("fire_boss")&&castPose()>=10){fireCombo(level,target);if(castTicks==0)entityData.set(CAST,0);return;}if(castTicks==5)releaseSpell(level,target);if(castTicks==0)entityData.set(CAST,0);return;}
        double distance=distanceToSqr(target);if(distance>14*14||!hasLineOfSight(target)){getNavigation().moveTo(target,1.1);return;}getNavigation().stop();
        if(--cooldown<=0){cooldown=boss()?(phase()==2?35:55):50;spellCycle++;if(kind.equals("fire_boss")&&distance<49){castTicks=60;entityData.set(CAST,10+spellCycle%7);}else{castTicks=20;entityData.set(CAST,1);}}
    }
    private void outfit(){String family=switch(kind){case "pyromancer","cryomancer","archevoker","cultist","priest"->kind;case "necromancer"->"plagued";case "cursed_armor_stand"->"netherite_mage";default->"";};if(!family.isEmpty()){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){String part=switch(slot){case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";};var item=JimtaSpellbooks.ITEMS.get(family+"_"+part);if(item!=null&&getItemBySlot(slot).isEmpty())setItemSlot(slot,new ItemStack(item));setDropChance(slot,0);}}
        String weapon=switch(kind){case "pyromancer"->"pyrium_staff";case "cryomancer"->"ice_staff";case "cultist","necromancer"->"blood_staff";case "fire_boss","dead_king"->"decrepit_scythe";case "cursed_armor_stand"->"claymore";default->"graybeard_staff";};var item=JimtaSpellbooks.ITEMS.get(weapon);if(item!=null&&!kind.equals("dead_king_corpse")&&getMainHandItem().isEmpty()){setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(item));setDropChance(EquipmentSlot.MAINHAND,0);}}
    private static final String[] FIRE_COMBOS={"scythe_dagger_double_horizontal","scythe_backpedal","scythe_sideslash_downslash_sideslash","scythe_jump_combo","scythe_downslash_sideslash","scythe_horizontal_slash_spin","scythe_dagger_combo_1"};
    private void fireCombo(ServerLevel level,LivingEntity target){int elapsed=60-castTicks,index=castPose()-10;if(index==3&&elapsed==8)setDeltaMovement(getLookAngle().multiply(.4,0,.4).add(0,.65,0));if(index==1&&elapsed==8)setDeltaMovement(getLookAngle().multiply(-.5,0,-.5));int[][] frames={{12,28},{15},{15,30,45},{30},{18,32},{20,40},{12,24,40}};boolean frame=false;for(int f:frames[index])frame|=elapsed==f;if(!frame)return;
        for(var foe:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(5),e->e.isAlive()&&e==target).stream().limit(8).toList()){Vec3 towards=foe.position().subtract(position()).normalize();if((index==5||towards.dot(getLookAngle().multiply(1,0,1))>-.1)&&hasLineOfSight(foe)){if(foe.hurtServer(level,damageSources().mobAttack(this),phase()==2?14:10))foe.igniteForSeconds(2);}}
        if(index==0||index==6)JimtaWizardBolt.launch(this,target,"fire",6,0);
    }
    private void releaseSpell(ServerLevel level,LivingEntity target){if(!hasLineOfSight(target))return;switch(kind){
        case "pyromancer","fire_boss"->{if(boss()&&spellCycle%3==0){for(int i=-2;i<=2;i++)JimtaWizardBolt.launch(this,target,"fire",phase()==2?9:7,i*.16);if(phase()==2)for(var foe:level.getEntitiesOfClass(Player.class,getBoundingBox().inflate(5),e->!e.isCreative()&&!e.isSpectator()).stream().limit(8).toList()){foe.hurtServer(level,damageSources().mobAttack(this),6);foe.igniteForSeconds(2);}}else JimtaWizardBolt.launch(this,target,"fire",6,0);}
        case "cryomancer"->{JimtaWizardBolt.launch(this,target,"ice",5,0);if(spellCycle%4==0)summon(level,target,EntityTypes.POLAR_BEAR);}
        case "archevoker"->{if(spellCycle%3==0)summon(level,target,EntityTypes.VEX);else{JimtaWizardBolt.launch(this,target,"evocation",5,0);for(int i=0;i<5;i++){Vec3 at=position().lerp(target.position(),(i+1)/5.0);level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,at.x,at.y+.1,at.z,8,.2,.2,.2,.1);}}}
        case "cultist","necromancer","dead_king"->{if((kind.equals("necromancer")||boss())&&spellCycle%3==0)summon(level,target,phase()==2?EntityTypes.VEX:EntityTypes.ZOMBIE);if(boss()&&phase()==2){for(int i=-1;i<=1;i++)JimtaWizardBolt.launch(this,target,"wither",8,i*.2);}else JimtaWizardBolt.launch(this,target,spellCycle%2==0?"blood":"wither",boss()?8:5,0);if(kind.equals("cultist")&&distanceToSqr(target)<9){boolean hit=target.hurtServer(level,damageSources().mobAttack(this),6);if(hit)heal(1);}}
        case "priest"->{JimtaWizardBolt.launch(this,target,"holy",5,0);if(getHealth()<getMaxHealth()/2)heal(5);}
        case "apothecarist"->JimtaWizardBolt.launch(this,target,"poison",4,0);
        case "cursed_armor_stand"->{getNavigation().moveTo(target,1.3);if(distanceToSqr(target)<9)target.hurtServer(level,damageSources().mobAttack(this),6);else JimtaWizardBolt.launch(this,target,"evocation",3,0);}
    }}
    private void summon(ServerLevel level,LivingEntity target,EntityType<? extends Mob> type){if(servants.size()>=4)return;var mob=type.create(level,EntitySpawnReason.MOB_SUMMONED);if(mob==null)return;Vec3 at=position().add(getRandom().nextBoolean()?2:-2,0,getRandom().nextBoolean()?2:-2);mob.snapTo(at.x,at.y,at.z,0,0);if(!level.noCollision(mob))return;mob.setTarget(target);mob.setDropChance(EquipmentSlot.MAINHAND,0);mob.setDropChance(EquipmentSlot.OFFHAND,0);if(level.addFreshEntity(mob))servants.add(mob.getUUID());}
    private final Set<UUID> participants=new HashSet<>();
    private boolean ominous;
    public void makeOminous(){ominous=true;}
    @Override protected void dropFromLootTable(ServerLevel level,DamageSource source,boolean recentlyHit){
        if(kind.equals("dead_king_corpse")||kind.equals("cursed_armor_stand"))return;
        var key=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("entities/jimtafacts_"+kind+(ominous&&boss()?"_ominous":"")));
        dropFromLootTable(level,source,recentlyHit,key);
        if(boss())for(UUID id:participants.stream().limit(64).toList()){
            var player=level.getServer().getPlayerList().getPlayer(id);if(player==null||player.level()!=level||player.distanceToSqr(this)>128*128)continue;
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,this).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,position()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,source).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER,player).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY,source.getEntity()).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY,source.getDirectEntity()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var perPlayer=ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("entities/jimtafacts_"+kind+"_per_player"+(ominous?"_ominous":"")));
            for(var stack:level.getServer().reloadableRegistries().getLootTable(perPlayer).getRandomItems(params))if(!player.getInventory().add(stack))player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
    @Override public void die(DamageSource source){if(level() instanceof ServerLevel level)for(var id:servants){var mob=level.getEntity(id);if(mob!=null)mob.discard();}bossBar.removeAllPlayers();super.die(source);}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){if(kind.equals("dead_king_corpse")){if(source.getEntity() instanceof Player)triggerCorpse();return false;}if(transition>0)return false;if(source.getEntity() instanceof JimtaWizard)return false;
        if(amount>0&&source.getEntity() instanceof ServerPlayer p&&participants.size()<64)participants.add(p.getUUID());return super.hurtServer(level,source,amount);}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(boss())bossBar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bossBar.removePlayer(p);}
    private List<String> schoolSpells(){String school=switch(kind){case "pyromancer","fire_boss"->"fire";case "cryomancer"->"ice";case "cultist","necromancer","dead_king"->"blood";case "priest"->"holy";case "apothecarist"->"nature";default->"evocation";};return JimtaSpellcasting.SPELLS.values().stream().filter(s->s.school().equals(school)).map(JimtaSpellcasting.Working::name).toList();}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){if(kind.equals("dead_king_corpse")){triggerCorpse();return InteractionResult.SUCCESS;}if(boss()||getTarget()!=null||kind.equals("cursed_armor_stand"))return super.mobInteract(p,hand);if(p instanceof ServerPlayer){trading=p;openTradingScreen(p,getDisplayName(),1);}return InteractionResult.SUCCESS;}
    private void triggerCorpse(){if(!level().isClientSide()&&transition==0){transition=300;entityData.set(CAST,4);playSound(SoundEvents.WITHER_SPAWN,1,0.7F);}}
    @Override public MerchantOffers getOffers(){if(offers==null){offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,2),new ItemStack(JimtaSpellbooks.ITEMS.get("common_ink")),12,1,.05F));for(String name:schoolSpells().stream().limit(6).toList())offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,Math.clamp(JimtaSpellcasting.SPELLS.get(name).focus()/2+4,4,32)),new ItemStack(JimtaSpellbooks.ITEMS.get("scroll_"+name)),4,2,.05F));}return offers;}
    @Override public void setTradingPlayer(Player p){trading=p;}@Override public Player getTradingPlayer(){return trading;}@Override public void overrideOffers(MerchantOffers o){offers=o;}@Override public void notifyTrade(MerchantOffer o){o.increaseUses();playSound(SoundEvents.VILLAGER_YES,1,1);}@Override public void notifyTradeUpdated(ItemStack stack){}@Override public int getVillagerXp(){return 0;}@Override public void overrideXp(int xp){}@Override public boolean showProgressBar(){return false;}@Override public SoundEvent getNotifyTradeSound(){return SoundEvents.VILLAGER_YES;}@Override public boolean isClientSide(){return level().isClientSide();}@Override public boolean stillValid(Player p){return isAlive()&&getTarget()==null&&distanceToSqr(p)<=64;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("outfitted",outfitted);out.putBoolean("ominous",ominous);out.store("participants",UUIDUtil.CODEC.listOf(),new ArrayList<>(participants));out.putLong("restock_day",restockDay);out.putInt("phase",phase());out.putInt("transition",transition);if(offers!=null)out.store("trades",MerchantOffers.CODEC,offers);out.store("servants",UUIDUtil.CODEC.listOf(),new ArrayList<>(servants));}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);outfitted=in.getBooleanOr("outfitted",false);ominous=in.getBooleanOr("ominous",false);participants.addAll(in.read("participants",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(64).toList());restockDay=in.getLongOr("restock_day",-1);entityData.set(PHASE,Math.clamp(in.getIntOr("phase",0),0,2));transition=Math.clamp(in.getIntOr("transition",0),0,300);if(kind.equals("dead_king_corpse")&&transition>0)entityData.set(CAST,4);offers=in.read("trades",MerchantOffers.CODEC).orElse(null);servants.addAll(in.read("servants",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(4).toList());}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){controllers.add(new AnimationController<JimtaWizard>("casting",2,state->{if(kind.equals("dead_king_corpse"))return state.setAndContinue(RawAnimation.begin().thenPlay(castPose()==4?"dead_king_rise":"dead_king_rest"));if(castPose()==0)return PlayState.STOP;return state.setAndContinue(RawAnimation.begin().thenPlay(castPose()>=10?FIRE_COMBOS[castPose()-10]:castPose()==3?"long_cast":"instant_projectile"));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
