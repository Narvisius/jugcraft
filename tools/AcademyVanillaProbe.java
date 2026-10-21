import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

/** Optional validation against an installed vanilla 26.3 game, not a Jugcraft GameTest.
 * Run with Java 25 and the launcher's vanilla 26.3 classpath; see the feature record.
 * It creates no world and deliberately does not validate the four Jugcraft states.
 */
public class AcademyVanillaProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Pass the generated winterhold.nbt path");
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var tag = NbtIo.readCompressed(Path.of(args[0]), NbtAccounter.unlimitedHeap());
        var palette = tag.getListOrEmpty("palette");
        int checked = 0, skipped = 0;
        for (int i = 0; i < palette.size(); i++) {
            var entry = palette.getCompoundOrEmpty(i);
            String name = entry.getStringOr("id", "");
            if (name.startsWith("jugcraft:")) {
                skipped++;
                continue;
            }
            if (!name.startsWith("minecraft:")) throw new AssertionError("Unexpected namespace: " + name);
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
            if (block == null || (block == Blocks.AIR && !name.equals("minecraft:air"))) {
                throw new AssertionError("Unknown block: " + name);
            }
            var props = entry.getCompoundOrEmpty("properties");
            for (String key : props.keySet()) {
                var property = block.getStateDefinition().getProperty(key);
                if (property == null || property.getValue(props.getStringOr(key, "")).isEmpty()) {
                    throw new AssertionError("Invalid state: " + name + " " + key);
                }
            }
            checked++;
        }
        System.out.println("PASS: " + checked + " vanilla palette states validated; " + skipped
            + " Jugcraft states require the mod runtime (not checked). NBT DataVersion=" + tag.getIntOr("DataVersion", -1));
    }
}
