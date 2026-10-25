package io.github.jimbozoomer.jugcraft.mixin;
import java.util.List;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Beardifier.class)
public interface JimtaBeardifierAccess {
    @Accessor("pieces") List<Beardifier.Rigid> jugcraft$pieces();
    @Accessor("junctions") List<JigsawJunction> jugcraft$junctions();
    @Accessor("affectedBox") BoundingBox jugcraft$affectedBox();
}
