package io.github.jimbozoomer.jugcraft.menagerie;

import java.util.*;
import io.github.jimbozoomer.jugcraft.backpack.OwnerBackpacks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Real saved book stacks, retaining written text, enchantments and the supplied cover variants. */
public final class OwnerSupplementBooks {
    public record Visual(Identifier model,boolean glint){}
    public record Type(boolean horizontal,float power,List<Visual> visuals){}
    private static final Map<Identifier,Type> TYPES=new HashMap<>();
    private OwnerSupplementBooks(){}
    public static Type type(ItemStack stack){return stack.isEmpty()?null:TYPES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));}
    public static void register(){
        for(var value:OwnerBackpacks.source("supps_books.json").getAsJsonArray("books")){
            var data=value.getAsJsonObject();var visuals=new ArrayList<Visual>();
            for(var v:data.getAsJsonArray("visuals")){var visual=v.getAsJsonObject();visuals.add(new Visual(Identifier.parse(visual.get("item_model").getAsString()),visual.get("glint").getAsBoolean()));}
            TYPES.put(Identifier.parse(data.get("item").getAsString()),new Type(data.get("horizontal").getAsBoolean(),data.get("power").getAsFloat(),List.copyOf(visuals)));
        }
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            var stack=player.getItemInHand(hand);var type=type(stack);
            if(type==null||!player.isShiftKeyDown()||player.isSpectator())return InteractionResult.PASS;
            var pos=hit.getBlockPos();
            if(level.getBlockEntity(pos) instanceof OwnerSupplementData existing&&existing.block().function.equals("books")){
                if(!existing.mayEdit(player))return InteractionResult.FAIL;
                return level.isClientSide()?InteractionResult.SUCCESS:insert(existing,stack,player);
            }
            pos=pos.relative(hit.getDirection());if(!level.hasChunkAt(pos)||!Menagerie.mayChange(level,player,pos)||!level.getBlockState(pos).isAir())return InteractionResult.FAIL;
            // Click a side to stand books on their spines; top placement forms a horizontal pile.
            var block=OwnerSupplementaries.BLOCKS.get(type.horizontal()&&hit.getDirection().getAxis().isHorizontal()?"book_pile_horizontal":"book_pile");
            var state=HouseholdBlock.set(block.defaultBlockState(),"facing",player.getDirection().getOpposite().getName());
            if(!state.canSurvive(level,pos))return InteractionResult.FAIL;
            if(!level.isClientSide()){
                level.setBlockAndUpdate(pos,state);
                if(level.getBlockEntity(pos) instanceof OwnerSupplementData books){books.owner=player.getUUID();insert(books,stack,player);}
            }
            return InteractionResult.SUCCESS;
        });
    }
    static InteractionResult insert(OwnerSupplementData books,ItemStack held,Player player){
        var type=type(held);if(type==null||books.block().name.endsWith("horizontal")&&!type.horizontal())return InteractionResult.TRY_WITH_EMPTY_HAND;
        for(int slot=0;slot<4;slot++)if(books.getItem(slot).isEmpty()){
            books.setItem(slot,held.copyWithCount(1));held.consume(1,player);return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
    static InteractionResult take(OwnerSupplementData books,Player player,BlockHitResult hit){
        int count=0;for(int i=0;i<4;i++)if(!books.getItem(i).isEmpty())count++;
        if(count==0)return InteractionResult.PASS;
        var pos=books.getBlockPos();int index;
        if(books.block().name.endsWith("horizontal")){
            var facing=HouseholdBlock.facing(books.getBlockState());double distance=facing.getAxis()==Direction.Axis.Z?hit.getLocation().x-pos.getX():hit.getLocation().z-pos.getZ();
            if(facing==Direction.NORTH||facing==Direction.EAST)distance=1-distance;
            double left=(switch(count){case 1->6;case 2->3;case 3->1;default->0;})/16D;index=Math.clamp((int)((distance-left)*4),0,count-1);
        }else index=Math.clamp((int)((hit.getLocation().y-pos.getY())*4),0,count-1);
        var result=books.removeItemNoUpdate(index);
        for(int i=index;i<3;i++)books.setItem(i,books.removeItemNoUpdate(i+1));books.changed();
        player.getInventory().placeItemBackInInventory(result,net.minecraft.util.Prediction.SERVER_ONLY);
        if(count==1)books.getLevel().removeBlock(pos,false);
        return InteractionResult.SUCCESS;
    }
    public static VoxelShape shape(BlockState state){
        int count=Integer.parseInt(HouseholdBlock.value(state,"books"));
        if(((OwnerSupplementBlock)state.getBlock()).name.endsWith("horizontal")){
            double min=switch(count){case 1->6;case 2->3;case 3->1;default->0;};
            return HouseholdBlock.facing(state).getAxis()==Direction.Axis.X?Block.box(4,0,min,12,10,16-min):Block.box(min,0,4,16-min,10,12);
        }
        return Block.box(3,0,3,13,count*4,13);
    }
}
