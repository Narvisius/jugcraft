package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Raider infantry's bodies (batch 57): the townsfolk's player-shaped boxes, outer layer included
 * ({@link TownsfolkModel#createLayer}), so helmets, goggles, respirators and coats (tools/raiders.py) stand out in relief.
 */
public class RaiderModel extends HumanoidModel<RaiderRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Jugcraft.id("raider"), "main");

	public RaiderModel(ModelPart root) {
		super(root);
	}
}
