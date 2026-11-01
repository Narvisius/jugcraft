package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.finance.ClaimTransfers;
import io.github.jimbozoomer.jugcraft.mixin.BotanicalFurnaceAccess;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

final class BotanicalHusbandry {
    private final BotanicalWorkshop flower;private int mode;private long lastScan=Long.MIN_VALUE;
    private final List<BlockPos> furnaces=new ArrayList<>();
    BotanicalHusbandry(BotanicalWorkshop flower){this.flower=flower;}
    void cycle(ServerPlayer player){mode=(mode+1)%3;flower.setChanged();player.sendOverlayMessage(Component.literal("Feeding: "+List.of("adults","young animals","adults and young animals").get(mode)));}
    void tick(ServerLevel level,ServerPlayer actor){
        if(flower.kind().equals("exoflame")){heat(level,actor);return;}
        if(level.getGameTime()%10!=0||flower.mana()<12)return;
        var origin=flower.effectivePos();var bounds=new AABB(origin).inflate(6);
        var food=level.getEntitiesOfClass(ItemEntity.class,bounds,d->d.isAlive()&&d.getAge()>=20&&!d.hasPickUpDelay()&&BotanicalDropCooldown.ready(d)&&Authority.mayChange(level,actor,d.blockPosition())&&ClaimTransfers.allowed(level,origin,d.blockPosition())).stream().limit(32).toList();
        var animals=level.getEntitiesOfClass(Animal.class,bounds,a->a.isAlive()&&Authority.mayChange(level,actor,a.blockPosition())&&Authority.mayStrike(actor,a)).stream().limit(32).sorted(Comparator.comparing(Animal::isBaby)).toList();
        for(var animal:animals){
            if(flower.mana()<12)break;boolean baby=animal.isBaby();
            if(mode==0&&baby||mode==1&&!baby||baby&&animal.getAge()>=-600||!baby&&(animal.getAge()!=0||!animal.canFallInLove()))continue;
            for(var drop:food){
                var stack=drop.getItem();if(!drop.isAlive()||stack.isEmpty()||!animal.isFood(stack))continue;
                if(!flower.draw(12))return;
                if(baby)animal.ageUp(Math.max(1,(int)(-animal.getAge()/20*.1)),true);else{animal.setInLove(actor);animal.setInLoveTime(1200);}
                var remainder=stack.getItem().getCraftingRemainder();if(remainder!=null)net.minecraft.world.level.block.Block.popResource(level,drop.blockPosition(),remainder.create());
                stack.shrink(1);if(stack.isEmpty())drop.discard();level.broadcastEntityEvent(animal,(byte)18);break;
            }
        }
    }
    private void heat(ServerLevel level,ServerPlayer actor){
        long now=level.getGameTime();if(now%2!=0||flower.mana()<300)return;var origin=flower.effectivePos();
        if(lastScan==Long.MIN_VALUE||now-lastScan>=40||now<lastScan){
            furnaces.clear();lastScan=now;
            for(var p:BlockPos.betweenClosed(origin.offset(-5,-2,-5),origin.offset(5,2,5))){
                if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof AbstractFurnaceBlockEntity)furnaces.add(p.immutable());if(furnaces.size()>=64)break;
            }
        }
        for(var pos:furnaces){
            if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos)||!ClaimTransfers.allowed(level,origin,pos)||!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace))continue;
            var input=new SingleRecipeInput(furnace.getItem(0));if(input.item().isEmpty())continue;
            var recipes=level.getServer().getRecipeManager();Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> match;
            if(furnace instanceof SmokerBlockEntity)match=recipes.getRecipeFor(RecipeType.SMOKING,input,level);
            else if(furnace instanceof BlastFurnaceBlockEntity)match=recipes.getRecipeFor(RecipeType.BLASTING,input,level);
            else if(furnace instanceof FurnaceBlockEntity)match=recipes.getRecipeFor(RecipeType.SMELTING,input,level);else continue;
            if(match.isEmpty())continue;var result=match.get().value().assemble(input);var output=furnace.getItem(2);
            if(result.isEmpty()||!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()+result.getCount()>Math.min(output.getMaxStackSize(),furnace.getMaxStackSize())))continue;
            var access=(BotanicalFurnaceAccess)furnace;
            if(access.jugcraft$heat()<2){
                if(!flower.draw(300))return;access.jugcraft$heat(access.jugcraft$heat()+200);access.jugcraft$heatTotal(200);
                var state=furnace.getBlockState();if(state.hasProperty(BlockStateProperties.LIT)&&!state.getValue(BlockStateProperties.LIT))level.setBlock(pos,state.setValue(BlockStateProperties.LIT,true),3);
            }
            if(access.jugcraft$cookTotal()>0&&access.jugcraft$heat()>0)access.jugcraft$cook(Math.min(access.jugcraft$cookTotal()-1,access.jugcraft$cook()+1));furnace.setChanged();
        }
    }
    void save(ValueOutput out){out.putInt("flower_feeding_mode",mode);}
    void load(ValueInput in){mode=Math.clamp(in.getIntOr("flower_feeding_mode",0),0,2);furnaces.clear();lastScan=Long.MIN_VALUE;}
}
