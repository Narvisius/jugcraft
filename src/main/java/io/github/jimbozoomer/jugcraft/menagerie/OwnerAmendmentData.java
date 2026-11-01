package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** A real underlying block and one added decoration/tool; neither is fabricated on removal. */
public final class OwnerAmendmentData extends BlockEntity {
    public BlockState base=Blocks.AIR.defaultBlockState(),decoration=Blocks.AIR.defaultBlockState();
    public ItemStack liquidServing=ItemStack.EMPTY;
    public ItemStack displayed=ItemStack.EMPTY,secondary=ItemStack.EMPTY;
    public int liquidColor=0xFFFFFF,spent;
    public net.minecraft.world.item.alchemy.PotionContents potion=net.minecraft.world.item.alchemy.PotionContents.EMPTY;
    private boolean restoring;
    public OwnerAmendmentData(BlockPos pos,BlockState state){super(OwnerAmendments.DATA,pos,state);}
    public BlockState actualBase(){return OwnerAmendments.copy(getBlockState(),base);}
    public void changed(){setChanged();if(level!=null){var s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,Block.UPDATE_CLIENTS);level.updateNeighbourForOutputSignal(worldPosition,s.getBlock());}}
    public boolean restore(Player player){
        if(level==null||level.isClientSide()||!Menagerie.mayChange(level,player,worldPosition))return false;
        var block=actualBase();var item=displayed.copy();var cover=new ItemStack(decoration.getBlock(),decoration.hasProperty(CandleBlock.CANDLES)?decoration.getValue(CandleBlock.CANDLES):1);
        restoring=true;
        if(!level.setBlock(worldPosition,block,3)){restoring=false;return false;}
        if(block.getBlock() instanceof AbstractSkullBlock){if(level.getBlockEntity(worldPosition)!=null)level.getBlockEntity(worldPosition).applyComponentsFromItemStack(item);}
        else if(!item.isEmpty())player.getInventory().placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);
        if(!secondary.isEmpty())player.getInventory().placeItemBackInInventory(secondary.copy(),net.minecraft.util.Prediction.SERVER_ONLY);
        if(!cover.isEmpty())player.getInventory().placeItemBackInInventory(cover,net.minecraft.util.Prediction.SERVER_ONLY);
        return true;
    }
    public static List<ItemStack> drops(LootParams.Builder builder){
        if(!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof OwnerAmendmentData data))return List.of();
        var state=data.actualBase();if(state.hasBlockEntity()||state.isAir())return List.of();
        return state.getDrops(builder.withOptionalParameter(LootContextParams.BLOCK_ENTITY,null).withParameter(LootContextParams.BLOCK_STATE,state));
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(!restoring&&level!=null&&!level.isClientSide()){
            if(!secondary.isEmpty())Block.popResource(level,pos,secondary.copy());
            if(!displayed.isEmpty())Block.popResource(level,pos,displayed.copy());
            if(!decoration.isAir())Block.popResource(level,pos,new ItemStack(decoration.getBlock(),decoration.hasProperty(CandleBlock.CANDLES)?decoration.getValue(CandleBlock.CANDLES):1));
        }
        super.preRemoveSideEffects(pos,state);
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("LiquidServing",ItemStack.OPTIONAL_CODEC,liquidServing);out.putInt("LiquidColor",liquidColor);out.putInt("Spent",spent);out.store("Potion",net.minecraft.world.item.alchemy.PotionContents.CODEC,potion);out.store("Base",BlockState.CODEC,base);out.store("Decoration",BlockState.CODEC,decoration);out.store("Displayed",ItemStack.OPTIONAL_CODEC,displayed);out.store("Secondary",ItemStack.OPTIONAL_CODEC,secondary);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);liquidServing=in.read("LiquidServing",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);if(!liquidServing.isEmpty()){if(OwnerAmendmentCauldrons.soup(liquidServing)==null)liquidServing=ItemStack.EMPTY;else liquidServing.setCount(1);}liquidColor=in.getIntOr("LiquidColor",0xFFFFFF)&0xFFFFFF;spent=Math.clamp(in.getIntOr("Spent",0),0,3);potion=in.read("Potion",net.minecraft.world.item.alchemy.PotionContents.CODEC).orElse(net.minecraft.world.item.alchemy.PotionContents.EMPTY);base=in.read("Base",BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());decoration=in.read("Decoration",BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());displayed=in.read("Displayed",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);secondary=in.read("Secondary",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);if(!secondary.isEmpty())secondary.setCount(1);if(!displayed.isEmpty())displayed.setCount(1);if(base.hasBlockEntity()&&!(base.getBlock() instanceof AbstractSkullBlock)||OwnerAmendments.BLOCKS.containsValue(base.getBlock()))base=Blocks.AIR.defaultBlockState();}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
