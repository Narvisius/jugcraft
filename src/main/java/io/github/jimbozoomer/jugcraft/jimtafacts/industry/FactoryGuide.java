package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Source guide/note art, native portable instructions with persistent topic bookmarks. */
public final class FactoryGuide {
    private FactoryGuide(){}
    public static Item GUIDE,NOTE;
    public record Chapter(String title,String text){}
    public static final List<Chapter> CHAPTERS=List.of(
        new Chapter("Getting started","Machines use Jugcraft Energy (JE). Connect a generator or battery with energy cables. Recipes and input/output previews show real ingredients. Keep outputs clear. Redstone pauses many workshops; check the machine screen before changing its wiring."),
        new Chapter("Surveying","Volume Boxes save named-by-reference regions up to 32 blocks on each axis. Click two corners, switching corner with use in air. Use on a paused civil workshop to bind. The bound volume cannot be edited until disconnected. Sneak-use with a Volume Marker in the other hand to disconnect near the paused machine."),
        new Chapter("Moving corners","Hold the selected Volume Box in one hand and Marker Connector in the other. Use the connector while aiming near a corner. Aim to move its preview, use again to save, or sneak-use to cancel. Access is checked again at confirmation. Nothing in the world is moved."),
        new Chapter("Filling and clearing","The Filler Planner chooses Fill, Box, Frame, Floor, Walls, Sphere, Pyramid, Stairs or Clear. Sneak-use toggles inversion. Apply it to a paused filler. Filling consumes real blocks. Clear needs a tool, spends JE and keeps real drops. Containers are not destroyed by clearing."),
        new Chapter("Mining pipes","The basic miner drills a straight column directly below its body. Supply mining pipes and an appropriate tool. Each extension spends a pipe and JE; mined items need output room. Pause and choose Retract to recover intact rods. Removed rods are never refunded twice. Claims can stop either operation."),
        new Chapter("Pumps and tanks","Place the factory pump above its intake and supply JE. It scans only loaded, permitted blocks. Water can be renewable; lava and other liquids are finite. Glass tanks hold 16 buckets each. Redstone can pause machinery. Fluid pipes and filters must match the fluid you want to move."),
        new Chapter("Transport and filters","A Factory List records up to nine item samples without consuming them. Hold a sample in the other hand and use to add or remove it; sneak-use clears the list. Use it in supported filter slots. A survey map records locations, areas or a bounded route for carrier robots."),
        new Chapter("Diagnostics","Workshop Goggles show nearby ports. The Diagnostic Probe reports the clicked face and authorized machine state. The JE Load Tester actually consumes incoming energy: connect it only while measuring. Redstone pauses its input. Sneak-use the tester to reset its total. Simulation does not count as delivered power."),
        new Chapter("Safety and ownership","Workshops recheck claims and ownership while working. They do not load remote chunks to bypass limits. Pause machinery before changing its survey. Keep the full input and output route inside authorized land. Guides explain controls; owning usable equipment never requires learning a guide first.")
    );
    public static void register(){
        GUIDE=put("factory_guide",false);NOTE=put("factory_guide_note",true);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o->{o.accept(GUIDE);o.accept(NOTE);});
    }
    private static Item put(String name,boolean note){var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+name));return Registry.register(BuiltInRegistries.ITEM,key,new Reader(new Item.Properties().setId(key).stacksTo(1),note));}
    public static int topic(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("FactoryTopic",0),0,CHAPTERS.size()-1);}
    public static List<Filterable<Component>> pages(ItemStack stack,boolean note){
        var pages=new ArrayList<Filterable<Component>>();int first=topic(stack);
        for(int i=0;i<(note?1:CHAPTERS.size());i++){
            var chapter=CHAPTERS.get((first+i)%CHAPTERS.size());String text=chapter.text();
            do {int cut=Math.min(170,text.length());if(cut<text.length()){int space=text.lastIndexOf(' ',cut);if(space>0)cut=space;}pages.add(Filterable.passThrough(Component.literal(chapter.title()+"\n\n"+text.substring(0,cut))));text=text.substring(cut).stripLeading();}while(!text.isEmpty());
        }return List.copyOf(pages);
    }
    private static final class Reader extends WrittenBookItem {
        final boolean note;Reader(Properties p,boolean note){super(p);this.note=note;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player.isShiftKeyDown()){
                if(player instanceof ServerPlayer){var stack=player.getItemInHand(hand);int next=(topic(stack)+1)%CHAPTERS.size();CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putInt("FactoryTopic",next));player.sendOverlayMessage(Component.literal("Bookmark: "+CHAPTERS.get(next).title()));}
                return InteractionResult.SUCCESS;
            }
            if(player instanceof ServerPlayer){var stack=player.getItemInHand(hand);var other=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(!note&&other.is(NOTE)){int selected=topic(other);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putInt("FactoryTopic",selected));}
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(note?"Factory Note":"Factory Field Guide"),"Jugcraft",0,pages(stack,note),true));
            }return super.use(level,player,hand);
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,java.util.function.Consumer<Component> out,TooltipFlag f){out.accept(Component.literal("Bookmark: "+CHAPTERS.get(topic(s)).title()));out.accept(Component.literal("Use to read. Sneak-use selects the next topic."));if(!note)out.accept(Component.literal("A Factory Note in the other hand selects its bookmarked topic."));}
    }
}
