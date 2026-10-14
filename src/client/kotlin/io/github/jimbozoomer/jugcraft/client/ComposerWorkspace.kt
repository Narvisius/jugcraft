package io.github.jimbozoomer.jugcraft.client

import io.github.jimbozoomer.jugcraft.concordance.compose.Composition
import io.github.jimbozoomer.jugcraft.concordance.compose.Draft
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.core.event.KeyboardEvent
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.text

/**
 * The Spell Composer as a GuiLib workspace (Chunk A, A-03): the same draft, words and answers as [ComposerScreen]
 * (both edit [ComposerClient]'s one draft), laid out as a compact spell strip over the words this player has learned.
 * Keyboard: Tab moves between controls, Enter or Space presses one, Alt+Left and Alt+Right move the chosen word,
 * Backspace removes it (outside the text box), Ctrl+Z undoes (in the text box too), Escape goes back to the journal when
 * it was opened from there and otherwise closes. The chosen word is in brackets and a word a
 * problem names is marked "!", so nothing relies on colour; reduced motion (the Concordance setting) stops transitions.
 * GuiLib has no narration, so the narrator reads only the answers here ([ComposerClient]); the plain screen reads every
 * control. Loaded only when GuiLib is installed ([ComposerClient] checks); it only edits the draft and shows what the
 * server sent.
 */
object ComposerWorkspace {
    private val PALETTE = listOf(Slot.DELIVERY, Slot.SELECTION, Slot.OPERATION, Slot.MODIFIER, Slot.TERMINATION)
    /** The screen Back returns to (the journal, when opened from there); with none, the workspace closes to the game. */
    private var parent: Screen? = null

    private val App = component("ConcordanceComposer") {
        var version by useState(0)
        var box by useState(ComposerClient.draft().text())
        useEffect {
            val listener = Runnable { version += 1 }
            ComposerClient.listen(listener)
            onCleanup { ComposerClient.unlisten(listener) }
        }
        // The box follows the draft after a button's edit, an undo or a loaded inscription; not while it is typed in.
        useEffect(version) {
            val shown = Draft()
            shown.read(box, false)
            if (shown.text() != ComposerClient.draft().text()) {
                box = ComposerClient.draft().text()
            }
        }
        useBodyClass("reduced-motion", ConcordanceClientOptions.reducedMotion())
        val document = useDocument()
        val back = parent
        useEscapeBack(back != null) { Minecraft.getInstance().gui.setScreen(back) }
        useDocumentEvent("keydown") { event ->
            val key = event as? KeyboardEvent ?: return@useDocumentEvent
            val draft = ComposerClient.draft()
            // Ctrl+Z (Cmd+Z on macOS) undoes everywhere, the spell box included: GuiLib's input has no undo of its own.
            if ((key.ctrlKey || key.modifiers.meta) && !key.shiftKey && !key.altKey && key.key.equals("z", ignoreCase = true)) {
                key.preventDefault()
                if (draft.undo()) {
                    ComposerClient.edited()
                }
                return@useDocumentEvent
            }
            if (document.focusedElement?.tagName == "input") {
                return@useDocumentEvent
            }
            val changed = when {
                key.altKey && key.key == "ArrowLeft" -> draft.move(-1)
                key.altKey && key.key == "ArrowRight" -> draft.move(1)
                key.key == "Backspace" && draft.chosen() != null -> draft.remove()
                else -> return@useDocumentEvent
            }
            key.preventDefault()
            if (changed) {
                ComposerClient.edited()
            }
        }
        val draft = ComposerClient.draft()
        val state = ComposerClient.state()
        div(className = "composer") {
            h2 { text(Component.translatable("screen.jugcraft.composer.title")) }
            p(className = "status") { text(status(state)) }
            if (state != null && state.status() == ComposerStatePayload.READY) {
                p(className = "status") {
                    text(if (state.inscribed().isEmpty()) Component.translatable("compose.jugcraft.nothing_inscribed")
                    else Component.translatable("screen.jugcraft.composer.inscribed", state.inscribed()))
                }
            }
            input(className = "spell", value = box, maxLength = Grammar.MAX_TEXT,
                placeholder = Component.translatable("screen.jugcraft.composer.hint").string,
                onChange = {
                    box = it.value
                    draft.read(it.value, true)
                    ComposerClient.edited()
                })
            scroll(className = "body") {
                strip(draft)
                edits(draft, state)
                palette(draft, state)
                report(draft)
                p(className = "help") { text(Component.translatable("screen.jugcraft.composer.help_workspace")) }
            }
            div(className = "footer") {
                button(className = "primary", disabled = !ComposerClient.inscribable(), onClick = { ComposerClient.inscribe() }) {
                    text(Component.translatable("screen.jugcraft.composer.inscribe"))
                }
                button(disabled = !ComposerClient.wipeable(), onClick = { ComposerClient.wipe() }) {
                    text(Component.translatable("screen.jugcraft.composer.wipe"))
                }
                button(onClick = { Minecraft.getInstance().gui.setScreen(back) }) {
                    text(Component.translatable(if (back == null) "gui.done" else "gui.back"))
                }
            }
        }
    }

