package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import org.slf4j.LoggerFactory;

/** Only hearth automation plus its companion spawn/render prerequisite. Never opens player worlds. */
public final class PeepoHearthClientTests implements FabricClientGameTest {
    private final List<String> failures=new ArrayList<>();
    private int groups,assertions;
    private BlockPos base;
    private PeepoEntity helper;
    private HearthOvenBlockEntity oven;
    private Container source,output;
    private Item raw,baked;
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private void group(String name,Runnable action){
        try{action.run();groups++;LoggerFactory.getLogger("hearth-test").info("[hearth-test] PASS {}",name);}
        catch(AssertionError|RuntimeException e){failures.add(name+": "+e);LoggerFactory.getLogger("hearth-test").error("[hearth-test] FAIL "+name,e);}
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");
            group("Peepo and Jughead client construction and spawn packets",()->spawn(context,server));
            new CompanionAutomationChecks(this::check,this::group,base).runHearthOnly(context,server);
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f 0 20",base.getX()+.5,base.getY()+1,base.getZ()-5.5));
            int i=0;for(var facing:Direction.Plane.HORIZONTAL){final int variant=i++;
                group("hearth animation and delivery "+facing,()->cycle(context,server,facing,variant));
            }
            group("interrupted loading preserves raw cargo and resumes",()->interrupted(server));
            group("full output and carrying reload preserve baked pie",()->blockedOutput(server));
            group("all fillings, single-pie capacity and burnt output",()->server.runOnServer(s->capacity(s.overworld())));
            server.runOnServer(s->reset(s.overworld()));
        }
        LoggerFactory.getLogger("hearth-test").info("[hearth-test] {} groups passed, {} assertions, {} failed groups",groups,assertions,failures.size());
        if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
    }
    private void reset(ServerLevel l){
        if(helper!=null){helper.transport.stop();helper.resetCompanionRoutine();helper.discard();helper=null;}
        for(var p:BlockPos.betweenClosed(base.offset(-9,-1,-9),base.offset(9,5,9)))
            l.setBlock(p,p.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private void ticks(TestServerContext s,int n){long end=s.computeOnServer(v->v.overworld().getGameTime()+n);s.waitFor(v->v.overworld().getGameTime()>=end,n+80);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private void spawn(ClientGameTestContext context,TestServerContext server){
        context.runOnClient(c->{
            // Reproduces the construction path before ClientboundAddEntityPacket supplies its ID.
            check(PeepoMod.PEEPO.create(c.level,EntitySpawnReason.COMMAND)!=null,"client Peepo construction");
            check(PeepoMod.JUGHEAD.create(c.level,EntitySpawnReason.COMMAND)!=null,"client Jughead construction");
            check(PeepoMod.LEGACY_JUGHEAD.create(c.level,EntitySpawnReason.COMMAND)!=null,"legacy client construction");
        });
        int[] ids=server.computeOnServer(s->{reset(s.overworld());var result=new int[2];
            for(int i=0;i<2;i++){var p=(i==0?PeepoMod.PEEPO:PeepoMod.JUGHEAD).create(s.overworld(),EntitySpawnReason.COMMAND);
                p.setNoAi(true);p.snapTo(base.getX()+i+.5,base.getY(),base.getZ()+.5,0,0);s.overworld().addFreshEntity(p);result[i]=p.getId();}
            return result;
        });
        context.waitFor(c->c.level.getEntity(ids[0]) instanceof PeepoEntity && c.level.getEntity(ids[1]) instanceof PeepoEntity,150);
        context.runOnClient(c->check(c.player!=null && c.level.getEntity(ids[1]).isAlive(),"spawn packets kept the client connected"));
        server.runOnServer(s->{for(int id:ids)s.overworld().getEntity(id).discard();});
    }
    private void setup(ServerLevel l,Direction facing,int variant){
        reset(l);l.setBlockAndUpdate(base,JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING,facing));
        oven=(HearthOvenBlockEntity)l.getBlockEntity(base);var filling=PieFilling.values()[variant%5];oven.selectPie(filling);oven.set(1600,100,null,0);
        raw=JugcraftAgriculture.item(filling.rawPie());baked=JugcraftAgriculture.item(filling.pie());
        l.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)l.getBlockEntity(base.west(6));source.setItem(0,new ItemStack(raw));
        l.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)l.getBlockEntity(base.east(6));
        helper=(variant%2==0?PeepoMod.PEEPO:PeepoMod.JUGHEAD).create(l,EntitySpawnReason.COMMAND);
        helper.snapTo(base.getX()-4.5,base.getY(),base.getZ()-2.5,0,0);l.addFreshEntity(helper);
        helper.orders.tame(l.getServer().getPlayerList().getPlayers().getFirst());helper.preferences.carryMeals=0;helper.preferences.social=false;
        if(variant>=2)helper.belongings.setItem(8,new ItemStack(Items.JACK_O_LANTERN));
        helper.assignments.assign(l,base);helper.assignments.assignContainer(l,base.west(6),Direction.UP,false);helper.assignments.assignContainer(l,base.east(6),Direction.UP,true);
    }
    private void waitAction(TestServerContext server,WorkAnimation action){
        try{server.waitFor(s->helper.workAnimation()==action,1200);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->"wanted "+action+", actual="+helper.workAnimation()+", transport="+helper.transport.activity()+", pos="+helper.position()+", pie="+oven.pie()+", baked="+oven.baked()),e);}
    }
    private void clientAction(ClientGameTestContext context,TestServerContext server,WorkAnimation action,boolean item){
        int id=server.computeOnServer(s->helper.getId());
        context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity p && p.workAnimation()==action,100);
        context.runOnClient(c->{var p=(PeepoEntity)c.level.getEntity(id);check(p.workPie().isEmpty()!=item,"client display stack for "+action);
            var renderer=(local.peepo.client.PeepoRenderer)c.getEntityRenderDispatcher().getRenderer(p);
            var state=renderer.createRenderState();renderer.extractRenderState(p,state,.5F);
            check(state.work==action,"renderer action "+action);check(state.pie.isEmpty()!=item,"resolved placeable pie model "+action);
        });
    }
    private void cycle(ClientGameTestContext context,TestServerContext server,Direction facing,int variant){
        server.runOnServer(s->setup(s.overworld(),facing,variant));
        waitAction(server,WorkAnimation.PIE_LOAD);clientAction(context,server,WorkAnimation.PIE_LOAD,true);
        server.runOnServer(s->{check(oven.pie()==null && count(helper.belongings,raw)==1,"raw cargo stays physical until placement");
            var d=helper.position().subtract(net.minecraft.world.phys.Vec3.atBottomCenterOf(base));
            check(d.x*facing.getStepX()+d.z*facing.getStepZ()>.5,"loads from front "+facing);
        });
        waitAction(server,WorkAnimation.PIE_WAIT);clientAction(context,server,WorkAnimation.PIE_WAIT,false);
        server.runOnServer(s->check(oven.pie()!=null && count(helper.belongings,raw)==0,"pie transferred once into oven"));
        if(variant==0)context.takeScreenshot("peepo_hearth_wait");
        waitAction(server,WorkAnimation.PIE_TAKE);clientAction(context,server,WorkAnimation.PIE_TAKE,true);
        server.runOnServer(s->check(oven.pie()==null && count(helper.belongings,baked)==1,"pie secured before removal animation"));
        waitAction(server,WorkAnimation.PIE_CARRY);clientAction(context,server,WorkAnimation.PIE_CARRY,true);
        try{server.waitFor(s->count(output,baked)==1,900);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->"delivery "+facing+": pos="+helper.position()+", transport="+helper.transport.activity()+", cargo="+count(helper.belongings,baked)+", energy="+helper.getEnergy()+", path="+helper.getNavigation().getPath()),e);}
        server.runOnServer(s->{check(count(source,raw)==0 && count(helper.belongings,baked)==0,"delivery conserves exactly one pie");check(helper.workPie().isEmpty(),"delivery clears display");});
    }
    private void interrupted(TestServerContext server){
        server.runOnServer(s->setup(s.overworld(),Direction.NORTH,0));waitAction(server,WorkAnimation.PIE_LOAD);
        server.runOnServer(s->check(helper.orders.command(s.getPlayerList().getPlayers().getFirst(),1),"Stay interrupts loading"));ticks(server,30);
        server.runOnServer(s->{check(oven.pie()==null && count(helper.belongings,raw)==1,"interruption retains raw pie");check(helper.workPie().isEmpty(),"interruption clears prop");helper.orders.command(s.getPlayerList().getPlayers().getFirst(),3);});
        server.waitFor(s->count(output,baked)==1,1600);
        server.runOnServer(s->check(count(helper.belongings,raw)==0,"resumed load consumed one raw pie"));
    }
    private void blockedOutput(TestServerContext server){
        server.runOnServer(s->setup(s.overworld(),Direction.NORTH,1));waitAction(server,WorkAnimation.PIE_WAIT);
        server.runOnServer(s->{for(int i=0;i<output.getContainerSize();i++)output.setItem(i,new ItemStack(Items.STONE,64));});
        waitAction(server,WorkAnimation.PIE_TAKE);ticks(server,60);
        server.runOnServer(s->{check(oven.pie()==null && count(helper.belongings,baked)==1,"full output still rescues pie");
            helper.transport.stop();helper.setNoAi(true);
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,s.registryAccess());helper.saveWithoutId(out);
            var copy=PeepoMod.JUGHEAD.create(s.overworld(),EntitySpawnReason.COMMAND);
            copy.load(TagValueInput.create(ProblemReporter.DISCARDING,s.registryAccess(),out.buildResult()));helper.discard();helper=copy;s.overworld().addFreshEntity(copy);
            check(copy.workPie().isEmpty() && count(copy.belongings,baked)==1,"reload keeps cargo, not transient prop");
            output.setItem(0,ItemStack.EMPTY);copy.setNoAi(false);
        });
        server.waitFor(s->count(output,baked)==1,1200);
        server.runOnServer(s->check(count(helper.belongings,baked)==0,"reload delivery does not duplicate pie"));
    }
    private void capacity(ServerLevel l){
        setup(l,Direction.NORTH,0);helper.setNoAi(true);
        check(!oven.getBlockState().isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),"brick oven shell is not a navigation passage");
        for(var f:PieFilling.values()){
            oven.selectPie(f);oven.set(0,0,null,0);var v=ItemVariant.of(JugcraftAgriculture.item(f.rawPie()));
            try(var tx=Transaction.openOuter()){check(oven.companionInputs().insert(v,64,tx)==1,"only one raw pie accepted "+f);tx.commit();}
            try(var tx=Transaction.openOuter()){check(oven.companionInputs().insert(v,64,tx)==0,"occupied oven rejects another pie "+f);}
            oven.set(600,100,f,HearthOvenBlockEntity.BURNT);
            try(var tx=Transaction.openOuter()){check(oven.companionOutputs().extract(ItemVariant.of(JugcraftAgriculture.item(f.burnt())),64,tx)==1,"burnt result "+f);tx.commit();}
            check(oven.pie()==null,"burnt extraction empties oven "+f);
        }
    }
}
