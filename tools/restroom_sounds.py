"""The restroom's sounds (round 26, #14), synthesised here (no recorded or third-party audio):

- toilet_flush: a commercial flush valve: the handle's click and the valve's thunk, the rush of water into the bowl
  (loud, broad, swirling as the bowl turns it), the siphon's gurgle as the bowl empties, the refill's hiss dying away.
- urinal_flush: the same valve on a urinal: a lighter, higher wash down the back, a trickle at the end.
- faucet: a sensor faucet running for five seconds: the solenoid's click, the stream on the bowl, the click off.
- hand_dryer: a high-speed hand dryer: its motor spinning up to a whine, the jet of air, spinning down.
- shower: a shower running (item 22, the Tri-Pillar's locker room): the spray's hiss and the water drumming on the tray,
  six seconds that repeat seamlessly while it runs (no clicks at its ends).

Writes assets/jugcraft/sounds/restroom/*.ogg (needs numpy and ffmpeg with libvorbis); their sounds.json entries are
SOUNDS, written with every other sound by tools/generate_material_data.py. Played by nuclear/RestroomBlocks.java.
"""
import os
import subprocess

import numpy as np

RATE = 44100
HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src/main/resources/assets/jugcraft/sounds/restroom")
EVENTS = {"restroom.toilet_flush": ("toilet_flush", "Toilet flushes", 16), "restroom.urinal_flush": ("urinal_flush", "Urinal flushes", 16),
          "restroom.faucet": ("faucet", "Faucet runs", 12), "restroom.hand_dryer": ("hand_dryer", "Hand dryer blows", 16),
          "restroom.shower": ("shower", "Shower runs", 12)}
SOUNDS = {event: {"subtitle": f"subtitles.jugcraft.{event}", "sounds": [{"name": f"jugcraft:restroom/{name}", "attenuation_distance": dist}]}
          for event, (name, _, dist) in EVENTS.items()}
LANG = {f"subtitles.jugcraft.{event}": text for event, (_, text, _) in EVENTS.items()}


def band_noise(n, low, high, rng):
    """Noise between low and high Hz (shaped in the frequency domain, soft edges)."""
    spectrum = rng.normal(size=n // 2 + 1) + 1j * rng.normal(size=n // 2 + 1)
    f = np.fft.rfftfreq(n, 1 / RATE)
    shape = np.clip((f - low * 0.7) / (low * 0.3 + 1e-9), 0, 1) * np.clip((high * 1.3 - f) / (high * 0.3), 0, 1)
    out = np.fft.irfft(spectrum * shape, n=n)
    return out / (np.max(np.abs(out)) + 1e-12)


def envelope(t, points):
    """A piecewise-linear envelope through (time, level) points."""
    ts, ls = zip(*points)
    return np.interp(t, ts, ls)


def click(n, rng, freq=2400, decay=0.004, at=0.0):
    t = np.arange(n) / RATE
    k = np.clip(t - at, 0, None)
    return (np.sin(2 * np.pi * freq * k) * np.exp(-k / decay) + band_noise(n, 1500, 7000, rng) * np.exp(-k / (decay * 0.6)) * 0.6) * (t >= at)


def thunk(n, rng, at=0.0):
    t = np.arange(n) / RATE
    k = np.clip(t - at, 0, None)
    return (np.sin(2 * np.pi * 95 * k) * np.exp(-k / 0.05) + band_noise(n, 80, 600, rng) * np.exp(-k / 0.03) * 0.5) * (t >= at)


def gurgle(n, rng, start, length):
    """The siphon breaking: bubbles of low noise, each a little lower than the last."""
    t = np.arange(n) / RATE
    out = np.zeros(n)
    k = start
    while k < start + length:
        f0 = rng.uniform(140, 320)
        d = rng.uniform(0.03, 0.08)
        m = (t >= k) & (t < k + d * 4)
        tt = t[m] - k
        out[m] += np.sin(2 * np.pi * f0 * (1 - tt * 2) * tt) * np.exp(-tt / d) * rng.uniform(0.4, 1.0)
        k += rng.uniform(0.04, 0.12)
    return out


def toilet_flush():
    rng = np.random.default_rng(1401)
    dur = 4.6
    n = int(RATE * dur)
    t = np.arange(n) / RATE
    rush = band_noise(n, 180, 5200, rng) * 0.8 + band_noise(n, 60, 400, rng) * 0.6
    swirl = 1 + 0.35 * np.sin(2 * np.pi * 3.2 * t + 2 * np.sin(2 * np.pi * 0.7 * t))
    rush *= swirl * envelope(t, [(0, 0), (0.12, 0), (0.3, 1.0), (1.6, 0.85), (2.7, 0.45), (3.4, 0.18), (4.6, 0.0)])
    hiss = band_noise(n, 2500, 9000, rng) * envelope(t, [(0, 0), (2.4, 0), (2.9, 0.18), (4.2, 0.08), (4.6, 0)])
    out = click(n, rng) * 0.5 + thunk(n, rng, 0.08) * 0.8 + rush + hiss + gurgle(n, rng, 2.5, 0.9) * 0.6
    return out


def urinal_flush():
    rng = np.random.default_rng(1402)
    dur = 3.2
    n = int(RATE * dur)
    t = np.arange(n) / RATE
    wash = band_noise(n, 500, 7000, rng) * envelope(t, [(0, 0), (0.12, 0), (0.25, 0.8), (1.4, 0.6), (2.2, 0.2), (3.2, 0)])
    trickle = band_noise(n, 1200, 5000, rng) * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t)) * envelope(t, [(0, 0), (1.8, 0), (2.4, 0.15), (3.2, 0)])
    return click(n, rng) * 0.5 + thunk(n, rng, 0.08) * 0.5 + wash + trickle


