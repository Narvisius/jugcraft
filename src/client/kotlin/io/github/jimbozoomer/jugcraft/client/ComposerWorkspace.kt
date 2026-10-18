package io.github.jimbozoomer.jugcraft.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/** Compatibility entry point: both installed-framework and standalone clients use the narrated constellation editor. */
object ComposerWorkspace {
    @JvmStatic
    @JvmOverloads
    fun open(parent: Screen? = null) {
        ComposerClient.open(Minecraft.getInstance(), parent)
    }
}
