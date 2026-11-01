package io.github.jimbozoomer.jugcraft.client;
import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.arcane.*;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

record ArcaneItemDetails() implements RangeSelectItemModelProperty {
    static final MapCodec<ArcaneItemDetails> CODEC=MapCodec.unit(new ArcaneItemDetails());
    static void register(){RangeSelectItemModelProperties.ID_MAPPER.put(Jugcraft.id("arcane_jar_fill"),CODEC);ItemTooltipCallback.EVENT.register((stack,context,type,lines)->{
        if(ArcaneJar.portable(stack))lines.add(Component.literal(new String[]{"Aureal","Souls","Blood","Experience"}[ArcaneJar.kind(stack)]+": "+ArcaneJar.amount(stack)+" / 10000"));
        if(stack.has(ArcaneModifiers.MODIFIER)){String modifier=stack.get(ArcaneModifiers.MODIFIER);if(!modifier.isEmpty())lines.add(Component.literal("Modifier: "+Character.toUpperCase(modifier.charAt(0))+modifier.substring(1)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));}
        if(stack.getItem() instanceof ArcaneBuckets bucket)lines.add(Component.literal("Stored: "+ArcaneBuckets.amount(stack)+" / "+bucket.capacity+" buckets"));
        if(stack.has(ArcaneCapture.CREATURE))lines.add(Component.literal("Contains: "+stack.get(ArcaneCapture.CREATURE).copyTag().getStringOr("name","creature")));
        if(stack.is(ForbiddenArcane.item("blood_test_tube")))lines.add(Component.literal("Blood: "+ArcaneEssences.amount(stack)+" / 3000"));
    });}
    public float get(ItemStack s,ClientLevel l,ItemOwner owner,int seed){int amount=ArcaneJar.amount(s);return amount==0?0:1+17*ArcaneJar.kind(s)+(amount-1)/625;}
    public MapCodec<ArcaneItemDetails> type(){return CODEC;}
}