def faucet():
    rng = np.random.default_rng(1403)
    dur = 5.0
    n = int(RATE * dur)
    t = np.arange(n) / RATE
    stream = band_noise(n, 300, 6500, rng)
    bubbles = band_noise(n, 900, 2600, rng) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 17 * t + 3 * np.sin(2 * np.pi * 1.3 * t))))
    body = (stream * 0.7 + bubbles * 0.4) * envelope(t, [(0, 0), (0.06, 0), (0.18, 0.7), (4.8, 0.7), (4.9, 0.0), (5.0, 0)])
    return click(n, rng, 1800, 0.003) * 0.35 + click(n, rng, 1800, 0.003, at=4.86) * 0.35 + body


def hand_dryer():
    rng = np.random.default_rng(1404)
    dur = 4.2
    n = int(RATE * dur)
    t = np.arange(n) / RATE
    speed = envelope(t, [(0, 0.05), (0.45, 1.0), (3.6, 1.0), (4.2, 0.2)])
    phase = 2 * np.pi * np.cumsum(820 * speed) / RATE
    whine = sum(np.sin(h * phase) * a for h, a in ((1, 0.25), (2, 0.18), (3, 0.12), (5, 0.06)))
    air = band_noise(n, 700, 9000, rng) * speed ** 1.5
    level = envelope(t, [(0, 0), (0.05, 0.4), (0.5, 1.0), (3.6, 1.0), (4.2, 0)])
    return (whine * 0.5 + air * 0.9) * level


def shower():
    """Six seconds of a shower: the spray (broad hiss), the drops drumming on the tray (low patter, uneven), a slow
    swell as the stream wanders; the first and last 0.4 s crossfaded into each other, so played back to back it never
    clicks or dips."""
    rng = np.random.default_rng(1405)
    dur, fade = 6.0, 0.4
    n, m = int(RATE * dur), int(RATE * fade)
    t = np.arange(n + m) / RATE
    spray = band_noise(n + m, 900, 9500, rng) * 0.55
    patter = band_noise(n + m, 120, 1400, rng) * (0.55 + 0.45 * np.abs(np.sin(2 * np.pi * 23 * t + 4 * np.sin(2 * np.pi * 1.9 * t))))
    swell = 1 + 0.12 * np.sin(2 * np.pi * t / dur * 2 + 0.6)
    sig = (spray + patter * 0.6) * swell
    out = sig[:n].copy()
    w = np.linspace(0, 1, m)
    out[:m] = sig[:m] * np.sqrt(w) + sig[n:n + m] * np.sqrt(1 - w)
    return out


def write(name, signal):
    os.makedirs(OUT, exist_ok=True)
    signal = np.tanh(signal / np.max(np.abs(signal)) * 1.2) / np.tanh(1.2) * 0.85
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=pcm, check=True)
    print("wrote", path)


if __name__ == "__main__":
    write("toilet_flush", toilet_flush())
    write("urinal_flush", urinal_flush())
    write("faucet", faucet())
    write("hand_dryer", hand_dryer())
    write("shower", shower())
