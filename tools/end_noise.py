"""How Fabric's End biome API shares out the outer End, so End biomes can be given the share they are meant to have.

Fabric picks an End biome from a weighted list with one noise value per place: the target is |noise| times the
list's total weight, and the entry whose running total first passes the target wins. Vanilla's own biome is always
first, with weight 1. The noise is a single octave of improved Perlin noise (`new PerlinNoise(random)` in Fabric's
`ClimateSamplerMixin`), read on the plane y = 0 at a place's quart x and z divided by 64. Its absolute value is
mostly small (half the places are below 0.2), so equal weights do not give equal shares: an entry late in the list
needs a large |noise| and is very rare. Barrens and midlands are picked with the same noise value, keyed by the
highlands biome picked there.

QUANTILES[i] is the |noise| below which a share i / 48 of places lie, measured by `python3 tools/end_noise.py`
(numpy), which samples the noise as described for 200 random seeds. The generator turns each End biome's share into
a Fabric weight with these (`tools/biomes.py`, `dimension_file`).
"""

QUANTILES = [0.0, 0.0071, 0.0144, 0.0217, 0.0292, 0.0367, 0.0443, 0.0519, 0.0597, 0.0676, 0.0755, 0.0836, 0.0917,
             0.0999, 0.1081, 0.1164, 0.1248, 0.1333, 0.1419, 0.1506, 0.1594, 0.1683, 0.1774, 0.1866, 0.1959, 0.2054,
             0.2149, 0.2247, 0.2345, 0.2445, 0.2546, 0.2651, 0.2758, 0.2869, 0.2984, 0.3103, 0.323, 0.3364, 0.3509,
             0.3665, 0.3835, 0.4018, 0.4218, 0.4438, 0.4682, 0.4955, 0.5312, 0.5858, 1.0]


def quantile(share):
    """The |noise| below which `share` (0 to 1) of places lie, interpolated in QUANTILES."""
    if not 0 <= share <= 1:
        raise ValueError(f"share {share} outside 0..1")
    position = share * (len(QUANTILES) - 1)
    low = int(position)
    if low == len(QUANTILES) - 1:
        return QUANTILES[-1]
    return QUANTILES[low] + (position - low) * (QUANTILES[low + 1] - QUANTILES[low])


def highlands_weights(shares):
    """Fabric weights for highlands biomes added in this order with these shares of the highlands (vanilla's End
    Highlands keep the rest, below the first). Returns the weights and vanilla's share."""
    vanilla = 1 - sum(shares)
    if not 0 < vanilla < 1:
        raise ValueError(f"highlands shares {shares} leave vanilla {vanilla}")
    total = 1 / quantile(vanilla)
    weights, below = [], vanilla
    for share in shares:
        weights.append((quantile(below + share) - quantile(below)) * total)
        below += share
    return weights, vanilla


def barrens_weight(share, vanilla_highlands):
    """The Fabric weight of one barrens biome keyed by vanilla's End Highlands that takes `share` of the barrens
    beside them. Vanilla's highlands hold where |noise| is below quantile(vanilla_highlands); there the barrens
    biome wins where |noise| times (1 + weight) reaches 1, so it takes the top `share` of that range."""
    if not 0 < share < 1:
        raise ValueError(f"barrens share {share} outside 0..1")
    return 1 / quantile(vanilla_highlands * (1 - share)) - 1


def measure(seeds=200, step=7, reach=2000):
    """Sample the noise as Fabric reads it and return QUANTILES afresh (needs numpy)."""
    import numpy as np
    gradients = np.array([[1, 1, 0], [-1, 1, 0], [1, -1, 0], [-1, -1, 0], [1, 0, 1], [-1, 0, 1], [1, 0, -1],
                          [-1, 0, -1], [0, 1, 1], [0, -1, 1], [0, 1, -1], [0, -1, -1], [1, 1, 0], [0, -1, 1],
                          [-1, 1, 0], [0, -1, -1]], float)

    def fade(t):
        return t * t * t * (t * (t * 6 - 15) + 10)

    def lerp(t, a, b):
        return a + t * (b - a)

    rng = np.random.default_rng(1)
    values = []
    grid = np.arange(-reach, reach, step) / 64.0
    xs, zs = np.meshgrid(grid, grid)
    for _ in range(seeds):
        ox, oy, oz = rng.random(3) * 256
        perm = rng.permutation(256)

        def p(i):
            return perm[i & 255]

        x, y, z = xs + ox, np.full_like(xs, oy), zs + oz
        xi, yi, zi = (np.floor(v).astype(int) for v in (x, y, z))
        xf, yf, zf = x - xi, y - yi, z - zi

        def grad(h, dx, dy, dz):
            g = gradients[h & 15]
            return g[..., 0] * dx + g[..., 1] * dy + g[..., 2] * dz

        a, b = p(xi), p(xi + 1)
        aa, ab, ba, bb = p(a + yi), p(a + yi + 1), p(b + yi), p(b + yi + 1)
        u, v, w = fade(xf), fade(yf), fade(zf)
        near = lerp(v, lerp(u, grad(p(aa + zi), xf, yf, zf), grad(p(ba + zi), xf - 1, yf, zf)),
                    lerp(u, grad(p(ab + zi), xf, yf - 1, zf), grad(p(bb + zi), xf - 1, yf - 1, zf)))
        far = lerp(v, lerp(u, grad(p(aa + zi + 1), xf, yf, zf - 1), grad(p(ba + zi + 1), xf - 1, yf, zf - 1)),
                   lerp(u, grad(p(ab + zi + 1), xf, yf - 1, zf - 1), grad(p(bb + zi + 1), xf - 1, yf - 1, zf - 1)))
        values.append(np.abs(lerp(w, near, far)).ravel())
    found = np.quantile(np.concatenate(values), [i / 48 for i in range(49)])
    return [round(float(q), 4) for q in found[:-1]] + [1.0]


if __name__ == "__main__":
    print(measure())
