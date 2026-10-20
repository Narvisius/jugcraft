package local.peepo;

import java.util.*;
import java.util.function.BiConsumer;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import local.peepo.client.CompanionScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;

/** Real breeding, normal death loot, modded species, two-worker reservations and menu/persistence checks. */
final class CompanionPopulationChecks {
    final BiConsumer<Boolean,String> check;
    final BlockPos base;
    final List<Entity> created=new ArrayList<>();
    PeepoEntity npc,other;
    Container supply,output;
    CompanionHusbandry.Job reserved;
    static final Identifier COW=Identifier.withDefaultNamespace("cow"),TURKEY=Identifier.fromNamespaceAndPath("jugcraft","turkey");
    CompanionPopulationChecks(BiConsumer<Boolean,String> check,BlockPos base){this.check=check;this.base=base;}
    void ticks(TestServerContext server,int n){long until=server.computeOnServer(s->s.overworld().getGameTime()+n);server.waitFor(s->s.overworld().getGameTime()>=until,n+50);}
    int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    <T extends Entity>T add(ServerLevel level,T e,double x,double z){e.snapTo(base.getX()+x,base.getY(),base.getZ()+z,0,0);level.addFreshEntity(e);created.add(e);return e;}
    PeepoEntity helper(ServerLevel level,double z){var p=add(level,PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND),.5,z);p.setNoAi(true);p.orders.tame(level.getServer().getPlayerList().getPlayers().getFirst());p.preferences.social=false;p.preferences.carryMeals=0;p.assignments.assignZone(base.below(),base.offset(7,-1,7));return p;}
    void tasks(PeepoEntity p,int mask){for(int bit:new int[]{1,2,4,8,16})if(p.assignments.get(1).task(bit)!=((mask&bit)!=0))p.assignments.toggleAnimalTask(1,bit);}
    List<Animal> animals(ServerLevel level,Identifier type){return level.getEntitiesOfClass(Animal.class,npc.assignments.get(1).animalBox(),a->a.isAlive() && BuiltInRegistries.ENTITY_TYPE.getKey(a.getType()).equals(type));}
    void clearAnimals(ServerLevel level){for(var a:level.getEntitiesOfClass(Animal.class,npc.assignments.get(1).animalBox()))a.discard();}
    void click(ClientGameTestContext context,String label){
        context.waitFor(c->c.gui.screen() instanceof CompanionScreen && c.gui.screen().children().stream().anyMatch(w->w instanceof Button b && b.visible && b.active && b.getMessage().getString().equals(label)),100);
        context.runOnClient(c->c.gui.screen().children().stream().filter(w->w instanceof Button b && b.visible && b.active && b.getMessage().getString().equals(label)).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("Missing button: "+label)).onPress(null));context.waitTicks(2);
    }
    void run(ClientGameTestContext context,TestServerContext server){
        try{
            context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});
            server.runOnServer(s->{var level=s.overworld();var owner=s.getPlayerList().getPlayers().getFirst();owner.setGameMode(GameType.SPECTATOR);
                for(var p:BlockPos.betweenClosed(base.offset(-2,-1,-2),base.offset(10,3,10)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
                for(int x=-1;x<=8;x++)for(int z=-1;z<=8;z++)if(x==-1 || z==-1 || x==8 || z==8)for(int y=0;y<3;y++)level.setBlockAndUpdate(base.offset(x,y,z),Blocks.GLASS.defaultBlockState());
                npc=helper(level,3.5);other=helper(level,4.5);
                check.accept(npc.assignments.get(1).zone().tasks()==7 && npc.assignments.get(1).zone().populations().isEmpty(),"new livestock zones do not enable breeding/slaughter or select species");
                var old=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());var child=old.child("AnimalZone");child.store("First",BlockPos.CODEC,base.below());child.store("Last",BlockPos.CODEC,base.offset(7,-1,7));
                var bounded=new AnimalCareZone.Population(COW,999,-5);check.accept(bounded.target()==64 && bounded.keep()==0,"population settings are bounded");
                var migrated=AnimalCareZone.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),old.buildResult()));check.accept(migrated.tasks()==7 && migrated.populations().isEmpty(),"old zone saves migrate with dangerous tasks off");
                level.setBlockAndUpdate(base.offset(0,0,1),Blocks.CHEST.defaultBlockState());supply=(Container)level.getBlockEntity(base.offset(0,0,1));level.setBlockAndUpdate(base.offset(0,0,6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.offset(0,0,6));
                for(var p:List.of(npc,other)){p.assignments.assignContainer(level,base.offset(0,0,1),Direction.UP,false);p.assignments.assignContainer(level,base.offset(0,0,6),Direction.UP,true);p.assignments.supplies.toggleTools(0);tasks(p,AnimalCareZone.BREED);p.assignments.population(1,COW,5,3,false);}
                for(int i=0;i<4;i++){var cow=add(level,EntityTypes.COW.create(level,EntitySpawnReason.COMMAND),3.5+i%2*2,1.5+i/2*2);cow.setNoAi(true);}
            });
            ticks(server,3);
            server.runOnServer(s->{var job=new CompanionHusbandry(npc,npc.assignments.get(1)).breed;check.accept(job.find() && job.claim(npc),"first worker reserves one breeding pair");reserved=job;});
            ticks(server,2);
            server.runOnServer(s->{var job=new CompanionHusbandry(other,other.assignments.get(1)).breed;check.accept(!job.find(),"another pair cannot overshoot the same total target");reserved.release(npc);reserved=null;check.accept(new CompanionHusbandry(npc,npc.assignments.get(1)).needed(new ItemStack(Items.WHEAT))==4,"breeding supply demand survives releasing the work goal");for(var p:List.of(npc,other))tasks(p,AnimalCareZone.SLAUGHTER);});
            ticks(server,3);
            server.runOnServer(s->{
                for(int i=0;i<8;i++)npc.belongings.setItem(i,new ItemStack(Items.STONE,64));
                check.accept(!new CompanionHusbandry(npc,npc.assignments.get(1)).slaughter.find(),"full cargo prevents starting slaughter");
                var cow=animals(s.overworld(),COW).getFirst();var drop=new net.minecraft.world.entity.item.ItemEntity(s.overworld(),cow.getX(),cow.getY(),cow.getZ(),new ItemStack(Items.BEEF,3));
                try(var capture=CompanionSlaughterLoot.capture(npc,npc.assignments.get(1),cow)){s.overworld().addFreshEntity(drop);}
                check.accept(drop.isAlive() && drop.getItem().getCount()==3 && CompanionSlaughterLoot.marked(drop) && CompanionSlaughterLoot.hasOverflow(npc),"overflow loot remains a real marked ground stack");drop.discard();
                for(int i=0;i<8;i++)npc.belongings.setItem(i,ItemStack.EMPTY);
            });
            ticks(server,3);
            server.runOnServer(s->{var job=new CompanionHusbandry(npc,npc.assignments.get(1)).slaughter;check.accept(job.find() && job.claim(npc),"first worker reserves one surplus adult");reserved=job;});
            ticks(server,2);
            server.runOnServer(s->{var job=new CompanionHusbandry(other,other.assignments.get(1)).slaughter;check.accept(!job.find(),"other worker cannot reduce adults below the retention limit");reserved.release(npc);reserved=null;other.discard();});
            preciseTurkeyApproach(server);
            server.runOnServer(s->{
                clearAnimals(s.overworld());tasks(npc,AnimalCareZone.BREED);npc.assignments.population(1,TURKEY,3,2,true);npc.assignments.population(1,COW,3,2,false);supply.setItem(0,new ItemStack(Items.WHEAT,4));
                for(int i=0;i<8;i++)npc.belongings.setItem(i,ItemStack.EMPTY);
                add(s.overworld(),EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND),3.5,2.5);add(s.overworld(),EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND),5.5,3.5);npc.setNoAi(false);
            });
            try{server.waitFor(s->animals(s.overworld(),COW).stream().anyMatch(Animal::isBaby),1400);}catch(AssertionError e){diagnostic(server,"cow breeding");throw e;}
            server.runOnServer(s->{org.slf4j.LoggerFactory.getLogger("peepo-population-test").info("Cow breeding completed");var cows=animals(s.overworld(),COW);check.accept(cows.size()==3 && cows.stream().filter(Animal::isBaby).count()==1,"automatic supplied breeding produces one real calf at target");check.accept(count(supply,Items.WHEAT)+count(npc.belongings,Items.WHEAT)==2,"breeding consumes exactly two real food items");check.accept(cows.stream().filter(a->!a.isBaby()).allMatch(a->a.getAge()>0 && !a.isInLove()),"normal parent breeding cooldowns apply");npc.setNoAi(true);npc.resetCompanionRoutine();
                for(int i=0;i<8;i++)npc.belongings.setItem(i,ItemStack.EMPTY);supply.clearContent();clearAnimals(s.overworld());npc.assignments.population(1,COW,3,2,true);npc.assignments.population(1,TURKEY,3,2,false);supply.setItem(0,new ItemStack(Items.WHEAT_SEEDS,4));
                add(s.overworld(),JugcraftAgriculture.TURKEY.create(s.overworld(),EntitySpawnReason.COMMAND),3.5,2.5);add(s.overworld(),JugcraftAgriculture.TURKEY.create(s.overworld(),EntitySpawnReason.COMMAND),5.5,3.5);npc.setNoAi(false);
            });
            try{server.waitFor(s->animals(s.overworld(),TURKEY).stream().anyMatch(Animal::isBaby),1400);}catch(AssertionError e){diagnostic(server,"modded turkey breeding");throw e;}
            server.runOnServer(s->{org.slf4j.LoggerFactory.getLogger("peepo-population-test").info("Modded turkey breeding completed");check.accept(animals(s.overworld(),TURKEY).size()==3 && count(supply,Items.WHEAT_SEEDS)+count(npc.belongings,Items.WHEAT_SEEDS)==2,"selected modded species uses its own food and offspring");npc.setNoAi(true);npc.resetCompanionRoutine();clearAnimals(s.overworld());supply.clearContent();output.clearContent();for(int i=0;i<8;i++)npc.belongings.setItem(i,ItemStack.EMPTY);
                npc.assignments.population(1,TURKEY,3,2,true);npc.assignments.population(1,COW,10,2,false);tasks(npc,AnimalCareZone.SLAUGHTER);
                for(int i=0;i<3;i++){var a=add(s.overworld(),EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND),i==0?2.5:i==1?4.5:6.5,i==0?5.5:i==1?2.5:4.5);a.setNoAi(true);if(i==0)a.setCustomName(Component.literal("Keep me"));}
                var baby=add(s.overworld(),EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND),5.5,5.5);baby.setBaby(true);baby.setNoAi(true);
                var sheep=add(s.overworld(),EntityTypes.SHEEP.create(s.overworld(),EntitySpawnReason.COMMAND),3.5,5.5);sheep.setNoAi(true);
                var pet=add(s.overworld(),EntityTypes.WOLF.create(s.overworld(),EntitySpawnReason.COMMAND),6.5,6.5);pet.setNoAi(true);pet.setTame(true,true);npc.assignments.population(1,Identifier.withDefaultNamespace("wolf"),0,0,false);
                for(int slot=0;slot<5;slot++)npc.belongings.setItem(slot,new ItemStack(Items.STONE,64)); // Exactly three loot slots; the temporary tool slot must not cancel its own delivery.
                npc.snapTo(base.getX()+.5,base.getY(),base.getZ()+3.5,0,0); // A short tool trip must work before the animal scan backoff expires.
                supply.setItem(0,new ItemStack(Items.DIAMOND_SWORD));npc.setNoAi(false);
            });
            try{server.waitFor(s->animals(s.overworld(),COW).stream().filter(a->!a.isBaby()).count()==2 && count(output,Items.BEEF)>0,1600);}catch(AssertionError e){diagnostic(server,"slaughter and Output");throw e;}
            ticks(server,120);
            server.runOnServer(s->{check.accept(animals(s.overworld(),COW).stream().filter(a->!a.isBaby()).count()==2,"slaughter stops at the adult retention limit");check.accept(animals(s.overworld(),COW).stream().anyMatch(Animal::hasCustomName) && animals(s.overworld(),COW).stream().anyMatch(Animal::isBaby),"named adults and babies are protected");check.accept(animals(s.overworld(),Identifier.withDefaultNamespace("sheep")).size()==1,"unselected animal types are untouched");check.accept(animals(s.overworld(),Identifier.withDefaultNamespace("wolf")).size()==1,"selected tamed animals remain protected even with keep zero");check.accept(npc.assignedHand().is(Items.DIAMOND_SWORD) && npc.assignedHand().getDamageValue()>0,"Tools supply fetched a real weapon and attacks wear it");check.accept(count(output,Items.BEEF)>0 && count(output,Items.COOKED_BEEF)==0,"normal raw death loot reaches Output");check.accept(count(npc.belongings,Items.STONE)==320,"tool swap and loot delivery preserve personal cargo with only three free slots");npc.setNoAi(true);npc.resetCompanionRoutine();
                npc.assignments.population(1,TURKEY,10,6,false);var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,s.overworld().registryAccess());npc.assignments.save(saved);npc.assignments.load(TagValueInput.create(ProblemReporter.DISCARDING,s.overworld().registryAccess(),saved.buildResult()));check.accept(npc.assignments.get(1).zone().population(COW).keep()==2 && npc.assignments.get(1).zone().population(TURKEY).target()==10,"species rules survive saving and loading");
                npc.assignments.population(1,TURKEY,10,6,true);var turkey=add(s.overworld(),JugcraftAgriculture.TURKEY.create(s.overworld(),EntitySpawnReason.COMMAND),6.5,4.5);turkey.setNoAi(true);
                var owner=s.getPlayerList().getPlayers().getFirst();owner.setGameMode(GameType.CREATIVE);npc.snapTo(base.getX()+.5,base.getY(),base.getZ()+3.5,0,0);
            });
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",base.getX()+.5,(double)base.getY(),base.getZ()+4.5));
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.openMenu(new SimpleMenuProvider((id,inv,owner)->new CompanionMenu(id,inv,npc),npc.getDisplayName()));});
            context.waitForScreen(CompanionScreen.class);click(context,"Zone options");click(context,"Population...");
            context.waitFor(c->((CompanionMenu)c.player.containerMenu).populationTypes.containsAll(List.of(COW,TURKEY)),100);context.waitTicks(5);context.takeScreenshot("peepo_population_settings");
            ticks(server,5);
            context.runOnClient(c->{var m=(CompanionMenu)c.player.containerMenu;ClientPlayNetworking.send(new PopulationEditPayload(m.containerId,1,base.below(),1,TURKEY));});
            server.waitFor(s->npc.assignments.get(1).zone().population(TURKEY)!=null,100);
            server.runOnServer(s->check.accept(npc.assignments.get(1).zone().population(TURKEY).target()==10 && npc.assignments.get(1).zone().population(TURKEY).keep()==6,"discovered modded types can be added through menu networking"));
            ticks(server,5);
            context.runOnClient(c->{var m=(CompanionMenu)c.player.containerMenu;ClientPlayNetworking.send(new PopulationEditPayload(m.containerId,1,base.below(),4,COW));});
            server.waitFor(s->npc.assignments.get(1).zone().population(COW).target()==11,100);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var m=(CompanionMenu)p.containerMenu;check.accept(!m.editPopulation(p,new PopulationEditPayload(m.containerId+1,1,base.below(),2,COW)),"forged menu cannot remove a species");check.accept(npc.assignments.get(1).zone().population(COW).target()==11,"population controls synchronize through the open menu");p.closeContainer();p.setGameMode(GameType.SPECTATOR);});
        }finally{server.runOnServer(s->{if(reserved!=null)reserved.release(npc);if(npc!=null)clearAnimals(s.overworld());for(var e:created){if(e instanceof PeepoEntity p){p.resetCompanionRoutine();p.transport.stop();}e.discard();}var p=s.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setGameMode(GameType.SPECTATOR);});}
    }
    void preciseTurkeyApproach(TestServerContext server){
        server.runOnServer(s->{
            clearAnimals(s.overworld());npc.resetCompanionRoutine();tasks(npc,AnimalCareZone.BREED);
            npc.assignments.population(1,COW,3,2,true);npc.assignments.population(1,TURKEY,3,2,false);
            supply.clearContent();npc.belongings.setItem(0,new ItemStack(Items.WHEAT_SEEDS,4));
            // Keep the parents still until fed so the fractional-block arrival gap is deterministic.
            // Their normal mating AI is restored below; the original free-roaming test still follows.
            var first=add(s.overworld(),JugcraftAgriculture.TURKEY.create(s.overworld(),EntitySpawnReason.COMMAND),2.5,3.1);first.setNoAi(true);
            var second=add(s.overworld(),JugcraftAgriculture.TURKEY.create(s.overworld(),EntitySpawnReason.COMMAND),1.5,5.5);second.setNoAi(true);
            npc.snapTo(base.getX()+4.04,base.getY(),base.getZ()+3.2,0,0);
            var approach=CompanionLivestock.standing(npc,npc.assignments.get(1),first);
            check.accept(approach!=null && npc.position().distanceToSqr(approach)>.64,"fractional turkey approach starts outside interaction arrival range");
            var center=net.minecraft.world.phys.Vec3.atBottomCenterOf(BlockPos.containing(approach));
            check.accept(Math.abs(npc.getX()-center.x)<.555 && Math.abs(npc.getZ()-center.z)<.555,"vanilla waypoint tolerance already considers the approach block reached");
            // This synthetic pose is placed directly on the stone floor; establish its contact state too.
            npc.setOnGround(false);
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach),"final approach requires ground contact");
            npc.setOnGround(true);
            check.accept(CompanionNavigation.canFinishApproach(npc,approach),"clear supported final approach is allowed: pos="+npc.position()+", target="+approach+", collision="+!s.overworld().noCollision(npc,npc.getBoundingBox().expandTowards(approach.subtract(npc.position())).deflate(1.0e-5)));
            var obstacle=BlockPos.containing(approach);
            s.overworld().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach),"final approach cannot pass through solid blocks");
            s.overworld().setBlockAndUpdate(obstacle,Blocks.OAK_FENCE_GATE.defaultBlockState());
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach),"final approach cannot bypass a closed gate");
            s.overworld().setBlockAndUpdate(obstacle,Blocks.AIR.defaultBlockState());
            s.overworld().setBlockAndUpdate(obstacle.below(),Blocks.AIR.defaultBlockState());
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach),"final approach cannot cross missing footing");
            s.overworld().setBlockAndUpdate(obstacle.below(),Blocks.MAGMA_BLOCK.defaultBlockState());
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach),"final approach cannot cross a harmful floor");
            s.overworld().setBlockAndUpdate(obstacle.below(),Blocks.STONE.defaultBlockState());
            check.accept(!CompanionNavigation.canFinishApproach(npc,approach.add(-3,0,0))
                && !CompanionNavigation.canFinishApproach(npc,approach.add(0,1,0)),"final approach is limited to a short level step");
            npc.setNoAi(false);
        });
        try{server.waitFor(s->animals(s.overworld(),TURKEY).stream().filter(Animal::isInLove).count()==2,400);}catch(AssertionError e){diagnostic(server,"fractional turkey approach");throw e;}
        server.runOnServer(s->{
            check.accept(count(npc.belongings,Items.WHEAT_SEEDS)==2,"precise approach feeds both parents using exactly two seeds");
            for(var a:animals(s.overworld(),TURKEY))a.setNoAi(false);
        });
        server.waitFor(s->animals(s.overworld(),TURKEY).stream().anyMatch(Animal::isBaby),300);
        server.runOnServer(s->{
            check.accept(animals(s.overworld(),TURKEY).size()==3,"precise approach still produces a normal turkey offspring");
            npc.setNoAi(true);npc.resetCompanionRoutine();
        });
    }
    void diagnostic(TestServerContext server,String phase){server.runOnServer(s->{
        var job=npc.livestock.job(base.below());
        var path=npc.getNavigation().getPath();
        org.slf4j.LoggerFactory.getLogger("peepo-population-test").error("path={}, next={}, workAnimal={}",path,path==null||path.isDone()?null:path.getNextEntityPos(npc),npc.workAnimal());
        org.slf4j.LoggerFactory.getLogger("peepo-population-test").error("{}: routine={}, transport={}, status={}, pos={}, approach={}, navigationDone={}, hand={}, food={}, seeds={}, supply={}, animals={}, beef={}, wear={}",phase,npc.routineStatus(),npc.transport.activity(),CompanionStatus.from(npc.report.row(1)),npc.position(),job==null?null:job.approachPosition(),npc.getNavigation().isDone(),npc.assignedHand(),count(npc.belongings,Items.WHEAT),count(npc.belongings,Items.WHEAT_SEEDS),count(supply,Items.WHEAT)+count(supply,Items.WHEAT_SEEDS),
            s.overworld().getEntitiesOfClass(Animal.class,npc.assignments.get(1).animalBox()).stream().map(a->BuiltInRegistries.ENTITY_TYPE.getKey(a.getType())+":"+a.getHealth()+":"+a.position()+":age="+a.getAge()+(a.isInLove()?":in love":"")).toList(),count(output,Items.BEEF),npc.assignedHand().getDamageValue());
    });}
}
