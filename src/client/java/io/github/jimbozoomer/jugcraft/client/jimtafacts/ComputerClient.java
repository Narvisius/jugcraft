package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import io.github.jimbozoomer.jugcraft.jimtafacts.computers.ComputerTerminal;
import io.github.jimbozoomer.jugcraft.jimtafacts.computers.WorkshopComputers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class ComputerClient {
    private ComputerClient() {}
    public static void register() {
        ComputerAudioClient.register();
        MenuScreens.register(ComputerTerminal.TYPE, ComputerScreen::new);
        BlockEntityRenderers.register(WorkshopComputers.NODE, ComputerMonitorRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(ComputerTerminal.CanvasSnapshot.TYPE,(snapshot,context)->context.client().execute(()->{
            var player=context.client().player;
            if(player!=null&&player.containerMenu instanceof ComputerTerminal.Menu menu&&menu.containerId==snapshot.menu()){
                menu.canvasText=snapshot.text();menu.canvasForeground=snapshot.foreground();menu.canvasBackground=snapshot.background();
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(ComputerTerminal.Snapshot.TYPE, (snapshot, context) -> context.client().execute(() -> {
            var player = context.client().player;
            if (player != null && player.containerMenu instanceof ComputerTerminal.Menu menu && menu.containerId == snapshot.menu()) {
                menu.program = snapshot.program(); menu.output = snapshot.output(); menu.running = snapshot.running();
                menu.energy = snapshot.energy(); menu.hardware = snapshot.hardware(); menu.readOnly = snapshot.readonly(); menu.turtle = snapshot.turtle();menu.files=snapshot.files();menu.fileStorage=snapshot.fileStorage();menu.loadRevision=snapshot.loadRevision();
            }
        }));
    }
}
