package io.github.jimbozoomer.jugcraft.mixin;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaStructures;
import java.util.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve each original pool piece's terrain setting while retaining all vanilla junctions. */
@Mixin(Beardifier.class)
public abstract class JimtaTerrainMixin {
    @Inject(method="forStructuresInChunk",at=@At("RETURN"),cancellable=true)
    private static void jugcraft$individualTerrain(StructureManager manager,ChunkPos chunk,CallbackInfoReturnable<Beardifier> cir){
        var replacements=new HashMap<BoundingBox,TerrainAdjustment>();
        for(var start:manager.startsForStructure(chunk.x(),chunk.z(),s->s.terrainAdaptation()!=TerrainAdjustment.NONE))for(var piece:start.getPieces()){
            if(piece instanceof PoolElementStructurePiece pool&&pool.getElement() instanceof JimtaStructures.TerrainElement element&&piece.isCloseToChunk(chunk,12))replacements.put(pool.getBoundingBox(),element.adjustment());
        }
        if(replacements.isEmpty())return;
        var old=(JimtaBeardifierAccess)(Object)cir.getReturnValue();var result=new ArrayList<Beardifier.Rigid>();boolean changed=false;
        for(var rigid:old.jugcraft$pieces()){
            var adjustment=replacements.get(rigid.box());if(adjustment==null){result.add(rigid);continue;}
            result.add(new Beardifier.Rigid(rigid.box(),adjustment,rigid.groundLevelDelta()));changed=true;
        }
        if(changed)cir.setReturnValue(new Beardifier(List.copyOf(result),old.jugcraft$junctions(),old.jugcraft$affectedBox()));
    }
}
