package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Reversible underwater air cells retain the exact water state they temporarily displaced. */
final class BotanicalBubble {
    private final BotanicalWorkshop flower;
    private BlockPos source;
    private BlockState water=Blocks.WATER.defaultBlockState();
    private int radius=2,cursor;
    private long paidUntil;
    BotanicalBubble(BotanicalWorkshop flower){this.flower=flower;}
    void tick(ServerLevel level,ServerPlayer actor){
        long now=level.getGameTime();if(!flower.draw(4))return;paidUntil=now+2;flower.setChanged();
        if(now%10==0)radius=Math.min(flower.kind().endsWith("chibi")?6:12,radius+1);
        int width=radius*2+1,total=width*width*width;var origin=flower.effectivePos();
        // Grow the bubble in slices; do not scan the entire 25³ cube each game tick.
        for(int n=0;n<256;n++){
            int i=Math.floorMod(cursor++,total);var pos=origin.offset(i%width-radius,(i/width)%width-radius,i/(width*width)-radius);
            if(pos.distSqr(origin)>=radius*radius||!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))continue;
            var original=level.getBlockState(pos);if(!original.is(Blocks.WATER))continue;
            if(!level.setBlock(pos,Jugtania.block("fake_air").defaultBlockState(),3))continue;
            if(level.getBlockEntity(pos) instanceof BotanicalWorkshop cell){cell.bubble.source=flower.getBlockPos().immutable();cell.bubble.water=original;cell.setChanged();}
            else level.setBlock(pos,original,3);
        }
    }
    void restoreWhenInactive(ServerLevel level){
        if(Math.floorMod(level.getGameTime()+flower.getBlockPos().asLong(),20)!=0)return;
        if(source!=null&&level.hasChunkAt(source)&&level.getBlockEntity(source) instanceof BotanicalWorkshop origin&&origin.kind().startsWith("bubbell")&&flower.getBlockPos().distSqr(origin.effectivePos())<origin.bubble.radius*origin.bubble.radius&&origin.bubble.paidUntil>=level.getGameTime()-2&&origin.bubble.paidUntil<=level.getGameTime()+2)return;
        // Roll back only this temporary cell. Player replacements have a different BE and are never touched.
        if(level.getBlockEntity(flower.getBlockPos())==flower)level.setBlock(flower.getBlockPos(),water,3);
    }
    void save(ValueOutput out){out.putInt("bubble_radius",radius);out.putInt("bubble_cursor",cursor);if(source!=null)out.store("bubble_source",BlockPos.CODEC,source);out.store("bubble_original_water",BlockState.CODEC,water);}
    void load(ValueInput in){radius=Math.clamp(in.getIntOr("bubble_radius",2),2,12);cursor=Math.floorMod(in.getIntOr("bubble_cursor",0),15625);source=in.read("bubble_source",BlockPos.CODEC).filter(p->p.distSqr(flower.getBlockPos())<=441).orElse(null);water=in.read("bubble_original_water",BlockState.CODEC).filter(s->s.is(Blocks.WATER)).orElse(Blocks.WATER.defaultBlockState());paidUntil=0;}
}
