package io.github.jimbozoomer.jugcraft.furniture;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;

/** Immutable, bounded designs: data only, with server-computed material cost and collision. */
public record FurnitureDesign(String name, List<Part> parts, int seat, boolean storage, int light, int effect, int sound) {
    public static final int MAX_TEXT=12000, MAX_PARTS=32;
    public static final String[] MATERIALS={"Oak", "Spruce", "Birch", "Iron", "White wool", "Glass", "Stone", "Purple wool"};
    public record Part(int x,int y,int z,int w,int h,int d,int material,boolean sprite) {
        public Part {
            if(x<0||y<0||z<0||w<1||h<1||d<1||x+w>16||y+h>16||z+d>16||material<0||material>=MATERIALS.length)
                throw new IllegalArgumentException("Parts must fit within the 16-pixel workspace");
            if(sprite&&d!=1)throw new IllegalArgumentException("Sprites must be one pixel deep");
        }
    }
    public FurnitureDesign {
        if(name==null||name.isBlank()||name.length()>40||name.chars().anyMatch(c->Character.isISOControl(c)||c==167))throw new IllegalArgumentException("Use a name of 1–40 ordinary characters");
        parts=List.copyOf(parts);
        if(parts.isEmpty()||parts.size()>MAX_PARTS||seat<0||seat>16||light<0||light>15||effect<0||effect>3||sound<0||sound>3)
            throw new IllegalArgumentException("Invalid furniture design limits");
    }
    public int cost(){return (int)Math.ceil(Math.sqrt(parts.stream().mapToInt(p->p.w*p.h*p.d).sum())/32D+parts.size()/4D+(storage?13.5:0)+light/15D+(seat>0?1:0)+(effect>0?1:0)+(sound>0?1:0));}
    public String json(){
        var o=new JsonObject();o.addProperty("schema",1);o.addProperty("name",name);o.addProperty("seat",seat);o.addProperty("storage",storage);o.addProperty("light",light);o.addProperty("effect",effect);o.addProperty("sound",sound);
        var a=new JsonArray();for(var p:parts){var v=new JsonArray();for(int n:new int[]{p.x,p.y,p.z,p.w,p.h,p.d,p.material,p.sprite?1:0})v.add(n);a.add(v);}o.add("parts",a);return o.toString();
    }
    private static int integer(JsonElement e){double v=e.getAsDouble();if(!Double.isFinite(v)||v!=Math.rint(v)||Math.abs(v)>100000)throw new IllegalArgumentException("Invalid integer");return (int)v;}
    public static FurnitureDesign parse(String text){
        if(text==null||text.length()>MAX_TEXT)throw new IllegalArgumentException("Design is too large");
        // Reject excessive nesting before Gson parses attacker-controlled input.
        int depth=0;boolean quoted=false,escape=false;for(char c:text.toCharArray()){if(quoted){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='\"')quoted=false;}else if(c=='\"')quoted=true;else if(c=='{'||c=='['){if(++depth>5)throw new IllegalArgumentException("Invalid nesting");}else if(c=='}'||c==']')depth--;}
        var o=JsonParser.parseString(text).getAsJsonObject();if(integer(o.get("schema"))!=1)throw new IllegalArgumentException("Unsupported design version");
        var a=o.getAsJsonArray("parts");if(a.size()>MAX_PARTS)throw new IllegalArgumentException("Too many parts");var parts=new ArrayList<Part>();
        for(var e:a){var v=e.getAsJsonArray();if(v.size()!=8)throw new IllegalArgumentException("Invalid part");int sprite=integer(v.get(7));if(sprite<0||sprite>1)throw new IllegalArgumentException("Invalid sprite");parts.add(new Part(integer(v.get(0)),integer(v.get(1)),integer(v.get(2)),integer(v.get(3)),integer(v.get(4)),integer(v.get(5)),integer(v.get(6)),sprite==1));}
        return new FurnitureDesign(o.get("name").getAsString(),parts,integer(o.get("seat")),o.get("storage").getAsBoolean(),integer(o.get("light")),integer(o.get("effect")),integer(o.get("sound")));
    }
    public static FurnitureDesign safe(String text){try{return parse(text);}catch(RuntimeException e){return stool();}}
    public VoxelShape shape(Direction facing){
        VoxelShape shape=Shapes.empty();for(var p:parts){if(p.sprite)continue;int x=p.x,z=p.z,w=p.w,d=p.d;
            for(int i=0;i<turns(facing);i++){int old=x;x=16-z-d;z=old;int t=w;w=d;d=t;}
            shape=Shapes.or(shape,Block.box(x,p.y,z,x+w,p.y+p.h,z+d));}return shape.isEmpty()?Block.box(1,0,7,15,16,9):shape;
    }
    public static int turns(Direction f){return switch(f){case EAST->1;case SOUTH->2;case WEST->3;default->0;};}
    public static FurnitureDesign stool(){return new FurnitureDesign("Oak stool",List.of(new Part(1,8,1,14,2,14,0,false),new Part(2,0,2,2,8,2,0,false),new Part(12,0,2,2,8,2,0,false),new Part(2,0,12,2,8,2,0,false),new Part(12,0,12,2,8,2,0,false)),10,false,0,0,0);}
}
