package io.github.jimbozoomer.jugcraft.client.jimtafacts;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.*;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellbooks;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSpellCreature;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaScribing;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSummonSpells;
import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import java.util.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Exact owner-supplied armor cubes and UVs; GeckoLib follows the wearer's normal pose. */
public final class JimtaSpellbooksClient {
    private JimtaSpellbooksClient() {}
    public static void register() {
        JimtaWardClient.register();
        JimtaWizardClient.register();
        JimtaPyriumRenderer.register();
        JimtaEncounterClient.register();
        JimtaWorkshopRenderer.register();
        io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaAreaEntity.TYPES.values().forEach(t->EntityRendererRegistry.register(t,JimtaAreaRenderer::new));
        var apparitions=io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaApparition.TYPES;
        EntityRendererRegistry.register(apparitions.get("ice_spike"),c->new JimtaApparitionRenderer(c,false));
        EntityRendererRegistry.register(apparitions.get("devour_jaw"),c->new JimtaApparitionRenderer(c,true));
        EntityRendererRegistry.register(apparitions.get("frozen_copy"),JimtaFrozenRenderer::new);
        EntityRendererRegistry.register(apparitions.get("frozen_humanoid"),JimtaFrozenRenderer::new);
        EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaDungeonMobs.ZOMBIE,net.minecraft.client.renderer.entity.ZombieRenderer::new);
        EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaDungeonMobs.MAGEHUNTER,net.minecraft.client.renderer.entity.VindicatorRenderer::new);
        EntityRendererRegistry.register(apparitions.get("echoing_sword"),c->new GeoEntityRenderer<io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaApparition,EntityRenderState>(c,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/summoned_claymore");}
            @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/summoned_weapons/summoned_claymore.png");}
            @Override public Identifier getAnimationResource(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaApparition e){return Jugcraft.id("entity/jimtafacts/summoned_weapons");}
        }));
        net.minecraft.client.gui.screens.MenuScreens.register(JimtaScribing.MENU,JimtaScribingScreen::new);
        EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaShot.TYPE,JimtaShotRenderer::new);
        EntityRendererRegistry.register(JimtaSummonSpells.STEED,SteedRenderer::new);
        EntityRendererRegistry.register(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaBloodProjectile.TYPE,JimtaBloodRenderer::new);
        EntityRendererRegistry.register(JimtaSummonSpells.ZOMBIE,c->new net.minecraft.client.renderer.entity.ZombieRenderer(c){@Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ZombieRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/summoned_zombie.png");}});
        EntityRendererRegistry.register(JimtaSummonSpells.SKELETON,c->new net.minecraft.client.renderer.entity.SkeletonRenderer(c){@Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.SkeletonRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/summoned_skeleton.png");}});
        io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSummonedWeapon.TYPES.forEach((name,type)->EntityRendererRegistry.register(type,c->new GeoEntityRenderer<io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSummonedWeapon,EntityRenderState>(c,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/summoned_"+name);}
            @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/summoned_weapons/summoned_"+name+".png");}
            @Override public Identifier getAnimationResource(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaSummonedWeapon e){return Jugcraft.id("entity/jimtafacts/summoned_weapons");}
        })));
        JimtaSpellCreature.TYPES.forEach((name,type)->EntityRendererRegistry.register(type,context->new GeoEntityRenderer<JimtaSpellCreature,EntityRenderState>(context,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/"+name);}
            @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/"+(name.equals("wisp")?"wisp/wisp":name)+".png");}
            @Override public Identifier getAnimationResource(JimtaSpellCreature entity){return Jugcraft.id("entity/jimtafacts/"+name);}
        })));
        io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaConstruct.TYPES.forEach((name,type)->EntityRendererRegistry.register(type,c->new GeoEntityRenderer<io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaConstruct,EntityRenderState>(c,new GeoModel<>(){
            @Override public Identifier getModelResource(GeoRenderState s){return Jugcraft.id("entity/jimtafacts/"+name);}
            @Override public Identifier getTextureResource(GeoRenderState s){return Jugcraft.id("textures/jimtafacts/spellbooks/"+switch(name){case "spectral_hammer"->"entity/shield/shield_overlay";case "void_tentacle"->"entity/void_tentacle/void_tentacle";default->"entity/ice_block";}+".png");}
            @Override public Identifier getAnimationResource(io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaConstruct e){return Jugcraft.id("entity/jimtafacts/"+name);}
        })));
        Map<String,GeoRenderProvider> renderers=new HashMap<>();
        JimtaSpellbooks.armorRenderer=item->renderers.computeIfAbsent(item.family,family->new GeoRenderProvider() {
            final GeoArmorRenderer<JimtaSpellbooks.Armor,HumanoidRenderState> renderer=new GeoArmorRenderer<>(new GeoModel<>() {
                @Override public Identifier getModelResource(GeoRenderState state) {return Jugcraft.id("armor/jimtafacts/"+item.model);}
                @Override public Identifier getTextureResource(GeoRenderState state) {return Jugcraft.id("textures/jimtafacts/spellbooks/models/armor/"+item.texture+".png");}
                @Override public Identifier getAnimationResource(JimtaSpellbooks.Armor armor) {return Jugcraft.id("armor/jimtafacts/"+(armor.family.equals("wizard")?"wizard":"empty"));}
            });
            @Override public GeoArmorRenderer<?,?> getGeoArmorRenderer(ItemStack stack,EquipmentSlot slot) {return renderer;}
        });
    }
    private static final class SteedRenderer extends net.minecraft.client.renderer.entity.AbstractHorseRenderer<JimtaSummonSpells.SpectralSteed,net.minecraft.client.renderer.entity.state.EquineRenderState,net.minecraft.client.model.animal.equine.HorseModel>{
        SteedRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){super(context,new net.minecraft.client.model.animal.equine.HorseModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.HORSE)),new net.minecraft.client.model.animal.equine.HorseModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.HORSE_BABY)));}
        @Override public net.minecraft.client.renderer.entity.state.EquineRenderState createRenderState(){return new net.minecraft.client.renderer.entity.state.EquineRenderState();}
        @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.EquineRenderState state){return Jugcraft.id("textures/jimtafacts/spellbooks/entity/horse/spectral_steed.png");}
    }
}
