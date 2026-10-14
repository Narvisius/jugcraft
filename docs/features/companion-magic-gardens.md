# Companion magic gardens

Owner-requested implementation on 11 October 2026 with OpenAI Codex (GPT-6). Extends existing two-corner Farmland zones, the four workstation assignments, shared search budgets, eight physical cargo slots and linked Supply/Output. No new assets, dependencies, resource currencies, packets or tickers. Maintenance remains deferred.

## Progression and behavior

Practitioner / Workshops integration for Verdant Husbandry and companion logistics. Players research and awaken beds/devices through the existing Greenwarden path. Existing organism items are planting inputs; outputs are alchemy reagents and Verdant Chaff. Chaff can be delivered through a shared container to a Mulch Maw; the native nutrient economy and Heart/Gleaner Verdance costs remain unchanged.

Verdant Beds qualify as garden soil, within the existing zone ground bounds. Native BlockItem crop seeds participate in the existing planting selection and Supply shortage planning, only on an awake accessible bed with a live crop organism definition. Empty selection plants compatible seeds; a selected seed limits new planting. Existing mixed magic crops always remain planted, even after the selection changes. Each harvest uses `OrganismCropBlock.harvestYield` and the live organism's regrowth stage, rather than ordinary break loot and seed-funded age-zero replanting. Full inventory aborts before changing the crop; Fabric transactions roll back cargo and provenance. The existing live-state check invalidates targets harvested by a Gleaner or another player/companion. No manual research evidence is awarded.

Magic planting/picking uses the existing interaction clip and 80 ticks at 4 JE/t (320 JE uninterrupted). Hoes neither speed nor wear on these actions; tool-fetch planning skips an active magic bed. Deliveries retain the existing stack/count-based energy cost. Ordinary garden behavior and crop growth conditions remain native; no fertilizer, watering, tilling, light management or habitat changes are introduced.

## Living-device logistics

Mulch Maw and Gleaner are Planner workstation targets, each using one job slot. They use the existing physical delivery lifecycle and safe approach selection, not a resident assistance job or speed bonus.

- Maw: transactional insertion into its real single slot, bounded to sixteen matching items. Its persistent nine-item companion filter defaults to chaff on new/legacy blocks; explicitly clearing it permits all native mulch. Only tagged compostables may be selected. Native feeding/hopper rules are unchanged. Demand pauses on the existing `beds_full` status or a held whole nutrient, without rescanning nearby beds. This can briefly lag the native two-second pulse. No extraction port is exposed; Output is marked unsupported.
- Gleaner: guarded transactional extraction of existing contents from its nine real output slots. No input port or supply plan; Supply is marked unsupported. Native harvesting, keeper-presence rules, Verdance transfers and cost of one Verdance per harvest are unchanged. Already harvested goods can be hauled by an authorized party companion while the device's keeper is away, provided the companion owner's authority is available.

The Maw filter is station-wide, saved as `CompanionFilter`, and edited through the existing funnel button. Its revision identifies an in-flight supply plan; changed filters or vanished demand use existing unused-cargo return behavior. The GUI explains that its filter selects compost inputs. Gleaner and Maw statuses distinguish disabled/dormant devices, access denied and missing Verdance where applicable.

## Permissions, persistence and performance

Cheap garden discovery checks feature enablement, awake state, companion owner availability and keeper/party membership. At the actual crop mutation, `Authority.mayChangeFor` checks both bed and plant as the companion owner, including spawn/world-border/town and Fabric protection listeners. Device insertion/extraction rechecks that same native authority and the live device identity. The existing `concordance.absent_owner_authority` server option applies; by default owners away/offline do not authorize changes. No new fake-player system is introduced. Native authority may use its existing stand-in only when that server option is enabled. Filter editing additionally requires the viewing player to pass the device's keeper/party permission.

Existing garden mob-griefing gating remains in force. Planting and harvesting require loaded blocks. Normal companion storage ownership, job links, settings pause, rest, food, priorities and cargo recovery continue to apply. No chunks are loaded. Crops reuse the existing 32-cell rolling discovery slice and shared per-cell leases; this integration does not sample habitats or add per-companion region scans. Device probes touch one Maw slot or nine Gleaner slots, under existing logistics budgets.

Assignments and cargo retain their existing save format. Only the Maw gains a saved companion filter; missing data defaults to chaff, while a saved explicitly empty filter stays empty. Feature disabling preserves all blocks and goods while stopping this integration. Neither a harvest nor a ghost filter creates new inventory outside native yields / actual transfers.

## Validation

Automated tests and gameplay were not run, as requested. Common/client compilation and `assemble` passed (final log: `build/magic-garden-final-build.log`, 40 seconds), with existing deprecation/annotation warnings. The first sandboxed build could not connect to its local Gradle daemon; the authorized build outside that restriction succeeded. The complete Modrinth distribution was rebuilt using the same eighteen locked libraries and platform pins. Its embedded JAR and SHA-512 sidecar were verified against the final build. `git diff --check` passed. No game launch, pack import, automated test suite or multiplayer playtest was performed.

Manual checks remaining: all four crop seeds and yields; mixed-bed preservation and crop selection; full cargo; normal Supply/Output with planting reserves; simultaneous Gleaner/player/companion picks; empty/wrong seeds; disabled/dormant beds/devices; keeper/party changes and owners away; protection listeners; Maw default/empty/restricted filters and sixteen-item stock; full beds and native hopper overlap; filter changes during a delivery; Gleaner full/empty Output and absent Verdance; saved filters, reload and removed devices. Compilation does not verify these gameplay or multiplayer cases.
