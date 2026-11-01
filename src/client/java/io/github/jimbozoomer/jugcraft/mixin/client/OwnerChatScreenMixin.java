/** Native adaptation of the owner-supplied chat animation reference; transport and tags remain vanilla. */
package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.OwnerChatAnimation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChatScreen.class})
public class OwnerChatScreenMixin {
    @Unique
    private boolean wasOpenedLastFrame = false;
    @Unique
    private long lastOpenTime = 0L;

    @Unique
    private float calculateDisplacement() {
        if (!OwnerChatAnimation.input()) {
            return 0.0f;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !this.wasOpenedLastFrame && !client.player.isSleeping()) {
            this.wasOpenedLastFrame = true;
            this.lastOpenTime = OwnerChatAnimation.now();
        }
        float FADE_TIME = OwnerChatAnimation.inputMillis();
        float FADE_OFFSET = 8.0f;
        float screenFactor = (float)client.getWindow().getHeight() / 1080.0f;
        float timeSinceOpen = Math.min((float)(OwnerChatAnimation.now() - this.lastOpenTime), FADE_TIME);
        float alpha = 1.0f - timeSinceOpen / FADE_TIME;
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float modifiedAlpha = c3 * alpha * alpha * alpha - c1 * alpha * alpha;
        return modifiedAlpha * FADE_OFFSET * screenFactor;
    }

    @Inject(method={"removed"}, at={@At(value="HEAD")})
    private void onClosed(CallbackInfo ci) {
        this.wasOpenedLastFrame = false;
    }

    @WrapOperation(method={"extractRenderState"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V")})
    private void wrapBackgroundFill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int col, Operation<Void> original) {
        OwnerChatAnimation.wrap(graphics, this.calculateDisplacement(), () -> original.call(new Object[]{graphics, x0, y0, x1, y1, col}));
    }

    @WrapOperation(method={"extractRenderState"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")})
    private void wrapScreenRender(ChatScreen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, Operation<Void> original) {
        OwnerChatAnimation.wrap(graphics, this.calculateDisplacement(), () -> original.call(new Object[]{instance, graphics, mouseX, mouseY, Float.valueOf(delta)}));
    }
}
