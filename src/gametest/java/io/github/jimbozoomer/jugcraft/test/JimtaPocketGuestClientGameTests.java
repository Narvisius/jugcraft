package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaPocketRooms;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Real dimension travel: the headless GameTestServer does not load custom dimensions. */
public final class JimtaPocketGuestClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{
                check(server.getLevel(JimtaPocketRooms.DIMENSION)!=null,"Original pocket dimension is loaded");
                var host=player(server);var guest=player(server);
                try{
                    var home=guest.position();var dimension=guest.level().dimension();
                    check(!JimtaPocketRooms.visit(guest,host),"No room or invitation means no entry");
                    check(JimtaPocketRooms.enter(host),"Host enters the original private room");loaded(host);
                    check(!JimtaPocketRooms.visit(guest,host),"An online host alone does not grant admission");
                    check(JimtaPocketRooms.invite(host,guest,false)==1,"Explicit invitation is recorded");
                    check(JimtaPocketRooms.visit(guest,host),"Invited guest opts into the room");loaded(guest);
                    check(guest.level().dimension().equals(JimtaPocketRooms.DIMENSION)&&JimtaPocketRooms.readOnlyGuest(guest),"Guest remains read-only");
                    check(!JimtaPocketRooms.readOnlyGuest(host),"Owner retains normal access");
                    check(!Authority.mayChange(guest.level(),guest,guest.blockPosition()),"Guest cannot edit arrival position");
                    check(JimtaPocketRooms.invite(host,guest,true)==1,"Invitation can be revoked");
                    JimtaPocketRooms.leave(guest);loaded(guest);
                    check(guest.level().dimension().equals(dimension)&&guest.position().distanceToSqr(home)<.01,"Guest returns to saved position");
                    check(!JimtaPocketRooms.visit(guest,host),"Revoked invitation cannot be reused");
                }finally{
                    if(guest.level().dimension().equals(JimtaPocketRooms.DIMENSION))JimtaPocketRooms.leave(guest);
                    if(host.level().dimension().equals(JimtaPocketRooms.DIMENSION))JimtaPocketRooms.leave(host);
                    server.getPlayerList().remove(guest);server.getPlayerList().remove(host);
                }
            });
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void loaded(ServerPlayer player){player.hasChangedDimension();player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());}
    private static ServerPlayer player(MinecraftServer server){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"pocket-guest-test");
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var player=new ServerPlayer(server,server.overworld(),profile,cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);loaded(player);
        var pos=new net.minecraft.core.BlockPos(2000,100,2000);server.overworld().getChunk(pos);
        server.overworld().setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        server.overworld().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());server.overworld().setBlockAndUpdate(pos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        player.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));return player;
    }
}
