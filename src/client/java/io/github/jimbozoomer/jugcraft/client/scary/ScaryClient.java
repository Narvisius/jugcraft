package io.github.jimbozoomer.jugcraft.client.scary;
import io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
public final class ScaryClient implements ClientModInitializer {
    public static final ModelLayerLocation BODY = new ModelLayerLocation(ScaryMod.id("space_kook"), "body");
    public static final ModelLayerLocation GLASS = new ModelLayerLocation(ScaryMod.id("space_kook"), "glass");
    public static final ModelLayerLocation CUTLER = new ModelLayerLocation(ScaryMod.id("captain_cutler"), "body");
    public static final ModelLayerLocation SHADOW = new ModelLayerLocation(ScaryMod.id("phantom_shadow"),"body");
    public static final ModelLayerLocation KNIGHT=new ModelLayerLocation(ScaryMod.id("black_knight"),"body");
    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(KNIGHT,BlackKnightGeometry::create);
        EntityRendererRegistry.register(ScaryMod.BLACK_KNIGHT,BlackKnightRenderer::new);
        ModelLayerRegistry.registerModelLayer(SHADOW,PhantomShadowGeometry::create);
        EntityRendererRegistry.register(ScaryMod.PHANTOM_SHADOW,PhantomShadowRenderer::new);
        ModelLayerRegistry.registerModelLayer(BODY, SpaceKookGeometry::body);
        ModelLayerRegistry.registerModelLayer(GLASS, SpaceKookGeometry::glass);
        EntityRendererRegistry.register(ScaryMod.SPACE_KOOK, SpaceKookRenderer::new);
        ModelLayerRegistry.registerModelLayer(CUTLER, CaptainCutlerGeometry::create);
        EntityRendererRegistry.register(ScaryMod.CAPTAIN_CUTLER, CaptainCutlerRenderer::new);
    }
}
