package io.github.jimbozoomer.jugcraft.enchanting;

import java.util.*;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native persistent enchanting, paid rerolls and supplied bookshelf-power rules. */
public final class OwnerEnchanting {
    public static final int REROLL_BUTTON=3, REROLL_LAPIS=1, REROLL_XP=5;
    private OwnerEnchanting() {}
    public interface Storage { Inventory jugcraft$enchantingInventory(); }
    public static final class Inventory extends SimpleContainer {
        private final BlockEntity owner;
        private final Set<EnchantmentMenu> viewers=Collections.newSetFromMap(new WeakHashMap<>());
        public Inventory(BlockEntity owner){super(2);this.owner=owner;}
        public void watch(EnchantmentMenu menu){viewers.add(menu);menu.slotsChanged(this);}
        public void unwatch(EnchantmentMenu menu){viewers.remove(menu);}
        @Override public void setChanged(){
            super.setChanged();owner.setChanged();
            if(owner.getLevel()!=null&&!owner.getLevel().isClientSide())owner.getLevel().sendBlockUpdated(owner.getBlockPos(),owner.getBlockState(),owner.getBlockState(),Block.UPDATE_CLIENTS);
            for(var menu:List.copyOf(viewers))menu.slotsChanged(this);
        }
        @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(owner,player)&&Authority.mayChange(player.level(),player,owner.getBlockPos());}
        @Override public boolean canPlaceItem(int slot,ItemStack item){return slot==0?item.isEnchantable():slot==1&&item.is(Items.LAPIS_LAZULI);}
        @Override public int getMaxStackSize(ItemStack stack){return stack.is(Items.LAPIS_LAZULI)?64:1;}
    }
    public static int experience(Player player){
        int level=player.experienceLevel;
        double total=level<=16?(double)level*level+6L*level:level<=31?2.5*level*level-40.5*level+360:4.5*level*level-162.5*level+2220;
        return (int)Math.min(Integer.MAX_VALUE,total+Math.floor(player.experienceProgress*player.getXpNeededForNextLevel()));
    }
    public static int bookshelfPower(Level level,BlockPos table){
        double power=0;
        for(BlockPos offset:EnchantingTableBlock.BOOKSHELF_OFFSETS){
            BlockPos shelf=table.offset(offset),gap=table.offset(offset.getX()/2,offset.getY(),offset.getZ()/2);
            if(!level.hasChunkAt(shelf)||!level.getBlockState(gap).getCollisionShape(level,gap).isEmpty()&&level.getBlockState(gap).isCollisionShapeFullBlock(level,gap))continue;
            var state=level.getBlockState(shelf);
            if(state.is(Blocks.CHISELED_BOOKSHELF)&&level.getBlockEntity(shelf) instanceof ChiseledBookShelfBlockEntity books){
                Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if(offset.getX()*facing.getStepX()+offset.getZ()*facing.getStepZ()>=0)continue;
                int count=0;for(int i=0;i<6;i++)if(!books.getItem(i).isEmpty())count++;power+=count/3;
            }else if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.botany.BotanicalBlock botanical&&botanical.kind.endsWith("_pylon"))power+=botanical.kind.equals("mana_pylon")?8:15;
            else if(level.getBlockEntity(shelf) instanceof io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementData pile&&pile.block().function.equals("books")){for(int slot=0;slot<4;slot++){var type=io.github.jimbozoomer.jugcraft.menagerie.OwnerSupplementBooks.type(pile.getItem(slot));if(type!=null)power+=type.power();}}else if(state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER))power++;
        }return (int)Math.min(15,power);
    }
}
