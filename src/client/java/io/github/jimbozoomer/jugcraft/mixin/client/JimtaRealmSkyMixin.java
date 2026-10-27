package io.github.jimbozoomer.jugcraft.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import io.github.jimbozoomer.jugcraft.client.jimtafacts.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaPocketRooms;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class JimtaRealmSkyMixin {
    @Shadow @Final private RenderTarget renderTarget;
    @Unique private JimtaRealmSky jugcraft$sky;

    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void jugcraft$extract(ClientLevel level,float partial,Camera camera,SkyRenderState state,CallbackInfo ci){
        boolean active=level.dimension().equals(JimtaPocketRooms.DIMENSION);
        var focus=camera.entity()==null?camera.blockPosition():camera.entity().blockPosition();
        var center=new net.minecraft.world.phys.Vec3(Math.floorDiv(focus.getX(),256)*256+8,33,Math.floorDiv(focus.getZ(),256)*256+8);
        ((JimtaRealmSkyState)state).jugcraft$realmSky(active,level.getGameTime()+(double)partial,center.subtract(camera.position()));
        if(active){
            // NONE skips the entire sky pass in LevelRenderer. END schedules it; our draw replaces it.
            state.skybox=DimensionType.Skybox.END;state.endFlashIntensity=0;ci.cancel();
        }
    }
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void jugcraft$render(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci){
        var realm=(JimtaRealmSkyState)state;if(!realm.jugcraft$realmSky())return;
        if(jugcraft$sky==null)jugcraft$sky=new JimtaRealmSky();
        jugcraft$sky.render(renderTarget,fog,realm.jugcraft$realmTicks(),realm.jugcraft$realmBorder());ci.cancel();
    }
    @Inject(method="close",at=@At("HEAD"))
    private void jugcraft$close(CallbackInfo ci){if(jugcraft$sky!=null){jugcraft$sky.close();jugcraft$sky=null;}}
}
