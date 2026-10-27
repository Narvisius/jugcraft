package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/** Local guides: original book art, native reader, server-derived personal discovery record. */
public final class JimtaGuides {
    private JimtaGuides(){}
    public static void register(){
        for(String name:List.of("chronicle","workshop_guide")){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(JimtaSpellbooks.PREFIX+name));
            var item=Registry.register(BuiltInRegistries.ITEM,key,new Reader(new Item.Properties().setId(key).stacksTo(1),name.equals("chronicle")));
            JimtaSpellbooks.ITEMS.put(name,item);
        }
    }
    private static List<String> chapters(){return JimtaSpellbooks.data("guides").getAsJsonArray("chapters").asList().stream().map(e->e.getAsJsonObject().get("title").getAsString()+"\n\n"+e.getAsJsonObject().get("text").getAsString()).toList();}
    public static List<String> discoveries(ServerPlayer player){
        var result=new ArrayList<String>();result.add("Your Workshop Chronicle\n\nA personal record of supplies and books you have acquired. Discoveries are reminders, not permission requirements. You can always use equipment you own.");
        StringBuilder page=new StringBuilder("Discoveries\n\n");int count=0;
        for(var entry:JimtaSpellbooks.data("guides").getAsJsonArray("milestones")){
            var row=entry.getAsJsonObject();var item=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(row.get("item").getAsString()));
            boolean found=player.getInventory().contains(stack->stack.is(item))||player.getStats().getValue(Stats.ITEM_PICKED_UP,item)>0||player.getStats().getValue(Stats.ITEM_CRAFTED,item)>0||player.getStats().getValue(Stats.ITEM_USED,item)>0;
            page.append(found?"✓ ":"○ ").append(row.get("title").getAsString()).append('\n').append(row.get("hint").getAsString()).append("\n\n");
            if(++count%3==0){result.add(page.toString());page=new StringBuilder("Discoveries continued\n\n");}
        }
        if(count%3!=0)result.add(page.toString());return result;
    }
    public static void write(ServerPlayer player,EncyclopediaRequestPayload request,PageWriter out){
        out.heading(Component.literal("Jimtafacts workshop guides"));
        out.fact(Component.literal("Craft the Workshop Guide for a portable reader, or the Chronicle for your personal discovery record. Recipes and shared Focus rules in Jugcraft take precedence over legacy guide names."));
        for(var entry:JimtaSpellbooks.data("guides").getAsJsonArray("chapters")){
            var row=entry.getAsJsonObject();out.heading(Component.literal(row.get("title").getAsString()));out.fact(Component.literal(row.get("text").getAsString()));
        }
    }
    private static final class Reader extends WrittenBookItem {
        final boolean chronicle;
        Reader(Properties p,boolean chronicle){super(p);this.chronicle=chronicle;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server){
                var pages=new ArrayList<Filterable<Component>>();
                for(String chapter:chronicle?discoveries(server):chapters()){
                    // Short authored pages; split paragraphs without truncation at the book's limits.
                    String remaining=chapter;while(remaining.length()>200){int cut=remaining.lastIndexOf(' ',200);if(cut<1)cut=200;pages.add(Filterable.passThrough(Component.literal(remaining.substring(0,cut))));remaining=remaining.substring(cut).stripLeading();}
                    pages.add(Filterable.passThrough(Component.literal(remaining)));
                }
                player.getItemInHand(hand).set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(chronicle?"Workshop Chronicle":"Workshop Guide"),"Jugcraft",0,pages,true));
            }
            return super.use(level,player,hand);
        }
    }
}
