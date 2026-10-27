package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.finance.*;
import io.github.jimbozoomer.jugcraft.storage.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Opt-in, test-mod-only control and measurement for a separate, normally ticking dedicated JVM. */
public final class StorageCrateMultiplayerHarness implements ModInitializer {
    static final BlockPos LARGE = new BlockPos(512, 220, 512), UPGRADE = LARGE.east(2);
    static final Path ROOT = Path.of(System.getProperty("jugcraft.crate.testDir", ".")).toAbsolutePath();
    private String lastId = "";
    private JsonObject pendingSample;
    private int warmup, sampleTicks;
    private long sampleStart, heapMin, heapMax, gcStart, gcMillisStart;
    private final List<Long> timings = new ArrayList<>();

    @Override public void onInitialize() {
        if (!Boolean.getBoolean("jugcraft.crate.server")) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> write("server-ready.json", success("ready", new JsonPrimitive(ProcessHandle.current().pid()))));
        ServerTickEvents.START_SERVER_TICK.register(this::tick);
    }
    private void tick(MinecraftServer server) {
        try {
            if (pendingSample != null) {
                if (warmup-- > 0) { sampleStart = System.nanoTime(); heapMin = Long.MAX_VALUE; heapMax = 0; gcStart = gcCount(); gcMillisStart = gcMillis(); }
                else {
                    // Fabric START fires at tickChildren, after the increment but before this tick is tallied.
                    check(server.getPlayerCount()==2&&server.getPlayerList().getPlayers().stream().allMatch(p->p.containerMenu instanceof StorageCrateMenu),"Both clients keep crates open during measurement");
                    timings.add(server.getTickTimesNanos()[Math.floorMod(server.getTickCount()-1,100)]);
                    long heap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    heapMin = Math.min(heapMin, heap); heapMax = Math.max(heapMax, heap);
                    if (--sampleTicks == 0) {
                        var sorted = timings.stream().sorted().toList(); var result = new JsonObject();
                        result.addProperty("label", pendingSample.get("value").getAsString());
                        result.addProperty("samples", sorted.size()); result.addProperty("players", server.getPlayerCount());
                        result.addProperty("medianMs", sorted.get(sorted.size()/2)/1e6);
                        result.addProperty("p95Ms", sorted.get((int)Math.ceil(sorted.size()*.95)-1)/1e6);
                        result.addProperty("maxMs", sorted.getLast()/1e6);
                        result.addProperty("over50ms", sorted.stream().filter(n -> n > 50_000_000L).count());
                        result.addProperty("elapsedSeconds", (System.nanoTime()-sampleStart)/1e9);
                        result.addProperty("heapMinMiB", heapMin/1048576.0); result.addProperty("heapMaxMiB", heapMax/1048576.0);
                        result.addProperty("gcCount", gcCount()-gcStart); result.addProperty("gcMillis", gcMillis()-gcMillisStart);
                        result.addProperty("hopperItems", hopperItems(server.overworld()));
                        if(result.get("label").getAsString().equals("active-1024"))verifyAutomation(server.overworld());
                        write("sample-"+result.get("label").getAsString()+".json", result);
                        write("server-response.json", success(pendingSample.get("id").getAsString(), result));
                        pendingSample=null;
                    }
                }
            }
            JsonObject request = read("server-request.json");
            if (request == null || request.get("id").getAsString().equals(lastId)) return;
            lastId=request.get("id").getAsString();
            String op=request.get("op").getAsString(); String value=request.has("value")?request.get("value").getAsString():"";
            if (op.equals("sample")) { check(pendingSample==null,"No overlapping samples"); pendingSample=request; warmup=200; sampleTicks=1200; timings.clear(); return; }
            JsonElement result=execute(server,op,value);
            write("server-response.json",success(lastId,result));
        } catch(Throwable failure) { var response=new JsonObject();response.addProperty("id",lastId);response.addProperty("error",failure.toString());write("server-response.json",response);failure.printStackTrace(); }
    }
    private JsonElement execute(MinecraftServer s,String op,String value) {
        ServerLevel level=s.overworld();
        switch(op) {
            case "prepare" -> {
                check(s.getPlayerCount()==2,"Two real connections required");
                check(s.getPlayerList().getPlayers().stream().map(p->p.getUUID()).distinct().count()==2,"Distinct player identities");
                for(int x=507;x<=518;x++)for(int z=507;z<=516;z++)level.setBlock(new BlockPos(x,219,z),Blocks.STONE.defaultBlockState(),3);
                for(int x=31;x<=36;x++)for(int z=31;z<=36;z++)level.setChunkForced(x,z,true);
                place(level,LARGE,StorageCrateTier.TUNGSTEN);place(level,UPGRADE,StorageCrateTier.BRONZE);
                crate(level,LARGE).setItem(0,new ItemStack(Items.COBBLESTONE,64));
                crate(level,LARGE).setItem(377,new ItemStack(Items.DIAMOND,7));
                var named=new ItemStack(JugcraftStorage.STORAGE_CRATES.get(StorageCrateTier.BRONZE));
                named.set(DataComponents.CUSTOM_NAME,Component.literal("Shared Upgrade"));crate(level,UPGRADE).applyComponentsFromItemStack(named);
                crate(level,UPGRADE).setItem(53,new ItemStack(Items.ENDER_PEARL,16));
                for(var p:s.getPlayerList().getPlayers()) {p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(513.5,220,509.5);}
                command(s,"gamerule minecraft:advance_time false");command(s,"gamerule minecraft:spawn_mobs false");
                command(s,"gamerule minecraft:random_tick_speed 0");command(s,"time set noon");command(s,"weather clear");
            }
            case "race-reset" -> {
                for(var p:s.getPlayerList().getPlayers()){check(p.containerMenu instanceof StorageCrateMenu,"Both viewers open");p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();}
                crate(level,LARGE).setItem(377,new ItemStack(Items.DIAMOND,7));
                for(var p:s.getPlayerList().getPlayers())p.containerMenu.broadcastChanges();
            }
            case "race-check" -> {
                int total=crate(level,LARGE).getItem(377).getCount();int winners=0;
                for(var p:s.getPlayerList().getPlayers()){var carried=p.containerMenu.getCarried();if(carried.is(Items.DIAMOND)){total+=carried.getCount();winners++;}}
                check(total==7&&winners==1&&crate(level,LARGE).getItem(377).isEmpty(),"Exactly one winner, seven diamonds: total="+total+", winners="+winners);
            }
            case "clear-cursors" -> {for(var p:s.getPlayerList().getPlayers()){p.containerMenu.setCarried(ItemStack.EMPTY);p.containerMenu.broadcastChanges();}crate(level,LARGE).setItem(377,new ItemStack(Items.DIAMOND,7));}
            case "give-kit" -> {var p=player(s,value);p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new ItemStack(JugcraftStorage.STORAGE_UPGRADES.get(StorageCrateTier.STEEL)));p.inventoryMenu.broadcastChanges();}
            case "check-upgrade" -> {
                var c=crate(level,UPGRADE);check(c.tier()==StorageCrateTier.STEEL&&c.getItem(53).getCount()==16&&c.getName().getString().equals("Shared Upgrade"),"Upgrade preserves data");
                check(player(s,value).getMainHandItem().isEmpty(),"Exactly one survival kit consumed");
            }
            case "claim" -> {var p=player(s,value);var book=Ledger.get(s).book();check(LandRegistry.get(s).assign(book,LandRegistry.keyOf(LARGE),book.account(p.getUUID(),p.getName().getString()),0).ok(),"Claim assigned");}
            case "unclaim" -> {var p=player(s,value);check(LandRegistry.get(s).abandon(p,Ledger.get(s).book(),LandRegistry.keyOf(LARGE),true,0).ok(),"Claim removed");}
            case "check-gems" -> {
                int total=0;var c=crate(level,LARGE);for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(Items.DIAMOND))total+=c.getItem(i).getCount();
                for(var p:s.getPlayerList().getPlayers()){for(var stack:p.getInventory().getNonEquipmentItems())if(stack.is(Items.DIAMOND))total+=stack.getCount();if(p.containerMenu.getCarried().is(Items.DIAMOND))total+=p.containerMenu.getCarried().getCount();}
                // Vanilla drops a disconnected player's cursor stack instead of putting it in their saved inventory.
                int dropped=0;for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(LARGE).inflate(12)))if(entity.getItem().is(Items.DIAMOND))dropped+=entity.getItem().getCount();
                check(total+dropped==7,"Reconnect/permission conservation: held="+total+", dropped="+dropped);
                var result=new JsonObject();result.addProperty("held",total);result.addProperty("dropped",dropped);return result;
            }
            case "persist" -> {var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setDamageValue(37);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Network Final Slot"));crate(level,LARGE).setItem(377,tool);s.saveEverything(true,true,true);}
            case "check-persist" -> {var stack=crate(level,LARGE).getItem(377);check(stack.is(Items.DIAMOND_PICKAXE)&&stack.getDamageValue()==37&&stack.getHoverName().getString().equals("Network Final Slot"),"Final slot survives process restart");check(crate(level,UPGRADE).tier()==StorageCrateTier.STEEL&&crate(level,UPGRADE).getItem(53).getCount()==16,"Upgrade survives process restart");}
            case "load" -> {
                int count=Integer.parseInt(value);
                for(int i=0;i<count;i++){var pos=loadPos(i);if(!(level.getBlockEntity(pos) instanceof StorageCrateBlockEntity)){place(level,pos,StorageCrateTier.TUNGSTEN);var c=crate(level,pos);for(int slot=0;slot<378;slot++)c.setItem(slot,new ItemStack(slot%2==0?Items.COBBLESTONE:Items.IRON_INGOT,64));}}
            }
            case "hoppers" -> {for(int i=0;i<64;i++){
                var pos=loadPos(i);level.setBlock(pos.below(),Blocks.HOPPER.defaultBlockState(),3);
                // The two hopper rows read outward, so comparator outputs cannot lock another test hopper.
                var side=i<32?Direction.NORTH:Direction.SOUTH;var comparator=pos.relative(side);
                level.setBlock(comparator.below(),Blocks.STONE.defaultBlockState(),3);
                level.setBlock(comparator,Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,side.getOpposite()),3);
                level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
            }}
            case "check-load" -> {long total=0;for(int i=0;i<1024;i++){var c=crate(level,loadPos(i));for(int j=0;j<378;j++)total+=c.getItem(j).getCount();}long moved=hopperItems(level);check(moved>0&&total+moved==1024L*378*64,"Automation conserves all load items: "+total+" + "+moved);}
            case "players" -> {var result=new JsonArray();for(var p:s.getPlayerList().getPlayers())result.add(p.getName().getString());return result;}
            case "stop" -> s.halt(false);
            default -> throw new IllegalArgumentException(op);
        }
        return new JsonPrimitive("passed "+op);
    }
    private static long hopperItems(ServerLevel level){long n=0;for(int i=0;i<64;i++)if(level.getBlockEntity(loadPos(i).below()) instanceof HopperBlockEntity h)for(int j=0;j<h.getContainerSize();j++)n+=h.getItem(j).getCount();return n;}
    private static void verifyAutomation(ServerLevel level){for(int i=0;i<64;i++){
        var pos=loadPos(i);check(level.getBlockState(pos.below()).getValue(HopperBlock.ENABLED),"Hopper "+i+" remains enabled");
        var hopper=(HopperBlockEntity)level.getBlockEntity(pos.below());int moved=0;for(int slot=0;slot<hopper.getContainerSize();slot++)moved+=hopper.getItem(slot).getCount();
        check(moved>=150,"Every hopper transfers throughout the measurement: "+i+" moved "+moved);
        var comparator=(net.minecraft.world.level.block.entity.ComparatorBlockEntity)level.getBlockEntity(pos.relative(i<32?Direction.NORTH:Direction.SOUTH));
        check(comparator.getOutputSignal()==14,"Comparator reads the partially extracted full crate");
    }}
    static BlockPos loadPos(int i){return new BlockPos(528+(i%32)*2,220,528+(i/32)*2);}
    static StorageCrateBlockEntity crate(ServerLevel l,BlockPos p){return (StorageCrateBlockEntity)l.getBlockEntity(p);}
    static void place(ServerLevel l,BlockPos p,StorageCrateTier t){l.setBlock(p,JugcraftStorage.STORAGE_CRATES.get(t).defaultBlockState(),3);}
    static ServerPlayer player(MinecraftServer s,String name){return Objects.requireNonNull(s.getPlayerList().getPlayerByName(name));}
    static void command(MinecraftServer s,String command){s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),command);}
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static JsonObject success(String id,JsonElement result){var j=new JsonObject();j.addProperty("id",id);j.add("result",result);return j;}
    static JsonObject read(String name){try{var path=ROOT.resolve(name);return Files.exists(path)?JsonParser.parseString(Files.readString(path)).getAsJsonObject():null;}catch(IOException e){throw new UncheckedIOException(e);}}
    static void write(String name,JsonObject json){try{Files.createDirectories(ROOT);var temp=ROOT.resolve(name+".tmp");Files.writeString(temp,json.toString());Files.move(temp,ROOT.resolve(name),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){throw new UncheckedIOException(e);}}
    private static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
}
