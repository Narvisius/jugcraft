package io.github.jimbozoomer.jugcraft.nouveau;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Enchanter equipment retains the native weapon action; sneak-use opens the shared spell editor. */
public final class NouveauInstruments {
    public static Function<String,Object> renderer=name->null;
    private static final Map<Visual,AnimatableInstanceCache> CACHES=new IdentityHashMap<>();
    private NouveauInstruments(){}
    public interface Visual extends GeoItem {
        String source();
        @Override default AnimatableInstanceCache getAnimatableInstanceCache(){return CACHES.computeIfAbsent(this,GeckoLibUtil::createInstanceCache);}
        @Override default void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
        @Override default Object getRenderProvider(){return renderer.apply(source());}
    }
    public static Item create(NouveauCatalog.Entry e,Item.Properties p){
        return switch(e.source()){
            case "spell_bow"->new Bow(p.durability(700).enchantable(18));
            case "spell_crossbow"->new Crossbow(p.durability(700).enchantable(18));
            case "enchanters_fishing_rod"->new Rod(p.durability(512).enchantable(18));
            case "enchanters_shield"->new Shield(p.durability(700).delayedComponent(DataComponents.BLOCKS_ATTACKS, registries -> new net.minecraft.world.item.component.BlocksAttacks(0.25F,1.0F,List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F,Optional.empty(),0.0F,1.0F)),new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F,1.0F,1.0F),Optional.of(registries.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK))));
            case "enchanters_gauntlet"->new NouveauGear.Gear(e,p.tool(ToolMaterial.DIAMOND,net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,io.github.jimbozoomer.jugcraft.Jugcraft.id("nouveau/gauntlet_mineable")),3,-2.4F,0));
            case "enchanters_sword"->new NouveauGear.Gear(e,p.sword(ToolMaterial.DIAMOND,3,-2.4F));
            default->new NouveauGear.Gear(e,p.stacksTo(1));
        };
    }
    private static InteractionResult edit(Level l,Player p,InteractionHand hand){return p instanceof ServerPlayer server?NouveauGear.cast(server,p.getItemInHand(hand)):InteractionResult.SUCCESS;}
    private static final class Bow extends BowItem implements Visual {
        Bow(Properties p){super(p);}public String source(){return "spell_bow";}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){return p.isShiftKeyDown()?edit(l,p,hand):super.use(l,p,hand);}
    }
    private static final class Crossbow extends CrossbowItem implements Visual {
        Crossbow(Properties p){super(p);}public String source(){return "spell_crossbow";}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){return p.isShiftKeyDown()?edit(l,p,hand):super.use(l,p,hand);}
    }
    private static final class Rod extends FishingRodItem implements Visual {
        Rod(Properties p){super(p);}public String source(){return "enchanters_fishing_rod";}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){
            if(p.isShiftKeyDown())return edit(l,p,hand);
            if(p instanceof ServerPlayer server&&p.fishing!=null&&p.fishing.getHookedIn() instanceof LivingEntity target){return NouveauEquipment.castAt(server,p.getItemInHand(hand),NouveauEquipment.impact(server,target),1)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
            return super.use(l,p,hand);
        }
    }
    private static final class Shield extends ShieldItem implements Visual {
        Shield(Properties p){super(p);}public String source(){return "enchanters_shield";}
        @Override public InteractionResult use(Level l,Player p,InteractionHand hand){return p.isShiftKeyDown()?edit(l,p,hand):super.use(l,p,hand);}
    }
}
