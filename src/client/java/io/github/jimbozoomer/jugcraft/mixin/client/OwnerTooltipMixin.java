package io.github.jimbozoomer.jugcraft.mixin.client;
import java.util.List;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.jimbozoomer.jugcraft.client.OwnerTooltipFrames;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
@Mixin(GuiGraphicsExtractor.class)
public abstract class OwnerTooltipMixin {
    @Shadow private Runnable deferredTooltip;
    @Unique private ItemStack jugcraft$requested=ItemStack.EMPTY,jugcraft$rendering=ItemStack.EMPTY;
    @WrapMethod(method="setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V")
    private void jugcraft$item(Font font,ItemStack stack,int x,int y,Operation<Void> original){
        var previous=jugcraft$requested;jugcraft$requested=stack.copy();try{original.call(font,stack,x,y);}finally{jugcraft$requested=previous;}
    }
    @WrapMethod(method="setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;ZZ)V")
    private void jugcraft$capture(Font font,List<ClientTooltipComponent> lines,int x,int y,ClientTooltipPositioner positioner,Identifier style,boolean replace,boolean extra,Operation<Void> original){
        Runnable before=deferredTooltip;original.call(font,lines,x,y,positioner,style,replace,extra);
        if(before==deferredTooltip||deferredTooltip==null)return;
        var stack=jugcraft$requested.copy();Runnable draw=deferredTooltip;
        deferredTooltip=()->{var previous=jugcraft$rendering;jugcraft$rendering=stack;try{draw.run();}finally{jugcraft$rendering=previous;}};
    }
    @WrapMethod(method="tooltip")
    private void jugcraft$frame(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,Identifier style,boolean extra,Operation<Void> original){
        var parts=style==null?OwnerTooltipFrames.components(components,jugcraft$rendering,font):components;
        original.call(font,parts,x,y,positioner,style,extra);
        if(style==null)OwnerTooltipFrames.decorate((GuiGraphicsExtractor)(Object)this,font,parts,jugcraft$rendering,x,y,positioner,extra);
    }
}
