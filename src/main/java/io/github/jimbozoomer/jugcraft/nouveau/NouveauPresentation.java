package io.github.jimbozoomer.jugcraft.nouveau;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Clip names and UV texture bindings are extracted from the owner files during resource generation. */
public final class NouveauPresentation {
    public record Model(String texture,String animation,String idle,String active){}
    private static final Map<String,Model> MODELS=new HashMap<>();
    private NouveauPresentation(){}
    public static void load(){
        try(var in=NouveauPresentation.class.getResourceAsStream("/jugcraft/nouveau/presentation.json")){
            if(in==null)throw new IllegalStateException("Missing Nouveau presentation bindings");
            JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().entrySet().forEach(e->{
                var v=e.getValue().getAsJsonObject();MODELS.put(e.getKey(),new Model(v.get("texture").getAsString(),v.get("animation").getAsString(),v.get("idle").getAsString(),v.get("active").getAsString()));
            });
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static String texture(String geometry){var m=MODELS.get(geometry);return m==null?geometry:m.texture();}
    public static String animation(String geometry,boolean active){var m=MODELS.get(geometry);return m==null?"":active?m.active():m.idle();}
}
