package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Original states/collision with native inventory, display, timing and rotation interactions. */
public final class OwnerSupplementBlock extends HouseholdBlock implements EntityBlock,OptionalWaterloggedBlock {
    public final String function;public final int capacity;
    OwnerSupplementBlock(Properties p,JsonObject d){super(d.get("function").getAsString().startsWith("spring")?p.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE):p,d);function=d.get("function").getAsString();capacity=d.get("capacity").getAsInt();registerDefaultState(set(set(set(defaultBlockState(),"enabled","false"),"lit",function.equals("statue")?"false":"true"),"facing",(function.equals("hourglass")||function.equals("cannon"))?"up":value(defaultBlockState(),"facing")));}
    private static int flowerSlot(BlockState s,BlockPos p,BlockHitResult hit){var dir=facing(s);double v=dir.getAxis()==Direction.Axis.X?hit.getLocation().z-p.getZ():hit.getLocation().x-p.getX();int slot=Math.clamp((int)(v*3),0,2);return dir==Direction.SOUTH||dir==Direction.WEST?2-slot:slot;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){
        var s=super.getStateForPlacement(c);if(s==null)return null;
        s=set(s,"waterlogged",Boolean.toString(c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER));
        if(function.equals("cannon"))s=set(s,"facing",c.getClickedFace().getName());
        if(function.equals("pulley"))s=set(s,"axis",(c.getClickedFace().getAxis().isVertical()?c.getHorizontalDirection().getClockWise().getAxis():c.getClickedFace().getAxis()).getName());
        if(function.equals("flowers"))s=set(s,"face",c.getClickedFace()==Direction.UP?"floor":c.getClickedFace()==Direction.DOWN?"ceiling":"wall");
        if(function.equals("mob_head")){s=set(s,"rotation",Integer.toString(net.minecraft.world.level.block.state.properties.RotationSegment.convertToSegment(c.getRotation())));if(name.contains("wall"))s=set(s,"facing",c.getClickedFace().getName());}
        if(function.equals("hourglass"))s=set(s,"facing","up");if(function.equals("spikes"))s=set(s,"facing",c.getClickedFace().getName());
        if(function.equals("bellows")||function.equals("spring")||function.equals("turntable"))s=set(s,"facing",c.getNearestLookingDirection().getOpposite().getName());
        if(name.startsWith("sconce_wall")||function.equals("faucet"))s=set(s,"facing",c.getClickedFace().getName());
        if(function.equals("rune_lamp")){var prop=getStateDefinition().getProperty("rune");if(prop!=null){var choices=prop.getPossibleValues();s=set(s,"rune",choices.get(c.getLevel().getRandom().nextInt(choices.size())).toString());}}
        if(function.startsWith("sconce")&&flag(s,"waterlogged"))s=set(s,"lit","false");
        return function.equals("fuse")?OwnerGunpowder.connections(s,c.getLevel(),c.getClickedPos()):s;
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){if(function.equals("fuse")){var result=s;for(var dir:Direction.Plane.HORIZONTAL)result=set(result,r.rotate(dir).getName(),value(s,dir.getName()));return result;}return function.equals("mob_head")&&!name.contains("wall")?set(s,"rotation",Integer.toString(r.rotate(Integer.parseInt(value(s,"rotation")),16))):super.rotate(s,r);}
    @Override protected BlockState mirror(BlockState s,Mirror m){if(function.equals("fuse")){var result=s;for(var dir:Direction.Plane.HORIZONTAL)result=set(result,m.mirror(dir).getName(),value(s,dir.getName()));return result;}return function.equals("mob_head")&&!name.contains("wall")?set(s,"rotation",Integer.toString(m.mirror(Integer.parseInt(value(s,"rotation")),16))):super.mirror(s,m);}
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return flag(s,"waterlogged")?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction dir,BlockPos q,BlockState neighbor,net.minecraft.util.RandomSource random){
        if(function.equals("fuse"))return OwnerGunpowder.supported(l,p)?OwnerGunpowder.connections(s,l,p):Blocks.AIR.defaultBlockState();
        if(flag(s,"waterlogged"))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(l));
        if(function.equals("mob_head")&&!canSurvive(s,l,p))return Blocks.AIR.defaultBlockState();
        if(function.equals("way_sign")){
            if(name.equals("way_sign_wall")&&!canSurvive(s,l,p))return s.getFluidState().createLegacyBlock();
            if(l instanceof Level level&&l.getBlockEntity(p) instanceof OwnerSupplementData d&&OwnerWaySigns.base(d.wayBase)){
                var base=d.wayBase.updateShape(l,ticks,p,dir,q,neighbor,random);base=HouseholdBlock.set(base,"waterlogged",value(s,"waterlogged"));
                if(base!=d.wayBase){d.wayBase=base;d.setChanged();if(!level.isClientSide())level.sendBlockUpdated(p,s,s,Block.UPDATE_CLIENTS);}
            }
        }
        if(function.equals("planter")&&dir==Direction.UP){var shape=neighbor.getShape(l,q);s=set(s,"extended",Boolean.toString(!shape.isEmpty()&&shape.bounds().minY<.06&&!(neighbor.getBlock() instanceof CropBlock)&&!(neighbor.getBlock() instanceof StemBlock)));}
        if((function.equals("doormat")||function.equals("books")||function.startsWith("sconce"))&&!canSurvive(s,l,p))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,l,ticks,p,dir,q,neighbor,random);
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){if(function.equals("fuse"))return OwnerGunpowder.supported(l,p);if(name.equals("way_sign_wall")||function.equals("mob_head")&&name.contains("wall")){var face=facing(s);return l.getBlockState(p.relative(face.getOpposite())).isFaceSturdy(l,p.relative(face.getOpposite()),face);}if(function.equals("rope_bunting"))return OwnerSupplementRopes.supported(l,p);if(function.equals("doormat"))return !l.getBlockState(p.below()).isAir();if(function.equals("books"))return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);if(function.startsWith("sconce")){var support=name.startsWith("sconce_wall")||function.equals("sconce_lever")?facing(s).getOpposite():Direction.DOWN;return l.getBlockState(p.relative(support)).isFaceSturdy(l,p.relative(support),support.getOpposite());}return super.canSurvive(s,l,p);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OwnerSupplementData(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide()&&t==OwnerSupplementaries.DATA?(level,pos,state,data)->((OwnerSupplementData)data).tick((ServerLevel)level):null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity who,ItemStack stack){super.setPlacedBy(l,p,s,who,stack);if(l.getBlockEntity(p) instanceof OwnerSupplementData d){d.owner=who==null?null:who.getUUID();if(function.equals("statue"))d.setCustomName(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));if(function.equals("cannon")&&who!=null){var local=OwnerCannon.mounted(who.getLookAngle(),OwnerCannon.mount(s),true);var world=OwnerCannon.mounted(OwnerCannon.direction(OwnerCannon.yaw(local),Math.clamp(OwnerCannon.pitch(local),-85,40)),OwnerCannon.mount(s),false);d.cannonYaw=OwnerCannon.yaw(world);d.cannonPitch=OwnerCannon.pitch(world);}if(d.portable())d.restore(stack);else if(function.equals("hidden_bricks")){var contents=NonNullList.withSize(1,ItemStack.EMPTY);stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(contents);d.setItem(0,contents.getFirst());}d.setChanged();}}
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof OwnerSupplementData d)||!d.allowed(who))return InteractionResult.FAIL;
        if(!d.mayEdit(who))return InteractionResult.TRY_WITH_EMPTY_HAND;
        if(l.isClientSide())return InteractionResult.SUCCESS;
        if(function.equals("pulley"))return OwnerPulleys.use(d,who,held);if(function.equals("slidy"))return OwnerSlidingBlocks.use(d,who,hit);if(function.equals("fuse"))return OwnerGunpowder.use(d,who,held,hand);if(function.equals("hidden_bricks")||function.equals("mob_head")||function.equals("bubble"))return InteractionResult.PASS;if(function.equals("cannon"))return OwnerCannon.use(d,who,held,hand);if(function.equals("way_sign"))return OwnerWaySigns.use(d,who,held,hand,hit);if(function.equals("globe"))return OwnerGlobes.use(d,who,held,hand);
        if(function.equals("rope_bunting"))return held.getItem() instanceof OwnerBuntings.Pennant?OwnerBuntings.put(l,p,who,held,hit):OwnerSupplementRopes.extend(s,l,p,who,held,hand);
        if(function.equals("jar")||function.equals("goblet")){var result=OwnerSoftLiquids.use(d,who,held,hand);if(result!=InteractionResult.PASS)return result;if(function.equals("goblet"))return InteractionResult.TRY_WITH_EMPTY_HAND;}
        if(OwnerSupplementText.text(d)){
            if(function.equals("doormat")&&hit.getDirection()!=Direction.UP&&!held.isEmpty()&&d.getItem(0).isEmpty()){d.setItem(0,held.copyWithCount(1));held.consume(1,who);return InteractionResult.SUCCESS;}
            boolean changed=false;
            if(held.is(Items.HONEYCOMB)&&!d.boardWaxed){d.boardWaxed=true;held.consume(1,who);changed=true;}
            else if(held.is(net.minecraft.tags.ItemTags.AXES)&&d.boardWaxed){d.boardWaxed=false;held.hurtAndBreak(1,who,hand.asEquipmentSlot());changed=true;}
            else if(!d.boardWaxed&&held.getItem() instanceof DyeItem dye){d.textColor=0xFF000000|dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getTextColor();held.consume(1,who);changed=true;}
            else if(!d.boardWaxed&&held.is(Items.GLOW_INK_SAC)&&!d.boardGlow){d.boardGlow=true;held.consume(1,who);changed=true;}
            else if(!d.boardWaxed&&held.is(Items.INK_SAC)&&d.boardGlow){d.boardGlow=false;held.consume(1,who);changed=true;}
            if(changed){d.boardRevision++;d.changed();return InteractionResult.SUCCESS;}return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if(function.equals("books"))return OwnerSupplementBooks.insert(d,held,who);
        if(function.equals("spikes"))return OwnerSupplementSpikes.use(d,held,who,hand);
        if(function.equals("blackboard")){
            boolean changed=false;
            if(held.is(Items.HONEYCOMB)&&!d.boardWaxed){d.boardWaxed=true;held.consume(1,who);changed=true;}
            else if(held.is(net.minecraft.tags.ItemTags.AXES)&&d.boardWaxed){d.boardWaxed=false;held.hurtAndBreak(1,who,hand.asEquipmentSlot());changed=true;}
            else if(!d.boardWaxed&&held.is(Items.GLOW_INK_SAC)&&!d.boardGlow){d.boardGlow=true;held.consume(1,who);changed=true;}
            else if(!d.boardWaxed&&held.is(Items.INK_SAC)&&d.boardGlow){d.boardGlow=false;held.consume(1,who);changed=true;}
            else if(!d.boardWaxed&&held.getItem() instanceof DyeItem dye&&hit.getDirection()==facing(s)){
                var dir=facing(s);double x=hit.getLocation().x-p.getX(),z=hit.getLocation().z-p.getZ();double u=switch(dir){case NORTH->1-x;case SOUTH->x;case EAST->1-z;default->z;};
                int col=Math.clamp((int)(u*16),0,15),row=Math.clamp((int)((1-(hit.getLocation().y-p.getY()))*16),0,15);d.boardPixels[row*16+col]=(byte)OwnerBlackboards.index(dye.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE));changed=true;
            }
            if(changed){d.boardRevision++;d.changed();return InteractionResult.SUCCESS;}
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if(function.equals("jar")){
            var context=net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofPlayerHand(who,hand);var heldFluid=net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.find(held,context);
            if(heldFluid!=null&&d.softPortions==0&&d.isEmpty()&&d.captive==null){try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                long moved=net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(heldFluid,d.fluid,v->true,net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET,tx);
                if(moved==0)moved=net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(d.fluid,heldFluid,v->true,net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET,tx);
                if(moved>0){tx.commit();return InteractionResult.SUCCESS;}
            }}
            for(int slot=0;slot<d.getContainerSize();slot++)if(d.getItem(slot).isEmpty()&&d.canPlaceItem(slot,held)){d.setItem(slot,held.split(1));return InteractionResult.SUCCESS;}
        }
        if(function.equals("statue")&&held.is(Items.NAME_TAG)){
            d.setCustomName(held.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));d.changed();held.consume(1,who);return InteractionResult.SUCCESS;
        }
        if(function.equals("flowers")){int slot=flowerSlot(s,p,hit);if(d.canPlaceItem(slot,held)&&d.getItem(slot).isEmpty()){d.setItem(slot,held.split(1));d.changed();return InteractionResult.SUCCESS;}return InteractionResult.TRY_WITH_EMPTY_HAND;}
        if(function.startsWith("sconce")&&(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE))&&!flag(s,"lit")&&!flag(s,"waterlogged")){l.setBlockAndUpdate(p,set(s,"lit","true"));if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,who,hand.asEquipmentSlot());else held.consume(1,who);return InteractionResult.SUCCESS;}
        if(function.equals("statue")||function.equals("frame")||function.equals("display")||function.equals("notice")||function.equals("hourglass")){
            if(who.isShiftKeyDown())return InteractionResult.TRY_WITH_EMPTY_HAND;
            if(d.getItem(0).isEmpty()&&d.canPlaceItem(0,held)){d.setItem(0,held.split(1));d.changed();return InteractionResult.SUCCESS;}
        }return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof OwnerSupplementData d)||!d.allowed(who))return InteractionResult.FAIL;if(l.isClientSide())return InteractionResult.SUCCESS;
        if(function.equals("pulley"))return OwnerPulleys.use(d,who,ItemStack.EMPTY);if(function.equals("slidy"))return OwnerSlidingBlocks.use(d,who,hit);if(function.equals("hidden_bricks")||function.equals("mob_head")||function.equals("bubble")||function.equals("fuse"))return InteractionResult.PASS;if(function.equals("cannon"))return OwnerCannon.use(d,who,ItemStack.EMPTY,InteractionHand.MAIN_HAND);if(function.equals("way_sign"))return OwnerWaySigns.use(d,who,ItemStack.EMPTY,InteractionHand.MAIN_HAND,hit);if(function.equals("globe"))return OwnerGlobes.use(d,who,ItemStack.EMPTY,InteractionHand.MAIN_HAND);
        if(function.equals("flag"))return OwnerSupplementFlags.move(l,p,who);
        if(function.equals("goblet"))return OwnerSoftLiquids.drink(d,who);if(OwnerSupplementText.text(d)){if(function.equals("doormat")&&(who.isShiftKeyDown()||hit.getDirection()!=Direction.UP)&&!d.getItem(0).isEmpty()){who.getInventory().placeItemBackInInventory(d.removeItemNoUpdate(0),net.minecraft.util.Prediction.SERVER_ONLY);d.changed();}else if(who instanceof net.minecraft.server.level.ServerPlayer player)OwnerSupplementText.open(player,d);return InteractionResult.SUCCESS;}if(function.equals("books"))return OwnerSupplementBooks.take(d,who,hit);if(function.equals("spikes"))return InteractionResult.PASS;if(function.equals("bellows")){if(d.power==0&&l.getGameTime()>=d.pulseUntil+10){d.pulseUntil=l.getGameTime()+10;d.changed();}return InteractionResult.SUCCESS;}
        if(function.equals("blackboard")){if(who instanceof net.minecraft.server.level.ServerPlayer player)OwnerBlackboards.open(player,d);return InteractionResult.SUCCESS;}
        if(d.captive!=null&&(function.equals("jar")||function.equals("cage"))){if(who instanceof net.minecraft.server.level.ServerPlayer player&&OwnerSupplementVessel.release(player,d.captive,p.relative(hit.getDirection()))){d.captive=null;d.changed();}return InteractionResult.SUCCESS;}
        if(function.equals("jar")&&who.isShiftKeyDown()){for(int slot=d.getContainerSize()-1;slot>=0;slot--)if(!d.getItem(slot).isEmpty()){who.getInventory().placeItemBackInInventory(d.removeItemNoUpdate(slot),net.minecraft.util.Prediction.SERVER_ONLY);d.changed();break;}return InteractionResult.SUCCESS;}
        if(function.equals("faucet")){l.setBlockAndUpdate(p,set(s,"enabled",Boolean.toString(!flag(s,"enabled"))));return InteractionResult.SUCCESS;}
        if(function.equals("rope_bunting"))return OwnerBuntings.take(d,who,hit);
        if(function.equals("flowers")){int slot=flowerSlot(s,p,hit);if(!d.getItem(slot).isEmpty()){who.getInventory().placeItemBackInInventory(d.removeItemNoUpdate(slot),net.minecraft.util.Prediction.SERVER_ONLY);d.changed();}return InteractionResult.SUCCESS;}
        if(function.equals("rune_lamp")){var prop=getStateDefinition().getProperty("rune");if(prop!=null){var values=prop.getPossibleValues();int i=0;for(var value:values){if(value.toString().equals(value(s,"rune")))break;i++;}l.setBlockAndUpdate(p,set(s,"rune",values.get((i+1)%values.size()).toString()));}return InteractionResult.SUCCESS;}
        if(function.equals("sconce_lever")){d.power=d.power>0?0:15;l.setBlockAndUpdate(p,set(s,"powered",Boolean.toString(d.power>0)));d.changed();l.updateNeighborsAt(p,this);l.updateNeighborsAt(p.relative(facing(s).getOpposite()),this);return InteractionResult.SUCCESS;}
        if(function.equals("sconce")&&flag(s,"lit")){l.setBlockAndUpdate(p,set(s,"lit","false"));l.playSound(null,p,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1);return InteractionResult.SUCCESS;}
        if(function.equals("statue")||function.equals("frame")||function.equals("display")||function.equals("notice")){
            if(who.isShiftKeyDown()&&!d.getItem(0).isEmpty()&&d.mayEdit(who)){var item=d.removeItemNoUpdate(0);who.getInventory().placeItemBackInInventory(item,net.minecraft.util.Prediction.SERVER_ONLY);d.changed();}
            else if(function.equals("notice")&&!d.getItem(0).isEmpty())who.openMenu(d);
            return InteractionResult.SUCCESS;
        }
        if(function.equals("hourglass")){
            if(who.isShiftKeyDown()){who.getInventory().placeItemBackInInventory(d.removeItemNoUpdate(0),net.minecraft.util.Prediction.SERVER_ONLY);d.progress=0;d.changed();}
            else l.setBlockAndUpdate(p,set(s,"facing",value(s,"facing").equals("down")?"up":"down"));return InteractionResult.SUCCESS;
        }
        if(function.equals("crank")&&l instanceof ServerLevel server&&who instanceof net.minecraft.server.level.ServerPlayer actor&&OwnerPulleys.drive(server,actor,p.relative(facing(s)),facing(s),who.isShiftKeyDown(),4))return InteractionResult.SUCCESS;
        if(function.equals("crank")){d.power=Math.floorMod(d.power+(who.isShiftKeyDown()?-1:1),16);l.setBlockAndUpdate(p,set(s,"power",Integer.toString(d.power)));d.changed();l.updateNeighborsAt(p,this);l.updateNeighborsAt(p.relative(facing(s).getOpposite()),this);l.playSound(null,p,net.minecraft.sounds.SoundEvents.LEVER_CLICK,net.minecraft.sounds.SoundSource.BLOCKS,.5F,.55F+d.power*.04F);return InteractionResult.SUCCESS;}
        if(function.equals("turntable")){if(who.isShiftKeyDown())l.setBlockAndUpdate(p,set(s,"inverted",Boolean.toString(!flag(s,"inverted"))));else d.rotate((ServerLevel)l);return InteractionResult.SUCCESS;}
        if(capacity>0){who.openMenu(d);return InteractionResult.SUCCESS;}return InteractionResult.PASS;
    }
    @Override public void fallOn(Level l,BlockState s,BlockPos p,Entity e,double distance){super.fallOn(l,s,p,e,distance);if(function.equals("hidden_bricks"))OwnerHiddenBricks.crumble(l,p,e,!e.isSteppingCarefully()&&distance>3);if(function.equals("bubble")&&distance>3&&!OwnerBubbles.gentle(e))OwnerBubbles.pop(l,p);}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){if(function.equals("bubble"))OwnerBubbles.contact(l,p,e,false);if(function.equals("turntable")&&l instanceof ServerLevel server)OwnerSupplementRotation.step(server,p,s,e);if(function.equals("bellows")&&e instanceof Player who&&Menagerie.mayChange(l,who,p)&&l.getBlockEntity(p) instanceof OwnerSupplementData d&&d.power==0&&!l.isClientSide()&&l.getGameTime()>=d.pulseUntil+10){d.pulseUntil=l.getGameTime()+10;if(!l.isClientSide())d.changed();}super.stepOn(l,p,s,e);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return function.equals("mob_head")&&l.getBlockEntity(p) instanceof OwnerSupplementData head?OwnerMobHeads.shape(head):function.equals("hidden_bricks")?OwnerHiddenBricks.collision(c):function.equals("bubble")?OwnerBubbles.collision(c,p):function.equals("way_sign")?OwnerWaySigns.collision(s,l,p,c):function.equals("rope_bunting")?OwnerSupplementRopes.collision(s,l,p,c,getShape(s,l,p,c)):super.getCollisionShape(s,l,p,c);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){if(function.equals("mob_head")&&l.getBlockEntity(p) instanceof OwnerSupplementData head)return OwnerMobHeads.shape(head);if(function.equals("cannon")){var plate=switch(OwnerCannon.mount(s)){case UP->Block.box(0,0,0,16,2,16);case DOWN->Block.box(0,14,0,16,16,16);case NORTH->Block.box(0,0,14,16,16,16);case SOUTH->Block.box(0,0,0,16,16,2);case EAST->Block.box(0,0,0,2,16,16);case WEST->Block.box(14,0,0,16,16,16);};return net.minecraft.world.phys.shapes.Shapes.or(plate,Block.box(2,2,2,14,14,14));}if(function.equals("way_sign"))return OwnerWaySigns.shape(s,l,p,c);if(function.equals("jar_boat"))return facing(s).getAxis()==Direction.Axis.X?Block.box(3,0,0,13,12,16):Block.box(0,0,3,16,12,13);if(function.equals("globe"))return Block.box(2,0,2,14,15,14);if(function.equals("books"))return OwnerSupplementBooks.shape(s);if(function.equals("spikes"))return OwnerSupplementSpikes.shape(facing(s));if(function.equals("spring_arm"))return OwnerSupplementSprings.shape(s,l,p);if(function.equals("bellows")){float h=l.getBlockEntity(p) instanceof OwnerSupplementData d&&d.getLevel()!=null?OwnerSupplementData.bellowsHeight(d.power,d.pulseUntil,d.getLevel().getGameTime()):0;return facing(s).getAxis()==Direction.Axis.Y?net.minecraft.world.phys.shapes.Shapes.box(0,0,-h,1,1,1+h):net.minecraft.world.phys.shapes.Shapes.box(0,-h,0,1,1+h,1);}return super.getShape(s,l,p,c);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean precise){if(function.equals("fuse"))OwnerGunpowder.contact(level,pos,entity);if(function.equals("hidden_bricks"))OwnerHiddenBricks.crumble(level,pos,entity,entity.yo<entity.getY());if(function.equals("bubble"))OwnerBubbles.contact(level,pos,entity,true);if(function.equals("spikes")&&level instanceof ServerLevel server&&level.getBlockEntity(pos) instanceof OwnerSupplementData data)OwnerSupplementSpikes.contact(server,data,entity);super.entityInside(state,level,pos,entity,effects,precise);}
    @Override public boolean publicTownInteraction(){return function.equals("notice");}
    @Override protected boolean isSignalSource(BlockState s){return function.equals("mob_head")||function.equals("relayer")||function.equals("sconce_lever")||function.equals("wind")||function.equals("crank")||function.equals("trapped");}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction dir){return function.equals("relayer")&&dir!=facing(s)?0:l.getBlockEntity(p) instanceof OwnerSupplementData d?function.equals("trapped")?Math.min(15,d.viewers):d.power:0;}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction direction){return l.getBlockEntity(p) instanceof OwnerSupplementData d?function.equals("globe")?OwnerGlobes.signal(d):d.softPortions>0?OwnerSoftLiquids.signal(d):function.equals("mob_head")||function.equals("hourglass")||function.equals("clock")||function.equals("wind")?d.power:net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(d):0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction dir){return ((function.equals("crank")||function.equals("relayer")||function.equals("sconce_lever"))&&facing(s)==dir||function.equals("wind")&&dir==Direction.UP)?getSignal(s,l,p,dir):0;}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(function.equals("bubble"))OwnerBubbles.pop(l,p);else super.tick(s,l,p,r);}
    @Override protected void onExplosionHit(BlockState s,ServerLevel l,BlockPos p,net.minecraft.world.level.Explosion explosion,java.util.function.BiConsumer<ItemStack,BlockPos> drops){
        if(function.equals("wind")&&explosion.canTriggerBlocks()&&l.getBlockEntity(p) instanceof OwnerSupplementData d){d.pulseUntil=l.getGameTime()+40;d.power=3;d.changed();l.updateNeighborsAt(p,this);}
        super.onExplosionHit(s,l,p,explosion,drops);
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        if(params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof OwnerSupplementData d&&d.portable())return java.util.List.of(d.packed());
        if(function.equals("way_sign")&&params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof OwnerSupplementData d)return OwnerWaySigns.base(d.wayBase)?java.util.List.of(new ItemStack(d.wayBase.getBlock())):java.util.List.of();
        return super.getDrops(s,params);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide()&&player.isCreative()&&l.getBlockEntity(p) instanceof OwnerSupplementData d&&d.portable()&&(!d.isEmpty()||d.captive!=null||!d.fluid.isResourceBlank()||function.equals("blackboard")))popResource(l,p,d.packed());
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState s,ServerLevel l,BlockPos p,boolean moved){Containers.updateNeighboursAfterDestroy(s,l,p);}
}
