package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Focused physical delivery, rollback, interruption and save-compatibility checks in a disposable world. */
public final class PeepoDeliveryClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private Container source,output;
    private int assertions;
    private void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private void reset(ServerLevel l,boolean jughead){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-8,-1,-7),base.offset(8,4,7)))l.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        l.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)l.getBlockEntity(base.west(6));
        l.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)l.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(l,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.snapTo(base.getX()-3.5,base.getY(),base.getZ()-2.5,0,0);npc.setOnGround(true);l.addFreshEntity(npc);
        var owner=l.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assignContainer(l,base.west(6),Direction.UP,false);npc.assignments.assignContainer(l,base.east(6),Direction.UP,true);
    }
    private void reload(ServerLevel l){
        npc.transport.stop();npc.setNoAi(true);
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());npc.saveWithoutId(out);
        var copy=(npc.isJughead()?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(l,EntitySpawnReason.COMMAND);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));npc.discard();npc=copy;l.addFreshEntity(copy);
    }
    private int reserved(){return reserved(npc);}
    private int reserved(PeepoEntity p){int n=0;for(int i=0;i<8;i++)if(p.transport.reserved(i))n++;return n;}
    private void picked(TestServerContext s,int slots){s.waitFor(v->reserved()>=slots,700);}
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
            foodBreak(server);
            for(boolean jughead:new boolean[]{false,true})porter(server,jughead);
            contention(server);
            garden(server);
            hearth(server);
            pot(server,false);pot(server,true);
            server.runOnServer(s->{npc.transport.stop();npc.discard();});
        }
        LoggerFactory.getLogger("peepo-delivery-test").info("[peepo-delivery-test] PASS {} assertions",assertions);
    }
    private void planning(ServerLevel l){
        reset(l,false);var from=new SimpleContainer(10);var to=new SimpleContainer(10);
        for(int i=0;i<9;i++)from.setItem(i,new ItemStack(Items.DIAMOND,64));
        var a=ContainerStorage.of(from,Direction.UP);var b=ContainerStorage.of(to,Direction.UP);
        var plan=CompanionDeliveryPlan.create(a,b,8,null,null);
        check(plan.size()==8 && plan.stream().mapToInt(ItemStack::getCount).sum()==512,"all eight full stacks fit the plan");
        check(count(from,Items.DIAMOND)==576 && to.isEmpty(),"planning never transfers or consumes real items");
        var limits=new CompanionSupplies.Route();limits.leave=512;limits.keep=48;
        plan=CompanionDeliveryPlan.create(a,b,8,null,limits);
        check(plan.stream().mapToInt(ItemStack::getCount).sum()==48,"batch respects combined source reserve and destination cap");
        from.clearContent();from.setItem(0,new ItemStack(Items.DIAMOND,20));from.setItem(1,new ItemStack(Items.DIAMOND,30));
        plan=CompanionDeliveryPlan.create(a,b,1,null,null);
        check(plan.size()==1 && plan.getFirst().getCount()==50,"split source stacks share one cargo slot");
        for(int i=0;i<to.getContainerSize();i++)to.setItem(i,new ItemStack(Items.STONE,64));
        check(CompanionDeliveryPlan.create(a,b,8,null,null).isEmpty() && count(from,Items.DIAMOND)==50,"full output probe rolls back source");
    }
    private void porter(TestServerContext s,boolean jughead){
        s.runOnServer(v->{reset(v.overworld(),jughead);
            source.setItem(0,new ItemStack(Items.DIAMOND,64));source.setItem(1,new ItemStack(Items.EMERALD,64));source.setItem(2,new ItemStack(Items.IRON_INGOT,64));
            source.getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal("Batch proof"));
            for(int i=2;i<8;i++)npc.belongings.setItem(i,new ItemStack(Items.COBBLESTONE,64));
            npc.belongings.setItem(9,new ItemStack(Items.TORCH));npc.assignments.supplies.route(0).enabled=true;npc.setNoAi(false);
        });
        picked(s,2);
        s.runOnServer(v->{npc.setNoAi(true);
            check(count(npc.belongings,Items.DIAMOND)==64 && count(npc.belongings,Items.EMERALD)==64 && count(source,Items.IRON_INGOT)==64,"fills both free slots without replacing personal cargo");
            check(output.isEmpty(),"pickup does not remotely insert at output");
            check(npc.transport.movementCost()==6,"128 items in two stacks cost 6 JE per moving tick");
            int before=npc.getEnergy();npc.snapTo(npc.getX()+.2,npc.getY(),npc.getZ(),0,0);npc.transport.tick();
            check(before-npc.getEnergy()==6,"moving a loaded batch actually deducts the scaled energy");
            check(npc.belongings.getItem(9).is(Items.TORCH),"equipment slot is not cargo");
            reload(v.overworld());check(reserved()==2 && npc.transport.movementCost()==6,"all manifest slots survive reload");
            check(npc.belongings.getItem(0).getHoverName().getString().equals("Batch proof"),"components survive batch reload");
            for(int i=0;i<output.getContainerSize();i++)output.setItem(i,new ItemStack(Items.STONE,64));npc.setNoAi(false);
        });
        s.waitFor(v->npc.transport.porterStatus()==CompanionStatus.FULL,800);
        s.runOnServer(v->{check(count(npc.belongings,Items.DIAMOND)==64 && count(npc.belongings,Items.EMERALD)==64,"blocked output retains entire batch");output.clearContent();});
        s.waitFor(v->count(output,Items.DIAMOND)==64 && count(output,Items.EMERALD)==64,1100);
        s.runOnServer(v->{check(count(source,Items.DIAMOND)==0 && count(source,Items.EMERALD)==0,"batch delivers each item exactly once");check(count(npc.belongings,Items.COBBLESTONE)==384,"personal cargo stays intact");});
    }
    private void contention(TestServerContext s){
        final PeepoEntity[] second={null};
        s.runOnServer(v->{var l=v.overworld();reset(l,false);
            for(int i=0;i<10;i++)source.setItem(i,new ItemStack(i<6?Items.DIAMOND:i<8?Items.EMERALD:Items.COAL,64));
            npc.assignments.supplies.route(0).enabled=true;
            second[0]=PeepoMod.JUGHEAD.create(l,EntitySpawnReason.COMMAND);var other=second[0];
            other.snapTo(base.getX()-3.5,base.getY(),base.getZ()+2.5,0,0);l.addFreshEntity(other);
            var owner=v.getPlayerList().getPlayers().getFirst();other.orders.tame(owner);other.orders.command(owner,3);other.preferences.social=false;other.preferences.carryMeals=0;
            other.assignments.assignContainer(l,base.west(6),Direction.UP,false);other.assignments.assignContainer(l,base.east(6),Direction.UP,true);other.assignments.supplies.route(0).enabled=true;npc.setNoAi(false);
        });
        s.waitFor(v->reserved()==8 || reserved(second[0])==8,700);
        s.runOnServer(v->{var full=reserved()==8?npc:second[0];check(full.transport.movementCost()==24,"eight full stacks cost 24 JE per moving tick");
            if(full==npc){reload(v.overworld());check(reserved()==8,"all eight cargo reservations survive reload");npc.setNoAi(false);}
        });
        s.waitFor(v->count(output,Items.DIAMOND)==384 && count(output,Items.EMERALD)==128 && count(output,Items.COAL)==128,1400);
        s.runOnServer(v->{check(source.isEmpty() && count(npc.belongings,Items.DIAMOND)+count(second[0].belongings,Items.DIAMOND)==0,"two competing batch carriers conserve all 640 items");second[0].transport.stop();second[0].discard();});
    }
    private void hearth(TestServerContext s){
        var filling=PieFilling.values()[0];var raw=JugcraftAgriculture.item(filling.rawPie());var baked=JugcraftAgriculture.item(filling.pie());
        s.runOnServer(v->{reset(v.overworld(),false);v.overworld().setBlockAndUpdate(base,JugcraftAgriculture.block("hearth_oven").defaultBlockState());
            var oven=(HearthOvenBlockEntity)v.overworld().getBlockEntity(base);oven.selectPie(filling);oven.set(0,0,null,0);
            npc.assignments.assign(v.overworld(),base);source.setItem(0,new ItemStack(Items.COAL,64));source.setItem(1,new ItemStack(raw,2));npc.setNoAi(false);
        });
        picked(s,2);
        s.runOnServer(v->{var oven=(HearthOvenBlockEntity)v.overworld().getBlockEntity(base);
            check(count(npc.belongings,raw)==1 && count(npc.belongings,Items.COAL)>0,"one trip carries raw pie and coal even when coal comes first in chest");
            check(oven.pie()==null && oven.burn()==0,"oven is untouched by pickup planning");
            check(count(source,raw)==1,"does not overfetch pies beyond single oven capacity");
            reload(v.overworld());check(reserved()==2,"pie and fuel reload together");npc.setNoAi(false);
        });
        s.waitFor(v->count(output,baked)>=1,1800);
        s.runOnServer(v->check(count(output,baked)==1,"cold oven bakes and delivers mixed batch"));
        s.waitFor(v->{
            if(count(source,raw)==0)return true;
            if(npc.transport.activity()==CompanionStatus.IDLE)throw new AssertionError("oven cycle became idle before fetching the next available pie");
            return false;
        },600);
        s.waitFor(v->count(output,baked)==2,1600);
        s.runOnServer(v->check(count(source,raw)==0,"second pie follows the first without a wandering handoff"));
    }
    private void foodBreak(TestServerContext s){
        s.runOnServer(v->{reset(v.overworld(),false);
            for(int i=0;i<9;i++)source.setItem(i,new ItemStack(Items.DIAMOND,64));
            npc.setHealth(npc.getMaxHealth()/2);npc.assignments.supplies.route(0).enabled=true;npc.setNoAi(false);
        });
        picked(s,8);
        s.runOnServer(v->v.overworld().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(v.overworld(),base.getX()+4.5,base.getY()+.1,base.getZ()+.5,new ItemStack(Items.COOKED_BEEF))));
        try{s.waitFor(v->npc.isEating(),500);}catch(AssertionError e){throw new AssertionError(s.computeOnServer(v->"meal: position="+npc.position()+", status="+npc.transport.activity()+", hungry="+npc.needsAutomaticFood()+", output="+count(output,Items.DIAMOND)+", source="+count(source,Items.DIAMOND)),e);}
        s.runOnServer(v->{check(count(output,Items.DIAMOND)==512,"needed meal waits for the current cargo to unload");
            check(count(source,Items.DIAMOND)==64,"needed meal interrupts the chain before the next porter pickup");
        });
    }
    private void garden(TestServerContext s){
        s.runOnServer(v->{var l=v.overworld();reset(l,false);
            l.setBlockAndUpdate(base.north(),Blocks.WATER.defaultBlockState());
            l.setBlockAndUpdate(base,Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE,7));npc.assignments.assign(l,base);
            // A saved harvest state: real inventory owns the items; garden marks only track their origin.
            npc.belongings.setItem(0,new ItemStack(Items.WHEAT,64));npc.belongings.setItem(1,new ItemStack(Items.CARROT,64));
            var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());
            for(int i=0;i<2;i++){saved.store("GardenHarvest"+i,ItemStack.CODEC,npc.belongings.getItem(i));saved.store("GardenOrigin"+i,GlobalPos.CODEC,GlobalPos.of(l.dimension(),base));}
            npc.garden.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),saved.buildResult()));npc.setNoAi(false);
        });
        picked(s,2);
        s.runOnServer(v->check(npc.transport.movementCost()==6,"garden groups full harvest stacks with seed reserve deducted"));
        s.waitFor(v->count(output,Items.WHEAT)==64 && count(output,Items.CARROT)==63,1000);
        s.runOnServer(v->{check(count(npc.belongings,Items.CARROT)==1,"garden retains one seed for its one-cell plot");check(count(npc.belongings,Items.WHEAT)==0,"full wheat stack offloaded in the same trip");});
    }
    private void pot(TestServerContext s,boolean disable){
        final CookingPotBlockEntity[] pot={null};final List<ItemStack> ingredients=new ArrayList<>();
        s.runOnServer(v->{reset(v.overworld(),true);v.overworld().setBlockAndUpdate(base,JugcraftAgriculture.block("cooking_pot").defaultBlockState());pot[0]=(CookingPotBlockEntity)v.overworld().getBlockEntity(base);
            var recipe=CookingPotRecipe.catalog(v).entrySet().stream().filter(e->e.getValue().parts().size()>=3 && e.getValue().parts().size()<=6).findFirst().orElseThrow();
            pot[0].selectRecipe(recipe.getKey());npc.assignments.assign(v.overworld(),base);
            for(var part:recipe.getValue().parts()){var stack=new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count());source.setItem(ingredients.size(),stack.copy());ingredients.add(stack);}
            npc.setNoAi(false);
        });
        picked(s,2);
        s.runOnServer(v->{npc.setNoAi(true);
            check(source.isEmpty(),"all ingredients for one recipe collected together");
            check(npc.food.meals()==0,"every ingredient cargo slot is protected from eating");
            for(var stack:ingredients)check(count(pot[0],stack.getItem())==0,"pot untouched until physical delivery");
            if(disable){npc.assignments.cycleTransport(1,true);npc.assignments.cycleTransport(1,true);}
            else {
                // A hopper/player supplies one ingredient while the batch is in flight.
                pot[0].setItem(0,ingredients.getFirst().copy());
            }
            reload(v.overworld());npc.setNoAi(false);
        });
        if(disable){
            s.waitFor(v->!npc.transport.hasPendingDelivery(),1200);
            s.runOnServer(v->{for(var stack:ingredients)check(count(source,stack.getItem())>=stack.getCount() && count(pot[0],stack.getItem())==0,"Supply Off returns every ingredient");});
        }else {
            s.waitFor(v->!npc.transport.hasPendingDelivery(),1200);
            s.runOnServer(v->{for(var stack:ingredients)check(count(pot[0],stack.getItem())>=stack.getCount(),"still-needed ingredients delivered after one becomes unnecessary");
                check(count(source,ingredients.getFirst().getItem())>=ingredients.getFirst().getCount(),"unneeded ingredient returned to original chest");
            });
        }
    }
}
