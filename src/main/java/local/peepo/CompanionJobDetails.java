package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import net.minecraft.world.item.ItemStack;

/** Read-only explanations. No source inventory, entity, recipe-catalog or path searches. */
public final class CompanionJobDetails {
    private CompanionJobDetails(){}
    public static String describe(PeepoEntity npc,int row,CompanionStatus status){
        var target=npc.assignments.get(row);
        if(target==null)return "Select a workstation or zone with the Planner.";
        if(!target.local(npc.level()))return "In "+target.at().dimension().identifier()+"; work requires the same dimension.";
        if(!npc.level().hasChunkAt(target.at().pos()))return "Area unloaded; work resumes when it is loaded.";
        if(!target.present(npc.level()))return "Assigned target missing or replaced; reassign with the Planner.";
        if(!CompanionJobs.permitted(npc,target.at().pos()) || status==CompanionStatus.FORBIDDEN)return "Access denied; check ownership, party access and protection.";
        String delivery=npc.transport.jobDetail(target);
        if(!delivery.isEmpty())return delivery;
        String source=containers(npc,target,false),output=containers(npc,target,true);
        String tool=npc.assignments.supplies.handLocked()?"Unlock Hand to fetch a tool, or equip it manually.":"Equip it or enable Tools on a stocked Supply.";
        return switch(status){
            case NO_INPUT, SUPPLY_EMPTY, RECIPE_MISSING -> input(npc,row,target,status,source);
            case FULL -> target.block().getPath().equals("generator_wheel")?"Wheel energy buffer full; use power from its side outputs.":
                target.block().getPath().equals("hand_crank")?"Kinetic network is not accepting power; check flywheel capacity and connections.":
                target.carriesProduce()?"Harvest/cargo capacity full; "+transport(npc,row,false,output)+".":
                "Output needs collecting; "+transport(npc,row,false,output)+".";
            case BLOCKED -> "Cannot reach the work position; clear a safe path and standing space.";
            case NO_TOOL -> "Needs a knife. "+tool;
            case NEEDS_SHEARS -> "Needs shears. "+tool;
            case NEEDS_CUTTER -> "Needs shears or a knife. "+tool;
            case NEEDS_HOE -> "Needs a hoe for farmland repair. "+tool;
            case NEEDS_WEAPON -> "Needs a sword or axe for slaughter. "+tool;
            case NO_BUCKETS -> "Needs empty buckets; "+transport(npc,row,true,source)+".";
            case NEEDS_BOTTLES -> "Needs empty glass bottles; "+transport(npc,row,true,source)+".";
            case NEEDS_CONTAINERS -> "Needs empty bottles, buckets or a refillable canteen; "+transport(npc,row,true,source)+".";
            case NO_SEEDS -> "Needs suitable planting items; "+transport(npc,row,true,source)+".";
            case NO_BUNDLES -> "Needs ready-made herb bundles; "+transport(npc,row,true,source)+".";
            case NEEDS_FERTILIZER -> "Needs bone meal, guano or Fertilizer; "+transport(npc,row,true,source)+".";
            case NEEDS_BREEDING_FOOD -> "Needs food accepted by the selected animals; "+transport(npc,row,true,source)+".";
            case NO_POWER -> "Machine lacks power; check its energy supply and connections.";
            case NO_HEAT -> "Station lacks heat; check fuel or the heat source below it.";
            case NEEDS_WATER -> "Needs water through the station's native fluid input.";
            case NO_WATER_SOURCE -> "No usable source water; restore it and leave a safe dry bank.";
            case WRONG_TEMPERATURE -> "Temperature does not match the recipe; adjust heat/cooling.";
            case REDSTONE_DISABLED -> "Redstone prevents work; check the station's redstone control.";
            case OCCUPIED -> "All helper positions are occupied; waiting for one to become free.";
            case WAITING -> "Queued for shared server work/path budget; retries automatically.";
            case RECOVERING -> "Recovering energy; resumes at "+npc.preferences.resumeAt+"%.";
            case SCHEDULED_REST -> "Outside the selected shift; change the schedule under Routine.";
            case GROWING -> "No harvest ready; waiting for crops to grow naturally.";
            case ANIMALS_WAIT -> "No eligible animal ready in the zone; check adults and cooldowns.";
            case EGGS_WAIT -> "No dropped eggs ready inside the assigned area.";
            case POPULATION_SETUP -> "Open Zone options > Population and add animal types.";
            case POPULATION_LIMIT -> "Population settings are satisfied; waiting for numbers to change.";
            case POPULATION_BUSY -> "Too many animals to safely manage; split or reduce this pen.";
            case NEEDS_SMOKE -> "Place a smoking campfire below the hive with a clear smoke path.";
            case HONEY_GROWING -> "Hive is not ready to harvest; waiting for honey.";
            case AGING -> "Waiting for the selected cider stage; check Filter for Sparkling/Aged.";
            case DRYING -> "Herb bundles are drying; collection begins when ready.";
            case COOLING, CANDY_SETTING -> "Waiting for the station's cooling/setting step to finish.";
            case GUANO_WAIT -> "No guano ready; resident bats produce it naturally.";
            case STOCK_SETUP -> "Stock the Farm Stand sale slots first to set their restock items.";
            case SERVING_SETUP -> "Serve the desired dishes manually once to set restock items.";
            case MACHINE_UNFORMED -> "Complete the machine structure and keep all its parts loaded.";
            case MACHINE_TOOL -> "Install the required tool in the machine's tool socket.";
            case MACHINE_PAUSED -> "Machine is paused; resume it using its own controls.";
            case DORMANT_GARDEN -> "Awaken the assigned magic bed/device through its native progression.";
            case NEEDS_VERDANCE -> "Magic device lacks Verdance; check its native supply.";
            case MAGIC_DISABLED, BAT_HOUSE_DISABLED, GARDEN_DISABLED -> "Disabled by server/game rules; this job cannot run until enabled.";
            case SPOILED_INPUT -> "Remove spoiled jars and supply fresh ingredients.";
            case UNSUPPORTED -> "This target has no supported companion job; reassign it.";
            case READY -> npc.orders.mode()!=3?"Ready; select Work to perform assigned jobs.":"Ready; follows job priority, energy and Routine settings.";
            case NONE -> "Checking workstation readiness...";
            case WORKING -> "Working at this station.";
            case TRAVELLING -> "Walking to the reserved work position.";
            default -> status.label+"; check this job's settings and linked containers.";
        };
    }
    private static String input(PeepoEntity npc,int row,CompanionAssignments.Target target,CompanionStatus status,String source){
        if(target.block().getPath().equals("hand_crank"))return "No kinetic demand; connect the crank to a machine or flywheel that can accept power.";
        String missing="No complete allowed recipe is ready; check ingredients and Filter";
        var block=npc.level().getBlockEntity(target.at().pos());
        if(block instanceof CookingPotBlockEntity pot && !pot.isLocked()){
            var plan=pot.supplyPlan().orElse(null);
            if(plan!=null){
                var installed=new ArrayList<ItemStack>();for(int i=0;i<CookingPotBlockEntity.INPUTS;i++)installed.add(pot.getItem(i));
                var parts=plan.ingredients().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList();
                String needs=missingIngredients(parts,installed);if(!needs.isEmpty())missing="Needs "+needs+" at the pot";
            }
        }else if(block instanceof io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity machine && !machine.isLocked()){
            var plan=machine.companionPort.selection();
            if(plan!=null){
                var installed=new ArrayList<ItemStack>();for(int i=0;i<machine.kind().outputSlot();i++)installed.add(machine.getItem(i));
                var parts=plan.parts().stream().map(p->new RecipeSupplies.Part(p.ingredient(),p.count())).toList();
                String needs=missingIngredients(parts,installed);if(!needs.isEmpty())missing="Needs "+needs+" at the machine";
            }
        }
        return missing+"; "+transport(npc,row,true,source)+".";
    }
    /** Count station deficits once, consuming each installed stack only once. Alternatives stay explicit. */
    static String missingIngredients(List<RecipeSupplies.Part> parts,List<ItemStack> installed){
        var stock=installed.stream().map(ItemStack::copy).toList();
        var ordered=new ArrayList<>(parts);ordered.sort(Comparator.comparingLong(p->stock.stream().filter(p.ingredient()::test).count()));
        var needs=new ArrayList<String>();int more=0;
        for(var part:ordered){
            int left=part.count();for(var stack:stock)if(left>0 && part.ingredient().test(stack)){int take=Math.min(left,stack.getCount());stack.shrink(take);left-=take;}
            if(left<=0)continue;
            if(needs.size()>=2){more++;continue;}
            var examples=part.ingredient().items().limit(2).toList();
            String name=examples.isEmpty()?"matching ingredient":new ItemStack(examples.getFirst()).getHoverName().getString();
            if(examples.size()>1)name+=" (or equivalent)";
            needs.add(left+" "+name);
        }
        return String.join(", ",needs)+(more>0?" + "+more+" other ingredient(s)":"");
    }
    static String containers(PeepoEntity npc,CompanionAssignments.Target target,boolean output){
        var labels=new ArrayList<String>();int mask=npc.assignments.supplies.mask(npc.assignments.workPriority(target.at().pos()),output);
        for(int i=0;i<4;i++)if((mask==0 || (mask&(1<<i))!=0) && npc.assignments.get(CompanionAssignments.containerSlot(output,i))!=null)labels.add(Integer.toString(i+1));
        return labels.isEmpty()?"":(output?"Output ":"Supply ")+String.join(", ",labels);
    }
    private static String transport(PeepoEntity npc,int row,boolean supply,String containers){
        int reason=npc.assignments.transportDisplay(row,supply)>>2;
        if(reason==1)return (supply?"Supply":"Output")+" is Off in this job";
        if(reason==2)return "Auto leaves "+(supply?"input":"output")+" to external automation";
        if(reason==3)return "use the station's native "+(supply?"inputs":"outputs");
        if(containers.isEmpty())return "link an "+(supply?"assigned Supply":"assigned Output")+" in Supplies > Job links";
        return (supply?"check ":"clear room in ")+containers;
    }
}
