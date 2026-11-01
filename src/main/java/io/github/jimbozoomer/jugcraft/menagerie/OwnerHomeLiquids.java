package io.github.jimbozoomer.jugcraft.menagerie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import io.github.jimbozoomer.jugcraft.concordance.Authority;

/** Conserved bottle units: taps never manufacture fluids or overwrite player cells. */
final class OwnerHomeLiquids {
    private OwnerHomeLiquids() {}
    static InteractionResult use(OwnerHomeBlock block,ItemStack stack,BlockState hitState,Level level,BlockPos hitPos,Player player,InteractionHand hand){
        BlockPos pos=block.root(hitPos,hitState);BlockState state=level.getBlockState(pos);
        if(!(level.getBlockEntity(pos) instanceof OwnerHome.Data data))return InteractionResult.PASS;
        int capacity=block.function.equals("bathtub")?6:block.function.equals("dye_vat")?5:3, amount=data.fluidUnits;
        String contains=HouseholdBlock.value(state,"contains"), incoming="";int delta=0;ItemStack result=ItemStack.EMPTY;
        if(block.function.equals("dye_vat")&&amount>0&&data.color!=-1&&stack.is(net.minecraft.tags.ItemTags.CAULDRON_CAN_REMOVE_DYE)){
            if(!level.isClientSide()){stack.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(data.color));data.fluidUnits--;refresh(block,level,pos,data,"water");}return InteractionResult.SUCCESS;
        }
        if(stack.is(Items.WATER_BUCKET)){incoming="water";delta=3;result=new ItemStack(Items.BUCKET);}
        else if(stack.is(Items.LAVA_BUCKET)){incoming="lava";delta=3;result=new ItemStack(Items.BUCKET);}
        else if(stack.is(Items.HONEY_BOTTLE)){incoming="honey";delta=1;result=new ItemStack(Items.GLASS_BOTTLE);}
        else if(stack.is(Items.POTION)&&stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).is(Potions.WATER)){incoming="water";delta=1;result=new ItemStack(Items.GLASS_BOTTLE);}
        else if(stack.is(Items.BUCKET)&&amount>=3&&(contains.equals("water")||contains.equals("lava"))){delta=-3;result=new ItemStack(contains.equals("water")?Items.WATER_BUCKET:Items.LAVA_BUCKET);}
        else if(stack.is(Items.GLASS_BOTTLE)&&amount>0&&(contains.equals("water")||contains.equals("honey"))){delta=-1;result=contains.equals("water")?PotionContents.createItemStack(Items.POTION,Potions.WATER):new ItemStack(Items.HONEY_BOTTLE);}
        else if(amount>0&&contains.equals("water")&&stack.has(DataComponents.DYED_COLOR)&&!stack.is(OwnerHome.item("paint_brush"))){
            if(!level.isClientSide()){stack.remove(DataComponents.DYED_COLOR);data.fluidUnits--;refresh(block,level,pos,data,contains);}return InteractionResult.SUCCESS;
        }
        if(delta==0)return InteractionResult.PASS;
        if(delta>0&&(amount+delta>capacity||amount>0&&!contains.equals(incoming)||block.function.equals("dye_vat")&&!incoming.equals("water")))return InteractionResult.FAIL;
        if(!level.isClientSide()){
            data.fluidUnits+=delta;refresh(block,level,pos,data,delta>0?incoming:contains);
            player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,result));
        }return InteractionResult.SUCCESS;
    }
    static void refresh(OwnerHomeBlock block,Level level,BlockPos pos,OwnerHome.Data data,String contains){
        BlockState state=level.getBlockState(pos);int units=data.fluidUnits;
        state=HouseholdBlock.set(state,"contains",units==0?"none":contains);
        state=HouseholdBlock.set(state,"level",Integer.toString(block.function.equals("bathtub")?(units+2)/3:units));
        level.setBlockAndUpdate(pos,state);data.changed();
        if(block.function.equals("bathtub")){
            BlockPos other=pos.relative(HouseholdBlock.facing(state));if(level.getBlockState(other).is(block))level.setBlockAndUpdate(other,HouseholdBlock.set(state,"part","back"));
        }
    }
    static void pump(ServerLevel l,BlockPos pos,BlockState state,OwnerHome.Data data){
        OwnerHomeBlock block=(OwnerHomeBlock)state.getBlock();boolean spout=block.function.equals("fountain_spout");
        if(!spout&&!HouseholdBlock.flag(state,"triggered")||!block.root(pos,state).equals(pos))return;
        if(!Authority.mayChangeFor(l,data.owner,pos))return;
        Direction back=HouseholdBlock.facing(state).getOpposite();
        var sources=spout?java.util.List.of(pos.relative(switch(HouseholdBlock.value(state,"face")){case "floor"->Direction.DOWN;case "ceiling"->Direction.UP;default->back;})):
            java.util.List.of(pos.relative(back),pos.relative(back,2),pos.below(),pos.below(2));
        BlockPos target=pos;
        if(spout){
            for(int i=1;i<=16;i++){
                BlockPos q=pos.below(i);if(!l.hasChunkAt(q)||!Authority.mayChangeFor(l,data.owner,q))return;
                if(!l.getBlockState(q).isAir()){target=q;break;}
            }
            if(target.equals(pos))return;
        }
        BlockState targetState=l.getBlockState(target);
        OwnerHome.Data targetData=l.getBlockEntity(target) instanceof OwnerHome.Data t?t:null;
        int max=targetState.getBlock() instanceof OwnerHomeBlock home&&home.function.equals("bathtub")?6:3;
        boolean targetBasin=targetState.getBlock() instanceof OwnerHomeBlock home&&java.util.Set.of("sink","sink_counter","bathtub","fountain").contains(home.function);
        if(!targetBasin&&!targetState.is(Blocks.CAULDRON))return;
        if(targetBasin&&targetData==null)return;
        int current=targetData==null?0:targetData.fluidUnits;
        if(current+3>max)return;
        for(BlockPos source:sources){
            if(source.equals(target)||!l.hasChunkAt(source)||!Authority.mayChangeFor(l,data.owner,source))continue;
            BlockState from=l.getBlockState(source);OwnerHome.Data fromData=l.getBlockEntity(source) instanceof OwnerHome.Data f?f:null;
            String fluid="";
            if(fromData!=null&&fromData.fluidUnits>=3)fluid=HouseholdBlock.value(from,"contains");
            else if(from.is(Blocks.WATER_CAULDRON)&&from.getValue(BlockStateProperties.LEVEL_CAULDRON)==3)fluid="water";
            else if(from.is(Blocks.LAVA_CAULDRON))fluid="lava";
            if(fluid.isEmpty()||fluid.equals("none")||current>0&&!fluid.equals(HouseholdBlock.value(targetState,"contains")))continue;
            if(targetState.is(Blocks.CAULDRON)&&!fluid.equals("water")&&!fluid.equals("lava"))continue;
            // Deduct before credit, within the same server tick and after checking both claims.
            if(fromData!=null){fromData.fluidUnits-=3;refresh((OwnerHomeBlock)from.getBlock(),l,source,fromData,fluid);}
            else l.setBlockAndUpdate(source,Blocks.CAULDRON.defaultBlockState());
            if(targetBasin&&targetData!=null){targetData.fluidUnits+=3;refresh((OwnerHomeBlock)targetState.getBlock(),l,target,targetData,fluid);}
            else l.setBlockAndUpdate(target,fluid.equals("lava")?Blocks.LAVA_CAULDRON.defaultBlockState():Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON,3));
            if(spout){
                l.setBlockAndUpdate(pos,HouseholdBlock.set(state,"contains",fluid));
                for(int y=target.getY()+1;y<=pos.getY();y++)l.sendParticles(fluid.equals("lava")?net.minecraft.core.particles.ParticleTypes.FALLING_LAVA:net.minecraft.core.particles.ParticleTypes.FALLING_WATER,pos.getX()+.5,y+.5,pos.getZ()+.5,6,.04,.25,.04,0);
            }return;
        }
        if(spout&&!HouseholdBlock.value(state,"contains").equals("none"))l.setBlockAndUpdate(pos,HouseholdBlock.set(state,"contains","none"));
    }
}
