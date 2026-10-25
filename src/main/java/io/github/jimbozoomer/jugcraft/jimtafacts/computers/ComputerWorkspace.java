package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A small virtual program shelf and integer registers, stored inside the computer's block data. */
public final class ComputerWorkspace {
    private record File(String name,String text){
        static final Codec<File> CODEC=RecordCodecBuilder.create(i->i.group(Codec.string(1,32).fieldOf("name").forGetter(File::name),Codec.string(0,8192).fieldOf("text").forGetter(File::text)).apply(i,File::new));
    }
    private record Register(String name,int value){
        static final Codec<Register> CODEC=RecordCodecBuilder.create(i->i.group(Codec.string(1,16).fieldOf("name").forGetter(Register::name),Codec.intRange(-1_000_000_000,1_000_000_000).fieldOf("value").forGetter(Register::value)).apply(i,Register::new));
    }
    private final Map<String,String> files=new TreeMap<>();
    private final Map<String,Integer> values=new TreeMap<>();
    private static String fileName(String name){if(!name.matches("[a-zA-Z0-9_-]{1,32}"))throw new IllegalArgumentException("Program names: 1..32 letters, numbers, underscore or dash.");return name;}
    private static String registerName(String name){if(!name.matches("[a-zA-Z][a-zA-Z0-9_]{0,15}"))throw new IllegalArgumentException("Register names: letter first, up to 16 letters/numbers/underscores.");return name;}
    public String list(){return files.isEmpty()?"No saved programs.":String.join(", ",files.keySet());}
    public void save(String name,String text){
        name=fileName(name);if(text.length()>8192||text.split("\\R",-1).length>128)throw new IllegalArgumentException("Program: 128 lines / 8192 characters.");
        if(!files.containsKey(name)&&files.size()>=16)throw new IllegalArgumentException("Program shelf is full (16 programs).");
        int size=files.values().stream().mapToInt(String::length).sum()-files.getOrDefault(name,"").length()+text.length();
        if(size>65536)throw new IllegalArgumentException("Computer program storage limit: 65536 characters.");files.put(name,text);
    }
    public String load(String name){String text=files.get(fileName(name));if(text==null)throw new IllegalArgumentException("No saved program named "+name);return text;}
    public void delete(String name){files.remove(fileName(name));}
    public void set(String name,long value){name=registerName(name);if(!values.containsKey(name)&&values.size()>=32)throw new IllegalArgumentException("At most 32 integer registers.");values.put(name,(int)Math.clamp(value,-1_000_000_000,1_000_000_000));}
    public void setPair(String first,int a,String second,int b){
        first=registerName(first);second=registerName(second);var names=new HashSet<>(values.keySet());names.add(first);names.add(second);
        if(names.size()>32)throw new IllegalArgumentException("Leave two register slots for touch_x/touch_y.");
        set(first,a);set(second,b);
    }
    public int get(String name){name=registerName(name);if(!values.containsKey(name))throw new IllegalArgumentException("Unset register: "+name);return values.get(name);}
    public int value(String word){if(word.startsWith("$"))return get(word.substring(1));try{return Math.clamp(Integer.parseInt(word),-1_000_000_000,1_000_000_000);}catch(NumberFormatException e){throw new IllegalArgumentException("Use an integer or $register.");}}
    public String text(String input){
        var result=new StringBuilder();for(String word:input.split(" ",-1)){if(result.length()>0)result.append(' ');result.append(word.startsWith("$")?get(word.substring(1)):word);if(result.length()>4096)throw new IllegalArgumentException("Expanded text exceeds 4096 characters.");}return result.toString();
    }
    public void save(ValueOutput out){
        out.store("workspace_files",File.CODEC.listOf(0,16),files.entrySet().stream().map(e->new File(e.getKey(),e.getValue())).toList());
        out.store("workspace_values",Register.CODEC.listOf(0,32),values.entrySet().stream().map(e->new Register(e.getKey(),e.getValue())).toList());
    }
    public void load(ValueInput in){
        files.clear();values.clear();
        for(var file:in.read("workspace_files",File.CODEC.listOf(0,16)).orElse(List.of()))try{save(file.name,file.text);}catch(IllegalArgumentException ignored){}
        for(var value:in.read("workspace_values",Register.CODEC.listOf(0,32)).orElse(List.of()))try{set(value.name,value.value);}catch(IllegalArgumentException ignored){}
    }
}
