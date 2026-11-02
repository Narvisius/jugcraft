package io.github.jimbozoomer.jugcraft.agriculture;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Explicit audited food catalog. Stable per-food block IDs; no registry scanning or work per tick. */
public final class FoodServings {
    private static final Map<Item,FoodServingBlock> SERVINGS=new LinkedHashMap<>();
    public static BlockEntityType<ShowcaseBlockEntity> ENTITY;
    static final ShowcaseBlockEntity.Showcase SLOT=new ShowcaseBlockEntity.Showcase() {
        public int places(){return 1;}
        public boolean accepts(ItemStack stack){return false;}
        public net.minecraft.sounds.SoundEvent putSound(){return SoundEvents.ITEM_PICKUP;}
    };
    private FoodServings() {}
    public static Map<Item,FoodServingBlock> entries(){return java.util.Collections.unmodifiableMap(SERVINGS);}
    public static void register() {
        try(var stream=FoodServings.class.getResourceAsStream("/jugcraft/food_servings.json")) {
            if(stream==null) throw new IllegalStateException("Food serving catalog is missing");
            var rows=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();
            for(var value:rows) {
                var row=value.getAsJsonObject();
                var foodId=Identifier.parse(row.get("id").getAsString());
                if(!BuiltInRegistries.ITEM.containsKey(foodId)) throw new IllegalStateException("Missing serving food: "+foodId);
                var food=BuiltInRegistries.ITEM.getValue(foodId);
                var id=Jugcraft.id("food_serving/"+foodId.getNamespace()+"/"+foodId.getPath());
                var key=ResourceKey.create(Registries.BLOCK,id);
                var shape=PlacedDishBlock.DishShape.valueOf(row.get("shape").getAsString());
                var bounds=row.getAsJsonArray("bounds");
                var outline=Block.box(bounds.get(0).getAsDouble(),bounds.get(1).getAsDouble(),bounds.get(2).getAsDouble(),bounds.get(3).getAsDouble(),bounds.get(4).getAsDouble(),bounds.get(5).getAsDouble());
                var block=new FoodServingBlock(food,shape,outline,BlockBehaviour.Properties.of().setId(key).strength(.3F)
                    .sound(SoundType.WOOL).noOcclusion().pushReaction(PushReaction.POPPED));
                Registry.register(BuiltInRegistries.BLOCK,key,block);
                SERVINGS.put(food,block);
            }
        } catch(java.io.IOException e){throw new IllegalStateException("Unable to read food serving catalog",e);}
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("food_serving"),
            FabricBlockEntityTypeBuilder.<ShowcaseBlockEntity>create((pos,state)->new ShowcaseBlockEntity(ENTITY,pos,state),
                SERVINGS.values().toArray(Block[]::new)).build());
        var phase=Jugcraft.id("food_servings");
        UseBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE,phase);
        UseBlockCallback.EVENT.addPhaseOrdering(Jugcraft.id("set_down_pie"),phase);
        UseBlockCallback.EVENT.register(phase,FoodServings::place);
    }
    public static InteractionResult place(Player player,Level level,InteractionHand hand,BlockHitResult hit) {
        var held=player.getItemInHand(hand);
        var block=SERVINGS.get(held.getItem());
        if(block==null || player.isSpectator() || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        var context=new BlockPlaceContext(new UseOnContext(player,hand,hit));
        BlockPos at=context.getClickedPos();
        var state=block.defaultBlockState().setValue(PlacedDishBlock.FACING,context.getHorizontalDirection().getOpposite());
        if(!context.canPlace() || !state.canSurvive(level,at) || !player.mayUseItemAt(at,hit.getDirection(),held)
                || !level.isUnobstructed(state,at,CollisionContext.empty())) return InteractionResult.FAIL;
        if(!level.isClientSide()) {
            if(!io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(player,level,at)
                    || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player,level,at)) return InteractionResult.FAIL;
            if(!level.setBlock(at,state,Block.UPDATE_ALL)) return InteractionResult.FAIL;
            if(!(level.getBlockEntity(at) instanceof ShowcaseBlockEntity serving) || !serving.put(0,held)) {
                level.removeBlock(at,false);
                return InteractionResult.FAIL;
            }
            held.consume(1,player);
            level.playSound(null,at,SoundEvents.WOOL_PLACE,SoundSource.BLOCKS,.7F,1.1F);
            level.gameEvent(player,GameEvent.BLOCK_PLACE,at);
        }
        return InteractionResult.SUCCESS;
    }
}
