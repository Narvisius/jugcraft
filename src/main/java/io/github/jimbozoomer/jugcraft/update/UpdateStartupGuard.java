package io.github.jimbozoomer.jugcraft.update;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.api.EnvType;
import java.nio.file.*;
import java.nio.channels.FileChannel;

/** Prevent a rushed relaunch from loading a partly applied update. No networking. */
public final class UpdateStartupGuard implements PreLaunchEntrypoint {
    // Held for this entire Java process. Do not allow a second game to use a
    // managed instance while its first process (or updater) owns the files.
    private static FileChannel gameChannel;
    private static java.nio.channels.FileLock gameLock;
    @Override public void onPreLaunch() {
        var loader=FabricLoader.getInstance(); if(loader.getEnvironmentType()!=EnvType.CLIENT||loader.isDevelopmentEnvironment()) return;
        Path root=loader.getGameDir(),work=root.resolve(UpdateApplier.WORK);
        if(!Files.exists(root.resolve(UpdateApplier.BASELINE)))return;
        try {
            UpdateApplier.safe(work.resolve("game.lock"));Files.createDirectories(work);
            gameChannel=FileChannel.open(work.resolve("game.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            gameLock=gameChannel.tryLock();
            UpdateApplier.require(gameLock!=null,"JUGCRAFT is already running or updating in this instance");
        }catch(Exception e){throw new IllegalStateException("Wait for the other JUGCRAFT game/update to close before pressing Play.",e);}
        if(!Files.exists(work.resolve("plan.properties"))&&!Files.exists(work.resolve("journal.properties"))) return;
        try {
            UpdateApplier.safe(work.resolve("helper.lock")); boolean idle;
            try(var channel=FileChannel.open(work.resolve("helper.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()) {idle=lock!=null;}
            if(idle) UpdateBootstrap.start(root,"recover");
        } catch(Exception e) {throw new IllegalStateException("JUGCRAFT update needs recovery. Keep this instance and its saves; see .jugcraft-game-update/helper.log",e);}
        throw new IllegalStateException("JUGCRAFT is finishing or recovering an update. Wait a few seconds, then press Play again. Your settings and saves are preserved.");
    }
}
