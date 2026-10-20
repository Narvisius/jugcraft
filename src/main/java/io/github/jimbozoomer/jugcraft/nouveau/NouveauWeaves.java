package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A saved visual facade never copies a container, its contents, or a block's gameplay behavior. */
public final class NouveauWeaves {
    public static BlockEntityType<Weave> TYPE;
    private NouveauWeaves(){}
    public static void register(){
        List<Block> blocks=new ArrayList<>();for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(e.kind().equals("weave")){
            blocks.add(NouveauCatalog.registerBlock(e,BlockBehaviour.Properties.of().strength(.5F).noOcclusion(),p->new WeaveBlock(e.source(),p.lightLevel(s->s.getValue(WeaveBlock.LIGHT)))));NouveauCatalog.registerBlockItem(e);
        }
        TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_weave"),FabricBlockEntityTypeBuilder.create(Weave::new,blocks.toArray(Block[]::new)).build());
    }
    public static final class WeaveBlock extends BaseEntityBlock {
        public static final net.minecraft.world.level.block.state.properties.IntegerProperty LIGHT=net.minecraft.world.level.block.state.properties.IntegerProperty.create("light",0,15);
        public final String kind;WeaveBlock(String kind,Properties p){super(p);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(LIGHT,0));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LIGHT);}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Weave(p,s);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return kind.equals("falseweave")?Shapes.empty():l.getBlockEntity(p) instanceof Weave weave?weave.facade.getCollisionShape(l,p,c):Shapes.block();}
        @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(!(l.getBlockEntity(pos) instanceof Weave weave))return InteractionResult.PASS;
            BlockState facade=null;if(stack.getItem() instanceof BlockItem item)facade=item.getBlock().defaultBlockState();
            else if(stack.has(NouveauUtilities.MIMIC)){var block=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(stack.get(NouveauUtilities.MIMIC)));if(block!=null)facade=block.defaultBlockState();}
            if(facade==null||facade.hasBlockEntity()||facade.getBlock() instanceof WeaveBlock)return InteractionResult.PASS;
            if(player instanceof ServerPlayer p){if(!JugNouveau.enabled()||!Authority.mayChange(p.level(),p,pos))return InteractionResult.FAIL;weave.facade=facade;weave.setChanged();l.setBlock(pos,s.setValue(LIGHT,facade.getLightEmission()),Block.UPDATE_ALL);l.sendBlockUpdated(pos,s,l.getBlockState(pos),Block.UPDATE_CLIENTS);}return InteractionResult.SUCCESS;
        }
    }
    public static final class Weave extends BlockEntity {
        private BlockState facade=Blocks.STONE.defaultBlockState();
        private boolean hidden,showFacade;
        public boolean hidden(){return hidden;}public boolean showFacade(){return showFacade;}
        public boolean spell(String operation,int power){
            if(level==null)return false;boolean changed=false;
            if(operation.equals("invisibility")&&kind().equals("ghostweave")){hidden=true;changed=true;}
            if(operation.equals("dispel")){if(kind().equals("sky_block")){showFacade=!showFacade;changed=true;}else if(hidden){hidden=false;changed=true;}}
            if(operation.equals("light")){level.setBlock(worldPosition,getBlockState().setValue(WeaveBlock.LIGHT,Math.clamp(11+power,0,15)),Block.UPDATE_ALL);changed=true;}
            if(changed){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}return changed;
        }
        Weave(BlockPos p,BlockState s){super(TYPE,p,s);}
        public BlockState facade(){return facade;}
        public String kind(){return ((WeaveBlock)getBlockState().getBlock()).kind;}
        @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("facade",BlockState.CODEC,facade);out.putBoolean("hidden",hidden);out.putBoolean("showFacade",showFacade);}
        @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);facade=in.read("facade",BlockState.CODEC).orElse(Blocks.STONE.defaultBlockState());hidden=in.getBooleanOr("hidden",false);showFacade=in.getBooleanOr("showFacade",false);}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    }
}
