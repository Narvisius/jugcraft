package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.UUID;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.*;

/** The placed rail remembers whose permissions authorize an automated cart's short spectral hop. */
public final class BotanicalGhostRail extends BaseRailBlock implements EntityBlock {
    public static BlockEntityType<Owner> OWNER;
    BotanicalGhostRail(Properties p){super(true,p.noOcclusion());registerDefaultState(defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT,RailShape.NORTH_SOUTH).setValue(WATERLOGGED,false));}
    static void register(){OWNER=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("botanical_ghost_rail"),FabricBlockEntityTypeBuilder.create(Owner::new,Jugtania.block("ghost_rail")).build());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.RAIL_SHAPE_STRAIGHT,WATERLOGGED);}
    @Override public Property<RailShape> getShapeProperty(){return BlockStateProperties.RAIL_SHAPE_STRAIGHT;}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new Owner(at,state);}
    @Override public void setPlacedBy(Level l,BlockPos at,BlockState s,LivingEntity placer,ItemStack item){super.setPlacedBy(l,at,s,placer,item);if(placer instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Owner owner){owner.id=p.getUUID();owner.setChanged();}}
    public static final class Owner extends BlockEntity {
        private UUID id;Owner(BlockPos at,BlockState s){super(OWNER,at,s);}
        public ServerPlayer actor(){return level instanceof ServerLevel l?Authority.answering(l,id):null;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(id!=null)out.store("owner",UUIDUtil.CODEC,id);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);id=in.read("owner",UUIDUtil.CODEC).orElse(null);}
    }
}
