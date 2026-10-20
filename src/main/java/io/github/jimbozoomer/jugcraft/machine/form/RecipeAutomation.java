package io.github.jimbozoomer.jugcraft.machine.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** A local recipe preference. Zero restores the default; targets count output buffers, not remote inventories. */
public record RecipeAutomation(Identifier recipe, int priority, int target) {
    public static final int MAX_RULES = 8;
    public static final Codec<RecipeAutomation> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("recipe").forGetter(RecipeAutomation::recipe),
        Codec.intRange(0, 3).fieldOf("priority").forGetter(RecipeAutomation::priority),
        Codec.intRange(0, 2000).fieldOf("target").forGetter(RecipeAutomation::target)
    ).apply(i, RecipeAutomation::new));
    public static final Codec<java.util.List<RecipeAutomation>> LIST_CODEC = CODEC.listOf(0, MAX_RULES);
}
