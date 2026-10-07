"""Writes the Pixel Hollows ambient loop: assets/jugcraft/sounds/ambient/pixel_hollows_loop.ogg.

Run from the repository root:  python3 tools/pixel_hollows_sound.py   (needs ffmpeg with libvorbis)
An original, quiet, chiptune-flavoured hum, synthesised here from plain waveforms: a soft square-ish drone on
A1 with a slow swell, a faint pulse an octave up, and every four seconds a short, quiet arpeggio. Every
frequency completes whole cycles in the 16-second loop, so it repeats without a click. The synthesis is
deterministic; the encoded bytes depend on the local ffmpeg build, so this is run by hand like the textures.
"""
import math
import struct
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "sounds" / "ambient" / "pixel_hollows_loop.ogg"
RATE = 22050
SECONDS = 16
# One chord per bar (four seconds each): A minor, F major, C major, G major, as rising four-note arpeggios.
CHORDS = [(220.0, 261.63, 329.63, 440.0), (174.61, 220.0, 261.63, 349.23),
          (130.81, 164.81, 196.0, 261.63), (196.0, 246.94, 293.66, 392.0)]


def soft_square(phase, harmonics=4):
    """A square wave with only its first few odd harmonics: buzzy like a console chip, but rounded off."""
    return sum(math.sin(phase * (2 * k + 1)) / (2 * k + 1) for k in range(harmonics)) * (4 / math.pi)


def triangle(phase):
    x = (phase / (2 * math.pi)) % 1.0
    return 4 * abs(x - 0.5) - 1


def sample(t):
    swell = 0.7 + 0.3 * math.sin(2 * math.pi * 0.25 * t)
    drone = 0.12 * swell * soft_square(2 * math.pi * 55 * t)
    pulse = 0.035 * (1 if math.sin(2 * math.pi * 110 * t) > 0.6 * math.sin(2 * math.pi * 0.125 * t) else -1)
    bar, within = divmod(t, 4.0)
    arp = 0.0
    if within < 0.64:
        note, offset = divmod(within, 0.16)
        frequency = CHORDS[int(bar) % len(CHORDS)][int(note)]
        envelope = math.exp(-offset * 18) * min(1.0, offset * 400)
        arp = 0.07 * envelope * triangle(2 * math.pi * frequency * t)
    return drone + pulse + arp


def main():
    samples = [sample(i / RATE) for i in range(RATE * SECONDS)]
    peak = max(abs(s) for s in samples)
    pcm = b"".join(struct.pack("<h", int(32767 * 0.35 * s / peak)) for s in samples)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "pipe:0",
                    "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact",
                    "-c:a", "libvorbis", "-q:a", "2", str(OUT)], input=pcm, check=True)


if __name__ == "__main__":
    main()
