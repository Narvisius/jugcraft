# The blueprint table fits a small window

Status: implemented (pending CI and review). Follows [the screen fit check](screen-fit-check.md) and [the benches in the workshop look](bench-screens.md), whose open question it answers.
Proposal issue: none. The owner, 26 October 2026, asked for the suggested polish items in order ("then do 2-9 in order"). The second was "blueprint table fit": the table's drafting board was the one screen left too big for the smallest GUI.

Owner: jimbozoomer-byte
Target milestone and tier: the blueprint table, from the start
Primary specialty and supported player role: builders who print and import blueprints

## Player experience

The blueprint table's board is 410 or 440 wide and 250 high. That is wider than a 4:3 window's 320 GUI pixels at the smallest scale, and taller than any window's 240. Before, it ran off the edges there: the title, tabs and the foot of the sheet were cut off.

Now, in a window too small for it, the whole board scales down to fit, with a 4-pixel margin. That includes the tabs, the drawing register, the turning preview, the bill of materials, the stamps and the IMPORT paste box. Everything works as at full size: clicking tabs, rows and stamps, scrolling the register, typing and selecting in the paste box. A material's tooltip appears at the mouse. The board is wide (with the tabs' full names) whenever the scaled window has room for it. Where it fits, nothing changes.

Its look is unchanged: a blueprint sheet clipped to a steel drawing board.

## Connections

- `BlueprintTableScreen` scales its board (`scale`, chosen in `init`) and the mouse (clicks, releases, drags, scrolls and moves) to match. It implements `ScreenKit.Fitted`.
- The [fit check](screen-fit-check.md) (`ScreenFitClientGameTests`) now opens it at the smallest GUI and photographs it (`jugcraft_fit_blueprint_table`).

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged: the same payloads, library and local import history.

## Dependencies and assets

None.

## Verification

Client game tests that show the table:
- `ScreenFitClientGameTests`: the table at 320 × 240;
- `BlueprintClientGameTests`: its library and an IMPORT of a pasted blueprint, at the test window's size.

### Results

Local, without Gradle or a game: the Java syntax parse and the repository checks; see the PR.

CI: see the PR.

Not run: a human typing an import into the scaled paste box.

## World and event applicability

Everywhere.

## Rollout and open questions

- The scaled board's text is small at the smallest GUI. A larger GUI scale, or a bigger window, gives it full size.

## History

Before this, the board was drawn at full size, centred, whatever the window, and ran off a small one.
