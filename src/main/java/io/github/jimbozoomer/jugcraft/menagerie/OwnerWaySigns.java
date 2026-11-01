package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Two paid, independently rotatable original wooden direction signs on a retained post. */
public final class OwnerWaySigns {
    public static final String[] WOODS={"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped"};
    private OwnerWaySigns(){}
    static void register(){for(var wood:WOODS){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("supps_way_sign_"+wood));var item=Registry.register(BuiltInRegistries.ITEM,key,new Board(wood,new Item.Properties().setId(key)));OwnerSupplementaries.ITEMS.put("way_sign_"+wood,item);CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->e.accept(item));}}
    public static boolean sign(OwnerSupplementData d){return d.block().function.equals("way_sign");}
    public static int slot(BlockPos p,BlockHitResult hit){return hit.getLocation().y-p.getY()>=.5?0:1;}
    public static boolean base(BlockState s){return !(s.getBlock() instanceof EntityBlock)&&(s.getBlock() instanceof FenceBlock||s.getBlock() instanceof WallBlock);}
    static VoxelShape shape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(level.getBlockEntity(pos) instanceof OwnerSupplementData data&&base(data.wayBase))return Shapes.or(data.wayBase.getShape(level,pos,context),Block.box(0,2,0,16,15,16));
        return Block.box(0,2,6,16,15,10);
    }
    static VoxelShape collision(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return level.getBlockEntity(pos) instanceof OwnerSupplementData d&&base(d.wayBase)?d.wayBase.getCollisionShape(level,pos,context):Shapes.empty();}
    static InteractionResult use(OwnerSupplementData d,Player player,ItemStack held,InteractionHand hand,BlockHitResult hit){
        if(!d.mayEdit(player))return InteractionResult.FAIL;
        int slot=slot(d.getBlockPos(),hit);if(d.getItem(slot).isEmpty()&&!d.getItem(1-slot).isEmpty())slot=1-slot;
        if(held.getItem() instanceof Board)return InteractionResult.PASS;
        if(d.getItem(slot).isEmpty())return InteractionResult.PASS;
        if(d.getLevel().isClientSide())return InteractionResult.SUCCESS;
        if(held.getItem() instanceof OwnerSupplementWrench){d.wayAngles[slot]=Math.floorMod(d.wayAngles[slot]+(player.isShiftKeyDown()?-1:1),16);d.boardRevision++;d.changed();held.hurtAndBreak(1,player,hand.asEquipmentSlot());d.getLevel().playSound(null,d.getBlockPos(),OwnerSupplementaries.SOUNDS.get("wrench_rotate"),net.minecraft.sounds.SoundSource.PLAYERS,1,1.4F);return InteractionResult.SUCCESS;}
        if(held.is(Items.SHEARS)&&player.isShiftKeyDown()){
            var taken=d.removeItemNoUpdate(slot);var previousText=d.wayTexts[slot];d.wayTexts[slot]="";d.boardRevision++;
            if(d.isEmpty()){var replacement=base(d.wayBase)?HouseholdBlock.set(d.wayBase,"waterlogged",HouseholdBlock.value(d.getBlockState(),"waterlogged")):d.getBlockState().getFluidState().createLegacyBlock();if(!d.getLevel().setBlockAndUpdate(d.getBlockPos(),replacement)){d.setItem(slot,taken);d.wayTexts[slot]=previousText;d.changed();return InteractionResult.FAIL;}}else d.changed();
            player.getInventory().placeItemBackInInventory(taken,net.minecraft.util.Prediction.SERVER_ONLY);return InteractionResult.SUCCESS;
        }
        boolean changed=false;
        if(held.is(Items.HONEYCOMB)&&!d.boardWaxed){d.boardWaxed=true;held.consume(1,player);changed=true;}
        else if(held.is(net.minecraft.tags.ItemTags.AXES)&&d.boardWaxed){d.boardWaxed=false;held.hurtAndBreak(1,player,hand.asEquipmentSlot());changed=true;}
        else if(!d.boardWaxed&&held.getItem() instanceof DyeItem dye){d.textColor=0xFF000000|dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextColor();held.consume(1,player);changed=true;}
        else if(!d.boardWaxed&&held.is(Items.GLOW_INK_SAC)&&!d.boardGlow){d.boardGlow=true;held.consume(1,player);changed=true;}
        else if(!d.boardWaxed&&held.is(Items.INK_SAC)&&d.boardGlow){d.boardGlow=false;held.consume(1,player);changed=true;}
        if(changed){d.boardRevision++;d.changed();return InteractionResult.SUCCESS;}
        if(held.isEmpty()&&player instanceof ServerPlayer server){OwnerSupplementText.openSign(server,d,slot);return InteractionResult.SUCCESS;}
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    public static final class Board extends Item {
        public final String wood;Board(String wood,Properties props){super(props);this.wood=wood;}
        @Override public InteractionResult useOn(UseOnContext context){
            var level=context.getLevel();var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
            var pos=context.getClickedPos();var old=level.getBlockState(pos);int slot=context.getClickLocation().y-pos.getY()>=.5?0:1;
            boolean existing=level.getBlockEntity(pos) instanceof OwnerSupplementData d&&sign(d);
            boolean wall=false;
            if(!existing&&!base(old)){
                var face=context.getClickedFace();if(!face.getAxis().isHorizontal()||!old.isFaceSturdy(level,pos,face))return InteractionResult.PASS;
                pos=pos.relative(face);if(!level.hasChunkAt(pos))return InteractionResult.FAIL;old=level.getBlockState(pos);
                existing=level.getBlockEntity(pos) instanceof OwnerSupplementData target&&sign(target);wall=true;
                if(!existing&&!old.isAir()&&!old.is(Blocks.WATER))return InteractionResult.FAIL;
            }
            if(!Menagerie.mayChange(level,player,pos))return InteractionResult.FAIL;
            if(existing&&level.getBlockEntity(pos) instanceof OwnerSupplementData d&&(!d.mayEdit(player)||!d.getItem(slot).isEmpty()))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            if(!existing){
                var state=OwnerSupplementaries.BLOCKS.get(wall?"way_sign_wall":"way_sign").defaultBlockState();if(wall)state=HouseholdBlock.set(state,"facing",context.getClickedFace().getName());state=HouseholdBlock.set(state,"waterlogged",Boolean.toString(!old.getFluidState().isEmpty()));
                if(!level.setBlockAndUpdate(pos,state))return InteractionResult.FAIL;
                if(!(level.getBlockEntity(pos) instanceof OwnerSupplementData d)){level.setBlockAndUpdate(pos,old);return InteractionResult.FAIL;}
                d.wayBase=wall?Blocks.AIR.defaultBlockState():old;d.owner=player.getUUID();
            }
            var data=(OwnerSupplementData)level.getBlockEntity(pos);data.wayAngles[slot]=Math.floorMod(Math.round((wall?context.getClickedFace().toYRot():player.getYRot())/22.5F)+8,16);data.wayTexts[slot]="";data.setItem(slot,context.getItemInHand().copyWithCount(1));data.boardRevision++;data.changed();context.getItemInHand().consume(1,player);return InteractionResult.SUCCESS;
        }
    }
}
