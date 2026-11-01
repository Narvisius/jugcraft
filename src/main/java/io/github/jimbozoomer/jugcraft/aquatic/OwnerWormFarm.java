package io.github.jimbozoomer.jugcraft.aquatic;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Native compost costs and sided automation, yielding bait instead of bone meal. */
public final class OwnerWormFarm extends ComposterBlock {
    public OwnerWormFarm(Properties p){super(p);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
        if(stack.has(DataComponents.COMPOSTABLE)&&state.getValue(LEVEL)<8)return super.useItemOn(stack,state,level,pos,player,hand,hit);
        return harvest(state,level,pos,player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return harvest(state,level,pos,player);}
    private InteractionResult harvest(BlockState state,Level level,BlockPos pos,Player player){
        if(player instanceof ServerPlayer p&&!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;
        int count=state.getValue(LEVEL);if(count<=0)return InteractionResult.PASS;
        if(!level.isClientSide()){level.setBlock(pos,state.setValue(LEVEL,count-1),3);popResource(level,pos.above(),new ItemStack(OwnerAquaculture.ITEMS.get("worm")));level.playSound(null,pos,OwnerAquaticSounds.sound("worm_farm_empty"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}
        return InteractionResult.SUCCESS;
    }
    @Override public WorldlyContainer getContainer(BlockState state,LevelAccessor level,BlockPos pos){return new WormPort(super.getContainer(state,level,pos));}
    private record WormPort(WorldlyContainer delegate) implements WorldlyContainer {
        private ItemStack out(ItemStack s){return s.is(Items.BONE_MEAL)?s.transmuteCopy(OwnerAquaculture.ITEMS.get("worm"),s.getCount()):s;}
        private ItemStack in(ItemStack s){return s.is(OwnerAquaculture.ITEMS.get("worm"))?s.transmuteCopy(Items.BONE_MEAL,s.getCount()):s;}
        public int[] getSlotsForFace(Direction side){return delegate.getSlotsForFace(side);}
        public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return delegate.canPlaceItemThroughFace(i,in(s),d);}
        public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return delegate.canTakeItemThroughFace(i,in(s),d);}
        public int getContainerSize(){return delegate.getContainerSize();}
        public boolean isEmpty(){return delegate.isEmpty();}
        public ItemStack getItem(int i){return out(delegate.getItem(i));}
        public ItemStack removeItem(int i,int n){return out(delegate.removeItem(i,n));}
        public ItemStack removeItemNoUpdate(int i){return out(delegate.removeItemNoUpdate(i));}
        public void setItem(int i,ItemStack s){delegate.setItem(i,in(s));}
        public int getMaxStackSize(){return delegate.getMaxStackSize();}
        public void setChanged(){delegate.setChanged();}
        public boolean stillValid(Player p){return delegate.stillValid(p);}
        public boolean canPlaceItem(int i,ItemStack s){return delegate.canPlaceItem(i,in(s));}
        public void clearContent(){delegate.clearContent();}
    }
}
