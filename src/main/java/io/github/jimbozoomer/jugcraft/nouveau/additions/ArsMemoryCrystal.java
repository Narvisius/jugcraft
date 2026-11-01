package io.github.jimbozoomer.jugcraft.nouveau.additions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity.Route;
import io.github.jimbozoomer.jugcraft.nouveau.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Ten typed configuration memories. Inventories, owners, energy and live job progress are never copied. */
public final class ArsMemoryCrystal extends Item {
    public static ArsMemoryCrystal ITEM;
    public static DataComponentType<List<Memory>> MEMORIES;
    public static DataComponentType<Integer> SELECTED;
    public record Memory(String kind,ItemStack filter,int threshold,boolean inverted,Optional<Inscription> spell,Optional<Route> route,boolean locked){
        static final Memory EMPTY=new Memory("",ItemStack.EMPTY,1,false,Optional.empty(),Optional.empty(),false);
        static final Codec<Memory> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("kind").forGetter(Memory::kind),ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(Memory::filter),
            Codec.intRange(1,4096).fieldOf("threshold").forGetter(Memory::threshold),Codec.BOOL.fieldOf("inverted").forGetter(Memory::inverted),
            Inscription.CODEC.optionalFieldOf("spell").forGetter(Memory::spell),Route.CODEC.optionalFieldOf("route").forGetter(Memory::route),
            Codec.BOOL.fieldOf("locked").forGetter(Memory::locked)).apply(i,Memory::new));
        Memory lock(){return new Memory(kind,filter,threshold,inverted,spell,route,!locked);}
    }
    private ArsMemoryCrystal(Properties p){super(p);}
    public static void register(){
        var codec=Memory.CODEC.listOf(0,10);
        MEMORIES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_memories"),DataComponentType.<List<Memory>>builder().persistent(codec).networkSynchronized(ByteBufCodecs.fromCodec(codec)).build());
        SELECTED=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("ars_memory_selected"),DataComponentType.<Integer>builder().persistent(Codec.intRange(0,9)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("ars_additions_memory_crystal"));
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,new ArsMemoryCrystal(new Properties().setId(key).stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->e.accept(ITEM));
    }
    private static List<Memory> memories(ItemStack crystal){var result=new ArrayList<>(crystal.getOrDefault(MEMORIES,List.of()));while(result.size()<10)result.add(Memory.EMPTY);return result;}
    private static Memory selected(ItemStack crystal){return memories(crystal).get(crystal.getOrDefault(SELECTED,0));}
    private static void set(ItemStack crystal,Memory value){var list=memories(crystal);list.set(crystal.getOrDefault(SELECTED,0),value);crystal.set(MEMORIES,List.copyOf(list));}
    private static String family(String kind){return kind.endsWith("spell_turret")?"turret":Set.of("rune","item_detector","spell_sensor").contains(kind)?kind:"";}
    private static ItemStack ghost(ItemStack original){
        if(original.isEmpty())return ItemStack.EMPTY;var result=new ItemStack(original.getItem());
        if(original.has(NouveauUtilities.FILTER))result.set(NouveauUtilities.FILTER,original.get(NouveauUtilities.FILTER));
        if(original.has(JugcraftConcordance.INSCRIPTION))result.set(JugcraftConcordance.INSCRIPTION,original.get(JugcraftConcordance.INSCRIPTION));
        return result;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||!(p.level().getBlockEntity(context.getClickedPos()) instanceof NouveauWorkshop station)||!station.mayUse(p))return InteractionResult.FAIL;
        String family=family(station.kind());if(family.isEmpty())return InteractionResult.PASS;
        var crystal=context.getItemInHand();var memory=selected(crystal);
        if(p.isShiftKeyDown()){
            if(memory.locked())return message(p,"That memory is locked.",false);
            set(crystal,new Memory(family,ghost(station.filter()),station.threshold(),station.inverted(),Optional.ofNullable(station.configuredSpell()),Optional.empty(),false));
            return message(p,"Configuration saved. No items or energy were copied.",true);
        }
        if(!family.equals(memory.kind()))return message(p,"Select a matching memory first.",false);
        station.filter(memory.filter());station.threshold(memory.threshold());if(station.inverted()!=memory.inverted())station.invert();
        if(family.equals("turret")||family.equals("rune"))station.rememberSpell(memory.spell().orElse(null));
        return message(p,"Configuration applied.",true);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(!(entity instanceof Starbuncle star))return InteractionResult.PASS;
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!JugNouveau.enabled()||!star.mayServe(p)||!Authority.mayChange(p.level(),p,star.blockPosition()))return InteractionResult.FAIL;
        var memory=selected(stack);
        if(p.isShiftKeyDown()){
            if(memory.locked()||star.route()==null)return message(p,"Unlock this memory and select a courier with a route.",false);
            set(stack,new Memory("starbuncle",ItemStack.EMPTY,1,false,Optional.empty(),Optional.of(star.route()),false));return message(p,"Courier route recorded.",true);
        }
        return memory.kind().equals("starbuncle")&&memory.route().isPresent()&&star.assign(p,memory.route().get())?message(p,"Courier route applied.",true):message(p,"Route unavailable, unauthorized or courier carrying cargo.",false);
    }
    private static InteractionResult message(ServerPlayer p,String text,boolean success){p.sendOverlayMessage(Component.literal(text));return success?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p&&JugNouveau.enabled()&&!p.isSpectator())p.openMenu(new SimpleMenuProvider((id,inv,who)->new Menu(id,inv,hand),Component.literal("Memory Crystal · Choose a configuration")));
        return InteractionResult.SUCCESS;
    }
    private static final class Menu extends ChestMenu {
        private final ItemStack crystal;private final InteractionHand hand;private final UUID owner;
        Menu(int id,Inventory inventory,InteractionHand hand){super(MenuType.GENERIC_9x3,id,inventory,new SimpleContainer(27),3);this.hand=hand;crystal=inventory.player.getItemInHand(hand);owner=inventory.player.getUUID();refresh();}
        private void icon(int slot,Item item,String name,String hint){var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(hint))));getContainer().setItem(slot,stack);}
        private void refresh(){
            getContainer().clearContent();var list=memories(crystal);int selected=crystal.getOrDefault(SELECTED,0);
            for(int i=0;i<10;i++){var m=list.get(i);icon(i,m.kind().isEmpty()?Items.GLASS_BOTTLE:Items.AMETHYST_SHARD,(i==selected?"> ":"")+(i+1)+" · "+(m.kind().isEmpty()?"Empty":m.kind())+(m.locked()?" [locked]":""),"Click to select; sneak-use on a device to save; use to apply.");}
            icon(20,Items.TRIPWIRE_HOOK,"Toggle selected memory lock","Locked memories cannot be overwritten or cleared.");
            icon(24,Items.BARRIER,"Clear selected memory","Only the selected, unlocked configuration is removed.");broadcastChanges();
        }
        @Override public boolean stillValid(Player p){return p.getUUID().equals(owner)&&p.isAlive()&&!p.isSpectator()&&p.getItemInHand(hand)==crystal&&crystal.is(ITEM)&&JugNouveau.enabled();}
        @Override public void clicked(int slot,int button,ContainerInput type,Player player){
            if(!(player instanceof ServerPlayer)||!stillValid(player)||type!=ContainerInput.PICKUP)return;
            if(slot>=0&&slot<10)crystal.set(SELECTED,slot);
            else if(slot==20)set(crystal,selected(crystal).lock());else if(slot==24&&!selected(crystal).locked())set(crystal,Memory.EMPTY);
            refresh();
        }
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    }
}
