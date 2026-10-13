package io.github.jimbozoomer.jugcraft.client

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.event.KeyboardEvent
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.text

/**
 * The Concordance Journal as a GuiLib workspace (roadmap step 26): the same journal [JournalScreen] shows, with a tab
 * per section, a scrolling page of its lines, the exact-values switch and a refresh button. Keyboard: Tab moves between
 * the controls, Left and Right change tab, Page Up, Page Down, Home and End scroll, R asks the server again, X shows or
 * hides the exact figures, Escape closes. Reduced motion (the Concordance setting) stops the tab indicator sliding.
 * Loaded only when GuiLib is installed ([JournalClient] checks); it shows only what the server sent.
 */
object JournalWorkspace {
    private val App = component("ConcordanceJournal") {
        var sections by useState(JournalClient.sections())
        var received by useState(JournalClient.received())
        var exact by useState(ConcordanceClientOptions.exactValues())
        var selected by useState("")
        useEffect {
            val listener = Runnable {
                sections = JournalClient.sections()
                received = true
            }
            JournalClient.listen(listener)
            onCleanup { JournalClient.unlisten(listener) }
        }
        useBodyClass("reduced-motion", ConcordanceClientOptions.reducedMotion())
        useDocumentEvent("keydown") { event ->
            val key = (event as? KeyboardEvent)?.key
            if (key == "r" || key == "R") {
                JournalClient.request()
            } else if (key == "x" || key == "X") {
                exact = !exact
                ConcordanceClientOptions.setExactValues(exact)
            }
        }
        val current = sections.firstOrNull { it.id() == selected } ?: sections.firstOrNull()
        div(className = "journal") {
            div(className = "header") {
                h2 { text(Component.translatable("screen.jugcraft.journal.title")) }
                div(className = "actions") {
                    switch(checked = exact, onChange = {
                        exact = it.checked
                        ConcordanceClientOptions.setExactValues(it.checked)
                    }, label = Component.translatable("screen.jugcraft.concordance.config.exact").string)
                    button(className = "refresh", onClick = { JournalClient.request() }) {
                        text(Component.translatable("screen.jugcraft.journal.refresh"))
                    }
                }
            }
            if (!received) {
                p(className = "status") { text(Component.translatable("screen.jugcraft.journal.loading")) }
            } else if (current == null) {
                p(className = "status") { text(Component.translatable("screen.jugcraft.journal.empty")) }
            } else {
                tabs(value = current.id(), onChange = { selected = it }, variant = "pills", className = "sections") {
                    for (section in sections) {
                        tab(section.id(), section.title().string)
                    }
                }
                scroll(className = "lines", key = current.id()) {
                    for ((index, line) in current.lines().withIndex()) {
                        div(className = "line", key = index) {
                            text(line.text())
                            if (exact && line.exact().isPresent) {
                                text(line.exact().get().copy().withStyle(ChatFormatting.GRAY), className = "exact")
                            }
                        }
                    }
                }
            }
            p(className = "help") { text(Component.translatable("screen.jugcraft.journal.help")) }
        }
    }

    /** Opens the workspace (Java calls this only when GuiLib is installed). */
    @JvmStatic
    fun open() {
        GuiLib.open(App, stylesheets = listOf("jugcraft:ui/journal.css"),
            title = Component.translatable("screen.jugcraft.journal.title").string,
            blurBackground = !ConcordanceClientOptions.reducedMotion())
    }
}
