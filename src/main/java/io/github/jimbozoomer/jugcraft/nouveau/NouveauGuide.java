package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
public final class NouveauGuide {
    private static com.google.gson.JsonArray topics;
    private NouveauGuide(){}
    public static void write(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        if(!JugNouveau.enabled())return;if(topics==null)topics=NouveauResources.list("guide");
        for(var value:topics){var t=value.getAsJsonObject();out.heading(Component.literal(t.get("title").getAsString()));
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse("jugcraft:"+t.get("icon").getAsString()));
            out.fact(Component.literal(t.get("summary").getAsString()),null,item==null?List.of():List.of(item));out.fact(Component.literal(t.get("detail").getAsString()));out.codex("nouveau/"+t.get("id").getAsString(),null,null);
        }
    }
}
