package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.jimtafacts.JimtaRealmSkyState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SkyRenderState.class)
public abstract class JimtaRealmSkyStateMixin implements JimtaRealmSkyState {
    @Unique private boolean jugcraft$realm;
    @Unique private double jugcraft$ticks;
    @Unique private net.minecraft.world.phys.Vec3 jugcraft$border=net.minecraft.world.phys.Vec3.ZERO;
    public void jugcraft$realmSky(boolean active,double ticks,net.minecraft.world.phys.Vec3 borderOffset){jugcraft$realm=active;jugcraft$ticks=ticks;jugcraft$border=borderOffset;}
    public net.minecraft.world.phys.Vec3 jugcraft$realmBorder(){return jugcraft$border;}
    public boolean jugcraft$realmSky(){return jugcraft$realm;}
    public double jugcraft$realmTicks(){return jugcraft$ticks;}
}
