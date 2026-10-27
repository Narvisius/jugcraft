package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** One bounded spatial operation per server. Internal access is scoped, never an actor bypass flag. */
public final class SpatialReservation {
    private static final ThreadLocal<Integer> INTERNAL=ThreadLocal.withInitial(()->0);
    private static volatile ServerLevel level;
    private static volatile AABB bounds;
    private static volatile BlockPos port;
    private static volatile boolean frozen;
    // setPos runs during construction, before client entities receive their network ID.
    // Weak identity keys avoid Entity.hashCode(), which requires that ID, and survive ID changes.
    private static final java.util.Map<net.minecraft.world.entity.Entity,Boolean> REJECTED=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private SpatialReservation(){}
    static synchronized void reserve(ServerLevel world,AABB box,BlockPos device){
        if(level!=null)throw new IllegalStateException("Another spatial region is reserved");
        bounds=box;port=device.immutable();frozen=false;level=world;
    }
    static synchronized void release(){level=null;bounds=null;port=null;frozen=false;REJECTED.clear();}
    static void freeze(){frozen=true;}
    static boolean internalAccess(){return INTERNAL.get()>0;}
    public static void rejected(net.minecraft.world.entity.Entity entity,boolean rejected){if(rejected)REJECTED.put(entity,true);else REJECTED.remove(entity);}
    public static boolean rejected(net.minecraft.world.entity.Entity entity){return REJECTED.containsKey(entity);}
    public static void rejectEntry(net.minecraft.world.entity.Entity entity){
        var device=port;
        if(entity instanceof net.minecraft.server.level.ServerPlayer player&&device!=null&&player.level()==level&&level.getEntity(player.getUUID())!=player){
            // Login/dimension arrival has no old live position to retain. Land on the reserved port, outside the frame.
            internal(()->{player.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(device.above()));return null;});
        }else rejected(entity,true);
    }
    public static boolean blocked(Level world,BlockPos pos){
        return INTERNAL.get()==0&&reserved(world,pos);
    }
    public static boolean reserved(Level world,BlockPos pos){
        if(world!=level)return false;
        var box=bounds;var device=port;return box!=null&&(box.contains(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)||device!=null&&pos.getX()==device.getX()&&pos.getZ()==device.getZ()&&pos.getY()>=device.getY()&&pos.getY()<=device.getY()+2);
    }
    public static boolean containerReserved(net.minecraft.world.Container container){
        if(level==null)return false;
        if(container instanceof net.minecraft.world.level.block.entity.BlockEntity node)return reserved(node.getLevel(),node.getBlockPos());
        if(container instanceof io.github.jimbozoomer.jugcraft.mixin.SpatialContainerAccess pair)return containerReserved(pair.jugcraft$first())||containerReserved(pair.jugcraft$second());
        return false;
    }
    public static boolean interior(Level world,BlockPos pos){return frozen&&blocked(world,pos)&&!pos.equals(port);}
    public static boolean intersects(Level world,AABB box){var region=bounds;return INTERNAL.get()==0&&world==level&&region!=null&&region.intersects(box);}
    public static <T> T internal(Supplier<T> action){int old=INTERNAL.get();INTERNAL.set(old+1);try{return action.get();}finally{INTERNAL.set(old);}}
}
