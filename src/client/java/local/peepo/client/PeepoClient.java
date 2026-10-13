package local.peepo.client;
import local.peepo.PeepoMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class PeepoClient implements ClientModInitializer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(PeepoMod.id("peepo"), "main");
    public static final ModelLayerLocation SPOON = new ModelLayerLocation(PeepoMod.id("stirring_spoon"), "main");
    public static final ModelLayerLocation WORK_WOOD = new ModelLayerLocation(PeepoMod.id("work_tools"), "wood");
    public static final ModelLayerLocation WORK_METAL = new ModelLayerLocation(PeepoMod.id("work_tools"), "metal");
    @Override public void onInitializeClient() {
        AssignmentPreview.initialize();
        TransportCrateRenderer.initialize();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->TransportCratePreview.clear());
        net.minecraft.client.gui.screens.MenuScreens.register(local.peepo.TransportCrateMenu.TYPE,TransportCrateScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(local.peepo.CompanionMenu.TYPE,CompanionScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(local.peepo.GeneratorWheel.MENU,WheelScreen::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(local.peepo.GeneratorWheel.ENTITY,WheelRenderer::new);
        ModelLayerRegistry.registerModelLayer(LAYER, PeepoGeometry::create);
        ModelLayerRegistry.registerModelLayer(SPOON, StirringSpoonModel::create);
        ModelLayerRegistry.registerModelLayer(WORK_WOOD, () -> WorkPropsModel.create(false));
        ModelLayerRegistry.registerModelLayer(WORK_METAL, () -> WorkPropsModel.create(true));
        EntityRendererRegistry.register(PeepoMod.PEEPO, PeepoRenderer::new);
        EntityRendererRegistry.register(PeepoMod.JUGHEAD, PeepoRenderer::new);
        EntityRendererRegistry.register(PeepoMod.LEGACY_JUGHEAD, PeepoRenderer::new);
    }
}
