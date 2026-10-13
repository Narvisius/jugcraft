package local.peepo;

import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CompanionStool extends Block implements Seat.Sittable {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(3,5,3,13,7,13),Block.box(4,0,4,6,5,6),Block.box(10,0,4,12,5,6),Block.box(4,0,10,6,5,12),Block.box(10,0,10,12,5,12));
    public CompanionStool(Properties p) { super(p); }
    public double seatHeight(BlockState state) { return 7/16.0; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player.isSecondaryUseActive())return InteractionResult.PASS;
        if(level instanceof ServerLevel server && !Seat.sit(server,pos,state,player))return InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }
    public static void initialize() {
        var id=PeepoMod.id("wooden_stool");
        var block=Registry.register(BuiltInRegistries.BLOCK,id,new CompanionStool(Properties.of().setId(ResourceKey.create(Registries.BLOCK,id)).strength(1).sound(SoundType.WOOD).noOcclusion()));
        var item=Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e->e.accept(item));
    }
}
