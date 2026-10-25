package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import java.util.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import org.jspecify.annotations.Nullable;

/** Conserved reusable fluid cells and the source recycling/biomass material chain. */
public final class IndustrialProduction {
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    private IndustrialProduction(){}
    public static void register(){
        for(String id:List.of("scrap","scrapbox","plant_ball","biochaff","fluid_cell","tin_can","filled_tin_can")){
            var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_"+id));var properties=new Item.Properties().setId(key);
            if(id.equals("filled_tin_can"))properties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()).usingConvertsTo(ITEMS.get("jimta_tin_can"));
            Item item=id.equals("fluid_cell")?new Cell(properties.stacksTo(1)):id.equals("scrapbox")?new Scrapbox(properties):new Item(properties);
            ITEMS.put("jimta_"+id,Registry.register(BuiltInRegistries.ITEM,key,item));
        }
        FluidStorage.ITEM.registerForItems((stack,context)->Cell.storage(context),ITEMS.get("jimta_fluid_cell"));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->ITEMS.values().forEach(out::accept));
    }
    private static final class Scrapbox extends Item {
        Scrapbox(Properties p){super(p);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer recipient))return InteractionResult.SUCCESS;
            // Consume before rolling/giving: opening a box is one authoritative action, including in creative.
            ItemStack held=player.getItemInHand(hand);if(held.isEmpty())return InteractionResult.FAIL;held.shrink(1);
            var table=server.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Jugcraft.id("jimtafacts/scrapbox")));
            var params=new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN,player.position()).withParameter(LootContextParams.THIS_ENTITY,recipient).create(LootContextParamSets.GIFT);
            for(ItemStack gift:table.getRandomItems(params))if(!recipient.getInventory().add(gift))recipient.spawnAtLocation(server,gift);
            return InteractionResult.SUCCESS;
        }
    }
    public static final class Cell extends Item {
        public static final long CAPACITY=1_000*FluidNetworks.DROPLETS_PER_MB;
        private static final List<String> SKINS=List.of("water","lava","distilled_water","coolant","hot_coolant","steam","superheated_steam","biogas","biomass","ethanol","crude_oil");
        public Cell(Properties p){super(p);}
        public static @Nullable StoredFluid contents(ItemStack stack){
            var value=stack.get(JugcraftFluids.STORED_FLUID);
            return value==null||value.amount()<=0||value.variant().isBlank()?null:new StoredFluid(value.variant(),Math.min(CAPACITY,value.amount()));
        }
        public static ItemStack filled(ItemStack stack,FluidVariant fluid,long amount){
            var result=stack.copyWithCount(1);float frame=0;
            if(amount<=0||fluid.isBlank())result.remove(JugcraftFluids.STORED_FLUID);
            else{result.set(JugcraftFluids.STORED_FLUID,new StoredFluid(fluid,Math.min(amount,CAPACITY)));int index=SKINS.indexOf(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath());frame=index<0?12:index+1;}
            result.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(frame),List.of(),List.of(),List.of()));return result;
        }
        public static Storage<FluidVariant> storage(ContainerItemContext context){return new SingleSlotStorage<>(){
            private StoredFluid held(){return contents(context.getItemVariant().toStack());}
            private boolean exchange(FluidVariant fluid,long amount,TransactionContext tx){var current=context.getItemVariant().toStack();return current.getItem() instanceof Cell&&context.exchange(ItemVariant.of(filled(current,fluid,amount)),1,tx)==1;}
            @Override public long insert(FluidVariant fluid,long maximum,TransactionContext tx){
                if(fluid.isBlank()||maximum<=0)return 0;var held=held();if(held!=null&&!held.variant().equals(fluid))return 0;
                long amount=held==null?0:held.amount(),moved=Math.min(maximum,CAPACITY-amount);return moved>0&&exchange(fluid,amount+moved,tx)?moved:0;
            }
            @Override public long extract(FluidVariant fluid,long maximum,TransactionContext tx){var held=held();if(held==null||maximum<=0||!held.variant().equals(fluid))return 0;long moved=Math.min(maximum,held.amount());return exchange(fluid,held.amount()-moved,tx)?moved:0;}
            @Override public boolean isResourceBlank(){return held()==null;}
            @Override public FluidVariant getResource(){return held()==null?FluidVariant.blank():held().variant();}
            @Override public long getAmount(){return held()==null?0:held().amount();}
            @Override public long getCapacity(){return CAPACITY;}
        };}
        @Override public InteractionResult useOn(UseOnContext context){
            Player player=context.getPlayer();if(player==null)return InteractionResult.PASS;
            if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            if(!Authority.mayChange(level,player,context.getClickedPos()))return InteractionResult.FAIL;
            var tank=FluidStorage.SIDED.find(level,context.getClickedPos(),context.getClickedFace());if(tank==null)return InteractionResult.PASS;
            var cell=storage(ContainerItemContext.ofPlayerHand(player,context.getHand()));long moved;
            try(var tx=Transaction.openOuter()){moved=player.isSecondaryUseActive()?StorageUtil.move(cell,tank,v->true,CAPACITY,tx):StorageUtil.move(tank,cell,v->true,CAPACITY,tx);if(moved>0)tx.commit();}
            var held=contents(player.getItemInHand(context.getHand()));player.sendOverlayMessage(held==null?Component.literal("Fluid cell: empty"):held.describe());return InteractionResult.SUCCESS;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
            super.appendHoverText(stack,context,display,lines,flag);var held=contents(stack);lines.accept(held==null?Component.literal("Empty — 1,000 mB capacity"):held.describe());lines.accept(Component.literal("Use on a tank to fill; sneak-use to empty. Reusable."));
        }
    }
}
