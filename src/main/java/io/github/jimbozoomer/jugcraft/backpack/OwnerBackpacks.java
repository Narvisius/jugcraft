package io.github.jimbozoomer.jugcraft.backpack;

import java.util.*;
import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Physical packs: inventory and upgrade state travel with the actual item, never a shared global ID. */
public final class OwnerBackpacks {
    private OwnerBackpacks(){}
    public static final List<String> TIERS=List.of("backpack","copper_backpack","iron_backpack","gold_backpack","diamond_backpack","netherite_backpack");
    public static final int[] CAPACITY={27,45,54,81,108,120},UPGRADE_SLOTS={1,1,2,3,5,7};
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final Map<String,OwnerBackpackBlock> BLOCKS=new LinkedHashMap<>();
    public static final Set<String> UPGRADE_NAMES=new LinkedHashSet<>();
    public static DataComponentType<ItemContainerContents> UPGRADES,FILTERS;
    public static DataComponentType<List<OwnerBackpackStacks.Entry>> CONTENTS;
    public static DataComponentType<List<CustomData>> CAPTURES;
    public static BlockEntityType<OwnerBackpackEntity> ENTITY;
    public static MenuType<OwnerBackpackMenu> MENU;
    public static JsonObject source(String resource){
        try(var stream=OwnerBackpacks.class.getResourceAsStream("/jugcraft/"+resource);var reader=new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
        catch(java.io.IOException e){throw new IllegalStateException("Missing backpack descriptor "+resource,e);}
    }
    public static void register(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(OwnerBackpackPreview.TYPE,OwnerBackpackPreview.CODEC);
        UPGRADES=component("backpack_upgrades");FILTERS=component("backpack_filters");
        CONTENTS=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("backpack_contents"),DataComponentType.<List<OwnerBackpackStacks.Entry>>builder().persistent(OwnerBackpackStacks.Entry.CODEC.listOf(0,120)).networkSynchronized(OwnerBackpackStacks.Entry.STREAM.apply(net.minecraft.network.codec.ByteBufCodecs.list(120))).build());
        CAPTURES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("backpack_creatures"),DataComponentType.<List<CustomData>>builder().persistent(CustomData.CODEC.listOf(0,16)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(CustomData.CODEC).apply(net.minecraft.network.codec.ByteBufCodecs.list(16))).build());
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            var pack=player.getItemInHand(hand);if(tier(pack)<0||!player.isShiftKeyDown()||!(entity instanceof LivingEntity living)||!has(pack,"mob_catcher_upgrade")&&!has(pack,"advanced_mob_catcher_upgrade"))return InteractionResult.PASS;
            return !(player instanceof ServerPlayer who)||OwnerBackpackMobs.capture(who,pack,living)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        });
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("owner_backpack"),new MenuType<>(OwnerBackpackMenu::new,FeatureFlags.DEFAULT_FLAGS));
        var descriptor=source("newest_backpacks.json");descriptor.getAsJsonArray("upgrades").forEach(e->UPGRADE_NAMES.add(e.getAsString()));
        for(var element:descriptor.getAsJsonArray("items")){
            String name=element.getAsString();int tier=TIERS.indexOf(name);
            var itemKey=ResourceKey.create(Registries.ITEM,Jugcraft.id("packworks_"+name));
            Item item;
            if(tier>=0){
                var blockKey=ResourceKey.create(Registries.BLOCK,itemKey.identifier());
                var block=new OwnerBackpackBlock(BlockBehaviour.Properties.of().setId(blockKey).strength(1.2F).noOcclusion().sound(SoundType.WOOL),tier);
                Registry.register(BuiltInRegistries.BLOCK,blockKey,block);BLOCKS.put(name,block);
                var properties=new Item.Properties().setId(itemKey).stacksTo(1).component(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
                if(tier==5)properties.fireResistant();
                item=new PackItem(block,properties,tier);
            }else item=new UpgradeItem(new Item.Properties().setId(itemKey).stacksTo(1),name);
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,itemKey,item));
        }
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("owner_backpack"),FabricBlockEntityTypeBuilder.create(OwnerBackpackEntity::new,BLOCKS.values().toArray(Block[]::new)).build());
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,Jugcraft.id("packworks_tier_upgrade"),OwnerBackpackRecipe.SERIALIZER);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->ITEMS.values().forEach(e::accept));
        OwnerBackpackUpgrades.register();
        OwnerBackpackFluids.register();
        OwnerBackpackAutomation.register();
        OwnerBackpackOpen.register();
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->!(entity instanceof OwnerBackpackEntity bag)||OwnerBackpackInfinity.mayEdit(player,bag.carrier()));
    }
    private static DataComponentType<ItemContainerContents> component(String name){return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id(name),DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());}
    public static eu.pb4.trinkets.api.TrinketSlotAccess worn(Player player){return io.github.jimbozoomer.jugcraft.concordance.trinket.Wayfaring.worn(player,s->tier(s)>=0);}
    public static List<ItemStack> carried(Player player){
        var result=new ArrayList<ItemStack>();var worn=worn(player);if(worn!=null)result.add(worn.get());
        for(int i=0;i<player.getInventory().getContainerSize()&&result.size()<3;i++){var item=player.getInventory().getItem(i);if(tier(item)>=0)result.add(item);}return result;
    }
    public static boolean heldBy(Player player,ItemStack pack,int slot){var worn=worn(player);return slot==-2?worn!=null&&worn.get()==pack:slot>=0&&player.getInventory().getItem(slot)==pack;}
    public static void changed(Player player,ItemStack pack){var worn=worn(player);if(worn!=null&&worn.get()==pack)worn.set(pack);player.getInventory().setChanged();}
    public static int tier(ItemStack stack){return stack.getItem() instanceof PackItem pack?pack.tier:-1;}
    public static boolean has(ItemStack pack,String name){return pack.getOrDefault(UPGRADES,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().anyMatch(s->s.is(ITEMS.get(name)));}
    public static boolean allows(ItemStack pack,ItemStack stack){
        if(stack.isEmpty()||stack==pack)return false;
        if(stack.getItem() instanceof PackItem)return has(pack,"inception_upgrade")&&!containsPack(stack);
        return stack.getItem().canFitInsideContainerItems();
    }
    private static boolean containsPack(ItemStack pack){
        var entries=pack.get(CONTENTS);
        return entries!=null?entries.stream().anyMatch(e->e.sample().getItem() instanceof PackItem):pack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().anyMatch(s->s.getItem() instanceof PackItem);
    }
    public static boolean filter(ItemStack pack,ItemStack stack){
        var filters=pack.getOrDefault(FILTERS,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().filter(s->!s.isEmpty()).toList();
        return filters.isEmpty()||filters.stream().anyMatch(s->s.is(stack.getItem()));
    }
    public static final class PackItem extends BlockItem implements eu.pb4.trinkets.api.callback.TrinketCallback {
        public final int tier;
        PackItem(Block block,Properties props,int tier){super(block,props);this.tier=tier;}
        @Override public boolean canFitInsideContainerItems(){return false;}
        @Override public boolean canEquip(ItemStack stack,eu.pb4.trinkets.api.TrinketSlotAccess slot,LivingEntity entity){return entity instanceof Player;}
        @Override public boolean canUnequip(ItemStack stack,eu.pb4.trinkets.api.TrinketSlotAccess slot,LivingEntity entity){return !(entity instanceof ServerPlayer player)||OwnerBackpackUpgrades.accessible(player,stack);}
        @Override public eu.pb4.trinkets.api.TrinketDropRule getDropRule(ItemStack stack,eu.pb4.trinkets.api.TrinketSlotAccess slot,LivingEntity wearer){return slot.cosmetic()?eu.pb4.trinkets.api.TrinketDropRule.KEEP:eu.pb4.trinkets.api.callback.TrinketCallback.super.getDropRule(stack,slot,wearer);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p&&!p.isSpectator()){
                var pack=p.getItemInHand(hand);int locked=hand==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():Inventory.SLOT_OFFHAND;
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new OwnerBackpackMenu(id,inv,pack,locked,null),pack.getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult useOn(UseOnContext c){
            if(c.getPlayer()==null)return InteractionResult.FAIL;
            if(has(c.getItemInHand(),"pump_upgrade")||has(c.getItemInHand(),"advanced_pump_upgrade")){
                var result=OwnerBackpackFluids.useOn(c);if(result!=InteractionResult.PASS)return result;
            }
            if(!c.getPlayer().isShiftKeyDown())return use(c.getLevel(),c.getPlayer(),c.getHand());
            if(!OwnerBackpackMobs.entries(c.getItemInHand()).isEmpty()){
                if(c.getPlayer() instanceof ServerPlayer who)return OwnerBackpackMobs.release(who,c.getItemInHand(),c.getClickedPos().relative(c.getClickedFace()),-1,who.getInventory()::setChanged)?InteractionResult.SUCCESS:InteractionResult.FAIL;
                return InteractionResult.SUCCESS;
            }
            if(c.getPlayer() instanceof ServerPlayer player){
                if(!Authority.mayChange(player.level(),player,c.getClickedPos()))return InteractionResult.FAIL;
                if(OwnerBackpackUpgrades.transfer(player,c.getClickedPos(),c.getClickedFace(),c.getItemInHand()))return InteractionResult.SUCCESS;
            }
            if(c.getPlayer() instanceof ServerPlayer who&&!OwnerBackpackInfinity.mayEdit(who,c.getItemInHand()))return InteractionResult.FAIL;
            return super.useOn(c);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            out.accept(Component.literal(CAPACITY[tier]+" storage slots · "+UPGRADE_SLOTS[tier]+" upgrade slots"));
            out.accept(Component.literal("Use to open · Sneak-use to place or transfer"));
            var fluid=OwnerBackpackFluids.contents(stack);if(fluid!=null)out.accept(fluid.describe());
        }
    }
    public static final class UpgradeItem extends Item implements io.github.jimbozoomer.jugcraft.tools.Chargeable {
        public final String name;
        UpgradeItem(Properties properties,String name){super(properties);this.name=name;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!OwnerBackpackConversions.isConversion(player.getItemInHand(hand)))return super.use(level,player,hand);
            if(player instanceof ServerPlayer who){var pack=who.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                if(tier(pack)<0||!OwnerBackpackUpgrades.accessible(who,pack))return InteractionResult.FAIL;
                return OwnerBackpackConversions.convert(who,pack,who.getItemInHand(hand),who.getInventory()::setChanged)?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        @Override public long baseCapacity(){return name.equals("battery_upgrade")?100_000:0;}
        @Override public InteractionResult useOn(UseOnContext c){return name.equals("tank_upgrade")?OwnerBackpackFluids.useOn(c):super.useOn(c);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
            String key=getDescriptionId()+".tooltip";if(net.minecraft.locale.Language.getInstance().has(key))out.accept(Component.translatable(key));
            if(name.equals("survival_infinity_upgrade"))out.accept(Component.literal("Server option: backpacks.survival_infinity (off by default)."));
            if(name.equals("battery_upgrade"))out.accept(Component.literal(io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(stack)+" / 100,000 JE"));
            if(name.equals("xp_pump_upgrade"))out.accept(Component.literal(stack.getOrDefault(io.github.jimbozoomer.jugcraft.concordance.assay.AlchemyStorage.EXPERIENCE,0)+" stored experience points"));
        }
    }
}
