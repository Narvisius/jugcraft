package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Generated source data from supplied AE matter_cannon recipes and AEColor, not executable source code. */
public final class DigitalMatterAmmo {
    private DigitalMatterAmmo() {}
    private record Entry(String tag, float weight, String metal) {}
    private static final Entry[] WEIGHTS = {
        new Entry("c:nuggets/aluminum", 26.9815f, "aluminum"),
        new Entry("c:nuggets/antimony", 121.76f, "antimony"),
        new Entry("c:nuggets/barium", 137.327f, "barium"),
        new Entry("c:nuggets/beef", 32.0f, "beef"),
        new Entry("c:nuggets/beryllium", 9.0122f, "beryllium"),
        new Entry("c:nuggets/bismuth", 208.9804f, "bismuth"),
        new Entry("c:nuggets/boron", 10.811f, "boron"),
        new Entry("c:nuggets/bromine", 79.904f, "bromine"),
        new Entry("c:nuggets/cadmium", 112.411f, "cadmium"),
        new Entry("c:nuggets/calcium", 40.078f, "calcium"),
        new Entry("c:nuggets/carbon", 12.0107f, "carbon"),
        new Entry("c:nuggets/cerium", 140.116f, "cerium"),
        new Entry("c:nuggets/cesium", 132.9055f, "cesium"),
        new Entry("c:nuggets/charcoal", 12.0107f, "charcoal"),
        new Entry("c:nuggets/chicken", 32.0f, "chicken"),
        new Entry("c:nuggets/coal", 12.0107f, "coal"),
        new Entry("c:nuggets/cobalt", 58.9332f, "cobalt"),
        new Entry("c:nuggets/copper", 63.546f, "copper"),
        new Entry("c:nuggets/electrum", 152.41737f, "electrum"),
        new Entry("c:nuggets/fish", 32.0f, "fish"),
        new Entry("c:nuggets/gallium", 69.723f, "gallium"),
        new Entry("c:nuggets/germanium", 72.64f, "germanium"),
        new Entry("c:nuggets/gold", 196.96655f, "gold"),
        new Entry("c:nuggets/indium", 114.818f, "indium"),
        new Entry("c:nuggets/invar", 56.794468f, "invar"),
        new Entry("c:nuggets/iodine", 126.9045f, "iodine"),
        new Entry("c:nuggets/iridium", 192.217f, "iridium"),
        new Entry("c:nuggets/iron", 55.845f, "iron"),
        new Entry("c:nuggets/krypton", 83.8f, "krypton"),
        new Entry("c:nuggets/lanthanum", 138.9055f, "lanthanum"),
        new Entry("c:nuggets/lead", 207.2f, "lead"),
        new Entry("c:nuggets/lithium", 6.941f, "lithium"),
        new Entry("c:nuggets/magnesium", 24.305f, "magnesium"),
        new Entry("c:nuggets/manganese", 54.938f, "manganese"),
        new Entry("c:nuggets/meat", 32.0f, "meat"),
        new Entry("c:nuggets/meatcooked", 32.0f, "meatcooked"),
        new Entry("c:nuggets/meatraw", 32.0f, "meatraw"),
        new Entry("c:nuggets/nickel", 58.6934f, "nickel"),
        new Entry("c:nuggets/niobium", 92.9064f, "niobium"),
        new Entry("c:nuggets/osmium", 190.23f, "osmium"),
        new Entry("c:nuggets/palladium", 106.42f, "palladium"),
        new Entry("c:nuggets/phosphorus", 30.9738f, "phosphorus"),
        new Entry("c:nuggets/platinum", 195.078f, "platinum"),
        new Entry("c:nuggets/plutonium", 244.0f, "plutonium"),
        new Entry("c:nuggets/potassium", 39.0983f, "potassium"),
        new Entry("c:nuggets/rhodium", 102.9055f, "rhodium"),
        new Entry("c:nuggets/rubidium", 85.4678f, "rubidium"),
        new Entry("c:nuggets/ruthenium", 101.07f, "ruthenium"),
        new Entry("c:nuggets/scandium", 44.9559f, "scandium"),
        new Entry("c:nuggets/sheep", 32.0f, "sheep"),
        new Entry("c:nuggets/silicon", 28.0855f, "silicon"),
        new Entry("c:nuggets/silver", 107.8682f, "silver"),
        new Entry("c:nuggets/sodium", 22.9897f, "sodium"),
        new Entry("c:nuggets/strontium", 87.62f, "strontium"),
        new Entry("c:nuggets/sulfur", 32.065f, "sulfur"),
        new Entry("c:nuggets/tantalum", 180.9479f, "tantalum"),
        new Entry("c:nuggets/technetium", 98.0f, "technetium"),
        new Entry("c:nuggets/tellurium", 127.6f, "tellurium"),
        new Entry("c:nuggets/tin", 118.71f, "tin"),
        new Entry("c:nuggets/titanium", 47.867f, "titanium"),
        new Entry("c:nuggets/tungsten", 183.84f, "tungsten"),
        new Entry("c:nuggets/uranium", 238.0289f, "uranium"),
        new Entry("c:nuggets/vanadium", 50.9415f, "vanadium"),
        new Entry("c:nuggets/xenon", 131.293f, "xenon"),
        new Entry("c:nuggets/yttrium", 88.9059f, "yttrium"),
        new Entry("c:nuggets/zinc", 65.39f, "zinc"),
        new Entry("c:nuggets/zirconium", 91.224f, "zirconium")
    };
    public static float weight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (Entry e : WEIGHTS) if (stack.is(TagKey.create(Registries.ITEM, Identifier.parse(e.tag))) || path.equals(e.metal + "_nugget")) return e.weight;
        return 0;
    }
    public static java.util.List<Integer> colors(DyeColor color) {
        int[] palette = switch(color) {
            case WHITE -> new int[]{11842740, 14737632, 16382457};
            case LIGHT_GRAY -> new int[]{8289918, 10526624, 12895428};
            case GRAY -> new int[]{5197647, 7105388, 9736852};
            case BLACK -> new int[]{1250067, 2565927, 3881787};
            case LIME -> new int[]{5161038, 7397977, 11794541};
            case YELLOW -> new int[]{16764736, 16769881, 16056192};
            case ORANGE -> new int[]{14252079, 15508028, 15907401};
            case BROWN -> new int[]{7227922, 8281110, 9334298};
            case RED -> new int[]{11149611, 14106178, 15758949};
            case PINK -> new int[]{14184106, 16751035, 16501461};
            case MAGENTA -> new int[]{12669321, 13988252, 15113919};
            case PURPLE -> new int[]{7232696, 9526733, 11562973};
            case BLUE -> new int[]{3375088, 3708159, 4243967};
            case LIGHT_BLUE -> new int[]{6928895, 7394047, 8452095};
            case CYAN -> new int[]{2273454, 3132599, 6678729};
            case GREEN -> new int[]{498539, 1554541, 3332176};
        };
        return java.util.List.of(palette[0], palette[1], palette[2]);
    }
}
