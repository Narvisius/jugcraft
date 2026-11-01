package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.WildFrozenState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(EntityRenderState.class)
public abstract class WildFrozenStateMixin implements WildFrozenState {
    @Unique private boolean jugcraft$frozen;
    public boolean jugcraft$frozen(){return jugcraft$frozen;}
    public void jugcraft$frozen(boolean value){jugcraft$frozen=value;}
}
