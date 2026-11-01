package io.github.jimbozoomer.jugcraft.botany;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

/** Carries the complete native spawner state once, consuming the aggregator only after successful replacement. */
public final class BotanicalSpawnerMover extends Item {
    public static DataComponentType<CustomData> SPAWNER;
    BotanicalSpawnerMover(Properties p){super(p.stacksTo(1));}
    static void register(){SPAWNER=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,Jugcraft.id("botanical_spawner_payload"),DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(p.isSpectator())return InteractionResult.FAIL;var stack=context.getItemInHand();if(p.getCooldowns().isOnCooldown(stack))return InteractionResult.FAIL;
        var level=p.level();var payload=stack.get(SPAWNER);
        if(payload==null){
            var at=context.getClickedPos();if(!level.hasChunkAt(at)||!Authority.mayChange(level,p,at)||!level.getBlockState(at).is(Blocks.SPAWNER)||!(level.getBlockEntity(at) instanceof SpawnerBlockEntity spawner))return InteractionResult.PASS;
            var saved=CustomData.of(spawner.saveWithFullMetadata(level.registryAccess()));
            if(!level.removeBlock(at,false))return InteractionResult.FAIL;
            stack.set(SPAWNER,saved);p.getCooldowns().addCooldown(stack,20);return InteractionResult.SUCCESS;
        }
        var placement=new BlockPlaceContext(context);var at=placement.getClickedPos();var old=level.getBlockState(at);
        if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||!Authority.mayChange(level,p,at)||!placement.canPlace()||level.getBlockEntity(at)!=null||!old.canBeReplaced(placement)||!level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(at),e->e.isAlive()&&!e.isSpectator()).isEmpty())return InteractionResult.FAIL;
        var saved=payload.copyTag();saved.putInt("x",at.getX());saved.putInt("y",at.getY());saved.putInt("z",at.getZ());
        var replacement=new SpawnerBlockEntity(at,Blocks.SPAWNER.defaultBlockState());
        replacement.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
        if(!level.setBlockAndUpdate(at,Blocks.SPAWNER.defaultBlockState()))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(at) instanceof SpawnerBlockEntity)){level.setBlockAndUpdate(at,old);return InteractionResult.FAIL;}
        level.setBlockEntity(replacement);replacement.setChanged();level.sendBlockUpdated(at,old,Blocks.SPAWNER.defaultBlockState(),3);
        // The source consumes the filled tool, including its captured data, on placement.
        stack.shrink(1);return InteractionResult.SUCCESS;
    }
}
