# Companion greetings and conversations

Owner-requested addition on `peepo-companion`, based on `7f9b6ea1`. Implementation: OpenAI Codex (GPT-6), under the owner's existing tool/model exception.

## Player behavior

- An idle Peepo or Jughead waves with its free left hand when its owner comes within six blocks with line of sight. The wave lasts up to two seconds. The owner must leave the eight-block vicinity before a later arrival can greet again, with a minimum 60-second cooldown. Arrival checks happen every four to five seconds when idle, so a busy companion can greet after finishing its activity.
- Two standing, idle companions within three blocks can face each other for a six-second conversation. They alternate small arm gestures, listening nods and quiet, pitched vanilla frog sounds every 1.5 seconds. Each companion waits 90–120 seconds before another conversation. These are gestures and sounds, without chat messages or speech bubbles.
- Both variants and the pumpkin costume use the existing rig. Held tools and raised lights keep their right-hand pose; social gestures use the free hand when carrying an item.
- Shift-right-click, open **Routine**, and toggle **Social: On/Off**. On is the default, including existing saves. Only the owner or an authorized party member can change this through the existing validated menu.
- Greetings may happen while standing still in Follow or Stay. Conversations are disabled in those two command modes. Neither behavior walks to a partner, joins a seated/sleeping pose, or interrupts productive work, transport, food, recovery or settings editing. Pending delivery cargo also prevents socializing. Available higher-priority work can preempt a conversation.

## Server behavior and compatibility

The server chooses partners and animation phases. Conversations use a vanilla goal below work/rest/transport/commands and above wandering. Pair cancellation releases both partners on commands, menus, unload, loss of eligibility, separation or timeout. Active references and poses are never saved; reload resumes normal behavior. Remaining greeting/conversation cooldowns and the per-companion toggle are additive save fields.

Discovery uses a separate server-wide FIFO budget of one social scan per tick across all dimensions; it does not consume existing work-search or navigation allowances. Searches are staggered 80–99 ticks and query only the local loaded AABB, with at most 16 companion results before eligibility filtering. A detected nearby conversation prevents another pair from starting. No social navigation, chunk tickets, global entity scan or offline simulation is introduced. Active pairs validate their partner and line of sight each tick; large-population performance still needs measurement. `/peepobudget` includes admitted/deferred social searches.

Only pose changes and their start time are synchronized through entity data; animation runs client-side on the existing model. The menu appends one data field and statuses append Greeting/Chatting, preserving previous indices and inventory slots. Update both server and clients together.

This is cosmetic companion behavior at the existing companion-management tier. It has no recipe, resource input/output, extra energy cost, reward, buff, unlock or dependency. Existing energy recovery continues unchanged. Food, machinery and progression remain independent of social participation.

## Assets and frameworks

The shared owner library and full catalog were inspected for compatible frog sounds and greeting/conversation clips. No suitable companion clip or sound was found. New procedural wave/talk/listen poses extend the existing authored companion rig; no imported animation files or new rendering framework are used. Sounds reference Minecraft's built-in `FROG_AMBIENT` event without copying Mojang files. Existing companion textures/models are unchanged. Vanilla goals and the current inventory-style menu are extended under the framework catalog's existing-feature allowances. No new assets or dependencies are bundled.

## Validation

On 8 October 2026 the owner's launcher log identified a client spawn disconnect: `CompanionSocial.<init>` called `Entity.getId()` before `ClientboundAddEntityPacket` assigned the numeric ID. Initial scan staggering now uses the already-available UUID. The hearth-only regression class includes direct client construction of Peepo, Jughead and the legacy variant, plus real Peepo/Jughead spawn packets. This fixes initialization without changing social cooldowns or saved data. See [Hearth Oven validation](companion-hearth.md#validation) for the scoped run; it does not retest all social behavior.

The initial social implementation passed common/client compilation and assembly without automated or in-game tests, following the owner's instruction at that time. The later hearth-only run above verifies its spawn prerequisite; the other social acceptance cases below remain unverified. Packaging uses the existing offline launcher-pack builder without dependency changes; package creation does not verify pack import or gameplay.

Manual acceptance remains: owner arrival/departure and cooldown; both character variants and held light poses; nearby pairs taking turns; work/food/rest/command/menu interruption; toggle and cooldown save/reload; partner unload/death/dimension change; two-client synchronization; crowded-server profiling. In-game animation appearance and sound volume are unverified.
