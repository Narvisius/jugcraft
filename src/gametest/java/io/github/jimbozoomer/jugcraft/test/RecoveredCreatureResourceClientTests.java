package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

/** Checks the final resource-pack lookup, including supplied audio and vanilla delegates. */
public final class RecoveredCreatureResourceClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        c.runOnClient(mc->{
            var resources=mc.getResourceManager();int sounds=0;
            try(var reader=resources.getResourceOrThrow(Jugcraft.id("sounds.json")).openAsReader()){
                var definitions=JsonParser.parseReader(reader).getAsJsonObject();
                for(var entry:definitions.entrySet())if(entry.getKey().startsWith("rotten.")){
                    for(var sound:entry.getValue().getAsJsonObject().getAsJsonArray("sounds")){
                        String name=sound.isJsonPrimitive()?sound.getAsString():sound.getAsJsonObject().get("name").getAsString();
                        var id=Identifier.parse(name);
                        try(var audio=resources.getResourceOrThrow(Identifier.fromNamespaceAndPath(id.getNamespace(),"sounds/"+id.getPath()+".ogg")).open()){
                            if(!java.util.Arrays.equals(audio.readNBytes(4),new byte[]{'O','g','g','S'}))throw new AssertionError("Invalid supplied Ogg: "+name);
                        }
                        sounds++;
                    }
                }
                if(sounds!=48)throw new AssertionError("Expected all 48 supplied sound files, found "+sounds);
                for(String egg:java.util.List.of("zombie","skeleton","endermite","wolf"))
                    resources.getResourceOrThrow(Identifier.withDefaultNamespace("models/item/"+egg+"_spawn_egg.json"));
                resources.getResourceOrThrow(Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png"));
                System.out.println("Recovered resources: 48 Ogg files, four current egg models and the vanilla creeper atlas resolved.");
            }catch(java.io.IOException e){throw new IllegalStateException("Recovered resource missing",e);}
        });
    }
}
