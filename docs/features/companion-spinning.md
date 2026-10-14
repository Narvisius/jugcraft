# Companion Spinning Wheel

Implemented locally with OpenAI Codex (GPT-6) on `peepo-companion`, at the owner's request on 11 October 2026. Uses the existing Spinning Wheel, wool/yarn rules, Planner job slots, shared station budgets/reservations and Supply/Output transport. No new dependency, item, currency, recipe, texture or model.

## Player workflow and progression

Assign a Spinning Wheel with the Planner. Link wool Supply and yarn Output through the existing Supplies page. The usual per-job Auto/On/Off controls, source/output links, container permissions, job priorities and delivery energy costs apply. A clear safe floor in front is required; there is no tool requirement. Wool comes from sheep/shearing (including Livestock Management); its four matching-color yarn balls feed the existing knitting chain. Any supported wool color can be supplied. This first job does not unravel garments or trigger the cursed spindle ritual.

## Processing and storage

One wool is inserted transactionally into the existing skein. Four strokes, one every 20 active ticks, cost 4 reserve JE per active tick: 80 ticks/320 JE for a fresh wool, plus deliveries. Completed strokes already supplied by a player remain valid. Energy extraction is exact and rolls back if insufficient. Duplicate work calls in the same tick do not debit or advance twice. Interrupted partial strokes restart; completed strokes use existing persistence.

Companion completion captures the real yarn stack in one saved batch slot instead of spawning loose entities. Yarn components/colors are preserved. Supply waits while a skein or output batch remains; Output extracts through the existing transaction/cargo pipeline. Snapshot rollback restores wool, completed/partial strokes, managed-batch identity and output together; no speculative insertion plays a sound or emits a block update. A batch supplied or begun by a companion remains managed if a player/redstone finishes it. Manual-only spinning retains ground output. Shift-empty-hand use recovers saved output into the player's inventory, leaving leftovers in the wheel; breaking drops the real saved yarn and remaining wool.

Cursed/portal-active wheels reject companion input and work. Output can still be recovered. A single expiring station lease prevents simultaneous operators, is cleared on removal, and does not load chunks. Three front entrance candidates are checked on a bounded cooldown. Existing loaded-state, safe-ground, navigation memory, owner assignment, town protection, rest and schedule checks are reused. Work mutations use live status; planning shares the existing readiness cache, invalidated on wheel changes.

## Animation

An appended `SPINNING` action preserves existing wire ordinals. The existing Peepo/Jughead/costume rig uses a client-side two-hand feeding pose, body lean/bob and a one-foot treadle stroke. Its grip is transformed from the block's front for all four facings. A short final positioning correction follows the existing crank pattern and keeps the entity outside the wheel's collision box. The existing wheel renderer receives actual turn updates and keeps its original rotation, wool and bobbin rendering. No new assets were needed; the existing rig, procedural work clips and wheel model were inspected/reused.

## Validation and limits

The owner explicitly requested no tests. Common/client compilation and assembly pass with `gradlew.bat assemble`; the local distribution is rebuilt afterwards. No automated test suites or gameplay sessions are run. This is compilation evidence only; animation appearance, all facings/costumes, multi-helper contention, interruption/reload, mixed wool, full Output, rollback, manual/redstone handoffs and portal transitions remain manual checks. No multiplayer performance claim is made.

## Front approach correction

The owner's manual playtest found a companion stuck behind the wheel. The wheel inherited passable-path behavior for a partial collision shape. It now explicitly rejects paths through its block; companion navigation also excludes the wheel and its top from walk/jump nodes. The job paths to an open front staging point before aligning to its original working position. Both points, floor safety and their swept clearance are checked; final alignment cannot cut through the wheel or a corner. This uses normal shared path budgets rather than repeated side searches or forced traversal. Recompiled and packaged; no tests or gameplay session run for this fix, as requested.
