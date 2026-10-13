# Companion kitchen positioning and chopping

Owner-requested on 8 October 2026, implemented with OpenAI Codex (GPT-6) on `peepo-companion`, from `ac3040f4`.

## Behavior

Cooking Pot helpers choose only the north or south rim. The current pot is not rotatable: its bail spans east-west at z=7.5..8.5 pixels, with lugs/uprights on the east and west. If one safe side is blocked the other is considered; helpers do not fall back through a handle when both are blocked. Existing lower-level access, headroom checks, leases, assistance and safe exits remain.

Cutting Board helpers approach the block's facing side and face inward toward its food. At floor level they stand beside the front edge. For a board on a one-block counter they approach from below and stand on its front edge, with a saved ground-level exit. Occupancy checks the companion's actual bounds, including Jughead's height. Removing/rotating the board, blocking its working space, stopping work or unloading releases the station through the existing routine.

The new CHOP action uses the actual knife in the Hand slot. Its native item model is resolved with `ItemDisplayContext.NONE`, without the ordinary companion held-item reduction or arm-bone scale. Existing knife textures/models and resource-pack changes therefore remain in use. The diagonal knife handle is the shared pivot for two stacked hands. A slow lift, quick downstroke, body lean and dip repeat over forty working ticks. The client phase uses the existing synchronized action start time; no new per-frame server packets, fake tools, entities or particles are introduced.

The cut still processes the existing recipe once per forty work ticks, costs up to 16 JE and wears the real equipped knife once. Filters, ingredients, output/byproduct storage, tool predicates and Supply/Output transport retain their existing authority. Only CHOP is appended to the action IDs; existing IDs and saved data remain compatible. Update both client and server to render the new action.

## Assets and framework use

The owner library and farming/food catalog were inspected, including its pot, board and knife textures. This revision copies no assets and changes no textures or item models. It reuses the runtime knife models (`assets/jugcraft/models/item/*_knife.json`), original pot/board models and the existing Peepo rig. The two-handed clip is original procedural source in `MachineWorkClip`, using the existing shared arm solver. This retains the simple renderer allowed by `docs/FRAMEWORKS.md`; no new animation infrastructure or dependency is introduced.

## Validation

Focused runtime command:

```powershell
.\build-local.ps1 -Tasks @('runClientGameTest','-PclientTests=PeepoKitchenAnimationClientTests','assemble')
```

The suite checks all four board orientations, floor/counter access, correct facing, blocked-front refusal, forty-tick cuts, output counts, reserve cost, knife wear and ground-level release. Client checks cover both companions and both costumes, both hands remaining on the grip, whole-body motion, native knife scale, idle reset, real iron/diamond knife rendering and autonomous cuts. Pot checks cover approach from every direction, actual occupied-rim handle clearance, alternate safe-side selection and refusal when both safe sides are blocked. The prior focused readiness, synchronization, bunk and navigation checks also execute.

Final run passed **1,096 assertions** on 8 October 2026 at 14:04 local time; compilation/assembly succeeded in 1 minute 12 seconds. The initial run also passed 1,086 assertions; the final run adds actual pot occupancy and clearer photographic fixtures. The four final raised/strike screenshots were inspected: the iron and diamond knife models retain native size, both rigs lean into the cut, both hands share the handle and Jughead's jug clears the raised knife. The screenshot helper uses the current GUI-toggle binding; an intermediate fixture-only compile error from the removed `Options.hideGui` field was corrected before the final run.

Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.0.4.1+1, and the locked development integrations. The test creates a disposable flat world with its kitchen at `(0,-56,0)` and the retained performance checks at `(0,-56,20)`. Evidence remains local in `build/companion-kitchen-animation-final.log` and `build/run/clientGameTest/screenshots/*_knife_*.png`. Repository/link checks and offline launcher packaging passed after assembly.

The final screenshots and log are also preserved in `build/peepo-kitchen-animation-evidence/` so a subsequent game-test run can replace its temporary folder safely.

No unrelated gameplay suites, player worlds, external knife models or two-client multiplayer load tests are included. The generic diagonal knife grip matches the current knife family; a future custom model with a different handle origin may need its own grip adjustment. No Blockbench exports changed.
