package io.github.jimbozoomer.jugcraft.aircraft;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** The owner's aircraft dimensions, equipment layout and tuning remain data, shared by both sides. */
public record AircraftSpec(String name, JsonObject definition, List<Socket> sockets, int width, int inventoryY) {
    public record Socket(String type, int x, int y) {}
    public static final Map<String, AircraftSpec> ALL = new LinkedHashMap<>();
    public static final Map<String, JsonObject> UPGRADES = new LinkedHashMap<>();
    public static void load() {
        if (!ALL.isEmpty()) return;
        try (var in = AircraftSpec.class.getResourceAsStream("/jugcraft/aircraft.json")) {
            var root = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.getAsJsonObject("upgrades").entrySet()) UPGRADES.put(e.getKey(), e.getValue().getAsJsonObject());
            for (var e : root.getAsJsonObject("craft").entrySet()) {
                var d = e.getValue().getAsJsonObject(); var list = new ArrayList<Socket>();
                int min = 0, max = 176, bottom = 90;
                for (var v : d.getAsJsonArray("inventorySlots")) {
                    var s = v.getAsJsonObject(); int x=s.get("x").getAsInt(), y=s.get("y").getAsInt();
                    int cols=s.has("cols")?s.get("cols").getAsInt():1, rows=s.has("rows")?s.get("rows").getAsInt():1;
                    for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) list.add(new Socket(s.get("type").getAsString(), x+c*18,y+r*18+14));
                    min=Math.min(min,x-8); max=Math.max(max,x+cols*18+8); bottom=Math.max(bottom,y+rows*18+14);
                }
                final int offset=-min;
                var adjusted=list.stream().map(s->new Socket(s.type(),s.x()+offset,s.y())).toList();
                ALL.put(e.getKey(), new AircraftSpec(e.getKey(),d,adjusted,max-min,bottom+28));
            }
        } catch(IOException ex) { throw new IllegalStateException("Aircraft specifications",ex); }
    }
    public double value(String key,double fallback) { var p=definition.getAsJsonObject("properties");return p.has(key)?p.get(key).getAsDouble():fallback; }
    public int seats() { return definition.getAsJsonArray("passengerPositions").size(); }
    public Vec3 seat(int index,int count) {
        var variants=definition.getAsJsonArray("passengerPositions");
        var seats=variants.get(Math.clamp(count-1,0,variants.size()-1)).getAsJsonArray();
        var p=seats.get(Math.clamp(index,0,seats.size()-1)).getAsJsonObject();
        return new Vec3(p.get("x").getAsDouble(),p.get("y").getAsDouble(),p.get("z").getAsDouble());
    }
    /** Source meshes use a cockpit pivot; lift their lowest authored point onto the ground. */
    public double groundOffset(){return switch(name){case "airship","cargo_airship"->.6875;case "biplane"->.5;case "gyrodyne"->.25;case "warship"->.78125;default->0;};}
    public boolean aeroplane() { return name.equals("biplane")||name.equals("bamboo_hopper"); }
    public int height() { return inventoryY+96; }
}
