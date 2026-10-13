package local.peepo;

import java.util.*;
import java.util.function.BiConsumer;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Additional checks run only by PeepoCompanionClientTests, in the same disposable world. */
final class CompanionAutomationChecks {
    private final BiConsumer<Boolean,String> assertion;
    private final BiConsumer<String,Runnable> group;
    private final BlockPos base;
    private final List<PeepoEntity> entities=new ArrayList<>();
    CompanionAutomationChecks(BiConsumer<Boolean,String> assertion,BiConsumer<String,Runnable> group,BlockPos base){this.assertion=assertion;this.group=group;this.base=base;}
    private void check(boolean condition,String message){assertion.accept(condition,message);}
    private void teleportPlayer(TestServerContext server,double z){
        // ServerPlayer.snapTo alone does not teleport the connected client; its next packet would undo it.
        server.runCommand(String.format(Locale.ROOT,"tp @a %.1f %d %.1f 0 0",base.getX()+.5,base.getY(),base.getZ()+z));
    }
    private void serverTicks(TestServerContext server,int ticks){
        long ready=server.computeOnServer(s->s.overworld().getGameTime()+ticks);
        server.waitFor(s->s.overworld().getGameTime()>=ready,ticks+50);
    }
    /** Narrow entry for hearth-only regression runs; no other automation suites execute. */
    void runHearthOnly(ClientGameTestContext context,TestServerContext server){
        teleportPlayer(server,-3.5);
        server.runOnServer(s->staticGroup("hearth recipes, fuel, capacity and tender exclusivity",s.overworld(),
            ()->hearth(s.overworld(),s.getPlayerList().getPlayers().getFirst())));
        group.accept("automatic hearth fuel, tending and timely output",()->kitchen(server,"hearth_oven"));
        server.runOnServer(s->reset(s.overworld()));
    }
    void run(ClientGameTestContext context,TestServerContext server){
        teleportPlayer(server,-3.5);
        server.runOnServer(s->{
            var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();
            staticGroup("planner role swaps and independent transport persistence",l,()->roles(l,p));
            staticGroup("machine automation transactions and live side changes",l,()->automation(l));
            staticGroup("all processor helper registrations and recipe ports",l,()->machinePorts(l,p));
            staticGroup("cider input conversion and output rollback",l,()->cider(l));
            staticGroup("canning water, jar components and rollback",l,()->canning(l));
            staticGroup("hearth recipes, fuel, capacity and tender exclusivity",l,()->hearth(l,p));
        });
        group.accept("reusable tool and crank clips on both companion rigs",()->context.runOnClient(c->local.peepo.client.CompanionClipChecks.run(assertion)));
        group.accept("compact processor productive speed and reserve cost",()->processor(server,false));
        group.accept("large processor two-helper speed, cost and exclusivity",()->processor(server,true));
        group.accept("crank accepted power, player priority and flywheel hysteresis",()->crank(server));
        group.accept("porter physical delivery, full output and manifest reload",()->porter(server));
        group.accept("two porters competing for one source conserve every item",()->porterContention(server));
        group.accept("planner clicks, role colors and safe left-click removal",()->planner(context,server));
        group.accept("settings pause, command packets and inline transport controls",()->settings(context,server));
        group.accept("automatic standard machine supply, assistance and output",()->kitchen(server,"electric_furnace"));
        group.accept("disabling Supply returns an in-flight ingredient safely",()->supplyOff(server));
        group.accept("automatic cider supply, work and output",()->kitchen(server,"cider_press"));
        group.accept("automatic canning supply, bucket return and output",()->kitchen(server,"canning_kettle"));
        group.accept("automatic hearth fuel, tending and timely output",()->kitchen(server,"hearth_oven"));
        server.runOnServer(s->reset(s.overworld()));
    }
    private void staticGroup(String name,ServerLevel level,Runnable action){group.accept(name,()->{reset(level);try{action.run();}finally{cleanup();}});}
    private void cleanup(){for(var p:entities){p.resetCompanionRoutine();p.discard();}entities.clear();}
    private void reset(ServerLevel l){
        cleanup();
        for(var pos:BlockPos.betweenClosed(base.offset(-8,-1,-8),base.offset(9,9,9)))
            l.setBlock(pos,pos.getY()==base.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private PeepoEntity npc(ServerLevel l,ServerPlayer owner,boolean jughead){
        var p=(jughead?PeepoMod.JUGHEAD:PeepoMod.PEEPO).create(l,EntitySpawnReason.COMMAND);
        p.setNoAi(true);p.setNoGravity(true);p.snapTo(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);l.addFreshEntity(p);
        p.orders.tame(owner);p.preferences.carryMeals=0;entities.add(p);return p;
    }
    private void walking(PeepoEntity p){p.setNoAi(false);p.setNoGravity(false);p.setOnGround(true);}
    private PeepoEntity copy(PeepoEntity p){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.level().registryAccess());p.saveWithoutId(out);
        var q=PeepoMod.PEEPO.create(p.level(),EntitySpawnReason.COMMAND);
        q.load(TagValueInput.create(ProblemReporter.DISCARDING,p.level().registryAccess(),out.buildResult()));return q;
    }
    private Container chest(ServerLevel l,BlockPos pos){l.setBlockAndUpdate(pos,Blocks.BARREL.defaultBlockState());return (Container)l.getBlockEntity(pos);}
    private BlockEntity foodBlock(ServerLevel l,String id,BlockPos pos){l.setBlockAndUpdate(pos,JugcraftAgriculture.block(id).defaultBlockState());return l.getBlockEntity(pos);}
    private MachineBlockEntity machine(ServerLevel l,MachineKind kind,boolean full){
        var block=JugcraftMachines.MACHINES.get(kind);var state=block.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH);
        if(full && block instanceof LargeMachineBlock large)state=large.formed(state);
        var footprint=block.footprint(state);var pos=base.offset(2,0,2);
        for(int part=0;part<footprint.size();part++)l.setBlock(footprint.partPos(pos,Direction.NORTH,part),state.hasProperty(LargeMachineBlock.PART)?state.setValue(LargeMachineBlock.PART,part):state,2);
        return (MachineBlockEntity)l.getBlockEntity(pos);
    }
    private long insert(Storage<ItemVariant> port,ItemStack stack,boolean commit){try(var tx=Transaction.openOuter()){long n=port.insert(ItemVariant.of(stack),stack.getCount(),tx);if(commit)tx.commit();return n;}}
    private long extract(Storage<ItemVariant> port,ItemStack stack,boolean commit){try(var tx=Transaction.openOuter()){long n=port.extract(ItemVariant.of(stack),stack.getCount(),tx);if(commit)tx.commit();return n;}}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private void ironRecipe(MachineBlockEntity m){
        for(int i=0;i<32;i++){
            m.companionPort.select(new ItemStack(Items.IRON_INGOT));var plan=m.companionPort.selection();
            if(plan!=null && plan.parts().getFirst().ingredient().test(new ItemStack(Items.IRON_ORE)))return;
        }
        throw new AssertionError("No iron ore smelting plan");
    }
    private void roles(ServerLevel l,ServerPlayer owner){
        var p=npc(l,owner,false);var a=base.east(4);var b=base.west(4);chest(l,a);chest(l,b);
        p.assignments.cycleContainer(l,a,Direction.UP);
        check(p.assignments.get(6).at().pos().equals(a) && p.assignments.get(7)==null,"first container must be Supply");
        p.assignments.cycleContainer(l,b,Direction.NORTH);
        check(p.assignments.get(8).at().pos().equals(b),"second new container fills Supply 2");
        p.assignments.cycleContainer(l,a,Direction.SOUTH);
        check(p.assignments.get(7).at().pos().equals(a) && p.assignments.get(6)==null && p.assignments.get(8).at().pos().equals(b),"click switches only selected role");
        check(p.assignments.get(7).face()==Direction.SOUTH && p.assignments.get(8).face()==Direction.NORTH,"switch retains other container face");
        l.setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());p.assignments.cycleContainer(l,a,Direction.UP);
        check(p.assignments.get(6).at().pos().equals(a) && p.assignments.get(7)==null,"role switch is independent of unavailable other sources");
        for(int i=0;i<2;i++){var pos=base.offset(i*3,0,5);foodBlock(l,"cooking_pot",pos);p.assignments.assign(l,pos);}
        check(p.assignments.cycleTransport(1,true) && p.assignments.transportMode(1,true)==1,"Auto to On");
        p.assignments.cycleTransport(1,true);check(p.assignments.transportMode(1,true)==2 && p.assignments.transportMode(1,false)==0,"Supply Off independent of Output");
        var first=p.assignments.get(1);p.assignments.moveWork(1,1);
        check(p.assignments.get(2).equals(first) && p.assignments.transportMode(2,true)==2,"priority must carry mode");
        p.assignments.clear(1);var saved=copy(p);
        check(saved.assignments.get(1).equals(first) && saved.assignments.transportMode(1,true)==2,"compaction/save must carry mode");
        check(!p.assignments.transportAllowed(first,true) && p.assignments.transportAllowed(first,false),"directional Off gates");
        p.assignments.supplies.route(0).enabled=true;check(copy(p).assignments.supplies.route(0).enabled,"explicit porter route save");
    }
    private void automation(ServerLevel l){
        var m=machine(l,MachineKind.ELECTRIC_FURNACE,false);var input=ItemStorage.SIDED.find(l,m.getBlockPos(),Direction.UP);
        var p=npc(l,l.getServer().getPlayerList().getPlayers().getFirst(),false);p.assignments.assign(l,m.getBlockPos());
        check(input!=null && insert(input,new ItemStack(Items.IRON_ORE,2),false)==2,"observed machine input");
        check(m.getItem(0).isEmpty() && !m.itemAutomation.external(true),"aborted input must not mark external automation");
        try(var outer=Transaction.openOuter()){
            try(var inner=outer.openNested()){input.insert(ItemVariant.of(Items.IRON_ORE),1,inner);inner.commit();}
        }
        check(m.getItem(0).isEmpty() && !m.itemAutomation.external(true),"nested commit/outer abort");
        try(var scope=MachineItemAutomation.companionTransfer()){insert(input,new ItemStack(Items.IRON_ORE),true);}
        check(!m.itemAutomation.external(true),"companion must not disable its own Supply");
        insert(input,new ItemStack(Items.IRON_ORE),true);
        check(m.itemAutomation.external(true) && !m.itemAutomation.external(false),"committed input detected independently");
        check(!p.assignments.transportAllowed(p.assignments.get(1),true) && p.assignments.transportAllowed(p.assignments.get(1),false),"Auto disables only the automated direction");
        p.assignments.cycleTransport(1,true);check(p.assignments.transportAllowed(p.assignments.get(1),true),"On overrides external automation");
        p.assignments.cycleTransport(1,true);check(!p.assignments.transportAllowed(p.assignments.get(1),true),"Off disables Supply unconditionally");
        m.setItem(1,new ItemStack(Items.IRON_INGOT,3));var output=ItemStorage.SIDED.find(l,m.getBlockPos(),Direction.DOWN);
        try(var tx=Transaction.openOuter()){for(var v:output)if(!v.isResourceBlank())v.extract(v.getResource(),1,tx);}
        check(!m.itemAutomation.external(false) && m.getItem(1).getCount()==3,"aborted view extraction");
        try(var tx=Transaction.openOuter()){for(var v:output)if(!v.isResourceBlank())v.extract(v.getResource(),1,tx);tx.commit();}
        check(m.itemAutomation.external(false) && m.getItem(1).getCount()==2,"committed view extraction detected");
        // Keep the same port object while changing its exposed slots.
        m.sides().click(SideConfig.Face.TOP.ordinal());
        check(insert(input,new ItemStack(Items.IRON_ORE),true)==0 && extract(input,new ItemStack(Items.IRON_INGOT),false)==1,"cached port must follow side changes");
        reset(l);m=machine(l,MachineKind.ELECTRIC_FURNACE,false);
        l.setBlockAndUpdate(m.getBlockPos().above(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        check(m.itemAutomation.external(true) && !m.itemAutomation.external(false),"input hopper direction");
        l.setBlockAndUpdate(m.getBlockPos().below(),Blocks.HOPPER.defaultBlockState());m.sides().click(0);
        check(m.itemAutomation.external(false),"output hopper detected");
    }
    private void machinePorts(ServerLevel l,ServerPlayer owner){
        int kinds=0;
        for(var kind:MachineKind.values())if(kind.supportsCompanionAssistance()){
            var block=JugcraftMachines.MACHINES.get(kind);var state=block instanceof LargeMachineBlock large?large.formed(block.defaultBlockState()):block.defaultBlockState();
            var m=new MachineBlockEntity(base,state);m.setLevel(l);
            check(m.companionHelperCount()==(block.footprint(state).size()>1 || kind==MachineKind.ARC_FURNACE?2:1),"helper slots: "+kind);
            check(WorkAnimation.processor(kind,0)!=WorkAnimation.NONE,"clip mapping: "+kind);kinds++;
        }
        check(kinds==36,"expected 36 enabled processor kinds, got "+kinds);
        var m=machine(l,MachineKind.ALLOY_SMELTER,false);var p=npc(l,owner,false);p.assignments.assign(l,m.getBlockPos());
        var result=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("jugcraft:bronze_ingot"));
        check(m.companionPort.select(new ItemStack(result)),"bronze ghost plan available");
        var plan=m.companionPort.selection();check(plan.parts().size()>=2,"multi-input recipe fixture");
        for(var part:plan.parts()){
            var stack=new ItemStack(part.ingredient().items().findFirst().orElseThrow(),64);
            check(insert(m.companionPort.inputs(),stack,false)>0,"planned ingredient probe");
            check(insert(m.companionPort.inputs(),stack,true)==part.count(),"stock exactly one recipe batch");
        }
        check(insert(m.companionPort.inputs(),new ItemStack(Items.DIAMOND),true)==0,"reject unrelated ingredient");
        check(extract(m.companionPort.outputs(),m.getItem(0),true)==0,"never collect inputs as output");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());m.companionPort.save(out);
        var restored=new MachineCompanionPort(m);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),out.buildResult()));
        check(restored.plan().equals(plan.id()),"machine recipe persisted");
        for(int i=0;i<9;i++)m.companionPort.filter().set(i,ItemStack.EMPTY);
        check(m.companionPort.select(ItemStack.EMPTY) && !m.companionPort.locked(),"clear filter restores automatic recipe");
        check(!m.companionPort.select(new ItemStack(Items.DIAMOND_SWORD)),"invalid ghost rejected");
    }
    private void cider(ServerLevel l){
        var c=(CiderPressBlockEntity)foodBlock(l,"cider_press",base.east(3));
        check(insert(c.companionInputs(),new ItemStack(Items.APPLE,16),false)==8 && c.apples()==0,"apple rollback and capacity");
        insert(c.companionInputs(),new ItemStack(Items.APPLE,8),true);
        for(int i=0;i<8;i++)c.grind(i*8);for(int i=0;i<4;i++)c.turn(64+i*8);
        check(c.juice()==8 && insert(c.companionInputs(),new ItemStack(Items.GLASS_BOTTLE,8),false)==8 && c.juice()==8,"bottle conversion rollback");
        check(insert(c.companionInputs(),new ItemStack(Items.GLASS_BOTTLE,16),true)==8 && c.juice()==0,"one bottle per cider");
        var drink=new ItemStack(JugcraftAgriculture.item("sweet_cider"),16);
        check(extract(c.companionOutputs(),drink,false)==8 && extract(c.companionOutputs(),drink,true)==8 && extract(c.companionOutputs(),drink,true)==0,"cider extraction rollback and no duplication");
    }
    private void canning(ServerLevel l){
        var pos=base.east(3);var c=(CanningKettleBlockEntity)foodBlock(l,"canning_kettle",pos);
        check(insert(c.companionInputs(),new ItemStack(Items.WATER_BUCKET),false)==1 && !c.water(),"water rollback");
        insert(c.companionInputs(),new ItemStack(Items.WATER_BUCKET),true);
        check(c.water() && extract(c.companionOutputs(),new ItemStack(Items.BUCKET),false)==1,"bucket remainder");
        var jar=new ItemStack(BuiltInRegistries.ITEM.stream().filter(i->i instanceof PreserveJarItem).findFirst().orElseThrow(),4);PreserveJarItem.cooked(jar,l.getGameTime());
        check(c.companionNeed(jar)==0,"must clear returned bucket");extract(c.companionOutputs(),new ItemStack(Items.BUCKET),true);
        check(c.companionNeed(jar)==0,"cold kettle refuses new jars");l.setBlockAndUpdate(pos.below(),Blocks.CAMPFIRE.defaultBlockState());
        check(insert(c.companionInputs(),jar,false)==4 && c.jars().isEmpty(),"jar insertion rollback");
        insert(c.companionInputs(),jar,true);
        check(c.jars().size()==4 && ItemStack.isSameItemSameComponents(c.jars().getFirst(),jar),"preserve jar freshness components");
        check(extract(c.companionOutputs(),jar,true)==0,"unsealed fresh jars not output");
        var spoiled=jar.copy();PreserveJarItem.cooked(spoiled,l.getGameTime()-PreserveJarItem.SPOIL_TICKS-1);
        check(c.companionNeed(spoiled)==0,"reject spoiled input");
    }
    private void hearth(ServerLevel l,ServerPlayer owner){
        var c=(HearthOvenBlockEntity)foodBlock(l,"hearth_oven",base.east(3));var filling=PieFilling.values()[0];c.selectPie(filling);
        var raw=new ItemStack(JugcraftAgriculture.item(filling.rawPie()));
        check(c.companionNeed(new ItemStack(Items.COAL))==0,"empty oven not fuelled");
        check(insert(c.companionInputs(),raw,false)==1 && c.pie()==null,"raw pie rollback");insert(c.companionInputs(),raw,true);
        check(c.companionNeed(new ItemStack(Items.COAL_BLOCK))==0,"oversized fuel rejected");
        check(insert(c.companionInputs(),new ItemStack(Items.OAK_LOG,64),true)==2 && c.burn()==600,"bounded wood fuel reserve");
        var a=UUID.randomUUID();var b=UUID.randomUUID();check(c.claimTender(a) && !c.claimTender(b),"only one oven tender");c.releaseTender(a);check(c.claimTender(b),"release oven tender");
        c.set(600,100,filling,HearthOvenBlockEntity.BAKED);var pie=new ItemStack(JugcraftAgriculture.item(filling.pie()));
        check(extract(c.companionOutputs(),pie,false)==1 && c.pie()==filling,"baked pie rollback");check(extract(c.companionOutputs(),pie,true)==1 && c.pie()==null,"baked pie output");
        var p=npc(l,owner,false);p.assignments.assign(l,c.getBlockPos());var destination=chest(l,base.west(4));p.assignments.assignContainer(l,base.west(4),Direction.UP,true);
        for(int i=0;i<destination.getContainerSize();i++)destination.setItem(i,new ItemStack(Items.STONE,64));
        var port=CompanionLogistics.resolve(p,p.assignments.get(1));check(port.needed(raw)==0,"full Output prevents starting another pie");
        destination.setItem(0,ItemStack.EMPTY);check(port.needed(raw)==1,"output space permits raw pie");
    }
    private void processor(TestServerContext server,boolean full){
        final MachineBlockEntity[] machine={null};var helpers=new ArrayList<PeepoEntity>();
        server.runOnServer(s->{reset(s.overworld());var m=machine(s.overworld(),MachineKind.ELECTRIC_FURNACE,full);machine[0]=m;
            m.setItem(0,new ItemStack(Items.IRON_ORE,64));
            try(var tx=Transaction.openOuter()){for(int i=0;i<100;i++)m.energyFor(null).insert(128,tx);tx.commit();}
            for(int i=0;i<(full?2:1);i++){
                var p=npc(s.overworld(),s.getPlayerList().getPlayers().getFirst(),i==1);p.assignments.assign(s.overworld(),m.getBlockPos());
                p.snapTo(base.getX()+i*2+.5,base.getY(),base.getZ()-.5,0,0);helpers.add(p);walking(p);
            }
        });
        server.waitFor(s->helpers.stream().allMatch(p->p.workAnimation().hasTool()),600);
        long[] initial=server.computeOnServer(s->{var m=machine[0];
            var third=npc(s.overworld(),s.getPlayerList().getPlayers().getFirst(),false);third.assignments.assign(s.overworld(),m.getBlockPos());check(!m.companionJob(third).claim(third),"extra helper rejected");third.discard();
            // Reset progress through recipe selection, preserving the active worker leases.
            ironRecipe(m);
            return new long[]{s.overworld().getGameTime(),m.processingProgress(),m.energyFor(null).getAmount(),helpers.getFirst().getEnergy(),helpers.getLast().getEnergy()};});
        server.waitFor(s->s.overworld().getGameTime()>=initial[0]+40,100);
        server.runOnServer(s->{var m=machine[0];long ticks=s.overworld().getGameTime()-initial[0];long progress=m.processingProgress()-initial[1];
            check(Math.abs(progress-ticks*1.5)<=2,"actual processor speed: "+progress+" in "+ticks+" ticks");
            check(initial[2]-m.energyFor(null).getAmount()==progress*10,"bonus production must pay normal machine electricity");
            for(int i=0;i<helpers.size();i++)check(Math.abs(initial[3+i]-helpers.get(i).getEnergy()-ticks*(full?2:4))<=4,"furnace helper reserve cost "+i);
            m.setItem(0,ItemStack.EMPTY);int energy=helpers.getFirst().getEnergy();m.serverTick(s.overworld(),m.getBlockPos(),m.getBlockState());check(helpers.getFirst().getEnergy()==energy,"no input means no helper charge");});
        server.waitFor(s->helpers.stream().allMatch(p->p.workAnimation()==WorkAnimation.NONE),30);
        server.runOnServer(s->{check(helpers.stream().allMatch(p->p.isUsingJobAt(machine[0].getBlockPos())),"brief missing input keeps helper sessions");machine[0].setItem(0,new ItemStack(Items.IRON_ORE,64));});
        server.waitFor(s->helpers.stream().allMatch(p->p.workAnimation().hasTool()),30);
        server.runOnServer(s->{check(helpers.stream().allMatch(p->p.isUsingJobAt(machine[0].getBlockPos())),"helpers resume after refill without leaving");cleanup();});
    }
    private void crank(TestServerContext server){
        final PeepoEntity[] helper={null};final HandCrankBlockEntity[] crank={null};final FlywheelBlockEntity[] wheel={null};
        server.runOnServer(s->{var l=s.overworld();reset(l);var pos=base.east(3);
            l.setBlockAndUpdate(pos,JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING,Direction.EAST));
            l.setBlockAndUpdate(pos.east(),JugcraftKinetics.FLYWHEEL.defaultBlockState().setValue(FlywheelBlock.FACING,Direction.EAST));
            crank[0]=(HandCrankBlockEntity)l.getBlockEntity(pos);wheel[0]=(FlywheelBlockEntity)l.getBlockEntity(pos.east());
            var p=npc(l,s.getPlayerList().getPlayers().getFirst(),false);helper[0]=p;p.assignments.assign(l,pos);var job=crank[0].companionJob.prepare(p);
            check(job.claim(p),"crank claim: "+job.workStatus(p));var at=job.approachPosition();p.snapTo(at.x,at.y,at.z,0,0);check(job.occupy(p),"crank occupy");
            int energy=p.getEnergy();long stored=wheel[0].stored();check(job.work(p)==CompanionStatus.WORKING && wheel[0].stored()-stored==16 && energy-p.getEnergy()==16,"16 accepted KE equals reserve debit");
            job.work(p);check(wheel[0].stored()-stored==16,"same tick crank cannot duplicate generation");
            wheel[0].setStored(FlywheelBlockEntity.CAPACITY);
        });
        server.waitFor(s->crank[0].companionJob.workStatus(helper[0])==CompanionStatus.FULL,60);
        server.runOnServer(s->wheel[0].setStored(FlywheelBlockEntity.CAPACITY*95/100));
        server.waitFor(s->s.overworld().getGameTime()%25==0,40);
        server.runOnServer(s->{check(crank[0].companionJob.workStatus(helper[0])==CompanionStatus.FULL,"95% remains paused");wheel[0].setStored(FlywheelBlockEntity.CAPACITY*89/100);});
        server.waitFor(s->crank[0].companionJob.workStatus(helper[0])==CompanionStatus.READY,60);
        server.runOnServer(s->{crank[0].addTurns(100);check(crank[0].companionJob.workStatus(helper[0])==CompanionStatus.OCCUPIED,"manual crank takes priority");cleanup();});
    }
    private void porter(TestServerContext server){
        final PeepoEntity[] helper={null};final Container[] source={null},output={null};
        server.runOnServer(s->{var l=s.overworld();reset(l);var p=npc(l,s.getPlayerList().getPlayers().getFirst(),false);helper[0]=p;
            source[0]=chest(l,base.west(4));output[0]=chest(l,base.east(4));var cargo=new ItemStack(Items.DIAMOND,40);cargo.set(DataComponents.CUSTOM_NAME,Component.literal("Porter proof"));source[0].setItem(0,cargo);
            p.assignments.cycleContainer(l,base.west(4),Direction.UP);p.assignments.assignContainer(l,base.east(4),Direction.UP,true);p.assignments.supplies.route(0).enabled=true;p.orders.command(s.getPlayerList().getPlayers().getFirst(),3);walking(p);
        });
        server.waitFor(s->helper[0].transport.reserved(0),500);
        server.runOnServer(s->{var p=helper[0];check(count(source[0],Items.DIAMOND)==0 && count(p.belongings,Items.DIAMOND)==40 && count(output[0],Items.DIAMOND)==0,"real batch removed only after walking to Supply");
            var restored=copy(p);check(restored.transport.reserved(0) && restored.assignments.supplies.route(0).enabled && restored.orders.mode()==3 && restored.belongings.getItem(0).getHoverName().getString().equals("Porter proof"),"manifest, mode and item components reload");
            for(int i=0;i<output[0].getContainerSize();i++)output[0].setItem(i,new ItemStack(Items.STONE,64));
        });
        server.waitFor(s->helper[0].transport.porterStatus()==CompanionStatus.FULL,600);
        server.runOnServer(s->{check(count(helper[0].belongings,Items.DIAMOND)==40,"full destination retains physical cargo");output[0].clearContent();});
        server.waitFor(s->count(output[0],Items.DIAMOND)==40,1200);
        server.runOnServer(s->{check(count(source[0],Items.DIAMOND)==0 && count(helper[0].belongings,Items.DIAMOND)==0,"porter conservation across multiple trips");check(output[0].getItem(0).getHoverName().getString().equals("Porter proof"),"delivered custom components");cleanup();});
    }
    private void planner(ClientGameTestContext context,TestServerContext server){
        final PeepoEntity[] helper={null};
        teleportPlayer(server,-3.5);
        server.runOnServer(s->{var l=s.overworld();reset(l);var player=s.getPlayerList().getPlayers().getFirst();
            helper[0]=npc(l,player,false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AssignmentTool.ITEM));
            net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player,l,InteractionHand.MAIN_HAND,helper[0],null);
            check(AssignmentTool.selected(l,player.getMainHandItem())==helper[0],"planner selects owned companion");
            chest(l,base.offset(-2,0,1));chest(l,base.offset(2,0,1));
        });
        for(int i=0;i<3;i++){
            final BlockPos target=base.offset(i==1?2:-2,0,1);
            serverTicks(server,6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(p,s.overworld(),InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(target),Direction.NORTH,target,false));});
        }
        server.runOnServer(s->{check(helper[0].assignments.get(8).at().pos().equals(base.offset(2,0,1)) && helper[0].assignments.get(7).at().pos().equals(base.offset(-2,0,1)),"same right-click gesture creates sources and switches the selected role");});
        context.waitFor(c->AssignmentTool.selected(c.level,c.player.getMainHandItem()) instanceof PeepoEntity p && p.assignments.view().get(7)!=null,100);
        context.getInput().lookAt(base.above());context.waitTicks(10);context.takeScreenshot("peepo_planner_role_frames");
        server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var pos=base.offset(-2,0,1);
            var result=net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.invoker().interact(p,s.overworld(),InteractionHand.MAIN_HAND,pos,Direction.NORTH);
            check(result==InteractionResult.SUCCESS && helper[0].assignments.get(7)==null && s.overworld().getBlockState(pos).is(Blocks.BARREL),"left-click removes role without mining");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);cleanup();});
    }
    private void porterContention(TestServerContext server){
        final Container[] source={null},output={null};var workers=new ArrayList<PeepoEntity>();
        server.runOnServer(s->{var l=s.overworld();reset(l);source[0]=chest(l,base.west(4));output[0]=chest(l,base.east(4));
            source[0].setItem(0,new ItemStack(Items.DIAMOND,64));source[0].setItem(1,new ItemStack(Items.DIAMOND,64));
            for(int i=0;i<2;i++){var p=npc(l,s.getPlayerList().getPlayers().getFirst(),i==1);p.snapTo(base.getX()+.5,base.getY(),base.getZ()+i*2+1.5,0,0);
                p.assignments.assignContainer(l,base.west(4),Direction.UP,false);p.assignments.assignContainer(l,base.east(4),Direction.UP,true);p.assignments.supplies.route(0).enabled=true;p.orders.command(s.getPlayerList().getPlayers().getFirst(),3);walking(p);workers.add(p);}
        });
        server.waitFor(s->{
            int total=count(source[0],Items.DIAMOND)+count(output[0],Items.DIAMOND);for(var p:workers)total+=count(p.belongings,Items.DIAMOND);
            if(total!=128)throw new AssertionError("concurrent transfer lost or duplicated items: "+total);
            return count(output[0],Items.DIAMOND)==128;
        },1400);
        server.runOnServer(s->{check(count(source[0],Items.DIAMOND)==0 && workers.stream().allMatch(p->count(p.belongings,Items.DIAMOND)==0),"two companions complete shared route without leftovers");cleanup();});
    }
    private void settings(ClientGameTestContext context,TestServerContext server){
        final PeepoEntity[] helper={null};final Vec3[] position={null};
        final HearthOvenBlockEntity[] oven={null};final MachineBlockEntity[] processor={null};
        teleportPlayer(server,.5);
        server.runOnServer(s->{reset(s.overworld());var owner=s.getPlayerList().getPlayers().getFirst();
            var p=npc(s.overworld(),owner,true);p.snapTo(base.getX()+2.5,base.getY(),base.getZ()+.5,0,0);helper[0]=p;p.setCustomName(Component.literal("Test Jughead"));
            for(int i=0;i<4;i++){
                var pos=base.offset(-6+i*3,0,5);
                if(i==1)oven[0]=(HearthOvenBlockEntity)foodBlock(s.overworld(),"hearth_oven",pos);
                else if(i==2){processor[0]=machine(s.overworld(),MachineKind.ELECTRIC_FURNACE,false);pos=processor[0].getBlockPos();}
                else foodBlock(s.overworld(),"cooking_pot",pos);
                p.assignments.assign(s.overworld(),pos);
            }
            chest(s.overworld(),base.west(4));chest(s.overworld(),base.east(4));p.assignments.cycleContainer(s.overworld(),base.west(4),Direction.UP);p.assignments.assignContainer(s.overworld(),base.east(4),Direction.UP,true);walking(p);
            p.getNavigation().moveTo(base.getX()+6.5,base.getY(),base.getZ()+.5,1);
            owner.openMenu(new SimpleMenuProvider((id,inv,player)->new CompanionMenu(id,inv,p),p.getDisplayName()));position[0]=p.position();
        });
        context.waitForScreen(local.peepo.client.CompanionScreen.class);serverTicks(server,25);
        server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();check(player.containerMenu instanceof CompanionMenu menu && menu.editing(helper[0]),"settings still open and valid");check(helper[0].position().distanceToSqr(position[0])<.01 && helper[0].getNavigation().isDone(),"settings keeps companion still: "+position[0]+" -> "+helper[0].position());});
        recipePacket(context,server,47,JugcraftAgriculture.item(PieFilling.values()[0].rawPie()),()->!oven[0].filter().empty());
        recipePacket(context,server,48,Items.IRON_INGOT,()->processor[0].companionPort.locked());
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,50));
        server.waitFor(s->helper[0].assignments.transportMode(1,true)==1,100);serverTicks(server,3);
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,51));
        server.waitFor(s->helper[0].assignments.transportMode(1,false)==1,100);serverTicks(server,3);
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,302));
        server.waitFor(s->helper[0].assignments.supplies.route(0).enabled,100);context.waitTicks(5);context.takeScreenshot("peepo_porter_jobs_controls");
        context.runOnClient(c->c.player.closeContainer());context.waitForScreen(null);
        server.waitFor(s->s.getPlayerList().getPlayers().getFirst().containerMenu instanceof net.minecraft.world.inventory.InventoryMenu,100);
        server.runOnServer(s->{helper[0].getNavigation().moveTo(base.getX()+6.5,base.getY(),base.getZ()+.5,1);});
        server.waitFor(s->helper[0].position().distanceToSqr(position[0])>.25,150);
        server.runOnServer(s->cleanup());
    }
    private void recipePacket(ClientGameTestContext context,TestServerContext server,int legacySlot,Item icon,java.util.function.BooleanSupplier selected){
        int slot=CompanionMenu.FILTER_START;
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,60+legacySlot-46));
        context.waitFor(c->c.player.containerMenu instanceof CompanionMenu m && m.filterRow()==legacySlot-46,100);
        server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().getInventory().setItem(9,new ItemStack(icon,3)));
        context.waitFor(c->c.player.containerMenu.getSlot(10).getItem().is(icon),100);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,10,0,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        context.waitFor(c->c.player.containerMenu.getCarried().getCount()==3,100);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        server.waitFor(s->selected.getAsBoolean(),100);
        context.waitFor(c->c.player.containerMenu.getSlot(slot).getItem().getCount()==1,100);
        check(context.computeOnClient(c->c.player.containerMenu.getCarried().getCount()==3),"new workstation ghost preserves real cursor: "+slot);
        serverTicks(server,3);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,1,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        server.waitFor(s->!selected.getAsBoolean(),100);
        serverTicks(server,3);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        server.waitFor(s->selected.getAsBoolean(),100);
        context.runOnClient(c->c.gameMode.handleContainerInput(c.player.containerMenu.containerId,10,0,net.minecraft.world.inventory.ContainerInput.PICKUP,c.player));
        context.waitFor(c->c.player.containerMenu.getCarried().isEmpty() && c.player.containerMenu.getSlot(10).getItem().getCount()==3,100);
        // Client pickup prediction is immediate; finish its server packet before replacing this fixture slot.
        server.waitFor(s->{var p=s.getPlayerList().getPlayers().getFirst();return p.containerMenu.getCarried().isEmpty() && p.getInventory().getItem(9).is(icon) && p.getInventory().getItem(9).getCount()==3;},100);
        context.runOnClient(c->c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,64));
        context.waitFor(c->c.player.containerMenu instanceof CompanionMenu m && m.filterRow()<0,100);
    }
    void workSessionRegressions(TestServerContext server){
        group.accept("compact processor session",()->processor(server,false));
        group.accept("two-helper processor session",()->processor(server,true));
        group.accept("crank full-flywheel stop and restart",()->crank(server));
        for(var kind:List.of("electric_furnace","cider_press","canning_kettle"))
            group.accept("continued "+kind+" supply/work/output",()->kitchen(server,kind));
    }
    private void kitchen(TestServerContext server,String kind){
        final Container[] destination={null};final PeepoEntity[] helper={null};final Item[] product={null};
        server.runOnServer(s->{var l=s.overworld();reset(l);var owner=s.getPlayerList().getPlayers().getFirst();var p=npc(l,owner,kind.equals("canning_kettle"));helper[0]=p;
            BlockEntity block=kind.equals("electric_furnace")?machine(l,MachineKind.ELECTRIC_FURNACE,false):foodBlock(l,kind,base.offset(2,0,3));
            var pos=block.getBlockPos();var supply=chest(l,base.west(4));destination[0]=chest(l,base.east(6));
            if(block instanceof MachineBlockEntity m){product[0]=Items.IRON_INGOT;ironRecipe(m);supply.setItem(0,new ItemStack(Items.IRON_ORE,2));try(var tx=Transaction.openOuter()){for(int i=0;i<100;i++)m.energyFor(null).insert(128,tx);tx.commit();}}
            if(block instanceof CiderPressBlockEntity){supply.setItem(0,new ItemStack(Items.APPLE,2));supply.setItem(1,new ItemStack(Items.GLASS_BOTTLE,2));product[0]=JugcraftAgriculture.item("sweet_cider");}
            else if(block instanceof CanningKettleBlockEntity){
                l.setBlockAndUpdate(pos.below(),Blocks.CAMPFIRE.defaultBlockState());product[0]=BuiltInRegistries.ITEM.stream().filter(i->i instanceof PreserveJarItem).findFirst().orElseThrow();
                var jar=new ItemStack(product[0],2);PreserveJarItem.cooked(jar,l.getGameTime());supply.setItem(0,new ItemStack(Items.WATER_BUCKET));supply.setItem(1,jar);
            }else if(block instanceof HearthOvenBlockEntity oven){var filling=PieFilling.values()[0];oven.selectPie(filling);product[0]=JugcraftAgriculture.item(filling.pie());supply.setItem(0,new ItemStack(JugcraftAgriculture.item(filling.rawPie())));supply.setItem(1,new ItemStack(Items.OAK_LOG,4));}
            p.assignments.assign(l,pos);p.assignments.assignContainer(l,base.west(4),Direction.UP,false);p.assignments.assignContainer(l,base.east(6),Direction.UP,true);walking(p);
        });
        try{server.waitFor(s->count(destination[0],product[0])>0,2400);}
        catch(AssertionError e){throw new AssertionError(server.computeOnServer(s->kind+": pos="+helper[0].position()+", routine="+helper[0].routineStatus()+", transport="+helper[0].transport.activity()+", energy="+helper[0].getEnergy()),e);}
        server.runOnServer(s->{
            check(count(destination[0],product[0])>0,"automatic "+kind+" product arrived");
            if(kind.equals("canning_kettle")){check(count(destination[0],Items.BUCKET)==1,"empty bucket delivered");for(int i=0;i<destination[0].getContainerSize();i++)if(destination[0].getItem(i).is(product[0]))check(PreserveJarItem.sealed(destination[0].getItem(i)),"only sealed jars delivered");}
            cleanup();
        });
    }
    private void supplyOff(TestServerContext server){
        final PeepoEntity[] helper={null};final Container[] source={null};final MachineBlockEntity[] processor={null};
        server.runOnServer(s->{var l=s.overworld();reset(l);var p=npc(l,s.getPlayerList().getPlayers().getFirst(),false);helper[0]=p;
            var m=machine(l,MachineKind.ELECTRIC_FURNACE,false);processor[0]=m;ironRecipe(m);source[0]=chest(l,base.west(4));source[0].setItem(0,new ItemStack(Items.IRON_ORE,2));
            p.assignments.assign(l,m.getBlockPos());p.assignments.assignContainer(l,base.west(4),Direction.UP,false);walking(p);
        });
        server.waitFor(s->helper[0].transport.reserved(0),500);
        server.runOnServer(s->{check(count(source[0],Items.IRON_ORE)==1,"one batch ingredient physically picked up");helper[0].assignments.cycleTransport(1,true);helper[0].assignments.cycleTransport(1,true);});
        server.waitFor(s->count(source[0],Items.IRON_ORE)==2,500);
        server.runOnServer(s->{check(processor[0].getItem(0).isEmpty() && count(helper[0].belongings,Items.IRON_ORE)==0,"Off returned ingredient without insertion or duplication");cleanup();});
    }
}
