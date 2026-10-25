package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Original timed grapples release before ejection and never restrain a dead passenger. */
@Mixin(Entity.class)
public abstract class JimtaSpiderDismountMixin {
    @Inject(method={"stopRiding","removeVehicle"},at=@At("HEAD"),cancellable=true)
    private void jimtafacts$spiderRestraint(CallbackInfo ci){Entity e=(Entity)(Object)this;if(e.isAlive()&&(e.getVehicle() instanceof JimtaEncounterCreatures.IceSpider spider&&spider.restraining()||e.getVehicle() instanceof JimtaSpiderTomb tomb&&tomb.restraining()))ci.cancel();}
}
