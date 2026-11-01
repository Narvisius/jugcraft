package io.github.jimbozoomer.jugcraft.mixin.client;
import io.github.jimbozoomer.jugcraft.client.OwnerChatAnimation;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.*;
@Mixin(GuiMessage.Line.class)
public abstract class OwnerChatLineMixin implements OwnerChatAnimation.Timestamp {
 @Unique private final long jugcraft$created=OwnerChatAnimation.now();
 @Override public long jugcraft$created(){return jugcraft$created;}
}
