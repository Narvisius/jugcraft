package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.*;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import org.jspecify.annotations.Nullable;

/** A player-following familiar uses owner geometry, not the workshop entity or a second worker inventory. */
public final class NouveauFamiliar extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> SPECIES=SynchedEntityData.defineId(NouveauFamiliar.class,EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner;
    public NouveauFamiliar(EntityType<? extends PathfinderMob> t,Level l){super(t,l);setNoGravity(true);moveControl=new FlyingMoveControl(this,20,true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FLYING_SPEED,.6).add(Attributes.FOLLOW_RANGE,32);}
    public @Nullable UUID owner(){return owner;}
    public String species(){return entityData.get(SPECIES);}
    public void bind(UUID id,String species){owner=id;entityData.set(SPECIES,species);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SPECIES,"bookwyrm");}
    @Override protected PathNavigation createNavigation(Level l){return new FlyingPathNavigation(this,l);}
    @Override protected void customServerAiStep(ServerLevel l){
        super.customServerAiStep(l);if(tickCount%20!=0)return;var p=Authority.present(l,owner);
        if(p==null||!JugNouveau.enabled()||!NouveauFamiliars.active(p).equals(species())){discard();return;}
        if(distanceToSqr(p)>32*32){discard();return;}navigation.moveTo(p.getX()+1,p.getY()+1.5,p.getZ(),1);
        switch(species()){
            case "starbuncle" -> p.addEffect(new MobEffectInstance(MobEffects.SPEED,40,1,true,false));
            case "wixie" -> {var target=p.getLastHurtMob();if(tickCount%100==0&&target!=null&&target.isAlive()&&target.distanceToSqr(p)<=64&&ConcordanceEffects.mayHarm(p,target)&&Authority.mayStrike(p,target))target.addEffect(new MobEffectInstance(MobEffects.POISON,100,0),p);}
            case "bookwyrm" -> {if(p.getAttachedOrElse(NouveauFamiliars.MAGNET,true)){
                for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(5),i->i.isAlive()&&i.getOwner()==null).stream().limit(12).toList())if(Authority.mayChange(l,p,item.blockPosition()))item.playerTouch(p);
                for(var orb:l.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,p.getBoundingBox().inflate(5),Entity::isAlive).stream().limit(8).toList())if(Authority.mayChange(l,p,orb.blockPosition()))orb.playerTouch(p);
            }}
            default -> {}
        }
    }
    @Override public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player who,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.Vec3 hit){
        if(!(who instanceof net.minecraft.server.level.ServerPlayer p))return net.minecraft.world.InteractionResult.SUCCESS;
        if(!p.getUUID().equals(owner)||!JugNouveau.enabled()||!RateGate.allow(p,"nouveau_familiar_gift",10))return net.minecraft.world.InteractionResult.FAIL;
        var item=p.getItemInHand(hand);
        if(species().equals("starbuncle")&&item.is(net.minecraft.world.item.Items.GOLD_NUGGET)){item.consume(1,p);NouveauStatuses.give(p,"scrying",1200,0,p);return net.minecraft.world.InteractionResult.SUCCESS;}
        if(species().equals("amethyst_golem")&&item.is(net.minecraft.world.item.Items.AMETHYST_SHARD)){item.consume(1,p);NouveauStatuses.give(p,"shielding",3600,0,p);return net.minecraft.world.InteractionResult.SUCCESS;}
        if(species().equals("wixie")&&!item.isEmpty()){
            for(var base:java.util.List.of(net.minecraft.world.item.alchemy.Potions.AWKWARD,net.minecraft.world.item.alchemy.Potions.WATER)){
                var input=new net.minecraft.world.item.crafting.BrewingInput(NouveauAlchemy.bottle(new net.minecraft.world.item.alchemy.PotionContents(base)),item);var recipe=p.level().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BREWING,input,p.level());
                if(recipe.isPresent()){var potion=recipe.get().value().assemble(input).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);if(potion!=null&&potion.hasEffects()){NouveauPotions.apply(p,potion,p,1.25F);item.consume(1,p);return net.minecraft.world.InteractionResult.SUCCESS;}}
            }
        }
        if(species().equals("bookwyrm")){p.setAttached(NouveauFamiliars.MAGNET,!p.getAttachedOrElse(NouveauFamiliars.MAGNET,true));p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Bookwyrm magnet: "+(p.getAttachedOrElse(NouveauFamiliars.MAGNET,true)?"on":"off")));return net.minecraft.world.InteractionResult.SUCCESS;}
        return super.interact(who,hand,hit);
    }
    @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);if(owner!=null)o.store("owner",UUIDUtil.CODEC,owner);o.putString("species",species());}
    @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);owner=i.read("owner",UUIDUtil.CODEC).orElse(null);entityData.set(SPECIES,i.getStringOr("species","bookwyrm"));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<NouveauFamiliar>("follow",5,s->s.setAndContinue(RawAnimation.begin().thenLoop(switch(species()){case "bookwyrm","whirlisprig"->"fly";case "drygmy"->"run";case "amethyst_golem"->"run_master";default->s.isMoving()?"run":"idle";}))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
