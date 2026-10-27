package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Bounded geometric selection only; all placement/mining still uses civil workshop claims and real supplies. */
public final class FillerPatterns {
    public static final List<String> NAMES=List.of("Fill","Box","Frame","Floor","Walls","Sphere","Pyramid","Stairs","Clear");
    private FillerPatterns(){}
    public static boolean selected(int pattern,boolean inverted,BlockPos at,BlockPos min,BlockPos max,Direction facing){
        if(min==null||max==null||at.getX()<min.getX()||at.getX()>max.getX()||at.getY()<min.getY()||at.getY()>max.getY()||at.getZ()<min.getZ()||at.getZ()>max.getZ())return false;
        int x=at.getX()-min.getX(),y=at.getY()-min.getY(),z=at.getZ()-min.getZ();
        int sx=max.getX()-min.getX()+1,sy=max.getY()-min.getY()+1,sz=max.getZ()-min.getZ()+1;
        boolean bx=x==0||x==sx-1,by=y==0||y==sy-1,bz=z==0||z==sz-1;
        double nx=(2.0*x+1-sx)/sx,ny=(2.0*y+1-sy)/sy,nz=(2.0*z+1-sz)/sz;
        int along=switch(facing){case EAST->x;case WEST->sx-1-x;case SOUTH->z;default->sz-1-z;};
        int length=facing.getAxis()==Direction.Axis.X?sx:sz;
        boolean selected=switch(pattern){
            case 1->bx||by||bz;
            case 2->(bx?1:0)+(by?1:0)+(bz?1:0)>=2;
            case 3->y==0;
            case 4->bx||bz;
            case 5->nx*nx+ny*ny+nz*nz<=1;
            case 6->Math.abs(nx)<=1-y/(double)sy&&Math.abs(nz)<=1-y/(double)sy;
            case 7->y<(along+1)*sy/(double)length;
            default->true;
        };
        return inverted?!selected:selected;
    }
}
