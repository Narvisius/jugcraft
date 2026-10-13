package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import local.peepo.client.PeepoState;

/** Focused visual exercise: planted model, two companion variants, and an ordinary torch for comparison. */
public final class TikiTorchClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        int[] ids=new int[3];
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});
            var server=world.getServer();var base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runOnServer(s->{
                var level=s.overworld();var player=s.getPlayerList().getPlayers().getFirst();
                for(var pos:BlockPos.betweenClosed(base.offset(-6,-1,-6),base.offset(6,-1,6)))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                var tiki=BuiltInRegistries.ITEM.getValue(PeepoMod.id("tiki_torch"));var block=((BlockItem)tiki).getBlock();
                for(int i=0;i<3;i++){
                    var npc=(i==1?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
                    npc.setNoAi(true);npc.snapTo(base.getX()-1.4+i*1.35,base.getY(),base.getZ()+.5,180,0);npc.yBodyRot=npc.yHeadRot=180;
                    npc.preferences.social=false;npc.preferences.carryMeals=0;npc.belongings.setItem(9,new ItemStack(i==2?Items.TORCH:tiki));
                    npc.setCustomName(Component.literal(i==0?"Peepo - Tiki":i==1?"Jughead - Tiki":"Regular torch"));npc.setCustomNameVisible(true);
                    level.addFreshEntity(npc);ids[i]=npc.getId();
                }
                var planted=base.offset(3,0,2);level.setBlockAndUpdate(planted,block.defaultBlockState().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.LOWER));level.setBlockAndUpdate(planted.above(),block.defaultBlockState().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER));
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            });
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f 0 8",base.getX()+.6,(double)base.getY(),base.getZ()-4.5));
            context.waitFor(c->{for(int id:ids)if(!(c.level.getEntity(id) instanceof PeepoEntity p) || p.getMainHandItem().isEmpty())return false;return true;},150);
            context.waitTicks(15);
            context.runOnClient(c->{for(int i=0;i<3;i++){
                var state=(PeepoState)c.getEntityRenderDispatcher().extractEntity(c.level.getEntity(ids[i]),0);
                if(!state.holdingLight || state.held.isEmpty() || state.heldLightScale!=(i==2?.35F:.5F))throw new AssertionError("Incorrect held-light render state for companion "+i);
            }});
            context.takeScreenshot("tiki_companions_and_planted_day");
            server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.getValue(PeepoMod.id("tiki_torch"))));player.inventoryMenu.broadcastChanges();});
            context.waitTicks(10);context.takeScreenshot("tiki_revised_item");
            server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.inventoryMenu.broadcastChanges();});
            server.runCommand("time set midnight");context.waitTicks(15);context.takeScreenshot("tiki_companions_and_planted_night");
        }
        org.slf4j.LoggerFactory.getLogger("tiki-torch-test").info("[tiki-torch-test] PASS: both companion variants enlarged, ordinary torch unchanged; day/night and item screenshots captured");
    }
}
