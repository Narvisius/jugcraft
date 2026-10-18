package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Storage is physical, not shared by bag color. All material conversion keeps the existing audited ledger. */
public final class AlchemyStorage {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final String[] COLORS = {"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    public static final int XP_CAPACITY = 1_000_000, XP_BATCH = 100;
    public static DataComponentType<Integer> EXPERIENCE;
    public static BlockEntityType<AlchemyStorageBlockEntity> ENTITY;
    public static MenuType<AlchemyStorageMenu> MENU;
    public static MenuType<AlchemyStorageCatalogueMenu> CATALOGUE_MENU;
    /** The client supplies its existing native guide panel. No client classes in common initialization. */
    public static Runnable openManual = () -> {};
    private AlchemyStorage() {}

    public static void register() {
        AlchemyPatterns.register(); AlchemyAutomation.register(); GemAbilities.register();
        EXPERIENCE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("mind_experience"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0,XP_CAPACITY)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("alchemical_storage"), new MenuType<>(AlchemyStorageMenu::new,FeatureFlags.DEFAULT_FLAGS));
        CATALOGUE_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("alchemical_catalogue"), new MenuType<>(AlchemyStorageCatalogueMenu::new,FeatureFlags.DEFAULT_FLAGS));
        for (String name : List.of("alchemical_chest", "transmutation_table")) {
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
            Block block = Registry.register(BuiltInRegistries.BLOCK,key,new AlchemyStorageBlock(BlockBehaviour.Properties.of().setId(key)
                    .mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).strength(3.5F,8).noOcclusion(), name.equals("alchemical_chest")));
            BLOCKS.put(name,block);
            var itemKey = ResourceKey.create(Registries.ITEM,Jugcraft.id(name));
            ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,itemKey,new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        }
        ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("alchemical_storage"),
                FabricBlockEntityTypeBuilder.create(AlchemyStorageBlockEntity::new,BLOCKS.get("alchemical_chest")).build());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((be,side)->net.fabricmc.fabric.api.transfer.v1.storage.Storage.empty(),ENTITY);
        for (String color : COLORS) item(color+"_alchemical_bag",true);
        for (String name : List.of("transmutation_tablet","alchemy_manual","alchemy_tome","gem_of_eternal_density","void_ring","mind_stone")) item(name,false);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> ITEMS.values().forEach(e::accept));
    }
    private static void item(String name,boolean bag) {
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(name));
        var props=new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.RARE);
        if(bag) props.component(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        if(name.equals("mind_stone")) props.component(EXPERIENCE,0);
        ITEMS.put(name,Registry.register(BuiltInRegistries.ITEM,key,new StorageItem(props,name,bag)));
    }
    public static boolean bag(ItemStack stack) { return stack.getItem() instanceof StorageItem item && item.bag; }
    /** Portable inventories cannot nest; ordinary empty container blocks remain normal building supplies. */
    public static boolean canStore(ItemStack stack) {
        // Vanilla chest/barrel items carry an EMPTY container component by default in 26.3.
        // Only a truly empty block item may retain that component; filled chests still cannot nest.
        var contents=stack.get(DataComponents.CONTAINER);
        boolean emptyBuildingBlock=stack.getItem() instanceof BlockItem && ItemContainerContents.EMPTY.equals(contents);
        return !bag(stack) && (contents==null || emptyBuildingBlock) && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && !stack.has(DataComponents.BLOCK_ENTITY_DATA) && !stack.has(DataComponents.ENTITY_DATA)
                && stack.getItem().canFitInsideContainerItems();
    }
    public static int storedXp(ItemStack stone) { return Math.clamp(stone.getOrDefault(EXPERIENCE,0),0,XP_CAPACITY); }
    /** XP level cost is nonlinear; totalExperience becomes stale after enchanting, so measure the actual bar. */
    public static int currentXp(Player player) {
        long level=Math.max(0,player.experienceLevel);
        double base=level<=16 ? level*level+6*level : level<=31 ? 2.5*level*level-40.5*level+360 : 4.5*level*level-162.5*level+2220;
        return (int)Math.clamp((long)base+Math.round(player.experienceProgress*player.getXpNeededForNextLevel()),0,Integer.MAX_VALUE);
    }
    public static int transferXp(ServerPlayer player,ItemStack stone,boolean withdraw) {
        if(!stone.is(ITEMS.get("mind_stone")) || !AlchemicalGear.ready(player)) return 0;
        int held=storedXp(stone),actual=currentXp(player);
        int moved=withdraw ? Math.min(XP_BATCH,Math.min(held,Integer.MAX_VALUE-actual)) : Math.min(XP_BATCH,Math.min(actual,XP_CAPACITY-held));
        if(moved<=0) return 0;
        // Vanilla resets and restores the exact integer point count, avoiding negative fractional level arithmetic.
        player.experienceLevel=0;player.experienceProgress=0;player.totalExperience=0;
        player.giveExperiencePoints(actual+(withdraw?moved:-moved));
        stone.set(EXPERIENCE,held+(withdraw?-moved:moved));if(storedXp(stone)>0)stone.set(AlchemicalGear.ACTIVE,net.minecraft.util.Unit.INSTANCE);else stone.remove(AlchemicalGear.ACTIVE);player.getInventory().setChanged();
        return moved;
    }
    private static boolean holds(Player p, Item item) { return p.getMainHandItem().is(item)||p.getOffhandItem().is(item); }
    private static final class StorageItem extends Item {
        final String name; final boolean bag;
        StorageItem(Properties properties,String name,boolean bag){super(properties);this.name=name;this.bag=bag;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            if(name.equals("alchemy_manual")) { if(level.isClientSide())openManual.run();return InteractionResult.SUCCESS; }
            if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!RateGate.allow(p,"alchemy_storage",4))return InteractionResult.FAIL;
            ItemStack held=p.getItemInHand(hand);
            if(bag) {
                if(!p.isAlive()||p.isSpectator())return InteractionResult.FAIL;
                p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyStorageMenu(id,inv,hand,held),held.getHoverName()));
                return InteractionResult.SUCCESS;
            }
            if(!AlchemicalGear.ready(p)) {p.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy.unavailable"));return InteractionResult.FAIL;}
            switch(name) {
                case "transmutation_tablet" -> p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyPatternMenu(id,inv,()->holds(p,this),ItemStack.EMPTY),held.getHoverName()));
                case "alchemy_tome" -> p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyStorageCatalogueMenu(id,inv),held.getHoverName()));
                case "gem_of_eternal_density" -> p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyAutomationMenu(id,inv,held,hand==InteractionHand.MAIN_HAND?inv.getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND),held.getHoverName()));
                case "void_ring" -> {
                    if(p.isShiftKeyDown()) {
                        p.openMenu(new SimpleMenuProvider((id,inv,who)->new AlchemyAutomationMenu(id,inv,held,hand==InteractionHand.MAIN_HAND?inv.getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND),held.getHoverName()));
                        return InteractionResult.SUCCESS;
                    }
                    ItemStack feed=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
                    if(feed.isEmpty())p.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy_storage.void_help"));
                    else Assaying.use(p,p.level(),feed,ItemStack.EMPTY);
                }
                case "mind_stone" -> {if(storedXp(held)>0)held.set(AlchemicalGear.ACTIVE,net.minecraft.util.Unit.INSTANCE);else held.remove(AlchemicalGear.ACTIVE);int n=transferXp(p,held,p.isShiftKeyDown());p.sendOverlayMessage(Component.translatable("message.jugcraft.alchemy_storage.xp",n,storedXp(held),XP_CAPACITY));}
                default -> {}
            }
            return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> text,TooltipFlag flag) {
            text.accept(Component.translatable("tooltip.jugcraft."+(bag?"alchemical_bag":name)).withStyle(ChatFormatting.GRAY));
            if(name.equals("mind_stone"))text.accept(Component.translatable("tooltip.jugcraft.alchemy_storage.xp",storedXp(stack),XP_CAPACITY).withStyle(ChatFormatting.AQUA));
            if(bag)text.accept(Component.translatable("tooltip.jugcraft.alchemy_storage.slots",stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().count(),54).withStyle(ChatFormatting.AQUA));
        }
    }
}
