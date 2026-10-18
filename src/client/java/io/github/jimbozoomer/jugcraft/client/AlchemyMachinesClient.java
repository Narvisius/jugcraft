package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyMachines;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Registers the native workshop screen and the pedestal's synchronized display item. */
public final class AlchemyMachinesClient {
    private AlchemyMachinesClient(){}
    public static void register(){
        MenuScreens.register(AlchemyMachines.MENU,AlchemyMachinesScreen::new);
        BlockEntityRenderers.register(AlchemyMachines.ENTITY,AlchemyPedestalRenderer::new);
    }
}
