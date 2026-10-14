package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.LoggerFactory;

/** Cooking-pot-only regression: balanced pickup, physical delivery and cook-before-unload. */
public final class PeepoCookingBatchClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private CookingPotBlockEntity pot;
    private Container source,output;
    private int assertions;
    private final Item tomato=JugcraftAgriculture.item("tomato"),onion=JugcraftAgriculture.item("onion"),soup=JugcraftAgriculture.item("tomato_soup");
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private int count(List<ItemStack> stacks,Item item){return stacks.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private void reset(ServerLevel level,boolean jughead){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))level.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        level.setBlockAndUpdate(base,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
        pot=(CookingPotBlockEntity)level.getBlockEntity(base);pot.selectRecipe(Jugcraft.id("pot_cooking/tomato_soup"));
        level.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(6));
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.snapTo(base.getX()-3.5,base.getY(),base.getZ()-2.5,0,0);npc.setOnGround(true);level.addFreshEntity(npc);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assign(level,base);
        npc.assignments.assignContainer(level,base.west(6),Direction.UP,false);npc.assignments.assignContainer(level,base.east(6),Direction.UP,true);
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+.5,base.getY(),base.getZ()-5.5));
            server.runOnServer(s->planning(s.overworld()));
            for(boolean jughead:new boolean[]{false,true})cooking(server,jughead);
            server.runOnServer(s->{npc.transport.stop();npc.resetCompanionRoutine();npc.discard();});
        }
        LoggerFactory.getLogger("peepo-cooking-batch-test").info("[peepo-cooking-batch-test] PASS {} assertions",assertions);
    }
    private void planning(ServerLevel level){
        reset(level,false);
        var supplies=new SimpleContainer(8);
        supplies.setItem(0,new ItemStack(Items.BOWL,64));supplies.setItem(1,new ItemStack(tomato,64));
        supplies.setItem(2,new ItemStack(tomato,64));supplies.setItem(3,new ItemStack(onion,64));
        var storage=ContainerStorage.of(supplies,Direction.UP);
        var plan=CookingPotSupplies.create(pot,storage,storage,8);
        check(count(plan,Items.BOWL)==64 && count(plan,tomato)==128 && count(plan,onion)==64,"loads 64 balanced recipes, including a two-stack ingredient");
        check(pot.isEmpty() && count(supplies,tomato)==128,"bulk planning rolls back all pot/source changes");
        check(CookingPotSupplies.create(pot,storage,storage,1).size()==1,"one free cargo slot still supplies part of a bulk recipe");
        check(CookingPotSupplies.create(pot,storage,storage,0).isEmpty(),"no free cargo slots means no pickup");
        supplies.setItem(3,new ItemStack(onion,3));plan=CookingPotSupplies.create(pot,storage,storage,8);
        check(count(plan,Items.BOWL)==3 && count(plan,tomato)==6 && count(plan,onion)==3,"scarce ingredient limits the balanced batch instead of hoarding others");
        supplies.clearContent();supplies.setItem(0,new ItemStack(Items.BOWL,8));
        var other=new SimpleContainer(new ItemStack(tomato,16),new ItemStack(onion,8));
        var linked=new CombinedStorage<ItemVariant,net.fabricmc.fabric.api.transfer.v1.storage.Storage<ItemVariant>>(List.of(storage,ContainerStorage.of(other,Direction.UP)));
        plan=CookingPotSupplies.create(pot,storage,linked,8);
        check(plan.size()==1 && count(plan,Items.BOWL)==8,"split Supply chests plan bulk quantities but only pick up the visited chest");
        pot.setItem(0,new ItemStack(Items.BOWL,8));
        check(CookingPotSupplies.missing(pot,new ItemStack(Items.BOWL))==0,"already stocked ingredient cannot hide the next Supply chest");
        plan=CookingPotSupplies.create(pot,ContainerStorage.of(other,Direction.UP),linked,8);
        check(count(plan,tomato)==16 && count(plan,onion)==8 && count(plan,Items.BOWL)==0,"next chest supplies the remaining bulk ingredients");
        for(int i=1;i<6;i++)pot.setItem(i,new ItemStack(Items.BOWL,64));
        check(CookingPotSupplies.create(pot,ContainerStorage.of(other,Direction.UP),linked,8).isEmpty(),"full pot refuses a plan without losing source ingredients");
        check(count(other,tomato)==16 && count(other,onion)==8,"failed bulk capacity simulation preserves ingredients");
    }
    private void cooking(TestServerContext server,boolean jughead){
        server.runOnServer(s->{reset(s.overworld(),jughead);
            s.overworld().setBlockAndUpdate(base.below(),Blocks.MAGMA_BLOCK.defaultBlockState());
            source.setItem(0,new ItemStack(Items.BOWL,8));source.setItem(1,new ItemStack(tomato,16));source.setItem(2,new ItemStack(onion,8));npc.setNoAi(false);
        });
        server.waitFor(s->count(npc.belongings,Items.BOWL)==8 && count(npc.belongings,tomato)==16 && count(npc.belongings,onion)==8,900);
        server.runOnServer(s->{check(source.isEmpty(),"one trip collects all eight recipes");check(pot.isEmpty(),"ingredients reach real cargo before the pot");});
        server.waitFor(s->count(pot,soup)>0,900);
        final boolean[] observedCooking={false};
        server.waitFor(s->{
            if(count(npc.belongings,soup)>0)return true;
            if(count(pot,soup)>0 && count(pot,soup)<4){observedCooking[0]=true;check(output.isEmpty(),"partial cooked batch remains at pot while more cooking is possible");}
            return false;
        },1100);
        server.runOnServer(s->{check(observedCooking[0],"observed cooking between the first and fourth meal");
            check(count(npc.belongings,soup)==4,"collects all four meals only when result slots fill");
            check(output.isEmpty(),"finished meals are physically carried before Output changes");
        });
        server.waitFor(s->count(output,soup)==8,1700);
        server.runOnServer(s->{check(count(pot,Items.BOWL)==0 && count(pot,tomato)==0 && count(pot,onion)==0,"all eight recipes cook and deliver exactly once");
            check(source.isEmpty() && count(npc.belongings,soup)==0,"batch finishes without lost or stranded supplies/results");
        });
    }
}
