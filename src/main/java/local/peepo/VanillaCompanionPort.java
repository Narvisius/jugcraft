package local.peepo;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.*;
import io.github.jimbozoomer.jugcraft.mixin.CompanionCampfireAccessor;

/** Vanilla processing remains authoritative. This port handles physical supplies and finished items only. */
public final class VanillaCompanionPort implements CompanionLogistics.Port {
    private static final Map<RecipeType<?>,List<RecipeHolder<?>>> CATALOG=new HashMap<>();
    private static final Map<Identifier,Recipe<?>> RECIPES=new HashMap<>();
    public static void clearCatalog(){CATALOG.clear();RECIPES.clear();CompanionBrewingChain.clearCache();}
    public static void initialize(){net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{
        if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty() || player.isSpectator())return net.minecraft.world.InteractionResult.PASS;
        if(!(level.getBlockEntity(hit.getBlockPos()) instanceof VanillaCompanionAccess access))return net.minecraft.world.InteractionResult.PASS;
        var port=access.peepo$existingPort();if(port==null || !port.hasOutput() || !level.mayInteract(player,hit.getBlockPos()) || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,level,hit.getBlockPos()))return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide() && port.live())port.recover(player);return net.minecraft.world.InteractionResult.SUCCESS;
    });}
    public static VanillaCompanionPort of(BlockEntity block){return block instanceof VanillaCompanionAccess access?access.peepo$port():null;}
    public static boolean supports(BlockEntity b){return b instanceof AbstractFurnaceBlockEntity || b instanceof BrewingStandBlockEntity || b instanceof CampfireBlockEntity || b instanceof CrafterBlockEntity || b instanceof BeehiveBlockEntity;}
    private final BlockEntity block;
    private final CompanionBrewingChain brewing;
    private HiveCompanionJob hiveJob;
    private int hiveBottles;
    public HiveCompanionJob hiveJob(){if(hiveJob==null)hiveJob=new HiveCompanionJob((BeehiveBlockEntity)block,this);return hiveJob;}
    public boolean honeycomb(){return filter.allows(new ItemStack(Items.HONEYCOMB)) && !filter.allows(new ItemStack(Items.HONEY_BOTTLE));}
    public void selectHoneycomb(boolean comb){for(int i=0;i<9;i++)filter.set(i,ItemStack.EMPTY);filter.set(0,new ItemStack(comb?Items.HONEYCOMB:Items.HONEY_BOTTLE));filterChanged();}
    int hiveBottles(){return hiveBottles;}
    boolean harvest(List<ItemStack> products,boolean apply){if(!buffer(products,apply))return false;if(apply){managed=true;if(!honeycomb())hiveBottles--;changed();}return true;}
    public final CompanionRecipeFilter filter;
    private Identifier selected;
    private int scanCursor;
    private final ItemStack[] output=new ItemStack[12],pattern=new ItemStack[9];
    private boolean managed;
    private record State(List<ItemStack> out,List<ItemStack> fire,int[] progress,int[] time,boolean managed,int bottles){}
    private final SnapshotParticipant<State> snapshot=new SnapshotParticipant<>(){
        protected State createSnapshot(){
            var fire=block instanceof CampfireBlockEntity c?c:null;var access=fire==null?null:(CompanionCampfireAccessor)fire;
            return new State(Arrays.stream(output).map(ItemStack::copy).toList(),fire==null?List.of():fire.getItems().stream().map(ItemStack::copy).toList(),
                access==null?null:access.peepo$progress().clone(),access==null?null:access.peepo$times().clone(),managed,hiveBottles);
        }
        protected void readSnapshot(State s){
            for(int i=0;i<output.length;i++)output[i]=s.out.get(i).copy();managed=s.managed;hiveBottles=s.bottles;
            if(block instanceof CampfireBlockEntity fire){var access=(CompanionCampfireAccessor)fire;
                for(int i=0;i<4;i++)fire.getItems().set(i,s.fire.get(i).copy());
                System.arraycopy(s.progress,0,access.peepo$progress(),0,4);System.arraycopy(s.time,0,access.peepo$times(),0,4);
            }
        }
        protected void onFinalCommit(){if(brewing!=null)brewing.transferred();changed();}
    };
    public VanillaCompanionPort(BlockEntity block){this.block=block;filter=new CompanionRecipeFilter(block instanceof BrewingStandBlockEntity);Arrays.fill(output,ItemStack.EMPTY);Arrays.fill(pattern,ItemStack.EMPTY);brewing=block instanceof BrewingStandBlockEntity b?new CompanionBrewingChain(b,filter,this::changed):null;}
    private ServerLevel level(){return (ServerLevel)block.getLevel();}
    private boolean live(){return !block.isRemoved() && block.getLevel() instanceof ServerLevel && (!(block instanceof BaseContainerBlockEntity c) || !c.isLocked());}
    private void changed(){block.setChanged();CompanionReadiness.invalidate(block);if(block.getLevel()!=null)block.getLevel().sendBlockUpdated(block.getBlockPos(),block.getBlockState(),block.getBlockState(),2);}
    private RecipeType<?> type(){return block instanceof BrewingStandBlockEntity?RecipeType.BREWING:block instanceof CampfireBlockEntity?RecipeType.CAMPFIRE_COOKING:block instanceof CrafterBlockEntity?RecipeType.CRAFTING:block instanceof SmokerBlockEntity?RecipeType.SMOKING:block instanceof BlastFurnaceBlockEntity?RecipeType.BLASTING:RecipeType.SMELTING;}
    private List<RecipeHolder<?>> catalog(){
        if(!(block.getLevel() instanceof ServerLevel))return List.of();
        return CATALOG.computeIfAbsent(type(),type->level().getServer().getRecipeManager().getRecipes().stream().filter(r->r.value().getType()==type).peek(r->RECIPES.put(r.id().identifier(),r.value())).sorted(Comparator.comparing(r->r.id().identifier().toString())).toList());
    }
    private Recipe<?> selection(){if(selected==null)return null;catalog();return RECIPES.get(selected);}
    private static ItemStack result(Recipe<?> recipe){return recipe instanceof AbstractCookingRecipe c?c.assemble(new SingleRecipeInput(ItemStack.EMPTY)):recipe instanceof BrewingRecipe b?b.getOutput().create():ItemStack.EMPTY;}
    public boolean acceptsFilter(ItemStack stack){
        if(stack.isEmpty())return true;
        if(block instanceof BeehiveBlockEntity)return stack.is(Items.HONEY_BOTTLE) || stack.is(Items.HONEYCOMB);
        if(block instanceof CrafterBlockEntity c){var r=CrafterBlock.getPotentialResults(level(),c.asCraftInput());return r.isPresent() && r.get().value().assemble(c.asCraftInput()).is(stack.getItem());}
        return catalog().stream().anyMatch(r->{var out=result(r.value());return out.is(stack.getItem()) && (!(block instanceof BrewingStandBlockEntity) || Objects.equals(out.get(DataComponents.POTION_CONTENTS),stack.get(DataComponents.POTION_CONTENTS)));});
    }
    public void filterChanged(){if(brewing!=null)brewing.filterChanged();if(!(block instanceof CrafterBlockEntity) && (!(block instanceof BrewingStandBlockEntity c) || c.isEmpty()))selected=null;changed();}
    public Identifier plan(){return block instanceof BeehiveBlockEntity?PeepoMod.id("hive_harvest"):brewing!=null?brewing.plan():selected;}
    public void prepare(Storage<ItemVariant> source){
        if(!live())return;
        if(block instanceof BeehiveBlockEntity)return;
        if(brewing!=null){brewing.prepare(source);return;}
        if(block instanceof CrafterBlockEntity crafter){prepareCrafter(crafter);return;}
        if(block instanceof CampfireBlockEntity fire && selected!=null && fire.getItems().stream().anyMatch(i->!i.isEmpty()))return;
        var supplies=RecipeSupplies.available(source);var installed=block instanceof AbstractFurnaceBlockEntity f?f.getItem(0):ItemStack.EMPTY;
        if(fits(selection(),supplies,installed))return;
        var catalog=catalog();int checks=Math.min(256,catalog.size());
        for(int scan=0;scan<checks;scan++){
            var holder=catalog.get(Math.floorMod(scanCursor++,catalog.size()));
            if(fits(holder.value(),supplies,installed)){if(!holder.id().identifier().equals(selected)){selected=holder.id().identifier();changed();}return;}
        }
        if(installed.isEmpty() && !(block instanceof BrewingStandBlockEntity))selected=null;
    }
    private boolean fits(Recipe<?> recipe,List<ItemStack> supplies,ItemStack installed){
        if(recipe==null || !filter.allows(result(recipe)))return false;
        if(recipe instanceof AbstractCookingRecipe c)return (installed.isEmpty() || c.input().test(installed)) && (!installed.isEmpty() || supplies.stream().anyMatch(c.input()::test));
        return false;
    }
    private void prepareCrafter(CrafterBlockEntity crafter){
        boolean mismatch=false;for(int i=0;i<9;i++)if(crafter.isSlotDisabled(i) && !pattern[i].isEmpty() || !crafter.getItem(i).isEmpty() && !ItemStack.isSameItemSameComponents(crafter.getItem(i),pattern[i]))mismatch=true;
        var recipe=CrafterBlock.getPotentialResults(level(),crafter.asCraftInput());
        if(mismatch || selected==null){
            selected=null;Arrays.fill(pattern,ItemStack.EMPTY);
            if(recipe.isPresent() && filter.allows(recipe.get().value().assemble(crafter.asCraftInput()))){
                for(int i=0;i<9;i++)pattern[i]=crafter.getItem(i).copyWithCount(1);
                selected=recipe.get().id().identifier();managed=true;changed();
            }
        }
    }
    private int missing(Container c,int slot,ItemStack item,int cap){var held=c.getItem(slot);return (held.isEmpty() || ItemStack.isSameItemSameComponents(held,item)) && c.canPlaceItem(slot,item)?Math.max(0,Math.min(cap,item.getMaxStackSize())-held.getCount()):0;}
    private int[] needs(ItemStack item){
        if(brewing!=null)return live()?brewing.needs(item):new int[9];
        int[] need=new int[9];if(!live() || selected==null)return need;var recipe=block instanceof CrafterBlockEntity?null:selection();
        if(block instanceof AbstractFurnaceBlockEntity furnace && recipe instanceof AbstractCookingRecipe cook){
            if(filter.allows(result(recipe)) && cook.input().test(item))need[0]=missing(furnace,0,item,8);
            if(item.has(DataComponents.COOKING_FUEL) && !furnace.getItem(0).isEmpty())need[1]=missing(furnace,1,item,1);
        }else if(block instanceof CrafterBlockEntity crafter){
            var input=CraftingInput.of(3,3,Arrays.asList(pattern));var expected=CrafterBlock.getPotentialResults(level(),input);
            if(expected.isEmpty() || !expected.get().id().identifier().equals(selected) || !filter.allows(expected.get().value().assemble(input)))return need;
            for(int i=0;i<9;i++)if(!crafter.isSlotDisabled(i) && !pattern[i].isEmpty() && ItemStack.isSameItemSameComponents(pattern[i],item))need[i]=missing(crafter,i,item,2);
        }
        return need;
    }
    public int needed(ItemStack item){
        if(block instanceof BeehiveBlockEntity)return live() && !honeycomb() && item.is(Items.GLASS_BOTTLE) && HiveCompanionJob.ready(block)?Math.max(0,4-hiveBottles):0;
        if(block instanceof CampfireBlockEntity fire){
            if(!live() || hasOutput() || !fire.getBlockState().getValue(CampfireBlock.LIT) || !(selection() instanceof CampfireCookingRecipe recipe) || !filter.allows(result(recipe)) || !recipe.input().test(item))return 0;
            return (int)fire.getItems().stream().filter(ItemStack::isEmpty).count();
        }
        return Arrays.stream(needs(item)).sum();
    }
    public Storage<ItemVariant> inputs(){return new InsertionOnlyStorage<>(){
        public long insert(ItemVariant item,long amount,TransactionContext tx){
            StoragePreconditions.notBlankNotNegative(item,amount);if(amount==0 || !live())return 0;var stack=item.toStack(1);long moved=0;
            if(block instanceof BeehiveBlockEntity){int count=(int)Math.min(amount,needed(stack));if(count>0){snapshot.updateSnapshots(tx);hiveBottles+=count;managed=true;}return count;}
            if(block instanceof CampfireBlockEntity fire){
                int count=(int)Math.min(amount,needed(stack));if(count==0)return 0;var recipe=(CampfireCookingRecipe)selection();var access=(CompanionCampfireAccessor)fire;
                snapshot.updateSnapshots(tx);managed=true;
                for(int i=0;i<4 && moved<count;i++)if(fire.getItems().get(i).isEmpty()){fire.getItems().set(i,stack.copy());access.peepo$progress()[i]=0;access.peepo$times()[i]=recipe.cookingTime();moved++;}
            }else if(block instanceof Container c){
                var inventory=ContainerStorage.of(c,null);var needs=needs(stack);
                for(int i=0;i<c.getContainerSize() && i<needs.length && moved<amount;i++)if(needs[i]>0)moved+=inventory.getSlot(i).insert(item,Math.min(amount-moved,needs[i]),tx);
            }
            if(brewing!=null && moved>0)snapshot.updateSnapshots(tx);
            return moved;
        }
    };}
    private Storage<ItemVariant> extractSlot(Container c,int slot,java.util.function.BooleanSupplier allowed){return new SingleSlotStorage<>(){
        public long insert(ItemVariant v,long n,TransactionContext tx){return 0;}
        public boolean supportsInsertion(){return false;}
        public long extract(ItemVariant v,long n,TransactionContext tx){return live() && allowed.getAsBoolean()?ContainerStorage.of(c,null).getSlot(slot).extract(v,n,tx):0;}
        public ItemVariant getResource(){return allowed.getAsBoolean()?ItemVariant.of(c.getItem(slot)):ItemVariant.blank();}
        public boolean isResourceBlank(){return getResource().isBlank();}
        public long getAmount(){return allowed.getAsBoolean()?c.getItem(slot).getCount():0;}
        public long getCapacity(){return 64;}
    };}
    public Storage<ItemVariant> outputs(){
        if(block instanceof AbstractFurnaceBlockEntity f)return new CombinedStorage<>(List.of(extractSlot(f,2,()->true),extractSlot(f,1,()->!f.getItem(1).has(DataComponents.COOKING_FUEL) && f.canTakeItemThroughFace(1,f.getItem(1),Direction.DOWN))));
        if(block instanceof BrewingStandBlockEntity b){
            var out=new ArrayList<Storage<ItemVariant>>();for(int i=0;i<3;i++){final int slot=i;out.add(extractSlot(b,i,()->brewing.output(slot)));}
            out.add(extractSlot(b,4,()->!b.getItem(4).isEmpty() && !b.getItem(4).has(DataComponents.BREWING_FUEL)));
            out.add(extractSlot(b,3,()->brewing.reagentOutput()));return new CombinedStorage<>(out);
        }
        var out=new ArrayList<Storage<ItemVariant>>();
        if(block instanceof BeehiveBlockEntity)out.add(new SingleSlotStorage<>(){
            public long insert(ItemVariant v,long n,TransactionContext tx){return 0;}
            public boolean supportsInsertion(){return false;}
            public long extract(ItemVariant v,long n,TransactionContext tx){StoragePreconditions.notBlankNotNegative(v,n);if(!live() || !v.equals(getResource()))return 0;int count=(int)Math.min(n,getAmount());if(count>0){snapshot.updateSnapshots(tx);hiveBottles-=count;}return count;}
            public ItemVariant getResource(){return honeycomb() && hiveBottles>0?ItemVariant.of(Items.GLASS_BOTTLE):ItemVariant.blank();}
            public boolean isResourceBlank(){return getResource().isBlank();}
            public long getAmount(){return honeycomb()?hiveBottles:0;}
            public long getCapacity(){return 4;}
        });
        for(int i=0;i<output.length;i++){final int slot=i;out.add(new SingleSlotStorage<>(){
            public long insert(ItemVariant v,long n,TransactionContext tx){return 0;}
            public boolean supportsInsertion(){return false;}
            public long extract(ItemVariant v,long n,TransactionContext tx){StoragePreconditions.notBlankNotNegative(v,n);if(!live() || !v.equals(getResource()))return 0;int count=(int)Math.min(n,getAmount());if(count>0){snapshot.updateSnapshots(tx);output[slot].shrink(count);}return count;}
            public ItemVariant getResource(){return ItemVariant.of(output[slot]);}
            public boolean isResourceBlank(){return output[slot].isEmpty();}
            public long getAmount(){return output[slot].getCount();}
            public long getCapacity(){return 64;}
        });}return new CombinedStorage<>(out);
    }
    public boolean hasOutput(){return block instanceof BeehiveBlockEntity && hiveBottles>0 || Arrays.stream(output).anyMatch(s->!s.isEmpty());}
    public boolean managed(){return managed;}
    private boolean buffer(List<ItemStack> products,boolean apply){
        var copy=Arrays.stream(output).map(ItemStack::copy).toArray(ItemStack[]::new);
        for(var product:products){var left=product.copy();for(int i=0;i<copy.length && !left.isEmpty();i++)if(copy[i].isEmpty() || ItemStack.isSameItemSameComponents(copy[i],left)){
            int n=Math.min(left.getCount(),Math.max(0,left.getMaxStackSize()-copy[i].getCount()));if(copy[i].isEmpty())copy[i]=left.copyWithCount(n);else copy[i].grow(n);left.shrink(n);
        }if(!left.isEmpty())return false;}
        if(apply){System.arraycopy(copy,0,output,0,copy.length);changed();}return true;
    }
    public boolean capture(ItemStack item){return managed && buffer(List.of(item),true);}
    public boolean canCraft(){
        if(!managed || !(block instanceof CrafterBlockEntity c))return true;
        if(!live() || selected==null)return false;
        for(int i=0;i<9;i++){var actual=c.getItem(i);if(pattern[i].isEmpty()?!actual.isEmpty():actual.isEmpty() || c.isSlotDisabled(i) || !ItemStack.isSameItemSameComponents(pattern[i],actual))return false;}
        var input=c.asCraftInput();var recipe=CrafterBlock.getPotentialResults(level(),input);if(recipe.isEmpty() || !recipe.get().id().identifier().equals(selected))return false;
        var result=recipe.get().value().assemble(input);if(!filter.allows(result))return false;
        var products=new ArrayList<ItemStack>();products.add(result);products.addAll(recipe.get().value().getRemainingItems(input));return buffer(products,false);
    }
    public CompanionStatus status(){
        if(!live())return CompanionStatus.FORBIDDEN;
        if(block instanceof BeehiveBlockEntity)return hiveJob().readiness();
        if(brewing!=null)return brewing.status();
        if(hasOutput())return CompanionStatus.FULL;
        if(block instanceof CampfireBlockEntity f && !f.getBlockState().getValue(CampfireBlock.LIT))return CompanionStatus.NO_HEAT;
        return selected==null?CompanionStatus.RECIPE_MISSING:CompanionStatus.IDLE;
    }
    public void recover(net.minecraft.world.entity.player.Player player){if(hiveBottles>0){var bottles=new ItemStack(Items.GLASS_BOTTLE,hiveBottles);player.getInventory().add(bottles);hiveBottles=bottles.getCount();}for(var item:output)if(!item.isEmpty())player.getInventory().add(item);changed();}
    public void drop(){if(hiveBottles>0 && block.getLevel() instanceof ServerLevel l){Block.popResource(l,block.getBlockPos(),new ItemStack(Items.GLASS_BOTTLE,hiveBottles));hiveBottles=0;}if(block.getLevel() instanceof ServerLevel l)for(int i=0;i<output.length;i++){if(!output[i].isEmpty())Block.popResource(l,block.getBlockPos(),output[i]);output[i]=ItemStack.EMPTY;}}
    public void save(ValueOutput out){out.putInt("PeepoHiveBottles",hiveBottles);if(brewing!=null)brewing.save(out);filter.save(out);if(selected!=null)out.store("PeepoVanillaRecipe",Identifier.CODEC,selected);out.store("PeepoVanillaOutput",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(output));out.store("PeepoCrafterPattern",ItemStack.OPTIONAL_CODEC.listOf(),Arrays.asList(pattern));out.putBoolean("PeepoManaged",managed);}
    public void load(ValueInput in){filter.load(in);selected=in.read("PeepoVanillaRecipe",Identifier.CODEC).orElse(null);managed=in.getBooleanOr("PeepoManaged",false);hiveBottles=Math.clamp(in.getIntOr("PeepoHiveBottles",0),0,4);if(brewing!=null)brewing.load(in,selected);Arrays.fill(output,ItemStack.EMPTY);Arrays.fill(pattern,ItemStack.EMPTY);in.read("PeepoVanillaOutput",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{for(int i=0;i<Math.min(output.length,list.size());i++)output[i]=list.get(i);});in.read("PeepoCrafterPattern",ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{for(int i=0;i<Math.min(9,list.size());i++)pattern[i]=list.get(i);});}
}
