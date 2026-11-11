# Drone tower apron and clear terminal entrance

New towers use layout 7. The full 81 by 81 ground pad is generated during tier 1 construction, with reinforced concrete backing, charcoal service lanes, metal borders and recessed floor lighting. The plain stone square previously visible in the client gallery was review-world preparation, rather than part of the generated tower. The gallery now shows the tower's own paving.

The ground lights now serve spawn prevention, with a minimum block light of one on exposed exterior floor cells. The previous dense grid targeted level five. Wider spacing and removal of redundant lights reduce the number of floor fixtures without relying on daylight. The command room retains useful task lighting, and every wall, ceiling and elevated light stays in its previous position. The apron lights remain fixed across tiers; later upgrades cannot restore removed paving. Required tower lighting remains independent of the removable outer apron.

The outer paving is cosmetic. It uses ordinary blocks, with no multiblock controller, integrity scan or repair timer. It can be removed with normal tools after placement; completed construction and later upgrades leave removed or replaced paving alone. The exported `cosmetic_apron` coordinates identify only this removable paving and backing. They exclude the core, plinth, landing field, machine foundations, bearing footings and all later construction or clearance cells. Removing functional pads or machine parts still has their normal effect.

The concrete column at the centre of the terminal entrance has been removed. Its overhead cell is a continuous longitudinal steel beam, carried by the existing surrounding structure. The three-block-wide approach is kept clear through the doorway.

Layouts 5 and 6 have their construction data and ten status images frozen byte for byte as `tower_industrial_v5` and `tower_industrial_v6`. Layouts 1 through 6 retain their saved progress and geometry. Layouts 6 and 7 use the existing chunk ticket mechanism with a 40-block radius to include the generated apron. This revision adds no block entities, light ticker, or periodic paving scan.

To inspect a newly generated tower, choose an open site at least 100 blocks from an older tower and run `/dronetest build industrial`. Existing towers are not automatically rebuilt over player changes.

Implementation and original layout changes: OpenAI Codex (GPT-6).
