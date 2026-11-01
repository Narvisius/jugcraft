package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.OwnerLecternEditScreen;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class OwnerLecternBookEditMixin {
    @Shadow @Final private List<String> pages;
    @Inject(method="saveChanges",at=@At("HEAD"),cancellable=true)
    private void jugcraft$save(CallbackInfo ci){if((Object)this instanceof OwnerLecternEditScreen screen){screen.saveLectern(pages,Optional.empty());ci.cancel();}}
}
