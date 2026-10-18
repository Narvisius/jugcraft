package io.github.jimbozoomer.jugcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import java.util.Arrays;

/** Automated disposable workshop; a separate saved copy is supplied for the owner's independent playtest. */
public class DarkMatterClientGameTests implements FabricClientGameTest {
    private static Item item(String id) { return DarkMatter.ITEMS.get(id); }
    private static void equip(ServerPlayer p) {
        p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);
        Item[] kit={item("dark_matter_pickaxe"),item("dark_matter_hammer"),item("dark_matter_axe"),AlchemicalGear.STONE,
                item("alchemical_coal"),item("mobius_fuel"),item("aeternalis_fuel"),item("high_covalence_dust"),Items.COOKED_BEEF,
                item("low_covalence_dust"),item("medium_covalence_dust"),item("dark_matter")};
        for(int i=0;i<kit.length;i++)p.getInventory().setItem(i,new ItemStack(kit[i],i<4?1:16));
        Assaying.setBalance(p,100000);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
    }
    private static void lanes(ServerPlayer p, BlockPos base) {
        var l=p.level();
        for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=2;dy++)l.setBlockAndUpdate(base.offset(dx,dy,-5),Blocks.STONE.defaultBlockState());
        for(int dx=6;dx<=8;dx++)for(int dy=0;dy<=2;dy++)for(int dz=-7;dz<=-5;dz++)l.setBlockAndUpdate(base.offset(dx,dy,dz),Blocks.IRON_ORE.defaultBlockState());
        for(int dy=0;dy<8;dy++)l.setBlockAndUpdate(base.offset(-7,dy,-5),Blocks.OAK_LOG.defaultBlockState());
        for(int dx=-9;dx<=-5;dx++)for(int dz=-7;dz<=-3;dz++)l.setBlockAndUpdate(base.offset(dx,8,dz),Blocks.OAK_LEAVES.defaultBlockState());
        for(int dz=-3;dz>=-9;dz--)l.setBlockAndUpdate(base.offset(12,1,dz),Blocks.STONE.defaultBlockState());
    }
    private static void sign(ServerPlayer p, BlockPos at, String... words) {
        var level=p.level();level.setBlockAndUpdate(at,Blocks.DARK_OAK_SIGN.defaultBlockState());
        var sign=(SignBlockEntity)level.getBlockEntity(at);
        var lines=Arrays.stream(words).map(Component::literal).map(c->(Component)c).toList();
        for(var slot:SignTextSlot.values())sign.setText(new SignText(lines,lines,DyeColor.WHITE,false),slot);
        sign.setWaxed(true);sign.setChanged();level.sendBlockUpdated(at,sign.getBlockState(),sign.getBlockState(),3);
    }
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();
            BlockPos base=c.computeOnClient(mc->new BlockPos(mc.player.blockPosition().getX(),100,mc.player.blockPosition().getZ()));
            int x=base.getX(),y=base.getY(),z=base.getZ();
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-15,y,z-12,x+15,y+10,z+10));
            server.runCommand("fill %d %d %d %d %d %d minecraft:polished_deepslate".formatted(x-15,y-1,z-12,x+15,y-1,z+10));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("setworldspawn %d %d %d".formatted(x,y,z+5));server.runCommand("tp @p %d %d %d 180 5".formatted(x,y,z));
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();var l=p.level();
                ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);
                equip(p);lanes(p,base);
                var at=base.offset(-5,0,4);l.setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)l.getBlockEntity(at);
                Item[] supplies={Items.COAL,Items.CHARCOAL,Items.COBBLESTONE,Items.IRON_INGOT,Items.REDSTONE,Items.DIAMOND,Items.LAPIS_LAZULI,
                        Items.DIAMOND_BLOCK,Items.STICK,Items.RAW_IRON,Items.COPPER_INGOT,item("dark_matter"),item("high_covalence_dust"),item("aeternalis_fuel")};
                for(int i=0;i<supplies.length;i++)chest.setItem(i,new ItemStack(supplies[i],64));
                var damaged=new ItemStack(item("dark_matter_pickaxe"));damaged.setDamageValue(1500);chest.setItem(15,damaged);
                l.setBlockAndUpdate(base.offset(-3,0,4),Blocks.CRAFTING_TABLE.defaultBlockState());l.setBlockAndUpdate(base.offset(-1,0,4),Blocks.ANVIL.defaultBlockState());
                l.setBlockAndUpdate(base.offset(1,0,4),Blocks.FURNACE.defaultBlockState());l.setBlockAndUpdate(base.offset(3,0,4),Assaying.SCALE.defaultBlockState());
                p.giveExperienceLevels(40);
                sign(p,base.offset(3,0,1),"DARK MATTER","Use: next mode","Sneak: one block","100,000 essence");
                sign(p,base.offset(0,0,-3),"HAMMER","3 x 3 face","Mine the centre","16 / extra block");
                sign(p,base.offset(-7,0,-3),"AXE","Fell logs","Leaves stay","8 / extra log");
                sign(p,base.offset(7,0,-3),"PICKAXE","Ore vein","Same ore only","8 / extra block");
                sign(p,base.offset(13,0,-2),"PICKAXE","Short tunnel","2 extra blocks","Stops at barrier");
                sign(p,base.offset(-4,0,6),"WORKSHOP","Supplies + repair","Table / anvil","Furnace / scale");
            });
            c.waitTicks(40);world.getConnection().waitForChunksRender();c.runOnClient(mc->mc.gui.toastManager().clear());c.takeScreenshot("dark_matter_pickaxe");
            c.getInput().pressKey(options->options.keyInventory);c.waitForScreen(InventoryScreen.class);c.waitTicks(5);c.takeScreenshot("dark_matter_materials");
            c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitForScreen(null);
            // Real use input cycles the held pickaxe's mode; its client-visible component must follow.
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(8);
            c.runOnClient(mc->{if(mc.player.getMainHandItem().getOrDefault(DarkMatter.MODE,0)!=1)throw new AssertionError("Use must sync ore-vein mode to the client");});
            c.takeScreenshot("dark_matter_pickaxe_mode");
            c.getInput().pressKey(InputConstants.KEY_2);c.waitTicks(8);
            c.takeScreenshot("dark_matter_hammer");c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);c.waitTicks(8);
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                if(p.getMainHandItem().getOrDefault(DarkMatter.MODE,0)!=1)throw new AssertionError("Hammer mode must cycle");
                if(!p.gameMode.destroyBlock(base.offset(0,1,-5)))throw new AssertionError("Workshop centre must break normally");
                for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=2;dy++)if(!p.level().getBlockState(base.offset(dx,dy,-5)).isAir())throw new AssertionError("Hammer must clear one face");
                if(Assaying.balance(p)!=99872)throw new AssertionError("Eight extras must debit 128 essence");
            });c.waitTicks(8);c.takeScreenshot("dark_matter_hammer_face");
            c.getInput().pressKey(InputConstants.KEY_3);c.waitTicks(40);c.takeScreenshot("dark_matter_axe");
            // Reset this disposable workshop before preserving an independent copy for manual play.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();lanes(p,base);equip(p);});
            server.runCommand("kill @e[type=minecraft:item]");server.runCommand("tp @p %d %d %d 180 5".formatted(x,y,z+5));
            c.getInput().pressKey(InputConstants.KEY_1);c.waitTicks(80);c.runOnClient(mc->mc.gui.toastManager().clear());world.getConnection().waitForChunksRender();c.takeScreenshot("dark_matter_workshop");
        }
    }
}
