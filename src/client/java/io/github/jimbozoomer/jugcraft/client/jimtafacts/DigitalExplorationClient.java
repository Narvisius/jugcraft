package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalTinyTnt;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.item.properties.numeric.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Source compass direction/spin timing and Tiny TNT fuse growth/flash, using supplied meshes. */
public final class DigitalExplorationClient {
    private DigitalExplorationClient() {}
    public static void register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(Jugcraft.id("jimta_meteorite_direction"), Needle.CODEC);
        EntityRendererRegistry.register(DigitalTinyTnt.ENTITY, TinyRenderer::new);
    }
    public record Needle() implements RangeSelectItemModelProperty {
        public static final MapCodec<Needle> CODEC = MapCodec.unit(new Needle());
        @Override public MapCodec<Needle> type() { return CODEC; }
        @Override public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            var tracker = stack.get(DataComponents.LODESTONE_TRACKER); double angle;
            if (level != null && owner != null && tracker != null && tracker.target().isPresent() && tracker.target().get().dimension().equals(level.dimension())) {
                var target = tracker.target().get().pos(); double dx = target.getX() - owner.position().x, dz = target.getZ() - owner.position().z;
                if (dx * dx + dz * dz <= 36) angle = (System.currentTimeMillis() % 500) / 500.0 * Math.PI * 2;
                else { Vec3 look = Vec3.Z_AXIS.yRot((float)Math.toRadians(-owner.getVisualRotationYInDegrees())); angle = Math.atan2(dx * look.z - dz * look.x, dx * look.x + dz * look.z); }
            } else angle = (System.currentTimeMillis() % 3000) / 3000.0 * Math.PI * 2;
            double turns = (angle - Math.PI / 4) / (Math.PI * 2); return (float)(turns - Math.floor(turns));
        }
    }
    public static final class TinyRenderer extends EntityRenderer<DigitalTinyTnt.Charge, TntRenderState> {
        private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
        private final BlockModelResolver resolver;
        TinyRenderer(EntityRendererProvider.Context c) { super(c); resolver = c.getBlockModelResolver(); shadowRadius = .25f; }
        @Override public TntRenderState createRenderState() { return new TntRenderState(); }
        @Override public void extractRenderState(DigitalTinyTnt.Charge e, TntRenderState s, float partial) { super.extractRenderState(e, s, partial); s.fuseRemainingInTicks = e.getFuse() - partial; resolver.update(s.blockState, DigitalTinyTnt.BLOCK.defaultBlockState(), DISPLAY); }
        @Override public void submit(TntRenderState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
            pose.pushPose(); float fraction = Math.clamp(1 - (s.fuseRemainingInTicks + 1) / 10f, 0, 1); float size = 1 + fraction * fraction * fraction * fraction * .3f;
            pose.translate(0, .25, 0); pose.scale(size, size, size); pose.translate(-.5, -.25, -.5);
            if (!s.blockState.isEmpty()) TntMinecartRenderer.submitWhiteSolidBlock(s.blockState, pose, out, s.lightCoords, ((int)s.fuseRemainingInTicks / 5) % 2 == 0, s.outlineColor);
            pose.popPose(); super.submit(s, pose, out, camera);
        }
    }
}
