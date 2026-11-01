package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.AABB;

/** Entropinnyum absorbs a dry fuse just before detonation, never after damaging the world. */
public final class BotanicalExplosives {
    private static final String CHECKED="jugcraft:botanical_tnt_checked",DUPLICATED="jugcraft:botanical_tnt_duplicated";
    public static void inspect(PrimedTnt tnt){
        if(!(tnt.level() instanceof ServerLevel level)||tnt.entityTags().contains(CHECKED))return;
        var at=tnt.blockPosition();boolean duplicated=level.getBlockState(at).getBlock() instanceof TntBlock;
        // Inspect after priming has removed legitimate TNT. Moving blocks expose common piston duplication.
        for(var side:Direction.values()){
            var pos=at.relative(side);if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof PistonMovingBlockEntity piston&&piston.getMovedState().getBlock() instanceof TntBlock)duplicated=true;
        }
        if(duplicated)tnt.addTag(DUPLICATED);tnt.addTag(CHECKED);
    }
    static void tick(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        if(flower.mana()!=0)return;int checked=0;
        for(var tnt:level.getEntitiesOfClass(PrimedTnt.class,new AABB(flower.effectivePos()).inflate(12))){
            if(++checked>32)break;
            if(!tnt.isAlive()||tnt.getFuse()>2||!tnt.entityTags().contains(CHECKED)||!level.getFluidState(tnt.blockPosition()).isEmpty()||!Authority.mayChange(level,actor,tnt.blockPosition()))continue;
            boolean duplicate=tnt.entityTags().contains(DUPLICATED);tnt.discard();flower.generate(duplicate?3:6500);flower.changed();
            level.sendParticles(duplicate?ParticleTypes.ANGRY_VILLAGER:ParticleTypes.EXPLOSION,tnt.getX(),tnt.getY()+.5,tnt.getZ(),8,.5,.5,.5,.02);return;
        }
    }
}
