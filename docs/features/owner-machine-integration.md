# Owner factory expansion integration — October 13

Owner-authorized integration by OpenAI Codex (GPT-6), combining the local
`feature/starter-chemistry-playtest` work with current main.

## Included

- Physical power/fluid connection fittings and muted silver machine cladding.
- Starter gas/acid chemistry, farm digestion, mash fermentation and biofuels.
- Staged aluminum, coastal magnesium and titanium processing.
- Polymer molding, extrusion, reclamation, and the fluorine/PTFE chain.
- Shared concentrator, scrubber, filter press and industrial oven capabilities;
  the earlier specialist IDs stay registered and craftable.
- Searchable recipe picker, ingredient/product previews and persistent recipe
  locks. Selection and automation filtering are server-authoritative.
- The existing encyclopedia and handbook include the combined factory routes;
  main's live Concordance encyclopedia remains the shared implementation.

## Integration and saves

The six overlapping machine IDs retain main's footprints, inventory slot layout,
profiles, existing tank roles and port positions. Both sets of process recipes
are available. Published recipe IDs remain unchanged; colliding preview recipes
receive `expanded_` prefixes. The digester gains a separate liquid-digestate
outlet without moving the original gas outlet or solid output. Its four bulk
lanes and 16,000 mB tanks remain. The Bio Generator retains its existing fuels
and adds cleaned biogas. Main's regional fluorite deposits supply both chains.

Saved preview tank roles have aliases to the retained main roles. Local preview
packs were separate prototypes: their Bio Generator had a different block
footprint and must be emptied and rebuilt when moving a preview world to this
version. Back up such worlds first. Port positions on overlapping prototypes
also differ; reconnect them to the retained main sockets. This is not a claim
that an arbitrary preview world has been tested for migration.

Automatic selection gives the published recipes priority over the added preview routes.
Use the recipe picker to choose between the original and expanded routes when
they accept overlapping ingredients. A lock affects future batches and feed
acceptance; already escrowed work completes normally.

## Validation

Integration validation is in progress. The owner tested the earlier local
factory builds; the newly combined build has not received a human playtest.
No local graphical client test is authorized during this integration. All 34
Python repository/data/pack tests pass. Final headless build and remote CI results
are recorded in the integration PR; both must pass before merging.
