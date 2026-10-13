package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Pot handle clearance and the actual equipped cutting tool, in a disposable client/server world. */
public final class PeepoKitchenAnimationClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private CuttingBoardBlockEntity board;
    private int assertions;
    private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private void reset(ServerLevel level){
        if(npc!=null){npc.resetCompanionRoutine();npc.discard();npc=null;}
        for(var pos:BlockPos.betweenClosed(base.offset(-6,-1,-6),base.offset(6,5,6)))
            level.setBlock(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(8)))drop.discard();
    }
    private PeepoEntity companion(ServerLevel level,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        p.setNoAi(true);p.setNoGravity(true);p.snapTo(base.getX()+.5,base.getY(),base.getZ()-3.5,0,0);p.setOnGround(true);level.addFreshEntity(p);
        p.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());p.preferences.social=false;p.preferences.carryMeals=0;return npc=p;
    }
    private CuttingBoardJob setup(ServerLevel level,Direction facing,boolean raised,boolean jughead){
        reset(level);companion(level,jughead);var pos=raised?base.above():base;
        if(raised)level.setBlockAndUpdate(base,Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(pos,JugcraftAgriculture.block("cutting_board").defaultBlockState().setValue(CuttingBoardBlock.FACING,facing));
        board=(CuttingBoardBlockEntity)level.getBlockEntity(pos);board.put(new ItemStack(Items.PORKCHOP));
        npc.belongings.setItem(9,new ItemStack(JugcraftAgriculture.item(jughead?"diamond_knife":"iron_knife")));
        npc.assignments.assign(level,pos);return board.companionKitchen.job.prepare(npc);
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f",base.getX()+3.5,base.getY(),base.getZ()-3.5));
            server.runOnServer(s->pot(s.overworld()));
            server.runOnServer(s->{for(var facing:Direction.Plane.HORIZONTAL)for(boolean raised:new boolean[]{false,true})board(s.overworld(),facing,raised);});
            context.runOnClient(c->local.peepo.client.CompanionClipChecks.chopping(this::check));
            for(boolean jughead:new boolean[]{false,true})visual(context,server,jughead);
            // Retain the focused cache/navigation regression after narrowing eligible work sides.
            new CompanionPerformanceChecks(this::check,(name,run)->run.run(),base.offset(0,0,20)).run(context,server);
            server.runOnServer(s->reset(s.overworld()));
        }
        LoggerFactory.getLogger("kitchen-animation-test").info("[kitchen-animation-test] PASS {} assertions",assertions);
    }
    private void pot(ServerLevel level){
        reset(level);companion(level,true);level.setBlockAndUpdate(base,JugcraftAgriculture.block("cooking_pot").defaultBlockState());
        var pot=(CookingPotBlockEntity)level.getBlockEntity(base);npc.assignments.assign(level,base);
        level.setBlockAndUpdate(base.below(),Blocks.CAMPFIRE.defaultBlockState());
        var recipe=CookingPotRecipe.catalog(level.getServer()).entrySet().stream().filter(e->e.getValue().parts().size()<=6).findFirst().orElseThrow();
        pot.selectRecipe(recipe.getKey());var plan=pot.supplyPlan().orElseThrow();
        for(int i=0;i<plan.ingredients().size();i++){var ingredient=plan.ingredients().get(i);pot.setItem(i,new ItemStack(ingredient.ingredient().items().findFirst().orElseThrow(),ingredient.count()*8));}
        for(var side:Direction.Plane.HORIZONTAL){
            var at=Vec3.atBottomCenterOf(base.relative(side,3));npc.snapTo(at.x,at.y,at.z,0,0);
            var job=pot.companionJob;job.approachFailed(npc);job.prepare(npc);
            check(BlockPos.containing(job.approachPosition()).getX()==base.getX(),"pot chooses a north/south rim even when approached from handle side");
            var entry=job.approachPosition();npc.snapTo(entry.x,entry.y,entry.z,0,0);
            check(job.claim(npc) && job.occupy(npc),"handle-free pot rim can be occupied");
            var box=npc.getBoundingBox();check(box.maxZ<base.getZ()+7.5/16 || box.minZ>base.getZ()+8.5/16,"standing body clears the modeled handle plane");
            job.release(npc);
        }
        npc.snapTo(base.getX()+.5,base.getY(),base.getZ()-2.5,0,0);
        level.setBlockAndUpdate(base.north(),Blocks.STONE.defaultBlockState());
        pot.companionJob.approachFailed(npc);pot.companionJob.prepare(npc);
        check(BlockPos.containing(pot.companionJob.approachPosition()).equals(base.south()),"blocked north rim falls back to south");
        level.setBlockAndUpdate(base.south(),Blocks.STONE.defaultBlockState());
        pot.companionJob.approachFailed(npc);pot.companionJob.prepare(npc);
        check(pot.companionJob.workStatus(npc)==CompanionStatus.BLOCKED,"no fallback through pot handles");
    }
    private void board(ServerLevel level,Direction facing,boolean raised){
        var job=setup(level,facing,raised,raised);var entry=job.approachPosition();
        check(BlockPos.containing(entry).equals(base.relative(facing)),"board front approach "+facing+" raised="+raised);
        npc.snapTo(entry.x,entry.y,entry.z,0,0);check(job.claim(npc) && job.occupy(npc),"board mount");
        check(npc.getDirection()==facing.getOpposite(),"faces the food from board front");
        check(Math.abs(npc.getY()-(base.getY()+(raised?1+1.0/16+.001:0)))<.00001,"correct board work height");
        for(int i=0;i<39;i++)check(job.work(npc)==CompanionStatus.WORKING && !board.item().isEmpty(),"no recipe before completed downstroke");
        int energy=npc.getEnergy();check(job.work(npc)==CompanionStatus.WORKING && board.item().isEmpty(),"cut completes on tick forty");
        check(energy-npc.getEnergy()==16 && npc.belongings.getItem(9).getDamageValue()==1,"one knife wear and one energy payment");
        check(board.companionKitchen.outputStorage().iterator().next().getAmount()==2,"both bacon results retained");
        job.release(npc);check(npc.getY()==base.getY() && !npc.isNoGravity(),"safe ground exit after cutting");
        board.put(new ItemStack(Items.PORKCHOP));
        level.setBlockAndUpdate(base.relative(facing),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base.relative(facing).above(),Blocks.STONE.defaultBlockState());
        job.approachFailed(npc);job.prepare(npc);check(job.workStatus(npc)==CompanionStatus.BLOCKED,"blocked front cannot chop from board back");
    }
    private void visual(ClientGameTestContext context,TestServerContext server,boolean jughead){
        server.runOnServer(s->{setup(s.overworld(),Direction.NORTH,jughead,jughead);npc.setNoAi(false);npc.setNoGravity(false);npc.setOnGround(true);});
        server.waitFor(s->npc.workAnimation()==WorkAnimation.CHOP,500);
        int id=server.computeOnServer(s->npc.getId());
        context.waitFor(c->c.level.getEntity(id) instanceof PeepoEntity p && p.workAnimation()==WorkAnimation.CHOP,100);
        context.runOnClient(c->{
            var p=(PeepoEntity)c.level.getEntity(id);var r=(local.peepo.client.PeepoRenderer)c.getEntityRenderDispatcher().getRenderer(p);var state=r.createRenderState();r.extractRenderState(p,state,.5F);
            check(state.work==WorkAnimation.CHOP && !state.knife.isEmpty() && state.held.isEmpty(),"actual equipped knife model resolves once");
        });
        server.waitFor(s->board.item().isEmpty(),100);
        server.runOnServer(s->{check(npc.belongings.getItem(9).getDamageValue()==1,"autonomous chop used equipped knife");board.put(new ItemStack(Items.PORKCHOP));});
        server.waitFor(s->npc.workAnimation()==WorkAnimation.CHOP,200);
        // Hold a live claimed station for photographing both phases; production was verified above.
        server.runOnServer(s->{npc.setNoAi(true);board.companionKitchen.job.release(npc);});
        int oldFov=context.computeOnClient(c->c.options.fov().get());context.runOnClient(c->c.options.fov().set(40));
        context.getInput().pressKey(options->options.keyToggleGui);
        server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",base.getX()+2.5,base.getY()+(jughead?.65:0),base.getZ()+2.5));
        context.waitTicks(8);
        context.getInput().lookAt(base.above(jughead?1:0));
        server.runOnServer(s->{var job=board.companionKitchen.job.prepare(npc);var at=job.approachPosition();npc.snapTo(at.x,at.y,at.z,0,0);check(job.claim(npc) && job.occupy(npc),"photo mount");npc.setWorkAnimation(WorkAnimation.CHOP,board.getBlockPos());});
        for(int phase:new int[]{27,39}){
            server.waitFor(s->{var job=board.companionKitchen.job;job.occupy(npc);npc.setWorkAnimation(WorkAnimation.CHOP,board.getBlockPos());return Math.floorMod(s.overworld().getGameTime()-npc.workStarted(),40)==phase;},100);
            context.takeScreenshot((jughead?"jughead":"peepo")+"_knife_"+(phase==27?"raised":"strike"));
        }
        context.runOnClient(c->c.options.fov().set(oldFov));context.getInput().pressKey(options->options.keyToggleGui);
        server.runOnServer(s->{board.companionKitchen.job.release(npc);npc.discard();npc=null;});
    }
}
