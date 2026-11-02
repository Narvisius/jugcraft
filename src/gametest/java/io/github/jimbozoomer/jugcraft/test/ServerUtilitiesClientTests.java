package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.server.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
public final class ServerUtilitiesClientTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c){try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
        world.getConnection().waitForChunksRender();var server=world.getServer();ServerPlayer[] peer={null};java.util.UUID[] owner={null};
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();owner[0]=p.getUUID();var at=p.blockPosition().above(3);for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){p.level().setBlock(at.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),3);for(int y=0;y<5;y++)p.level().removeBlock(at.offset(x,y,z),false);}p.teleportTo(p.level(),at.getX()+.5,at.getY(),at.getZ()+.5,java.util.Set.of(),0,0,true);
            var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TradePartner");var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);var b=new ServerPlayer(s,p.level(),profile,cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);s.getPlayerList().placeNewPlayer(connection,b,cookie);b.hasChangedDimension();b.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());b.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);peer[0]=b;
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GOLD_INGOT,8));b.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.EMERALD,2));});c.waitTicks(20);server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);if(NearbyTrade.offer(p,peer[0])!=1)throw new AssertionError("Trade did not open");});
        c.waitForScreen(ContainerScreen.class);c.waitTicks(5);c.takeScreenshot("nearby_trade_offers");
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,1,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(3);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);if(p.getMainHandItem().getCount()!=8)throw new AssertionError("Preview item could be taken");});
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,4,0,ContainerInput.PICKUP,mc.player));c.waitTicks(4);c.takeScreenshot("nearby_trade_confirmed");
        server.runOnServer(s->{if(NearbyTrade.accept(peer[0])!=1)throw new AssertionError("Peer acceptance failed");var p=s.getPlayerList().getPlayer(owner[0]);if(!p.getMainHandItem().is(Items.EMERALD)||p.getMainHandItem().getCount()!=2)throw new AssertionError("Wrong trade result");});c.waitForScreen(null);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withEntity(p),"invsee TradePartner");});c.waitForScreen(ContainerScreen.class);c.waitTicks(4);c.takeScreenshot("operator_inventory_inspection");
        c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(3);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);if(!peer[0].getMainHandItem().is(Items.GOLD_INGOT)||peer[0].getMainHandItem().getCount()!=8)throw new AssertionError("Inspection modified target");p.closeContainer();s.getPlayerList().remove(peer[0]);});c.waitForScreen(null);
        server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);var home=p.position();var commands=s.getCommands();p.snapTo(home.x-3,home.y,home.z-3,0,0);commands.performPrefixedCommand(s.createCommandSourceStack().withEntity(p),"protect pos1");p.snapTo(home.x+3,home.y+3,home.z+3,0,0);commands.performPrefixedCommand(s.createCommandSourceStack().withEntity(p),"protect pos2");commands.performPrefixedCommand(s.createCommandSourceStack().withEntity(p),"protect create review");p.teleportTo(p.level(),home.x,home.y,home.z-2,java.util.Set.of(),0,10,true);var tool=new ItemStack(Items.GOLDEN_HOE);net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,tool,t->t.putBoolean("jugcraft_protection_tool",true));p.setItemInHand(InteractionHand.MAIN_HAND,tool);});c.waitTicks(30);c.takeScreenshot("operator_region_boundaries");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayer(owner[0]);var at=p.position().add(0,0,5);
            var guard=io.github.jimbozoomer.jugcraft.town.CustomNpcs.create(p.level(),at.add(-2,0,0),180,"Market Guard", "guard",p.getUUID());
            var npc=io.github.jimbozoomer.jugcraft.town.CustomNpcs.create(p.level(),at.add(2,0,0),180,"Town Guide", "npc",p.getUUID());
            if(guard==null||npc==null)throw new AssertionError("Custom NPC placement failed");
            io.github.jimbozoomer.jugcraft.town.CustomNpcs.setWander(npc,0);
            io.github.jimbozoomer.jugcraft.town.CustomNpcs.setPlayerSkin(npc,"Preview",net.minecraft.world.item.component.ResolvableProfile.createResolved(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Preview")));
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),0,0,true);
        });c.waitTicks(50);c.takeScreenshot("custom_guard_and_player_skin_npc");

    }}
}
