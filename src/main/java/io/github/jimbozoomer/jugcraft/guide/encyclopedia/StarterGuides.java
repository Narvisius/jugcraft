package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Shared, read-only lessons used by the Encyclopedia and town mentors. No rewards or research gates. */
public final class StarterGuides {
    public static final List<String> IDS = List.of("technology_start", "magic_start", "alchemy_start", "mentors");
    private static JsonObject lessons;
    private StarterGuides() {}
    private static JsonObject data() {
        if (lessons == null) try (var in = StarterGuides.class.getResourceAsStream("/jugcraft/starter_guidance.json")) {
            if (in == null) throw new IllegalStateException("Missing starter guidance");
            lessons = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        return lessons;
    }
    public static void write(String id, ServerPlayer player, PageWriter out) {
        for (var value : data().getAsJsonArray(id)) {
            var topic = value.getAsJsonObject();
            out.heading(Component.literal(topic.get("title").getAsString()));
            for (var text : topic.getAsJsonArray("text")) out.fact(Component.literal(text.getAsString()));
            if (topic.has("items")) for (var entry : topic.getAsJsonArray("items")) {
                var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(entry.getAsString()));
                int count = player.getInventory().countItem(item);
                out.fact(Component.translatable("encyclopedia.jugcraft.have_now", new net.minecraft.world.item.ItemStack(item).getHoverName(), count), null, List.of(item));
            }
            if (topic.has("recipes")) for (var entry : topic.getAsJsonArray("recipes")) {
                var recipe = Identifier.parse(entry.getAsString());
                out.recipe(EncyclopediaPayload.EdgeKind.REQUIRED, recipe,
                        Component.translatable("encyclopedia.jugcraft.server_recipe", recipe.getPath().replace('_', ' ')));
            }
            if (topic.has("links")) for (var entry : topic.getAsJsonArray("links")) {
                var link = entry.getAsJsonObject();
                out.link(Component.literal(link.get("title").getAsString()), new EncyclopediaPayload.Link(
                        link.has("route") ? EncyclopediaPayload.Link.ROUTE : EncyclopediaPayload.Link.PAGE,
                        link.get(link.has("route") ? "route" : "page").getAsString()));
            }
        }
    }
}
