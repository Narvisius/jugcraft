package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The supplied Book Wyrm, with server-owned flight and a persistent, one-to-one lectern binding. */
public final class BookWyrm extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(BookWyrm.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos home;
    private @Nullable UUID owner;
    public @Nullable UUID owner(){return owner;}
    private int visit;
    public BookWyrm(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();setNoGravity(true);moveControl=new FlyingMoveControl(this,20,true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.FLYING_SPEED,.45).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected PathNavigation createNavigation(Level level){return new FlyingPathNavigation(this,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(COLOR,3);}
    public int color(){return Math.clamp(entityData.get(COLOR),0,5);}
    public void color(int color){entityData.set(COLOR,Math.clamp(color,0,5));}
    public @Nullable BlockPos home(){return home;}
    public void bind(BlockPos home,@Nullable UUID owner,int color){this.home=home.immutable();this.owner=owner;color(color);}
    @Override protected void customServerAiStep(ServerLevel level){
        super.customServerAiStep(level);
        if(tickCount%20!=0||home==null)return;
        if(!level.isLoaded(home)){navigation.stop();return;}
        if(!(level.getBlockEntity(home) instanceof StorageLectern lectern)||!lectern.wyrms().contains(getUUID())){discard();return;}
        if(!JugNouveau.enabled()){navigation.stop();return;}
        var links=lectern.links();
        if(navigation.isDone()||tickCount%120==0){
            BlockPos target=home.above();
            if(!links.isEmpty()&&visit++%2==0){var link=links.get(Math.floorMod(visit/2,Math.min(links.size(),lectern.capacity())));if(level.isLoaded(link))target=link.above();}
            Vec3 at=Vec3.atCenterOf(target);
            if(level.noCollision(this,getBoundingBox().move(at.subtract(position()))))navigation.moveTo(at.x,at.y,at.z,.9);
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p&&home!=null&&level().isLoaded(home)&&level().getBlockEntity(home) instanceof StorageLectern lectern&&lectern.mayUse(p)){
            var held=player.getItemInHand(hand);
            int dye=java.util.Arrays.asList(DyeColor.BLACK,DyeColor.BLUE,DyeColor.GREEN,DyeColor.PURPLE,DyeColor.RED,DyeColor.WHITE)
                .indexOf(io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock.dyeColor(held));
            if(dye>=0){if(color()!=dye){color(dye);held.consume(1,p);}return InteractionResult.SUCCESS;}
            p.sendOverlayMessage(Component.translatable("message.jugcraft.bookwyrm.status"));return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player,hand);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
        // Do not lose the binding/refund while its home is unloaded.
        if(home!=null&&!level.isLoaded(home))return false;
        if(source.getEntity() instanceof Player p&&!Authority.mayChange(level,p,blockPosition()))return false;
        return super.hurtServer(level,source,amount);
    }
    public boolean dispel(ServerPlayer player){if(home==null||!player.level().hasChunkAt(home)||!(player.level().getBlockEntity(home) instanceof StorageLectern lectern)||!lectern.mayUse(player))return false;lectern.detach(getUUID());home=null;spawnAtLocation(player.level(),new ItemStack(JugNouveau.CHARM));discard();return true;}
    @Override public void die(DamageSource source){
        if(level() instanceof ServerLevel server&&home!=null&&server.isLoaded(home)
            &&server.getBlockEntity(home) instanceof StorageLectern lectern&&lectern.wyrms().contains(getUUID())){
            lectern.detach(getUUID());home=null;spawnAtLocation(server,new ItemStack(JugNouveau.CHARM));
        }
        super.die(source);
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean canUsePortal(boolean allowPassengers){return false;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);if(home!=null)out.store("home",BlockPos.CODEC,home);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);out.putInt("color",color());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);home=in.read("home",BlockPos.CODEC).orElse(null);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);color(in.getIntOr("color",3));}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){out.add(new AnimationController<BookWyrm>("flight",4,s->s.setAndContinue(RawAnimation.begin().thenLoop("fly"))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
