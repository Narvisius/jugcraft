package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

public final class AlchemyStorageClient {
    private AlchemyStorageClient(){}
    public static void register(){
        MenuScreens.register(AlchemyStorage.MENU,AlchemyStorageScreen::new);
        MenuScreens.register(AlchemyStorage.CATALOGUE_MENU,AlchemyStorageCatalogueScreen::new);
        MenuScreens.register(io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyPatterns.MENU,AlchemyPatternScreen::new);
        MenuScreens.register(io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyAutomation.MENU,AlchemyAutomationScreen::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(AlchemyStorage.ENTITY,AlchemyChestRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(io.github.jimbozoomer.jugcraft.concordance.assay.AlchemicalWorkshops.ENTITY,AlchemyChestRenderer::new);
        GemAbilitiesScreen.register();
        AlchemyFeedbackClient.register();
        AlchemyStorage.openManual=()->Minecraft.getInstance().gui.setScreen(new AlchemyStorageManualScreen());
    }
}
