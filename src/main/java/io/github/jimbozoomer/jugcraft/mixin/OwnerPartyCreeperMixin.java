package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.menagerie.OwnerConfetti;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persistent harmless party creepers; ordinary creeper explosions are unchanged. */
@Mixin(Creeper.class)
public abstract class OwnerPartyCreeperMixin implements OwnerConfetti.PartyCreeper {
    @Unique private static final EntityDataAccessor<Boolean> JUGCRAFT_PARTY=SynchedEntityData.defineId(Creeper.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void jugcraft$define(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(JUGCRAFT_PARTY,false);}
    public boolean jugcraft$isParty(){return ((Creeper)(Object)this).getEntityData().get(JUGCRAFT_PARTY);}
    public void jugcraft$setParty(boolean value){((Creeper)(Object)this).getEntityData().set(JUGCRAFT_PARTY,value);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$save(ValueOutput out,CallbackInfo ci){out.putBoolean("JugcraftParty",jugcraft$isParty());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void jugcraft$load(ValueInput in,CallbackInfo ci){jugcraft$setParty(in.getBooleanOr("JugcraftParty",false));}
    @Inject(method="explodeCreeper",at=@At("HEAD"),cancellable=true)
    private void jugcraft$party(CallbackInfo ci){
        if(!jugcraft$isParty())return;var creeper=(Creeper)(Object)this;
        if(creeper.level() instanceof ServerLevel level){OwnerConfetti.burst(level,new OwnerConfetti.Burst(creeper.position().add(0,creeper.getBbHeight()*.5,0),net.minecraft.world.phys.Vec3.ZERO,java.util.List.of(),creeper.isPowered()?128:80,true));creeper.discard();}
        ci.cancel();
    }
}
