package io.github.jimbozoomer.jugcraft.update;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.api.EnvType;
import java.nio.file.*;

/** Prevent a rushed relaunch from loading a partly applied update. No networking. */
public final class UpdateStartupGuard implements PreLaunchEntrypoint {
    // Held for this entire Java process. Do not allow a second game to use a
    // managed instance while its first process (or updater) owns the files.
    private static UpdateApplier.StartupLease gameLease;
    @Override public void onPreLaunch() {
        var loader=FabricLoader.getInstance(); if(loader.getEnvironmentType()!=EnvType.CLIENT||loader.isDevelopmentEnvironment()) return;
        Path root=loader.getGameDir(),work=root.resolve(UpdateApplier.WORK);
        if(!Files.exists(root.resolve(UpdateApplier.BASELINE)))return;
        try {
            gameLease=UpdateApplier.startupLease(root);
        }catch(Exception e){throw new IllegalStateException("Wait for the other JUGCRAFT game/update to close before pressing Play.",e);}
        if(gameLease!=null)return;
        try {
            // No game lock is held by this rejected launch. The helper can finish as soon as we exit.
            UpdateBootstrap.start(root,"recover");
        } catch(Exception e) {throw new IllegalStateException("JUGCRAFT update needs recovery. Keep this instance and its saves; see .jugcraft-game-update/helper.log",e);}
        throw new IllegalStateException("JUGCRAFT is finishing or recovering an update. Wait a few seconds, then press Play again. Your settings and saves are preserved.");
    }
}
