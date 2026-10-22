package io.github.jimbozoomer.jugcraft.update;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** Strict data-only protocol shared by the menu updater and offline tests. */
public record UpdateManifest(String version, String minecraft, String loader, int javaVersion, Map<String, Entry> files) {
    public static final String RELEASES = "https://github.com/jimbozoomer-byte/jugcraft/releases/download/";
    public static final String CHANNEL = RELEASES + "player-channel/stable.json";
    public record Entry(String path, String hash, long size, String url) {}
    public static String string(JsonObject object, String field) throws IOException {
        var v=object.get(field);
        UpdateApplier.require(v!=null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString(), "Missing text: "+field);
        return v.getAsString();
    }
    public static String version(String text) throws IOException {
        UpdateApplier.require(text.matches("[A-Za-z0-9][A-Za-z0-9._+\\-]{0,79}"),"Invalid release version"); return text;
    }
    public static String digest(String text) throws IOException {
        UpdateApplier.require(text.matches("[0-9a-f]{128}"),"Invalid SHA-512"); return text;
    }
    public static JsonObject object(byte[] bytes) throws IOException {
        UpdateApplier.require(bytes.length<=1048576,"Oversized update metadata");
        try {return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();}
        catch(RuntimeException e) {throw new IOException("Invalid update metadata",e);}
    }
    public static UpdateManifest parse(byte[] bytes) throws IOException {
        try {
            var o=object(bytes);
            UpdateApplier.require(o.get("schemaVersion").getAsInt()==1 && o.get("updaterVersion").getAsInt()==1,"This release needs a newer installer");
            String version=version(string(o,"releaseVersion"));
            Map<String,Entry> files=new LinkedHashMap<>(); Set<String> names=new HashSet<>(); long total=0;
            var rows=o.getAsJsonArray("files");
            UpdateApplier.require(rows.size()>0&&rows.size()<=200,"Invalid managed inventory");
            for(var row:rows) {
                var r=row.getAsJsonObject(); String name=string(r,"path");
                UpdateApplier.validateName(name);
                UpdateApplier.require(names.add(name.toLowerCase(Locale.ROOT)),"Duplicate managed file");
                String hash=digest(string(r,"sha512")),url=string(r,"url"); long size=r.get("size").getAsLong();
                UpdateApplier.require(size>0&&size<=1073741824L,"Invalid download size"); total+=size;
                UpdateApplier.require("managed".equals(string(r,"policy")),"Unsupported file policy");
                approvedUrl(url,version); files.put(name,new Entry(name,hash,size,url));
            }
            UpdateApplier.require(total<=3L*1073741824 && files.containsKey("mods/jugcraft.jar"),"Invalid pack inventory");
            return new UpdateManifest(version,string(o,"minecraft"),string(o,"fabricLoader"),o.get("java").getAsInt(),Collections.unmodifiableMap(files));
        } catch(RuntimeException e) {throw new IOException("Malformed release inventory",e);}
    }
    public static void approvedUrl(String value,String version) throws IOException {
        URI uri;
        try {uri=URI.create(value);} catch(IllegalArgumentException e) {throw new IOException("Invalid download URL",e);}
        UpdateApplier.require("https".equals(uri.getScheme())&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.getQuery()==null&&uri.getPort()==-1,"Unsafe download URL");
        boolean own=value.equals(RELEASES+"pack-"+version+"/jugcraft-client.jar");
        boolean library="cdn.modrinth.com".equals(uri.getHost())&&uri.getPath().startsWith("/data/");
        boolean gui=value.equals("https://raw.githubusercontent.com/SkyblockOverhaul/maven/15091644f73b6e85eee84bd2526fbbe29b54fe96/net/sbo/guilib-26.3-fabric/0.12.4/guilib-26.3-fabric-0.12.4.jar");
        UpdateApplier.require(own||library||gui,"Unapproved update source");
    }
    public void compatible(UpdateManifest next) throws IOException {
        UpdateApplier.require(minecraft.equals(next.minecraft)&&loader.equals(next.loader)&&javaVersion==next.javaVersion,"Minecraft/loader changes require a new pack import");
    }
    /** Numeric segments compare naturally, preventing .10 sorting before .9 and silent downgrades. */
    public static int compare(String a,String b) {
        var pattern=java.util.regex.Pattern.compile("[0-9]+|[^0-9]+");
        var x=pattern.matcher(a); var y=pattern.matcher(b);
        while(true) {
            boolean ax=x.find(),by=y.find(); if(!ax||!by) return Boolean.compare(ax,by);
            String s=x.group(),t=y.group();
            int c=Character.isDigit(s.charAt(0))&&Character.isDigit(t.charAt(0)) ? new java.math.BigInteger(s).compareTo(new java.math.BigInteger(t)) : s.compareTo(t);
            if(c!=0)return c;
        }
    }
}
