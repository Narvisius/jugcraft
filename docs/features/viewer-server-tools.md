# Viewer server tools

Owner-requested October 21 coordinated update batch. Implemented locally with
OpenAI Codex (GPT-6); not deployed or promoted through the client updater.

## Tree cutting

Crouch while breaking the bottom log with an ordinary axe to fell a rooted tree.
Normal individual chopping stays available without crouching. Every extra log
uses Minecraft's player break path: loot, enchantments, durability, claims, town
protection and other block-break protection callbacks still apply. Switching the
held tool, death, logout, dimension changes or moving away cancels the job.

Discovery requires dirt-family soil and a non-persistent leaf canopy. It follows
only matching logs, up to 256 logs, 10 blocks horizontally and 48 vertically; an
oversized or incompletely loaded tree is left for ordinary chopping. It never
loads chunks. Work is limited to 16 extra logs per player and 64 server-wide per
tick. Powered and alchemical tools retain their existing special modes. The
soil/canopy heuristic is not historical proof that every log was naturally placed;
explicit crouching and existing land permissions remain important around builds.

## Personal keep-inventory

Operator level 2 or console:

```text
/jugcraft keepinventory PlayerName true
/jugcraft keepinventory PlayerName false
/jugcraft keepinventory PlayerName
```

Targets must be online when selected; the setting persists by UUID across logout,
respawn, name changes and server restarts, in the world's `jugcraft:player_rules`
saved data. `true` keeps inventory, armour, offhand, XP and worn Trinkets slots
(including the pack's backpack slot). `false` removes the personal override and
uses the ordinary server gamerule. It does not force drops when global
keep-inventory is already enabled. The global gamerule is never changed, even
temporarily, so two players can have different death outcomes at the same time.

## Solo and party land

Use the existing Exchange land map (`/jugcraft exchange`, Land tab). Claims remain
Overworld chunks, use the existing economy and preserve existing ownership,
auctions, corporation land and trusted-player lists. No separate claim mod or
second ownership database is added.

```text
/claim                        # information about the current chunk
/claim buy                    # claim current chunk: starter allowance first, then money
/claim abandon                # release current chunk; return its free slot if applicable
/claim party true             # share this personally owned claim with your current party
/claim party false            # remove this claim's party sharing
/claim trust PlayerName       # existing owner-wide trusted-player access
/claim untrust PlayerName
```

Everyone has four reusable starter chunks (1,024 blocks of ground, or 32x32 when
arranged as a square). The land map and command use the same allowance. The fifth
chunk starts at the existing paid base price; later paid claims retain the usual
price growth. Old paid claims remain paid and do not consume the new allowance.
Starter claims cannot be sold, auctioned or transferred into corporations: abandon
one to reuse its slot elsewhere. This prevents cycling free land into unlimited
sale income. Existing admin claim limits and town exclusions still apply.

Create/invite/accept using the existing `/party` commands. Sharing is explicit per
claim and bound to the selected party's ID. Party members can build/use protected
blocks but cannot sell or release the owner's land. Leaving, being kicked or the
owner leaving the bound party revokes access. A replacement party does not inherit
it. Selling, assigning, auctioning or zoning ownership clears old party sharing.
Corporation land continues to use its controlling shareholder's existing rules.

Native explosions cannot break claimed blocks. Fire is extinguished before its
tick and cannot consume a claimed neighbouring block. Pistons and water/lava/native
fluid spread cannot cross ownership boundaries; movement within the same owner's
adjacent claims remains allowed. These changes apply to existing claims after
upgrading. They do not undo prior damage.

### Remaining server-policy and protection work

- Decide the total claim limit, inactive-player policy and whether additional
  dimensions should support land ownership.
- Finish a dedicated two-account abuse test covering bucket raycasts, projectiles,
  custom weapons, hopper/container automation and every custom world-changing
  machine. The native protections above are not a claim that all indirect modded
  interactions have been exhaustively audited.
- Add party-sharing buttons to the land map; commands already manage that setting.

## Validation

The combined local build passed all 2,244 required server game tests, including
the Candy Cavity changes. After the owner chose free starter land, the rebuilt
version passed all 14 tests selected by Fabric
`fabric-api.gametest.filter=*:*claim*`, including the new allowance lifecycle
and resale/auction/zoning restrictions. Client code compiled. All 187 Python
tests, repository/document checks and targeted tags/worldgen/delivery audits
passed. A hosted two-account acceptance/abuse test is still outstanding; nothing
has been deployed or published.

`PlayerRulesGameTests` exercises
UUID persistence, command permissions, actual death/respawn and worn slots, native
tree breaking/durability and non-tree rejection. `ClaimProtectionGameTests` covers
opt-in sharing, kick/transfer revocation, save/load, real explosions, fluids,
pistons and fire, plus the free allowance surviving save/load, being returned on
abandonment and not being resold or transferred for profit.

## Follow-up claim and defense audit

The [server defense and clock record](server-defense-and-clock.md) tracks the new
weapon, bucket, machinery and artillery guards, simulated two-player regressions,
automatic defense setup and current validation. Keep the hosted two-account
acceptance distinct from automated server simulations.
