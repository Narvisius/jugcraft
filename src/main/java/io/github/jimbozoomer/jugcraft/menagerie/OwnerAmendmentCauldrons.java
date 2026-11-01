package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Three-bottle cauldrons with paid dyeing, potion mixing and tipped-arrow batches. */
public final class OwnerAmendmentCauldrons {
    private OwnerAmendmentCauldrons(){}
    static boolean custom(BlockState s){return s.getBlock() instanceof OwnerAmendmentBlock b&&b.name.endsWith("cauldron");}
    static int amount(BlockState s){return Integer.parseInt(HouseholdBlock.value(s,"level"));}
    static String soup(ItemStack stack){
        if(stack.is(Items.MUSHROOM_STEW))return "mushroom_stew";
        if(stack.is(Items.RABBIT_STEW))return "rabbit_stew";
        if(stack.is(Items.BEETROOT_SOUP))return "beetroot_soup";
        if(stack.is(Items.SUSPICIOUS_STEW))return "suspicious_stew";
        return null;
    }
    private static InteractionResult serving(Player who,Level level,InteractionHand hand,BlockPos pos,BlockState state,ItemStack held){
        String kind=soup(held);var data=level.getBlockEntity(pos) instanceof OwnerAmendmentData d?d:null;
        boolean stored=data!=null&&!data.liquidServing.isEmpty();
        if(kind==null&&!stored)return InteractionResult.PASS;
        if(!Menagerie.mayChange(level,who,pos))return InteractionResult.FAIL;
        if(kind!=null){
            if(!state.is(Blocks.CAULDRON)&&(!stored||!ItemStack.isSameItemSameComponents(data.liquidServing,held)||amount(state)>=3))return InteractionResult.FAIL;
            if(!level.isClientSide()){
                int count=stored?amount(state)+1:1;
                var next=HouseholdBlock.set(HouseholdBlock.set(OwnerAmendments.BLOCKS.get("liquid_cauldron").defaultBlockState(),"kind",kind),"level",Integer.toString(count));
                if(!level.setBlock(pos,next,3))return InteractionResult.FAIL;
                if(!(level.getBlockEntity(pos) instanceof OwnerAmendmentData target))return InteractionResult.FAIL;
                target.base=Blocks.CAULDRON.defaultBlockState();target.liquidServing=held.copyWithCount(1);target.liquidColor=0xFFFFFF;target.potion=PotionContents.EMPTY;target.spent=0;target.changed();
                who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,new ItemStack(Items.BOWL)));
            }return InteractionResult.SUCCESS;
        }
        if(held.is(Items.BOWL)){
            if(!level.isClientSide()){var result=data.liquidServing.copy();consume(data,1,true);who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,result));}
            return InteractionResult.SUCCESS;
        }
        if(held.is(Items.WATER_BUCKET)){
            if(!level.isClientSide()){level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,new ItemStack(Items.BUCKET)));}return InteractionResult.SUCCESS;
        }
        // No bottle extraction, dilution or tipped-arrow conversion of stored food.
        return InteractionResult.FAIL;
    }
    static InteractionResult use(Player who,Level level,InteractionHand hand,BlockHitResult hit){
        var pos=hit.getBlockPos();var state=level.getBlockState(pos);var held=who.getItemInHand(hand);
        if(!(state.is(Blocks.CAULDRON)||state.is(Blocks.WATER_CAULDRON)||custom(state)))return InteractionResult.PASS;
        var serving=serving(who,level,hand,pos,state,held);if(serving!=InteractionResult.PASS)return serving;
        boolean dyeItem=held.getItem() instanceof DyeItem,dyeBottle=held.is(OwnerAmendments.DYE_BOTTLE),potionItem=held.is(Items.POTION);
        var supplied=held.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        if(!custom(state)){
            boolean dye=dyeItem&&state.is(Blocks.WATER_CAULDRON)||dyeBottle;
            if(!dye&&!potionItem)return InteractionResult.PASS;
            if(potionItem&&supplied.is(Potions.WATER))return InteractionResult.PASS;
            if(!Menagerie.mayChange(level,who,pos))return InteractionResult.FAIL;
            int water=state.is(Blocks.WATER_CAULDRON)?state.getValue(LayeredCauldronBlock.LEVEL):0;
            // A pour is one bottle. Dyes tint existing water without manufacturing more liquid.
            if((dyeBottle||potionItem)&&water>=3)return InteractionResult.FAIL;
            int count=dyeItem?water:water+1;
            var potion=potionItem?mix(new PotionContents(Potions.WATER),supplied,water):PotionContents.EMPTY;
            if(potionItem&&potion==null)return InteractionResult.FAIL;
            if(!level.isClientSide()){
                var next=HouseholdBlock.set(OwnerAmendments.BLOCKS.get(dye?"dye_cauldron":"liquid_cauldron").defaultBlockState(),"level",Integer.toString(count));
                if(!level.setBlock(pos,next,3))return InteractionResult.FAIL;
                if(level.getBlockEntity(pos) instanceof OwnerAmendmentData data){data.base=Blocks.CAULDRON.defaultBlockState();data.liquidColor=dyeItem?((DyeItem)held.getItem()).getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor():dyeBottle?OwnerAmendmentDyes.color(held):potion.getColor();data.potion=potion==null?PotionContents.EMPTY:potion;data.spent=0;data.changed();}
                if(dyeItem)held.consume(1,who);else who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,new ItemStack(Items.GLASS_BOTTLE)));
            }return InteractionResult.SUCCESS;
        }
        if(!Menagerie.mayChange(level,who,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof OwnerAmendmentData data))return InteractionResult.FAIL;
        boolean dyed=((OwnerAmendmentBlock)state.getBlock()).name.equals("dye_cauldron");int count=amount(state);
        if(dyed&&dyeItem){if(!level.isClientSide()){data.liquidColor=OwnerAmendmentDyes.mix(data.liquidColor,((DyeItem)held.getItem()).getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextureDiffuseColor(),count,1);data.changed();held.consume(1,who);}return InteractionResult.SUCCESS;}
        if(held.is(Items.GLASS_BOTTLE)){
            if(!level.isClientSide()){
                var out=dyed?OwnerAmendmentDyes.bottle(data.liquidColor):new ItemStack(Items.POTION);if(!dyed)out.set(DataComponents.POTION_CONTENTS,data.potion);
                consume(data,1,true);who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,out));
            }return InteractionResult.SUCCESS;
        }
        if((dyed&&dyeBottle)||(!dyed&&potionItem)){
            if(count>=3)return InteractionResult.FAIL;var mixed=dyed?PotionContents.EMPTY:mix(data.potion,supplied,count);if(mixed==null)return InteractionResult.FAIL;
            if(!level.isClientSide()){
                if(dyed)data.liquidColor=OwnerAmendmentDyes.mix(data.liquidColor,OwnerAmendmentDyes.color(held),count,1);else{data.potion=mixed;data.liquidColor=mixed.getColor();}
                level.setBlockAndUpdate(pos,HouseholdBlock.set(state,"level",Integer.toString(count+1)));data.changed();who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,new ItemStack(Items.GLASS_BOTTLE)));
            }return InteractionResult.SUCCESS;
        }
        var result=dyed?OwnerAmendmentDyes.recolor(held,data.liquidColor):ItemStack.EMPTY;
        int cost=1;
        if(!dyed&&held.is(Items.ARROW)&&held.getCount()>=8&&data.potion.hasEffects()){result=new ItemStack(Items.TIPPED_ARROW,8);result.set(DataComponents.POTION_CONTENTS,data.potion);cost=8;}
        if(!result.isEmpty()){
            if(!level.isClientSide()){held.consume(cost,who);who.getInventory().placeItemBackInInventory(result,net.minecraft.util.Prediction.SERVER_ONLY);consume(data,1,false);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.DYE_USE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}return InteractionResult.SUCCESS;
        }
        // Buckets intentionally discard the mixture rather than turning it into valuable free fluids.
        if(held.is(Items.WATER_BUCKET)){
            if(!level.isClientSide()){level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));who.setItemInHand(hand,ItemUtils.createFilledResult(held,who,new ItemStack(Items.BUCKET)));}return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    static void tick(ServerLevel level,net.minecraft.core.BlockPos pos,OwnerAmendmentData data){
        if(level.getGameTime()%20!=0)return;
        var state=data.getBlockState();if(!custom(state)||!data.liquidServing.isEmpty())return;
        var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.item.ItemEntity.class),new net.minecraft.world.phys.AABB(pos).deflate(.125),e->e.isAlive()&&!e.hasPickUpDelay(),drops,16);
        for(var entity:drops){
            if(!(entity.getOwner() instanceof ServerPlayer player)||player.level()!=level||!Menagerie.mayChange(level,player,pos))continue;
            var stack=entity.getItem();boolean dyed=((OwnerAmendmentBlock)state.getBlock()).name.equals("dye_cauldron");
            ItemStack result=dyed?OwnerAmendmentDyes.recolor(stack,data.liquidColor):ItemStack.EMPTY;int cost=1;
            if(!dyed&&stack.is(Items.ARROW)&&stack.getCount()>=8&&data.potion.hasEffects()){result=new ItemStack(Items.TIPPED_ARROW,8);result.set(DataComponents.POTION_CONTENTS,data.potion);cost=8;}
            if(result.isEmpty())continue;
            var output=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+1.05,pos.getZ()+.5,result);output.setDefaultPickUpDelay();output.setThrower(player);output.setDeltaMovement(0,.12,0);
            if(!level.addFreshEntity(output))continue;
            stack.shrink(cost);if(stack.isEmpty())entity.discard();else entity.setItem(stack);consume(data,1,false);
            // One batch per cauldron per second, even in a dense item pile.
            return;
        }
    }
    private static void consume(OwnerAmendmentData data,int units,boolean whole){
        var state=data.getBlockState();int count=amount(state);int limit=((OwnerAmendmentBlock)state.getBlock()).name.equals("dye_cauldron")?4:2;
        if(whole){count-=units;data.spent=0;}else{data.spent+=units;if(data.spent>=limit){count--;data.spent=0;}}
        if(count<=0)data.getLevel().setBlockAndUpdate(data.getBlockPos(),Blocks.CAULDRON.defaultBlockState());
        else{data.getLevel().setBlockAndUpdate(data.getBlockPos(),HouseholdBlock.set(state,"level",Integer.toString(count)));data.changed();}
    }
    private static PotionContents mix(PotionContents a,PotionContents b,int amount){
        if(amount==0||a.equals(b))return b;
        Map<Holder<MobEffect>,MobEffectInstance> first=new LinkedHashMap<>(),second=new LinkedHashMap<>();
        for(var e:a.getAllEffects()){if(e.getEffect().value().isInstantaneous())return null;first.put(e.getEffect(),e);if(first.size()>8)return null;}
        for(var e:b.getAllEffects()){if(e.getEffect().value().isInstantaneous())return null;second.put(e.getEffect(),e);if(second.size()>8)return null;}
        var keys=new LinkedHashSet<>(first.keySet());keys.addAll(second.keySet());if(keys.size()>8)return null;
        var out=new ArrayList<MobEffectInstance>();
        for(var effect:keys){var x=first.get(effect);var y=second.get(effect);int duration=(int)Math.min(Integer.MAX_VALUE,((long)(x==null?0:Math.max(0,x.getDuration()))*amount+(y==null?0:Math.max(0,y.getDuration())))/(amount+1));int amplifier=x==null?y.getAmplifier():y==null?x.getAmplifier():Math.min(x.getAmplifier(),y.getAmplifier());if(duration>0)out.add(new MobEffectInstance(effect,duration,amplifier));}
        return new PotionContents(Optional.empty(),Optional.empty(),List.copyOf(out),Optional.empty());
    }
}
