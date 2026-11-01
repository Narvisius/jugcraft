package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.enchanting.OwnerEnchanting;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Saved flower diets and cooldowns. Searches are bounded and never load chunks. */
final class BotanicalGenerators {
    static final Set<String> TYPES=Set.of("endoflame","hydroangeas","thermalily","gourmaryllis","rosa_arcana","kekimurus","munchdew","spectrolus","narslimmus","shulk_me_not","rafflowsia","dandelifeon","entropinnyum");
    private static final int[] LAVA_WEIGHTS={10,5,3,2,1,1,3,3,3,2,1,1,1,2,2};
    private static final double[] FOOD_MULTIPLIERS={0,1,1.3,1.5,1.6,1.7,1.75,1.8};
    private final BotanicalWorkshop flower;
    private int rosePending;
    private int burning,cooldown,cooldownStrength=15,age,digesting,streak,repeats,color,cursor,emptyScans;
    private final List<ItemStack> foods=new ArrayList<>();
    BotanicalGenerators(BotanicalWorkshop flower){this.flower=flower;}
    static int capacity(String kind){return switch(kind){case "endoflame"->300;case "hydroangeas"->150;case "thermalily"->750;case "gourmaryllis"->18144;case "rosa_arcana"->6000;case "kekimurus"->9001;case "munchdew"->10000;case "spectrolus"->16000;case "narslimmus"->19200;case "shulk_me_not"->75000;case "rafflowsia"->638554;case "dandelifeon"->50000;case "entropinnyum"->6500;default->1000000;};}
    String status(){return flower.kind().equals("spectrolus")?" · Next wool: "+Jugtania.COLORS.get(color):cooldown>0?" · Cooldown: "+(cooldown+19)/20+"s":"";}
    void tick(ServerLevel level,ServerPlayer actor){
        switch(flower.kind()){
            case "narslimmus","shulk_me_not","rafflowsia"->flower.harvest.tick(level,actor);
            case "dandelifeon"->flower.life.tick(level,actor);
            case "entropinnyum"->BotanicalExplosives.tick(flower,level,actor);
            case "hydroangeas","thermalily"->fluid(level,actor);
            case "gourmaryllis"->food(level,actor);
            case "rosa_arcana"->rose(level,actor);
            case "kekimurus"->{if(level.getGameTime()%80==0&&flower.capacity()-flower.mana()>=1800)cake(level,actor);}
            case "munchdew"->leaves(level,actor);
            case "spectrolus"->{if(level.getGameTime()%10==0&&flower.capacity()-flower.mana()>=1200)wool(level,actor);}
            default->{}
        }
        if(level.getGameTime()%20==0)flower.changed();
    }
    private void fluid(ServerLevel level,ServerPlayer actor){
        boolean lava=flower.kind().equals("thermalily");var at=flower.effectivePos();
        if(!lava&&++age>72000){var actual=flower.getBlockPos();var wilt=Blocks.DEAD_BUSH.defaultBlockState();level.setBlock(actual,wilt.canSurvive(level,actual)?wilt:Blocks.AIR.defaultBlockState(),3);return;}
        if(burning>0){
            burning--;if(lava||level.getGameTime()%(level.isRainingAt(at)?2:3)==0)flower.generate(lava?45:1);
            if(level.getGameTime()%10==0)level.sendParticles(lava?ParticleTypes.FLAME:ParticleTypes.SPLASH,at.getX()+.5,at.getY()+.6,at.getZ()+.5,2,.15,.1,.15,0);
            flower.setChanged();return;
        }
        if(cooldown>0)cooldown--;flower.setChanged();if(level.getGameTime()%10!=0||flower.mana()>=flower.capacity())return;
        for(int n=0;n<8;n++){
            int i=(n+cursor)%8;int x=new int[]{-1,0,1,1,1,0,-1,-1}[i],z=new int[]{-1,-1,-1,0,1,1,1,0}[i];var pos=at.offset(x,0,z);
            if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))continue;var state=level.getBlockState(pos);
            if(!state.is(lava?Blocks.LAVA:Blocks.WATER)||!state.getFluidState().isSource())continue;
            int neighbors=0;if(!lava)for(Direction side:Direction.values())if(level.hasChunkAt(pos.relative(side))&&level.getFluidState(pos.relative(side)).is(net.minecraft.tags.FluidTags.WATER))neighbors++;
            if((lava||neighbors<2)&&!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3))continue;
            if(cooldown==0){burning=lava?600:40;if(lava){int roll=level.getRandom().nextInt(Arrays.stream(LAVA_WEIGHTS).sum());cooldownStrength=1;for(int weight:LAVA_WEIGHTS){if(roll<weight)break;roll-=weight;cooldownStrength++;}cooldown=cooldownStrength*400;}}
            else cooldown=cooldownStrength*400;cursor=(i+1)%8;flower.changed();break;
        }
    }
    private void food(ServerLevel level,ServerPlayer actor){
        if(cooldown>0){cooldown--;flower.setChanged();return;}
        if(digesting>0){int accepted=flower.generate(digesting);digesting-=accepted;flower.setChanged();if(digesting>0)return;}
        if(level.getGameTime()%5!=0)return;
        for(var drop:drops(level,actor,1)){
            var stack=drop.getItem();var food=stack.get(DataComponents.FOOD);if(food==null||food.nutrition()<=0)continue;
            int index=-1;for(int i=0;i<foods.size();i++)if(ItemStack.isSameItemSameComponents(stack,foods.get(i))){index=i;break;}
            streak=Math.min(streak+1,index<0?7:index);if(index>=0)foods.remove(index);foods.addFirst(stack.copyWithCount(1));while(foods.size()>6)foods.removeLast();
            double multiplier=streak==0?1.0/++repeats:FOOD_MULTIPLIERS[streak];if(streak>0)repeats=1;
            int value=Math.min(12,food.nutrition());digesting=Math.max(1,(int)(value*value*70*multiplier));cooldown=Math.max(1,value*10);
            // Consume exactly one offering; remainder containers stay in the world.
            var remainder=stack.getItem().getCraftingRemainder();if(remainder!=null)Block.popResource(level,drop.blockPosition(),remainder.create());consume(drop);flower.changed();return;
        }
    }
    private void rose(ServerLevel level,ServerPlayer actor){
        if(rosePending>0){rosePending-=flower.generate(rosePending);flower.setChanged();return;}
        if(level.getGameTime()%5!=0||flower.capacity()-flower.mana()<50)return;
        if(actor.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(flower.effectivePos()))<=4&&actor.onGround()&&!Authority.isStandIn(actor)&&!actor.getAbilities().instabuild&&OwnerEnchanting.experience(actor)>0){actor.giveExperiencePoints(-1);flower.generate(50);return;}
        var bounds=new AABB(flower.effectivePos()).inflate(1);int inspected=0;
        for(var orb:level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,bounds)){
            if(++inspected>32)break;if(!orb.isAlive()||orb.getValue()<=0||orb.getValue()>32767||!Authority.mayChange(level,actor,orb.blockPosition()))continue;
            var merged=(io.github.jimbozoomer.jugcraft.mixin.AlchemyExperienceAccess)orb;int count=merged.jugcraft$count();if(count<=0)continue;
            rosePending=orb.getValue()*50;merged.jugcraft$count(count-1);if(count==1)orb.discard();flower.setChanged();return;
        }
        for(var drop:drops(level,actor,1)){
            var stack=drop.getItem();var enchantments=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(stack);
            long xp=0;for(var entry:enchantments.entrySet())if(!entry.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE))xp+=Math.max(0,entry.getKey().value().getMinCost(entry.getIntValue()));
            if(xp<=0)continue;
            var result=stack.copyWithCount(1);var curses=new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(enchantments);curses.removeIf(e->!e.is(net.minecraft.tags.EnchantmentTags.CURSE));
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(result,curses.toImmutable());result.set(DataComponents.REPAIR_COST,0);
            if(result.is(Items.ENCHANTED_BOOK)&&curses.toImmutable().isEmpty()){result=result.transmuteCopy(Items.BOOK);result.remove(DataComponents.STORED_ENCHANTMENTS);}
            var output=new ItemEntity(level,drop.getX(),drop.getY(),drop.getZ(),result);output.setDeltaMovement(drop.getDeltaMovement());output.setDefaultPickUpDelay();
            if(!level.addFreshEntity(output))return;consume(drop);rosePending=(int)Math.min(1638350,xp*50);flower.setChanged();return;
        }
    }
    private void wool(ServerLevel level,ServerPlayer actor){
        for(var sheep:level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class,new AABB(flower.effectivePos())).stream().limit(16).toList()){
            int value=sheep.isBaby()?1:5000;
            if(sheep.isSheared()||sheep.getColor()!=DyeColor.byId(color)||flower.capacity()-flower.mana()<value||!BotanicalDefenses.eligible(level,actor,sheep))continue;
            sheep.discard();color=(color+1)%16;flower.generate(value);flower.changed();return;
        }
        String wanted="minecraft:"+Jugtania.COLORS.get(color)+"_wool";
        for(var drop:drops(level,actor,1))if(BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString().equals(wanted)){consume(drop);color=(color+1)%16;flower.generate(1200);flower.changed();return;}
    }
    private void cake(ServerLevel level,ServerPlayer actor){
        for(var pos:BlockPos.betweenClosed(flower.effectivePos().offset(-5,-5,-5),flower.effectivePos().offset(5,5,5))){
            if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);if(!(state.getBlock() instanceof CakeBlock)||!Authority.mayChange(level,actor,pos))continue;
            int bites=state.getValue(CakeBlock.BITES);if(level.setBlock(pos,bites==6?Blocks.AIR.defaultBlockState():state.setValue(CakeBlock.BITES,bites+1),3)){flower.generate(1800);return;}
        }
    }
    private void leaves(ServerLevel level,ServerPlayer actor){
        if(cooldown>0){cooldown--;flower.setChanged();return;}if(level.getGameTime()%4!=0||flower.capacity()-flower.mana()<160)return;
        // Walk the original 17×17×17 canopy in slices, rather than scanning 4,913 blocks every four ticks.
        for(int n=0;n<128;n++){
            int i=Math.floorMod(cursor++,4913);var pos=flower.effectivePos().offset(i%17-8,(i/17)%17,i/289-8);
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(BlockTags.LEAVES)||!Authority.mayChange(level,actor,pos))continue;
            boolean exposed=false;for(Direction d:Direction.values())if(level.hasChunkAt(pos.relative(d))&&level.isEmptyBlock(pos.relative(d)))exposed=true;
            if(exposed&&level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){flower.generate(160);emptyScans=0;flower.setChanged();return;}
        }
        if(++emptyScans>=39){cooldown=1600;emptyScans=0;}flower.setChanged();
    }
    private List<ItemEntity> drops(ServerLevel level,ServerPlayer actor,int range){return level.getEntitiesOfClass(ItemEntity.class,new AABB(flower.effectivePos()).inflate(range)).stream().filter(e->e.isAlive()&&e.getAge()>=20&&!e.hasPickUpDelay()&&BotanicalDropCooldown.ready(e)&&!e.getItem().isEmpty()&&Authority.mayChange(level,actor,e.blockPosition())).limit(32).toList();}
    private static void consume(ItemEntity drop){drop.getItem().shrink(1);if(drop.getItem().isEmpty())drop.discard();}
    void save(ValueOutput out){out.putInt("flower_rose_pending",rosePending);out.putInt("flower_burn",burning);out.putInt("flower_cooldown",cooldown);out.putInt("flower_cooldown_strength",cooldownStrength);out.putInt("flower_age",age);out.putInt("flower_digesting",digesting);out.putInt("flower_streak",streak);out.putInt("flower_repeats",repeats);out.putInt("flower_color",color);out.putInt("flower_cursor",cursor);out.putInt("flower_empty_scans",emptyScans);out.store("flower_foods",ItemStack.CODEC.listOf(),foods);}
    void load(ValueInput in){rosePending=Math.clamp(in.getIntOr("flower_rose_pending",0),0,1638350);burning=Math.clamp(in.getIntOr("flower_burn",0),0,600);cooldown=Math.clamp(in.getIntOr("flower_cooldown",0),0,6000);cooldownStrength=Math.clamp(in.getIntOr("flower_cooldown_strength",15),1,15);age=Math.clamp(in.getIntOr("flower_age",0),0,72001);digesting=Math.clamp(in.getIntOr("flower_digesting",0),0,18144);streak=Math.clamp(in.getIntOr("flower_streak",0),0,7);repeats=Math.clamp(in.getIntOr("flower_repeats",0),0,100000);color=Math.clamp(in.getIntOr("flower_color",0),0,15);cursor=Math.floorMod(in.getIntOr("flower_cursor",0),4913);emptyScans=Math.clamp(in.getIntOr("flower_empty_scans",0),0,39);foods.clear();in.read("flower_foods",ItemStack.CODEC.listOf()).orElse(List.of()).stream().limit(6).map(s->s.copyWithCount(1)).forEach(foods::add);}
}
