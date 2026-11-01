package io.github.jimbozoomer.jugcraft.loot;

import java.util.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.finance.LandRegistry;
import io.github.jimbozoomer.jugcraft.town.TownProtection;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.PushReaction;

/** Original Lootr container families with private, saved per-player contents. */
public final class OwnerLoot {
    private OwnerLoot() {}
    public static final Map<String,OwnerLootBlock> BLOCKS=new LinkedHashMap<>();
    public static BlockEntityType<OwnerLootContainer> TYPE;
    public static void register(){
        OwnerLootEntities.register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
            net.minecraft.commands.Commands.literal("jugcraftlootentity").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(net.minecraft.commands.Commands.argument("entity",net.minecraft.commands.arguments.EntityArgument.entity())
                    .then(net.minecraft.commands.Commands.argument("table",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx->{
                        var entity=net.minecraft.commands.arguments.EntityArgument.getEntity(ctx,"entity");var id=net.minecraft.resources.Identifier.tryParse(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"table"));
                        if(!(entity instanceof OwnerLootEntities.Personal personal)||id==null){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Select a personal-loot minecart/frame and a valid table."));return 0;}
                        var table=ResourceKey.create(Registries.LOOT_TABLE,id);
                        if(ctx.getSource().getServer().reloadableRegistries().getLootTable(table)==net.minecraft.world.level.storage.loot.LootTable.EMPTY){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Missing or empty loot table."));return 0;}
                        try{personal.rewards().configure(table,ctx.getSource().getLevel().getRandom().nextLong());}catch(IllegalStateException ex){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Already configured; existing rewards were preserved."));return 0;}
                        ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Personal entity rewards assigned."),true);return 1;
                    })))));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(
            net.minecraft.commands.Commands.literal("jugcraftloot").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(net.minecraft.commands.Commands.argument("position",net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                    .then(net.minecraft.commands.Commands.argument("table",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx->{
                        var at=net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx,"position");
                        var id=net.minecraft.resources.Identifier.tryParse(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"table"));
                        if(id==null||!(ctx.getSource().getLevel().getBlockEntity(at) instanceof OwnerLootContainer target)){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Choose a personal loot container and a valid loot-table ID."));return 0;}
                        var key=ResourceKey.create(Registries.LOOT_TABLE,id);
                        if(ctx.getSource().getServer().reloadableRegistries().getLootTable(key)==net.minecraft.world.level.storage.loot.LootTable.EMPTY){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("That loot table is missing or empty."));return 0;}
                        try{target.configure(key,ctx.getSource().getLevel().getRandom().nextLong());}
                        catch(IllegalStateException e){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This container is already configured. Existing player inventories were preserved."));return 0;}
                        ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Personal rewards assigned from "+id+". Each player can collect once."),true);return 1;
                    })))));
        for(String name:List.of("chest","trapped_chest","barrel","shulker_box","copper_chest","exposed_copper_chest","weathered_copper_chest","oxidized_copper_chest","decorated_pot","suspicious_sand","suspicious_gravel","inventory")){
            var key=ResourceKey.create(Registries.BLOCK,Jugcraft.id("personal_loot_"+name));
            var block=new OwnerLootBlock(BlockBehaviour.Properties.of().setId(key).strength(-1,3600000).noOcclusion().pushReaction(PushReaction.IMMOVEABLE),name);
            Registry.register(BuiltInRegistries.BLOCK,key.identifier(),block);BLOCKS.put(name,block);
            var itemKey=ResourceKey.create(Registries.ITEM,key.identifier());Registry.register(BuiltInRegistries.ITEM,itemKey.identifier(),new BlockItem(block,new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        }
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("personal_loot"),FabricBlockEntityTypeBuilder.create(OwnerLootContainer::new,BLOCKS.values().toArray(Block[]::new)).build());
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!(player instanceof ServerPlayer p)||player.isShiftKeyDown()||!mayOpen(player,level,hit.getBlockPos()))return InteractionResult.PASS;
            var original=level.getBlockEntity(hit.getBlockPos());
            if(!(original instanceof RandomizableContainer source)||source.getLootTable()==null)return InteractionResult.PASS;
            // Only unopened generated vanilla loot containers. Player-built/storage inventories are untouched.
            var state=original.getBlockState();var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if(!id.getNamespace().equals("minecraft"))return InteractionResult.PASS;
            String name=id.getPath();if(name.endsWith("shulker_box"))name="shulker_box";
            var replacement=BLOCKS.get(name);if(replacement==null)return InteractionResult.PASS;
            var table=source.getLootTable();long seed=source.getLootTableSeed();
            var decorations=original instanceof DecoratedPotBlockEntity pot?pot.getDecorations():PotDecorations.EMPTY;
            var originalData=original.saveWithFullMetadata(level.registryAccess());
            var next=replacement.defaultBlockState();
            for(var property:next.getProperties())next=copyProperty(state,next,property);
            // Prevent the outgoing vanilla container from rolling/spilling its loot during replacement.
            source.setLootTable(null);
            if(!level.setBlock(hit.getBlockPos(),next,Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS)){source.setLootTable(table);source.setLootTableSeed(seed);return InteractionResult.FAIL;}
            if(level.getBlockEntity(hit.getBlockPos()) instanceof OwnerLootContainer target){target.configure(table,seed);target.decorations(decorations);target.open(p);return InteractionResult.SUCCESS;}
            // Unexpected registry failure: restore the original untouched loot-table source.
            level.setBlock(hit.getBlockPos(),state,Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);
            var restored=level.getBlockEntity(hit.getBlockPos());
            if(restored!=null){restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),originalData));restored.setChanged();}
            return InteractionResult.FAIL;
        });
    }
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from,BlockState to,net.minecraft.world.level.block.state.properties.Property<T> property){return from.hasProperty(property)?to.setValue(property,from.getValue(property)):to;}
    public static boolean mayOpen(Player player,Level level,BlockPos pos){
        return player!=null&&player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64
            &&LandRegistry.mayBuild(player,level,pos)&&!TownProtection.denies(player,level,pos)&&level.getWorldBorder().isWithinBounds(pos);
    }
}
