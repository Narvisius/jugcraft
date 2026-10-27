package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.control.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;

public final class JimtaWireGameTests {
    @GameTest public void wireChannelsCannotCrossAnUnfittedColourAndRemovalDisconnects(GameTestHelper h){
        var controller=new BlockPos(1,2,2);var first=new BlockPos(2,2,2);var second=new BlockPos(3,2,2);var red=new BlockPos(4,2,2);var blue=new BlockPos(3,2,3);
        h.setBlock(controller,JugcraftControl.LOGIC_CONTROLLER);
        h.setBlock(first,IndustrialTransport.BLOCKS.get("jimta_pipe_stone_item"));h.setBlock(second,IndustrialTransport.BLOCKS.get("jimta_pipe_stone_item"));
        h.setBlock(red,JugcraftControl.RELAY.defaultBlockState().setValue(Channels.CHANNEL,DyeColor.RED));
        h.setBlock(blue,JugcraftControl.RELAY.defaultBlockState().setValue(Channels.CHANNEL,DyeColor.BLUE));
        var a=TransportAttachments.at(h.getLevel(),h.absolutePos(first));var b=TransportAttachments.at(h.getLevel(),h.absolutePos(second));
        int redBit=1<<DyeColor.RED.ordinal(),blueBit=1<<DyeColor.BLUE.ordinal();a.wires=redBit|blueBit;b.wires=redBit;
        var network=ControlNetwork.find(h.getLevel(),h.absolutePos(controller));
        h.assertTrue(network.switches().contains(h.absolutePos(red))&&!network.switches().contains(h.absolutePos(blue)),"A red-only segment cannot carry the blue channel");
        b.wires|=blueBit;network=ControlNetwork.find(h.getLevel(),h.absolutePos(controller));
        h.assertTrue(network.switches().contains(h.absolutePos(blue))&&network.cables()==2,"Adding the missing conductor completes the channel without changing cargo routing");
        var saved=h.getBlockEntity(second,TransportAttachments.Node.class).getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(saved.getIntOr("signal_wires",0)==(redBit|blueBit),"Both conductors persist and synchronize in the host fittings");
        a.wires=0;network=ControlNetwork.find(h.getLevel(),h.absolutePos(controller));
        h.assertTrue(network.switches().isEmpty(),"Removing the bridge conductor disconnects both endpoints immediately");h.succeed();
    }
}

