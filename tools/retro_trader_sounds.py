"""Writes the Retro Trader's stomp and clap: assets/jugcraft/sounds/entity/retro_trader/{stomp,clap}{1,2,3}.ogg.

Run from the repository root:  python3 tools/retro_trader_sounds.py   (needs numpy and soundfile with Ogg Vorbis)
Original sounds synthesised here (no recorded or third-party audio), three takes of each so repeats do not sound
mechanical:

- stomp: a boot on a wooden shop floor: a low thump (a sine falling from about 120 Hz to 45 Hz), the board's
  knock (a short burst of band-limited noise) and a little ring from the floor;
- clap: a pair of hands: three or four tiny noise bursts a few milliseconds apart, filtered to the 1-3 kHz band
  where a clap's crack sits, with a short room tail.

Mono at 44.1 kHz, as the game needs for positioned sounds. The synthesis is deterministic; the encoded bytes depend
on the local libvorbis, so this is run by hand like the textures.
"""
from pathlib import Path

import numpy as np
import soundfile

RATE = 44100
OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "jugcraft" / "sounds" / "entity" / "retro_trader"


def band(noise, low, high):
    """Noise limited to low..high Hz (a brick-wall filter in the frequency domain; fine for short bursts)."""
    spectrum = np.fft.rfft(noise)
    freqs = np.fft.rfftfreq(len(noise), 1 / RATE)
    spectrum[(freqs < low) | (freqs > high)] = 0
    return np.fft.irfft(spectrum, len(noise))


def room(signal, decay=0.32, delays=(0.011, 0.017, 0.023, 0.031)):
    """A small room: a few feedback echoes, quiet and short."""
    out = signal.copy()
    for delay in delays:
        step = int(delay * RATE)
        echo = np.zeros_like(signal)
        echo[step:] = signal[:-step] * decay
        for _ in range(3):
            echo[step:] += echo[:-step] * decay * 0.5
        out += echo * 0.35
    return out


def normalise(signal, peak):
    fade = np.ones_like(signal)
    tail = int(0.02 * RATE)
    fade[-tail:] = np.linspace(1, 0, tail)
    return signal / np.max(np.abs(signal)) * peak * fade


def stomp(seed):
    rng = np.random.default_rng(seed)
    n = int(0.45 * RATE)
    t = np.arange(n) / RATE
    start, end = 118 + rng.uniform(-8, 8), 45 + rng.uniform(-4, 4)
    freq = end + (start - end) * np.exp(-t * 18)
    thump = np.sin(2 * np.pi * np.cumsum(freq) / RATE) * np.exp(-t * 9.5) * np.minimum(1, t / 0.002)
    knock = band(rng.standard_normal(n), 300, 1600) * np.exp(-t * 55) * 0.9
    ring_hz = 210 + rng.uniform(-20, 20)
    ring = np.sin(2 * np.pi * ring_hz * t) * np.exp(-t * 22) * 0.25
    return normalise(room(thump + knock + ring, decay=0.22), 0.92)


def clap(seed):
    rng = np.random.default_rng(seed)
    n = int(0.35 * RATE)
    t = np.arange(n) / RATE
    hit = np.zeros(n)
    offsets = np.cumsum(rng.uniform(0.004, 0.009, size=rng.integers(3, 5)))
    for i, offset in enumerate(np.concatenate([[0.0], offsets])):
        burst = np.clip(t - offset, 0, None)
        gate = (t >= offset) * np.exp(-burst * (420 if i < len(offsets) else 60))
        hit += gate * (0.75 if i < len(offsets) else 1.0)
    crack = band(rng.standard_normal(n), 900 + rng.uniform(-100, 100), 3200 + rng.uniform(-300, 300)) * hit
    body = band(rng.standard_normal(n), 350, 900) * hit * 0.35
    return normalise(room(crack + body, decay=0.3), 0.85)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for take in (1, 2, 3):
        soundfile.write(OUT / f"stomp{take}.ogg", stomp(100 + take), RATE, format="OGG", subtype="VORBIS")
        soundfile.write(OUT / f"clap{take}.ogg", clap(200 + take), RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    main()
