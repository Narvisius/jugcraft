package io.github.jimbozoomer.jugcraft.machine;
import java.util.*;
import local.peepo.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.CrafterBlock;
/** Small bounded input plans for machines whose recipe is procedural or a player-seeded grid. */
final class MachineSpecialSupply {
 private final MachineBlockEntity machine;private final CompanionRecipeFilter filter;
 private ItemStack tool=ItemStack.EMPTY,metal=ItemStack.EMPTY;
 MachineSpecialSupply(MachineBlockEntity machine,CompanionRecipeFilter filter){this.machine=machine;this.filter=filter;}
 boolean applies(){return machine.kind().solidFuelCompanion() || recipeCapable();}
 boolean recipeCapable(){return machine.kind()==MachineKind.AUTO_CRAFTER || machine.kind()==MachineKind.ELECTROPLATING_BATH;}
 Identifier plan(){return Identifier.fromNamespaceAndPath("jugcraft",machine.kind().id);}
 private CraftingInput grid(){var items=new ArrayList<ItemStack>();for(int i=0;i<9;i++)items.add(machine.getItem(i).copyWithCount(1));return CraftingInput.of(3,3,items);}
 private ItemStack craftResult(){if(!(machine.getLevel() instanceof ServerLevel level))return ItemStack.EMPTY;var grid=grid();return CrafterBlock.getPotentialResults(level,grid).map(r->r.value().assemble(grid)).orElse(ItemStack.EMPTY);}
 boolean acceptsFilter(ItemStack item){return item.isEmpty() || machine.kind()==MachineKind.ELECTROPLATING_BATH && Electroplating.platable(item) || machine.kind()==MachineKind.AUTO_CRAFTER && craftResult().is(item.getItem());}
 void prepare(Storage<ItemVariant> source){
  prepare(source,null);
 }
 MachineCompanionPort.Plan stockPlan(){
  if(machine.kind()!=MachineKind.ELECTROPLATING_BATH || tool.isEmpty() || metal.isEmpty())return null;
  var result=Electroplating.plate(tool,Electroplating.metalOf(metal),machine.getLevel().registryAccess());
  return result.isEmpty()?null:new MachineCompanionPort.Plan(plan(),List.of(new MachineCompanionPort.Part(Ingredient.of(tool.getItem()),1),new MachineCompanionPort.Part(Ingredient.of(metal.getItem()),1)),List.of(result),true,null);
 }
 void prepare(Storage<ItemVariant> source,java.util.function.ToDoubleFunction<ItemStack> priority){
  if(machine.kind()!=MachineKind.ELECTROPLATING_BATH || !(machine.getLevel() instanceof ServerLevel))return;
  tool=ItemStack.EMPTY;metal=ItemStack.EMPTY;var available=RecipeSupplies.available(source);
  var tools=new ArrayList<>(machine.getItem(0).isEmpty()?available:List.of(machine.getItem(0)));var metals=machine.getItem(1).isEmpty()?available:List.of(machine.getItem(1));
  if(priority!=null)tools.sort(Comparator.comparingDouble(priority).reversed());
  var coatings=new LinkedHashMap<String,ItemStack>();for(var ingot:metals){var type=Electroplating.metalOf(ingot);if(type!=null)coatings.putIfAbsent(type,ingot);}
  // At most three coating types per bounded supply snapshot, regardless of chest stack count.
  for(var candidate:tools){if(!Electroplating.platable(candidate) || !filter.allows(candidate) || priority!=null && priority.applyAsDouble(candidate)<=-2)continue;
   for(var ingot:coatings.values()){var coating=Electroplating.metalOf(ingot);if(coating==null || coating.equals(candidate.get(Electroplating.PLATING)) && !candidate.isDamaged())continue;
    if(!Electroplating.plate(candidate,coating,machine.getLevel().registryAccess()).isEmpty()){tool=candidate.copyWithCount(1);metal=ingot.copyWithCount(1);return;}
   }
  }
 }
 private int missing(int slot,ItemStack item,int cap){var held=machine.getItem(slot);if(!held.isEmpty() && !ItemStack.isSameItemSameComponents(held,item))return 0;
  for(var face:net.minecraft.core.Direction.values())for(int exposed:machine.getSlotsForFace(face))if(exposed==slot && machine.canPlaceItemThroughFace(slot,item,face))return Math.max(0,Math.min(cap,item.getMaxStackSize())-held.getCount());return 0;
 }
 int[] needs(ItemStack item){int[] need=new int[machine.kind().slots];if(machine.isRemoved() || machine.isLocked() || item.isEmpty())return need;
  var kind=machine.kind();
  if(kind.solidFuelCompanion()){
   var energy=machine.energyFor(null);if(energy!=null && energy.getCapacity()>0 && energy.getAmount()>=energy.getCapacity())return need;
   if((kind.isBoiler()?GeneratorFuels.steamBurnTicks(item):GeneratorFuels.burnTicks(item))>0)need[0]=missing(0,item,4);
   if(kind.isBoiler() && item.is(Items.WATER_BUCKET) && machine.companionTankAmount()<=kind.tankCapacity()-1000 && (machine.getItem(2).isEmpty() || machine.getItem(2).is(Items.BUCKET) && machine.getItem(2).getCount()<machine.getItem(2).getMaxStackSize()))need[1]=missing(1,item,1);
  }else if(kind==MachineKind.AUTO_CRAFTER){
   var result=craftResult();if(result.isEmpty() || !filter.allows(result))return need;
   for(int i=0;i<9;i++)if(!machine.getItem(i).isEmpty() && ItemStack.isSameItemSameComponents(machine.getItem(i),item))need[i]=missing(i,item,2);
  }else if(kind==MachineKind.ELECTROPLATING_BATH && filter.allows(tool)){
   if(ItemStack.isSameItemSameComponents(tool,item))need[0]=missing(0,item,1);
   if(ItemStack.isSameItemSameComponents(metal,item))need[1]=missing(1,item,1);
  }
  return need;
 }
}
