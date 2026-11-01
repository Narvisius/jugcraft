package io.github.jimbozoomer.jugcraft.mixin.client;
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.gui.screens.inventory.BookEditScreen.class)
public interface OwnerLecternPageAccess {
    @org.spongepowered.asm.mixin.gen.Accessor("currentPage") int jugcraft$currentPage();
}
