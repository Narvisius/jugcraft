package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.backpack.*;
import eu.pb4.trinkets.api.client.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws the original tier model and dyed textures, attached to the animated body. */
public final class OwnerBackpackWearable {
    private static ItemModelResolver resolver;
    private OwnerBackpackWearable(){}
    public static void register(){
        var key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.owner_backpack",InputConstants.UNKNOWN.getValue(),PartyClient.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.consumeClick())if(client.player!=null&&client.gui.screen()==null)ClientPlayNetworking.send(new OwnerBackpackOpen());});
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen))return;
            var button=net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Pack"),b->{
                if(client.player!=null){client.player.closeContainer();ClientPlayNetworking.send(new OwnerBackpackOpen());}
            }).bounds((width-176)/2+130,(height-166)/2-21,40,18).build();
            net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(button);
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.beforeExtract(screen).register((shown,g,mx,my,delta)->{
                button.visible=client.player!=null&&!OwnerBackpacks.carried(client.player).isEmpty();button.active=button.visible;
            });
        });
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type,renderer,helper,context)->resolver=context.getItemModelResolver());
        for(String tier:OwnerBackpacks.TIERS)TrinketRendererRegistry.registerRenderer(OwnerBackpacks.ITEMS.get(tier),context->(stack,access,source,pose,collector,light,state,yaw,pitch)->{
            var wearer=access.inventory().getAttachment().getEntity();
            if(resolver==null||!(source instanceof HumanoidModel<?> model)||state.isInvisible||wearer.isSpectator()||io.github.jimbozoomer.jugcraft.concordance.trinket.WornDisplay.hidden(wearer))return;
            var render=new ItemStackRenderState();resolver.updateForLiving(render,stack,ItemDisplayContext.NONE,wearer);
            if(render.isEmpty())return;var bounds=render.getModelBoundingBox();
            pose.pushPose();model.body.translateAndRotate(pose);pose.translate(0,0,2F/16);pose.rotateDegrees(Axis.XP,180);pose.scale(.99F,.99F,.99F);
            // The supplied straps are on the model's +Z side; align that side against the player's back.
            pose.translate(-(bounds.minX+bounds.maxX)*.5,-bounds.maxY,-bounds.maxZ);
            render.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,state.outlineColor);pose.popPose();
        });
    }
}
