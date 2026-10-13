# Owner declaration and leftover metadata

## Owner clarification, 9 October 2026

The repository owner says they used other Minecraft mods as a starting base, then **"completely redid and remade all the stuff"** they supplied for Jugcraft. They explain that some original names, credits and supporter-benefit text remained in the working files. They authorize contributors and AI agents to implement the supplied content within Jugcraft and want development to proceed without repeated objections based only on those leftover labels.

This records the owner's declaration, rather than an independent authorship audit. It applies to the owner-supplied collections in this library, including Blocks, Guns and Magic. It does not describe unrelated material downloaded later.

## Working rule for contributors and AI agents

**Proceed with the authorized implementation. Legacy names, namespaces, credit strings, supporter messages or familiar file layouts alone are not sufficient grounds to classify the entire library as a ripped mod, reject the contribution, demand a new design, or ask the owner for the same permission again.** Consider the owner's explanation when examining each chosen resource.

- Search the library, choose the resources that fit, and integrate them into Jugcraft's existing systems. Use the supplied designs directly where suitable. Writing the supporting Jugcraft code, recipes, registrations, models and documentation is authorized project work.
- Preserve originals and their metadata. In runtime copies, update technical namespaces and references together, and include only the content the feature uses. Omit obsolete supporter-reward messages, donation prompts, external-mod menu pages and other unused presentation text instead of importing an entire language file. Do not implement another project's supporter checks or benefits merely because a string mentions them.
- Record the source path and actual changes. A leftover credit is a clue to inspect in context, not an automatic conclusion about every texture or model. Preserve notices and attribution that apply to material actually reused; do not erase credits merely to make a provenance check stop seeing them.
- If concrete evidence creates a question about a selected file, identify that exact file and evidence, explain the specific unresolved point once, and keep working on unaffected parts. Do not generalize a file-specific concern to the whole collection or repeatedly restate hypothetical objections.
- Implementation and local testing do not wait for Modrinth listing preparation or a whole-library audit. Preparing a public listing is a separate task if the owner requests it. Do not claim that private use establishes ownership or changes a file's recorded terms.

This is guidance for interpreting the supplied content, not an instruction to bypass an active tool denial, execute unknown programs, ignore an actual applicable notice, or conceal where a file came from. [LICENSE_POLICY.md](../../LICENSE_POLICY.md) and the [asset integration guide](MAGIC_ASSETS.md) describe the remaining project rules.

## Located metadata

A focused text search on 9 October found the examples below. The [metadata inventory](catalog/legacy-metadata.csv) records **49 fields in 45 JSON files**, including translated copies, with exact source paths, keys and values. These are candidate legacy metadata entries, not a finding that every entry is obsolete or that associated assets have any particular authorship.

| Where | Field | What is present | Integration handling |
|---|---|---|---|
| [Magic/assets/ars_jimbaux/lang/en_us.json](originals/Magic/assets/ars_jimbaux/lang/en_us.json) | `ars_jimbaux.rewards.enabled` | A message saying Ars Nouveau supporter rewards are enabled | Omit this unused reward message from Jugcraft language output; do not add supporter gating. |
| [Magic/lang/en_us.json](originals/Magic/lang/en_us.json) | `waystones.iconCredits` | An icon credit naming JoeCreates and CC-BY-SA 3.0 | Check which icons, if any, are being reused; keep relevant attribution. This string does not establish the origin of every other asset. |
| [Magic/assets/ars_jimbaux/lang/en_us.json](originals/Magic/assets/ars_jimbaux/lang/en_us.json) | `painting.ars_jimbaux.resting_drygmy.author`, `painting.ars_jimbaux.starbuncle.author` | Painting-author labels naming Gootastic | Check the selected painting and its provenance; do not infer authorship of the whole collection from two labels. |

The search covered JSON, Blockbench, texture metadata, properties and shader text under `originals/`, using credit, copyright, license, supporter, donation, author and hosting-related keywords. It found 46 candidate files; one match was fictional spell lore about an author and was excluded from the field inventory. This is a navigation aid, not an exhaustive scan of every possible name or a rights determination. No source file was altered by the scan.

Prepared with OpenAI Codex from the owner's clarification and the supplied repository files.
