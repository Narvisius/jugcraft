package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.farming.*;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.courier.*;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;

/** Recent integration regression: real world adapters, transaction aborts, save state and autonomous hauling. */
public final class PeepoIntegrationClientTests implements FabricClientGameTest {
    private BlockPos base;
    private PeepoEntity npc;
    private Container source,output;
    private int checks,groups;
    private final List<String> failures=new ArrayList<>();
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private void group(String name,Runnable action){try{action.run();groups++;org.slf4j.LoggerFactory.getLogger("peepo-integration-test").info("PASS {}",name);}catch(Throwable error){failures.add(name+": "+error);org.slf4j.LoggerFactory.getLogger("peepo-integration-test").error("FAIL "+name,error);}}
    private int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private long amount(Storage<ItemVariant> s,Item item){long n=0;for(var v:s)if(v.getResource().isOf(item))n+=v.getAmount();return n;}
    private Block block(String id){var b=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("jugcraft",id));check(b!=Blocks.AIR,"registered block "+id);return b;}
    private void reset(ServerLevel level){
        if(npc!=null){npc.transport.stop();npc.resetCompanionRoutine();npc.discard();}
        for(var p:BlockPos.betweenClosed(base.offset(-8,-2,-6),base.offset(8,5,6)))level.setBlock(p,p.getY()<base.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),18);
        level.setBlockAndUpdate(base.west(6),Blocks.BARREL.defaultBlockState());source=(Container)level.getBlockEntity(base.west(6));
        level.setBlockAndUpdate(base.east(6),Blocks.BARREL.defaultBlockState());output=(Container)level.getBlockEntity(base.east(6));
        npc=PeepoMod.PEEPO.create(level,EntitySpawnReason.COMMAND);npc.setNoAi(true);npc.snapTo(base.getX()-2.5,base.getY(),base.getZ()+2.5,0,0);npc.setOnGround(true);level.addFreshEntity(npc);
        var owner=level.getServer().getPlayerList().getPlayers().getFirst();npc.orders.tame(owner);npc.orders.command(owner,3);npc.preferences.social=false;npc.preferences.carryMeals=0;
        npc.assignments.assignContainer(level,base.west(6),Direction.UP,false);npc.assignments.assignContainer(level,base.east(6),Direction.UP,true);
    }
    private CompanionLogistics.Port assign(ServerLevel level,Block b){level.setBlock(base,b.defaultBlockState(),18);var placed=level.getBlockEntity(base);if(placed instanceof FarmStandBlockEntity stand)stand.setOwner(level.getServer().getPlayerList().getPlayers().getFirst());if(placed instanceof CandyBowlBlockEntity bowl)bowl.setOwner(npc.orders.ownerId());npc.assignments.assign(level,base);var t=npc.assignments.get(1);check(t!=null,"assignment "+b);var port=CompanionLogistics.resolve(npc,t);check(port!=null,"logistics port "+b);return port;}
    private long insert(Storage<ItemVariant> s,ItemStack item,int n,boolean commit){try(var tx=Transaction.openOuter()){long result=s.insert(ItemVariant.of(item),n,tx);if(commit)tx.commit();return result;}}
    private long extract(Storage<ItemVariant> s,Item item,int n,boolean commit){try(var tx=Transaction.openOuter()){long result=s.extract(ItemVariant.of(item),n,tx);if(commit)tx.commit();return result;}}
    public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().adjustSettings(c->{c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));c.setGenerateStructures(false);}).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();base=context.computeOnClient(c->c.player.blockPosition().above(5));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode creative @a");server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runOnServer(s->{var l=s.overworld();group("sprinkler transactions, limits and persistence",()->sprinkler(l));group("serving stations",()->serving(l));group("spinning and cider",()->craftStations(l));group("vanilla, brewing, hives and compost",()->vanilla(l));group("farm stand and crucible",()->commerce(l));group("crucible demand: the exact shortage, nothing for an incompatible formula",()->crucibleDemand(l));group("crucible demand: noOverSupplyWithAnOpenCourierRequest",()->noOverSupplyWithAnOpenCourierRequest(l));group("solid fuel and special-machine logistics",()->machines(l));group("garden zones, maintenance and crop rules",()->garden(l));group("industrial, magic and bat house",()->special(l));});
            group("water collection Supply to Output",()->waterTrip(server));
            group("sprinkler Supply to Output",()->sprinklerTrip(server));
            group("water bucket and named canteen",()->waterVariants(server));
            group("water bank arrival",()->waterBank(server));
            group("spinning wheel autonomous operation",()->spinningTrip(server));
            group("rice and cranberry garden harvests",()->wetCrops(server));
            group("industrial two-helper productive effort",()->industrialTrip(server));
            group("brewing chain Supply to Output",()->brewingTrip(server));
            group("hive and magic crop harvesting",()->livingHarvests(server));
            server.runOnServer(s->{if(npc!=null){npc.resetCompanionRoutine();npc.discard();}});
        }
        org.slf4j.LoggerFactory.getLogger("peepo-integration-test").info("RESULT {} groups, {} assertions, {} failures",groups,checks,failures.size());
        if(!failures.isEmpty())throw new AssertionError(String.join("; ",failures));
    }
    private void sprinkler(ServerLevel l){
        reset(l);var p=assign(l,JugcraftFarming.SPRINKLER);var be=(SprinklerBlockEntity)l.getBlockEntity(base);
        check(insert(p.inputs(),new ItemStack(Items.WATER_BUCKET),4,false)==4,"probe accepts four buckets");check(be.water().amount==0 && amount(p.outputs(),Items.BUCKET)==0,"probe creates no water or buckets");
        check(insert(p.inputs(),new ItemStack(Items.WATER_BUCKET),4,true)==4,"fill four real buckets");check(be.water().amount==4*FluidConstants.BUCKET && amount(p.outputs(),Items.BUCKET)==4,"water and empties conserved");
        check(insert(p.inputs(),new ItemStack(Items.WATER_BUCKET),1,true)==0,"full tank refuses water");
        check(insert(p.inputs(),new ItemStack(PetroItems.FERTILIZER),64,false)==4 && be.fertilizer()==0,"fertilizer simulation rollback");
        check(insert(p.inputs(),new ItemStack(PetroItems.FERTILIZER),64,true)==4 && be.fertilizer()==4,"fertilizer stock cap");check(p.needed(new ItemStack(Items.BONE_MEAL))==0,"native fertilizer only");
        check(extract(p.outputs(),Items.BUCKET,4,false)==4 && amount(p.outputs(),Items.BUCKET)==4,"output abort conserves empties");
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,l.registryAccess());be.companion.save(save);be.companion.takeBuckets();be.companion.load(TagValueInput.create(ProblemReporter.DISCARDING,l.registryAccess(),save.buildResult()));check(amount(p.outputs(),Items.BUCKET)==4,"bucket buffer reload");
        check(extract(p.outputs(),Items.BUCKET,4,true)==4 && amount(p.outputs(),Items.BUCKET)==0,"output commit");
        try(var tx=Transaction.openOuter()){be.water().extract(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),FluidConstants.BUCKET/2,tx);tx.commit();}
        check(p.needed(new ItemStack(Items.WATER_BUCKET))==0,"half-bucket room cannot waste water");
    }
    private void serving(ServerLevel l){
        reset(l);var p=assign(l,block("feast_table"));var table=(FeastTableBlockEntity)l.getBlockEntity(base);table.serve(0,new ItemStack(Items.BREAD));table.companion.observe();table.clear(0);
        check(p.needed(new ItemStack(Items.BREAD))==8,"remember empty dish");check(insert(p.inputs(),new ItemStack(Items.BREAD),64,false)==8 && table.dish(0).isEmpty(),"table rollback");check(insert(p.inputs(),new ItemStack(Items.BREAD),64,true)==8,"table servings capped");check(extract(p.outputs(),Items.BREAD,8,true)==0,"table food cannot be stolen as output");
        reset(l);p=assign(l,block("candy_bowl"));check(p.needed(new ItemStack(Items.COBBLESTONE))==0,"bowl rejects non-treats");
        reset(l);p=assign(l,block("witchs_brew_punch_bowl"));check(insert(p.inputs(),new ItemStack(Items.SWEET_BERRIES),64,false)==6,"punch capacity probe");check(l.getBlockState(base).getValue(PunchBowlBlock.SERVINGS_LEFT)==0,"punch probe leaves block unchanged");
        check(insert(p.inputs(),new ItemStack(Items.SWEET_BERRIES),6,true)==6,"punch berries accepted");check(insert(p.inputs(),new ItemStack(Items.GLASS_BOTTLE),12,true)==12,"punch bottles produced");check(amount(p.outputs(),JugcraftAgriculture.item("witchs_brew_punch"))==12,"punch output bounded");
    }
    private void craftStations(ServerLevel l){
        reset(l);var p=assign(l,block("spinning_wheel"));var wheel=(SpinningWheelBlockEntity)l.getBlockEntity(base);
        check(insert(p.inputs(),new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool"))),64,false)==1 && wheel.wool()==null,"spinning input rollback");check(insert(p.inputs(),new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool"))),64,true)==1,"one wool batch");check(p.needed(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_wool"))))==0,"busy spinning rejects next batch");
        reset(l);p=assign(l,block("cider_barrel"));var barrel=(CiderBarrelBlockEntity)l.getBlockEntity(base);var cider=new ItemStack(JugcraftAgriculture.item("sweet_cider"));
        check(insert(p.inputs(),cider,20,false)==16 && barrel.servings()==0,"cider rollback");check(insert(p.inputs(),cider,20,true)==16 && barrel.servings()==16,"cider batch capacity");check(amount(p.outputs(),JugcraftAgriculture.item("sparkling_cider"))==0,"unaged cider not exposed");barrel.selectCompanionStage(true);check(barrel.companionStage()==2,"aged selection");barrel.selectCompanionStage(false);check(barrel.companionStage()==1,"sparkling selection");
        reset(l);p=assign(l,block("candy_kettle"));source.setItem(0,new ItemStack(Items.SUGAR,4));source.setItem(1,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(2,new ItemStack(JugcraftAgriculture.item("candy_tray")));p.prepare(ContainerStorage.of(source,Direction.UP));check(p.plan()!=null,"candy chooses available batch");check(insert(p.inputs(),new ItemStack(Items.SUGAR),64,false)==4 && p.needed(new ItemStack(Items.SUGAR))==4,"candy bounded input rollback");
    }
    private void vanilla(ServerLevel l){
        for(var b:List.of(Blocks.FURNACE,Blocks.SMOKER,Blocks.BLAST_FURNACE,Blocks.CAMPFIRE,Blocks.CRAFTER,Blocks.BREWING_STAND,Blocks.BEEHIVE,Blocks.COMPOSTER)){reset(l);var p=assign(l,b);check(insert(p.inputs(),new ItemStack(Items.BEDROCK),64,true)==0,"reject unrelated vanilla input "+b);}
        reset(l);var p=assign(l,Blocks.FURNACE);source.setItem(0,new ItemStack(Items.RAW_IRON,3));source.setItem(1,new ItemStack(Items.COAL,4));p.prepare(ContainerStorage.of(source,Direction.UP));check(p.plan()!=null,"furnace selects available recipe");check(insert(p.inputs(),new ItemStack(Items.RAW_IRON),3,false)>0 && ((Container)l.getBlockEntity(base)).getItem(0).isEmpty(),"furnace abort");
        reset(l);p=assign(l,Blocks.BREWING_STAND);var brew=(VanillaCompanionPort)p;brew.filter.set(0,PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS));brew.filterChanged();source.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(1,new ItemStack(Items.NETHER_WART));source.setItem(2,new ItemStack(Items.SUGAR));source.setItem(3,new ItemStack(Items.BLAZE_POWDER));brew.prepare(ContainerStorage.of(source,Direction.UP));check(brew.plan()!=null,"water to swiftness chain resolves");check(brew.needed(new ItemStack(Items.NETHER_WART))==0,"reagent waits for bottles");check(insert(brew.inputs(),PotionContents.createItemStack(Items.POTION,Potions.WATER),1,true)==1,"starting bottle loaded");check(brew.needed(new ItemStack(Items.NETHER_WART))>0,"first-stage reagent required");check(brew.needed(new ItemStack(Items.SUGAR))==0,"later reagent waits its turn");
        reset(l);p=assign(l,Blocks.BEEHIVE);var hive=(VanillaCompanionPort)p;hive.selectHoneycomb(true);check(hive.honeycomb() && p.needed(new ItemStack(Items.GLASS_BOTTLE))==0,"honeycomb mode needs no bottles");hive.selectHoneycomb(false);check(p.needed(new ItemStack(Items.GLASS_BOTTLE))==0,"unripe hive does not stock bottles");l.setBlock(base,Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5),18);l.setBlock(base.below(),Blocks.CAMPFIRE.defaultBlockState(),18);check(p.needed(new ItemStack(Items.GLASS_BOTTLE))>0,"bottling mode requests real bottle");
        reset(l);p=assign(l,Blocks.COMPOSTER);l.setBlock(base,Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL,8),18);check(extract(p.outputs(),Items.BONE_MEAL,1,false)==1 && l.getBlockState(base).getValue(ComposterBlock.LEVEL)==8,"compost output rollback");check(extract(p.outputs(),Items.BONE_MEAL,1,true)==1 && l.getBlockState(base).getValue(ComposterBlock.LEVEL)==0,"compost commits exactly one output");
    }
    private void commerce(ServerLevel l){
        reset(l);var p=assign(l,block("farm_stand"));var stand=(FarmStandBlockEntity)l.getBlockEntity(base);stand.setOwner(l.getServer().getPlayerList().getPlayers().getFirst());stand.crates().setItem(0,new ItemStack(Items.APPLE));stand.setPrice(0,7);stand.companion.observe();stand.crates().setItem(0,ItemStack.EMPTY);
        check(p.needed(new ItemStack(Items.APPLE))==64,"farm stand remembers sold-out stock");check(insert(p.inputs(),new ItemStack(Items.APPLE),64,false)==64 && stand.crate(0).isEmpty(),"restock rollback");check(insert(p.inputs(),new ItemStack(Items.APPLE),64,true)==64 && stand.price(0)==7,"restock preserves price");check(extract(p.outputs(),Items.APPLE,64,true)==0,"sale inventory never output");
        reset(l);p=assign(l,block("crucible"));check(p.plan()==null && p.needed(new ItemStack(Items.GLASS_BOTTLE))==0,"unprogrammed crucible waits for formula");var crucible=(CrucibleCompanion)p;crucible.selectSalve(true);check(crucible.salve(),"salve selection");crucible.selectSalve(false);check(!crucible.salve(),"draught selection");
    }
    private static final String THREE_GLOW="water 3; add minecraft:glowstone_dust jugcraft:raw; add minecraft:glowstone_dust jugcraft:raw; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
    private CourierPostBlockEntity courierPost(ServerLevel l,UUID owner){l.setBlockAndUpdate(base.north(3),Couriers.COURIER_POST.defaultBlockState());var post=(CourierPostBlockEntity)l.getBlockEntity(base.north(3));post.setOwner(owner);return post;}
    private void pour(CrucibleBlockEntity c,int parts){try(var tx=Transaction.openOuter()){c.water.insert(FluidVariant.of(Fluids.WATER),parts*FluidConstants.BOTTLE,tx);tx.commit();}}
    /** Concordance Chunk C, C-06: the crucible's one demand as the Peepo port sees it: the exact shortage; nothing for a formula the rules cannot follow, which files no courier request either. */
    private void crucibleDemand(ServerLevel l){
        var owner=l.getServer().getPlayerList().getPlayers().getFirst().getUUID();var glow=new ItemStack(Items.GLOWSTONE_DUST);var ledger=CourierLedger.of(l.getServer());
        reset(l);var p=assign(l,block("crucible"));var crucible=(CrucibleBlockEntity)l.getBlockEntity(base);
        crucible.follow(Formula.parse(THREE_GLOW),owner);crucible.setItem(0,new ItemStack(Items.GLOWSTONE_DUST));
        var reagent=new ItemStack(JugcraftConcordance.REAGENT_ITEM);reagent.set(JugcraftConcordance.REAGENT,new Reagent("minecraft:glowstone_dust","jugcraft:ground"));
        check(p.needed(glow)==2 && p.needed(reagent)==0 && p.needed(new ItemStack(Items.SPIDER_EYE))==0,"the exact shortage: two plain glowstone, nothing else");
        check(insert(p.inputs(),glow,64,false)==2 && count(crucible,Items.GLOWSTONE_DUST)==1,"a probe takes two and changes nothing");
        check(insert(p.inputs(),glow,64,true)==2 && count(crucible,Items.GLOWSTONE_DUST)==3 && p.needed(glow)==0,"two delivered, then none wanted");
        reset(l);p=assign(l,block("crucible"));crucible=(CrucibleBlockEntity)l.getBlockEntity(base);courierPost(l,owner);
        crucible.follow(Formula.parse("water 1; add minecraft:glowstone_dust jugcraft:raw; add minecraft:dirt jugcraft:raw"),owner);pour(crucible,1);
        long k=ledger.ledger().nextId();
        check(crucible.incompatibility()!=null && p.needed(glow)==0 && p.status()==CompanionStatus.RECIPE_MISSING,"an incompatible formula asks for nothing");
        var refused=crucible.automate(l);
        check(refused!=null && refused.code()==Code.INCOMPATIBLE && crucible.request()==-1L && ledger.ledger().nextId()==k,"and files nothing");
    }
    /** Concordance Chunk C, C-06 (C-P50): beside the crucible's own courier request, a Peepo brings only what no courier holding it is bringing: never the same need twice, and nothing lost. */
    private void noOverSupplyWithAnOpenCourierRequest(ServerLevel l){
        var owner=l.getServer().getPlayerList().getPlayers().getFirst().getUUID();var glow=new ItemStack(Items.GLOWSTONE_DUST);var ledger=CourierLedger.of(l.getServer());
        reset(l);var p=assign(l,block("crucible"));var crucible=(CrucibleBlockEntity)l.getBlockEntity(base);courierPost(l,owner);
        crucible.follow(Formula.parse(THREE_GLOW),owner);pour(crucible,3);source.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,2));
        crucible.automate(l);var waits=crucible.automate(l);long id=crucible.request();var open=ledger.ledger().get(id);
        check(waits!=null && waits.code()==Code.MISSING_INGREDIENT && open!=null && open.ticket().wanted()==3,"it asks a courier for three");
        check(p.needed(glow)==3,"no courier holds the request yet, so the Peepo may bring all three");
        long now=l.getGameTime();var courier=new UUID(61L,1L);var barrel=ItemStorage.SIDED.find(l,base.west(6),null);
        check(ledger.claim(id,courier,now)==Logistics.Outcome.DONE && p.needed(glow)==0 && insert(p.inputs(),glow,3,true)==0,"claimed: the Peepo brings none");
        check(ledger.reserve(id,courier,Couriers.place(l,base.west(6)),barrel,now)==Logistics.Outcome.DONE
            && ledger.pickUp(id,courier,barrel,now)==Logistics.Outcome.DONE && p.needed(glow)==0,"reserved and carried: still none");
        check(ledger.deliver(id,courier,ItemStorage.SIDED.find(l,base,null),now)==Logistics.Outcome.DONE && count(crucible,Items.GLOWSTONE_DUST)==2,"two delivered");
        check(ledger.ledger().get(id)!=null && ledger.ledger().get(id).progress().worker()==null && p.needed(glow)==1,"the rest unheld: the Peepo may bring one");
        check(insert(p.inputs(),glow,64,true)==1 && count(crucible,Items.GLOWSTONE_DUST)==3,"one brought, no over-supply");
        crucible.automate(l);
        check(ledger.ledger().get(id)==null && crucible.request()==-1L && count(source,Items.GLOWSTONE_DUST)==0 && ledger.problems().isEmpty(),"satisfied, the request is turned back; nothing lost");
    }
    private void garden(ServerLevel l){
        reset(l);var soil=base.below();l.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),18);npc.assignments.assignGardenZone(soil,soil.east(2).south(2),false);var target=npc.assignments.get(1);check(target.gardenZone()!=null,"two-corner garden assigned");
        var maintenance=new GardenMaintenance(npc);maintenance.observe(target,soil);l.setBlock(soil,Blocks.DIRT.defaultBlockState(),18);check(!maintenance.repairable(target,soil),"repair off by default");
        for(var item:List.of(Items.SUGAR_CANE,Items.BAMBOO,Items.CACTUS,Items.SWEET_BERRIES,Items.GLOW_BERRIES,Items.COCOA_BEANS,Items.PUMPKIN_SEEDS,Items.MELON_SEEDS,JugcraftAgriculture.item("cranberries")))check(CompanionGardenCrops.seed(new ItemStack(item)),"special seed supported "+item);
        check(npc.assignments.toggleGardenMaintenance(1,CompanionAssignments.REPAIR),"enable farmland repair");check(maintenance.repairable(target,soil) && maintenance.repair(target,soil),"learned trampled soil repaired");check(l.getBlockState(soil).is(Blocks.FARMLAND),"repair restores farmland");check(!maintenance.repairable(target,soil.east()),"arbitrary dirt not tilled");
        npc.assignments.toggleGardenMaintenance(1,CompanionAssignments.FERTILIZE);l.setBlock(base,Blocks.WHEAT.defaultBlockState(),18);npc.belongings.setItem(0,new ItemStack(Items.BONE_MEAL,2));var feeding=new GardenMaintenance(npc);check(feeding.fertilize(target,base),"fertilizer applies native growth");check(npc.belongings.getItem(0).getCount()==1,"one fertilizer consumed");check(!feeding.ready(target) && !feeding.fertilizable(target,base),"zone and crop cooldowns enforced");
        l.setBlock(base,Blocks.PUMPKIN.defaultBlockState(),18);check(!CompanionGardenCrops.ripe(l,base,l.getBlockState(base)),"decorative pumpkins left intact");l.setBlock(base.west(),Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING,Direction.EAST),18);check(CompanionGardenCrops.ripe(l,base,l.getBlockState(base)),"attached ripe pumpkin recognized");check(GardenMaintenance.fertilizer(new ItemStack(PetroItems.FERTILIZER)),"native chemical Fertilizer supported by maintenance");
    }
    private void machines(ServerLevel l){
        for(String id:List.of("coal_generator","steam_generator","large_steam_engine")){reset(l);var p=assign(l,block(id));check(p.needed(new ItemStack(Items.COAL))==4,"fuel stock bounded "+id);check(insert(p.inputs(),new ItemStack(Items.COAL),64,false)==4,"fuel simulation "+id);check(p.needed(new ItemStack(Items.COAL))==4,"fuel rollback "+id);check(insert(p.inputs(),new ItemStack(Items.COAL),64,true)==4,"fuel committed "+id);check(p.needed(new ItemStack(Items.COAL))==0,"no overstock "+id);}
        reset(l);var p=assign(l,block("auto_crafter"));var machine=(io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity)l.getBlockEntity(base);var plank=Blocks.OAK_PLANKS.asItem();machine.setItem(0,new ItemStack(plank));machine.setItem(3,new ItemStack(plank));p.prepare(ContainerStorage.of(source,Direction.UP));check(p.needed(new ItemStack(plank))==2,"auto crafter refills seeded pattern");check(insert(p.inputs(),new ItemStack(plank),10,false)==2 && machine.getItem(0).getCount()==1,"auto crafter rollback");
        reset(l);p=assign(l,block("electroplating_bath"));source.setItem(0,new ItemStack(Items.IRON_SWORD));source.setItem(1,new ItemStack(Items.GOLD_INGOT));p.prepare(ContainerStorage.of(source,Direction.UP));check(p.needed(new ItemStack(Items.IRON_SWORD))==1 && p.needed(new ItemStack(Items.GOLD_INGOT))==1,"plating plans tool and metal");check(insert(p.inputs(),new ItemStack(Items.IRON_SWORD),1,false)==1,"plating tool probe");
    }
    private void special(ServerLevel l){
        reset(l);l.setBlock(base,IndustrialForms.ELECTROLYTIC_SEPARATOR.defaultBlockState(),18);check(CompanionAssignments.work(l,base),"industrial separator assignable");var machine=(FormMachineBlockEntity)l.getBlockEntity(base);check(machine.companionHelperCount()==2,"two industrial helpers");check(machine.companionStatus()!=CompanionStatus.READY,"incomplete structure never assists");
        for(String id:List.of("mulch_maw","gleaner")){reset(l);var p=assign(l,block(id));check(p.status()==CompanionStatus.DORMANT_GARDEN,"dormant device gated "+id);check(insert(p.inputs(),new ItemStack(Items.WHEAT),10,true)==0,"no feeding dormant device");var device=(io.github.jimbozoomer.jugcraft.concordance.garden.LivingDeviceBlockEntity)l.getBlockEntity(base);device.awaken(npc.orders.ownerId());
            if(device instanceof io.github.jimbozoomer.jugcraft.concordance.garden.MulchMawBlockEntity maw){var chaff=new ItemStack(io.github.jimbozoomer.jugcraft.concordance.garden.Garden.VERDANT_CHAFF);check(p.needed(chaff)==16,"maw caps native mulch stock");check(insert(p.inputs(),chaff,64,false)==16 && maw.food().isEmpty(),"maw rollback");check(insert(p.inputs(),chaff,64,true)==16 && maw.food().getCount()==16,"maw committed restock");}
            else {var gleaner=(io.github.jimbozoomer.jugcraft.concordance.garden.GleanerBlockEntity)device;gleaner.setItem(0,new ItemStack(Items.WHEAT,3));check(extract(p.outputs(),Items.WHEAT,3,false)==3 && gleaner.getItem(0).getCount()==3,"gleaner output rollback");check(extract(p.outputs(),Items.WHEAT,3,true)==3 && gleaner.getItem(0).isEmpty(),"gleaner output commit");}}
        reset(l);assign(l,block("bat_house"));var house=(BatHouseBlockEntity)l.getBlockEntity(base);house.set(2,5);try(var tx=Transaction.openOuter()){check(house.collectGuano(5,tx),"guano extraction probe");}check(house.guano()==5 && house.residents()==2,"guano rollback and bats preserved");try(var tx=Transaction.openOuter()){check(house.collectGuano(5,tx),"guano committed pickup");tx.commit();}check(house.guano()==0 && house.residents()==2,"only guano removed");
    }
    private void waitFor(TestServerContext server,java.util.function.Predicate<net.minecraft.server.MinecraftServer> predicate,int ticks){
        try{server.waitFor(predicate,ticks);}catch(AssertionError error){
            server.runOnServer(s->{var saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,s.overworld().registryAccess());npc.assignments.save(saved);npc.garden.save(saved);npc.transport.save(saved);
                var log=org.slf4j.LoggerFactory.getLogger("peepo-integration-test");log.error("TIMEOUT position={} energy={} status={} saved={}",npc.position(),npc.getEnergy(),npc.stationStatus(base),saved.buildResult());
                var target=npc.assignments.get(1);if(target!=null && target.garden()){var job=npc.garden.job(target.at().pos());log.error("garden status={} approach={} crop={}",job==null?null:job.workStatus(npc),job==null?null:job.approachPosition(),s.overworld().getBlockState(target.at().pos().above()));}for(var entry:Map.of("source",source,"output",output,"cargo",(Container)npc.belongings).entrySet())for(int i=0;i<entry.getValue().getContainerSize();i++)if(!entry.getValue().getItem(i).isEmpty())log.error("{}[{}]={} components={}",entry.getKey(),i,entry.getValue().getItem(i),entry.getValue().getItem(i).getComponents());
            });throw error;
        }
    }
    private void waterTrip(TestServerContext server){
        server.runOnServer(s->{var l=s.overworld();reset(l);l.setBlock(base.below(),Blocks.WATER.defaultBlockState(),18);npc.assignments.assign(l,base.below());source.setItem(0,new ItemStack(Items.GLASS_BOTTLE,3));npc.setNoAi(false);});
        waitFor(server,s->count(output,Items.POTION)==3,1400);
        server.runOnServer(s->{check(count(source,Items.GLASS_BOTTLE)==0,"three actual empty bottles consumed");check(CompanionWater.source(s.overworld(),base.below()),"bottling preserves source");check(count(npc.belongings,Items.GLASS_BOTTLE)==0,"no duplicate carried empties");for(int i=0;i<output.getContainerSize();i++)if(output.getItem(i).is(Items.POTION))check(output.getItem(i).get(DataComponents.POTION_CONTENTS).is(Potions.WATER),"actual water potion produced");});
    }
    private void waterVariants(TestServerContext server){
        for(boolean bucket:new boolean[]{false,true}){
            var item=bucket?Items.BUCKET:JugcraftAgriculture.item("gourd_canteen");var filled=bucket?Items.WATER_BUCKET:item;
            server.runOnServer(s->{reset(s.overworld());s.overworld().setBlock(base.below(),Blocks.WATER.defaultBlockState(),18);npc.assignments.assign(s.overworld(),base.below());var input=new ItemStack(item);if(!bucket){input.set(JugcraftAgriculture.CANTEEN_WATER,1);input.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Kept name"));}source.setItem(0,input);npc.setNoAi(false);});
            waitFor(server,s->count(output,filled)==1,1000);
            server.runOnServer(s->{check(count(source,item)==0,"input container consumed once");check(CompanionWater.source(s.overworld(),base.below())!=bucket,"native source consumption behavior");if(!bucket){var stack=output.getItem(0);check(GourdCanteenItem.water(stack)==GourdCanteenItem.CAPACITY && stack.getHoverName().getString().equals("Kept name"),"canteen capacity and custom name preserved");}});
        }
    }
    private void waterBank(TestServerContext server){
        var canteen=JugcraftAgriculture.item("gourd_canteen");
        server.runOnServer(s->{reset(s.overworld());s.overworld().setBlock(base.below(),Blocks.WATER.defaultBlockState(),18);npc.assignments.assign(s.overworld(),base.below());npc.belongings.setItem(0,new ItemStack(canteen));npc.snapTo(base.getX()+.5,base.getY()-.25,base.getZ()+.2,0,0);npc.setOnGround(false);npc.setNoAi(false);});
        waitFor(server,s->count(output,canteen)==1,1000);
        server.runOnServer(s->{check(GourdCanteenItem.water(output.getItem(0))==GourdCanteenItem.CAPACITY,"water-edge arrival fills and delivers container");check(CompanionWater.source(s.overworld(),base.below()),"water-edge recovery preserves source");});
    }
    private void sprinklerTrip(TestServerContext server){
        server.runOnServer(s->{reset(s.overworld());assign(s.overworld(),JugcraftFarming.SPRINKLER);source.setItem(0,new ItemStack(Items.WATER_BUCKET));source.setItem(1,new ItemStack(Items.WATER_BUCKET));source.setItem(2,new ItemStack(PetroItems.FERTILIZER,8));npc.setNoAi(false);});
        waitFor(server,s->count(output,Items.BUCKET)==2 && ((SprinklerBlockEntity)s.overworld().getBlockEntity(base)).fertilizer()==4,1400);
        server.runOnServer(s->{check(count(source,Items.WATER_BUCKET)==0,"two water buckets delivered");check(count(source,PetroItems.FERTILIZER)==4,"fertilizer supply retains excess");check(((SprinklerBlockEntity)s.overworld().getBlockEntity(base)).water().amount>0,"sprinkler actually watered");});
    }
    private void livingHarvests(TestServerContext server){
        server.runOnServer(s->{var l=s.overworld();reset(l);var p=(VanillaCompanionPort)assign(l,Blocks.BEEHIVE);p.selectHoneycomb(true);l.setBlock(base,Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5),18);check(p.hiveJob().readiness()==CompanionStatus.NEEDS_SMOKE,"hive refuses unsmoked harvest");l.setBlock(base.below(),Blocks.CAMPFIRE.defaultBlockState(),18);npc.belongings.setItem(9,new ItemStack(Items.SHEARS));npc.setNoAi(false);});
        waitFor(server,s->count(output,Items.HONEYCOMB)==3,1400);
        server.runOnServer(s->{check(s.overworld().getBlockState(base).getValue(BeehiveBlock.HONEY_LEVEL)==0,"hive honey reset after paid harvest");check(npc.belongings.getItem(9).getDamageValue()==1,"hive shears wear once");});
        server.runOnServer(s->{var l=s.overworld();reset(l);var garden=io.github.jimbozoomer.jugcraft.concordance.garden.Garden.VERDANT_BED;l.setBlock(base.below(),garden.defaultBlockState(),18);var bed=(io.github.jimbozoomer.jugcraft.concordance.garden.VerdantBedBlockEntity)l.getBlockEntity(base.below());bed.awaken(npc.orders.ownerId());var crop=(io.github.jimbozoomer.jugcraft.concordance.garden.OrganismCropBlock)io.github.jimbozoomer.jugcraft.concordance.garden.Garden.SUNPETAL_CROP;l.setBlock(base,crop.getStateForAge(crop.getMaxAge()),18);npc.assignments.assignGardenZone(base.below(),base.below(),false);npc.setNoAi(false);});
        waitFor(server,s->count(output,io.github.jimbozoomer.jugcraft.concordance.garden.Garden.SUNPETAL)>0,1400);
        server.runOnServer(s->{var state=s.overworld().getBlockState(base);check(state.is(io.github.jimbozoomer.jugcraft.concordance.garden.Garden.SUNPETAL_CROP) && !((CropBlock)state.getBlock()).isMaxAge(state),"magic crop retained at regrowth stage");});
    }
    private void brewingTrip(TestServerContext server){
        server.runOnServer(s->{reset(s.overworld());var p=(VanillaCompanionPort)assign(s.overworld(),Blocks.BREWING_STAND);p.filter.set(0,PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS));p.filterChanged();source.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));source.setItem(1,new ItemStack(Items.NETHER_WART));source.setItem(2,new ItemStack(Items.SUGAR));source.setItem(3,new ItemStack(Items.BLAZE_POWDER));npc.setNoAi(false);});
        waitFor(server,s->count(output,Items.POTION)==1,2200);
        server.runOnServer(s->{check(output.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.SWIFTNESS),"final brewed potion delivered, not intermediate");check(count(source,Items.NETHER_WART)+count(source,Items.SUGAR)==0,"both chain reagents consumed");});
    }
    private void industrialTrip(TestServerContext server){
        FormMachineBlockEntity[] machine={null};PeepoEntity[] second={null};
        try{
            server.runOnServer(s->{var l=s.overworld();reset(l);var b=IndustrialForms.ELECTROLYTIC_SEPARATOR;l.setBlock(base,b.defaultBlockState(),18);b.setPlacedBy(l,base,l.getBlockState(base),null,ItemStack.EMPTY);machine[0]=(FormMachineBlockEntity)l.getBlockEntity(base);
                ((io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage)machine[0].energy()).setAmount(machine[0].energy().getCapacity());try(var tx=Transaction.openOuter()){check(machine[0].tanks().input(0).insert(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),FluidConstants.BUCKET,tx)==FluidConstants.BUCKET,"industrial water input");tx.commit();}
                npc.assignments.assign(l,base);npc.snapTo(base.getX()-.5,base.getY(),base.getZ()-2.5,0,0);npc.setNoAi(false);var owner=l.getServer().getPlayerList().getPlayers().getFirst();second[0]=PeepoMod.JUGHEAD.create(l,EntitySpawnReason.COMMAND);second[0].snapTo(base.getX()+.5,base.getY(),base.getZ()-2.5,0,0);l.addFreshEntity(second[0]);second[0].orders.tame(owner);second[0].orders.command(owner,3);second[0].preferences.social=false;second[0].preferences.carryMeals=0;second[0].assignments.assign(l,base);
            });
            waitFor(server,s->{((io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage)machine[0].energy()).setAmount(machine[0].energy().getCapacity());return npc.stationStatus(base)==CompanionStatus.WORKING && second[0].stationStatus(base)==CompanionStatus.WORKING && machine[0].lane(0)!=null && machine[0].lane(0).progress()>30;},800);
            long[] start=server.computeOnServer(s->new long[]{s.overworld().getGameTime(),machine[0].lane(0).progress(),machine[0].energy().getAmount(),npc.getEnergy(),second[0].getEnergy()});
            waitFor(server,s->s.overworld().getGameTime()>=start[0]+40,100);
            server.runOnServer(s->{long ticks=s.overworld().getGameTime()-start[0],work=machine[0].lane(0).progress()-start[1];check(work>=ticks*1.5-2 && work<=ticks*1.5+2,"two helpers add combined 50 percent progress: ticks="+ticks+" work="+work+" energy="+machine[0].energy().getAmount()+" status="+machine[0].companionStatus()+" drains="+(start[3]-npc.getEnergy())+","+(start[4]-second[0].getEnergy()));check(start[2]-machine[0].energy().getAmount()==work*256,"bonus production pays normal electricity");check(Math.abs((start[3]-npc.getEnergy())-ticks*16)<=32,"Peepo pays machine-based 16 JE per tick");check(Math.abs((start[4]-second[0].getEnergy())-ticks*16)<=32,"Jughead pays machine-based 16 JE per tick");});
        }finally{server.runOnServer(s->{if(second[0]!=null){second[0].resetCompanionRoutine();second[0].discard();}});}
    }
    private void wetCrops(TestServerContext server){
        for(boolean rice:new boolean[]{false,true}){
            var crop=rice?block("rice_crop"):block("cranberry_bush");var produce=JugcraftAgriculture.item(rice?"rice_panicle":"cranberries");
            server.runOnServer(s->{var l=s.overworld();reset(l);l.setBlock(base.below(2),Blocks.DIRT.defaultBlockState(),18);
                if(rice){var tall=(TallCropBlock)crop;for(int i=0;i<tall.crop().height(7);i++)l.setBlock(base.below().above(i),tall.stateFor(7,i),18);}else l.setBlock(base.below(),crop.defaultBlockState().setValue(CranberryBushBlock.AGE,3),18);
                npc.assignments.assignGardenZone(base.below(2),base.below(2),false);npc.setNoAi(false);
            });
            waitFor(server,s->count(output,produce)>0,1400);
            server.runOnServer(s->{check(s.overworld().getBlockState(base.below()).is(crop),"wet crop remains planted");check(s.overworld().getFluidState(base.below()).isSource(),"harvesting preserves paddy water");check(rice?!TallCropBlock.isRipe(s.overworld().getBlockState(base.below())):!CranberryBushBlock.isRipe(s.overworld().getBlockState(base.below())),"wet crop resets to regrow");});
        }
    }
    private void spinningTrip(TestServerContext server){
        server.runOnServer(s->{reset(s.overworld());assign(s.overworld(),block("spinning_wheel"));source.setItem(0,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool"))));npc.setNoAi(false);});
        waitFor(server,s->{for(int i=0;i<output.getContainerSize();i++)if(!output.getItem(i).isEmpty())return true;return false;},1600);
        server.runOnServer(s->{check(count(source,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool")))==0,"spinning consumes supplied wool");check(count(npc.belongings,BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool")))==0,"spinning finished its delivery");});
    }
}
