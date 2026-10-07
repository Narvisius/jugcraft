"""Original sounds for Pumpkin Night (tools/decor20.py), synthesised here (no recorded or third-party audio).

The Singing Pumpkins' voices: for each of the four voices, two sung vowels ("ah" and "oh", picked at random each time
it sings) on the voice's middle note, which the game raises or lowers like a note block. Each vowel is built
harmonic by harmonic: a glottal source whose harmonics fall away more slowly the brighter the voice, shaped by the
vowel's three formants (second-order resonances), with a gentle vibrato that comes in after the onset, three slightly
detuned singers for a warm chorus, and a breath of noise. And the Harvest Effigy catching: a rising roar of flame
with crackles. Writes assets/jugcraft/sounds/choir/*.ogg and effigy/*.ogg (needs numpy and ffmpeg with libvorbis).
"""
import os
import subprocess
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import decor20  # noqa: E402

RATE = 44100
MOD = "jugcraft"
ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jugcraft/sounds")
VOWELS = ["ah", "oh"]
SING_SECONDS = 1.4
BURN_SECONDS = 2.6
# Each formant's strength (relative to the first) and bandwidth in Hz.
FORMANT_GAIN = [1.0, 0.5, 0.22]
FORMANT_WIDTH = [80.0, 100.0, 130.0]
# The chorus: each singer's detuning in cents and vibrato phase.
SINGERS = [(0.0, 0.0), (-7.0, 2.1), (6.0, 4.0)]


def sound_name(voice, vowel):
    return f"{MOD}:choir/{voice}_{vowel}"


# Their entries in assets/jugcraft/sounds.json (written with every other sound by tools/generate_material_data.py).
SOUNDS = {f"singing_pumpkin.{voice}": {"subtitle": f"subtitles.{MOD}.singing_pumpkin.{voice}",
                                       "sounds": [{"name": sound_name(voice, vowel), "attenuation_distance": 32} for vowel in VOWELS]}
          for voice in decor20.VOICES}
SOUNDS["effigy.burn"] = {"subtitle": f"subtitles.{MOD}.effigy.burn",
                         "sounds": [{"name": f"{MOD}:effigy/effigy_burn", "attenuation_distance": 32}]}


def envelope(n, attack, release):
    t = np.arange(n) / RATE
    env = np.minimum(1.0, t / attack)
    tail = (n / RATE) - t
    return env * np.clip(tail / release, 0.0, 1.0) ** 1.5


def formant_gain(freq, formants):
    """How strongly the vowel's resonances pass a harmonic at {@code freq} Hz."""
    total = np.zeros_like(freq)
    for centre, gain, width in zip(formants, FORMANT_GAIN, FORMANT_WIDTH):
        ratio = freq / centre
        total += gain / np.sqrt((1 - ratio ** 2) ** 2 + (freq * width / centre ** 2) ** 2)
    return total


def sing(hz, formants, brightness, seed):
    n = int(RATE * SING_SECONDS)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(seed)
    # Vibrato: 5.4 Hz, a little over a quarter tone deep, easing in after the first 0.25 s.
    vibrato_depth = 0.012 * np.clip((t - 0.25) / 0.35, 0.0, 1.0)
    tilt = 1.9 - 0.9 * brightness
    out = np.zeros(n)
    for cents, phase in SINGERS:
        f0 = hz * 2 ** (cents / 1200) * (1 + vibrato_depth * np.sin(2 * np.pi * 5.4 * t + phase))
        # A touch of drift, as a held note wanders.
        f0 *= 1 + 0.002 * np.sin(2 * np.pi * 0.7 * t + phase * 1.7)
        cycles = np.cumsum(f0) / RATE
        for k in range(1, int(9000 / hz) + 1):
            freq = f0 * k
            amp = formant_gain(freq, formants) * k ** -tilt
            out += amp * np.sin(2 * np.pi * k * cycles + rng.uniform(0, 2 * np.pi))
    out /= np.max(np.abs(out))
    breath = rng.normal(size=n)
    breath = np.convolve(breath, np.ones(6) / 6, mode="same") * 0.025 * (0.5 + brightness)
    return (out + breath) * envelope(n, 0.07, 0.45)


def burn(seed):
    """A roar of catching flame: noise opening from a low rumble to a bright rush, then settling, with crackles."""
    n = int(RATE * BURN_SECONDS)
    t = np.arange(n) / RATE
    rng = np.random.default_rng(seed)
    noise = rng.normal(size=n)
    # A one-pole low-pass whose corner climbs from 250 Hz to 3.2 kHz over 0.5 s and sinks back to 900 Hz.
    corner = np.where(t < 0.5, 250 + (3200 - 250) * (t / 0.5) ** 0.7, 900 + 2300 * np.exp(-(t - 0.5) * 2.2))
    alpha = 1 - np.exp(-2 * np.pi * corner / RATE)
    roar = np.empty(n)
    level = 0.0
    for i in range(n):
        level += alpha[i] * (noise[i] - level)
        roar[i] = level
    roar /= np.max(np.abs(roar))
    rumble = np.sin(2 * np.pi * 48 * t + 0.6 * np.sin(2 * np.pi * 3.1 * t)) * 0.35
    crackle = np.zeros(n)
    for _ in range(90):
        at = rng.integers(int(0.15 * RATE), n - 400)
        length = rng.integers(40, 260)
        crackle[at:at + length] += rng.normal(size=length) * np.exp(-np.arange(length) / (length / 4)) * rng.uniform(0.4, 1.0)
    swell = np.clip(t / 0.35, 0, 1) ** 1.5
    return (roar * 0.9 + rumble * swell + crackle * 0.5) * swell * envelope(n, 0.02, 1.2)


def write(folder, name, signal):
    out = os.path.join(ROOT, folder)
    os.makedirs(out, exist_ok=True)
    signal = signal / np.max(np.abs(signal)) * 0.8
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(out, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=pcm, check=True)
    print("wrote", path)


if __name__ == "__main__":
    for index, (voice, spec) in enumerate(decor20.VOICES.items()):
        for vowel, formants in zip(VOWELS, spec["formants"]):
            write("choir", f"{voice}_{vowel}", sing(spec["hz"], formants, spec["brightness"], seed=17 + index * 2 + VOWELS.index(vowel)))
    write("effigy", "effigy_burn", burn(seed=31))
