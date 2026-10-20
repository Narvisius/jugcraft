package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;

public final class NouveauWorkshops {
    public static BlockEntityType<NouveauWorkshop> ENTITY;
    public static MenuType<NouveauWorkshopMenu> MENU;
    private NouveauWorkshops(){}
    public record Search(int menu,String query) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        public static final Type<Search> TYPE=new Type<>(Jugcraft.id("nouveau_recipe_search"));
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Search> CODEC=new net.minecraft.network.codec.StreamCodec<>(){public Search decode(net.minecraft.network.RegistryFriendlyByteBuf b){return new Search(b.readVarInt(),b.readUtf(48));}public void encode(net.minecraft.network.RegistryFriendlyByteBuf b,Search p){b.writeVarInt(p.menu);b.writeUtf(p.query,48);}};public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(){
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(Search.TYPE,Search.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Search.TYPE,(packet,context)->{var p=context.player();if(p.containerMenu instanceof NouveauWorkshopMenu menu&&menu.containerId==packet.menu())menu.search(p,packet.query());});
        List<Block> blocks=new ArrayList<>();
        for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(e.kind().equals("station")){
            var b=NouveauCatalog.registerBlock(e,BlockBehaviour.Properties.of().strength(2.5F).noOcclusion().sound(SoundType.STONE)
                .mapColor(MapColor.COLOR_PURPLE).pushReaction(PushReaction.IMMOVEABLE),p->new NouveauWorkshopBlock(e,p));blocks.add(b);
            NouveauCatalog.registerItem(e,p->e.geo().isEmpty()?new BlockItem(b,p.useBlockDescriptionPrefix()):new NouveauWorkshopItem(e,b,p.useBlockDescriptionPrefix()));
        }
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_workshop"),FabricBlockEntityTypeBuilder.create(NouveauWorkshop::new,blocks.toArray(Block[]::new)).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("nouveau_workshop"),new MenuType<>(NouveauWorkshopMenu::new,FeatureFlags.DEFAULT_FLAGS));
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Jugcraft.id("nouveau_recipes"),NouveauRecipes.INSTANCE);
    }
}
