package io.github.jimbozoomer.jugcraft.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.botany.BotanicalFields;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Enderman.class)
public abstract class BotanicalEndermanMixin {
    @WrapMethod(method="teleport(DDD)Z")
    private boolean jugcraft$redirect(double x,double y,double z,Operation<Boolean> original){
        var redirect=BotanicalFields.redirect((Enderman)(Object)this,new Vec3(x,y,z));if(redirect==null)return original.call(x,y,z);
        boolean success=false;
        try{var to=redirect.target();success=original.call(to.x,to.y,to.z);return success;}finally{if(!success)redirect.refund();}
    }
}
