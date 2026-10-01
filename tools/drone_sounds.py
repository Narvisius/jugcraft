"""Original drone sounds, synthesised here (no recorded or third-party audio).

Two seamless 2-second mono loops: drone_hum (tiers 1-4, small quad rotors, a bright buzz) and
drone_hum_heavy (tiers 5-9, big lift fans and tilt-rotors, a deep throb). Every tone is a whole number of
cycles in the loop and the rotor wash is noise shaped in the frequency domain, so both loop with no click.
Writes assets/jugcraft/sounds/drone/*.ogg (needs numpy and ffmpeg with libvorbis).
"""
import os
import subprocess
import numpy as np

RATE = 44100
LANG = {"subtitles.jugcraft.drone.hum": "Drone buzzes"}
SECONDS = 2.0
N = int(RATE * SECONDS)
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jugcraft/sounds/drone")


def tone(freq, harmonics, phase=0.0):
    """A rotor's blade-pass tone: harmonics with falling strength (freq rounded to whole cycles per loop)."""
    t = np.arange(N) / RATE
    f = round(freq * SECONDS) / SECONDS
    out = np.zeros(N)
    for k, amp in enumerate(harmonics, start=1):
        out += amp * np.sin(2 * np.pi * f * k * t + phase * k)
    return out


def wash(low, high, seed):
    """Rotor wash: noise band-limited between low and high Hz, periodic over the loop."""
    rng = np.random.default_rng(seed)
    spectrum = rng.normal(size=N // 2 + 1) + 1j * rng.normal(size=N // 2 + 1)
    freqs = np.fft.rfftfreq(N, 1 / RATE)
    shape = np.exp(-0.5 * ((np.log(np.maximum(freqs, 1)) - np.log(np.sqrt(low * high))) / (np.log(high / low) / 2)) ** 2)
    noise = np.fft.irfft(spectrum * shape, n=N)
    return noise / np.max(np.abs(noise))


def wobble(rate, depth):
    """Slow amplitude beat between rotors (whole cycles per loop)."""
    t = np.arange(N) / RATE
    r = round(rate * SECONDS) / SECONDS
    return 1 + depth * np.sin(2 * np.pi * r * t)


def small():
    # Four small rotors a few Hz apart beat against each other: the familiar quadcopter buzz.
    rotors = sum(tone(f, [1, 0.55, 0.35, 0.2, 0.12, 0.06], p) for f, p in ((176, 0), (181, 1.1), (186.5, 2.3), (191, 0.4)))
    motor = tone(1410, [0.25, 0.08]) * wobble(3, 0.4)
    return rotors / 4 * wobble(5.5, 0.12) + 0.35 * wash(500, 4000, 1) + motor * 0.4


def heavy():
    # Large lift fans: a low throb with a turbine whine over it.
    fans = sum(tone(f, [1, 0.7, 0.45, 0.3, 0.18], p) for f, p in ((62, 0), (64.5, 0.7), (93, 1.9)))
    whine = tone(820, [0.3, 0.1]) * wobble(1.5, 0.3)
    return fans / 3 * wobble(4, 0.25) + 0.45 * wash(120, 1500, 2) + whine * 0.5


def write(name, signal):
    os.makedirs(OUT, exist_ok=True)
    signal = signal / np.max(np.abs(signal)) * 0.8
    pcm = (signal * 32767).astype("<i2").tobytes()
    path = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=pcm, check=True)
    print("wrote", path)


if __name__ == "__main__":
    write("drone_hum", small())
    write("drone_hum_heavy", heavy())
