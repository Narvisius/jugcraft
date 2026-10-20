package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;

/** Native editable signs, single-block helper beds and spell-only blocks. */
public final class NouveauDecor {
    public static BlockEntityType<SignBlockEntity> SIGN,HANGING_SIGN;
    private NouveauDecor(){}
    public static void register(){
        List<Block> signs=new ArrayList<>(),hanging=new ArrayList<>();
        for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(Set.of("sign","wall_sign","hanging_sign","hanging_wall_sign").contains(e.kind())){
            Block b=NouveauCatalog.registerBlock(e,BlockBehaviour.Properties.of().strength(1).noCollision().sound(SoundType.WOOD).mapColor(MapColor.WOOD),p->switch(e.kind()){
                case "sign"->new StandingSignBlock(WoodType.OAK,p){@Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SignBlockEntity(SIGN,pos,state);}};
                case "wall_sign"->new WallSignBlock(WoodType.OAK,p){@Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SignBlockEntity(SIGN,pos,state);}};
                case "hanging_sign"->new CeilingHangingSignBlock(WoodType.OAK,p){@Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HangingText(pos,state);}};
                default->new WallHangingSignBlock(WoodType.OAK,p){@Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HangingText(pos,state);}};
            });(e.kind().contains("hanging")?hanging:signs).add(b);
        }
        SIGN=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_sign"),FabricBlockEntityTypeBuilder.create((BlockPos pos,BlockState state)->new SignBlockEntity(SIGN,pos,state),signs.toArray(Block[]::new)).build());
        HANGING_SIGN=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_hanging_sign"),FabricBlockEntityTypeBuilder.<SignBlockEntity>create(HangingText::new,hanging.toArray(Block[]::new)).build());
        var entries=NouveauCatalog.BLOCK_ENTRIES;
        NouveauCatalog.registerItem(entries.get("archwood_sign"),p->new StandingAndWallBlockItem(NouveauCatalog.block("archwood_sign"),NouveauCatalog.block("archwood_wall_sign"),Direction.DOWN,p.stacksTo(16)));
        NouveauCatalog.registerItem(entries.get("archwood_hanging_sign"),p->new HangingSignItem(NouveauCatalog.block("archwood_hanging_sign"),NouveauCatalog.block("archwood_hanging_wall_sign"),p.stacksTo(16)));
        for(var e:entries.values())if(e.kind().equals("internal")){
            NouveauCatalog.registerBlock(e,(e.source().equals("dim_boundary")?BlockBehaviour.Properties.of():BlockBehaviour.Properties.of().noCollision()).noOcclusion().strength(e.source().equals("dim_boundary")?-1:0),p->switch(e.source()){
                case "portal"->new NouveauPortals.PortalBlock(p);
                case "dim_boundary"->new NouveauPlanarium.Boundary(p);
                case "light_block","temporary_light_block"->new LightBlock(p.lightLevel(s->s.getValue(LightBlock.LEVEL)));
                case "magic_fire"->new MageFire(p.lightLevel(s->8));
                case "particle_block"->new NouveauFixtures.ParticleBlock(p);
                case "redstone_air"->new PoweredBlock(p){@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}};
                case "intangible_air"->new Block(p){@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}};
                default->new AirBlock(p);
            });
        }
        NouveauWeaves.register();NouveauPortals.register();
    }
    private static final class HangingText extends SignBlockEntity {
        HangingText(BlockPos pos,BlockState state){super(HANGING_SIGN,pos,state);}
        @Override public int getTextLineHeight(){return 9;}
        @Override public int getMaxTextLineWidth(){return 60;}
    }
    public static final class PetBed extends Block {
        public PetBed(Properties p){super(p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,5,16);}
    }
    public static final class SourceLamp extends CopperBulbBlock {
        public static final IntegerProperty BRIGHTNESS=IntegerProperty.create("brightness",0,15);
        public SourceLamp(Properties p){super(p.lightLevel(s->s.getValue(CopperBulbBlock.LIT)?s.getValue(BRIGHTNESS):0));registerDefaultState(defaultBlockState().setValue(BRIGHTNESS,15));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BRIGHTNESS);}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p,Direction side){return s.getValue(CopperBulbBlock.LIT)?s.getValue(BRIGHTNESS):0;}
    }
    private static final class MageFire extends Block {
        MageFire(Properties p){super(p);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
        @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){l.scheduleTick(p,this,100);}
        @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){l.removeBlock(p,false);}
    }
}
