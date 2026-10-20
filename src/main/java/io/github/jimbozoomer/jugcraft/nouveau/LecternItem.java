package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.*;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Inventory and held views use the same supplied geometry as the placed lectern. */
public final class LecternItem extends BlockItem implements GeoItem {
    public static java.util.function.Supplier<Object> renderer=()->null;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public LecternItem(Block block,Properties properties){super(block,properties);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar out){
        out.add(new AnimationController<LecternItem>("ledger",4,s->s.setAndContinue(RawAnimation.begin().thenLoop("ledger_float"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public Object getRenderProvider(){return renderer.get();}
}
