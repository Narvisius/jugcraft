package io.github.jimbozoomer.jugcraft.client.ember;

import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberArmorItem;
import java.util.List;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

/**
 * A fire set as worn: GeckoLib's armour renderer over the owner's model (assets/jugcraft/geckolib/models/armor/
 * &lt;model&gt;.geo.json) and the set's texture (textures/armor/&lt;set&gt;.png), both imported as supplied, one renderer for
 * a set's four pieces. GeckoLib poses the model's armour bones to the wearer each frame; on top of that:
 * <ul>
 * <li>the slim-armed sleeves are hidden: the model carries both widths, one over the other, and vanilla armour draws the
 * same sleeves for every wearer;</li>
 * <li>babies and small armour stands wear nothing, as Jugcraft's other modelled armour (the model is sized for an
 * adult);</li>
 * <li>an enchanted piece shows vanilla's armour glint (GeckoLib records it but draws none itself).</li>
 * </ul>
 */
public final class EmberArmorRenderer extends GeoArmorRenderer<EmberArmorItem, HumanoidRenderState> {
	/** The bones of the narrower sleeves (the owner's model has both). */
	public static final List<String> SLIM_SLEEVES = List.of("armorRightArmSlim", "armorLeftArmSlim");

	/** A renderer for {@code set}'s pieces: its texture, worn on the owner's {@code model} (GeckoLib's paths under "armor"). */
	public EmberArmorRenderer(String set, String model) {
		super(new DefaultedGeoModel<EmberArmorItem>(Jugcraft.id(set)) {
			@Override
			protected String subtype() {
				return "armor";
			}
		}.withAltModel(Jugcraft.id(model)));
		withRenderLayer(new Glint(this, Jugcraft.id("textures/armor/" + set + ".png")));
	}

	@Override
	public List<ArmorSegment> getSegmentsForSlot(HumanoidRenderState renderState, EquipmentSlot slot) {
		return renderState.isBaby ? List.of() : super.getSegmentsForSlot(renderState, slot);
	}

	/** GeckoLib poses the armour bones to the wearer here (super, first); then the slim sleeves are hidden. */
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<HumanoidRenderState> renderPassInfo, BoneSnapshots snapshots) {
		super.adjustModelBonesForRender(renderPassInfo, snapshots);
		for (String sleeve : SLIM_SLEEVES) {
			snapshots.ifPresent(sleeve, bone -> bone.skipRender(true));
		}
	}

	/** Vanilla's armour glint over an enchanted piece: the model drawn again with the glint render type. */
	private static final class Glint extends TextureLayerGeoLayer<EmberArmorItem, GeoArmorRenderer.RenderData, HumanoidRenderState> {
		Glint(GeoArmorRenderer<EmberArmorItem, HumanoidRenderState> renderer, Identifier texture) {
			super(renderer, texture, RenderTypes::armorCutoutNoCullGlint);
		}

		@Override
		protected @Nullable RenderType getRenderType(HumanoidRenderState renderState) {
			return Boolean.TRUE.equals(renderState.getGeckolibData(DataTickets.HAS_GLINT)) ? super.getRenderType(renderState) : null;
		}
	}
}
