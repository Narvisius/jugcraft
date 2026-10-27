# The trade look

Status: implemented (pending CI and review). Follows [the workshop, aircraft and furniture screens in the looks](new-screen-styles.md), whose PR it is stacked on (it uses its `FittedContainerScreen`).
Proposal issue: none. The owner, 26 October 2026, asked for the suggested polish items in order ("then do 2-9 in order"). The third was "trade look": one look for the shops, the bank, the Exchange and the market chest.

Owner: jimbozoomer-byte
Target milestone and tier: the town, the Exchange and the personal shops
Primary specialty and supported player role: traders, shopkeepers and anyone paying in Jugs

## Player experience

Every screen where Jugs or goods change hands now has one look: the Exchange's walnut and brass. That means:
- a walnut panel in a brass rim, with gold filigree at the corners;
- slate trays for slots, ledgers and displays;
- gold headings and sums, and cream text on the slate;
- brass plates for the key action and walnut plates for the rest.

| Screen | Before | Now |
| --- | --- | --- |
| Town shop | A wooden counter board, offers on parchment, vanilla buttons | The shop's name and your Jugs on the walnut. Buy and Sell are folder tabs, the open one in brass. The offers are on a striped slate ledger: the item, name and count in cream, the price in gold, and a brass Buy (or Sell) plate. The page plates and "page / pages" are under the ledger. A long "nothing here today" wraps. |
| Jug Teller (ATM) | The shop's wooden board, a green screen, vanilla buttons | The names are walnut plates on a slate ledger, the chosen one in brass. It is updated when the server's choice changes. The teller's display is a slate tray: your Jugs in gold, the recipient, and the amount in green once it is above 0. The step, Clear and All plates are walnut, and Send is brass. |
| Personal safe | The source mod's grey chest image | The walnut panel with the safe's 54 slots and the inventory each on slate, and its title in gold. |
| Barter station and energy vendor | The source mod's grey images | What you pay and what you get are samples on slate. The stock or charge and its trades are on a second slate, with a brass Trade (or Buy charge) key, then the stock and the inventory. The state is in green when ready, red when something is in the way and dim when private. "Cursor stack sets sample" replaces the state while the owner points at a sample. The owner's Published/Private and charge keys are walnut plates. |
| Market chest | Already walnut and brass | Unchanged, but drawn with the shared parts. At 332 wide it now scales down to fit a narrower window, with its price box, rows, scrolling and mouse. |
| Exchange and its guide | Already walnut and brass | Unchanged. Both size themselves to the window. |

Every key keeps its button id, every slot its place, and every tooltip its words.

## Connections

- `client/TradeLook`: the look's parts. It has the frame (panel, rim and filigree), slate trays, ledger stripes, brass rules and slots, and the colours, all from `ExchangeScreen`'s palette.
- `ExchangeScreen.Plate` (the brass and walnut keys) is now public, so the personal shops (in another package) use it.
- `FittedContainerScreen` (from [new-screen-styles.md](new-screen-styles.md)) now tells a screen its scale (`scale()`). The market chest uses it to scale its own scrolling test.
- The [fit check](screen-fit-check.md) opens these screens at the smallest GUI (320 × 240), on stand-in menus:
  - the shop (the General Store), the Jug Teller (with eight names, so it has two pages) and the market chest;
  - the Exchange guide;
  - the personal safe, and the barter station and energy vendor (their blocks are placed first, since their menus read them).

## Balance and automation

None. Display only.

## Multiplayer and persistence

Unchanged. Every trade is still a menu button or payload the server checks. The Jug Teller's chosen name is the server's, and the screen follows it.

## Dependencies and assets

None. The personal shops no longer draw the source images (`textures/gui/jimtafacts/personal_safe.png`, `barter_station_*.png`, `energy_vendor_*.png`), which stay in the mod.

## Verification

Client game tests that show these screens:
- `ScreenFitClientGameTests`: all of them at 320 × 240, with screenshots (`jugcraft_fit_shop`, `jugcraft_fit_atm`, `jugcraft_fit_market_chest`, `jugcraft_fit_barter_station`);
- `TownClientGameTests`: a shop and a Jug Teller opened by a player (`jugcraft_town_shop`, `jugcraft_town_atm`);
- `ExchangeClientGameTests`: the market chest (`jugcraft_exchange_market_chest`).

### Results

Local, without Gradle or a game: the Java syntax parse and the repository checks; see the PR.

CI: see the PR.

Not run:
- a human trading at each screen;
- a second player at a personal shop.

## World and event applicability

Everywhere these blocks and the town are.

## Rollout and open questions

- The casino tables (poker, blackjack) already use the walnut plates; their felt stays.
- The farm stand kept the workshop look ([bench-screens.md](bench-screens.md)): it is a farmer's crate stand, not a counter.

## History

Before this, the town shop and Jug Teller shared a wooden counter board with parchment and vanilla buttons. The personal safe and shops drew the source mod's grey images. The Exchange and market chest had walnut and brass of their own.
