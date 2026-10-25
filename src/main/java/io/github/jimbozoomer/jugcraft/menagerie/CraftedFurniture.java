package io.github.jimbozoomer.jugcraft.menagerie;

import com.google.gson.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Owner furniture uses the established seating, storage, claims and vanilla furnace systems. */
public final class CraftedFurniture {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private CraftedFurniture() {}
    public static void register() {
        JsonObject catalog;
        try (var in = CraftedFurniture.class.getResourceAsStream("/jugcraft/menagerie/crafted.json")) {
            catalog = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException(e); }
        for (var entry : catalog.getAsJsonObject("blocks").entrySet()) {
            String id = entry.getKey(); var definition = entry.getValue().getAsJsonObject();
            String kind = definition.get("kind").getAsString();
            var key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
            var props = BlockBehaviour.Properties.of().setId(key).strength(2).noOcclusion().sound(SoundType.WOOD);
            Block block = kind.equals("crafted_bed") ? new CraftedBedBlock(props)
                : kind.equals("crafted_oven") ? new FurnaceBlock(props.lightLevel(s -> s.getValue(FurnaceBlock.LIT) ? 13 : 0))
                : HouseholdBlock.create(props, definition);
            Registry.register(BuiltInRegistries.BLOCK, key, block);
            BLOCKS.put(id, block); Menagerie.BLOCKS.put(id, block);
            Menagerie.item(id, p -> new BlockItem(block, p.useBlockDescriptionPrefix()));
            if (kind.equals("storage")) ((FabricBlockEntityType)JugcraftAgriculture.KITCHEN_CABINET_ENTITY).addValidBlock(block);
            if (kind.equals("crafted_oven")) ((FabricBlockEntityType)BlockEntityTypes.FURNACE).addValidBlock(block);
        }
        for (var entry : catalog.getAsJsonObject("items").entrySet()) {
            String name = entry.getValue().getAsJsonObject().get("source").getAsString();
            Menagerie.item(entry.getKey(), p -> name.equals("hammer") ? new Hammer(p.stacksTo(1))
                : name.equals("fancy_painting") ? new FancyPaintingItem(p) : new Item(p));
        }
    }
    private static final class FancyPaintingItem extends Item {
        FancyPaintingItem(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            if(c.getClickedFace().getAxis().isVertical()||c.getPlayer()==null)return InteractionResult.FAIL;
            var level=c.getLevel();var pos=c.getClickedPos().relative(c.getClickedFace());
            if(!Menagerie.mayChange(level,c.getPlayer(),pos))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            var choices=new ArrayList<net.minecraft.world.entity.decoration.painting.Painting>();
            for(var holder:level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).listElements().toList()){
                var id=holder.key().identifier();if(!id.getNamespace().equals("jugcraft")||!id.getPath().startsWith("crafted_"))continue;
                var painting=new net.minecraft.world.entity.decoration.painting.Painting(level,pos,c.getClickedFace(),holder);
                if(painting.survives())choices.add(painting);
            }
            if(choices.isEmpty())return InteractionResult.FAIL;
            int area=choices.stream().mapToInt(p->p.getVariant().value().width()*p.getVariant().value().height()).max().orElse(0);
            choices.removeIf(p->p.getVariant().value().width()*p.getVariant().value().height()<area);
            var painting=choices.get(level.getRandom().nextInt(choices.size()));
            if(level.addFreshEntity(painting)){painting.playPlacementSound();c.getItemInHand().consume(1,c.getPlayer());return InteractionResult.SUCCESS;}
            return InteractionResult.FAIL;
        }
    }
    private static final class Hammer extends Item {
        Hammer(Properties p) { super(p); }
        @Override public InteractionResult useOn(UseOnContext c) {
            var p = c.getPlayer(); var state = c.getLevel().getBlockState(c.getClickedPos());
            if (p == null || !(state.getBlock() instanceof CraftedBlock b) || !Menagerie.mayChange(c.getLevel(), p, c.getClickedPos())) return InteractionResult.PASS;
            if (!c.getLevel().isClientSide()) b.customize(c.getLevel(), c.getClickedPos(), state, p.isShiftKeyDown(), p);
            return InteractionResult.SUCCESS;
        }
    }
}
