package local.peepo;

import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.*;

/** One authoritative costume slot. Bonuses add no world searches or extra AI goals. */
public final class CompanionUniforms extends SnapshotParticipant<Long> {
    public enum Type {
        NONE("",0), FARMER("farmer_uniform",15), FISHERMAN("fisherman_uniform",20),
        CHEF("chef_uniform",10), MECHANIC("mechanic_uniform",25), PORTER("porter_uniform",25),
        BAKER("baker_uniform",20), RANCH_HAND("ranch_hand_uniform",15), BEEKEEPER("beekeeper_uniform",20),
        RUNNER("runner_uniform",15), ALCHEMIST("alchemist_uniform",20),
        ORCHARD_KEEPER("orchard_keeper_uniform",15), GROUNDSKEEPER("groundskeeper_uniform",20);
        public final String path;public final int saving;
        Type(String path,int saving){this.path=path;this.saving=saving;}
        public String key(){return "item.peepo_companion."+path;}
    }
    private static final Set<String> KITCHEN=Set.of("cooking_pot","skillet","kitchen_stove","cutting_board","candy_kettle","canning_kettle");
    private static final Map<Type,Item> ITEMS=new EnumMap<>(Type.class);
    private static final net.minecraft.resources.Identifier PORTER_SPEED=PeepoMod.id("porter_uniform_speed");
    private final PeepoEntity npc;
    private long bonusSequence;
    CompanionUniforms(PeepoEntity npc){this.npc=npc;}
    public static Type type(ItemStack stack){return stack.getItem() instanceof UniformItem item?item.type:Type.NONE;}
    public static boolean costume(ItemStack stack){return stack.is(Items.JACK_O_LANTERN) || type(stack)!=Type.NONE;}
    public static Type fromId(int id){return Type.values()[Math.clamp(id,0,Type.values().length-1)];}
    public boolean wearing(Type type){return type!=Type.NONE && npc.uniform()==type;}
    public int saving(Type job){return wearing(job)?job.saving:0;}
    public int duration(Type job,int ticks){return wearing(job)?Math.max(1,(int)Math.ceil(ticks/(job==Type.FARMER?1.15:1.20))):ticks;}
    public static boolean kitchen(CompanionAssignments.Target target){
        if(target==null)return false;
        return target.block().getNamespace().equals("jugcraft") && KITCHEN.contains(target.block().getPath());
    }
    /** Classify an already selected route, without reading blocks or scanning the world. */
    public static Type deliveryJob(CompanionAssignments.Target target){
        if(target==null)return Type.NONE;
        if(target.orchard() || target.treeTap())return Type.ORCHARD_KEEPER;
        if(target.water())return Type.GROUNDSKEEPER;
        var id=target.block();
        if(id.getNamespace().equals("minecraft") && id.getPath().equals("brewing_stand"))return Type.ALCHEMIST;
        if(!id.getNamespace().equals("jugcraft"))return Type.NONE;
        return switch(id.getPath()){
            case "hearth_oven" -> Type.BAKER;
            case "crucible" -> Type.ALCHEMIST;
            case "cider_press","cider_barrel" -> Type.ORCHARD_KEEPER;
            case "sprinkler" -> Type.GROUNDSKEEPER;
            default -> kitchen(target)?Type.CHEF:Type.NONE;
        };
    }
    public static boolean coversCuffs(Type type){
        return switch(type){case FARMER,MECHANIC,PORTER,RANCH_HAND,BEEKEEPER,RUNNER,GROUNDSKEEPER -> true;default -> false;};
    }
    /** Deterministic next roll is saved and advances only with a committed production/delivery transaction. */
    public boolean bonus(Type type,TransactionContext tx){
        if(!wearing(type))return false;
        updateSnapshots(tx);
        long z=++bonusSequence+npc.getUUID().getLeastSignificantBits();
        z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;z^=z>>>31;
        return Math.floorMod(z,100)<15;
    }
    /** Duplicate the actual recipe results, retaining their components and stack limits. */
    public List<ItemStack> chefResults(List<ItemStack> results,TransactionContext tx){
        if(results.stream().noneMatch(s->!s.isEmpty()) || !bonus(Type.CHEF,tx))return results;
        var doubled=new ArrayList<ItemStack>(results.size()*2);
        for(var result:results)if(!result.isEmpty()){doubled.add(result.copy());doubled.add(result.copy());}
        return doubled;
    }
    @Override protected Long createSnapshot(){return bonusSequence;}
    @Override protected void readSnapshot(Long value){bonusSequence=value;}
    void save(ValueOutput out){out.putLong("UniformBonusSequence",bonusSequence);out.putInt("UniformEnergyFraction",npc.energyStore.discountFraction());}
    void load(ValueInput in){bonusSequence=in.getLongOr("UniformBonusSequence",0);npc.energyStore.discountFraction(in.getIntOr("UniformEnergyFraction",99));}
    void tick(){
        var speed=npc.getAttribute(Attributes.MOVEMENT_SPEED);if(speed==null)return;
        double bonus=npc.transport!=null && npc.transport.carryingDelivery() && !npc.editingSettings() && !npc.isEating()
            && npc.getRestMode()==CompanionEnergy.Rest.NONE?npc.transport.deliverySpeedBonus():0;
        var current=speed.getModifier(PORTER_SPEED);
        if(current!=null && current.amount()!=bonus){speed.removeModifier(PORTER_SPEED);current=null;}
        if(bonus>0 && current==null)speed.addTransientModifier(new AttributeModifier(PORTER_SPEED,bonus,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private static final class UniformItem extends Item {
        final Type type;
        UniformItem(Properties p,Type type){super(p);this.type=type;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> tooltip,TooltipFlag flag){
            tooltip.accept(Component.translatable("tooltip.peepo_companion.uniform"));
            tooltip.accept(Component.translatable(type.key()+".bonus"));
        }
    }
    public static void initialize(){
        for(var type:Type.values())if(type!=Type.NONE){var id=PeepoMod.id(type.path);ITEMS.put(type,Registry.register(BuiltInRegistries.ITEM,id,new UniformItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1),type)));}
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
    }
}
