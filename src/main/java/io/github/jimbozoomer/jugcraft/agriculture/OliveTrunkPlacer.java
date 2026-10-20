package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/** Low, connected forks under native foliage clusters; a four-sapling olive keeps a squat 2x2 base. */
public final class OliveTrunkPlacer extends TrunkPlacer {
	public static final MapCodec<OliveTrunkPlacer> CODEC=RecordCodecBuilder.mapCodec(i->trunkPlacerParts(i)
			.and(Codec.BOOL.optionalFieldOf("giant",false).forGetter(p->p.giant)).apply(i,OliveTrunkPlacer::new));
	public static final TrunkPlacerType<OliveTrunkPlacer> TYPE=new TrunkPlacerType<>(CODEC);
	private final boolean giant;
	public OliveTrunkPlacer(int height,int randomA,int randomB,boolean giant){super(height,randomA,randomB);this.giant=giant;}
	@Override protected TrunkPlacerType<?> type(){return TYPE;}
	@Override public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level,BiConsumer<BlockPos,BlockState> setter,
			RandomSource random,int height,BlockPos origin,TreeFeature tree){
		var crowns=new ArrayList<FoliagePlacer.FoliageAttachment>();
		int width=giant?2:1;
		for(int x=0;x<width;x++)for(int z=0;z<width;z++) {
			var base=origin.offset(x,0,z); placeBelowTrunkBlock(level,setter,random,base.below(),tree);
			for(int y=0;y<height;y++) if(!placeLog(level,setter,random,base.above(y),tree))break;
		}
		crowns.add(new FoliagePlacer.FoliageAttachment(origin.above(height),0,giant));
		Direction first=Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int count=giant?4:2+random.nextInt(2);
		for(int branch=0;branch<count;branch++){
			Direction side=first; for(int turn=0;turn<branch;turn++)side=side.getClockWise();
			int baseY=giant?2+random.nextInt(2):1+random.nextInt(2);
			BlockPos end=origin.offset(giant&&side==Direction.EAST?1:0,baseY,giant&&side==Direction.SOUTH?1:0);
			int length=giant?2:1+random.nextInt(2);
			for(int step=0;step<length;step++){
				BlockPos next=end.relative(side); final var axis=side.getAxis();
				if(!placeLog(level,setter,random,next,tree,s->s.setValue(RotatedPillarBlock.AXIS,axis)))break;
				end=next;
				if(step==0&&baseY+1<height&&placeLog(level,setter,random,end.above(),tree))end=end.above();
			}
			// Low forks feed a broad upper crown, leaving the base visible instead of a tiered cone.
			while(end.getY()<origin.getY()+height-1){
				if(!placeLog(level,setter,random,end.above(),tree))break;
				end=end.above();
			}
			// A crown is attached only to an actual log, including a branch shortened by an obstruction.
			crowns.add(new FoliagePlacer.FoliageAttachment(end.above(),0,false));
		}
		return crowns;
	}
}
