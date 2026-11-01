package io.github.jimbozoomer.jugcraft.backpack;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.mixin.OwnerItemTargetAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.*;

/** Paid bounded operations on real inventories. No remote chunk loads or bypass of personal loot/claims. */
public final class OwnerBackpackUpgrades {
    private OwnerBackpackUpgrades(){}
    private static boolean has(ItemStack pack,String name){return OwnerBackpacks.has(pack,name+"_upgrade")||OwnerBackpacks.has(pack,"advanced_"+name+"_upgrade");}
    public static boolean accessible(ServerPlayer p,ItemStack pack){return OwnerBackpacks.tier(pack)>=0&&!(p.containerMenu instanceof OwnerBackpackMenu menu&&menu.uses(pack))&&!OwnerBackpackWorkstations.uses(p,pack)&&!OwnerBackpackCooking.uses(p,pack);}
    static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{OwnerBackpackCooking.clear();OwnerBackpackMusic.clear();OwnerBackpackWorkstations.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;
            OwnerBackpackWorkstations.clean();
            OwnerBackpackCooking.tickOpen();
            OwnerBackpackMusic.tick(server);
            for(var p:server.getPlayerList().getPlayers())if(p.isAlive()&&!p.isSpectator()){
                int processed=0;var worn=OwnerBackpacks.worn(p);if(worn!=null&&accessible(p,worn.get())){tick(p,worn.get());OwnerBackpacks.changed(p,worn.get());processed++;}
                for(int i=0;i<p.getInventory().getContainerSize()&&processed<3;i++){
                    var pack=p.getInventory().getItem(i);if(!accessible(p,pack))continue;processed++;tick(p,pack);
                }
            }
        });
        AttackBlockCallback.EVENT.register((player,level,hand,pos,face)->{
            if(!(player instanceof ServerPlayer p)||p.isCreative()||hand!=InteractionHand.MAIN_HAND||!Authority.mayChange(p.level(),p,pos))return InteractionResult.PASS;
            for(int i=0;i<p.getInventory().getContainerSize();i++){
                var pack=p.getInventory().getItem(i);if(!accessible(p,pack)||!has(pack,"tool_swapper"))continue;
                var inv=new OwnerBackpackInventory(pack,p.getInventory()::setChanged);var held=p.getMainHandItem();var block=level.getBlockState(pos);float best=held.getDestroySpeed(block);int chosen=-1;
                if(held==pack||!held.isEmpty()&&!OwnerBackpacks.allows(pack,held))continue;
                for(int slot=0;slot<inv.getContainerSize();slot++){
                    var tool=inv.getItem(slot);if(tool.isEmpty()||!OwnerBackpackFilters.matches(pack,tool,"tool_swapper"))continue;
                    float speed=tool.getDestroySpeed(block);if(speed>best&&(!block.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(block))){best=speed;chosen=slot;}
                }
                if(chosen>=0){var tool=inv.getItem(chosen);inv.setItem(chosen,held);p.setItemInHand(hand,tool);p.getInventory().setChanged();break;}
            }
            return InteractionResult.PASS;
        });
    }
    private static void tick(ServerPlayer p,ItemStack pack){
        if(!OwnerBackpackInfinity.mayEdit(p,pack))return;
        var inv=new OwnerBackpackInventory(pack,p.getInventory()::setChanged);
        if(has(pack,"pickup")||has(pack,"magnet")&&!io.github.jimbozoomer.jugcraft.botany.BotanicalFields.magnetBlocked(p)){
            double range=has(pack,"magnet")?(OwnerBackpacks.has(pack,"advanced_magnet_upgrade")?5:3):1.25;
            var nearby=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(range),e->e.isAlive()&&!e.hasPickUpDelay());int remaining=32;
            for(var drop:nearby){if(remaining--<=0)break;
                if(has(pack,"magnet")&&(io.github.jimbozoomer.jugcraft.botany.BotanicalFields.magnetBlocked(p)||io.github.jimbozoomer.jugcraft.botany.BotanicalFields.magnetBlocked(drop)))continue;
                var target=((OwnerItemTargetAccess)drop).jugcraft$pickupTarget();
                if(target!=null&&!target.equals(p.getUUID())||!Authority.mayChange(p.level(),p,drop.blockPosition())||!p.hasLineOfSight(drop)||!OwnerBackpackFilters.matches(pack,drop.getItem(),has(pack,"magnet")?"magnet":"pickup"))continue;
                var before=drop.getItem();var left=inv.insert(before);int moved=before.getCount()-left.getCount();
                if(moved>0){p.take(drop,moved);if(left.isEmpty())drop.discard();else drop.setItem(left);}
            }
        }
        if(has(pack,"refill")){
            int budget=64;
            for(int slot=0;slot<36&&budget>0;slot++){
                var held=p.getInventory().getItem(slot);if(held.isEmpty()||held==pack||!OwnerBackpackFilters.matches(pack,held,"refill"))continue;
                int wanted=Math.min(budget,held.getMaxStackSize()-held.getCount());
                for(int j=0;j<inv.getContainerSize()&&wanted>0;j++)if(ItemStack.isSameItemSameComponents(held,inv.getItem(j))){int n=Math.min(wanted,inv.getItem(j).getCount());inv.removeItem(j,n);held.grow(n);wanted-=n;budget-=n;p.getInventory().setChanged();}
            }
        }
        if(has(pack,"feeding")&&p.level().getServer().getTickCount()%40==0&&p.getFoodData().needsFood()){
            for(int i=0;i<inv.getContainerSize();i++){
                var food=inv.getItem(i);var nutrition=food.get(DataComponents.FOOD);
                if(nutrition==null||!OwnerBackpackFilters.matches(pack,food,"feeding")||20-p.getFoodData().getFoodLevel()<nutrition.nutrition())continue;
                // Run normal consumption so bowls, effects and use remainders are retained.
                var serving=food.copyWithCount(1);var remainder=serving.finishUsingItem(p.level(),p);inv.removeItem(i,1);
                if(!remainder.isEmpty()){var left=inv.insert(remainder);if(!left.isEmpty())p.getInventory().placeItemBackInInventory(left,net.minecraft.util.Prediction.SERVER_ONLY);}
                break;
            }
        }
        // A blank filter must never mean "erase everything" for destructive upgrades.
        if(has(pack,"void")&&OwnerBackpackFilters.hasSamples(pack,"void")){
            int budget=64;for(int i=0;i<inv.getContainerSize()&&budget>0;i++){var stack=inv.getItem(i);if(!stack.isEmpty()&&OwnerBackpackFilters.matches(pack,stack,"void")){int n=Math.min(budget,stack.getCount());inv.removeItem(i,n);budget-=n;}}
        }
        OwnerBackpackCooking.tick(p,pack,inv);
        if(p.level().getServer().getTickCount()%20==0&&has(pack,"compacting"))compact(p,pack,inv);
        charge(p,pack,inv);
        OwnerBackpackAlchemy.tick(p,pack,inv);
        OwnerBackpackFluids.automatic(p,pack);
    }
    static void placed(ServerPlayer owner,OwnerBackpackEntity placed){
        var pack=placed.carrier();if(!OwnerBackpackInfinity.mayEdit(owner,pack))return;
        var inventory=placed.inventory();OwnerBackpackCooking.tick(owner,pack,inventory);
        if(owner.level().getServer().getTickCount()%20==0&&has(pack,"compacting"))compact(owner,pack,inventory);
        charge(owner,pack,inventory,false);
        OwnerBackpackFluids.automatic(owner,pack);placed.refreshInventory();placed.setChanged();
    }
    private static void charge(ServerPlayer p,ItemStack pack,OwnerBackpackInventory inventory){charge(p,pack,inventory,true);}
    private static void charge(ServerPlayer p,ItemStack pack,OwnerBackpackInventory inventory,boolean carried){
        if(!has(pack,"battery"))return;
        var upgrades=NonNullList.withSize(7,ItemStack.EMPTY);pack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).copyInto(upgrades);boolean changed=false;
        for(var battery:upgrades)if(battery.is(OwnerBackpacks.ITEMS.get("battery_upgrade"))){
            long budget=Math.min(256,io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(battery)),used=0;
            for(int i=0;i<inventory.getContainerSize()&&used<budget;i++)used+=io.github.jimbozoomer.jugcraft.tools.Chargeable.charge(inventory.getItem(i),budget-used);
            for(int i=0;carried&&i<p.getInventory().getContainerSize()&&used<budget;i++){var item=p.getInventory().getItem(i);if(item!=pack)used+=io.github.jimbozoomer.jugcraft.tools.Chargeable.charge(item,budget-used);}
            if(used>0){io.github.jimbozoomer.jugcraft.tools.Chargeable.drain(battery,used);changed=true;}
        }
        if(changed){pack.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(upgrades));inventory.setChanged();p.getInventory().setChanged();}
    }
    private static void compact(ServerPlayer p,ItemStack pack,OwnerBackpackInventory inventory){
        int start=pack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("PackCompactCursor",0);
        for(int checked=0;checked<8;checked++){
            int slot=Math.floorMod(start+checked,inventory.getContainerSize());var sample=inventory.getItem(slot);
            if(sample.isEmpty()||!OwnerBackpackFilters.matches(pack,sample,"compacting")||!sample.isStackable())continue;
            long count=0;for(int i=0;i<inventory.getContainerSize();i++)if(ItemStack.isSameItemSameComponents(sample,inventory.getItem(i)))count+=inventory.getItem(i).getCount();
            for(int width:OwnerBackpacks.has(pack,"advanced_compacting_upgrade")?new int[]{3,2}:new int[]{2}){
                int cost=width*width;if(count<cost)continue;
                var grid=net.minecraft.world.item.crafting.CraftingInput.of(width,width,java.util.Collections.nCopies(cost,sample.copyWithCount(1)));
                var recipe=p.level().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,p.level());if(recipe.isEmpty())continue;
                var result=recipe.get().value().assemble(grid);if(result.isEmpty()||result.getCount()>=cost||ItemStack.isSameItemSameComponents(sample,result))continue;
                // Simulate consumption, output and every remainder before committing the replacement.
                var simulation=new OwnerBackpackInventory(pack.copy(),()->{});int left=cost;
                for(int i=0;i<simulation.getContainerSize()&&left>0;i++)if(ItemStack.isSameItemSameComponents(sample,simulation.getItem(i))){int used=Math.min(left,simulation.getItem(i).getCount());simulation.removeItem(i,used);left-=used;}
                if(!simulation.insert(result).isEmpty())continue;boolean fits=true;
                for(var remainder:recipe.get().value().getRemainingItems(grid))if(!remainder.isEmpty()&&!simulation.insert(remainder).isEmpty()){fits=false;break;}
                if(!fits)continue;
                for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,simulation.getItem(i));
                net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putInt("PackCompactCursor",slot+1));return;
            }
        }
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putInt("PackCompactCursor",Math.floorMod(start+8,inventory.getContainerSize())));
    }
    public static boolean transfer(ServerPlayer player,BlockPos pos,Direction side,ItemStack pack){
        if(!accessible(player,pack)||!has(pack,"deposit")&&!has(pack,"restock")||!Authority.mayChange(player.level(),player,pos))return false;
        var target=HopperBlockEntity.getContainerAt(player.level(),pos);if(target==null||target instanceof RandomizableContainer random&&random.getLootTable()!=null)return false;
        if(!target.stillValid(player)||target.getContainerSize()>512)return false;
        var inventory=new OwnerBackpackInventory(pack,player.getInventory()::setChanged);int moved=0,budget=256;
        int[] slots=target instanceof WorldlyContainer sided?sided.getSlotsForFace(side):java.util.stream.IntStream.range(0,target.getContainerSize()).toArray();
        boolean deposit=has(pack,"deposit");
        if(deposit){
            for(int i=0;i<inventory.getContainerSize()&&budget>0;i++){
                var stack=inventory.getItem(i);if(stack.isEmpty()||!OwnerBackpackFilters.matches(pack,stack,"deposit"))continue;
                for(int slot:slots){if(budget<=0||stack.isEmpty())break;if(slot<0||slot>=target.getContainerSize()||!target.canPlaceItem(slot,stack)||target instanceof WorldlyContainer sided&&!sided.canPlaceItemThroughFace(slot,stack,side))continue;
                    var existing=target.getItem(slot);if(!existing.isEmpty()&&!ItemStack.isSameItemSameComponents(existing,stack))continue;
                    int take=Math.min(budget,Math.min(stack.getCount(),Math.min(stack.getMaxStackSize(),target.getMaxStackSize(stack))-existing.getCount()));if(take<=0)continue;
                    var next=existing.isEmpty()?stack.copyWithCount(take):existing.copyWithCount(existing.getCount()+take);target.setItem(slot,next);inventory.removeItem(i,take);budget-=take;moved+=take;
                }
            }
        }else{
            for(int slot:slots){if(budget<=0)break;if(slot<0||slot>=target.getContainerSize())continue;var stack=target.getItem(slot);
                if(stack.isEmpty()||!OwnerBackpackFilters.matches(pack,stack,"restock")||target instanceof WorldlyContainer sided&&!sided.canTakeItemThroughFace(slot,stack,side))continue;
                var offered=stack.copyWithCount(Math.min(budget,stack.getCount()));var left=inventory.insert(offered);int take=offered.getCount()-left.getCount();if(take>0){target.removeItem(slot,take);budget-=take;moved+=take;}
            }
        }
        if(moved>0)target.setChanged();player.sendOverlayMessage(net.minecraft.network.chat.Component.literal((deposit?"Deposited ":"Restocked ")+moved+" items."));return true;
    }
}
