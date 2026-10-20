package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;

/** The owner's Jug Nouveau resources, ported into Jugcraft's existing authority and resource systems. */
public final class JugNouveau {
    public static final String[] COLORS = {"black", "blue", "green", "purple", "red", "white"};
    public static EntityType<BookWyrm> BOOK_WYRM;
    public static EntityType<Starbuncle> STARBUNCLE;
    public static Block LECTERN;
    public static BlockEntityType<StorageLectern> LECTERN_ENTITY;
    public static MenuType<StorageLecternMenu> MENU;
    public static Item CHARM, WAND, LECTERN_ITEM;
    public static Item STARBUNCLE_CHARM;
    public static DataComponentType<GlobalPos> WAND_LECTERN;
    public static DataComponentType<java.util.UUID> WAND_WORKER;
    public static DataComponentType<Integer> CHARM_COLOR;
    private JugNouveau() {}

    public static void register() {
        WAND_LECTERN=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_lectern"),
            DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(GlobalPos.CODEC)).build());
        WAND_WORKER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_worker"),
            DataComponentType.<java.util.UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(UUIDUtil.CODEC)).build());
        CHARM_COLOR=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("nouveau_color"),
            DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(0,15)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        var entityKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("book_wyrm"));
        BOOK_WYRM=Registry.register(BuiltInRegistries.ENTITY_TYPE,entityKey,EntityType.Builder.<BookWyrm>of(BookWyrm::new,MobCategory.MISC)
            .sized(0.65F,0.65F).eyeHeight(0.45F).clientTrackingRange(10).build(entityKey));
        FabricDefaultAttributeRegistry.register(BOOK_WYRM,BookWyrm.attributes());
        var starKey=ResourceKey.create(Registries.ENTITY_TYPE,Jugcraft.id("starbuncle"));
        STARBUNCLE=Registry.register(BuiltInRegistries.ENTITY_TYPE,starKey,EntityType.Builder.<Starbuncle>of(Starbuncle::new,MobCategory.CREATURE)
            .sized(.6F,.8F).eyeHeight(.5F).clientTrackingRange(10).build(starKey));
        FabricDefaultAttributeRegistry.register(STARBUNCLE,Starbuncle.attributes());NouveauSpawns.register(STARBUNCLE,false);
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld().and(c->c.getBiomeKey().identifier().getPath().contains("forest")),MobCategory.CREATURE,STARBUNCLE,4,1,2);
        var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("bookwyrm_lectern"));
        LECTERN=Registry.register(BuiltInRegistries.BLOCK,key,new StorageLecternBlock(BlockBehaviour.Properties.of().setId(key)
            .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.IMMOVEABLE)));
        LECTERN_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("bookwyrm_lectern"),
            FabricBlockEntityTypeBuilder.create(StorageLectern::new,LECTERN).build());
        MENU=Registry.register(BuiltInRegistries.MENU,Jugcraft.id("bookwyrm_storage"),new MenuType<>(StorageLecternMenu::new,FeatureFlags.DEFAULT_FLAGS));
        LECTERN_ITEM=item("bookwyrm_lectern",p->new LecternItem(LECTERN,p.useBlockDescriptionPrefix()));
        CHARM=item("bookwyrm_charm",p->new NouveauItem(p,NouveauItem.Kind.CHARM));
        WAND=item("dominion_wand",p->new NouveauItem(p.stacksTo(1),NouveauItem.Kind.WAND));
        STARBUNCLE_CHARM=item("starbuncle_charm",p->new NouveauItem(p,NouveauItem.Kind.STARBUNCLE));
        NouveauCatalog.load();NouveauPresentation.load();
        NouveauStatuses.register();NouveauResources.register();NouveauNature.register();NouveauUtilities.register();NouveauContainment.register();NouveauPotions.register();
        NouveauDecor.register();NouveauChest.register();NouveauAnimatedDecor.register();NouveauBoat.register();NouveauGear.register();NouveauWorkshops.register();NouveauScrying.register();NouveauPlanarium.register();NouveauSignals.register();NouveauRepositories.register();NouveauStorageTerminal.register();
        net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper.register(Jugcraft.id("nouveau_arcane_poi"),1,1,NouveauCatalog.block("scribes_table").getStateDefinition().getPossibleStates());
        NouveauHelpers.register();NouveauEncounters.register();NouveauSpine.register();NouveauFamiliars.register();
        NouveauControls.register();NouveauAnimatedBlock.register();NouveauSummons.register();NouveauGlyphs.register();NouveauRituals.register();NouveauSource.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->{
            for(var entry:NouveauCatalog.BLOCK_ENTRIES.values()){var item=NouveauCatalog.ITEMS.get(entry.source());if(item!=null&&!entry.kind().equals("internal")&&!entry.kind().contains("wall_sign")&&!entry.source().equals("bookwyrm_lectern"))out.accept(item);}
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{
            for(var entry:NouveauCatalog.ITEM_ENTRIES.values()){var item=NouveauCatalog.ITEMS.get(entry.source());if(item!=null&&item!=CHARM&&item!=WAND&&item!=STARBUNCLE_CHARM)out.accept(item);}
        });
        // Intercept before a chest consumes the right-click to open its ordinary inventory.
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            var stack=player.getItemInHand(hand);
            if(player.isSpectator()||!(stack.getItem() instanceof NouveauItem item))return net.minecraft.world.InteractionResult.PASS;
            if(stack.is(CHARM)&&!(level.getBlockEntity(hit.getBlockPos()) instanceof StorageLectern))return net.minecraft.world.InteractionResult.PASS;
            return item.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out->out.accept(LECTERN_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(CHARM);out.accept(WAND);out.accept(STARBUNCLE_CHARM);});
    }
    private static Item item(String id,java.util.function.Function<Item.Properties,Item> make) {
        var key=ResourceKey.create(Registries.ITEM,Jugcraft.id(id));
        return Registry.register(BuiltInRegistries.ITEM,key,make.apply(new Item.Properties().setId(key)));
    }
    public static boolean enabled(){return Workers.enabled();}
}
