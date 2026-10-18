package io.github.jimbozoomer.jugcraft.concordance.assay;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;

/** Per-player learned identities, never client-authored rewards. Removed catalogue entries stay saved but unusable. */
public final class AlchemyPatterns {
    public static AttachmentType<List<String>> KNOWN, FAVORITES;
    public static DataComponentType<String> TARGET;
    public static DataComponentType<Integer> AREA;
    public static MenuType<AlchemyPatternMenu> MENU;
    private AlchemyPatterns() {}
    public static void register() {
        AlchemyTelemetry.register();
        KNOWN=AttachmentRegistry.<List<String>>builder().persistent(Saved.versioned("alchemy_patterns",Codec.STRING.listOf(0,65536))).copyOnDeath().buildAndRegister(Jugcraft.id("alchemy_patterns"));
        FAVORITES=AttachmentRegistry.<List<String>>builder().persistent(Saved.versioned("alchemy_favorites",Codec.STRING.listOf(0,65536))).copyOnDeath().buildAndRegister(Jugcraft.id("alchemy_favorites"));
        AREA=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("alchemy_build_area"),DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
        TARGET=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("alchemy_target"),DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("alchemy_patterns"),new MenuType<>(AlchemyPatternMenu::new,FeatureFlags.DEFAULT_FLAGS));
        PayloadTypeRegistry.serverboundPlay().register(Search.TYPE,Search.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Search.TYPE,(request,ctx)->{
            var p=ctx.player();
            if(p.containerMenu instanceof AlchemyPatternMenu menu&&menu.containerId==request.menu()&&menu.stillValid(p)
                    &&RateGate.allow(p,"alchemy_pattern_search",2)) menu.search(request.text());
        });
    }
    public static List<String> known(Player p){return p.getAttachedOrElse(KNOWN,List.of());}
    public static List<String> favorites(Player p){return p.getAttachedOrElse(FAVORITES,List.of());}
    public static boolean learn(ServerPlayer p,ItemStack stack){
        var m=AlchemicalWorkshopBlockEntity.material(stack);
        if(!AlchemicalGear.ready(p)||m==null||!m.formable())return false;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(known(p).contains(id))return true;
        if(known(p).size()>=65536)return false;
        var ids=new TreeSet<>(known(p));ids.add(id);p.setAttached(KNOWN,List.copyOf(ids));return true;
    }
    public static void favorite(Player p,String id){
        if(!known(p).contains(id))return;
        var ids=new TreeSet<>(favorites(p));if(!ids.remove(id))ids.add(id);p.setAttached(FAVORITES,List.copyOf(ids));
    }
    public static boolean building(ItemStack stack){
        var m=AlchemicalWorkshopBlockEntity.material(stack);
        return m!=null&&m.formable()&&stack.getItem() instanceof BlockItem b&&!b.getBlock().defaultBlockState().hasBlockEntity()
                &&b.getBlock().defaultBlockState().getFluidState().isEmpty();
    }
    public record Search(int menu,String text) implements CustomPacketPayload {
        public static final Type<Search> TYPE=new Type<>(Jugcraft.id("alchemy_pattern_search"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Search> CODEC=new StreamCodec<>(){
            public Search decode(RegistryFriendlyByteBuf b){return new Search(b.readVarInt(),b.readUtf(48));}
            public void encode(RegistryFriendlyByteBuf b,Search p){b.writeVarInt(p.menu);b.writeUtf(p.text,48);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
