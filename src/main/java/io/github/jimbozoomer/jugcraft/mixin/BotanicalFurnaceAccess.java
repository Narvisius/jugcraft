package io.github.jimbozoomer.jugcraft.mixin;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractFurnaceBlockEntity.class)
public interface BotanicalFurnaceAccess {
    @Accessor("litTimeRemaining") int jugcraft$heat();
    @Accessor("litTimeRemaining") void jugcraft$heat(int value);
    @Accessor("litTotalTime") void jugcraft$heatTotal(int value);
    @Accessor("cookingTimer") int jugcraft$cook();
    @Accessor("cookingTimer") void jugcraft$cook(int value);
    @Accessor("cookingTotalTime") int jugcraft$cookTotal();
}
