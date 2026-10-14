package local.peepo;

import java.util.*;
import java.util.function.BiConsumer;
import local.peepo.client.CompanionScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Zone selection, combined work, naming and server-synchronized labels in the produce suite. */
final class CompanionAnimalCareChecks {
    private final BiConsumer<Boolean,String> check;
    private final BlockPos base;
    private PeepoEntity npc;
    private Sheep sheep;
    private Cow cow;
    private Container source,output;
    private final List<Entity> spawned=new ArrayList<>();
    CompanionAnimalCareChecks(BiConsumer<Boolean,String> check,BlockPos base){this.check=check;this.base=base;}
    private void pause(TestServerContext server,int ticks){long start=server.computeOnServer(s->s.overworld().getGameTime());server.waitFor(s->s.overworld().getGameTime()>=start+ticks,ticks+30);}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private void place(ServerLevel level,Entity entity,double x,double z){entity.snapTo(base.getX()+x,base.getY(),base.getZ()+z,0,0);entity.setOnGround(true);level.addFreshEntity(entity);spawned.add(entity);}
    private ItemEntity egg(ServerLevel level,double x,double z){var egg=new ItemEntity(level,base.getX()+x,base.getY(),base.getZ()+z,new ItemStack(Items.EGG));egg.setDeltaMovement(Vec3.ZERO);egg.setNoPickUpDelay();egg.setOnGround(true);level.addFreshEntity(egg);spawned.add(egg);return egg;}
    private void click(CompanionScreen screen,String label){screen.children().stream().filter(w->w instanceof Button b && b.visible && b.active && b.getMessage().getString().equals(label)).map(w->(Button)w).findFirst().orElseThrow().onPress(null);}
    private void rename(CompanionScreen screen,String text){screen.children().stream().filter(w->w instanceof EditBox).map(w->(EditBox)w).findFirst().orElseThrow().setValue(text);click(screen,"Save");}
    void run(ClientGameTestContext context,TestServerContext server){
        try{
            context.runOnClient(c->{c.options.guiScale().set(1);c.resizeGui();});
            server.runOnServer(s->{var level=s.overworld();var player=s.getPlayerList().getPlayers().getFirst();
                for(var p:BlockPos.betweenClosed(base.offset(-3,-1,-3),base.offset(17,3,17)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
                player.setGameMode(GameType.CREATIVE);player.snapTo(base.getX()+.5,base.getY(),base.getZ()-.5,0,0);
                npc=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);npc.setNoAi(true);place(level,npc,-1.5,2.5);npc.orders.tame(player);npc.preferences.carryMeals=0;npc.preferences.social=false;
                var tool=new ItemStack(AssignmentTool.ITEM);player.setItemInHand(InteractionHand.MAIN_HAND,tool);AssignmentTool.select(player,tool,npc);AssignmentTool.plannerAction(player,0,1);
                check.accept(AssignmentTool.zoneMode(tool) && AssignmentTool.selected(level,tool)==npc,"Planner zone mode preserves the selected companion");
                check.accept(AnimalCareZone.valid(base,base.offset(15,0,15)) && !AnimalCareZone.valid(base,base.offset(16,0,0)) && !AnimalCareZone.valid(base,base.above(4)),"inclusive 16x16 and height limits");
                check.accept(npc.assignments.assignZone(base.below(),base.offset(16,-1,0)).startsWith("Choose corners"),"oversized zone rejected on server");
            });
            server.runCommand(String.format(Locale.ROOT,"tp @a %.2f %.2f %.2f",base.getX()+.5,(double)base.getY(),base.getZ()-.5));
            context.waitFor(c->AssignmentTool.zoneMode(c.player.getMainHandItem()) && AssignmentTool.selected(c.level,c.player.getMainHandItem())!=null,100);
            context.runOnClient(c->AssignmentTool.openPlanner.accept(InteractionHand.MAIN_HAND));context.takeScreenshot("peepo_planner_modes");context.runOnClient(c->c.gui.screen().onClose());
            pause(server,6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();AssignmentTool.edit(p,s.overworld(),InteractionHand.MAIN_HAND,base.below(),Direction.UP,false);check.accept(base.below().equals(AssignmentTool.firstCorner(p.getMainHandItem())),"first click saves only the corner");check.accept(npc.assignments.get(1)==null,"first corner does not consume a work slot");});
            pause(server,6);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();AssignmentTool.edit(p,s.overworld(),InteractionHand.MAIN_HAND,base.offset(3,-1,3),Direction.UP,false);var target=npc.assignments.get(1);
                check.accept(target!=null && target.care() && target.zone().tasks()==7,"second click creates one combined job with all tasks enabled");
                check.accept(!AssignmentTool.zoneMode(p.getMainHandItem()) && AssignmentTool.firstCorner(p.getMainHandItem())==null,"completed selection returns to normal assignments");
                check.accept(target.animalContains(Vec3.atBottomCenterOf(base.offset(3,0,3))) && !target.animalContains(Vec3.atBottomCenterOf(base.offset(4,0,3))),"exact rectangular boundary replaces the radius");
                npc.assignments.renameJob(1,"Main Pen");var before=npc.assignments.get(1);check.accept(before.equals(target),"job naming does not change transport identity");
                npc.assignments.assignZone(base.offset(6,-1,0),base.offset(8,-1,2));npc.assignments.renameJob(2,"Side Pen");npc.assignments.moveWork(1,1);
                check.accept(npc.assignments.label(2).equals("Main Pen") && npc.assignments.get(2).zone().equals(target.zone()),"job name and zone move together with priority");npc.assignments.clear(1);
                npc.assignments.toggleAnimalTask(1,AnimalCareZone.MILK);
                var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,s.overworld().registryAccess());npc.assignments.save(save);npc.assignments.load(TagValueInput.create(ProblemReporter.DISCARDING,s.overworld().registryAccess(),save.buildResult()));
                check.accept(npc.assignments.label(1).equals("Main Pen") && npc.assignments.get(1).zone().tasks()==5,"zone corners, toggles and name survive save/reload/compaction");npc.assignments.toggleAnimalTask(1,AnimalCareZone.MILK);
                var level=s.overworld();sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);sheep.setNoAi(true);sheep.setColor(DyeColor.WHITE);place(level,sheep,2.5,.5);cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);cow.setNoAi(true);place(level,cow,2.5,2.5);
                level.setBlockAndUpdate(base.west(2),Blocks.CHEST.defaultBlockState());source=(Container)level.getBlockEntity(base.west(2));level.setBlockAndUpdate(base.south(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.south(6));
                npc.assignments.assignContainer(level,base.west(2),Direction.UP,false);npc.assignments.assignContainer(level,base.south(6),Direction.UP,true);npc.assignments.supplies.toggleTools(0);
                var signPos=base.west(2).north();level.setBlockAndUpdate(signPos,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.NORTH));
                var sign=(SignBlockEntity)level.getBlockEntity(signPos);List<Component> lines=List.of(Component.literal("Animal Supplies"),Component.empty(),Component.empty(),Component.empty());sign.setText(new SignText(lines,lines,DyeColor.BLACK,false),SignTextSlot.FRONT);
                npc.assignments.refreshLabels();check.accept(npc.assignments.label(CompanionAssignments.SUPPLY).equals("Animal Supplies"),"attached chest sign labels Supply");
                p.openMenu(new SimpleMenuProvider((id,inv,owner)->new CompanionMenu(id,inv,npc),npc.getDisplayName()));
            });
            context.waitFor(c->c.gui.screen() instanceof CompanionScreen && c.player.containerMenu instanceof CompanionMenu m && m.companion(c.player)!=null && m.companion(c.player).assignments.view().get(1)!=null,100);
            context.waitTicks(2);
            context.runOnClient(c->{var screen=(CompanionScreen)c.gui.screen();click(screen,"Rename job 1");rename(screen,"Happy Barn");});
            server.waitFor(s->npc.assignments.label(1).equals("Happy Barn"),100);
            context.waitFor(c->{var n=((CompanionMenu)c.player.containerMenu).companion(c.player);return n!=null && n.assignments.label(1).equals("Happy Barn") && n.assignments.label(CompanionAssignments.SUPPLY).equals("Animal Supplies");},100);
            context.takeScreenshot("peepo_animal_care_settings");
            pause(server,5);
            context.runOnClient(c->{var screen=(CompanionScreen)c.gui.screen();click(screen,"Rename companion");rename(screen,"Barn Buddy");});
            server.waitFor(s->npc.getDisplayName().getString().equals("Barn Buddy"),100);
            pause(server,5);
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var m=(CompanionMenu)p.containerMenu;
                check.accept(!m.editDetails(p,new CompanionEditPayload(m.containerId,1,base.east(),0,"Wrong job")),"stale row identity cannot rename another job");
                check.accept(!m.editDetails(p,new CompanionEditPayload(m.containerId+1,0,BlockPos.ZERO,0,"Wrong menu")),"wrong menu identity cannot rename companion");
                check.accept(m.editDetails(p,new CompanionEditPayload(m.containerId,1,base.below(),AnimalCareZone.SHEAR,"")),"menu can toggle a zone task");
                check.accept(!npc.assignments.get(1).task(AnimalCareZone.SHEAR) && npc.assignments.get(1).task(AnimalCareZone.MILK),"task toggle changes only the requested task");
                p.closeContainer();p.setGameMode(GameType.SPECTATOR);
                egg(s.overworld(),1.5,1.5);egg(s.overworld(),4.5,1.5);npc.setNoAi(false);
            });
            server.waitFor(s->count(output,Items.EGG)==1,1000);
            server.runOnServer(s->{check.accept(!sheep.isSheared() && count(output,Items.MILK_BUCKET)==0,"disabled shearing and missing milk buckets do not block eggs");npc.assignments.toggleAnimalTask(1,AnimalCareZone.SHEAR);source.setItem(0,new ItemStack(Items.SHEARS));source.setItem(1,new ItemStack(Items.BUCKET,2));});
            try{server.waitFor(s->sheep.isSheared() && count(output,Items.MILK_BUCKET)==1 && count(output,Blocks.WOOL.white().asItem())>0,1500);}
            catch(AssertionError failure){server.runOnServer(s->org.slf4j.LoggerFactory.getLogger("peepo-care-test").error("Care timeout: routine={}, transport={}, status={}, pos={}, hand={}, buckets={}, milk={}, wool={}",npc.routineStatus(),npc.transport.activity(),CompanionStatus.from(npc.report.row(1)),npc.position(),npc.belongings.getItem(9),count(npc.belongings,Items.BUCKET),count(output,Items.MILK_BUCKET),count(output,Blocks.WOOL.white().asItem())));throw failure;}
            server.runOnServer(s->{check.accept(count(output,Items.EGG)==1,"out-of-zone egg is untouched");check.accept(npc.belongings.getItem(9).is(Items.SHEARS) && npc.belongings.getItem(9).getDamageValue()==1,"shared Tools Supply fetched and used shears once");check.accept(npc.assignments.label(1).equals("Happy Barn"),"zone name persists through combined work and deliveries");
                npc.setNoAi(true);for(int task:new int[]{1,2,4})npc.assignments.toggleAnimalTask(1,task);
                check.accept(npc.livestock.job(base.below()).workStatus(npc)==CompanionStatus.IDLE && npc.livestock.port(npc.assignments.get(1)).needed(new ItemStack(Items.BUCKET))==0,"all tasks off idles and stops bucket requests");
                npc.assignments.renameJob(1,"");check.accept(npc.assignments.label(1).startsWith("Livestock Management"),"blank job name restores default");
                check.accept(CompanionNames.clean("a\nb\tc\u0000d").equals("abcd") && CompanionNames.clean("x".repeat(80)).length()==48,"names reject control text and enforce a bounded length");
                var level=s.overworld();var target=npc.assignments.get(CompanionAssignments.SUPPLY);level.setBlockAndUpdate(base.west(2).north(),Blocks.AIR.defaultBlockState());
                check.accept(CompanionNames.container(level,target).equals(target.name()),"removed sign falls back to container name");
                var left=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT);var partner=base.west(2).relative(ChestBlock.getConnectedDirection(left));level.setBlock(base.west(2),left,18);level.setBlock(partner,left.setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT),18);
                var signPos=partner.north();level.setBlockAndUpdate(signPos,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.NORTH));var sign=(SignBlockEntity)level.getBlockEntity(signPos);
                List<Component> lines=List.of(Component.literal("Barn Tools"),Component.empty(),Component.empty(),Component.empty());sign.setText(new SignText(lines,lines,DyeColor.BLACK,false),SignTextSlot.BACK);
                check.accept(CompanionNames.container(level,target).equals("Barn Tools"),"back text on the other half of a double chest labels both halves");});
        }finally{server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();player.closeContainer();player.setGameMode(GameType.SPECTATOR);for(var e:spawned){if(e instanceof PeepoEntity p){p.transport.stop();p.resetCompanionRoutine();}e.discard();}});}
    }
}
