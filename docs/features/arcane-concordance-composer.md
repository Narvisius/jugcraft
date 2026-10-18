# Arcane Concordance: the Spell Composer

**16 October local UI revision:** the owner requested a constellation composer with no typing and a simple illustrated field guide. See the [current redesign record](constellation-and-field-guide.md); earlier screen descriptions below document the prior implementation.

Status: implemented for Concordance Chunk A (units 1 and 2: tickets A-01 to A-03); its tests pass in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953)
on `aa4d06c4e`). See Verification for what has run.
Proposal: the owner chose Chunk A on 10 October 2026; [the chunk's contract](concordance-chunk-a.md) holds the scope.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Practitioner and up (First Light understood). Builds on
[composition](arcane-concordance-composition.md) (the compiler and the inscription) and
[the journal](arcane-concordance-journal.md) (the screens' patterns and the J key).

## What already existed, and what this reuses

Spells could already be composed, but only by typing `/jugcraft concordance compose check|inscribe <spell>`; the
codex told players to. The compiler, the inscription item component, the cast through Spell Engine and its payment
were all there. This adds no rule and no new kind of spell: it puts a screen over the same compiler, and makes the
command and the screen one service so they cannot disagree.

## Player experience

**Opening it.** Hold a composing instrument (the Initiate's or the Adept's Wand), press **J** for the journal and
choose **Compose**. A key of its own (Controls: *Open the Spell Composer*) can be bound; it is unbound to start with.

**Writing a spell.** The composer shows the instrument and its limits, what is inscribed now, and the spell being
written:

- **As text**, in a box that takes typing: the same words as the command (`touch struck light`).
- **As words**, in order, one row for the spell and one for its branch. Any word can be chosen (click it, or reach it
  with Tab or the arrow keys); the chosen one is in brackets.
- **The words this player has learned**, by kind: deliveries, selections, operations, modifiers, endings. Choosing one
  writes it where the grammar wants it (a new delivery or selection replaces the old one). A modifier joins the chosen
  word, and is offered only for a word it can change. Words not yet learned are counted ("10 more words are yet to be
  learned"), never named. Each word's tooltip says what it does and costs.
- **Edits**: move earlier or later, remove, remove a modifier, add or remove a branch (as far as the instrument
  allows), undo, start again, and load what is inscribed.

**Being told.** A moment after editing stops, the server checks the spell and the composer shows its answer in the
command's own words: what each part does, the Focus and cooldown, the targets, work and capacity it takes; or every
problem, each once, with the word it concerns marked "!". While a newer check is on its way the composer says
"Checking...".

**Inscribing.** **Inscribe** is enabled only once the spell, exactly as it stands, was checked and works for the
instrument and rules the composer shows now. It writes the spell on the instrument, which then carries it to Spell
Engine's spell bar as before. **Wipe inscription** clears it. Opening the composer with an empty draft starts from
what the instrument holds; opening it with a draft checks that draft again (the instrument in hand may be another).
Words added with the buttons stop at the compiler's 256 characters.

**Keyboard and accessibility.**

- The text box has the keyboard when the plain composer opens.
- Tab and the arrow keys move between controls; a word of the spell reached with them is chosen.
- Alt+Left and Alt+Right move the chosen word; Backspace (outside the text box) removes it; Ctrl+Z undoes.
- Ctrl+Z works in the text box too, in either view.
- Esc goes back (to the journal, when it was opened from there), in either view.
- Nothing is said by colour alone: brackets mark the chosen word, "!" marks a problem, and each line says what it means.
- The plain composer's controls and lines are read by the narrator. The narrator also reads each new verdict and each
  write in either view.
- The plain composer scrolls and wraps, so it works at high GUI scales and in small windows.
- Reduced motion (the Concordance setting) stops the GuiLib workspace's transitions and its background blur.

**Two views, one draft.** With GuiLib installed the composer opens as a GuiLib workspace; without it, or with
**Simple journal** chosen in the settings (now "the plain journal and Spell Composer screens"), as the plain screen.
Both edit the same draft, which is kept until the player leaves the world.

## How it works

**One service** (`concordance/Composing`). `compose(player, text, inscribe, shown)` asks, in the command's order:

1. whether the Concordance is on;
2. whether an instrument is in the main hand;
3. for a screen's change, whether that instrument and the rules are still the ones the screen showed (`Held`: the rules
   revision and the held stack, compared whole; nothing is added to the item);
4. whether 20 ticks have passed since the player's last composition or tuning (the `compose` rate gate, shared by the
   command, the screen and `tune`);
5. whether the text compiles.

Only then is a valid spell written, replacing what was there. `show` and `clear` serve the command as before; the
screen's clear asks what composing asks. The command (`ConcordanceCommand`) is now an adapter that says what the service
decided, in the words it always used.

**The protocol** (`concordance/composer`). One request (`ComposerRequestPayload`: sequence, action, revision, text)
and two answers:

- `ComposerStatePayload`: the revision, the instrument's limits, the learned words (with the slot, capacity, Focus
  and what can change each) and the count of the rest, and the inscription.
