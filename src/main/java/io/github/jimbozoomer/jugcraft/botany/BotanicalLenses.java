package io.github.jimbozoomer.jugcraft.botany;

import com.mojang.serialization.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Original lens compatibility masks; composites retain both real lenses and their individual dyes. */
public final class BotanicalLenses {
    public static final Map<String,Integer> PROPS=Map.ofEntries(Map.entry("piston",12),Map.entry("warp",0),Map.entry("mine",12),Map.entry("weight",12),Map.entry("paint",12),Map.entry("fire",28),Map.entry("light",12),Map.entry("explosive",28),Map.entry("storm",0),Map.entry("normal",0),Map.entry("speed",0),Map.entry("power",1),Map.entry("time",0),Map.entry("efficiency",0),Map.entry("gravity",2),Map.entry("messenger",1),Map.entry("bounce",4),Map.entry("damage",16),Map.entry("phantom",4),Map.entry("magnet",2),Map.entry("influence",0),Map.entry("redirect",12),Map.entry("firework",4),Map.entry("flare",32),Map.entry("tripwire",32));
    public static DataComponentType<ItemStack> SECOND_STACK;public static DataComponentType<String> SECOND;public static DataComponentType<Integer> COLOR,SECOND_COLOR;
    private final BotanicalWorkshop host;private UUID flying;private int absent;private boolean powered;private long cooldown;
    BotanicalLenses(BotanicalWorkshop host){this.host=host;}
    static void register(){
        SECOND=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_second_lens"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        SECOND_STACK=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_second_lens_stack"),DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());
        COLOR=integer("botanical_lens_color");SECOND_COLOR=integer("botanical_second_lens_color");
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("botanical_lens_crafting"),Crafting.SERIALIZER);BotanicalBurst.register();
    }
    private static DataComponentType<Integer> integer(String id){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(id),DataComponentType.<Integer>builder().persistent(Codec.intRange(-1,16)).networkSynchronized(ByteBufCodecs.VAR_INT).build());}
    public static boolean handles(String name){return name.startsWith("lens_")&&PROPS.containsKey(name.substring(5));}
    public static String name(ItemStack stack){return stack.getItem() instanceof Lens lens?lens.kind:"";}
    static List<String> kinds(ItemStack stack){String first=name(stack),second=stack.getOrDefault(SECOND,"");if(first.isEmpty())return List.of();return PROPS.containsKey(second)&&compatible(first,second)?List.of(first,second):List.of(first);}
    static boolean compatible(String a,String b){return !a.equals(b)&&!a.equals("normal")&&!b.equals("normal")&&(PROPS.getOrDefault(a,63)&PROPS.getOrDefault(b,63))==0&&(PROPS.getOrDefault(a,63)&32)==0&&(PROPS.getOrDefault(b,63)&32)==0;}
    public static int color(ItemStack lens,long ticks){int color=lens.getOrDefault(SECOND_COLOR,-1);if(color<0)color=lens.getOrDefault(COLOR,-1);return color==16?net.minecraft.util.Mth.hsvToRgb((ticks%180)/180F,1,1):color<0?0x20FF20:DyeColor.byId(color).getTextureDiffuseColor();}
    static Item create(String name,Item.Properties properties){return new Lens(name.substring(5),properties.stacksTo(16));}
    private static final class Lens extends Item {
        final String kind;Lens(String kind,Properties p){super(p);this.kind=kind;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> text,TooltipFlag flag){var second=stack.getOrDefault(SECOND,"");if(!second.isEmpty())text.accept(net.minecraft.network.chat.Component.literal("Composite: "+second));text.accept(net.minecraft.network.chat.Component.literal("Use on a mana spreader. Sneak with an empty hand to remove."));}
    }
    InteractionResult use(ServerPlayer p,InteractionHand hand){
        var held=p.getItemInHand(hand);
        if(held.getItem() instanceof Lens){if(!host.getItem(0).isEmpty())return InteractionResult.FAIL;host.setItem(0,held.copyWithCount(1));if(!p.hasInfiniteMaterials())held.shrink(1);return InteractionResult.SUCCESS;}
        if(held.isEmpty()&&p.isShiftKeyDown()){var old=host.removeItem(0,1);if(!old.isEmpty())p.setItemInHand(hand,old);return InteractionResult.SUCCESS;}return InteractionResult.PASS;
    }
    void tick(ServerLevel level,ServerPlayer actor){
        boolean now=level.hasNeighborSignal(host.getBlockPos()),edge=now&&!powered;if(now!=powered){powered=now;host.setChanged();}
        if(flying!=null){if(level.getEntity(flying) instanceof BotanicalBurst){absent=0;return;}if(++absent<100)return;flying=null;absent=0;host.setChanged();}
        if(cooldown>level.getGameTime()+100)cooldown=0;if(level.getGameTime()<cooldown)return;
        var lens=host.getItem(0);var names=kinds(lens);boolean pulse=host.kind().equals("redstone_spreader");
        if(pulse?!edge:now)return;
        if(names.contains("storm")&&!actor.hasInfiniteMaterials())return;
        var start=Vec3.atCenterOf(host.getBlockPos());var direction=host.aim();
        if(names.contains("flare")){var at=start.add(direction.scale(.7));level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color(lens,level.getGameTime()),.8F),at.x,at.y,at.z,1,.02,.02,.02,.01);cooldown=level.getGameTime()+2;return;}
        if(names.contains("tripwire")&&!BotanicalBurst.tripped(host,actor,lens))return;
        var props=BotanicalBurst.properties(host.kind(),names);int amount=Math.min(host.mana(),props.capacity());if(amount<=0)return;
        // Debit before spawning; only the immediate failed spawn refunds. Missing saved entities never mint mana.
        if(!host.draw(amount))return;
        var burst=BotanicalBurst.launch(host,actor,lens,amount,props);
        if(burst==null){host.fill(amount);return;}flying=burst.getUUID();cooldown=level.getGameTime()+4;host.setChanged();
    }
    void finished(UUID id){if(id.equals(flying)){flying=null;absent=0;cooldown=host.getLevel().getGameTime()+4;host.setChanged();}}
    void save(ValueOutput out){if(flying!=null)out.store("burst_flying",UUIDUtil.CODEC,flying);out.putBoolean("burst_powered",powered);out.putLong("burst_cooldown",cooldown);}
    void load(ValueInput in){flying=in.read("burst_flying",UUIDUtil.CODEC).orElse(null);powered=in.getBooleanOr("burst_powered",false);cooldown=in.getLongOr("burst_cooldown",0);absent=0;}
    public static final class Crafting extends CustomRecipe {
        private static final Crafting INSTANCE=new Crafting();public static final RecipeSerializer<Crafting> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(INSTANCE),StreamCodec.<RegistryFriendlyByteBuf,Crafting>unit(INSTANCE));
        private record Match(ItemStack first,ItemStack second,int slot,int dye,boolean split){}
        private Match find(CraftingInput in){ItemStack a=ItemStack.EMPTY,b=ItemStack.EMPTY;int slot=-1,dye=-1;boolean glue=false;
            for(int i=0;i<in.size();i++){var s=in.getItem(i);if(s.isEmpty())continue;if(s.getItem() instanceof Lens){if(a.isEmpty()){a=s;slot=i;}else if(b.isEmpty())b=s;else return null;}
                else if(s.getItem() instanceof DyeItem d&&dye<0)dye=d.getDefaultInstance().getOrDefault(net.minecraft.core.component.DataComponents.DYE,net.minecraft.world.item.DyeColor.WHITE).getId();else if(s.is(Jugtania.item("mana_pearl"))&&dye<0)dye=16;
                else if(s.is(Items.SLIME_BALL)&&!glue)glue=true;else return null;}
            if(a.isEmpty())return null;
            if(b.isEmpty()&&!glue){if(dye>=0)return new Match(a,b,slot,dye,false);if(kinds(a).size()==2)return new Match(a,b,slot,-1,true);return null;}
            return !b.isEmpty()&&glue&&dye<0&&kinds(a).size()==1&&kinds(b).size()==1&&compatible(name(a),name(b))?new Match(a,b,slot,-1,false):null;
        }
        @Override public boolean matches(CraftingInput in,Level level){return find(in)!=null;}
        @Override public ItemStack assemble(CraftingInput in){var m=find(in);if(m==null)return ItemStack.EMPTY;
            if(m.split){var saved=m.first.get(SECOND_STACK);var out=saved!=null&&name(saved).equals(m.first.get(SECOND))?saved.copyWithCount(1):new ItemStack(Jugtania.item("lens_"+m.first.get(SECOND)));out.set(COLOR,m.first.getOrDefault(SECOND_COLOR,-1));return out;}
            var out=m.first.copyWithCount(1);if(m.dye>=0)out.set(COLOR,m.dye);else{out.set(SECOND_STACK,m.second.copyWithCount(1));out.set(SECOND,name(m.second));out.set(SECOND_COLOR,m.second.getOrDefault(COLOR,-1));}return out;
        }
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput in){var out=NonNullList.withSize(in.size(),ItemStack.EMPTY);var m=find(in);if(m!=null&&m.split){var lens=m.first.copyWithCount(1);lens.remove(SECOND_STACK);lens.remove(SECOND);lens.remove(SECOND_COLOR);out.set(m.slot,lens);}return out;}
        @Override public RecipeSerializer<Crafting> getSerializer(){return SERIALIZER;}
    }
}
