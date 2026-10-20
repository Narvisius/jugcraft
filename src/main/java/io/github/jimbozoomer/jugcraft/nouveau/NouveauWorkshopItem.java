package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Function;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public final class NouveauWorkshopItem extends BlockItem implements GeoItem {
    public static Function<NouveauCatalog.Entry,Object> renderer=e->null;
    public final NouveauCatalog.Entry entry;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public NouveauWorkshopItem(NouveauCatalog.Entry e,Block b,Properties p){super(b,p);entry=e;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public Object getRenderProvider(){return renderer.apply(entry);}
}