    /** The instrument and its limits, or why there is nothing to compose for. */
    private fun status(state: ComposerStatePayload?): Component = when {
        !ComposerClient.available() -> Component.translatable("screen.jugcraft.composer.unavailable")
        state == null -> Component.translatable("screen.jugcraft.composer.opening")
        state.status() == ComposerStatePayload.DISABLED -> Component.translatable("message.jugcraft.concordance.disabled")
        state.status() != ComposerStatePayload.READY || state.instrument().isEmpty ->
            Component.translatable("compose.jugcraft.problem.no_instrument")
        else -> {
            val instrument = state.instrument().get()
            val held: Component = Minecraft.getInstance().player?.mainHandItem?.hoverName ?: Component.empty()
            Component.translatable("screen.jugcraft.composer.instrument", held, ComposerClient.capacity(), instrument.capacity(),
                instrument.targets(), instrument.work(), instrument.branches())
        }
    }

    /** The spell's words in order, one row per level; any can be chosen. */
    private fun NodeBuilder.strip(draft: Draft) {
        if (draft.typed()) {
            p(className = "hint") { text(Component.translatable("screen.jugcraft.composer.typed")) }
            return
        }
        val levels = draft.levels()
        if (levels.size == 1 && levels[0].isEmpty()) {
            p(className = "hint") { text(Component.translatable("screen.jugcraft.composer.empty")) }
            return
        }
        val blamed = ComposerClient.result()?.report()?.blamed()?.toSet() ?: emptySet()
        for ((i, parts) in levels.withIndex()) {
            div(className = "level", key = "level$i") {
                span(className = "label") {
                    text(Component.translatable(if (i == 0) "screen.jugcraft.composer.spell" else "screen.jugcraft.composer.then"))
                }
                for ((j, part) in parts.withIndex()) {
                    val chosen = draft.level() == i && draft.index() == j
                    val wrong = blamed.contains(Composition.shortName(part.id()))
                        || part.modifiers().any { blamed.contains(Composition.shortName(it)) }
                    button(className = classNames("part", "chosen" to chosen, "wrong" to wrong), key = "part$i:$j", title = describe(part),
                        onClick = {
                            if (chosen) draft.choose(-1, -1) else draft.choose(i, j)
                            ComposerClient.changed()
                        }) {
                        +((if (wrong) "! " else "") + (if (chosen) "[${part.written()}]" else part.written()))
                    }
                }
            }
        }
    }

    /** The edits, each enabled only when it can do something. */
    private fun NodeBuilder.edits(draft: Draft, state: ComposerStatePayload?) {
        val chosen = draft.chosen()
        val branches = state?.instrument()?.orElse(null)?.branches() ?: 0
        val size = draft.levels()[draft.level()].size
        // Keyed, so it keeps its place (and the keyboard) when a branch is added or removed before it.
        div(className = "edits", key = "edits") {
            edit("screen.jugcraft.composer.earlier", chosen != null && draft.index() > 0) { draft.move(-1) }
            edit("screen.jugcraft.composer.later", chosen != null && draft.index() < size - 1) { draft.move(1) }
            edit("screen.jugcraft.composer.remove", chosen != null) { draft.remove() }
            edit("screen.jugcraft.composer.unjoin", chosen != null && chosen.modifiers().isNotEmpty()) { draft.unjoin() }
            edit("screen.jugcraft.composer.branch", draft.canBranch() && draft.levels().size - 1 < branches) { draft.branch() }
            edit("screen.jugcraft.composer.unbranch", draft.levels().size > 1) { draft.unbranch() }
            edit("screen.jugcraft.composer.undo", draft.canUndo()) { draft.undo() }
            edit("screen.jugcraft.composer.restart", !draft.isEmpty || draft.levels().size > 1) { draft.clear() }
            edit("screen.jugcraft.composer.load", state != null && state.inscribed().isNotEmpty()) {
                ComposerClient.load()
                false
            }
        }
    }

