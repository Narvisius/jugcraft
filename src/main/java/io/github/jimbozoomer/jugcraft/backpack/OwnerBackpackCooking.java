package io.github.jimbozoomer.jugcraft.backpack;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;

/** Persistent paid cooking modules; native furnace screens and ordinary fuel components. */
final class OwnerBackpackCooking {
    private OwnerBackpackCooking(){}
    private static final Map<AbstractContainerMenu,Session> OPEN=new IdentityHashMap<>();
    static boolean uses(ServerPlayer p,ItemStack pack){var session=OPEN.get(p.containerMenu);return session!=null&&session.pack==pack;}
    static void tickOpen(){
        OPEN.entrySet().removeIf(e->e.getValue().owner.containerMenu!=e.getKey()||!e.getValue().valid());
        for(var entry:OPEN.entrySet()){entry.getValue().unit.tick(10);entry.getKey().broadcastChanges();}
    }
    static void clear(){OPEN.clear();}
    static void tick(ServerPlayer player,ItemStack pack,OwnerBackpackInventory bag){
        var upgrades=NonNullList.withSize(7,ItemStack.EMPTY);pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).copyInto(upgrades);
        for(int i=0;i<upgrades.size();i++)if(kind(upgrades.get(i))!=null){
            final int slot=i;new Unit(player,upgrades.get(i),bag,()->{pack.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(upgrades));player.getInventory().setChanged();}).tick(10);
        }
    }
    private static String kind(ItemStack item){
        if(!(item.getItem() instanceof OwnerBackpacks.UpgradeItem upgrade))return null;
        String name=upgrade.name.replaceFirst("^auto_","").replace("_upgrade","");return Set.of("smelting","smoking","blasting").contains(name)?name:null;
    }
    static boolean open(ServerPlayer player,ItemStack pack,int locked,OwnerBackpackEntity placed,int action){
        String requested=switch(action){case 20->"smelting";case 21->"smoking";case 22->"blasting";default->"";};
        if(requested.isEmpty())return false;
        var upgrades=NonNullList.withSize(7,ItemStack.EMPTY);pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).copyInto(upgrades);
        int selected=-1;for(int i=0;i<upgrades.size();i++)if(requested.equals(kind(upgrades.get(i)))){selected=i;break;}if(selected<0)return false;
        var session=new Session(player,pack,locked,placed,upgrades,selected);
        player.openMenu(new SimpleMenuProvider((id,inv,p)->{
            AbstractContainerMenu menu=switch(requested){
                case "smoking"->new SmokerMenu(id,inv,session.unit,session.unit.data){
                    @Override public boolean stillValid(Player p){return p==player&&session.valid();}
                    @Override public void clicked(int s,int b,ContainerInput t,Player p){if(session.click(this,s,b,t))super.clicked(s,b,t,p);}
                };
                case "blasting"->new BlastFurnaceMenu(id,inv,session.unit,session.unit.data){
                    @Override public boolean stillValid(Player p){return p==player&&session.valid();}
                    @Override public void clicked(int s,int b,ContainerInput t,Player p){if(session.click(this,s,b,t))super.clicked(s,b,t,p);}
                };
                default->new FurnaceMenu(id,inv,session.unit,session.unit.data){
                    @Override public boolean stillValid(Player p){return p==player&&session.valid();}
                    @Override public void clicked(int s,int b,ContainerInput t,Player p){if(session.click(this,s,b,t))super.clicked(s,b,t,p);}
                };
            };
            for(int i=0;i<menu.slots.size();i++){var old=menu.slots.get(i);if(old.container!=inv||old.getContainerSlot()!=locked)continue;
                var fixed=new Slot(inv,locked,old.x,old.y){@Override public boolean mayPickup(Player p){return false;}@Override public boolean mayPlace(ItemStack s){return false;}};fixed.index=i;menu.slots.set(i,fixed);
            }
            OPEN.put(menu,session);return menu;
        },net.minecraft.network.chat.Component.literal("Backpack "+requested)));return true;
    }
    private static final class Session {
        final ServerPlayer owner;final ItemStack pack;final int locked;final OwnerBackpackEntity placed;final Unit unit;
        Session(ServerPlayer owner,ItemStack pack,int locked,OwnerBackpackEntity placed,NonNullList<ItemStack> upgrades,int slot){
            this.owner=owner;this.pack=pack;this.locked=locked;this.placed=placed;
            var bag=placed!=null?placed.inventory():new OwnerBackpackInventory(pack,owner.getInventory()::setChanged);
            unit=new Unit(owner,upgrades.get(slot),bag,()->{if(valid()){pack.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(upgrades));if(placed!=null)placed.setChanged();else OwnerBackpacks.changed(owner,pack);}});
        }
        boolean valid(){return owner.isAlive()&&!owner.isSpectator()&&(placed!=null?placed.mayUse(owner)&&placed.carrier()==pack:OwnerBackpacks.heldBy(owner,pack,locked));}
        boolean click(AbstractContainerMenu menu,int slot,int button,ContainerInput input){return valid()&&!(input==ContainerInput.SWAP&&button==locked)&&!(slot>=0&&slot<menu.slots.size()&&menu.slots.get(slot).container==owner.getInventory()&&menu.slots.get(slot).getContainerSlot()==locked);}
    }
    private static final class Unit extends SimpleContainer {
        final SimpleContainerData data=new SimpleContainerData(4);
        final ServerPlayer player;final ItemStack upgrade;final OwnerBackpackInventory bag;final Runnable persist;
        final String kind;final boolean automatic;boolean loading=true;float speed=1,experience;String recipe="";
        Unit(ServerPlayer player,ItemStack upgrade,OwnerBackpackInventory bag,Runnable persist){
            super(3);this.player=player;this.upgrade=upgrade;this.bag=bag;this.persist=persist;kind=kind(upgrade);automatic=((OwnerBackpacks.UpgradeItem)upgrade.getItem()).name.startsWith("auto_");
            var items=NonNullList.withSize(3,ItemStack.EMPTY);upgrade.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(items);for(int i=0;i<3;i++)setItem(i,items.get(i));
            var tag=upgrade.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();for(int i=0;i<4;i++)data.set(i,Math.clamp(tag.getIntOr("PackCook"+i,0),0,1_000_000));
            speed=Math.clamp(tag.getFloatOr("PackCookSpeed",1),.01F,100);experience=tag.getFloatOr("PackCookExperience",0);recipe=tag.getStringOr("PackCookRecipe","");loading=false;
        }
        @Override public void setChanged(){super.setChanged();save();}
        @Override public boolean stillValid(Player who){return player==who&&player.isAlive();}
        private void save(){if(loading)return;
            var list=NonNullList.withSize(3,ItemStack.EMPTY);for(int i=0;i<3;i++)list.set(i,getItem(i));upgrade.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(list));
            CustomData.update(DataComponents.CUSTOM_DATA,upgrade,tag->{for(int i=0;i<4;i++)tag.putInt("PackCook"+i,data.get(i));tag.putFloat("PackCookSpeed",speed);tag.putFloat("PackCookExperience",experience);tag.putString("PackCookRecipe",recipe);});persist.run();
        }
        private Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe(ItemStack stack){var input=new SingleRecipeInput(stack);var manager=player.level().getServer().getRecipeManager();return switch(kind){case "smoking"->manager.getRecipeFor(RecipeType.SMOKING,input,player.level());case "blasting"->manager.getRecipeFor(RecipeType.BLASTING,input,player.level());default->manager.getRecipeFor(RecipeType.SMELTING,input,player.level());};}
        private LootContext context(){return new LootContext.Builder(new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN,player.position()).withParameter(LootContextParams.THIS_ENTITY,player).create(LootContextParamSets.ALL_PARAMS)).create(Optional.empty());}
        private int fuel(ItemStack stack){return ResolvableInt.getFromItem(stack,DataComponents.COOKING_FUEL,CookingFuel::burnTime,context(),0);}
        void tick(int elapsed){
            if(automatic){
                if(!getItem(2).isEmpty()){var output=getItem(2);var left=bag.insert(output);if(left.getCount()!=output.getCount())setItem(2,left);}
                for(int slot=0;slot<bag.getContainerSize();slot++){
                    var candidate=bag.getItem(slot);if(candidate.isEmpty()||!OwnerBackpacks.filter(bag.carrier,candidate))continue;
                    if(getItem(0).isEmpty()&&recipe(candidate).isPresent())setItem(0,bag.removeItem(slot,Math.min(64,candidate.getCount())));
                    else if(getItem(1).isEmpty()&&fuel(candidate)>0)setItem(1,bag.removeItem(slot,1));
                    if(!getItem(0).isEmpty()&&!getItem(1).isEmpty())break;
                }
            }
            var found=recipe(getItem(0));var output=found.map(r->r.value().assemble(new SingleRecipeInput(getItem(0)))).orElse(ItemStack.EMPTY);
            String id=found.map(r->r.id().identifier().toString()).orElse("");if(!recipe.equals(id)){recipe=id;data.set(2,0);}
            var stored=getItem(2);boolean canCook=!output.isEmpty()&&(stored.isEmpty()||ItemStack.isSameItemSameComponents(stored,output)&&stored.getCount()+output.getCount()<=stored.getMaxStackSize());
            // Ten real ticks per pass; fuel and partial progress never advance while offline.
            for(int step=0;step<elapsed;step++){
                if(data.get(0)>0)data.set(0,data.get(0)-1);
                if(!canCook){data.set(2,Math.max(0,data.get(2)-2));continue;}
                if(data.get(0)<=0){
                    var stack=getItem(1);int duration=fuel(stack);if(duration<=0){data.set(2,Math.max(0,data.get(2)-2));continue;}
                    speed=Math.clamp(ResolvableFloat.getFromItem(stack,DataComponents.COOKING_FUEL,CookingFuel::speedMultiplier,context(),1),.01F,100);
                    data.set(0,duration);data.set(1,duration);var remainder=stack.getItem().getCraftingRemainder();removeItem(1,1);
                    if(remainder!=null){if(getItem(1).isEmpty())setItem(1,remainder.create());else{var left=bag.insert(remainder.create());if(!left.isEmpty())player.getInventory().placeItemBackInInventory(left,net.minecraft.util.Prediction.SERVER_ONLY);}}
                }
                int duration=Math.max(1,(int)Math.ceil(found.orElseThrow().value().cookingTime()/speed));data.set(3,duration);data.set(2,data.get(2)+1);
                if(data.get(2)>=duration){
                    data.set(2,0);removeItem(0,1);setItem(2,stored.isEmpty()?output.copy():stored.copyWithCount(stored.getCount()+output.getCount()));
                    experience+=found.orElseThrow().value().experience();int xp=(int)experience;experience-=xp;save();if(xp>0)player.giveExperiencePoints(xp);
                    break; // Re-evaluate recipe/output capacity on the next bounded pass.
                }
            }
            save();
        }
    }
}
