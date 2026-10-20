package io.github.jimbozoomer.jugcraft.client;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauPotions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
public record NouveauFillProperty(boolean potion) implements RangeSelectItemModelProperty {
    public static final MapCodec<NouveauFillProperty> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.BOOL.optionalFieldOf("potion",false).forGetter(NouveauFillProperty::potion)).apply(i,NouveauFillProperty::new));
    public static void register(){RangeSelectItemModelProperties.ID_MAPPER.put(Jugcraft.id("nouveau_fill"),CODEC);}
    public float get(ItemStack stack,ClientLevel level,ItemOwner owner,int seed){return potion?stack.getOrDefault(NouveauPotions.JAR_DOSES,0)/100F:stack.getOrDefault(JugcraftConcordance.LEY_CHARGE,0)/1000F;}
    public MapCodec<NouveauFillProperty> type(){return CODEC;}
}
