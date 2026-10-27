package local.peepo;

import io.github.jimbozoomer.jugcraft.agriculture.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Tap rollback, batch limits and actual chest-to-tap-to-chest routes for both companions. */
public final class PeepoTreeTapClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private Container source,output;
    private int checks;
    private void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private TreeTapBlock tap(){return (TreeTapBlock)JugcraftAgriculture.block("tree_tap");}
    private void reset(ServerLevel level,boolean jughead,int fill,boolean upgraded){
        if(npc!=null){npc.resetCompanionRoutine();npc.transport.stop();npc.discard();}
        for(var pos:BlockPos.betweenClosed(base.offset(-8,-1,-6),base.offset(8,4,6)))level.setBlock(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        level.setBlock(base.south(),JugcraftAgriculture.block("rubber_log").defaultBlockState().setValue(RubberLogBlock.NATURAL,true),2);
        level.setBlock(base,tap().defaultBlockState().setValue(TreeTapBlock.FACING,Direction.NORTH).setValue(TreeTapBlock.UPGRADED,upgraded).setValue(TreeTapBlock.LATEX,fill),2);
        level.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(6));
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(6));
        npc=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(level,EntitySpawnReason.COMMAND);
        npc.setNoAi(true);npc.snapTo(base.getX()-2.5,base.getY(),base.getZ()-2.5,0,0);npc.setOnGround(true);level.addFreshEntity(npc);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);
        npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assign(level,base);
        npc.assignments.assignContainer(level,base.west(6),Direction.UP,false);
        npc.assignments.assignContainer(level,base.east(6),Direction.UP,true);
    }
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            c.setGenerateStructures(false);
        }).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            base=context.computeOnClient(c->c.player.blockPosition().above(4));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
            CompanionJob[] batch={null};server.runOnServer(s->batch[0]=unit(s.overworld()));
            batch(server,batch[0]);
            delivery(server,false,4,false,1);
            delivery(server,true,31,true,7);
            server.runOnServer(s->{npc.transport.stop();npc.resetCompanionRoutine();npc.discard();});
        }
        org.slf4j.LoggerFactory.getLogger("peepo-tree-tap-test").info("[peepo-tree-tap-test] PASS {} assertions",checks);
    }
    private CompanionJob unit(ServerLevel level){
        reset(level,false,32,true);
        check(npc.assignments.get(1).treeTap() && npc.assignments.get(1).carriesProduce(),"planner recognizes tap and marked Output");
        try(var tx=Transaction.openOuter()){
            check(tap().collectBottles(level,base,3,tx),"transaction extracts three bottles");
            check(level.getBlockState(base).getValue(TreeTapBlock.LATEX)==20,"three bottles consume twelve fill steps");
        }
        check(level.getBlockState(base).getValue(TreeTapBlock.LATEX)==32,"aborted extraction restores latex");
        try(var tx=Transaction.openOuter()){check(!tap().collectBottles(level,base,9,tx),"cannot over-extract");tx.commit();}
        check(level.getBlockState(base).getValue(TreeTapBlock.LATEX)==32,"invalid extraction unchanged");
        var port=CompanionLogistics.resolve(npc,npc.assignments.get(1));
        check(port.needed(new ItemStack(Items.GLASS_BOTTLE))==8,"supply buffers eight bottles");
        check(port.needed(new ItemStack(Items.BUCKET))==0,"companions never install bucket upgrades");
        npc.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(base.west()));
        var path=npc.getNavigation().createPath(base.east(),0,16);
        check(path!=null && path.canReach(),"can navigate from one side of the bucket-upgraded tap to the other");
        for(int i=0;i<path.getNodeCount();i++){
            var node=path.getNode(i);check(!node.asBlockPos().equals(base) && !node.asBlockPos().equals(base.above()),"delivery path goes around the tap, not through or over it");
        }
        var job=npc.treeTaps.job(base);check(job.workStatus(npc)==CompanionStatus.NEEDS_BOTTLES,"missing bottles status");
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();
        var land=io.github.jimbozoomer.jugcraft.finance.LandRegistry.get(level.getServer());
        var book=io.github.jimbozoomer.jugcraft.finance.Ledger.get(level.getServer()).book();
        var foreign=book.account(java.util.UUID.randomUUID(),"TapClaimOwner");
        try{
            check(land.assign(book,io.github.jimbozoomer.jugcraft.finance.LandRegistry.keyOf(base),foreign,0).ok(),"foreign claim fixture");
            check(job.workStatus(npc)==CompanionStatus.FORBIDDEN,"foreign claim denies tapping");
            check(port.needed(new ItemStack(Items.GLASS_BOTTLE))==0,"cached supply port observes permission change");
        }finally{land.abandon(owner,book,io.github.jimbozoomer.jugcraft.finance.LandRegistry.keyOf(base),true,0);}

        npc.belongings.setItem(0,new ItemStack(Items.GLASS_BOTTLE,8));
        check(job.claim(npc),"ready job claims tap");npc.setPos(job.approachPosition());
        var rival=PeepoMod.JUGHEAD.create(level,EntitySpawnReason.COMMAND);rival.setNoAi(true);rival.setPos(base.getX()+3,base.getY(),base.getZ()-3);level.addFreshEntity(rival);
        rival.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());rival.orders.command(level.getServer().getPlayerList().getPlayers().getFirst(),3);rival.assignments.assign(level,base);
        check(!rival.treeTaps.job(base).availableTo(rival),"second helper cannot claim occupied tap");
        rival.discard();return job;
    }
    private void batch(TestServerContext server,CompanionJob batch){
        long[] last={Long.MIN_VALUE};int[] ticks={0};CompanionStatus[] result={CompanionStatus.WORKING};
        server.waitFor(s->{
            if(last[0]==s.overworld().getGameTime())return false;last[0]=s.overworld().getGameTime();
            int energy=npc.getEnergy();result[0]=batch.work(npc);ticks[0]++;
            check(npc.getEnergy()==energy-CompanionTreeTap.ENERGY_PER_TICK,"collection charges four reserve JE per actual tick");
            return result[0]!=CompanionStatus.WORKING;
        },80);
        server.runOnServer(s->{var level=s.overworld();
        check(result[0]==CompanionStatus.IDLE && ticks[0]==CompanionTreeTap.WORK_TICKS,"collection takes exactly forty actual ticks");
        check(level.getBlockState(base).getValue(TreeTapBlock.LATEX)==0,"one action drains eight ready bottles");
        check(count(npc.belongings,JugcraftAgriculture.item("latex_bottle"))==8,"batch creates exactly eight filled bottles");
        check(count(npc.belongings,Items.GLASS_BOTTLE)==0,"batch consumes eight empties");
        check(level.getBlockTicks().hasScheduledTick(base,tap()),"native fill scheduler resumes");
        batch.release(npc);
        reset(level,false,4,false);
        npc.belongings.setItem(0,new ItemStack(Items.GLASS_BOTTLE,2));for(int i=1;i<8;i++)npc.belongings.setItem(i,new ItemStack(Items.STONE,64));
        var job=npc.treeTaps.job(base);check(job.workStatus(npc)==CompanionStatus.FULL,"full cargo refuses collection");
        check(count(npc.belongings,Items.GLASS_BOTTLE)==2 && level.getBlockState(base).getValue(TreeTapBlock.LATEX)==4,"capacity probe preserves bottles and latex");
        npc.belongings.setItem(0,new ItemStack(Items.GLASS_BOTTLE));job.release(npc);
        check(job.workStatus(npc)==CompanionStatus.READY,"consuming last empty bottle can free its slot");
        level.setBlock(base,Blocks.AIR.defaultBlockState(),2);
        check(job.workStatus(npc)==CompanionStatus.MISSING,"removed tap stops work");
        check(PeepoSpawns.LOCAL_LIMIT==6,"six-companion local cap remains");
        });
    }
    private void delivery(TestServerContext server,boolean jughead,int fill,boolean upgraded,int expected){
        server.runOnServer(s->{reset(s.overworld(),jughead,fill,upgraded);source.setItem(0,new ItemStack(Items.GLASS_BOTTLE,8));npc.setNoAi(false);});
        long[] nextLog={0};
        server.waitFor(s->{
            if(count(output,JugcraftAgriculture.item("latex_bottle"))==expected)return true;
            long now=s.overworld().getGameTime();if(now>=nextLog[0]){
                nextLog[0]=now+200;
                org.slf4j.LoggerFactory.getLogger("peepo-tree-tap-test").info("[peepo-tree-tap-test] route jughead={} position={} status={} transport={} source={} empties={} filled={} tap={} output={}",jughead,npc.position(),npc.stationStatus(base),npc.transport.activity(),count(source,Items.GLASS_BOTTLE),count(npc.belongings,Items.GLASS_BOTTLE),count(npc.belongings,JugcraftAgriculture.item("latex_bottle")),s.overworld().getBlockState(base),count(output,JugcraftAgriculture.item("latex_bottle")));
            }
            return false;
        },1600);
        server.runOnServer(s->{
            check(s.overworld().getBlockState(base).getValue(TreeTapBlock.LATEX)==fill-expected*4,"physical route preserves fractional latex");
            check(count(source,Items.GLASS_BOTTLE)+count(npc.belongings,Items.GLASS_BOTTLE)+count(output,Items.GLASS_BOTTLE)==8-expected,"physical supply consumes exact bottle count");
            check(count(output,JugcraftAgriculture.item("latex_bottle"))==expected,"physical Output receives marked latex for "+(jughead?"Jughead":"Peepo"));
        });
    }
}
