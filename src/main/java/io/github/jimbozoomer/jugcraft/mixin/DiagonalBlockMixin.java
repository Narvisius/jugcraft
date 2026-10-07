package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import java.util.Arrays;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Diagonal properties in every block that has them (fences, bars and walls; see DiagonalStateMixin):
 * <ul>
 * <li>They start false. A block's constructor names its default state by setting each of its own properties on the state
 * definition's first state, whose booleans are all true, so the four diagonal properties would otherwise start true.
 * This clears them in every default state that has them, whatever subclass registers it.</li>
 * <li>A block's shape function ignores them. Fences, bars and walls work out a shape for each of their states up front,
 * ignoring only the properties they name (waterlogged). Their shapes do not depend on the diagonals, which are added
 * afterwards (DiagonalConnections.Arms), so naming the diagonals too keeps one shape, not sixteen, for each set of
 * straight sides: a sixteenth of the shapes to work out and keep.</li>
 * </ul>
 */
@Mixin(Block.class)
public abstract class DiagonalBlockMixin {
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState jugcraft$noDiagonals(BlockState state) {
		return DiagonalConnections.withoutDiagonals(state);
	}

	@ModifyVariable(method = "getShapeForEachState(Ljava/util/function/Function;[Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/util/function/Function;",
			at = @At("HEAD"), argsOnly = true)
	private Property<?>[] jugcraft$sameShapeWhateverTheDiagonals(Property<?>[] ignored) {
		// Jugcraft's own four property objects, not another mod's properties that share a name.
		int diagonals = 0;
		for (Property<?> property : ((Block) (Object) this).getStateDefinition().getProperties()) {
			for (DiagonalConnections.Diagonal diagonal : DiagonalConnections.Diagonal.ALL) {
				diagonals += property == diagonal.property ? 1 : 0;
			}
		}
		for (Property<?> property : ignored) {
			diagonals -= property == DiagonalConnections.NORTH_EAST ? 4 : 0;
		}
		if (diagonals != 4) {
			return ignored;
		}
		Property<?>[] more = Arrays.copyOf(ignored, ignored.length + 4);
		int next = ignored.length;
		for (DiagonalConnections.Diagonal diagonal : DiagonalConnections.Diagonal.ALL) {
			more[next++] = diagonal.property;
		}
		return more;
	}
}
