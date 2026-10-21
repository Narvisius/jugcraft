# Exchange integration, October 20

Owner-selected Group G: PR #416, integrated against main after #445.
Integration and additional regression work: OpenAI Codex (GPT-6). Original implementation
and asset provenance remain in [finance-exchange.md](finance-exchange.md).

## Combined scope

The existing Jugs wallet backs banking, payment contracts, player/company commodity
listings, buy requests, collection, land ownership/auctions, corporations and share
trading. Market Chests list company goods. Poker tables and the casino suite are
included in the candidate, with the supplied models, textures, screens and guide.
No new dependency or world generation is introduced.

## Repairs

- Preserve all current main tests, materials, mineable tags and generated resources.
- Exchange defaults to semicolon, keeping N for the backpack and B for the encyclopedia.
- Purchases that do not fit remain in saved collection; collecting twice cannot duplicate goods.
- Buy requests require the requested default item components rather than erasing the
  data of enchanted, damaged or otherwise customized items offered in fulfillment.
- Payments/refunds above the town wallet cap remain the recipient's saved Exchange
  credit, included in displayed balances and spendable through Exchange actions.
  They are never silently donated to the central bank. Town purchases still use the
  capped town wallet; overflow credit is available through the Exchange.
- Reject negative escrow releases, overflow in wallet capacity checks and remote
  table/chest requests before looking up their chunks. Spectator/dead-player packets
  cannot trade. Rate-limited packets no longer trigger another full snapshot.
- Table placement checks land permission for every occupied part. The land office
  checks every town-boundary column before selling a chunk.

- Market Chests save their placing owner immediately, including before first opening, and
  retain the old company while its listed goods cannot all be returned.
- Casino funding shortfalls become saved, automatically retried prize claims instead of
  silently reducing a winner's entitlement. Houses with unpaid prizes cannot accept new
  covered bets. Claims are liabilities, not spendable money; payout waits for funding.

- Scratch cards issued by a kiosk carry a saved, single-use paid ticket ID. Alchemy
  blanks and cloned ticket data cannot generate winnings; ordinary item conversion stays available.

- Diorama's minimap is suppressed only while an Exchange screen is open, preventing
  it from covering the Market Chest prices. Closing the screen restores normal map rendering
  without changing the player's map preferences; Diorama remains optional.

- Give the welcome guide only to a client that advertises the Exchange channel, avoiding
  inventory injection into automation/mock players. Dream fixtures define their inventories
  explicitly and also check that a real Exchange guide survives escrow in its exact slot.

- The full combined run exposed a pre-existing fairy-ring tick scan loading distant
  chunks around old test players. Two local server stack captures confirmed the scan
  blocking in chunk generation/loading. Counting and sprouting now inspect loaded
  terrain only; a regression checks that neither operation loads its distant probe area.

## Verification

Local combined common/client/GameTest compilation passed. All 13 new Exchange server
regressions passed. All eight dream/curse regressions passed with an explicit guide
escrow fixture. There were also 160 Python checks and 11 World Designer tests.
The six survival recipe/registry tests also passed; the live registry and acquisition
audit include the purchased Market Chest and paid tickets. The generated material/model audit passed. Full GitHub server configurations,
rendered client screenshots and installer validation are still required before merge.
New regressions cover market collection with a full inventory and save/load, duplicate
purchase/collection attempts, requests and auction refunds, wallet overflow persistence,
corporate ownership, claims and forged distant requests, side pots, repeated game
cancellations and every slot variant's money conservation. The client suite opens the
Exchange tabs and guide, renders the casino/poker models and screens over a dedicated
server connection, and checks reconnect and key conflicts.

These are automated checks. No human playtest, two-account multiplayer certification,
production-save migration or representative player-load test is claimed. The owner's
local Modrinth installation and worlds are untouched.
