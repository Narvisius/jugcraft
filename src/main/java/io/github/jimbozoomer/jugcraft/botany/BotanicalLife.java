package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Redstone-clocked 25×25 cellular automaton. Only its own cells or empty space can change. */
final class BotanicalLife {
    private static final int WIDTH=25,RADIUS=12;
    private final BotanicalWorkshop workshop;
    int generation;
    private BlockPos parent;
    private long lease;
    BotanicalLife(BotanicalWorkshop workshop){this.workshop=workshop;}
    private boolean available(ServerLevel level,BlockPos center){return parent==null||parent.equals(center)||lease<level.getGameTime()-20||lease>level.getGameTime();}
    void tick(ServerLevel level,ServerPlayer actor){
        if(level.getGameTime()%10!=0||!level.hasNeighborSignal(workshop.effectivePos()))return;
        var center=workshop.effectivePos();var old=new BlockState[WIDTH*WIDTH];var ages=new int[old.length];Arrays.fill(ages,-1);var snapshots=new net.minecraft.nbt.CompoundTag[old.length];
        // Take one complete snapshot. Pause instead of calculating against an unloaded or protected board.
        for(int x=0;x<WIDTH;x++)for(int z=0;z<WIDTH;z++){
            int i=x*WIDTH+z;var pos=center.offset(x-RADIUS,0,z-RADIUS);
            if(!level.hasChunkAt(pos)||!Authority.mayChange(level,actor,pos))return;
            old[i]=level.getBlockState(pos);
            if(level.getBlockEntity(pos) instanceof BotanicalWorkshop cell&&cell.kind().equals("cell_block")){
                if(!cell.life.available(level,center)||!cell.permitted(actor))return;
                ages[i]=cell.life.generation;snapshots[i]=cell.saveWithFullMetadata(level.registryAccess());
            }
        }
        // A one-cell read-only halo carries the supplied quarter-generation boundary penalty.
        // Only cells leased by this flower may influence it from outside its board.
        var boundary=new java.util.HashMap<BlockPos,Integer>();
        for(int x=-1;x<=WIDTH;x++)for(int z=-1;z<=WIDTH;z++){
            if(x>=0&&z>=0&&x<WIDTH&&z<WIDTH)continue;var pos=center.offset(x-RADIUS,0,z-RADIUS);
            if(!level.hasChunkAt(pos))return;
            if(Authority.mayChange(level,actor,pos)&&level.getBlockEntity(pos) instanceof BotanicalWorkshop cell&&cell.kind().equals("cell_block")&&center.equals(cell.life.parent)&&cell.life.lease>=level.getGameTime()-20&&cell.life.lease<=level.getGameTime()&&cell.permitted(actor))boundary.put(pos,cell.life.generation/4);
        }
        var next=new int[ages.length];Arrays.fill(next,-1);int reward=0;boolean consume=false;
        for(int x=0;x<WIDTH;x++)for(int z=0;z<WIDTH;z++){
            int i=x*WIDTH+z;if(!old[i].isAir()&&ages[i]<0)continue;
            int count=0,maximum=-1;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dz==0)continue;int nx=x+dx,nz=z+dz;int life=nx<0||nz<0||nx>=WIDTH||nz>=WIDTH?boundary.getOrDefault(center.offset(nx-RADIUS,0,nz-RADIUS),-1):ages[nx*WIDTH+nz];if(life>=0){count++;maximum=Math.max(maximum,life);}
            }
            if(ages[i]<0&&count==3)next[i]=Math.min(100,maximum+1);
            else if(ages[i]>=0&&(count==2||count==3))next[i]=Math.min(100,ages[i]+1);
            if(Math.abs(x-RADIUS)<=1&&Math.abs(z-RADIUS)<=1&&next[i]>=0){
                // Freshly placed cells cannot be fed directly into the flower.
                if(ages[i]<=1&&ages[i]>=0)next[i]=-1;
                else if(old[i].isAir()){reward+=next[i]*60;consume=true;next[i]=-1;}
            }
        }
        if(consume)Arrays.fill(next,-1);
        if(reward>workshop.capacity()-workshop.mana())return;
        var changed=new ArrayList<Integer>();
        for(int i=0;i<next.length;i++){
            var pos=center.offset(i/WIDTH-RADIUS,0,i%WIDTH-RADIUS);
            if(next[i]>=0){
                changed.add(i);
                if(ages[i]<0&&!level.setBlock(pos,Jugtania.block("cell_block").defaultBlockState(),3)){rollback(level,center,old,snapshots,changed);return;}
                if(level.getBlockEntity(pos) instanceof BotanicalWorkshop cell){if(ages[i]<0)cell.owner(actor.getUUID());cell.life.generation=next[i];cell.life.parent=center.immutable();cell.life.lease=level.getGameTime();cell.setChanged();}
                else {rollback(level,center,old,snapshots,changed);return;}
            }else if(ages[i]>=0){changed.add(i);if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){rollback(level,center,old,snapshots,changed);return;}}
        }
        if(reward>0){workshop.generate(reward);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,center.getX()+.5,center.getY()+.6,center.getZ()+.5,20,.4,.3,.4,.03);}
    }
    private void rollback(ServerLevel level,BlockPos center,BlockState[] old,net.minecraft.nbt.CompoundTag[] snapshots,List<Integer> changed){
        for(int i:changed){var pos=center.offset(i/WIDTH-RADIUS,0,i%WIDTH-RADIUS);level.removeBlockEntity(pos);level.setBlock(pos,old[i],3);
            if(snapshots[i]!=null){var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,old[i],snapshots[i],level.registryAccess());if(restored==null)throw new IllegalStateException("Cannot restore cellular simulation at "+pos);level.setBlockEntity(restored);restored.setChanged();}
        }
    }
    void save(ValueOutput out){out.putInt("cell_generation",generation);out.putLong("cell_lease",lease);if(parent!=null)out.store("cell_parent",BlockPos.CODEC,parent);}
    void load(ValueInput in){generation=Math.clamp(in.getIntOr("cell_generation",0),0,100);lease=in.getLongOr("cell_lease",0);parent=in.read("cell_parent",BlockPos.CODEC).orElse(null);}
}