- `ComposerResultPayload`: the verdict, its heading, the spell's numbers, each distinct line once (at most 40) and the
  written names the problems blame.

`Composer` keeps one session per player in memory: a revision and what the screen was last shown. An inscribe or clear
naming an older revision, or made after the instrument or rules changed, writes nothing; the screen is sent its state
again (a request naming an older revision counts against the shared gate, so this happens at most once a second). A check, inscribe or clear that comes within the rate waits for its tick, the newest replacing an older one, so
a screen checking as its player types costs at most one compilation a second. Opening has its own gate (10 ticks). The
request's text decodes to at most 1024 characters (the compiler refuses past 256); anything past that is a malformed
packet. Sessions are forgotten when the player leaves and when the server stops.

**On the client.** `ComposerClient` keeps the draft (`compose/Draft`, pure Java), the last state and answer, and sends
a check 8 ticks after editing stops (and when the composer opens, or a write is refused or wiped), dropping answers to
older requests; it promises no check on a server without the composer, and forgets everything on the client thread
when the player leaves. `ComposerScreen` is the plain screen (its body
scrolls in `ComposerScroll`); `ComposerWorkspace` (Kotlin) is the GuiLib workspace with `assets/jugcraft/ui/composer.css`,
opened only behind `isModLoaded("guilib")`. The client has no rules: everything it offers comes from the server's
state.

## Connections

- Input producer: First Light research (the words), the instrument (its limits), the compiler.
- Output consumer: the inscription on the instrument, cast through Spell Engine as before.
- Magic connection: every composable word; branches as far as the instrument allows.
- Reachable entry path: understand First Light, craft the Initiate's Wand, press J and Compose.
- Solo, trade and cooperative routes: each player composes for their own instrument.
- Mastery: none new.

## Balance

No balance change: the same compiler, limits, costs and cooldowns; the composer only writes text. Casting still pays
when the spell takes effect, as before.

## Multiplayer and persistence

- Server authority: the server decides what may be composed, checks every word, and writes the inscription; the client
  sends text only.
- Hidden information: only learned words are sent; the rest are counted.
- Rate and bounds: as above.
- Persistence: the inscription is the existing item component. Sessions and drafts are not saved; the draft is
  forgotten on leaving a world.
- Disable behaviour: with the Concordance switched off the composer says so and composes nothing.

## Dependencies and assets

No new dependency. GuiLib (optional, client, pinned 0.12.4) draws the workspace; its Kotlin loads only with GuiLib. No
art: the screens are drawn in code and CSS.

## Verification

- `python3 scripts/check_repository.py` and `python3 tools/check_mod_data.py` pass locally. The new
  `check_composer` audit checks the service's order of questions, that only `Composing` makes a composed inscription,
  that the command and the screen answer through it, the request's bound, the gated opening, the GuiLib guard, the
  stylesheet and every word the screens use.
- Game tests, passed in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953)): `ConcordanceComposeGameTests.oneServiceAnswersCommandAndScreen`,
  `theDraftWritesWhatTheGrammarWants`, `theComposerAnswersAsTheCommandDoes` and `composerMessagesSurviveTheTrip`.
- Client game test, passed in CI (run [38435014953](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38435014953)): `ConcordanceComposerClientGameTests`, on a dedicated server, with
  screenshots. Its first CI screenshot of the GuiLib workspace (run 38428501876) showed the spell box squeezed to a
  sliver with its text cut off: GuiLib's inputs clip, so the panel's column shrank it to its padding. Unit 5 fixed the
  stylesheet (the box keeps its height and the scrolling body takes what is left), and `check_composer` requires both.

Not run: the narrator heard by a person; the GuiLib workspace by keyboard by hand; two clients; a person judging the
screenshots.

## Rollout and open questions

New identifiers: the payloads `jugcraft:concordance_composer_request`, `jugcraft:concordance_composer_state` and
`jugcraft:concordance_composer_result`; the key `key.jugcraft.concordance_composer`. The codex page Composing Spells now
points at the composer, with the commands on a page of their own. Nothing new is saved in worlds.

Known limits:

- A problem names the words it concerns, not their place, so a word written twice is marked twice.
- The command's tab suggestions list every word in the rules (see the chunk's decisions).
- The GuiLib workspace has no narration of its own (GuiLib has none); the plain screen does.
