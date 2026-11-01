package io.github.jimbozoomer.jugcraft.aquatic;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.*;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

/** Native fishing tools and physical, component-backed tackle. No research prerequisite. */
public final class OwnerAquaticGear {
    private OwnerAquaticGear(){}
    public static DataComponentType<ItemContainerContents> TACKLE;
    public static MenuType<OwnerTackleMenu> MENU;
    public static final ToolMaterial NEPTUNIUM=new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,1796,8.5f,3.5f,14,TagKey.create(Registries.ITEM,Jugcraft.id("aquatic_repairs_neptunium")));
    public static void register(){
        TACKLE=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("aquatic_tackle"),DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("aquatic_tackle"),new MenuType<>(OwnerTackleMenu::new,FeatureFlags.DEFAULT_FLAGS));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("aquatic_fillet"),OwnerFilletRecipe.SERIALIZER);
        String[] names={"wooden","stone","iron","gold","diamond","neptunium"};ToolMaterial[] mats={ToolMaterial.WOOD,ToolMaterial.STONE,ToolMaterial.IRON,ToolMaterial.GOLD,ToolMaterial.DIAMOND,NEPTUNIUM};
        for(int i=0;i<names.length;i++){var mat=mats[i];boolean eternal=i==5;OwnerAquaculture.item(names[i]+"_fillet_knife",p->new Knife(p,mat,eternal));}
        String[] rods={"iron","gold","diamond","neptunium"};int[] life={125,55,450,1000};ToolMaterial[] tiers={ToolMaterial.IRON,ToolMaterial.GOLD,ToolMaterial.DIAMOND,NEPTUNIUM};
        for(int i=0;i<rods.length;i++){var tier=tiers[i];int durability=life[i];String name=rods[i];OwnerAquaculture.item(name+"_fishing_rod",p->new Rod(p.durability(durability).enchantable(tier.enchantmentValue()).repairable(tier.repairItems()),name));}
        for(String hook:new String[]{"iron","gold","diamond","light","heavy","double","redstone","note","nether_star"})OwnerAquaculture.item(hook+"_hook",p->new Hook(p,hook));
        OwnerAquaculture.item("worm",p->new Bait(p,20));OwnerAquaculture.item("leech",p->new Bait(p,35));
        for(String name:new String[]{"fishing_line","bobber","neptunium_nugget","neptunium_ingot","driftwood","tin_can","goldfish"})OwnerAquaculture.item(name,Item::new);
    }
    public static ItemStack part(ItemStack rod,int index){var contents=rod.getOrDefault(TACKLE,ItemContainerContents.EMPTY);return index<contents.size()?contents.itemCopies().skip(index).findFirst().orElse(ItemStack.EMPTY):ItemStack.EMPTY;}
    public static String hook(ItemStack rod){return part(rod,0).getItem() instanceof Hook h?h.name:"";}
    public static ItemStack held(Player p){return p.getMainHandItem().getItem() instanceof Rod?p.getMainHandItem():p.getOffhandItem().getItem() instanceof Rod?p.getOffhandItem():ItemStack.EMPTY;}
    public static void payBait(ServerPlayer p,ItemStack rod){
        if(p.isCreative())return;var bait=part(rod,1).copy();if(bait.isEmpty())return;
        bait.hurtAndBreak(1,p.level(),p,item->{});
        var contents=net.minecraft.core.NonNullList.withSize(4,ItemStack.EMPTY);for(int i=0;i<4;i++)contents.set(i,i==1?bait:part(rod,i).copy());rod.set(TACKLE,ItemContainerContents.fromItems(contents));p.getInventory().setChanged();
    }
    public static final class Knife extends Item {
        public final boolean eternal;
        Knife(Properties p,ToolMaterial material,boolean eternal){super(properties(p,material,eternal));this.eternal=eternal;}
        private static Properties properties(Properties p,ToolMaterial m,boolean eternal){p.sword(m,m.attackDamageBonus()/2,-2.2f).durability((int)(m.durability()*.75f));if(eternal)p.component(DataComponents.UNBREAKABLE,Unit.INSTANCE);return p;}
    }
    public static final class Bait extends Item {public Bait(Properties p,int uses){super(p.durability(uses));}}
    public static final class Hook extends Item {public final String name;Hook(Properties p,String name){super(p);this.name=name;}}
    public static final class Rod extends FishingRodItem {
        public final String material;
        Rod(Properties p,String material){super(p.component(TACKLE,ItemContainerContents.EMPTY));this.material=material;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player.isShiftKeyDown()&&player.fishing==null){
                if(player instanceof ServerPlayer p){var rod=p.getItemInHand(hand);int slot=hand==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():Inventory.SLOT_OFFHAND;p.openMenu(new SimpleMenuProvider((id,inv,who)->new OwnerTackleMenu(id,inv,rod,slot),Component.literal("Fishing tackle")));}
                return InteractionResult.SUCCESS;
            }
            return super.use(level,player,hand);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){out.accept(Component.literal("Sneak-use to fit hook, bait, line and bobber."));var hook=part(stack,0);if(!hook.isEmpty())out.accept(hook.getHoverName());}
    }
}
