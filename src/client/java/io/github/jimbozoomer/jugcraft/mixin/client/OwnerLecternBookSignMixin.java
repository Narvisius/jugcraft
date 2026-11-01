package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.OwnerLecternEditScreen;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookSignScreen.class)
public abstract class OwnerLecternBookSignMixin {
    @Shadow @Final private BookEditScreen bookEditScreen;
    @Shadow @Final private List<String> pages;
    @Shadow private String titleValue;
    @Inject(method="saveChanges",at=@At("HEAD"),cancellable=true)
    private void jugcraft$sign(CallbackInfo ci){if(bookEditScreen instanceof OwnerLecternEditScreen screen){screen.saveLectern(pages,Optional.of(titleValue.trim()));ci.cancel();}}
}
