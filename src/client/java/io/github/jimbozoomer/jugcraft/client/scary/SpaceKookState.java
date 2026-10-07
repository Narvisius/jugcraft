package io.github.jimbozoomer.jugcraft.client.scary;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
public final class SpaceKookState extends LivingEntityRenderState {
    public boolean chasing;
    public float attackTime;
    public boolean redPulse() { return chasing && ((int)ageInTicks % 16 < 8); }
}
