# Arcane Concordance: logistics with reservations and accountable transit

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 18) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 10, Practitioner stage (Jugcraft Workshops). Roadmap step 18.
Primary specialty and supported player role: the Clockhearts (constructs and accountable logistics). Ask for an item and
have it fetched from your stores, with every item accounted for on the way.

Builds on [spirits, familiars and constructs](arcane-concordance-workers.md) (the Clockwork Porter, the Porter Key and
the Binding Arts) and the shared item and container rules. Contract and checklist:
[ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once you understand the Binding Arts, build a **Courier Post** (planks, a barrel and a book and quill) among your chests.
Use a Porter Key on the post, then on a Clockwork Porter: the porter now serves the post.

**Requesting.** Use the post with an item in your hand to request a stack of exactly that item: a named, enchanted or
otherwise different one is a different item. `/jugcraft concordance logistics request <count>` asks for a number of what
you hold instead, at the nearest post you may use within 8 blocks.

**Fetching.** A courier takes one request at a time, finds the nearest chest (or barrel, or other plain container)
within 8 blocks of the post that holds the item, reserves what it will take there, picks it up, carries it to the post
and puts it in the post's nine slots, for you or a hopper below. Two couriers never reach for the same items: what one
has reserved, another does not count. If the chest was emptied meanwhile it takes what is there and finds another.

**When things go wrong.** A full post takes what fits; the rest stays in transit and the courier says the post is full.
A courier broken, unloaded or lost in a restart leaves its cargo **stranded** at the post: another courier takes it up
there, or you take it yourself (`/jugcraft concordance logistics recover <number>`). Cancel a request
(`... cancel <number>`) and anything picked up is taken back to its chest first (or into the post if the chest has no
room); a broken post does the same for every request bound for it. Nothing is ever dropped or deleted on the way.

**Seeing it.** Use the post with an empty hand: how many requests are open and in transit, its free slots and its last
eight steps (filed, claimed, reserved, picked up, delivered, stranded, taken up, taken back, cancelled, recovered, with
the reason). `/jugcraft concordance logistics` lists your requests with their state, progress and reason. Jade shows a
post's counts and its latest step, and a courier's status (its step 17 status: working, full, waiting for resources,
blocked by access, cannot find a way, its destination not loaded...).

## How it works

**The ledger** (`concordance/logistics`, pure Java): every open request, as a fixed ticket (requester, post,
destination, the item's key, how many) and its progress (delivered, carried, reserved and where, the claiming courier
and its lease, whether the cargo is aboard, whether it is being taken back, a note). The state (open, claimed, in
transit, stranded, returning) is worked out from the progress, never stored beside it. Every step is a method that
checks the courier holds the claim and the counts add up, or refuses with a reason:

- **Claims** are exclusive and carry a lease of ten seconds that the courier renews each decision: a courier that is
  removed, unloaded or missing after a restart lets its claim lapse by itself.
- **Reservations**: a courier reserves no more at a source than the source holds less what live claims have reserved
  there for the same item; picking up re-reads the container and takes no more than was reserved.
- **Cargo in transit belongs to the request**: a courier released while carrying leaves it in the ledger, stranded; a
  request is never closed with cargo still carried (cancelled or its destination gone, it is taken back first).
- **History**: each post keeps its last 32 steps.

**One place for items in transit.** The world's `CourierLedger` (saved data) keeps the ledger and each request's exact
item, a Fabric Transfer API `ItemVariant` (the item and every component it carries). A request asks for one variant, so
its cargo in transit is exactly the ledger's carried count of that variant: there is no second copy that could
disagree. A courier holds only its claim's id, never items: not in its inventory, its brain's memories or its
animation.

**Moving items** goes through the Fabric Transfer API, as Jugcraft's item pipes do (`ItemStorage.SIDED`). A pickup
extracts the reserved variant from the source and a delivery inserts it into the destination, each inside one
transaction that commits only if the ledger accepts the step; a refused step rolls the container back, so a container
and the ledger never disagree. Containers keep their own rules: slots, stack sizes and what each slot accepts.
`recover` gives the requester the variant in whole stacks (any that do not fit drop at their feet, as vanilla does).

**Item identity.** Matching is by variant, so a named, enchanted or damaged item is never taken for a plain one. The
ledger's key for an item is the variant's canonical encoding, the same in every session, so reservations of one item
always add up across restarts.

**Couriers** are Clockwork Porters bound to a post (`ClockworkPorterEntity`, courier mode): each decision makes at most
one ledger step. SmartBrainLib walks them (step 17): the decision chooses the work, the brain only moves the body, and
nothing about the cargo lives in the brain. A courier fuels at a pylon by its post; each delivery is a trip (Ley Charge
and wear). Sources are plain containers (not furnace-like ones) whose chunks are loaded and that its keeper may open;
nothing is loaded to fetch from.

## Connections

- Input producer: any container round the post; Clockwork Porters (step 17) and their Ley Charge.
- Output consumer: the post's slots, hoppers and pipes below it, the requester.
- Technology connection: porters run on Ley Charge (from Radiance or electricity); posts feed hopper lines.
- Magic connection: the Binding Arts (the porter, the key).
- Reachable entry path: First Light → the Binding Arts understood → a Clockwork Porter, a Porter Key and a Courier Post
  (vanilla materials).
- Solo, trade and cooperative routes: solo, or a party sharing a post (its owner's party may file and bind couriers).
- Specialty use without other branches: needs only the Binding Arts.

## Balance

- No item is created or destroyed: every item is in a container, in transit (counted once) or handed to its
  requester. The harness checks this after every step of 200 random runs; the game tests audit it on a real server.
- A request asks for 1 to 256 items; a player has at most 16 open requests and a world 1024. A courier moves at most
  what its body allows (step 17: 16 items, 2 Ley Charge and 1 wear a delivery).
- A request with no source rests ten seconds before another try, so idle couriers do not churn.

## Multiplayer and persistence

- Server authority: filing, claims, reservations, transfers, cancellation and recovery are the server's; the client
  only sees chat, Jade and the porter's synced status. Filing and reading a post are rate-limited.
- Permissions: only the post's owner's party files requests and binds couriers; a courier takes only from containers
  its keeper may open; only a request's requester cancels or recovers it.
- Persistence: the ledger, samples and cargo in `jugcraft:courier_ledger` (world data); a courier's post and claim
  (`post`, `task`) on the porter; a key's post in its `jugcraft:courier_post` component; the post's slots and owner on
  its block entity. The cargo is saved with the ledger, so a restart neither repeats nor loses a step.
- PvP: nothing here harms anyone.
- Disable behaviour: with `concordance.enabled=false` couriers stop (switched off) and nothing is filed; the ledger and
  everything in transit are kept.

## Dependencies and assets

No new dependency. Framework use:

- **SmartBrainLib** (through step 17's workers): movement only; the ledger is not in the brain.
- **Jade (optional)**: a post's open and in-transit counts and its latest step; couriers show their status.
- **Modonomicon**: a **Couriers** entry in the Binding category.
- **GuiLib**: not used. The bounded request-history view is the post's chat summary (its last 8 of 32 steps), the
  `logistics` command and Jade; a GuiLib history screen is an open item (step 26).

- **Fabric Transfer API**: every pickup, delivery and return is a transaction on the containers' `ItemStorage`, the
  same infrastructure as Jugcraft's item pipes and extractors; items are identified by `ItemVariant`. The post's nine
  slots are an ordinary container: hoppers below, or a Pneumatic Extractor into Jugcraft's pipes, take deliveries out.

Art: the Courier Post's three 16x16 faces (a dark-oak cabinet of nine pigeonholes, one per slot, with brass corner caps
and letters; on top, an open ledger with a violet seal) and its 16x16 icon map, drawn by
`tools/concordance_logistics_art.py` and `tools/item_icons/courier_post.txt` in the Concordance's own dark oak, brass,
violet and paper colours. Nothing is taken from the owner's library (its cabinets, crates and brass blocks were looked at
and did not fit) or from Mojang's files. The art has not been shown to the owner yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 18 checks (`check_logistics`): the Java ledger and post numbers equal the
  generator's; every state, note, event and refusal the Java can name has its text; the post has its recipe, model,
  loot and icon; the logistics package is pure Java; a courier's work moves items only through `CourierLedger`, whose
  every move is a Transfer API transaction committed with its ledger step.
- The pure core compiles with JDK 21. The step 18 harness passes **41 checks**: a whole request in steps with its
  history; two claims never reserve the same items, and couriers take only what is really there when items were taken
  by hand; a named item is not a plain one; a full destination takes what fits and the rest stays carried; cancelling
  takes cargo back; a removed courier strands its cargo and another takes it up at the post; a lapsed claim releases its
  reservation; a restart keeps the cargo in transit, ids and history; a broken post turns carried requests back and
  cancels the rest; only the requester recovers, once; the limits; a released request rests before the next try; and
  200 random runs of filing, claiming, reserving, picking up, delivering, cancelling, recovering, breaking the post,
  removing couriers and restarting never duplicate or lose an item and never let two couriers work one request.
- Game tests added: `ConcordanceLogisticsGameTests` (five): a request is fetched and delivered (exactly the plain
  ingots, never the named ones in the same chest; the history tells each step); simultaneous requests never claim the
  same items (two couriers reserve the chest's 40, not 60, and pick up 40); full storage, removal and a restart lose
  nothing (the post takes 10, 20 stay in transit and the courier says full; removed, its cargo is stranded; the ledger
  saved and loaded keeps it; another courier takes it up and delivers it); cancelling and recovering give items back
  (taken back to the chest; stranded cargo recovered by its requester, once); and a broken post turns its request back
  with every item counted. Every test audits the ledger against its cargo.
- CI: the first push (d86f0895) failed to compile: in 26.3 `Inventory.placeItemBackInInventory` takes a `Prediction`
  (recovered cargo now uses `SERVER_ONLY`, as the crucible and circle anchor do). Fixed in 8c20db46; run 37619575937
  passes the whole Build workflow: it builds, passes the data checks and all 990 required server game tests (the five
  above among them), and the client test shards pass.

Not yet run: any client (the post's look, Jade lines, codex page), a two-client dedicated server, real walking between
chests, and a real restart of a server with couriers mid-delivery (the test saves and loads the ledger's codec).

## World and event applicability

Works in every dimension; a post fetches only from its own dimension's containers. No seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:courier_post`; data component `jugcraft:courier_post`; saved data
`jugcraft:courier_ledger`. Removing them needs a migration (and the cargo in transit handed back first).

Open items:

- A request is delivered to its post only; delivering to another container chosen by the requester needs a way to
  mark it (a GuiLib screen or another key use).
- A GuiLib history screen (step 26); the chat summary shows the last 8 of the 32 steps kept.
- Sources are found by nearness only; preferring or excluding particular chests needs a marking tool.
- A crash between saving a chunk and saving the world's data could repeat or lose one step's transfer, as with
  vanilla's own block entities; a clean stop or autosave keeps them together.
- Sources are plain containers (chests, barrels and the like); furnace-like blocks, shulker boxes and other mods'
  storages that are not plain containers are not fetched from.
