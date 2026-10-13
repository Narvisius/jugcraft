package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Focused disposable-world coverage: crate storage, ownership, upgrade, release and actual client rendering. */
public final class TransportCrateClientTests implements FabricClientGameTest {
    private int assertions;
    private BlockPos base;
    private ItemStack crate;
    private UUID companion;
    private void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    private ServerPlayer player(ServerLevel level){return level.getServer().getPlayerList().getPlayers().getFirst();}
    private Mob spawn(ServerLevel level,String type){
        var mob=(Mob)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type)).create(level,EntitySpawnReason.COMMAND);
        mob.setNoAi(true);mob.snapTo(base.getX()+.5,base.getY(),base.getZ()+1.5,0,0);level.addFreshEntity(mob);return mob;
    }
    @Override public void runTest(ClientGameTestContext context){
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});var server=world.getServer();
            base=context.computeOnClient(c->c.player.blockPosition().above(4));server.runCommand("time set noon");server.runCommand("weather clear");
            server.runOnServer(s->{var l=s.overworld();for(var pos:BlockPos.betweenClosed(base.offset(-9,-1,-9),base.offset(9,-1,9)))l.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());player(l).snapTo(base.getX()+.5,base.getY(),base.getZ()-.5,0,0);});
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f 0 12",base.getX()+.5,base.getY(),base.getZ()-.5));
            server.runOnServer(s->captureTests(s.overworld()));
            context.waitTicks(4);
            server.runOnServer(s->releaseTests(s.overworld()));
            server.runOnServer(s->fill(s.overworld(),false));
            context.waitTicks(8);
            context.runOnClient(c->{for(var entry:MobTransportCrate.occupants(c.player.getMainHandItem()))check(local.peepo.client.TransportCratePreview.mob(entry)!=null,"every stored mob has a client preview: "+entry.getStringOr("Type",""));});
            context.takeScreenshot("wood_transport_crate_held");
            holdingViews(context,server,"wood");
            server.runOnServer(s->MobTransportCrate.open(player(s.overworld()),InteractionHand.MAIN_HAND));context.waitForScreen(local.peepo.client.TransportCrateScreen.class);context.waitTicks(8);context.takeScreenshot("wood_transport_crate_menu");
            context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,2));
            server.waitFor(s->MobTransportCrate.count(crate)==3,100);check(true,"client button releases selected mob beside player");
            context.runOnClient(c->c.player.closeContainer());context.waitForScreen(null);
            server.runOnServer(s->fill(s.overworld(),true));context.waitTicks(8);context.takeScreenshot("iron_transport_crate_held");
            holdingViews(context,server,"iron");
            server.runOnServer(s->MobTransportCrate.open(player(s.overworld()),InteractionHand.MAIN_HAND));context.waitForScreen(local.peepo.client.TransportCrateScreen.class);context.waitTicks(8);context.takeScreenshot("iron_transport_crate_menu");
            server.runOnServer(s->{var p=player(s.overworld());var menu=p.containerMenu;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MobTransportCrate.IRON));check(!menu.stillValid(p) && !menu.clickMenuButton(p,0),"replacing held stack invalidates old menu");});
            context.waitForScreen(null);
            server.runOnServer(s->{var p=player(s.overworld());p.setItemInHand(InteractionHand.MAIN_HAND,crate);p.inventoryMenu.broadcastChanges();});
            save=world.getWorldSave();
        }
        try(var world=save.open()){
            world.getConnection().waitForChunksRender();world.getServer().runOnServer(s->{var p=player(s.overworld());var stored=p.getMainHandItem();check(MobTransportCrate.count(stored)==8,"filled iron crate survives actual world restart");
                check(MobTransportCrate.release(p,stored,1,null) && MobTransportCrate.count(stored)==7,"saved ledger and item still release after world restart");});
        }
        LoggerFactory.getLogger("transport-crate-test").info("[transport-crate-test] PASS {} assertions",assertions);
    }
    private void captureTests(ServerLevel level){
        var p=player(level);crate=new ItemStack(MobTransportCrate.WOOD);p.setItemInHand(InteractionHand.MAIN_HAND,crate);
        check(!(MobTransportCrate.WOOD instanceof BlockItem) && !(MobTransportCrate.IRON instanceof BlockItem),"both variants item only");
        var npc=(PeepoEntity)spawn(level,"peepo_companion:jughead");npc.orders.tame(p);npc.setCustomName(Component.literal("Travel Buddy"));npc.setHealth(7);npc.setStoredEnergy(3210);npc.belongings.setItem(0,new ItemStack(Items.DIAMOND,13));npc.belongings.setItem(9,new ItemStack(Items.TORCH));
        level.setBlockAndUpdate(base.east(4),Blocks.BARREL.defaultBlockState());npc.assignments.assignContainer(level,base.east(4),Direction.UP,false);npc.assignments.supplies.toggleTools(0);companion=npc.getUUID();
        check(MobTransportCrate.capture(p,crate,npc) && npc.isRemoved(),"capture companion removes live entity");
        var entry=MobTransportCrate.occupants(crate).getFirst();check(!entry.getCompoundOrEmpty("Preview").contains("Belongings"),"client previews contain no inventory");
        var ledger=TransportLedger.get(level.getServer());var encoded=TransportLedger.CODEC.encodeStart(NbtOps.INSTANCE,ledger).getOrThrow();var decoded=TransportLedger.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();check(decoded.get(entry.getStringOr("Claim",""))!=null,"authoritative snapshot survives ledger codec round trip");
        var encodedItem=ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),crate).getOrThrow();var restored=ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),encodedItem).getOrThrow();check(MobTransportCrate.count(restored)==1,"item claims survive serialization");
        var hostile=spawn(level,"minecraft:zombie");check(!MobTransportCrate.capture(p,crate,hostile) && !hostile.isRemoved(),"hostile refused");hostile.discard();
        var wolf=(net.minecraft.world.entity.TamableAnimal)spawn(level,"minecraft:wolf");wolf.setOwnerReference(EntityReference.of(UUID.randomUUID()));wolf.setTame(true,true);check(!MobTransportCrate.capture(p,crate,wolf),"other player's tame animal protected");wolf.discard();
        for(String type:List.of("minecraft:cow","minecraft:sheep","minecraft:villager"))check(MobTransportCrate.capture(p,crate,spawn(level,type)),"capture friendly "+type);
        var extra=spawn(level,"minecraft:pig");check(!MobTransportCrate.capture(p,crate,extra) && !extra.isRemoved() && MobTransportCrate.count(crate)==4,"wood full preserves fifth mob");extra.discard();
        var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(i==4?crate:new ItemStack(Items.IRON_INGOT));
        var input=CraftingInput.of(3,3,grid);check(TransportCrateUpgrade.INSTANCE.matches(input,level),"upgrade shape accepted");crate=TransportCrateUpgrade.INSTANCE.assemble(input);p.setItemInHand(InteractionHand.MAIN_HAND,crate);
        check(crate.is(MobTransportCrate.IRON) && MobTransportCrate.count(crate)==4 && MobTransportCrate.capacity(crate)==8,"filled upgrade keeps all claims");grid.set(0,new ItemStack(Items.DIRT));check(!TransportCrateUpgrade.INSTANCE.matches(CraftingInput.of(3,3,grid),level),"invalid upgrade rejected");
    }
    private void holdingViews(ClientGameTestContext context,TestServerContext server,String kind){
        context.runOnClient(c->{c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);c.options.fov().set(45);});
        context.waitTicks(8);context.takeScreenshot(kind+"_crate_right_hand");
        server.runOnServer(s->{var p=player(s.overworld());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,crate);p.inventoryMenu.broadcastChanges();});
        context.waitTicks(8);context.takeScreenshot(kind+"_crate_left_hand");
        context.runOnClient(c->{c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);c.options.fov().set(70);});
        context.waitTicks(8);context.takeScreenshot(kind+"_crate_offhand_first_person");
        server.runOnServer(s->{var p=player(s.overworld());p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,crate);p.inventoryMenu.broadcastChanges();});
        context.waitTicks(4);
    }
    private void releaseTests(ServerLevel level){
        var p=player(level);var copied=crate.copy();var at=Vec3.atBottomCenterOf(base.east(3));
        level.setBlockAndUpdate(base.east(3),Blocks.STONE.defaultBlockState());check(!MobTransportCrate.release(p,crate,0,at) && MobTransportCrate.count(crate)==4,"blocked release retains mob and claim");level.setBlockAndUpdate(base.east(3),Blocks.AIR.defaultBlockState());
        check(!MobTransportCrate.release(p,crate,0,at.add(100,0,0)),"far release rejected");
        check(MobTransportCrate.release(p,crate,0,at),"selected companion released on ground");var npc=(PeepoEntity)level.getEntity(companion);
        check(npc!=null && npc.orders.owner(p) && npc.getName().getString().equals("Travel Buddy") && npc.getHealth()==7 && npc.getEnergy()==3210,"identity owner name health and energy preserved");
        check(npc.belongings.getItem(0).getCount()==13 && npc.belongings.getItem(9).is(Items.TORCH) && npc.assignments.get(6)!=null && npc.assignments.supplies.tools(0),"cargo hand and assignments preserved");
        check(!MobTransportCrate.release(p,copied,0,Vec3.atBottomCenterOf(base.west(3))) && MobTransportCrate.count(copied)==3,"copied claim cannot duplicate; stale slot cleared");
        check(!MobTransportCrate.release(p,crate,8,at) && !MobTransportCrate.release(p,crate,-1,at),"invalid slot rejected");npc.discard();
    }
    private void fill(ServerLevel level,boolean iron){
        var p=player(level);for(var mob:level.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(10)))mob.discard();
        crate=new ItemStack(iron?MobTransportCrate.IRON:MobTransportCrate.WOOD);p.setItemInHand(InteractionHand.MAIN_HAND,crate);
        var types=List.of("peepo_companion:peepo","peepo_companion:jughead","minecraft:pig","minecraft:chicken","minecraft:cow","minecraft:sheep","minecraft:villager","minecraft:rabbit");
        for(int i=0;i<(iron?8:4);i++){var mob=spawn(level,types.get(i));if(i==0)mob.setCustomName(Component.literal("Peepo Pal"));check(MobTransportCrate.capture(p,crate,mob),"fill display slot "+i);}
        if(iron){var extra=spawn(level,"minecraft:pig");check(!MobTransportCrate.capture(p,crate,extra) && MobTransportCrate.count(crate)==8,"iron capacity eight");extra.discard();}
        p.inventoryMenu.broadcastChanges();
    }
}
