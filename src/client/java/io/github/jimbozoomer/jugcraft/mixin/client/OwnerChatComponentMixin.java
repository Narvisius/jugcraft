/** Native adaptation of the owner-supplied chat animation reference; transport and tags remain vanilla. */
package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.OwnerChatAnimation;



import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChatComponent.class})
public class OwnerChatComponentMixin {
    @Shadow
    private int chatScrollbarPos;
    @Unique
    private long lastMessageTime = 0L;

    @Shadow
    private int getLineHeight() {
        return 0;
    }

    @Unique
    private float calculateDisplacement() {
        if (!OwnerChatAnimation.messages() || this.chatScrollbarPos != 0) {
            return 0.0f;
        }
        float fadeTime = OwnerChatAnimation.messageMillis();
        int lineHeight = this.getLineHeight();
        float fadeOffsetYScale = 0.8f;
        float maxDisplacement = (float)lineHeight * fadeOffsetYScale;
        long lifetime = OwnerChatAnimation.now() - this.lastMessageTime;
        float alpha = Math.min((float)lifetime / fadeTime, 1.0f);
        return maxDisplacement - alpha * maxDisplacement;
    }

    @Inject(method={"addMessage"}, at={@At(value="TAIL")})
    private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        this.lastMessageTime = OwnerChatAnimation.now();
    }

    @WrapOperation(method={"extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V")})
    private void wrapRender(ChatComponent instance, ChatComponent.ChatGraphicsAccess queueMessage, int restrictedMessageWidth, int restrictedMessage, ChatComponent.DisplayMode alpha, Operation<Void> original, @Local(argsOnly=true) GuiGraphicsExtractor graphics) {
        OwnerChatAnimation.wrap(graphics, this.calculateDisplacement(), () -> original.call(new Object[]{instance, queueMessage, restrictedMessageWidth, restrictedMessage, alpha}));
    }

    @ModifyVariable(method={"forEachLine"}, at=@At(value="STORE"), ordinal=0)
    private float modifyChatOpacity(float original, @Local GuiMessage.Line line) {
        float age = OwnerChatAnimation.age(line);
        float alpha = original * (float)OwnerChatAnimation.getOpacityFactor(age);
        return OwnerChatAnimation.messages()?(float)Math.sin(alpha*Math.PI/2):original;
    }
}