    private fun NodeBuilder.edit(key: String, enabled: Boolean, change: () -> Boolean) {
        button(className = "edit", key = key, disabled = !enabled, onClick = {
            if (change()) {
                ComposerClient.edited()
            }
        }) {
            text(Component.translatable(key))
        }
    }

    /** Every word the player has learned, by slot; a modifier only for a chosen word it can change. */
    private fun NodeBuilder.palette(draft: Draft, state: ComposerStatePayload?) {
        if (state == null || state.status() != ComposerStatePayload.READY) {
            return
        }
        val chosen = draft.chosen()
        val changes = chosen?.let { ComposerClient.word(it.id())?.aspects() } ?: 0
        for (slot in PALETTE) {
            val words = state.words().filter { ComposerClient.slot(it) == slot }
            if (words.isEmpty()) {
                continue
            }
            div(className = "slot", key = slot.id()) {
                span(className = "label") { text(Component.translatable("screen.jugcraft.composer.slot." + slot.id())) }
                for (word in words) {
                    val usable = slot != Slot.MODIFIER || (chosen != null && (changes and word.aspects()) != 0
                        && !chosen.modifiers().contains(word.id()) && chosen.modifiers().size < Grammar.MAX_MODIFIERS)
                    button(className = "word", key = word.id(), title = describe(word.id()), disabled = !usable, onClick = {
                        if (draft.add(word.id(), slot) { ComposerClient.slotOf(it) }) {
                            ComposerClient.edited()
                        }
                    }) {
                        +Composition.shortName(word.id())
                    }
                }
            }
        }
        if (state.locked() > 0) {
            p(className = "hint") { text(Component.translatable("screen.jugcraft.composer.locked", state.locked())) }
        }
    }

    /** What the server's last check said (and whether a newer one is on its way). */
    private fun NodeBuilder.report(draft: Draft) {
        if (draft.isEmpty) {
            return
        }
        if (ComposerClient.writing()) {
            p(className = "busy") { text(Component.translatable("screen.jugcraft.composer.writing")) }
        } else if (ComposerClient.checking()) {
            p(className = "busy") { text(Component.translatable("screen.jugcraft.composer.checking")) }
        }
        val result = ComposerClient.result() ?: return
        div(className = "report") {
            p(className = "heading") { text(result.heading()) }
            for ((index, line) in result.report().lines().withIndex()) {
                div(className = "line", key = index) { text(line) }
            }
        }
    }

    /** A part's words, for its tooltip: each one's name, what it does and what it costs. */
    private fun describe(part: Draft.Part): String =
        (listOf(part.id()) + part.modifiers()).joinToString("\n") { describe(it) }

    private fun describe(id: String): String {
        val parsed = Identifier.tryParse(id)
        val key = if (parsed == null) "component.jugcraft.$id" else "component.${parsed.namespace}.${parsed.path.replace('/', '.')}"
        val word = ComposerClient.word(id)
        val cost = if (word == null) "" else " " + Component.translatable("screen.jugcraft.composer.word_cost", word.capacity(), word.focus()).string
        return Component.translatableWithFallback(key, Composition.shortName(id)).string + ": " +
            Component.translatableWithFallback("$key.description", "").string + cost
    }

    /** Opens the workspace; [parent] is the screen Back returns to, if any (Java calls this only when GuiLib is installed). */
    @JvmStatic
    @JvmOverloads
    fun open(parent: Screen? = null) {
        this.parent = parent
        GuiLib.open(App, stylesheets = listOf("jugcraft:ui/composer.css"),
            title = Component.translatable("screen.jugcraft.composer.title").string,
            blurBackground = !ConcordanceClientOptions.reducedMotion())
    }
}
