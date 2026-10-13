# Tiki torch proportions and companion scale

Owner-requested visual revision, implemented with OpenAI Codex (GPT-6) on `peepo-companion`, 8 October 2026.

The woven reservoir and its stepped cap are 15% narrower. The lower shaft is 1.6 model pixels thick (previously 1.1) and continues straight through the basket to just below the burner cap. Four short open supports flare outward only over the last six pixels below the basket, instead of twelve. The binding collar follows the shorter flare. The slightly lower burner leaves room for an orange base, two smaller side tongues and a taller yellow central flame, inside the existing two-block height.

`tools/generate_tiki_model.py` produces the item and both placed halves from the same geometry. Explicit UVs keep the two-block-tall item's faces inside their texture sprites, so the held basket uses the weave instead of stretching the sprite edge. The selection outline follows the thicker stem, shorter flare and narrower top. The shaft ends inside the cap to avoid coplanar wood/metal surfaces. Existing pixel weave and resource texture references are reused; no new texture assets or copied Minecraft files are introduced. The shared owner library/catalog was checked; the existing tiki weave remains appropriate to this edit.

Peepo and Jughead now draw the tiki torch at 0.50 scale instead of 0.35 (about 43% larger). The scale is selected in the existing client render state and applies through the upright held-light layer. Ordinary torches and other lights retain 0.35 scale. Recipe, light strength, dynamic-light definition, placement height and item identity stay the same. No new networking, AI or dependencies are added.

## Validation

`TikiTorchClientTests` is a focused runtime visual exercise: a planted torch, Peepo and Jughead holding tiki torches, and Peepo holding an ordinary torch for comparison. It verifies populated held render states and the respective scales, and captures day, night and player-held views. It uses a disposable world and does not execute unrelated feature tests. Local log: `build/tiki-refinement-final.log`; screenshots are preserved in `build/tiki-refinement-evidence/` after inspection.

The focused client exercise and `assemble` passed. In-game screenshots confirmed the revised proportions, upright enlarged companion torches and corrected held-item weave. Repository checks and whitespace validation also passed.
