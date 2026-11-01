package io.github.jimbozoomer.jugcraft.botany;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.*;

/** Manastar compares adjacent pool totals against a three-second sample; never moves mana. */
final class BotanicalMeter {
    private int sample,delta;private boolean initialized;
    String status(){return initialized?" · Pool trend: "+(delta>0?"gaining "+delta:delta<0?"losing "+(-delta):"steady")+" mana / sample":" · Awaiting adjacent pool sample";}
    void tick(BotanicalWorkshop flower,ServerLevel level,ServerPlayer actor){
        int total=0;for(var direction:Direction.Plane.HORIZONTAL){
            var pos=flower.getBlockPos().relative(direction);
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof BotanicalWorkshop pool&&pool.kind().endsWith("pool")&&pool.permitted(actor))total+=pool.mana();
        }
        if(!initialized){sample=total;initialized=true;}
        delta=total-sample;
        if(delta!=0&&level.getGameTime()%5==0){var pos=flower.getBlockPos();level.sendParticles(new DustParticleOptions(delta>0?0x0D0DFF:0xFF0D0D,.6F),pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5,1,.1,.1,.1,.01);}
        if(level.getGameTime()%60==0)sample=total;
    }
}
