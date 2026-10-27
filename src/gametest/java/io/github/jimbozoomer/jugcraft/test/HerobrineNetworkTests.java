package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.*;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Two real client JVMs: shared entity replication, independent witnesses, shared disappearance. */
public final class HerobrineNetworkTests implements FabricClientGameTest {
    private static final String PEER="jugcraft.ownerTestPeer";
    private static Path coordination(){return Path.of(System.getProperty("jugcraft.ownerTestCoord","owner-network-evidence")).toAbsolutePath();}
    private static void mark(String name,String text){try{Files.createDirectories(coordination());Files.writeString(coordination().resolve(name),text);}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean marked(String name){return Files.exists(coordination().resolve(name));}
    private static AssertionError peerFailure(String message) {
        var tail = new ArrayDeque<String>();
        try (var lines = Files.lines(coordination().resolve("peer.log"))) {
            lines.forEach(line -> { if (tail.size() == 120) tail.removeFirst(); tail.addLast(line); });
        } catch (IOException e) { tail.addLast("Could not read peer log: " + e); }
        return new AssertionError(message + "\nSecond-client log tail:\n" + String.join("\n", tail));
    }
    @Override public void runTest(ClientGameTestContext c) {
        if (System.getProperty(PEER) != null) { peer(c); return; }
        Process peer = null;
        try {
            System.setProperty("jugcraft.ownerTestCoord", Path.of("owner-network-evidence", "herobrine-" + UUID.randomUUID()).toAbsolutePath().toString());
            Files.createDirectories(coordination());
            var properties = new Properties();
            properties.setProperty("server-ip", "127.0.0.1");
            properties.setProperty("online-mode", "false");
            properties.setProperty("view-distance", "4");
            properties.setProperty("simulation-distance", "4");
            properties.setProperty("max-players", "2");
            try (var server = c.worldBuilder().adjustSettings(s -> {
                s.setWorldType(new net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry(s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
                s.setGenerateStructures(false);
            }).createServer(properties); var connection = server.connect()) {
                connection.waitForChunksRender();
                server.runCommand("time set noon");
                server.runCommand("gamerule minecraft:spawn_mobs false");
                server.runCommand("gamemode creative @a");
                server.runOnServer(s -> s.getPlayerList().getWhiteList().add(new net.minecraft.server.players.UserWhiteListEntry(net.minecraft.server.players.NameAndId.createOffline("OwnerPeer"))));
                InProcessServerConfigs.beforeAdditionalClient(server);
                peer = launchPeer(server.computeOnServer(s -> s.getPort()), c);
                mark("peer-pid.txt", Long.toString(peer.pid()));
                final Process other = peer;
                server.waitFor(s -> {
                    if (!other.isAlive()) throw peerFailure("Second client exited before login: " + other.exitValue());
                    return s.getPlayerList().getPlayerByName("OwnerPeer") != null;
                }, 2400);
                server.runCommand("gamemode creative @a");
                server.runCommand("fill -5 219 -24 5 219 3 minecraft:stone");
                server.runCommand("fill -5 220 -24 5 225 3 minecraft:air");
                server.runCommand("tp @a[name=!OwnerPeer] -1.5 220 0.5 180 0");
                server.runCommand("tp OwnerPeer 2.5 220 0.5 0 0");
                c.waitTicks(20);
                connection.waitForChunksRender();
                mark("positioned", "");
                c.waitFor(mc -> marked("peer-ready") && mc.gui.screen() == null, 1200);
                c.runOnClient(mc -> { if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
                server.runCommand("tick freeze");
                int id = server.computeOnServer(s -> {
                    var player = s.getPlayerList().getPlayers().stream().filter(p -> !p.getName().getString().equals("OwnerPeer")).findFirst().orElseThrow();
                    var attempt = HerobrineEncounters.attemptAt(player, new HerobrineDirector.Request(player.getUUID(), Herobrine.Kind.DISTANT, HerobrineDirector.FORCED), new Vec3(0.5, 220, -19.5), Herobrine.Variant.WATCHER);
                    if (!attempt.placed()) throw new AssertionError("Shared encounter placement: " + attempt.failure());
                    return attempt.herobrine().getId();
                });
                mark("entity.txt", Integer.toString(id));
                c.waitFor(mc -> mc.level.getEntity(id) instanceof Herobrine, 400);
                c.waitFor(mc -> marked("peer-saw-entity"), 1200);
                server.runCommand("tick step 13");
                server.waitFor(s -> {
                    var h = (Herobrine) s.overworld().getEntity(id);
                    var away = s.getPlayerList().getPlayerByName("OwnerPeer");
                    if (h == null) throw new AssertionError("Shared encounter ended too soon");
                    if (h.witnesses().contains(away.getUUID())) throw new AssertionError("Looking-away peer wrongly credited");
                    return h.witnesses().size() == 1;
                }, 100);
                server.runCommand("tp OwnerPeer 2.5 220 0.5 180 0");
                mark("turn", "");
                c.waitFor(mc -> marked("peer-facing"), 400);
                server.runCommand("tick step 13");
                server.waitFor(s -> s.overworld().getEntity(id) instanceof Herobrine h && h.witnesses().size() == 2, 100);
                c.takeScreenshot("jugcraft_herobrine_shared_primary");
                mark("both-witnessed", "");
                c.waitFor(mc -> marked("peer-screenshot"), 400);
                server.runCommand("tp @a[name=!OwnerPeer] 0.5 220 -10.5 180 0");
                server.runCommand("tick step 2");
                c.waitFor(mc -> mc.level.getEntity(id) == null, 200);
                mark("removed", "");
                c.waitFor(mc -> marked("peer-finished"), 400);
                c.waitFor(mc -> !other.isAlive(), 1200);
                if (other.exitValue() != 0) throw peerFailure("Second client failed: " + other.exitValue());
                server.runCommand("tick unfreeze");
                mark("passed", "Two real clients received one encounter; looking-away peer was not credited, turning credited it independently, approach removed it on both clients.");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        finally { if (peer != null && peer.isAlive()) peer.destroy(); }
    }
    private static Process launchPeer(int port,ClientGameTestContext c)throws Exception{
        var dir=coordination().resolve("peer-profile");Files.createDirectories(dir);Files.writeString(dir.resolve("options.txt"),"preferredGraphicsBackend:\"opengl\"\nfullscreen:false\nlang:en_us\n");
        var metadata=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("jugcraft-test").orElseThrow().findPath("fabric.mod.json").orElseThrow();
        var resources=coordination().resolve("peer-test-resources");Files.createDirectories(resources);
        try(var paths=Files.walk(metadata.getParent())){for(var file:paths.filter(Files::isRegularFile).toList()){var target=resources.resolve(metadata.getParent().relativize(file));Files.createDirectories(target.getParent());Files.copy(file,target,StandardCopyOption.REPLACE_EXISTING);}}
        var json=com.google.gson.JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
        var tests=new com.google.gson.JsonArray();tests.add(HerobrineNetworkTests.class.getName());json.getAsJsonObject("entrypoints").add("fabric-client-gametest",tests);
        var client=new com.google.gson.JsonArray();client.add(HerobrineNetworkTests.class.getName()+"$PeerStartup");json.getAsJsonObject("entrypoints").add("client",client);
        Files.writeString(resources.resolve("fabric.mod.json"),json.toString());
        String oldResources=metadata.getParent().toAbsolutePath().toString();
        // DevLaunchInjector removes its private system property after applying it.
        Path config=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(config!=null&&!Files.isRegularFile(config.resolve(".gradle/loom-cache/launch.cfg")))config=config.getParent();
        if(config==null)throw new IllegalStateException("Cannot locate this checkout's Loom launch.cfg");
        var launch=Files.readAllLines(config.resolve(".gradle/loom-cache/launch.cfg"));String assets=null,assetIndex=null;
        for(int i=0;i+1<launch.size();i++){if(launch.get(i).trim().equals("--assetsDir"))assets=launch.get(i+1).trim();if(launch.get(i).trim().equals("--assetIndex"))assetIndex=launch.get(i+1).trim();}
        if(assets==null||assetIndex==null)throw new IllegalStateException("Loom launch config lacks asset paths");
        var cmd=new ArrayList<String>();cmd.add(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString());cmd.add("-Xmx2G");cmd.add("-XX:ActiveProcessorCount=4");cmd.add("-XX:ErrorFile="+coordination().resolve("hs_err_peer.log"));
        for(var e:System.getProperties().entrySet()){String k=e.getKey().toString();if(k.startsWith("fabric.")||k.startsWith("fabric-api.")||k.startsWith("org.lwjgl.")||k.equals("java.library.path")||k.equals("log4j.configurationFile"))cmd.add("-D"+k+"="+e.getValue().toString().replace(oldResources,resources.toString()));}
        cmd.add("-D"+PEER+"="+port);cmd.add("-Djugcraft.ownerTestCoord="+coordination());cmd.add("-cp");cmd.add(System.getProperty("java.class.path").replace(oldResources,resources.toString()));cmd.add("net.fabricmc.loader.impl.launch.knot.KnotClient");
        cmd.addAll(List.of("--username","OwnerPeer","--version","26.3","--accessToken","0","--gameDir",dir.toString(),"--assetIndex",assetIndex,"--assetsDir",assets));
        // Avoid Windows command-length limits with the development dependency classpath.
        var arguments=coordination().resolve("peer-java.args");
        Files.write(arguments,cmd.subList(1,cmd.size()).stream().map(a->"\""+a.replace("\\","\\\\").replace("\"","\\\"")+"\"").toList());
        return new ProcessBuilder(cmd.getFirst(),"@"+arguments).directory(dir.toFile()).redirectErrorStream(true).redirectOutput(coordination().resolve("peer.log").toFile()).start();
    }
    public static final class PeerStartup implements net.fabricmc.api.ClientModInitializer {
        public void onInitializeClient(){
            System.out.println("Owner peer test mode="+System.getProperty("fabric.client.gametest")+"; port="+System.getProperty(PEER));
            mark("peer-initialized","");
            int[] ticks={0};
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc->{if(ticks[0]++%600==0)System.out.println("Owner peer tick "+ticks[0]+" screen="+mc.gui.screen()+" overlay="+mc.gui.overlay());});
        }
    }
    private static void peer(ClientGameTestContext c) {
        try {
            String address = "127.0.0.1:" + System.getProperty(PEER);
            c.runOnClient(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address), new ServerData("Herobrine disposable test", address, ServerData.Type.OTHER), false, null));
            c.waitFor(mc -> {
                if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen) throw new AssertionError("Peer disconnected during login");
                return mc.level != null && mc.player != null;
            }, 2400);
            c.waitFor(mc -> marked("positioned") && mc.player.getY() > 219 && mc.gui.screen() == null, 1200);
            c.waitTicks(20);
            c.runOnClient(mc -> { if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
            mark("peer-ready", "");
            c.waitFor(mc -> marked("entity.txt"), 1200);
            int id = Integer.parseInt(Files.readString(coordination().resolve("entity.txt")));
            c.waitFor(mc -> mc.level.getEntity(id) instanceof Herobrine && Math.abs(mc.player.getYRot()) < 1, 400);
            mark("peer-saw-entity", "");
            c.waitFor(mc -> marked("turn") && Math.abs(Math.abs(mc.player.getYRot()) - 180) < 1, 400);
            mark("peer-facing", "");
            c.waitFor(mc -> marked("both-witnessed"), 400);
            c.takeScreenshot("jugcraft_herobrine_shared_peer");
            mark("peer-screenshot", "");
            c.waitFor(mc -> marked("removed") && mc.level.getEntity(id) == null, 400);
            mark("peer-finished", "");
            c.runOnClient(mc -> { mc.disconnectWithSavingScreen(); mc.gui.setScreen(new TitleScreen()); });
            c.waitForScreen(TitleScreen.class);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
