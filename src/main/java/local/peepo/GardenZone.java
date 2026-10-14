package local.peepo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Compact corners, not a saved list of every block. Discovery is sliced by the garden scheduler. */
public record GardenZone(BlockPos first,BlockPos last,boolean orchard) {
    public static final Identifier FARM=PeepoMod.id("farmland_zone"),ORCHARD=PeepoMod.id("orchard_zone");
    public static final int CANOPY_HEIGHT=8,SCAN_SLICE=32;
    public GardenZone {first=first.immutable();last=last.immutable();}
    public static boolean valid(BlockPos a,BlockPos b){return AnimalCareZone.valid(a,b);}
    public int width(){return Math.abs(first.getX()-last.getX())+1;}
    public int depth(){return Math.abs(first.getZ()-last.getZ())+1;}
    public int minY(){return Math.min(first.getY(),last.getY());}
    public int groundMaxY(){return Math.max(first.getY(),last.getY());}
    public int maxY(){return groundMaxY()+(orchard?CANOPY_HEIGHT:2);}
    public boolean ground(BlockPos p){return contains(p) && p.getY()<=groundMaxY();}
    public int size(){return width()*depth()*(maxY()-minY()+1);}
    public BlockPos cell(int index){int height=maxY()-minY()+1;int column=orchard?index/height:index%(width()*depth());return new BlockPos(Math.min(first.getX(),last.getX())+column%width(),minY()+(orchard?index%height:index/(width()*depth())),Math.min(first.getZ(),last.getZ())+column/width());}
    public boolean contains(BlockPos p){return p.getX()>=Math.min(first.getX(),last.getX()) && p.getX()<=Math.max(first.getX(),last.getX()) && p.getZ()>=Math.min(first.getZ(),last.getZ()) && p.getZ()<=Math.max(first.getZ(),last.getZ()) && p.getY()>=minY() && p.getY()<=maxY();}
    public AABB box(){return new AABB(Math.min(first.getX(),last.getX()),minY(),Math.min(first.getZ(),last.getZ()),Math.max(first.getX(),last.getX())+1,maxY()+(orchard?1:2),Math.max(first.getZ(),last.getZ())+1);}
    public void save(ValueOutput out){var c=out.child("GardenZone");c.store("First",BlockPos.CODEC,first);c.store("Last",BlockPos.CODEC,last);c.putBoolean("Orchard",orchard);}
    public static GardenZone load(ValueInput in){var c=in.child("GardenZone");if(c.isEmpty())return null;var a=c.get().read("First",BlockPos.CODEC).orElse(null);var b=c.get().read("Last",BlockPos.CODEC).orElse(null);return a==null || b==null || !valid(a,b)?null:new GardenZone(a,b,c.get().getBooleanOr("Orchard",false));}
}
