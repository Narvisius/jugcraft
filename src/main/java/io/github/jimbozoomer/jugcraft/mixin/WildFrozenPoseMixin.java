package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.wilds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep ordinary gravity but stop walking, jumping and turning throughout the freeze. */
@Mixin(LivingEntity.class)
public abstract class WildFrozenPoseMixin implements WildFrozenPose {
    @Unique private boolean jugcraft$wasFrozen;
    @Unique private float jugcraft$yaw,jugcraft$pitch,jugcraft$head,jugcraft$body,jugcraft$age,jugcraft$walk;
    @Override public float jugcraft$frozenAge(){return jugcraft$age;}
    @Override public float jugcraft$frozenWalk(){return jugcraft$walk;}
    @Inject(method="tick",at=@At("HEAD"))
    private void jugcraft$capture(CallbackInfo ci){
        var e=(LivingEntity)(Object)this;boolean frozen=WildCold.frozen(e)&&e.isAlive();
        if(frozen&&!jugcraft$wasFrozen){jugcraft$yaw=e.getYRot();jugcraft$pitch=e.getXRot();jugcraft$head=e.yHeadRot;jugcraft$body=e.yBodyRot;jugcraft$age=e.tickCount;jugcraft$walk=e.walkAnimation.position();}
        jugcraft$wasFrozen=frozen;if(frozen)jugcraft$hold();
    }
    @Unique private void jugcraft$hold(){
        var e=(LivingEntity)(Object)this;e.setYRot(jugcraft$yaw);e.yRotO=jugcraft$yaw;e.setXRot(jugcraft$pitch);e.xRotO=jugcraft$pitch;
        e.yHeadRot=e.yHeadRotO=jugcraft$head;e.yBodyRot=e.yBodyRotO=jugcraft$body;
        e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);e.setJumping(false);e.stopUsingItem();
    }
    @Inject(method="tick",at=@At("TAIL"))
    private void jugcraft$holdPose(CallbackInfo ci){if(jugcraft$wasFrozen&&WildCold.frozen((LivingEntity)(Object)this))jugcraft$hold();}
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void jugcraft$noJump(CallbackInfo ci){if(WildCold.frozen((LivingEntity)(Object)this))ci.cancel();}
    @ModifyVariable(method="travel",at=@At("HEAD"),argsOnly=true)
    private Vec3 jugcraft$noTravel(Vec3 movement){return WildCold.frozen((LivingEntity)(Object)this)?Vec3.ZERO:movement;}
}
