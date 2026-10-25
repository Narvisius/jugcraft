package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.furniture.FurnitureDesign;
import java.nio.file.*;
import java.util.*;

/** Local bounded library. No downloads, scripts, arbitrary paths or server-side item grants. */
public final class ArtisanLibrary {
    private ArtisanLibrary(){}
    public static Path file(){return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("jugcraft/furniture/library.json");}
    public static List<FurnitureDesign> load() throws java.io.IOException {
        Path file=file();if(!Files.exists(file))return new ArrayList<>();if(Files.size(file)>800000)throw new java.io.IOException("Furniture library exceeds its size limit");
        try{var a=JsonParser.parseString(Files.readString(file)).getAsJsonArray();if(a.size()>64)throw new IllegalArgumentException();var result=new ArrayList<FurnitureDesign>();for(var v:a)result.add(FurnitureDesign.parse(v.getAsString()));return result;}catch(RuntimeException e){throw new java.io.IOException("Furniture library is invalid; the original file has been preserved",e);}
    }
    public static void save(List<FurnitureDesign> designs) throws java.io.IOException {
        if(designs.size()>64)throw new java.io.IOException("Library is full (64 designs)");var a=new JsonArray();designs.forEach(d->a.add(d.json()));Path file=file();Files.createDirectories(file.getParent());Path temporary=file.resolveSibling("library.json.tmp");Files.writeString(temporary,a.toString());try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
    }
}
