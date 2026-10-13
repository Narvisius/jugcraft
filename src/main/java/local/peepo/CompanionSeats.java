package local.peepo;

import java.util.*;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import io.github.jimbozoomer.jugcraft.tower.SeatEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** Adapts player furniture and natural perches without adding block entities to every seat. */
public final class CompanionSeats {
    public static final TagKey<Block> EXTRA_SEATS = TagKey.create(Registries.BLOCK, PeepoMod.id("seats"));
    private record Claim(UUID npc, long expires) {}
    private static final Map<Level, Map<BlockPos, Claim>> CLAIMS = new WeakHashMap<>();
    public static boolean isReserved(Level level, BlockPos pos) {
        var claims=CLAIMS.get(level);
        if(claims==null)return false;
        var claim=claims.get(pos);
        if(claim==null)return false;
        if(claim.expires < level.getGameTime() || !(level instanceof ServerLevel s) || s.getEntity(claim.npc)==null || !s.getEntity(claim.npc).isAlive()) {
            claims.remove(pos); return false;
        }
        return true;
    }
    static boolean externalAvailable(PeepoEntity npc,BlockPos pos){return !isReserved(npc.level(),pos) || CLAIMS.get(npc.level()).get(pos).npc.equals(npc.getUUID());}
    static void reserveExternal(PeepoEntity npc,BlockPos pos,int ticks){
        var claims=CLAIMS.computeIfAbsent(npc.level(),l->new HashMap<>());var current=claims.get(pos);
        long now=npc.level().getGameTime();
        if(current==null || !current.npc.equals(npc.getUUID()) || current.expires-now<ticks/2)
            claims.put(pos.immutable(),new Claim(npc.getUUID(),now+ticks));
    }
    static void releaseExternal(PeepoEntity npc,BlockPos pos){var map=CLAIMS.get(npc.level());if(map!=null && map.containsKey(pos) && map.get(pos).npc.equals(npc.getUUID()))map.remove(pos);}
    private static boolean fence(BlockState state) { return state.getBlock() instanceof FenceBlock || state.is(BlockTags.FENCES); }
    private static boolean connected(BlockState state,int dx,int dz) {
        String name=dz<0?"north":dz>0?"south":"";
        if(dx!=0)name+=(name.isEmpty()?"":"_")+(dx>0?"east":"west");
        for(var property:state.getProperties())if(property.getName().equals(name) && property instanceof BooleanProperty b)return state.getValue(b);
        return false;
    }
    private static final List<BlockPos> OFFSETS=new ArrayList<>();
    static {
        for(int x=-6;x<=6;x++)for(int y=-2;y<=2;y++)for(int z=-6;z<=6;z++)OFFSETS.add(new BlockPos(x,y,z));
        OFFSETS.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getY()*p.getY()+p.getZ()*p.getZ()));
    }
    private static final class Scan { BlockPos center; int cursor; Scan(BlockPos center){this.center=center;} }
    private static final Map<PeepoEntity,Scan> SCANS=new WeakHashMap<>();
    private static boolean operator(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(Identifier.fromNamespaceAndPath("jugcraft","operator_chair"));
    }
    private static boolean supported(BlockState state) {
        return state.getBlock() instanceof Seat.Sittable || operator(state) || state.is(EXTRA_SEATS)
            || state.getBlock() instanceof CompanionBedBlock || fence(state)
            || state.getBlock() instanceof BedBlock && state.getValue(BlockStateProperties.BED_PART)==BedPart.FOOT;
    }
    public static Surface find(PeepoEntity npc, Map<BlockPos,Long> excluded) {
        List<Surface> candidates=new ArrayList<>();
        Scan scan=SCANS.computeIfAbsent(npc,n->new Scan(n.blockPosition()));
        if(scan.center.distSqr(npc.blockPosition())>16){scan.center=npc.blockPosition();scan.cursor=0;}
        for(int reads=0;reads<128;reads++) {
            if(scan.cursor>=OFFSETS.size()){scan.cursor=0;scan.center=npc.blockPosition();break;}
            BlockPos pos=scan.center.offset(OFFSETS.get(scan.cursor++));
            if(!npc.level().hasChunkAt(pos) || excluded.containsKey(pos))continue;
            var state=npc.level().getBlockState(pos);
            if(!supported(state))continue;
            if(fence(state)) {
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                    if(dx==0 && dz==0 || !connected(state,dx,dz))continue;
                    BlockPos other=pos.offset(dx,0,dz);
                    if(!npc.level().hasChunkAt(other))continue;
                    var neighbor=npc.level().getBlockState(other);
                    if(!fence(neighbor) || !connected(neighbor,-dx,-dz))continue;
                    for(int side:new int[]{-1,1})candidates.add(new Surface(npc.level(),pos,state,dx,dz,side));
                }
                continue;
            }
            for(Direction facing:Direction.Plane.HORIZONTAL) {
                if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && !(state.getBlock() instanceof BedBlock)
                    && !(state.getBlock() instanceof CompanionBedBlock) && facing!=state.getValue(BlockStateProperties.HORIZONTAL_FACING))continue;
                if(state.getBlock() instanceof CompanionBedBlock && facing!=state.getValue(CompanionBedBlock.FACING).getOpposite())continue;
                var seat=new Surface(npc.level(),pos,state,facing);
                candidates.add(seat);
            }
        }
        candidates.sort(Comparator.comparingDouble(s->npc.position().distanceToSqr(s.approachPosition())));
        int checks=0,paths=0;
        for(var seat:candidates) {
            if(++checks>8 || paths>=2)break;
            if(!npc.orders.station(seat) || !seat.availableTo(npc))continue;
            if(!CompanionBudget.path(npc))break;
            paths++;
            var path=npc.getNavigation().createPath(BlockPos.containing(seat.approachPosition()),0);
            if(path!=null && path.canReach())return seat;
        }
        return null;
    }
    public static final class Surface implements CompanionStation {
        public final BlockPos pos;
        private final Level level;
        private final BlockState original;
        private final Direction facing;
        private boolean mounted;
        private int railX,railZ;
        private Vec3 normal;
        private BlockState neighborState;
        private long nextValidation;
        private boolean validSpace;
        private boolean rail(){return railX!=0 || railZ!=0;}
        private BlockPos other(){return pos.offset(railX,0,railZ);}
        Surface(Level level,BlockPos pos,BlockState state,int dx,int dz,int side) {
            this(level,pos,state,Direction.NORTH);railX=dx;railZ=dz;
            normal=new Vec3(-dz*side,0,dx*side).normalize();neighborState=level.getBlockState(other());
        }
        Surface(Level level,BlockPos pos,BlockState state,Direction facing) { this.level=level;this.pos=pos;original=state;this.facing=facing; }
        private boolean edge() { return original.getBlock() instanceof BedBlock || original.getBlock() instanceof CompanionBedBlock; }
        private Vec3 entrance() {
            if(rail())return Vec3.atBottomCenterOf(BlockPos.containing(pos.getX()+.5+railX*.5+normal.x*.9,pos.getY(),pos.getZ()+.5+railZ*.5+normal.z*.9));
            if(level.getBlockEntity(pos) instanceof CompanionBedEntity bed)return bed.entrance();
            return Vec3.atBottomCenterOf(pos.relative(facing));
        }
        private Vec3 seatPosition() {
            if(rail())return new Vec3(pos.getX()+.5+railX*.5+normal.x*.10,pos.getY()+15/16.0-.094,pos.getZ()+.5+railZ*.5+normal.z*.10);
            double height=original.getBlock() instanceof Seat.Sittable s ? s.seatHeight(original) : operator(original) ? .56
                : original.getCollisionShape(level,pos).max(Direction.Axis.Y);
            // Put the hips just inside the visible front edge, letting the tiny legs hang free.
            var shape=original.getShape(level,pos);
            double extent=facing.getAxisDirection()==Direction.AxisDirection.POSITIVE ? shape.max(facing.getAxis())-.5 : .5-shape.min(facing.getAxis());
            double offset=edge() ? .49 : Math.max(.20,Math.min(.49,extent-.015));
            return new Vec3(pos.getX()+.5+facing.getStepX()*offset,pos.getY()+height-.094,pos.getZ()+.5+facing.getStepZ()*offset);
        }
        public Kind kind() { return Kind.CHAIR; }
        public BlockPos stationPosition(){return pos;}
        public Vec3 approachPosition() { return mounted?seatPosition():entrance(); }
        public boolean availableTo(PeepoEntity npc) {
            if(!level.hasChunkAt(pos) || level.getBlockState(pos)!=original || npc.isPassenger())return false;
            if(rail() && (!level.hasChunkAt(other()) || level.getBlockState(other())!=neighborState))return false;
            if(rail() && isReserved(level,other()) && !CLAIMS.get(level).get(other()).npc.equals(npc.getUUID()))return false;
            if(isReserved(level,pos) && !CLAIMS.get(level).get(pos).npc.equals(npc.getUUID()))return false;
            if(original.hasProperty(BlockStateProperties.OCCUPIED) && original.getValue(BlockStateProperties.OCCUPIED))return false;
            if(level.getBlockEntity(pos) instanceof CompanionBedEntity bed && bed.hasOccupant())return false;
            if(level.getGameTime()<nextValidation)return validSpace;
            nextValidation=level.getGameTime()+(mounted?20:1);
            validSpace=false;
            if(!rail() && !edge() && (!Seat.at(level,pos).isEmpty() || !level.getEntitiesOfClass(SeatEntity.class,new AABB(pos),e->e.isVehicle()).isEmpty()))return false;
            Vec3 entry=entrance(),at=seatPosition();BlockPos floor=BlockPos.containing(entry).below();
            if(!level.hasChunkAt(floor) || !level.hasChunkAt(BlockPos.containing(entry).above()))return false;
            validSpace=level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)
                && CompanionHazards.safeAt(npc,entry) && CompanionHazards.safeAt(npc,at) && level.noCollision(npc,new AABB(entry.x-.22,entry.y,entry.z-.22,entry.x+.22,entry.y+1,entry.z+.22))
                && headroom(npc,new AABB(at.x-.19,at.y+.11,at.z-.19,at.x+.19,at.y+npc.getBbHeight(),at.z+.19));
            return validSpace;
        }
        private boolean headroom(PeepoEntity npc,AABB box) {
            if(!rail())return level.noCollision(npc,box);
            // Fences' collision barriers extend above their visible rails. Check visible fence
            // geometry here, retaining real collision checks for every other nearby block.
            for(BlockPos check:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))) {
                if(!level.hasChunkAt(check))return false;
                var state=level.getBlockState(check);
                var shape=state.getCollisionShape(level,check);
                if(fence(state)) {
                    // The selection shape also fills rails to 16 pixels, not their visible 15.
                    // Clip only the arms; preserve the full-height four-pixel center post.
                    shape=Shapes.or(Block.box(6,0,6,10,16,10),Shapes.join(
                        state.getShape(level,check),Block.box(0,0,0,16,15,16),BooleanOp.AND));
                }
                if(Shapes.joinIsNotEmpty(shape.move(check.getX(),check.getY(),check.getZ()),Shapes.create(box),BooleanOp.AND))return false;
            }
            return true;
        }
        public boolean claim(PeepoEntity npc) {
            if(!availableTo(npc))return false;
            var claims=CLAIMS.computeIfAbsent(level,l->new HashMap<>());
            claims.entrySet().removeIf(e->e.getValue().expires<level.getGameTime());
            Claim claim=new Claim(npc.getUUID(),level.getGameTime()+240);claims.put(pos,claim);if(rail())claims.put(other(),claim);return true;
        }
        public boolean occupy(PeepoEntity npc) {
            if(!availableTo(npc) || !isReserved(level,pos))return false;
            var current=CLAIMS.get(level).get(pos);
            if(current.expires-level.getGameTime()<20) {
                Claim claim=new Claim(npc.getUUID(),level.getGameTime()+40);
                CLAIMS.get(level).put(pos,claim);if(rail())CLAIMS.get(level).put(other(),claim);
            }
            if(!npc.setRestMode(CompanionEnergy.Rest.SITTING))return false;
            mounted=true;npc.setNoGravity(true);npc.setBedExit(BlockPos.containing(entrance()));
            Vec3 at=seatPosition();float yaw=rail()?(float)Math.toDegrees(Math.atan2(-normal.x,normal.z)):facing.toYRot();
            CompanionMotion.position(npc,at,yaw);npc.setDeltaMovement(Vec3.ZERO);npc.resetFallDistance();npc.getNavigation().stop();return true;
        }
        public void release(PeepoEntity npc) {
            var claims=CLAIMS.get(level);var claim=claims==null?null:claims.get(pos);
            if(claim!=null && claim.npc.equals(npc.getUUID()))claims.remove(pos);
            if(rail() && claims!=null) {
                var adjacent=claims.get(other());if(adjacent!=null && adjacent.npc.equals(npc.getUUID()))claims.remove(other());
            }
            if(mounted) { npc.setRestMode(CompanionEnergy.Rest.NONE);npc.setNoGravity(false);npc.leaveCompanionBed(); }
            mounted=false;
        }
    }
}
