package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.StorageCrateScreen;
import io.github.jimbozoomer.jugcraft.storage.StorageCrateMenu;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.Direction;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.*;
import static io.github.jimbozoomer.jugcraft.test.StorageCrateMultiplayerHarness.*;

/** Explicit opt-in: two rendered JVMs and a third dedicated-server JVM, all communicating over loopback TCP. */
public final class StorageCrateMultiplayerClientGameTests implements FabricClientGameTest {
    private int previousMenu;
    private String address;
    private ClientGameTestContext c;
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("jugcraft.crate.multiplayer")) return;
        c=context;
        c.runOnClient(mc->{mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.framerateLimit().set(60);});
        if (System.getProperty("jugcraft.crate.role","A").equals("B")) { secondary(); return; }
        Process server=null, second=null;
        try {
            Files.createDirectories(ROOT);
            int port;try(var socket=new ServerSocket(0,0,java.net.InetAddress.getLoopbackAddress())){port=socket.getLocalPort();}
            address="127.0.0.1:"+port;
            var settings=new JsonObject();settings.addProperty("address",address);write("connection.json",settings);
            Path serverDir=ROOT.resolve("server");Files.createDirectories(serverDir);
            Files.writeString(serverDir.resolve("eula.txt"),"eula=true\n");
            Files.writeString(serverDir.resolve("server.properties"),"server-ip=127.0.0.1\nserver-port="+port+"\nonline-mode=false\nwhite-list=false\nenforce-secure-profile=false\nlevel-name=world\nlevel-seed=storage-crate-network\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=4\nsimulation-distance=4\nspawn-protection=0\nmax-players=2\npause-when-empty-seconds=0\nallow-flight=true\n");
            seedConfig(serverDir);server=launch("server",serverDir);
            await(()->read("server-ready.json")!=null,240,"Dedicated server startup");
            Path bDir=ROOT.resolve("client-b");Files.createDirectories(bDir);seedConfig(bDir);second=launch("B",bDir);
            connect();await(()->read("client-b-ready.json")!=null,240,"Second independent client joins");
            String owner=c.computeOnClient(mc->mc.player.getName().getString());
            check(request("server","players","").getAsJsonArray().size()==2,"Two distinct local test identities connected");
            request("server","prepare","");c.waitTicks(40);
            action("open","large");request("client-b","open","large");
            action("search","diamond");request("client-b","search","cobblestone");
            check(c.computeOnClient(mc->((StorageCrateScreen)mc.gui.screen()).matchCount()==1),"A search stays independent");
            c.takeScreenshot("storage_crate_two_clients_a");request("client-b","screenshot","storage_crate_two_clients_b");
            request("client-b","search","diamond");
            for(int attempt=0;attempt<12;attempt++) {
                request("server","race-reset","");
                action("wait-stack","377");request("client-b","wait-stack","377");
                String id=send("client-b","click","377");action("click","377");response("client-b",id,60);
                c.waitTicks(10);request("server","race-check","");
            }
            Jugcraft.LOGGER.info("[crate-network] Passed 12 competing extraction rounds, exactly seven diamonds each");
            request("server","clear-cursors","");action("close","");request("client-b","close","");
            action("open","upgrade");request("client-b","open","upgrade");
            action("close","");request("server","give-kit",owner);c.waitTicks(10);
            action("use","upgrade");request("client-b","wait-closed","");
            request("client-b","stale-click","53");c.waitTicks(10);request("server","check-upgrade",owner);
            action("open","large");request("client-b","open","large");
            request("server","claim",owner);request("client-b","wait-closed","");
            request("client-b","stale-click","377");c.waitTicks(10);request("server","check-gems","");
            Jugcraft.LOGGER.info("[crate-network] Passed upgrade and permission revocation with stale network clicks");
            request("server","unclaim",owner);request("client-b","open","large");request("client-b","search","diamond");
            request("client-b","click","377");c.waitTicks(10);request("client-b","disconnect","");
            await(()->request("server","players","").getAsJsonArray().size()==1,30,"Disconnected player removed on server");
            request("client-b","connect","");c.waitTicks(20);Jugcraft.LOGGER.info("[crate-network] Cursor disconnect/reconnect conservation: {}",request("server","check-gems",""));
            request("server","persist","");action("close","");request("client-b","disconnect","");disconnect();
            request("server","stop","");Process stopping=server;await(()->!stopping.isAlive(),120,"Server saves and exits");
            Files.delete(ROOT.resolve("server-ready.json"));Files.delete(ROOT.resolve("server-request.json"));
            server=launch("server",serverDir);await(()->read("server-ready.json")!=null,240,"Dedicated server restart");
            connect();request("client-b","connect","");c.waitTicks(30);request("server","check-persist","");request("server","check-gems","");
            Jugcraft.LOGGER.info("[crate-network] Passed full dedicated process restart, final-slot components and item conservation");
            action("open","large");request("client-b","open","large");
            action("search","Network Final Slot");request("client-b","search","cobblestone");
            action("wait-stack","377");c.takeScreenshot("storage_crate_multiplayer_after_restart");
            for(String label:List.of("baseline","filled-256","filled-1024","active-1024")) {
                if(label.equals("filled-256"))request("server","load","256");
                if(label.equals("filled-1024"))request("server","load","1024");
                if(label.equals("active-1024"))request("server","hoppers","");
                Jugcraft.LOGGER.info("[crate-network] Starting 200 warmup + 1200 measured ticks: {}",label);
                JsonElement result=response("server",send("server","sample",label),180);
                Jugcraft.LOGGER.info("[crate-network] {}",result);
            }
            request("server","check-load","");request("server","persist","");
            action("close","");request("client-b","finish","");disconnect();request("server","stop","");
            Process finalServer=server, finalSecond=second;await(()->!finalServer.isAlive()&&!finalSecond.isAlive(),120,"Both child processes exit");
            check(server.exitValue()==0&&second.exitValue()==0,"Clean child exit codes");
            var passed=new JsonObject();passed.addProperty("status","PASS");passed.addProperty("races",12);passed.addProperty("clientA",ProcessHandle.current().pid());passed.addProperty("clientB",second.pid());passed.addProperty("dedicatedServer",server.pid());write("result.json",passed);
            Jugcraft.LOGGER.info("[crate-network] PASS: two JVM clients, dedicated JVM, races, independent search, upgrade, permissions, cursor disconnect, restart and four load stages");
        } catch(IOException e) {throw new UncheckedIOException(e);}
        finally {
            if(c.computeOnClient(mc->mc.level!=null))disconnect();
            for(Process p:Arrays.asList(second,server))if(p!=null&&p.isAlive())p.destroy();
        }
    }
    private void secondary() {
        address=Objects.requireNonNull(read("connection.json")).get("address").getAsString();connect();
        write("client-b-ready.json",success("ready",new JsonPrimitive(ProcessHandle.current().pid())));
        String last="";
        while(true) {
            var request=read("client-b-request.json");
            if(request!=null&&!request.get("id").getAsString().equals(last)) {
                last=request.get("id").getAsString();String op=request.get("op").getAsString();
                try{action(op,request.get("value").getAsString());write("client-b-response.json",success(last,new JsonPrimitive("passed "+op)));if(op.equals("finish"))return;}
                catch(Throwable t){var failed=new JsonObject();failed.addProperty("id",last);failed.addProperty("error",t.toString());write("client-b-response.json",failed);throw t;}
            }
            c.waitTicks(2);
        }
    }
    private void action(String op,String value) {
        switch(op) {
            case "open" -> {use(value);await(()->c.computeOnClient(mc->mc.gui.screen() instanceof StorageCrateScreen),30,"Crate opens through network");previousMenu=c.computeOnClient(mc->mc.player.containerMenu.containerId);c.waitTicks(10);}
            case "use" -> use(value);
            case "search" -> {c.runOnClient(mc->((StorageCrateScreen)mc.gui.screen()).searchFor(value));c.waitTicks(5);}
            case "click" -> click(Integer.parseInt(value));
            case "wait-stack" -> await(()->c.computeOnClient(mc->mc.player!=null&&mc.player.containerMenu instanceof StorageCrateMenu menu&&!menu.getSlot(Integer.parseInt(value)).getItem().isEmpty()),30,"Stack reaches client");
            case "close" -> {c.runOnClient(mc->mc.player.closeContainer());await(()->c.computeOnClient(mc->!(mc.gui.screen() instanceof StorageCrateScreen)),30,"Menu closes");}
            case "wait-closed" -> await(()->c.computeOnClient(mc->!(mc.gui.screen() instanceof StorageCrateScreen)),30,"Server invalidates menu");
            case "stale-click" -> c.runOnClient(mc->mc.getConnection().send(new ServerboundContainerClickPacket(previousMenu,0,(short)Integer.parseInt(value),(byte)0,ContainerInput.PICKUP,new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(),HashedStack.EMPTY)));
            case "screenshot" -> c.takeScreenshot(value);
            case "disconnect", "finish" -> disconnect();
            case "connect" -> connect();
            default -> throw new IllegalArgumentException(op);
        }
    }
    private void use(String target){c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(target.equals("large")?LARGE:UPGRADE),Direction.NORTH,target.equals("large")?LARGE:UPGRADE,false)));}
    private void click(int index) {
        double[] xy=c.computeOnClient(mc->{var screen=(StorageCrateScreen)mc.gui.screen();var slot=screen.getMenu().getSlot(index);check(slot.isActive(),"Clicked slot visible");return new double[]{((screen.width-202)/2.0+slot.x+8)/MouseHandler.getScaledXPos(mc.getWindow(),1),((screen.height-(152+18*screen.visibleRows()))/2.0+slot.y+8)/MouseHandler.getScaledYPos(mc.getWindow(),1)};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTick();c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(3);
    }
    private void connect(){c.runOnClient(mc->ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("Local crate test",address,ServerData.Type.OTHER),false,null));await(()->c.computeOnClient(mc->{check(!(mc.gui.screen() instanceof DisconnectedScreen),"Connection rejected; inspect server log");return mc.player!=null&&mc.level!=null&&mc.gui.screen()==null;}),120,"Network join "+address);c.waitTicks(20);}
    private void disconnect(){c.runOnClient(mc->{if(mc.level!=null)mc.level.disconnect(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);mc.disconnect(new TitleScreen(),false);});c.waitForScreen(TitleScreen.class);}
    private String send(String target,String op,String value){String id=UUID.randomUUID().toString();var request=new JsonObject();request.addProperty("id",id);request.addProperty("op",op);request.addProperty("value",value);write(target+"-request.json",request);return id;}
    private JsonElement request(String target,String op,String value){return response(target,send(target,op,value),120);}
    private JsonElement response(String target,String id,int seconds){await(()->{var r=read(target+"-response.json");return r!=null&&r.get("id").getAsString().equals(id);},seconds,target+" response");var r=read(target+"-response.json");check(!r.has("error"),r.toString());return r.get("result");}
    private void await(BooleanSupplier predicate,int seconds,String description){long until=System.nanoTime()+seconds*1_000_000_000L;while(!predicate.getAsBoolean()){check(System.nanoTime()<until,"Timed out: "+description);c.waitTicks(2);}}
    private void seedConfig(Path dir)throws IOException {for(String relative:List.of("config/travelersbackpack-common.toml","defaultconfigs/travelersbackpack-server.toml","options.txt","diorama/config.json")){Path source=Path.of(relative);if(Files.exists(source)){Path out=dir.resolve(relative);Files.createDirectories(out.getParent());Files.copy(source,out,StandardCopyOption.REPLACE_EXISTING);}}}
    private Process launch(String role,Path directory)throws IOException {
        var args=new ArrayList<String>();boolean server=role.equals("server");
        for(String arg:ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if(arg.startsWith("-Xmx")||arg.startsWith("-Dfabric.dli.main=")||arg.startsWith("-Dfabric.dli.env=")||arg.startsWith("-Djugcraft.crate.role=")||arg.startsWith("-Djugcraft.crate.server="))continue;
            if(server&&(arg.startsWith("-Dfabric.client.gametest")||arg.startsWith("-Dfabric-api.gametest")))continue;
            args.add(arg);
        }
        args.add("-Xmx4G");args.add("-Dfabric.dli.env="+(server?"server":"client"));args.add("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.Knot"+(server?"Server":"Client"));
        args.add("-Djugcraft.crate.role="+role);args.add("-Djugcraft.crate.server="+server);
        args.add("-cp");args.add(System.getProperty("java.class.path"));args.add("net.fabricmc.devlaunchinjector.Main");
        if(server)args.add("--nogui");else{args.addAll(List.of("--username","CrateClientB","--uuid","00000000-0000-0000-0000-000000000002","--accessToken","0"));}
        Path argFile=ROOT.resolve(role+".args");Files.write(argFile,args.stream().map(a->"\""+a.replace("\\","\\\\").replace("\"","\\\"")+"\"").toList());
        return new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"@"+argFile).directory(directory.toFile()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(ROOT.resolve(role+".log").toFile())).start();
    }
}
