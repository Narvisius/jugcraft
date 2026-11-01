package io.github.jimbozoomer.jugcraft.arcane;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;

/** Short-lived original ten-block lift column; never overwrites terrain or another player's claim. */
public final class ArcaneWind extends BlockEntity {
    private UUID owner;
    public ArcaneWind(BlockPos p,BlockState s){super(ForbiddenArcane.WIND,p,s);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.store("owner",UUIDUtil.CODEC,owner);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    public static final class Prism extends Item {
        public Prism(Properties p){super(p.stacksTo(1));}
        @Override public InteractionResult useOn(UseOnContext c){
            if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;var l=p.level();if(p.getCooldowns().isOnCooldown(c.getItemInHand()))return InteractionResult.FAIL;var base=c.getClickedPos();if(!l.getBlockState(base).canBeReplaced())base=base.relative(c.getClickedFace());
            for(int i=0;i<=10;i++){var at=base.above(i);if(!l.isInWorldBounds(at)||!l.hasChunkAt(at)||!l.isEmptyBlock(at)||!Authority.mayChange(l,p,at))return InteractionResult.FAIL;}
            l.setBlock(base,ForbiddenArcane.block("whirlwind").defaultBlockState(),3);if(l.getBlockEntity(base) instanceof ArcaneWind wind){wind.owner=p.getUUID();wind.setChanged();}
            for(int i=1;i<=10;i++)l.setBlock(base.above(i),ForbiddenArcane.block("upwind").defaultBlockState(),3);
            l.scheduleTick(base,ForbiddenArcane.block("whirlwind"),20);p.getCooldowns().addCooldown(c.getItemInHand(),20);return InteractionResult.SUCCESS;
        }
    }
    public static class Column extends Block {
        public Column(Properties p){super(p.noCollision().noOcclusion().noLootTable());}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
        @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction side,BlockPos other,BlockState n,RandomSource r){if(side==Direction.DOWN&&!n.is(ForbiddenArcane.block("upwind"))&&!n.is(ForbiddenArcane.block("whirlwind")))ticks.scheduleTick(p,this,1);return s;}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){var below=l.getBlockState(p.below());if(!below.is(ForbiddenArcane.block("upwind"))&&!below.is(ForbiddenArcane.block("whirlwind")))l.removeBlock(p,false);}
        @Override protected void entityInside(BlockState s,Level raw,BlockPos p,Entity target,InsideBlockEffectApplier effects,boolean moving){
            if(!(raw instanceof ServerLevel l))return;ArcaneWind root=null;for(int i=0;i<=10;i++){var at=p.below(i);if(l.getBlockEntity(at) instanceof ArcaneWind w){root=w;break;}if(!l.getBlockState(at).is(ForbiddenArcane.block("upwind")))break;}
            if(root==null)return;var actor=Authority.present(l,root.owner);if(actor==null||!Authority.mayChange(l,actor,target.blockPosition())||target!=actor&&(!(target instanceof LivingEntity living)||!io.github.jimbozoomer.jugcraft.weapons.TwoHanded.allowed(actor,l,living)))return;
            if(l.isEmptyBlock(p.above())){target.onAboveBubbleColumn(false,p);if(target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,70));}else target.onInsideBubbleColumn(false);
        }
        @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){var effect=net.minecraft.core.particles.SpellParticleOption.create(ParticleTypes.EFFECT,0xFFFFFF,1);
            l.addParticle(effect,p.getX()+.5,p.getY(),p.getZ()+.5,0,.1,0);
            l.addParticle(effect,p.getX()+r.nextDouble(),p.getY()+r.nextDouble(),p.getZ()+r.nextDouble(),0,.1,0);
            if(r.nextInt(200)==0)l.playLocalSound(p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,.2F+r.nextFloat()*.2F,.9F+r.nextFloat()*.15F,false);}
    }
    public static final class Root extends Column implements EntityBlock {
        public Root(Properties p){super(p);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArcaneWind(p,s);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){l.removeBlock(p,false);for(int i=1;i<=10;i++)if(l.getBlockState(p.above(i)).is(ForbiddenArcane.block("upwind")))l.removeBlock(p.above(i),false);}
    }
}
