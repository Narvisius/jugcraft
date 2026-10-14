package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Two ground corners, inclusive horizontally, with three blocks of headroom. */
public record AnimalCareZone(BlockPos first,BlockPos last,int tasks,List<Population> populations) {
    public static final int SHEAR=1,MILK=2,EGGS=4,BREED=8,SLAUGHTER=16,DEFAULT_TASKS=7,ALL=31,MAX_WIDTH=16,MAX_SPECIES=8,MAX_POPULATION=64;
    public record Population(Identifier type,int target,int keep){
        public static final Codec<Population> CODEC=RecordCodecBuilder.create(i->i.group(Identifier.CODEC.fieldOf("Type").forGetter(Population::type),Codec.INT.fieldOf("Target").forGetter(Population::target),Codec.INT.fieldOf("Keep").forGetter(Population::keep)).apply(i,Population::new));
        public Population { target=Math.clamp(target,0,MAX_POPULATION);keep=Math.clamp(keep,0,MAX_POPULATION); }
    }
    public AnimalCareZone(BlockPos first,BlockPos last,int tasks){this(first,last,tasks,List.of());}
    public AnimalCareZone { first=first.immutable();last=last.immutable();tasks&=ALL;var unique=new LinkedHashMap<Identifier,Population>();for(var p:populations){if(unique.size()>=MAX_SPECIES)break;unique.putIfAbsent(p.type(),p);}populations=List.copyOf(unique.values()); }
    public Population population(Identifier type){return populations.stream().filter(p->p.type().equals(type)).findFirst().orElse(null);}
    public AnimalCareZone withTasks(int mask){return new AnimalCareZone(first,last,mask,populations);}
    public AnimalCareZone withPopulation(Identifier type,int target,int keep,boolean remove){
        var list=new ArrayList<>(populations);int index=-1;for(int i=0;i<list.size();i++)if(list.get(i).type().equals(type)){index=i;break;}
        if(index>=0){if(remove)list.remove(index);else list.set(index,new Population(type,target,keep));}
        else if(!remove && list.size()<MAX_SPECIES)list.add(new Population(type,target,keep));
        return new AnimalCareZone(first,last,tasks,list);
    }
    public static boolean valid(BlockPos a,BlockPos b){return Math.abs((long)a.getX()-b.getX())<MAX_WIDTH && Math.abs((long)a.getZ()-b.getZ())<MAX_WIDTH && Math.abs((long)a.getY()-b.getY())<=3;}
    public boolean enabled(int task){return (tasks&task)!=0;}
    public int width(){return Math.abs(first.getX()-last.getX())+1;}
    public int depth(){return Math.abs(first.getZ()-last.getZ())+1;}
    public AABB box(){return new AABB(Math.min(first.getX(),last.getX()),Math.min(first.getY(),last.getY())+1,Math.min(first.getZ(),last.getZ()),Math.max(first.getX(),last.getX())+1,Math.max(first.getY(),last.getY())+4,Math.max(first.getZ(),last.getZ())+1);}
    public boolean contains(Vec3 point){return box().contains(point);}
    public void save(ValueOutput out){var child=out.child("AnimalZone");child.store("First",BlockPos.CODEC,first);child.store("Last",BlockPos.CODEC,last);child.putInt("Tasks",tasks);child.store("Populations",Population.CODEC.listOf(),populations);}
    public static AnimalCareZone load(ValueInput in){var c=in.child("AnimalZone");if(c.isEmpty())return null;var a=c.get().read("First",BlockPos.CODEC).orElse(null);var b=c.get().read("Last",BlockPos.CODEC).orElse(null);return a==null || b==null || !valid(a,b)?null:new AnimalCareZone(a,b,c.get().getIntOr("Tasks",DEFAULT_TASKS),c.get().read("Populations",Population.CODEC.listOf()).orElse(List.of()));}
}
