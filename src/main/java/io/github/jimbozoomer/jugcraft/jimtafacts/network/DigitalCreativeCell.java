package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Original creative cell: explicitly configured in Creative, never a craftable survival resource source. */
public final class DigitalCreativeCell extends DigitalStorage.CellItem {
    static final int DISPLAY_AMOUNT=2_097_152;
    static final long DISPLAY_FLUID=1_000_000L*FluidConstants.BUCKET;
    DigitalCreativeCell(Properties properties){super(properties,DISPLAY_AMOUNT);}
    static void register(){
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id("jimta_creative_storage_cell"));
        DigitalStorage.ITEMS.put("creative_storage_cell",Registry.register(BuiltInRegistries.ITEM,key,new DigitalCreativeCell(new Properties().setId(key).stacksTo(1))));
    }
    public static boolean creative(ItemStack stack){return stack.getItem() instanceof DigitalCreativeCell;}
    static boolean fluid(ItemStack stack){return creative(stack)&&!stack.getOrDefault(FluidCells.CONTENTS,FluidCells.Contents.EMPTY).entries().isEmpty();}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(!p.isCreative()||p.isSpectator()){p.sendOverlayMessage(Component.literal("Only Creative players can configure this cell."));return InteractionResult.FAIL;}
        var cell=p.getItemInHand(hand);
        if(p.isShiftKeyDown()){
            cell.remove(DigitalStorage.CONTENTS);cell.remove(FluidCells.CONTENTS);
            p.sendOverlayMessage(Component.literal("Creative cell cleared."));return InteractionResult.SUCCESS;
        }
        var sample=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(sample.isEmpty()||sample.getItem() instanceof DigitalStorage.CellItem||sample.getItem() instanceof FluidCells.Cell){
            p.sendOverlayMessage(Component.literal("Hold an item or filled fluid container in the other hand. Sneak-use clears the cell."));return InteractionResult.FAIL;
        }
        var container=FluidStorage.ITEM.find(sample,ContainerItemContext.withConstant(sample));
        if(container!=null){int scanned=0;for(var view:container){
            if(++scanned>32)break;
            if(!view.isResourceBlank()&&view.getAmount()>0){
                cell.set(FluidCells.CONTENTS,new FluidCells.Contents(List.of(new FluidCells.Entry(view.getResource(),DISPLAY_FLUID))));
                cell.remove(DigitalStorage.CONTENTS);p.sendOverlayMessage(Component.literal("Creative fluid source configured. Place in a drive; extraction still needs JE."));return InteractionResult.SUCCESS;
            }
        }}
        // Configure item type only: never clone sample inventories, capabilities or player-owned saved data.
        if(!sample.getItem().canFitInsideContainerItems()){p.sendOverlayMessage(Component.literal("Container-like items cannot be creative sources."));return InteractionResult.FAIL;}
        cell.set(DigitalStorage.CONTENTS,new CellContents(List.of(new CellContents.Entry(new ItemStack(sample.getItem()),DISPLAY_AMOUNT))));
        cell.remove(FluidCells.CONTENTS);p.sendOverlayMessage(Component.literal("Creative item source configured. Place in a drive; extraction still needs JE."));return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> out,TooltipFlag flag){
        var items=stack.getOrDefault(DigitalStorage.CONTENTS,CellContents.EMPTY);
        if(!items.entries().isEmpty())out.accept(Component.literal("Unlimited: ").append(items.entries().getFirst().sample().getHoverName()));
        else out.accept(Component.literal(fluid(stack)?"Unlimited selected fluid":"Unconfigured creative source"));
        out.accept(Component.literal("Creative-only configuration; other-hand sample. Sneak-use clears. No survival recipe."));
        out.accept(Component.literal("Does not accept deposits or consume its configured template."));
    }
}
