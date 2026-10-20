package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
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

/** Two actual client JVMs against a disposable loopback dedicated server. Never uses an owner profile. */
public final class OwnerCollectionsNetworkTests implements FabricClientGameTest {
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
    @Override public void runTest(ClientGameTestContext c){
        if(System.getProperty(PEER)!=null){peer(c);return;}
        Process peer=null;
        try {
            System.setProperty("jugcraft.ownerTestCoord",Path.of("owner-network-evidence",UUID.randomUUID().toString()).toAbsolutePath().toString());
            Files.createDirectories(coordination());
            var properties=new Properties();properties.setProperty("server-ip","127.0.0.1");properties.setProperty("online-mode","false");properties.setProperty("view-distance","4");properties.setProperty("simulation-distance","4");properties.setProperty("max-players","2");
            try(var server=c.worldBuilder().adjustSettings(s->{s.setWorldType(new net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry(s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));s.setGenerateStructures(false);}).createServer(properties)){
                UUID[] petId=new UUID[1];UUID[] ownerId=new UUID[1];
                try(var connection=server.connect()){
                    connection.waitForChunksRender();server.runCommand("time set noon");server.runCommand("gamerule minecraft:spawn_mobs false");server.runCommand("gamemode creative @a");
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();ownerId[0]=p.getUUID();var pet=Menagerie.TYPES.get("wildlife_hedgehog").create(p.level(),EntitySpawnReason.COMMAND);pet.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);pet.tame(p.getUUID());pet.sit(true);pet.variant(1);pet.setBaby(true);p.level().addFreshEntity(pet);petId[0]=pet.getUUID();});
                    mark("pet.txt",petId[0]+"\n"+ownerId[0]);
                    server.runOnServer(s->s.getPlayerList().getWhiteList().add(new net.minecraft.server.players.UserWhiteListEntry(net.minecraft.server.players.NameAndId.createOffline("OwnerPeer"))));
                    InProcessServerConfigs.beforeAdditionalClient(server);
                    peer=launchPeer(server.computeOnServer(s->s.getPort()),c);mark("peer-pid.txt",Long.toString(peer.pid()));
                    final Process startingPeer=peer;peer.onExit().thenAccept(done->mark("peer-exit.txt",Integer.toString(done.exitValue())));
                    server.waitFor(s->{if(!startingPeer.isAlive())throw peerFailure("Second client exited before login: "+startingPeer.exitValue());return s.getPlayerList().getPlayerByName("OwnerPeer")!=null;},2400);
                    server.runCommand("tp OwnerPeer @p[name=!OwnerPeer]");
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayerByName("OwnerPeer");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("wildlife_capture_net")));});
                    mark("ready","");
                    c.waitFor(mc->marked("attempted"),1200);
                    c.waitTicks(20);
                    server.runOnServer(s->{if(!(s.overworld().getEntity(petId[0]) instanceof MenagerieAnimal a)||!ownerId[0].equals(a.owner()))throw new AssertionError("Peer stole or altered owned pet");});
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayer(ownerId[0]);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Menagerie.item("wildlife_capture_net")));});
                    c.waitTicks(5);
                    c.runOnClient(mc->{var animal=mc.level.getEntitiesOfClass(MenagerieAnimal.class,mc.player.getBoundingBox().inflate(12)).stream().filter(a->a.getUUID().equals(petId[0])).findFirst().orElseThrow();mc.gameMode.interact(mc.player,animal,new net.minecraft.world.phys.EntityHitResult(animal),InteractionHand.MAIN_HAND);});
                    server.waitFor(s->s.overworld().getEntity(petId[0])==null,100);
                    mark("captured","");c.waitFor(mc->marked("peer-finished"),1200);
                    final Process other=peer;c.waitFor(mc->!other.isAlive(),1200);
                    if(peer.exitValue()!=0)throw peerFailure("Second client failed: "+peer.exitValue());
                }
                InProcessServerConfigs.beforeReconnect(server);
                try(var connection=server.connect()){
                    connection.waitForChunksRender();
                    server.runOnServer(s->{var p=s.getPlayerList().getPlayer(ownerId[0]);if(p==null||p.getInventory().getNonEquipmentItems().stream().filter(stack->stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getStringOr("Keeper","").equals(ownerId[0].toString())).mapToInt(ItemStack::getCount).sum()!=1)throw new AssertionError("Exactly one captured pet must survive reconnect");});
                    var reports=new ArrayList<Map<String,Object>>();
                    for(int count:new int[]{0,64,192}){
                        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(var a:p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(120)))a.discard();String[] ids={"wildlife_deer","wildlife_capybara","wildlife_tortoise","wildlife_zebra","wildlife_hedgehog","wildlife_elephant"};for(int i=0;i<count;i++){var a=Menagerie.TYPES.get(ids[i%ids.length]).create(p.level(),EntitySpawnReason.COMMAND);a.snapTo(p.getX()+(i%16-8)*3,p.getY(),p.getZ()+(i/16-6)*3,0,0);a.setPersistenceRequired();a.setPermanentlyInvulnerable(true);p.level().addFreshEntity(a);}});
                        c.waitTicks(240);
                        reports.add(server.computeOnServer(s->{long[] times=s.getTickTimesNanos().clone();Arrays.sort(times);var p=s.getPlayerList().getPlayers().getFirst();int alive=p.level().getEntitiesOfClass(MenagerieAnimal.class,p.getBoundingBox().inflate(160)).size();if(alive!=count)throw new AssertionError("Population fixture lost entities: "+alive+"/"+count);return Map.<String,Object>of("animals",count,"meanMs",s.getAverageTickTimeNanos()/1e6,"p95Ms",times[(int)(times.length*.95)]/1e6,"maxMs",times[times.length-1]/1e6,"samples",times.length);}));
                    }
                    mark("population.json",new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
                }
            }
            mark("passed","Two client JVMs: state replication, ownership rejection, capture, peer removal, reconnect; synthetic live-AI population timings.");
        }catch(Exception e){throw new RuntimeException(e);}finally{if(peer!=null&&peer.isAlive())peer.destroy();}
    }
    private static Process launchPeer(int port,ClientGameTestContext c)throws Exception{
        var dir=coordination().resolve("peer-profile");Files.createDirectories(dir);Files.writeString(dir.resolve("options.txt"),"preferredGraphicsBackend:\"opengl\"\nfullscreen:false\nlang:en_us\n");
        var metadata=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("jugcraft-test").orElseThrow().findPath("fabric.mod.json").orElseThrow();
        var resources=coordination().resolve("peer-test-resources");Files.createDirectories(resources);
        try(var paths=Files.walk(metadata.getParent())){for(var file:paths.filter(Files::isRegularFile).toList()){var target=resources.resolve(metadata.getParent().relativize(file));Files.createDirectories(target.getParent());Files.copy(file,target,StandardCopyOption.REPLACE_EXISTING);}}
        var json=com.google.gson.JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
        var tests=new com.google.gson.JsonArray();tests.add(OwnerCollectionsNetworkTests.class.getName());json.getAsJsonObject("entrypoints").add("fabric-client-gametest",tests);
        var client=new com.google.gson.JsonArray();client.add(OwnerCollectionsNetworkTests.class.getName()+"$PeerStartup");json.getAsJsonObject("entrypoints").add("client",client);
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
    private static void peer(ClientGameTestContext c){
        mark("peer-entered","");
        try{
            String address="127.0.0.1:"+System.getProperty(PEER);
            c.runOnClient(mc->ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Owner disposable test",address,ServerData.Type.OTHER),false,null));
            c.waitFor(mc->{if(mc.gui.screen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen)throw new AssertionError("Peer disconnected during login; see peer.log");return mc.level!=null&&mc.player!=null;},2400);
            c.waitFor(mc->marked("ready"),1200);
            c.waitFor(mc->mc.player.getMainHandItem().is(Menagerie.item("wildlife_capture_net")),200);
            var lines=Files.readAllLines(coordination().resolve("pet.txt"));UUID pet=UUID.fromString(lines.get(0)),owner=UUID.fromString(lines.get(1));
            c.waitFor(mc->mc.level.getEntitiesOfClass(MenagerieAnimal.class,mc.player.getBoundingBox().inflate(20)).stream().anyMatch(a->a.getUUID().equals(pet)&&owner.equals(a.owner())&&a.sitting()&&a.isBaby()),400);
            c.runOnClient(mc->{var a=mc.level.getEntitiesOfClass(MenagerieAnimal.class,mc.player.getBoundingBox().inflate(20)).stream().filter(e->e.getUUID().equals(pet)).findFirst().orElseThrow();mc.gameMode.interact(mc.player,a,new net.minecraft.world.phys.EntityHitResult(a),InteractionHand.MAIN_HAND);});
            c.waitTicks(20);mark("attempted","");
            c.waitFor(mc->marked("captured"),1200);
            c.waitFor(mc->mc.level.getEntitiesOfClass(MenagerieAnimal.class,mc.player.getBoundingBox().inflate(20)).stream().noneMatch(a->a.getUUID().equals(pet)),200);
            c.takeScreenshot("owner_peer_capture_sync");mark("peer-finished","");c.runOnClient(mc->{mc.disconnectWithSavingScreen();mc.gui.setScreen(new TitleScreen());});c.waitForScreen(TitleScreen.class);
        }catch(Exception e){throw new RuntimeException(e);}
    }
}
