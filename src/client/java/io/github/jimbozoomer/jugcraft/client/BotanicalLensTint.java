package io.github.jimbozoomer.jugcraft.client;

import com.mojang.serialization.MapCodec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.botany.BotanicalLenses;
import net.minecraft.client.color.item.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

record BotanicalLensTint() implements ItemTintSource {
    static final MapCodec<BotanicalLensTint> CODEC=MapCodec.unit(new BotanicalLensTint());
    static void register(){ItemTintSources.ID_MAPPER.put(Jugcraft.id("botanical_cosmetic"),Cosmetic.CODEC);ItemTintSources.ID_MAPPER.put(Jugcraft.id("botanical_rainbow"),Rainbow.CODEC);ItemTintSources.ID_MAPPER.put(Jugcraft.id("botanical_lens"),CODEC);}
    @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity entity){
        int color=stack.getOrDefault(BotanicalLenses.COLOR,-1);
        if(color<0)return 0xFFFFFFFF;
        int rgb=color==16?net.minecraft.util.Mth.hsvToRgb(((level==null?0:level.getGameTime())%180)/180F,1,1):DyeColor.byId(color).getTextureDiffuseColor();return 0xFF000000|rgb;
    }
    @Override public MapCodec<BotanicalLensTint> type(){return CODEC;}
    record Cosmetic() implements ItemTintSource {
        static final MapCodec<Cosmetic> CODEC=MapCodec.unit(new Cosmetic());
        @Override public int calculate(ItemStack stack,ClientLevel level,LivingEntity entity){
            if(stack.is(io.github.jimbozoomer.jugcraft.botany.Jugtania.item("cosmetic_orange_shades")))return 0xB2FFFFFF;
            var colors=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,net.minecraft.world.item.component.CustomModelData.EMPTY).colors();
            return colors.isEmpty()?0xFFFFFFFF:0xFF000000|colors.getFirst();
        }
        @Override public MapCodec<Cosmetic> type(){return CODEC;}
    }
    record Rainbow() implements ItemTintSource {
        static final MapCodec<Rainbow> CODEC=MapCodec.unit(new Rainbow());
        static int color(){var level=net.minecraft.client.Minecraft.getInstance().level;return 0xFF000000|net.minecraft.util.Mth.hsvToRgb(((level==null?0:level.getGameTime())%360)/360F,.45F,1);}
        @Override public int calculate(ItemStack s,ClientLevel l,LivingEntity e){return color();}
        @Override public MapCodec<Rainbow> type(){return CODEC;}
    }
}
