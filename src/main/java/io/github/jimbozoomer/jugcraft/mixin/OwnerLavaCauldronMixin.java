package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
/** Supplied lava-cauldron ambience, with sound height relative to the actual cauldron. */
@Mixin(LavaCauldronBlock.class)
public abstract class OwnerLavaCauldronMixin extends Block {
    protected OwnerLavaCauldronMixin(Properties p){super(p);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        super.animateTick(state,level,pos,random);
        if(random.nextInt(4)!=0||!level.getBlockState(pos.above()).isAir())return;
        double y=pos.getY()+.9375;
        if(random.nextInt(20)==0){level.addParticle(ParticleTypes.LAVA,pos.getX()+.1875+random.nextDouble()*.625,y,pos.getZ()+.1875+random.nextDouble()*.625,0,0,0);level.playLocalSound(pos.getX()+.5,y,pos.getZ()+.5,SoundEvents.LAVA_POP,SoundSource.BLOCKS,.2F+random.nextFloat()*.2F,.9F+random.nextFloat()*.15F,false);}
        if(random.nextInt(40)==0)level.playLocalSound(pos.getX()+.5,y,pos.getZ()+.5,SoundEvents.LAVA_AMBIENT,SoundSource.BLOCKS,.2F+random.nextFloat()*.2F,.9F+random.nextFloat()*.15F,false);
    }
}
