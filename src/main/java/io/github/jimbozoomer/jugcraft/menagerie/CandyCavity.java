package io.github.jimbozoomer.jugcraft.menagerie;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** New-chunk Candy terrain. Every column and decoration anchor depends only on seed/coordinates.
 * Writes are confined to the current chunk, including templates spanning chunk borders.
 * The supplied cake/arch/tree algorithms inform these native 26.3 implementations; see the feature record.
 */
public final class CandyCavity implements Feature {
    public static final CandyCavity INSTANCE = new CandyCavity();
    public static final MapCodec<CandyCavity> CODEC = MapCodec.unit(INSTANCE);
    public static final int SODA_Y = -40;
    private static final String[] COLORS = {"pink", "purple", "yellow", "light_blue", "orange", "lime"};
    public record Column(int floor, int roof, boolean river) {}
    private CandyCavity() {}
    @Override public MapCodec<? extends Feature> codec() { return CODEC; }
    static BlockState block(String name) { return Objects.requireNonNull(Menagerie.BLOCKS.get("cavern_" + name), name).defaultBlockState(); }
    static long mix(long n) { n ^= n >>> 30; n *= 0xbf58476d1ce4e5b9L; n ^= n >>> 27; n *= 0x94d049bb133111ebL; return n ^ n >>> 31; }
    static long anchorSeed(long seed, int x, int z) { return mix(seed ^ (long)x * 341873128712L ^ (long)z * 132897987541L); }
    private static double value(long seed, int x, int z) { return (anchorSeed(seed, x, z) >>> 11) * 0x1.0p-53; }
    private static double noise(long seed, double x, double z) {
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double a = x - ix, b = z - iz; a = a * a * (3 - 2 * a); b = b * b * (3 - 2 * b);
        double lo = value(seed, ix, iz) * (1 - a) + value(seed, ix + 1, iz) * a;
        double hi = value(seed, ix, iz + 1) * (1 - a) + value(seed, ix + 1, iz + 1) * a;
        return lo * (1 - b) + hi * b;
    }
    /** Meandering connected rivers, independent of the order in which chunks are generated. */
    public static double riverZ(long seed, int x, int band) {
        return band * 160.0 + 80 + Math.sin(x * .017 + (seed & 1023)) * 18
                + (noise(seed ^ 7143, x / 70.0, band) - .5) * 24;
    }
    public static Column column(long seed, int x, int z) {
        int band = Math.floorDiv(z, 160);
        double river = Math.abs(z - riverZ(seed, x, band));
        double n = noise(seed, x / 86.0, z / 86.0) * .8 + noise(seed ^ 9321, x / 31.0, z / 31.0) * .2;
        int terrace = Math.min(4, (int)(n * 5)), floor = -38 + terrace * 7;
        if (river < 5) floor = SODA_Y - 3 - (river < 2 ? 2 : 0);
        else if (river < 23) floor = (int)Math.floor(SODA_Y + (floor - SODA_Y) * (river - 5) / 18);
        int bottleX=Math.floorDiv(x,160)*160+80;
        double bottleZ=riverZ(seed,bottleX,band);
        if(Math.abs(x-bottleX)<=7 && z>=bottleZ-27 && z<=bottleZ) floor=Math.min(floor,SODA_Y-5);
        int roof = 22 + (int)(noise(seed ^ 341, x / 100.0, z / 100.0) * 8);
        return new Column(floor, roof, river < 5);
    }
    public static BlockState terrain(long seed, int x, int y, int z, Column c) {
        if (y > c.floor && y < c.roof) return c.river && y <= SODA_Y ? block("purple_soda") : Blocks.CAVE_AIR.defaultBlockState();
        if (y == c.floor) return block(c.river ? "block_of_chocolate" : "block_of_frosted_chocolate");
        if (y >= c.roof) return block("cake_layer");
        // Narrow, winding seams remain visible in the plateau faces, not just as loose ore piles.
        double vein = Math.abs(noise(seed ^ 553, x / 29.0, z / 29.0) - .5);
        if (vein < .025 && Math.floorMod(y + (int)(noise(seed ^ 72, x / 40.0, z / 40.0) * 12), 13) < 2)
            return block("rock_candy_" + COLORS[Math.floorMod((int)(anchorSeed(seed, Math.floorDiv(x, 48), Math.floorDiv(z, 48)) >>> 16), COLORS.length)]);
        return block(Math.floorMod(y + 56, 7) < 2 ? "cake_layer" : "block_of_chocolate");
    }
    /** Per-placement coordinate cache; sampling never requests neighbouring chunks. */
    private record CandyBounds(net.minecraft.world.level.biome.BiomeResolver resolver, Map<Long, Boolean> columns) {
        boolean contains(int x, int z) {
            long key = ((long)x << 32) ^ (z & 0xffffffffL);
            return columns.computeIfAbsent(key, ignored -> resolver.getNoiseBiome(Math.floorDiv(x, 4), -8, Math.floorDiv(z, 4))
                    .is(Jugcraft.id("cavern_candy_cavity")));
        }
    }
    @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource ignored, BlockPos origin) {
        int sx = Math.floorDiv(origin.getX(), 16) * 16, sz = Math.floorDiv(origin.getZ(), 16) * 16;
        long seed = level.getSeed(); boolean placed = false;
        var bounds = new CandyBounds(generator.getBiomeSource().createUncachedResolver(level.getLevel().getChunkSource().randomState()), new HashMap<>());
        var clip = new BoundingBox(sx, -53, sz, sx + 15, 34, sz + 15);
        var at = new BlockPos.MutableBlockPos();
        for (int x = sx; x < sx + 16; x++) for (int z = sz; z < sz + 16; z++) {
            if (!bounds.contains(x, z)) continue;
            Column c = column(seed, x, z);
            for (int y = -53; y <= c.roof + 4; y++) {
                at.set(x, y, z);
                if (level.ensureCanWrite(at) && !level.getBlockState(at).is(Blocks.BEDROCK) && level.getBlockEntity(at) == null)
                    level.setBlock(at, terrain(seed, x, y, z, c), Block.UPDATE_CLIENTS);
            }
            placed = true;
        }
        if (!placed) return false;
        // A bounded 3x3 anchor neighbourhood reconstructs complete cross-chunk decorations.
        for (int gx = Math.floorDiv(sx, 32) - 1; gx <= Math.floorDiv(sx + 15, 32) + 1; gx++)
            for (int gz = Math.floorDiv(sz, 32) - 1; gz <= Math.floorDiv(sz + 15, 32) + 1; gz++) {
                var r = RandomSource.create(anchorSeed(seed, gx, gz));
                int x = gx * 32 + 8 + r.nextInt(16), z = gz * 32 + 8 + r.nextInt(16);
                Column c = column(seed, x, z); int kind = r.nextInt(4);
                if(x+8<clip.minX()||x-8>clip.maxX()||z+8<clip.minZ()||z-8>clip.maxZ())continue;
                if (c.river || !footprint(level, bounds, x, z, 8)) continue;
                BlockPos base = new BlockPos(x, c.floor + 1, z);
                switch (kind) {
                    case 0 -> lollipop(level, bounds, clip, base, r);
                    case 1 -> licoroot(level, bounds, clip, base, r);
                    case 2 -> gobstopper(level, bounds, clip, base, r);
                    default -> candyCane(level, bounds, clip, base, r);
                }
            }
        arches(level, bounds, clip);
        bottles(level, bounds, clip);
        return true;
    }
    private static boolean footprint(WorldGenLevel l, CandyBounds bounds, int x, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx += Math.max(1, radius))
            for (int dz = -radius; dz <= radius; dz += Math.max(1, radius))
                if (!bounds.contains(x + dx, z + dz)) return false;
        return true;
    }
    private static void put(WorldGenLevel l, CandyBounds bounds, BoundingBox clip, BlockPos p, BlockState state) {
        if (!clip.isInside(p) || !l.ensureCanWrite(p) || !bounds.contains(p.getX(), p.getZ()) || l.getBlockEntity(p) != null || l.getBlockState(p).is(Blocks.BEDROCK)) return;
        if (p.getY() >= column(l.getSeed(), p.getX(), p.getZ()).roof) return;
        l.setBlock(p, state, Block.UPDATE_CLIENTS);
    }
    private static void template(WorldGenLevel l, BoundingBox clip, String name, BlockPos p, RandomSource r) {
        var template = l.getLevel().getStructureTemplateManager().get(Jugcraft.id(name)).orElseThrow();
        var size = template.getSize();
        if (!clip.intersects(new BoundingBox(p.getX(), p.getY(), p.getZ(), p.getX()+size.getX()-1, p.getY()+size.getY()-1, p.getZ()+size.getZ()-1))) return;
        template.placeInWorld(l, p, p, new StructurePlaceSettings().setBoundingBox(clip).setIgnoreEntities(true), r, Block.UPDATE_CLIENTS);
    }
    private static void lollipop(WorldGenLevel l, CandyBounds bounds, BoundingBox clip, BlockPos p, RandomSource r) {
        boolean big = r.nextBoolean(); int pole = big ? 7 : 4, variant = r.nextInt(big ? 5 : 4), color = r.nextInt(COLORS.length);
        String name = "cavern/candy/" + (big ? "big" : "small") + "_lollipop_" + variant + "_" + color;
        var size = l.getLevel().getStructureTemplateManager().get(Jugcraft.id(name)).orElseThrow().getSize();
        if (p.getY()+pole+size.getY() >= column(l.getSeed(),p.getX(),p.getZ()).roof) return;
        for (int y=0;y<pole;y++) put(l,bounds,clip,p.above(y),block("stripped_candy_cane_block"));
        template(l,clip,name,p.offset(-size.getX()/2,pole,-size.getZ()/2),r);
    }
    private static BlockState axis(BlockState s, Direction.Axis axis) { return s.hasProperty(RotatedPillarBlock.AXIS) ? s.setValue(RotatedPillarBlock.AXIS,axis) : s; }
    private static void licoroot(WorldGenLevel l, CandyBounds bounds, BoundingBox clip, BlockPos p, RandomSource r) {
        int height=9+r.nextInt(5);
        for(int y=0;y<height;y++) put(l,bounds,clip,p.above(y),block("licoroot"));
        for(Direction d:Direction.Plane.HORIZONTAL) {
            int length=4+r.nextInt(3); var tip=p.above(height-2);
            // Square curls, rather than the generic leafy-tree canopy used previously.
            Direction[] turns={d,Direction.UP,d.getOpposite(),Direction.DOWN};
            for(int turn=0;turn<4;turn++) for(int n=0;n<length-turn;n++) {
                tip=tip.relative(turns[turn]); put(l,bounds,clip,tip,axis(block("licoroot"),turns[turn].getAxis()));
                if(turn==0&&n%2==1) for(int v=1;v<=3;v++) {
                    var vine=block("licoroot_vine");
                    for(var property:vine.getProperties()) {
                        if(property.getName().equals("facing")) vine=with(vine,property,"down");
                        if(property.getName().equals("end")) vine=with(vine,property,Boolean.toString(v==3));
                    }
                    put(l,bounds,clip,tip.below(v),vine);
                }
            }
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static BlockState with(BlockState s,net.minecraft.world.level.block.state.properties.Property p,String value) {
        return (BlockState)p.getValue(value).map(v->s.setValue(p,(Comparable)v)).orElse(s);
    }
    private static void gobstopper(WorldGenLevel l, CandyBounds bounds, BoundingBox clip, BlockPos p, RandomSource r) {
        int radius=5+r.nextInt(3),color=r.nextInt(COLORS.length); boolean bite=r.nextBoolean(); p=p.above(2);
        for(int x=-radius;x<=radius;x++) for(int y=-radius;y<=radius;y++) for(int z=-radius;z<=radius;z++) {
            double distance=Math.sqrt(x*x+y*y+z*z);
            if(distance>radius || bite&&x>0&&z>0&&y>0) continue;
            String shade=distance>radius-1?"white":COLORS[Math.floorMod((int)distance+color,COLORS.length)];
            put(l,bounds,clip,p.offset(x,y,z),block("rock_candy_"+shade));
        }
    }
    private static void candyCane(WorldGenLevel l, CandyBounds bounds, BoundingBox clip, BlockPos p, RandomSource r) {
        int h=7+r.nextInt(4); Direction d=Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for(int y=0;y<h;y++) put(l,bounds,clip,p.above(y),block("candy_cane_block"));
        for(int n=1;n<=3;n++) put(l,bounds,clip,p.above(h-1).relative(d,n),axis(block("candy_cane_block"),d.getAxis()));
        put(l,bounds,clip,p.above(h-2).relative(d,3),block("candy_cane_block"));
    }
    private static void arches(WorldGenLevel l, CandyBounds bounds,BoundingBox clip) {
        for(int gx=Math.floorDiv(clip.minX(),112)-1;gx<=Math.floorDiv(clip.maxX(),112)+1;gx++)
            for(int gz=Math.floorDiv(clip.minZ(),112)-1;gz<=Math.floorDiv(clip.maxZ(),112)+1;gz++) {
                var r=RandomSource.create(anchorSeed(l.getSeed()^991,gx,gz));
                if(r.nextInt(4)==0)continue;
                int x=gx*112+56,z=gz*112+56;boolean alongX=r.nextBoolean();
                if(x+18<clip.minX()||x-18>clip.maxX()||z+18<clip.minZ()||z-18>clip.maxZ()||!footprint(l,bounds,x,z,18))continue;
                int floor=Math.max(column(l.getSeed(),x+(alongX?12:0),z+(alongX?0:12)).floor,column(l.getSeed(),x-(alongX?12:0),z-(alongX?0:12)).floor);
                int center=Math.min(floor+8,column(l.getSeed(),x,z).roof-17);
                String frosting=List.of("block_of_frosting","block_of_vanilla_frosting","block_of_chocolate_frosting").get(r.nextInt(3));
                for(int u=-16;u<=16;u++)for(int y=-16;y<=16;y++)for(int depth=-3;depth<=3;depth++) {
                    double ring=Math.sqrt(u*u+y*y); if(Math.pow(ring-11,2)+depth*depth>16)continue;
                    var at=new BlockPos(x+(alongX?u:depth),center+y,z+(alongX?depth:u));
                    String b=depth>=1?frosting:"dough_block";
                    if(depth>=2&&Math.floorMod(anchorSeed(l.getSeed(),at.getX()+y,at.getZ()-y),17)==0)b="rock_candy_"+COLORS[Math.floorMod(u+y,COLORS.length)];
                    put(l,bounds,clip,at,block(b));
                }
            }
    }
    private static void bottles(WorldGenLevel l, CandyBounds bounds,BoundingBox clip) {
        for(int gx=Math.floorDiv(clip.minX(),160)-1;gx<=Math.floorDiv(clip.maxX(),160)+1;gx++)
            for(int gz=Math.floorDiv(clip.minZ(),160)-1;gz<=Math.floorDiv(clip.maxZ(),160)+1;gz++) {
                int x=gx*160+80,z=(int)Math.round(riverZ(l.getSeed(),x,gz));
                if(x+6<clip.minX()||x-6>clip.maxX()||z+1<clip.minZ()||z-26>clip.maxZ()||!footprint(l,bounds,x,z-13,14))continue;
                // The sideways supplied bottle opens directly onto the connected river.
                template(l,clip,"cavern/candy/soda_bottle",new BlockPos(x-5,SODA_Y-4,z-25),RandomSource.create(anchorSeed(l.getSeed(),gx,gz)));
            }
    }
}
