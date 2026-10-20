package io.github.jimbozoomer.jugcraft.nouveau;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

/** Decorative geometry has its own display-only block entity, with no phantom workshop inventory. */
public final class NouveauAnimatedDecor extends BlockEntity implements GeoBlockEntity {
    public static BlockEntityType<NouveauAnimatedDecor> TYPE;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public NouveauAnimatedDecor(BlockPos p,BlockState s){super(TYPE,p,s);}
    public NouveauCatalog.Entry entry(){return ((DisplayBlock)getBlockState().getBlock()).entry;}
    public static void register(){var blocks=new ArrayList<Block>();for(var e:NouveauCatalog.BLOCK_ENTRIES.values())if(e.kind().equals("animated_decor")){
        var b=NouveauCatalog.registerBlock(e,BlockBehaviour.Properties.of().strength(.5F).noOcclusion().lightLevel(s->8),p->new DisplayBlock(e,p));blocks.add(b);NouveauCatalog.registerItem(e,p->new NouveauWorkshopItem(e,b,p.useBlockDescriptionPrefix()));
    }TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,Jugcraft.id("nouveau_animated_decor"),FabricBlockEntityTypeBuilder.create(NouveauAnimatedDecor::new,blocks.toArray(Block[]::new)).build());}
    public static final class DisplayBlock extends BaseEntityBlock {
        final NouveauCatalog.Entry entry;DisplayBlock(NouveauCatalog.Entry e,Properties p){super(p);entry=e;}
        public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new NouveauAnimatedDecor(p,s);}
        protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    }
    public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<NouveauAnimatedDecor>("display",4,s->{String clip=NouveauPresentation.animation(entry().geo(),true);return clip.isEmpty()?com.geckolib.animation.object.PlayState.STOP:s.setAndContinue(RawAnimation.begin().thenLoop(clip));}));}
    public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
