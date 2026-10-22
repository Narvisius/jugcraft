package io.github.jimbozoomer.jugcraft.update;

import java.io.IOException;
import java.nio.file.*;

/** Starts only the Java helper bundled in this installed mod, never downloaded code. */
public final class UpdateBootstrap {
    public static Process start(Path root, String mode) throws IOException {
        Path work=root.resolve(UpdateApplier.WORK); UpdateApplier.safe(work); Files.createDirectories(work);
        byte[] bytes;
        try(var in=UpdateBootstrap.class.getResourceAsStream("/jugcraft/updater-helper.jar")) {
            UpdateApplier.require(in!=null,"Bundled updater is missing"); bytes=in.readAllBytes();
        }
        Path helper=work.resolve("helper-"+UpdateApplier.hash(bytes).substring(0,16)+".jar");
        if(!UpdateApplier.matches(helper,UpdateApplier.hash(bytes))) UpdateApplier.atomic(helper,bytes);
        Path java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"javaw.exe":"java");
        UpdateApplier.require(Files.isRegularFile(java),"Minecraft Java runtime was not found");
        UpdateApplier.safe(work.resolve("ready.txt"));UpdateApplier.safe(work.resolve("helper.log"));
        Files.deleteIfExists(work.resolve("ready.txt"));
        return new ProcessBuilder(java.toString(),"-Xmx1G","-jar",helper.toString(),root.toString(),Long.toString(ProcessHandle.current().pid()),mode)
            .directory(root.toFile()).redirectErrorStream(true).redirectOutput(work.resolve("helper.log").toFile()).start();
    }
}
