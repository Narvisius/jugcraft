package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;

/** All new families share one real rendered client. The resulting save is copied for independent manual play. */
public class CompleteAlchemyClientGameTests implements FabricClientGameTest {
    private static final List<BlockPos> OWNED=new ArrayList<>();
    private static AlchemyMachinesBlockEntity machine(ServerPlayer p,BlockPos at,String id){
        p.level().setBlockAndUpdate(at,AlchemyMachines.BLOCKS.get(id).defaultBlockState().setValue(AlchemyMachinesBlock.FACING,Direction.SOUTH));
        var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(at);be.placedBy(p);be.setEssence(8000);OWNED.add(at);return be;
    }
    private static void sign(ServerPlayer p,BlockPos at,String... lines){
        p.level().setBlockAndUpdate(at,Blocks.DARK_OAK_SIGN.defaultBlockState());var be=(SignBlockEntity)p.level().getBlockEntity(at);
        var text=Arrays.stream(lines).map(Component::literal).map(t->(Component)t).toList();
        for(var side:SignTextSlot.values())be.setText(new SignText(text,text,DyeColor.WHITE,false),side);
        be.setWaxed(true);be.setChanged();p.level().sendBlockUpdated(at,be.getBlockState(),be.getBlockState(),3);
    }
    private static void supplies(ServerPlayer p,BlockPos at,List<Item> items,String title){
        if(items.size()>27)throw new IllegalArgumentException("Showroom chest overflow: "+title);
        p.level().setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)p.level().getBlockEntity(at);
        for(int i=0;i<items.size();i++)chest.setItem(i,new ItemStack(items.get(i)));
        sign(p,at.south(),title,"Right-click items","Read their tooltips","Essence is supplied");
    }
    private static void hold(ServerPlayer p,String id){
        p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new ItemStack(AlchemyStorage.ITEMS.get(id)));
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
    }
    private static void close(ClientGameTestContext c){c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);c.waitTicks(5);}
    private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc->mc.gui.toastManager().clear());c.waitTicks(2);c.takeScreenshot(name);}
    private static void bindPack(Path save){
        var root=save.resolve("datapacks/jugcraft_complete_alchemy_welcome");var functions=root.resolve("data/jugcraft_playtest/function");
        var tags=root.resolve("data/minecraft/tags/function");
        var tick=new StringBuilder("execute unless data storage jugcraft_playtest:setup bound as @a[limit=1] at @s");var bind=new StringBuilder();
        for(var pos:OWNED){String at=pos.getX()+" "+pos.getY()+" "+pos.getZ();tick.append(" if loaded ").append(at);bind.append("data modify block ").append(at).append(" owner set from entity @s UUID\n");}
        tick.append(" run function jugcraft_playtest:bind\n");bind.append("data modify storage jugcraft_playtest:setup bound set value 1b\n");
        try{
            Files.createDirectories(functions);Files.createDirectories(tags);
            Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Local alchemy showroom: first-player workshop ownership\",\"min_format\":[121,0],\"max_format\":[121,0]}}");
            Files.writeString(tags.resolve("tick.json"),"{\"values\":[\"jugcraft_playtest:tick\"]}");
            Files.writeString(functions.resolve("tick.mcfunction"),tick);Files.writeString(functions.resolve("bind.mcfunction"),bind);
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    @Override public void runTest(ClientGameTestContext c){
        OWNED.clear();
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();var start=c.computeOnClient(mc->mc.player.blockPosition());
            int x=start.getX(),y=101,z=start.getZ();var base=new BlockPos(x,y,z);
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-18,y-2,z-14,x+18,y+9,z+14));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-18,y-3,z-14,x+18,y-1,z+14));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("setworldspawn %d %d %d".formatted(x,y,z+3));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();
                if(Assaying.knows(p))throw new AssertionError("Fresh equipment test must start without Assay research");
                p.getInventory().clearContent();Assaying.setBalance(p,500000);
                for(int i=1;i<=3;i++){
                    var relay=machine(p,base.offset(-16+4*i,0,-8),"relay_mk"+i);relay.setItem(1,new ItemStack(Items.COAL,64));relay.setItem(0,new ItemStack(KleinStars.ITEMS.get("klein_star_4")));
                    sign(p,relay.getBlockPos().south(2),"RELAY MK "+i,"Materials -> essence","Front face output","Or charge a star");
                }
                for(int i=0;i<2;i++){
                    var furnace=machine(p,base.offset(4*i,0,-8),i==0?"dark_matter_furnace":"red_matter_furnace");furnace.setItem(1,new ItemStack(Items.RAW_IRON,64));
                    sign(p,furnace.getBlockPos().south(2),i==0?"DARK FURNACE":"RED FURNACE","16 essence per item","Store / take essence","Pause / hopper ports");
                }
                var pedestal=machine(p,base.offset(8,0,-8),"dark_matter_pedestal");pedestal.setItem(1,new ItemStack(AlchemyArtifacts.ITEMS.get("life_stone")));
                machine(p,base.offset(12,0,-8),"interdiction_torch");
                sign(p,base.offset(8,0,-6),"PEDESTAL / WARD","Life Stone is kept","Party support","Nearby monster ward");
                p.level().setBlockAndUpdate(base.offset(-8,0,0),AlchemyStorage.BLOCKS.get("alchemical_chest").defaultBlockState().setValue(AlchemyStorageBlock.FACING,Direction.SOUTH));
                var chest=(AlchemyStorageBlockEntity)p.level().getBlockEntity(base.offset(-8,0,0));chest.placedBy(p);OWNED.add(base.offset(-8,0,0));chest.setItem(0,new ItemStack(Items.COAL,64));
                p.level().setBlockAndUpdate(base.offset(-4,0,0),AlchemyStorage.BLOCKS.get("transmutation_table").defaultBlockState());
                sign(p,base.offset(-8,0,2),"ALCHEMICAL CHEST","54 physical slots","Party access","Mine -> contents drop");
                sign(p,base.offset(-4,0,2),"TRANSMUTATION","Dissolve -> essence","Dissolve once to learn","Withdraw at value");
                // Real collector -> relay -> furnace line. Relay's south face points into the furnace.
                p.level().setBlockAndUpdate(base.offset(3,0,1),AlchemicalWorkshops.BLOCKS.get("collector_mk3").defaultBlockState());
                var collector=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(base.offset(3,0,1));collector.placedBy(p);OWNED.add(base.offset(3,0,1));
                machine(p,base.offset(4,0,1),"relay_mk3");var connected=machine(p,base.offset(4,0,2),"red_matter_furnace");connected.setEssence(0);connected.setItem(1,new ItemStack(Items.RAW_GOLD,64));
                sign(p,base.offset(7,0,2),"CONNECTED LINE","Collector -> relay","Relay -> furnace","Gold in output slots");
                for(int tier=1;tier<=2;tier++){
                    var at=base.offset(6+4*tier,0,6);p.level().setBlockAndUpdate(at,AlchemicalWorkshops.BLOCKS.get("condenser_mk"+tier).defaultBlockState().setValue(AlchemicalWorkshopBlock.FACING,Direction.SOUTH));
                    var be=(AlchemicalWorkshopBlockEntity)p.level().getBlockEntity(at);be.placedBy(p);be.setEssence(8000);be.setItem(1,new ItemStack(Items.OAK_LOG));OWNED.add(at);
                    sign(p,at.south(2),"CONDENSER MK "+tier,"Sample -> copies","Uses stored essence","Animated owner art");
                }
                var equipment=new ArrayList<>(ExtendedAlchemyEquipment.ITEMS.values());
                supplies(p,base.offset(-15,0,10),equipment.subList(0,27),"EQUIPMENT / I");supplies(p,base.offset(-10,0,10),equipment.subList(27,equipment.size()),"EQUIPMENT / II");
                supplies(p,base.offset(-5,0,10),new ArrayList<>(AlchemyStorage.ITEMS.values()),"BAGS / TABLETS");
                supplies(p,base.offset(0,0,10),new ArrayList<>(AlchemyArtifacts.ITEMS.values()),"RINGS / ARTIFACTS");
                var workshops=new ArrayList<Item>();AlchemyMachines.BLOCKS.values().forEach(b->workshops.add(b.asItem()));AlchemicalWorkshops.BLOCKS.values().forEach(b->workshops.add(b.asItem()));
                supplies(p,base.offset(5,0,10),workshops,"WORKSHOPS / NOVA");
                var old=new ArrayList<>(DarkMatter.ITEMS.values());old.addAll(KleinStars.ITEMS.values());old.add(AlchemicalGear.STONE);old.add(AlchemicalGear.TALISMAN);old.add(AlchemicalGear.BAND);old.add(Items.IRON_INGOT);old.add(Items.GOLD_INGOT);old.add(Items.COAL);old.add(Items.GLASS);
                supplies(p,base.offset(10,0,10),old,"STARS / MATERIALS");
                p.level().setBlockAndUpdate(base.offset(15,0,10),Blocks.CHEST.defaultBlockState());var supply=(ChestBlockEntity)p.level().getBlockEntity(base.offset(15,0,10));
                Item[] stock={Items.GLASS,Items.COAL,Items.RAW_IRON,Items.IRON_INGOT,Items.GOLD_INGOT,Items.COOKED_BEEF,Items.WHEAT_SEEDS,Items.CARROT,Items.POTATO,Items.STONE,Items.OAK_LOG,Items.HOPPER,Items.LEVER,Items.WATER_BUCKET,Items.HUSK_SPAWN_EGG,Items.SHEEP_SPAWN_EGG,Items.COW_SPAWN_EGG,Items.EXPERIENCE_BOTTLE};
                for(int i=0;i<stock.length;i++)supply.setItem(i,new ItemStack(stock[i],Math.min(64,stock[i].getDefaultMaxStackSize())));
                sign(p,base.offset(15,0,11),"PRACTICE SUPPLIES","Real blocks / crops","Food and materials","Build your own lines");
                for(int a=10;a<=14;a++)for(int b=-13;b<=-10;b++)if(a==10||a==14||b==-13||b==-10)p.level().setBlockAndUpdate(base.offset(a,0,b),Blocks.DARK_OAK_FENCE.defaultBlockState());
                sign(p,base.offset(15,0,-12),"COMBAT PRACTICE","Spawn eggs in chest","Use inside the pen","Rings / weapons / ward");
                for(int set=0;set<3;set++){
                    String name=new String[]{"dark_matter","red_matter","gem"}[set];var stand=new ArmorStand(p.level(),x-15+set*3+.5,y,z+.5);
                    stand.setYRot(0);stand.setNoGravity(true);
                    for(var pair:Map.of(EquipmentSlot.HEAD,"helmet",EquipmentSlot.CHEST,"chestplate",EquipmentSlot.LEGS,"leggings",EquipmentSlot.FEET,"boots").entrySet())stand.setItemSlot(pair.getKey(),new ItemStack(ExtendedAlchemyEquipment.ITEMS.get(name+"_"+pair.getValue())));
                    p.level().addFreshEntity(stand);
                }
                for(int a=0;a<4;a++)for(int b=0;b<4;b++){
                    p.level().setBlockAndUpdate(base.offset(11+a,-1,-1+b),Blocks.FARMLAND.defaultBlockState());
                    p.level().setBlockAndUpdate(base.offset(11+a,0,-1+b),Blocks.WHEAT.defaultBlockState());
                }
                sign(p,base.offset(12,0,4),"GROWTH GARDEN","Hold active band","Or use a pedestal","Ripe: use to replant");
                p.level().setBlockAndUpdate(base.offset(15,-1,2),Blocks.WATER.defaultBlockState());
                for(int a=-2;a<=2;a++)for(int b=0;b<5;b++)p.level().setBlockAndUpdate(base.offset(-15+a,b,5),a==0?Blocks.IRON_ORE.defaultBlockState():Blocks.STONE.defaultBlockState());
                sign(p,base.offset(-14,0,7),"MINING PRACTICE","Tools / rods / lenses","Claims still apply","Sneak for precision");
                p.level().setBlockAndUpdate(base.offset(-1,0,4),Blocks.CRAFTING_TABLE.defaultBlockState());p.level().setBlockAndUpdate(base.offset(1,0,4),Blocks.SMITHING_TABLE.defaultBlockState());
            });
            // Networked real machine menu actions and two scales.
            server.runCommand("tp @p %.1f %d %.1f 90 29".formatted(x+2.5,y,z-7.5));c.waitTicks(50);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyMachinesScreen.class);c.waitTicks(10);
            c.clickScreenButton("screen.jugcraft.alchemy_machine.pause");c.waitTicks(6);
            c.clickScreenButton("screen.jugcraft.alchemy_machine.deposit");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(0,0,-8));if(!be.paused()||be.essence()!=be.capacity())throw new AssertionError("Real furnace pause/deposit controls");});
            shot(c,"matter_furnace_interface");
            c.clickScreenButton("screen.jugcraft.alchemy_machine.withdraw");c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(((AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(0,0,-8))).essence()!=0)throw new AssertionError("Exact real withdrawal");});
            c.clickScreenButton("screen.jugcraft.alchemy_machine.deposit");c.waitTicks(5);c.clickScreenButton("screen.jugcraft.alchemy_machine.resume");c.waitTicks(5);close(c);
            c.runOnClient(mc->{var be=(AlchemyMachinesBlockEntity)mc.level.getBlockEntity(base.offset(8,0,-8));if(be==null||!be.displayArtifact().is(AlchemyArtifacts.ITEMS.get("life_stone")))throw new AssertionError("Pedestal initial chunk sync displays its artifact");});
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(8,0,-8));be.setItem(1,new ItemStack(AlchemyArtifacts.ITEMS.get("harvest_goddess_band")));});c.waitTicks(8);
            c.runOnClient(mc->{var be=(AlchemyMachinesBlockEntity)mc.level.getBlockEntity(base.offset(8,0,-8));if(!be.displayArtifact().is(AlchemyArtifacts.ITEMS.get("harvest_goddess_band")))throw new AssertionError("Pedestal replacement sends the new display item");});
            server.runCommand("tp @p %.1f %d %.1f 180 35".formatted(x+8.5,y,z-5.5));c.waitTicks(20);shot(c,"pedestal_retained_artifact");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();((AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(8,0,-8))).setItem(1,new ItemStack(AlchemyArtifacts.ITEMS.get("life_stone")));});
            // Portable bag: shift-click from actual player inventory, close, reopen and verify persistence.
            server.runCommand("tp @p %.1f %d %.1f 0 -40".formatted(x+.5,y,z+5.5));
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();hold(p,"purple_alchemical_bag");p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,11));p.inventoryMenu.broadcastChanges();});c.waitTicks(15);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyStorageScreen.class);c.waitTicks(6);
            c.runOnClient(mc->{
                if(AlchemyStorageScreen.PANEL_WIDTH>mc.getWindow().getGuiScaledWidth()||AlchemyStorageScreen.PANEL_HEIGHT>mc.getWindow().getGuiScaledHeight())throw new AssertionError("Whole storage panel fits the actual GUI scale");
                for(var slot:mc.player.containerMenu.slots)if(slot.x<0||slot.y<0||slot.x+16>AlchemyStorageScreen.PANEL_WIDTH||slot.y+19>AlchemyStorageScreen.PANEL_HEIGHT)throw new AssertionError("Storage slot and lock marker fit panel bounds");
            });
            c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,54,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(8);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(!(p.containerMenu instanceof AlchemyStorageMenu m)||m.contents().countItem(Items.IRON_INGOT)!=11)throw new AssertionError("Real bag input shift-click");});
            shot(c,"alchemical_bag_interface");close(c);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyStorageScreen.class);c.waitTicks(6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(((AlchemyStorageMenu)p.containerMenu).contents().countItem(Items.IRON_INGOT)!=11)throw new AssertionError("Real bag persisted after close");});close(c);
            server.runOnServer(s->hold(s.getPlayerList().getPlayers().getFirst(),"alchemy_tome"));c.waitTicks(15);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyStorageCatalogueScreen.class);c.waitTicks(8);shot(c,"alchemy_material_catalogue");
            c.clickScreenButton("screen.jugcraft.alchemy_storage.next");c.waitTicks(5);c.clickScreenButton("screen.jugcraft.alchemy_storage.previous");c.waitTicks(5);close(c);
            server.runOnServer(s->hold(s.getPlayerList().getPlayers().getFirst(),"alchemy_manual"));c.waitTicks(15);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyStorageManualScreen.class);c.waitTicks(6);
            c.clickScreenButton("screen.jugcraft.alchemy_storage.manual.title_3");shot(c,"alchemy_manual_storage");
            int scale=c.computeOnClient(mc->mc.options.guiScale().get());c.runOnClient(mc->{mc.options.guiScale().set(1);mc.resizeGui();});c.waitTicks(5);shot(c,"alchemy_manual_scale1");
            c.runOnClient(mc->{mc.options.guiScale().set(scale);mc.resizeGui();});close(c);
            server.runOnServer(s->hold(s.getPlayerList().getPlayers().getFirst(),"transmutation_tablet"));c.waitTicks(15);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitForScreen(AlchemyPatternScreen.class);c.waitTicks(8);shot(c,"portable_transmutation_tablet");close(c);
            server.runCommand("tp @p %.1f %d %.1f 180 9".formatted(x-11.5,y,z+4.5));c.waitTicks(40);shot(c,"alchemy_armor_sets");
            server.runCommand("gamemode spectator @p");server.runCommand("tp @p %.1f %d %.1f 180 30".formatted(x+.5,y+10,z+19.5));c.waitTicks(60);world.getConnection().waitForChunksRender();shot(c,"complete_alchemy_showroom");
            server.runCommand("tp @p %.1f %d %.1f 180 0".formatted(x+.5,y,z+5.5));server.runCommand("gamemode survival @p");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var be=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(4,0,2));if(be.countItem(Items.GOLD_INGOT)<1)throw new AssertionError("Connected real collector/relay/furnace line produces output");
                be.setItem(1,new ItemStack(Items.RAW_GOLD,64));be.setItem(4,ItemStack.EMPTY);be.setEssence(4000);
                for(int dx:new int[]{0,4}){var furnace=(AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(dx,0,-8));furnace.setItem(1,new ItemStack(Items.RAW_IRON,64));furnace.setItem(4,ItemStack.EMPTY);furnace.setEssence(8000);}
                p.getInventory().clearContent();
                Item[] hotbar={AlchemyStorage.ITEMS.get("transmutation_tablet"),AlchemyStorage.ITEMS.get("alchemy_manual"),AlchemyStorage.ITEMS.get("alchemy_tome"),AlchemyArtifacts.ITEMS.get("swiftwolf_rending_gale"),ExtendedAlchemyEquipment.ITEMS.get("red_matter_morning_star"),AlchemyArtifacts.ITEMS.get("harvest_goddess_band"),AlchemyStorage.ITEMS.get("purple_alchemical_bag"),KleinStars.ITEMS.get("klein_star_6"),Items.COOKED_BEEF};
                for(int i=0;i<hotbar.length;i++)p.getInventory().setItem(i,new ItemStack(hotbar[i],i==8?64:1));
                Assaying.setBalance(p,500000);p.getFoodData().setFoodLevel(20);p.setHealth(p.getMaxHealth());p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
            });
            ConnectedAlchemyClientChecks.run(c,server,base);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                Item[] hotbar={AlchemyStorage.ITEMS.get("transmutation_tablet"),AlchemicalGear.STONE,AlchemyArtifacts.ITEMS.get("mercurial_eye"),AlchemyStorage.ITEMS.get("gem_of_eternal_density"),AlchemyStorage.ITEMS.get("void_ring"),AlchemyStorage.ITEMS.get("alchemy_manual"),AlchemyStorage.ITEMS.get("purple_alchemical_bag"),KleinStars.ITEMS.get("klein_star_6"),Items.COOKED_BEEF};
                for(int i=0;i<hotbar.length;i++)p.getInventory().setItem(i,new ItemStack(hotbar[i],i==8?64:1));KleinStars.setStored(p.getInventory().getItem(7),500000);Assaying.setBalance(p,500000);p.inventoryMenu.broadcastChanges();
            });
            bindPack(world.getWorldSave().getSaveDirectory());
            server.runCommand("data remove block %d %d %d owner".formatted(x,y,z-8));server.runCommand("reload");
            server.waitFor(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.getUUID().equals(((AlchemyMachinesBlockEntity)p.level().getBlockEntity(base.offset(0,0,-8))).owner());},600);
            server.runCommand("tp @p %.1f %d %.1f 180 0".formatted(x+.5,y,z+5.5));c.waitTicks(10);
        }
    }
}
