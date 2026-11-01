package io.github.jimbozoomer.jugcraft.menagerie;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Original layered food, rake patterns, loose ash, fodder and slippery soap. */
final class OwnerSupplementTerrain extends HouseholdBlock implements SimpleWaterloggedBlock,Fallable {
    OwnerSupplementTerrain(Properties p,JsonObject d){super(p,d);if(name.equals("pancake"))registerDefaultState(set(set(defaultBlockState(),"pancakes","1"),"topping","none"));if(name.equals("grouper"))registerDefaultState(set(set(defaultBlockState(),"match","none"),"open_sides","false"));if(name.equals("rope"))registerDefaultState(set(set(defaultBlockState(),"up","true"),"down","true"));if(rod())registerDefaultState(set(set(set(defaultBlockState(),"axis_x","false"),"axis_z","false"),"axis_y","true"));if(name.equals("ash")||name.equals("fodder"))registerDefaultState(set(defaultBlockState(),"layers","1"));}
    private boolean rod(){return java.util.Set.of("stick","blaze_rod","breeze_rod","prismarine_rod","edelwood_stick","propelplant_cane").contains(name);}
    private boolean awning(){return name.startsWith("awning");}
    private String layerKey(){return name.equals("pancake")?"pancakes":"layers";}
    private boolean layered(){return name.equals("pancake")||name.equals("ash")||name.equals("fodder");}
    private int layers(BlockState s){return Integer.parseInt(value(s,layerKey()));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var old=c.getLevel().getBlockState(c.getClickedPos());if(name.equals("confetti_litter"))return old.is(this)?set(old,"confetti_amount",Integer.toString(Math.min(4,Integer.parseInt(value(old,"confetti_amount"))+1))):set(defaultBlockState(),"facing",c.getHorizontalDirection().getOpposite().getName());if(layered()&&old.is(this))return set(old,layerKey(),Integer.toString(Math.min(8,layers(old)+1)));
        if(name.equals("rope"))return OwnerSupplementRopes.connections(set(defaultBlockState(),"waterlogged",Boolean.toString(c.getLevel().getFluidState(c.getClickedPos()).is(FluidTags.WATER))),c.getLevel(),c.getClickedPos());
        if(rod()){var next=old.is(this)?old:set(set(set(defaultBlockState(),"axis_x","false"),"axis_y","false"),"axis_z","false");return set(set(next,"axis_"+c.getClickedFace().getAxis().getName(),"true"),"waterlogged",Boolean.toString(c.getLevel().getFluidState(c.getClickedPos()).is(FluidTags.WATER)));}
        if(awning()){
            for(var dir:c.getNearestLookingDirections())if(dir.getAxis().isHorizontal()){
                boolean bottom=c.getClickedFace()!=Direction.DOWN&&(c.getClickedFace()==Direction.UP||c.getClickLocation().y-c.getClickedPos().getY()<=.5);boolean slanted=false;
                var behind=c.getLevel().getBlockState(c.getClickedPos().relative(dir));if(behind.getBlock() instanceof OwnerSupplementTerrain t&&t.awning()&&flag(behind,"slanted")){slanted=true;bottom=true;}
                var candidate=set(set(set(defaultBlockState(),"facing",dir.getOpposite().getName()),"bottom",Boolean.toString(bottom)),"slanted",Boolean.toString(slanted));if(candidate.canSurvive(c.getLevel(),c.getClickedPos()))return candidate;
            }return null;
        }
        var s=super.getStateForPlacement(c);if(s==null)return null;s=set(s,"waterlogged",Boolean.toString(c.getLevel().getFluidState(c.getClickedPos()).is(FluidTags.WATER)));
        if(name.equals("fire_pit"))s=set(set(s,"hanging",Boolean.toString(c.getLevel().getBlockState(c.getClickedPos().above()).isFaceSturdy(c.getLevel(),c.getClickedPos().above(),Direction.DOWN))),"lit",Boolean.toString(!flag(s,"waterlogged")));
        if(name.equals("raked_gravel"))s=connected(s,c.getLevel(),c.getClickedPos(),c.getHorizontalDirection());return s;
    }
    @Override protected boolean canBeReplaced(BlockState s,BlockPlaceContext c){if(name.equals("confetti_litter"))return c.getItemInHand().getItem() instanceof OwnerConfetti&&Integer.parseInt(value(s,"confetti_amount"))<4;if(rod())return !c.isSecondaryUseActive()&&c.getItemInHand().is(asItem())&&!flag(s,"axis_"+c.getClickedFace().getAxis().getName());if(layered()&&c.getItemInHand().is(asItem())&&layers(s)<8)return !c.replacingClickedOnBlock()||c.getClickedFace()==Direction.UP;return name.equals("ash")&&layers(s)<=3;}
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return flag(s,"waterlogged")?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){if(name.equals("confetti_litter"))return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);if(name.equals("rope"))return OwnerSupplementRopes.supported(l,p);if(awning()){var back=p.relative(facing(s).getOpposite());var b=l.getBlockState(back);if(b.isSolid()||b.getBlock() instanceof OwnerSupplementTerrain t&&t.awning()&&(!flag(b,"slanted")||flag(s,"bottom")))return true;var upper=l.getBlockState(back.above());return upper.getBlock() instanceof OwnerSupplementTerrain t&&t.awning()&&flag(upper,"slanted")||l.getBlockState(p.relative(facing(s).getClockWise())).isSolid()||l.getBlockState(p.relative(facing(s).getCounterClockWise())).isSolid();}if(name.equals("fire_pit"))return flag(s,"hanging")?l.getBlockState(p.above()).isFaceSturdy(l,p.above(),Direction.DOWN):l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);if(name.equals("pancake"))return !l.getBlockState(p.below()).getCollisionShape(l,p.below()).getFaceShape(Direction.UP).isEmpty();if(name.equals("raked_gravel"))return !l.getBlockState(p.above()).isSolid()||l.getBlockState(p.above()).getBlock() instanceof FenceGateBlock;return true;}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos q,BlockState neighbor,RandomSource r){if(flag(s,"waterlogged"))ticks.scheduleTick(p,net.minecraft.world.level.material.Fluids.WATER,5);if(name.equals("rope")){ticks.scheduleTick(p,this,1);return OwnerSupplementRopes.connections(s,l,p);}if((name.equals("confetti_litter")||name.equals("pancake")||name.equals("fire_pit")||awning())&&!s.canSurvive(l,p))return Blocks.AIR.defaultBlockState();if(name.equals("ash")||name.equals("raked_gravel"))ticks.scheduleTick(p,this,2);return s;}
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);if(name.equals("ash")||name.equals("raked_gravel")||name.equals("cog_block")||name.equals("grouper")||name.equals("rope"))l.scheduleTick(p,this,2);}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,net.minecraft.world.level.redstone.Orientation orientation,boolean moving){if(name.equals("cog_block")||name.equals("grouper")||name.equals("rope"))l.scheduleTick(p,this,1);super.neighborChanged(s,l,p,b,orientation,moving);}
    @Override protected boolean isSignalSource(BlockState s){return name.equals("cog_block")||name.equals("grouper");}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){return name.equals("grouper")?(side==facing(s)?OwnerSupplementGrouper.signal(s):0):name.equals("cog_block")?Math.max(0,Integer.parseInt(value(s,"power"))-1):0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction side){return name.equals("grouper")?getSignal(s,l,p,side):0;}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(name.equals("rope")){if(!s.canSurvive(l,p))l.destroyBlock(p,true);return;}
        if(name.equals("grouper")){OwnerSupplementGrouper.tick(l,p,s);return;}
        if(name.equals("cog_block")){var next=set(s,"power",Integer.toString(l.getBestNeighborSignal(p)));if(next!=s)l.setBlockAndUpdate(p,next);return;}
        if(name.equals("raked_gravel")&&!s.canSurvive(l,p)){l.setBlockAndUpdate(p,Block.pushEntitiesUp(s,Blocks.GRAVEL.defaultBlockState(),l,p));return;}
        if(name.equals("ash")||name.equals("raked_gravel")){
            var below=l.getBlockState(p.below());
            if(name.equals("ash")&&below.is(this)&&layers(below)<8){int total=layers(s)+layers(below);l.setBlockAndUpdate(p.below(),set(below,"layers",Integer.toString(Math.min(8,total))));if(total<=8)l.removeBlock(p,false);else l.setBlockAndUpdate(p,set(s,"layers",Integer.toString(total-8)));return;}
            if(FallingBlock.isFree(below)&&p.getY()>l.getMinY())FallingBlockEntity.fall(l,p,s);
        }
    }
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(name.equals("ash")&&l.isRainingAt(p.above())&&r.nextInt(4)==0)removeLayer(l,p,s);}
    private void removeLayer(Level l,BlockPos p,BlockState s){int n=layers(s);if(n>1)l.setBlockAndUpdate(p,set(s,layerKey(),Integer.toString(n-1)));else l.removeBlock(p,false);}
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit){
        if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;
        if(name.equals("rope"))return held.getItem() instanceof OwnerBuntings.Pennant?OwnerBuntings.put(l,p,who,held,hit):OwnerSupplementRopes.extend(s,l,p,who,held,hand);
        if(name.equals("fire_pit")){
            if(!flag(s,"lit")&&!flag(s,"waterlogged")&&(held.is(Items.FLINT_AND_STEEL)||held.is(Items.FIRE_CHARGE))){if(!l.isClientSide()){l.setBlockAndUpdate(p,set(s,"lit","true"));if(held.is(Items.FLINT_AND_STEEL))held.hurtAndBreak(1,who,hand.asEquipmentSlot());else held.consume(1,who);}return InteractionResult.SUCCESS;}
            if(flag(s,"lit")&&held.is(net.minecraft.tags.ItemTags.SHOVELS)){if(!l.isClientSide()){l.setBlockAndUpdate(p,set(s,"lit","false"));held.hurtAndBreak(1,who,hand.asEquipmentSlot());l.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.5F,1);}return InteractionResult.SUCCESS;}
        }
        if(name.equals("pancake")&&value(s,"topping").equals("none")){
            String topping=held.is(Items.HONEY_BOTTLE)?"honey":held.is(Items.SWEET_BERRIES)?"jam":held.is(Items.COCOA_BEANS)?"chocolate":held.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse("c:foods/syrup")))?"syrup":"none";
            if(!topping.equals("none")){if(!l.isClientSide()){l.setBlockAndUpdate(p,set(s,"topping",topping));boolean bottle=held.is(Items.HONEY_BOTTLE);if(!who.isCreative()){held.shrink(1);if(bottle)who.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE),net.minecraft.util.Prediction.SERVER_ONLY);}}return InteractionResult.SUCCESS;}
        }
        if(name.equals("ash")&&held.getItem() instanceof BrushItem||name.equals("fodder")&&held.is(net.minecraft.tags.ItemTags.HOES)){if(!l.isClientSide()){removeLayer(l,p,s);held.hurtAndBreak(1,who,hand.asEquipmentSlot());}return InteractionResult.SUCCESS;}
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){
        if(name.equals("stick"))return OwnerSupplementFlags.fromPole(l,p,who);
        if(name.equals("rope")){if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;return OwnerSupplementRopes.retrieve(l,p,who);}
        if(awning()){if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;if(!l.isClientSide())l.setBlockAndUpdate(p,set(s,"slanted",Boolean.toString(!flag(s,"slanted"))));return InteractionResult.SUCCESS;}
        if(!name.equals("pancake")||!who.canEat(false))return InteractionResult.PASS;if(!Menagerie.mayChange(l,who,p))return InteractionResult.FAIL;
        if(!l.isClientSide()){
            // Bound the scan and check every layer before eating the top of a stacked serving.
            BlockPos q=p;BlockState top=s;for(int i=0;i<32&&layers(top)==8;i++){var next=q.above();if(!l.hasChunkAt(next)||!l.getBlockState(next).is(this))break;if(!Menagerie.mayChange(l,who,next))return InteractionResult.FAIL;q=next;top=l.getBlockState(q);}
            who.getFoodData().eat(1,.1F);if(!value(top,"topping").equals("none"))who.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED,160));removeLayer(l,q,top);l.playSound(null,p,SoundEvents.GENERIC_EAT.value(),SoundSource.PLAYERS,1,1);
        }return InteractionResult.SUCCESS;
    }
    @Override public boolean placeLiquid(LevelAccessor l,BlockPos p,BlockState s,net.minecraft.world.level.material.FluidState fluid){boolean placed=SimpleWaterloggedBlock.super.placeLiquid(l,p,s,fluid);if(placed&&name.equals("fire_pit"))l.setBlock(p,set(l.getBlockState(p),"lit","false"),Block.UPDATE_ALL);return placed;}
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e,InsideBlockEffectApplier effects,boolean precise){if(name.equals("fire_pit")&&flag(s,"lit")&&l instanceof ServerLevel server&&e instanceof net.minecraft.world.entity.LivingEntity)e.hurtServer(server,server.damageSources().inFire(),1);super.entityInside(s,l,p,e,effects,precise);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){if(rod()&&r.nextFloat()<.25F&&(name.equals("blaze_rod")||name.equals("breeze_rod"))){var axes=new java.util.ArrayList<Direction.Axis>();for(var axis:Direction.Axis.values())if(flag(s,"axis_"+axis.getName()))axes.add(axis);if(!axes.isEmpty()){var axis=axes.get(r.nextInt(axes.size()));double along=r.nextDouble();l.addParticle(name.equals("breeze_rod")?ParticleTypes.SMALL_GUST:flag(s,"waterlogged")?ParticleTypes.BUBBLE:ParticleTypes.SMOKE,p.getX()+(axis==Direction.Axis.X?along:.5),p.getY()+(axis==Direction.Axis.Y?along:.5),p.getZ()+(axis==Direction.Axis.Z?along:.5),0,.01,0);}}if(name.equals("fire_pit")&&flag(s,"lit")){for(int i=0;i<3;i++)l.addParticle(ParticleTypes.SMOKE,p.getX()+.25+r.nextDouble()*.5,p.getY()+.375+r.nextDouble()*.5,p.getZ()+.25+r.nextDouble()*.5,0,.04,0);if(r.nextInt(10)==0)l.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,SoundEvents.FIRE_AMBIENT,SoundSource.BLOCKS,1+r.nextFloat(),.6F+r.nextFloat()*.7F,false);}}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return name.equals("fire_pit");}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction direction){return name.equals("fire_pit")&&flag(s,"lit")?15:0;}
    @Override public void stepOn(Level l,BlockPos p,BlockState s,Entity e){
        if(awning()&&!e.isSuppressingBounce()){
            var v=e.getDeltaMovement();if(v.y<-.32){double angle=Math.atan(.375);var dir=facing(s);var normal=flag(s,"slanted")?new Vec3(dir.getStepX()*Math.sin(angle),Math.cos(angle),dir.getStepZ()*Math.sin(angle)):new Vec3(0,1,0);e.setDeltaMovement(v.subtract(normal.scale(2*v.dot(normal))));e.needsSync=true;if(!l.isClientSide())l.playSound(null,p,OwnerSupplementaries.SOUNDS.get("awning_bounce"),SoundSource.BLOCKS,1,1);}else e.setDeltaMovement(v.multiply(1,0,1));
        }
        if(name.equals("blaze_rod")&&l instanceof ServerLevel server&&e instanceof net.minecraft.world.entity.LivingEntity&&!io.github.jimbozoomer.jugcraft.town.TownProtection.shieldsBlock(l,p)&&!io.github.jimbozoomer.jugcraft.finance.LandRegistry.isClaimed(l,p))e.hurtServer(server,l.damageSources().inFire(),.5F);
        if(name.equals("soap_block")&&l instanceof ServerLevel&&!e.isSteppingCarefully()&&l.getRandom().nextFloat()<.14F){var v=e.getDeltaMovement().multiply(1,0,1);if(v.lengthSqr()>.0008){v=v.normalize().scale(.1+l.getRandom().nextFloat()*.13).yRot((20+l.getRandom().nextFloat()*73)*(l.getRandom().nextBoolean()?1:-1)*(float)Math.PI/180);e.setDeltaMovement(e.getDeltaMovement().add(v));e.syncVelocity=true;}}
        if(name.equals("ash")&&l instanceof ServerLevel server&&e.getDeltaMovement().horizontalDistanceSqr()>.001&&l.getRandom().nextInt(3)==0)server.sendParticles(ParticleTypes.ASH,e.getX(),p.getY()+layers(s)/8D,e.getZ(),3,.1,.02,.1,.01);
        super.stepOn(l,p,s,e);
    }
    @Override public void fallOn(Level l,BlockState s,BlockPos p,Entity e,double distance){if(awning()&&!e.isSuppressingBounce())e.causeFallDamage(distance,0,l.damageSources().fall());else if(name.equals("ash"))e.causeFallDamage(distance,layers(s)>2?.3F:1F,l.damageSources().fall());else super.fallOn(l,s,p,e,distance);}
    @Override public void onLand(Level l,BlockPos p,BlockState s,BlockState replaced,FallingBlockEntity falling){if(name.equals("ash")&&l.getBlockState(p.below()).is(Blocks.BASALT))l.setBlockAndUpdate(p.below(),OwnerSupplementaries.BLOCKS.get("ashen_basalt").defaultBlockState());}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(name.equals("confetti_litter"))return Shapes.empty();if(name.equals("rope"))return OwnerSupplementRopes.collision(s,l,p,c,super.getShape(s,l,p,c));if(awning()){if(c.isDescending())return Shapes.empty();double y=flag(s,"bottom")?0:8;var shape=Block.box(0,y+1,0,16,y+8,16);return c.isAbove(shape,p,true)?shape:Shapes.empty();}if(name.equals("ash"))return Block.box(0,0,0,16,Math.max(.5,(layers(s)-1)*2),16);return super.getCollisionShape(s,l,p,c);}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){
        if(name.equals("rope")){var next=state;for(var direction:Direction.Plane.HORIZONTAL)next=set(next,rotation.rotate(direction).getName(),value(state,direction.getName()));return next;}
        if(rod())return rotation==Rotation.CLOCKWISE_90||rotation==Rotation.COUNTERCLOCKWISE_90?set(set(state,"axis_x",value(state,"axis_z")),"axis_z",value(state,"axis_x")):state;
        if(name.equals("raked_gravel"))return pattern(state,rotation::rotate);
        return super.rotate(state,rotation);
    }
    @Override protected BlockState mirror(BlockState state,Mirror mirror){if(name.equals("rope")){var next=state;for(var direction:Direction.Plane.HORIZONTAL)next=set(next,mirror.mirror(direction).getName(),value(state,direction.getName()));return next;}return name.equals("raked_gravel")?pattern(state,mirror::mirror):super.mirror(state,mirror);}
    private static BlockState pattern(BlockState state,java.util.function.UnaryOperator<Direction> transform){
        String[] parts=value(state,"shape").split("_");if(parts.length!=2)return state;
        Direction a=transform.apply(Direction.byName(parts[0])),b=transform.apply(Direction.byName(parts[1]));
        String shape=a.getAxis()==b.getAxis()?(a.getAxis()==Direction.Axis.X?"east_west":"north_south"):(a.getAxis()==Direction.Axis.Z?a.getName()+"_"+b.getName():b.getName()+"_"+a.getName());return set(state,"shape",shape);
    }
    static BlockState connected(BlockState s,LevelAccessor l,BlockPos p,Direction front){
        Direction first=front;var back=l.getBlockState(p.relative(front.getOpposite()));if(back.is(s.getBlock())&&value(back,"shape").contains(front.getName()))first=front.getOpposite();
        for(var side:new Direction[]{front.getClockWise(),front.getCounterClockWise()}){var n=l.getBlockState(p.relative(side));if(n.is(s.getBlock())&&value(n,"shape").contains(side.getOpposite().getName())){String a=first.getName(),b=side.getName();return set(s,"shape",first.getAxis()==Direction.Axis.Z?a+"_"+b:b+"_"+a);}}
        return set(s,"shape",front.getAxis()==Direction.Axis.X?"east_west":"north_south");
    }
}
