package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Editable cuboid model from tools/styx.py, with a player-textured face, thin glasses, hat, robe, vials and open-frame staff. */
public final class StyxModel extends HumanoidModel<StyxRenderer.State> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Jugcraft.id("styxhexenhammer"),"main");
	public StyxModel(ModelPart root) { super(root); }
	public static LayerDefinition createLayer() {
		MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
		Map<String,PartDefinition> parts=new HashMap<>();
		for(String name:new String[]{"head","body","left_arm","right_arm","left_leg","right_leg"}) {
			float x=switch(name) {case "left_arm" -> 5;case "right_arm" -> -5;case "left_leg" -> 2;case "right_leg" -> -2;default -> 0;};
			float y=name.endsWith("leg") ? 12 : name.endsWith("arm") ? 2 : 0;
			parts.put(name,root.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offset(x,y,0)));
		}
		parts.put("staff",parts.get("left_arm").addOrReplaceChild("staff",CubeListBuilder.create(),PartPose.offsetAndRotation(0,9,0,0,0,0.30F)));
		parts.get("head").addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
		try(var stream=StyxModel.class.getResourceAsStream("/assets/jugcraft/styx_model.json")) {
			if(stream==null) throw new IllegalStateException("Missing Styx model");
			int n=0;
			for(var e:JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray()) {
				var item=e.getAsJsonObject();var b=item.getAsJsonArray("box");int c=item.get("color").getAsInt();
				parts.get(item.get("part").getAsString()).addOrReplaceChild("piece_"+n++,CubeListBuilder.create().texOffs((c%8)*64,(c/8)*64)
						.addBox(b.get(0).getAsFloat(),b.get(1).getAsFloat(),b.get(2).getAsFloat(),b.get(3).getAsFloat(),b.get(4).getAsFloat(),b.get(5).getAsFloat()),PartPose.ZERO);
			}
		} catch(Exception e) {throw new IllegalStateException("Could not load Styx model",e);}
		return LayerDefinition.create(mesh,512,512);
	}
	@Override public void setupAnim(StyxRenderer.State state) {
		super.setupAnim(state);
		// Keep the staff stable during walking; a small ritual lift rather than a combat swing.
		leftArm.xRot=state.ritual ? -0.55F : -0.12F;
		leftArm.zRot=-0.06F;
	}
}
