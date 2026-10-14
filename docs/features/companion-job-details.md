# Companion job status details

Owner-requested on 12 October 2026, implemented using OpenAI Codex (GPT-6). Extends the existing companion inventory screen and report system; no new dependencies or assets.

## Player outcome

Each of the four Jobs rows has a dedicated status line beneath its controls. Rows are 28 pixels tall, with Lunch and the player inventory moved down to avoid overlap. Hover a status line to read the complete explanation. The menu explicitly says that the companion pauses while settings are open.

Explanations identify linked Supply/Output numbers, disabled Supply/Output and external automation, missing tools and a locked Hand, unsafe approaches, unavailable machines, energy recovery thresholds, crop/animal readiness, and the action needed for common station problems. Generator Wheel and Hand Crank capacity messages refer to energy, not item chests.

When a cooking pot or ordinary processor already has a selected recipe, missing ingredient counts describe deficits in the station's installed inputs. Ingredient alternatives are marked as equivalents. These are not claims that a particular chest is empty: the explanation names the linked Supplies to check without searching their contents. Without a selected plan, the message asks the player to check ingredients and Filter instead of guessing a recipe. Actual retained delivery manifests report item/count, additional stacks and the numbered source/destination. Failed transport approaches remain explicitly labeled as the last attempt for up to ten seconds.

## Server cost and compatibility

Only an authorized, valid, currently open menu requests details. It samples at most four rows once per second, and sends a packet only when the four bounded text/target pairs change. The packet carries at most four 512-character messages. Client application checks the menu ID; each row also checks its assignment identity so reordering or removal cannot display another station's detail. No additional chest scans, path searches, chunk loads, world tickers or recipe planning are introduced. Descriptions reuse current report states, selected recipe data and physical transport manifests.

No resources, progression costs, recipes, ownership rules or saved assignment formats change. This is interface infrastructure for existing jobs; ordinary work and Supply/Output remain authoritative. Both client and server must update for the new menu packet. There are no worldgen or dimension-return changes.

## Validation

Validated on 12 October 2026 in the working tree based on `6b0835e4`, using Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and the pinned Java 25 environment.

- `build-local.ps1 -Tasks runClientGameTest,'-PclientTests=PeepoSuppliesClientTests',assemble`: passed, **46 assertions**, 2m47s. Only the companion supplies/menu suite ran, not other gameplay suites. Checks cover existing physical routing/tool pickup plus linked-source explanations, disabled Supply, Hand lock, missing Output links, nonmutating ingredient deficit counts, reordered assignment identity, menu synchronization and the refreshed live heat explanation. Common/client compilation and assembly passed.
- Inspected the actual `peepo_job_status_details` gameplay screenshot: all four job lines fit beneath their controls without overlapping Lunch or inventory. The initial snapshot exposed a temporary misleading unassigned label; it now says readiness is being checked until the report refreshes.
- Repository structure/documentation-link and whitespace checks passed. Local test evidence: `build/companion-job-details-tests.log` and `build/run/clientGameTest/screenshots/0000_peepo_job_status_details.png` (not committed).

Large-server load, two-independent-client testing and every status on every machine are outside this UI change. This run is not a multiplayer performance certification.
