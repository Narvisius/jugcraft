package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.computers.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Real server ticks, inventories and shared protection callbacks; no interpreter mocking. */
public final class JimtafactsComputerGameTests {
    @GameTest public void programShelfLoadsWithoutRunningAndPreservesDraftOnRejectedWrite(GameTestHelper h){
        var p=player(h);var node=place(h,p,"computer_normal",new BlockPos(2,2,2));
        node.saveProgram("print running draft\nstop");node.start();node.storeProgram("greenhouse","print stored greenhouse\nstop");
        h.assertTrue(node.running()&&node.program().contains("running draft")&&node.savedPrograms().equals(java.util.List.of("greenhouse")),"Storing a draft does not unexpectedly replace the running program");
        node.loadProgram("greenhouse");h.assertTrue(!node.running()&&node.program().contains("stored greenhouse"),"Explicit load stops hardware actions and never autoruns");
        boolean path=false,tooLarge=false;
        try{node.storeProgram("../outside","stop");}catch(IllegalArgumentException expected){path=true;}
        try{node.storeProgram("greenhouse","print a\n".repeat(129));}catch(IllegalArgumentException expected){tooLarge=true;}
        node.loadProgram("greenhouse");h.assertTrue(path&&tooLarge&&node.program().contains("stored greenhouse"),"Invalid path and overlong code leave saved program intact");
        var data=node.saveWithoutMetadata(h.getLevel().registryAccess());node.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),data));
        h.assertTrue(node.savedPrograms().equals(java.util.List.of("greenhouse")),"Shelf survives saving and reloading");node.deleteProgram("greenhouse");
        h.assertTrue(node.savedPrograms().isEmpty()&&node.program().contains("stored greenhouse"),"Deleting a shelf entry does not erase the editor");h.succeed();
    }
    private static final java.util.Map<ServerPlayer,Integer> PLAYERS=new java.util.IdentityHashMap<>();
    static{
        // Helpers' scheduled callbacks stop after a failed test; cleanup instead has a server-tick deadline.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            var it=PLAYERS.entrySet().iterator();while(it.hasNext()){var e=it.next();if(e.getKey().level().getServer()==server&&server.getTickCount()>=e.getValue()){server.getPlayerList().remove(e.getKey());it.remove();}}
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->PLAYERS.keySet().removeIf(p->p.level().getServer()==server));
    }
    @GameTest(maxTicks=140) public void turtleFarmsAndRearrangesRealCargo(GameTestHelper h){
        var p=player(h);var node=place(h,p,"turtle_normal",new BlockPos(2,2,2));h.setBlock(new BlockPos(2,2,1),Blocks.DIRT);h.setBlock(new BlockPos(2,3,1),Blocks.AIR);
        h.setBlock(new BlockPos(1,2,1),Blocks.WATER);
        node.setItem(16,new ItemStack(Items.DIAMOND_HOE));node.setItem(0,new ItemStack(Items.WHEAT_SEEDS,10));
        node.saveProgram("till front\ntransferslot 2 4\nslotvar a 1\nslotvar b 2\ncomparevar same 2\ndetectvar soil front\nprint Cargo $a $b $same $soil\nstop");node.start();
        h.runAfterDelay(100,()->{
            h.assertTrue(h.getBlockState(new BlockPos(2,2,1)).is(Blocks.FARMLAND)&&node.upgrade(0).getDamageValue()==1,"Fitted original hoe performs a real soil change and wears once");
            h.assertTrue(node.getItem(0).getCount()==6&&node.getItem(1).getCount()==4&&node.output().contains("Cargo 6 4 1 1"),"Inventory instructions preserve ten seeds and report actual quantities/comparisons");
            h.assertTrue(!node.running()&&node.energy.getAmount()==972,"Eight bounded instructions and one 20 JE till action are paid");h.succeed();
        });
    }
    @GameTest public void virtualProgramsRegistersAndCanvasSurviveSave(GameTestHelper h){
        var shelf=new ComputerWorkspace();shelf.save("greenhouse","set stock 12\nprint $stock\nstop");shelf.set("stock",12);shelf.set("limited",Long.MAX_VALUE);
        var display=new ComputerCanvas();display.ink(14,15);display.cursor(48,24);display.write("AB");
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());shelf.save(out);display.save(out);
        var in=TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult());var restored=new ComputerWorkspace();restored.load(in);var screen=new ComputerCanvas();screen.load(in);
        h.assertTrue(restored.load("greenhouse").contains("$stock")&&restored.text("Stock $stock").equals("Stock 12")&&restored.get("limited")==1_000_000_000,"Virtual files and bounded registers survive saved block data");
        h.assertTrue(screen.text().charAt(23*48)=='B'&&screen.text().charAt(23*48-1)=='A'&&screen.foreground().charAt(23*48)=='e',"Bottom-edge wrapping scrolls exactly once and retains color");
        boolean rejected=false;try{restored.save("../outside","stop");}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Virtual program names cannot address host paths");h.succeed();
    }
    @GameTest public void canvasRejectsMalformedBlitBeforeChangingAnyCell(GameTestHelper h){
        var canvas=new ComputerCanvas();canvas.write("KEEP");String before=canvas.text();boolean rejected=false;
        try{canvas.blit("fail","00x0","ffff");}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected&&canvas.text().equals(before),"Invalid color input cannot partly overwrite the display");
        var bytes=ComputerAudio.parse("00ff55");h.assertTrue(bytes.length==3&&bytes[1]==(byte)255,"Audio hex is decoded as finite bytes");
        rejected=false;try{ComputerAudio.parse("a");}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Truncated audio byte is rejected");h.succeed();
    }
    @GameTest(maxTicks=140) public void integerBranchesDriveNativePrograms(GameTestHelper h){
        var p=player(h);var node=place(h,p,"computer_normal",new BlockPos(2,2,2));
        node.saveProgram("set loops 0\nadd loops 1\nifvar loops < 3 2\nprint Completed $loops\nstop");node.start();
        h.runAfterDelay(100,()->{h.assertTrue(!node.running()&&node.output().contains("Completed 3"),"Bounded conditional loop reaches its real result on server ticks");
            var tag=node.getUpdateTag(h.getLevel().registryAccess());h.assertTrue(!tag.contains("workspace_files")&&!tag.contains("workspace_values"),"Public world packets never expose private programs or registers");h.succeed();});
    }
    public static ServerPlayer player(GameTestHelper h) {
        var player = JimtaRobotGameTests.player(h);
        PLAYERS.put(player,h.getLevel().getServer().getTickCount()+300);
        return player;
    }
    private static ComputerNode place(GameTestHelper h, ServerPlayer p, String kind, BlockPos pos) {
        h.setBlock(pos, WorkshopComputers.BLOCKS.get(kind));
        var node = h.getBlockEntity(pos, ComputerNode.class); node.owner(p.getUUID()); node.energy.setAmount(1000); return node;
    }
    @GameTest public void allHardwareRegistersAndPowerFuelIsFinite(GameTestHelper h) {
        h.assertTrue(WorkshopComputers.BLOCKS.size() == 16 && WorkshopComputers.ITEMS.size() == 7, "All 23 named computer entries registered");
        var p = player(h); var node = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        node.energy.setAmount(0); node.setItem(0, new ItemStack(Items.COAL, 2));
        h.assertTrue(node.refuel() && node.energy.getAmount() == 2000 && node.getItem(0).getCount() == 1, "One fuel item supplies exactly 2000 shared JE");
        node.energy.setAmount(15000);
        h.assertTrue(!node.refuel() && node.getItem(0).getCount() == 1, "Overfull fuel conversion consumes nothing"); h.succeed();
    }
    @GameTest(maxTicks = 80) public void serverTicksRunThenStopWithoutSignalLeak(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        node.saveProgram("signal front 15\nprint powered workshop\nstop"); node.start();
        h.runAfterDelay(35, () -> {
            h.assertTrue(node.output().contains("powered workshop") && !node.running(), "Program executes on server ticks and stops");
            h.assertTrue(node.energy.getAmount() == 997 && node.signal(Direction.NORTH) == 0, "One JE per instruction and stop clears output"); h.succeed();
        });
    }
    @GameTest public void programsAreBoundedAndDiskRoundtripSurvivesSave(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        node.setItem(0, new ItemStack(WorkshopComputers.ITEMS.get("disk")));
        node.saveProgram("print disk kept my program\nstop"); node.disk(p, true);
        node.saveProgram("stop"); node.disk(p, false);
        var saved = node.saveWithoutMetadata(h.getLevel().registryAccess());
        node.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        h.assertTrue(node.program().contains("disk kept my program") && !node.running(), "Disk and saved program retained; loading never autoruns");
        boolean refused = false; try { node.saveProgram("print x\n".repeat(129)); } catch (IllegalArgumentException expected) { refused = true; }
        h.assertTrue(refused && node.program().contains("disk kept"), "Overlong program rejected before replacing saved program");
        h.assertTrue(!node.getUpdateTag(h.getLevel().registryAccess()).contains("program"), "World render packets do not leak private source"); h.succeed();
    }
    @GameTest(maxTicks = 80) public void turtleMovementPreservesCargoUpgradesAndPaysOnce(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "turtle_normal", new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 1), Blocks.AIR); node.setItem(0, new ItemStack(Items.COBBLESTONE, 13));
        node.setItem(16, new ItemStack(Items.DIAMOND_PICKAXE)); node.saveProgram("move front\nprint arrived\nstop"); node.start();
        h.runAfterDelay(40, () -> {
            h.assertTrue(h.getBlockState(new BlockPos(2, 2, 2)).isAir(), "Old turtle block removed");
            var moved = h.getBlockEntity(new BlockPos(2, 2, 1), ComputerNode.class);
            h.assertTrue(moved.getItem(0).getCount() == 13 && moved.upgrade(0).is(Items.DIAMOND_PICKAXE), "Cargo and fitted upgrade preserved exactly");
            h.assertTrue(moved.output().contains("arrived") && moved.energy.getAmount() == 977, "Program resumes with one move debit and three instruction debits"); h.succeed();
        });
    }
    @GameTest(maxTicks = 80) public void turtleWillNotMineOtherPlayersClaim(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "turtle_normal", new BlockPos(2, 2, 2));
        var target = new BlockPos(2, 2, 1); h.setBlock(target, Blocks.STONE); node.setItem(16, new ItemStack(Items.DIAMOND_PICKAXE));
        var claim = TestClaims.open().block(h.absolutePos(target), UUID.randomUUID());
        node.saveProgram("dig front\nstop"); node.start();
        h.runAfterDelay(30, () -> {
            try {
                h.assertTrue(h.getBlockState(target).is(Blocks.STONE) && node.energy.getAmount() == 999 && !node.running(), "Claim checked before breaking, tool damage or action JE");
                h.assertTrue(node.upgrade(0).getDamageValue() == 0, "Protected block did not wear upgrade");
            } finally { claim.close(); }
            h.succeed();
        });
    }
    @GameTest(maxTicks = 80) public void fittedCraftingUpgradeConsumesRealInputs(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "turtle_normal", new BlockPos(2, 2, 2));
        node.setItem(0, new ItemStack(Items.OAK_LOG)); node.setItem(16, new ItemStack(Items.CRAFTING_TABLE));
        node.saveProgram("craft\nstop"); node.start();
        h.runAfterDelay(30, () -> {
            h.assertTrue(node.getItem(0).is(Items.OAK_PLANKS) && node.getItem(0).getCount() == 4, "One vanilla recipe consumes one log for four planks");
            h.assertTrue(node.upgrade(0).is(Items.CRAFTING_TABLE) && node.energy.getAmount() == 978, "Upgrade preserved and actual craft pays 20 JE plus instructions"); h.succeed();
        });
    }
    @GameTest(maxTicks = 120) public void turtleCannotUseAnUnchargedPoweredCargoTool(GameTestHelper h) {
        var p = player(h); var node = place(h, p, "turtle_normal", new BlockPos(2, 2, 2));
        var target = new BlockPos(2, 2, 1); h.setBlock(target, Blocks.DIRT);
        node.setItem(0, new ItemStack(io.github.jimbozoomer.jugcraft.tools.JugcraftTools.MINING_DRILL));
        node.saveProgram("dig front\nstop"); node.start();
        h.runAfterDelay(30, () -> {
            h.assertTrue(h.getBlockState(target).is(Blocks.DIRT) && node.energy.getAmount() == 999, "Empty tool battery cannot mine even a hand-mineable block");
            var tool = node.getItem(0); var powered = (io.github.jimbozoomer.jugcraft.tools.PoweredToolItem) tool.getItem();
            long cost = powered.energyPerBlock(tool);
            io.github.jimbozoomer.jugcraft.tools.Chargeable.setEnergy(tool, cost);
            node.start();
            h.runAfterDelay(30, () -> {
                h.assertTrue(h.getBlockState(target).isAir(), "Charged cargo drill mines the permitted block");
                h.assertTrue(io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(node.getItem(0)) == 0, "Exactly one tool action charge consumed");
                h.assertTrue(node.energy.getAmount() == 967, "Turtle pays its own 30 JE plus two instructions as well"); h.succeed();
            });
        });
    }
    @GameTest(maxTicks = 100) public void connectedPrinterBindsBookWithoutDuplicatingMaterials(GameTestHelper h) {
        var p = player(h); var computer = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        var printer = place(h, p, "printer", new BlockPos(3, 2, 2));
        printer.setItem(0, new ItemStack(Items.PAPER, 3)); printer.setItem(1, new ItemStack(Items.INK_SAC));
        printer.setItem(2, new ItemStack(Items.STRING)); printer.setItem(3, new ItemStack(Items.LEATHER));
        computer.saveProgram("print workshop journal\nprintbook\nstop"); computer.start();
        h.runAfterDelay(40, () -> {
            var book = printer.getItem(17);
            h.assertTrue(book.is(WorkshopComputers.ITEMS.get("printed_book")) && book.getOrDefault(WorkshopComputers.DOCUMENT, "").contains("workshop journal"), "Readable book carries real output");
            h.assertTrue(printer.getItem(0).is(Items.PAPER) && printer.getItem(0).getCount() == 2, "One printed page consumes one of three sheets");
            for (int i = 1; i < 4; i++) h.assertTrue(printer.getItem(i).isEmpty(), "Ink, binding and leather consumed once"); h.succeed();
        });
    }
    @GameTest(maxTicks = 100) public void monitorWallUsesOriginalConnectedModelState(GameTestHelper h) {
        var p = player(h);
        ComputerNode a = null;
        for (int x = 2; x <= 3; x++) for (int y = 2; y <= 3; y++) {
            var node = place(h, p, "monitor_normal", new BlockPos(x, y, 2)); if (x == 3 && y == 3) a = node;
        }
        var anchor = a;
        h.runAfterDelay(30, () -> {
            h.assertTrue(anchor.monitorOrigin() && anchor.monitorWidth() == 2 && anchor.monitorHeight() == 2, "Owner-matched rectangle forms one display");
            h.assertTrue(anchor.getBlockState().getValue(ComputerMonitors.CONNECTIONS) == 10, "Top-left uses original right/down connection variant"); h.succeed();
        });
    }
    @GameTest(maxTicks = 120) public void modemProtocolPreservesChannelAndReplyWithWiredLink(GameTestHelper h) {
        var p = player(h); var sender = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        var receiver = place(h, p, "computer_advanced", new BlockPos(3, 2, 2));
        receiver.saveProgram("listen 42\nreceive\nstop"); receiver.start();
        sender.saveProgram("wait 15\ntransmit 42 7 chemistry ready\nstop"); sender.start();
        h.runAfterDelay(70, () -> {
            h.assertTrue(receiver.output().contains("channel 42; reply 7") && receiver.output().contains("chemistry ready"), "Bounded inbox routes matching channel and reply channel");
            h.assertTrue(!receiver.running(), "Receive resumes the waiting program"); h.succeed();
        });
    }
    @GameTest(maxTicks = 100) public void unknownLuaAndEmptyPowerCannotExecuteHardware(GameTestHelper h) {
        var p = player(h); var computer = place(h, p, "computer_normal", new BlockPos(2, 2, 2));
        computer.saveProgram("os.execute('anything')\nsignal front 15"); computer.start();
        var unpowered = place(h, p, "computer_advanced", new BlockPos(3, 2, 2)); unpowered.energy.setAmount(0);
        unpowered.saveProgram("signal front 15"); unpowered.start();
        h.runAfterDelay(30, () -> {
            h.assertTrue(!computer.running() && computer.signal(Direction.NORTH) == 0 && computer.output().contains("Lua/CraftOS is not supported"), "Foreign executable syntax is rejected explicitly");
            h.assertTrue(unpowered.cursor() == 0 && unpowered.signal(Direction.NORTH) == 0, "Unpowered computer performs no instruction"); h.succeed();
        });
    }
}
