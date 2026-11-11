"""Puts the squad film together from what the game wrote (client/FilmShoot, client/FilmCapture): the frames (JPEGs in
frames-T-N.mjpg, each one's place and time in frames.csv) laid out at 30 a second by their times, and the game's own
sound (film-audio.wav, OpenAL's wave writer) lined up by the marker sound played before rolling (and checked against the
one played after), into an H.264/AAC mp4.

Usage: python3 tools/film_assemble.py <film folder> <marker ogg> <out.mp4> [--no-fade] [--crf N]
"""
import csv, os, re, struct, subprocess, sys, tempfile
import numpy as np

SR = 48000
FPS = 30


def read_scene(folder):
    s = open(os.path.join(folder, "scene.txt"), encoding="utf-8").read()
    m = re.search(r"marker (\d+) rolled (\d+) end marker (\d+)", s)
    return int(m.group(1)), int(m.group(2)), int(m.group(3)), s


def read_frames(folder):
    rows = []
    with open(os.path.join(folder, "frames.csv"), newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            rows.append(dict(frame=int(r["frame"]), file=r["file"], offset=int(r["offset"]), length=int(r["length"]),
                             nanos=int(r["nanos"]), w=int(r["width"]), h=int(r["height"]), tag=r["tag"]))
    rows.sort(key=lambda r: r["nanos"])
    return rows


def load_wav(path):
    """The wave file as float stereo; its sizes put right if the writer never finished them (the game killed)."""
    data = open(path, "rb").read()
    if data[:4] != b"RIFF":
        raise SystemExit("not a wave file: " + path)
    pos, fmt, raw = 12, None, None
    while pos + 8 <= len(data):
        cid, size = data[pos:pos + 4], struct.unpack("<I", data[pos + 4:pos + 8])[0]
        body = pos + 8
        if cid == b"fmt ":
            fmt = data[body:body + size]
        elif cid == b"data":
            if size == 0 or body + size > len(data):
                size = len(data) - body
            raw = data[body:body + size]
            break
        pos = body + size + (size & 1)
    tag, ch, sr, _, align, bits = struct.unpack("<HHIIHH", fmt[:16])
    if tag == 0xFFFE:
        tag = struct.unpack("<H", fmt[24:26])[0]
    n = len(raw) // align
    if tag == 3:
        x = np.frombuffer(raw[:n * align], dtype="<f4").reshape(n, ch).astype(np.float64)
    elif bits == 16:
        x = np.frombuffer(raw[:n * align], dtype="<i2").reshape(n, ch).astype(np.float64) / 32768
    elif bits == 32:
        x = np.frombuffer(raw[:n * align], dtype="<i4").reshape(n, ch).astype(np.float64) / 2 ** 31
    else:
        raise SystemExit("wave format %d/%d bits not handled" % (tag, bits))
    if ch == 1:
        x = np.repeat(x, 2, axis=1)
    return sr, x[:, :2]


def decode(path, sr):
    out = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-ac", "1", "-ar", str(sr), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(out, dtype="<f4").astype(np.float64)


def find_markers(mono, mark, sr, gap_s):
    """The two marker sounds: the pair of strongest matches about {gap_s} seconds apart (their starts, in samples)."""
    n = len(mono) + len(mark)
    size = 1 << (n - 1).bit_length()
    corr = np.fft.irfft(np.fft.rfft(mono, size) * np.conj(np.fft.rfft(mark, size)), size)[:len(mono)]
    # normalised by the signal's energy under the marker
    energy = np.sqrt(np.convolve(mono ** 2, np.ones(len(mark)), "valid"))
    energy = np.concatenate([energy, np.full(len(mono) - len(energy), energy[-1] if len(energy) else 1)])
    score = corr / (energy * np.sqrt(np.sum(mark ** 2)) + 1e-9)
    order = np.argsort(score)[::-1]
    peaks = []
    for i in order[:20000]:
        if all(abs(i - p) > sr * 0.3 for p in peaks):
            peaks.append(int(i))
        if len(peaks) >= 12:
            break
    best = None
    for a in peaks:
        for b in peaks:
            if b > a:
                err = abs((b - a) / sr - gap_s)
                s = score[a] + score[b] - err * 2
                if err < 0.5 and (best is None or s > best[0]):
                    best = (s, a, b, score[a], score[b])
    if best is None:
        a = peaks[0]
        return a, None, score[a], 0
    return best[1], best[2], best[3], best[4]


def main():
    folder, mark_path, out = sys.argv[1:4]
    fade = "--no-fade" not in sys.argv
    crf = sys.argv[sys.argv.index("--crf") + 1] if "--crf" in sys.argv else "18"
    mark_ns, rolled_ns, end_ns, scene = read_scene(folder)
    frames = read_frames(folder)
    if not frames:
        raise SystemExit("no frames")
    t0, t1 = frames[0]["nanos"], frames[-1]["nanos"]
    n_out = int((t1 - t0) * FPS / 1e9) + 1
    print("frames %d over %.2f s -> %d at %d fps (%.1f%% new)" % (len(frames), (t1 - t0) / 1e9, n_out, FPS, 100 * len(frames) / n_out))
    work = tempfile.mkdtemp(prefix="film_", dir=os.path.dirname(os.path.abspath(out)))
    # the JPEGs, in time order
    handles = {}
    src = []
    for i, r in enumerate(frames):
        f = handles.get(r["file"]) or handles.setdefault(r["file"], open(os.path.join(folder, r["file"]), "rb"))
        f.seek(r["offset"])
        p = os.path.join(work, "f%06d.jpg" % i)
        with open(p, "wb") as o:
            o.write(f.read(r["length"]))
        src.append(p)
    # 30 a second: each picture the nearest frame taken at or before its time (a frame late is shown again)
    times = np.array([r["nanos"] for r in frames], dtype=np.int64)
    gaps = 0
    for k in range(n_out):
        t = t0 + int(k * 1e9 / FPS)
        j = int(np.searchsorted(times, t + int(0.5e9 / FPS), side="right")) - 1
        j = max(0, j)
        if k > 0 and j == last:
            gaps += 1
        last = j
        os.symlink(src[j], os.path.join(work, "o%06d.jpg" % k))
    print("pictures shown twice: %d" % gaps)
    # the sound, lined up by the marker
    wav = os.path.join(folder, "film-audio.wav")
    audio = None
    if os.path.exists(wav):
        sr, x = load_wav(wav)
        mark = decode(mark_path, sr)
        mono = x.mean(axis=1)
        a, b, sa, sb = find_markers(mono, mark, sr, (end_ns - mark_ns) / 1e9)
        rate = 1.0
        if b is not None:
            rate = (b - a) / sr / ((end_ns - mark_ns) / 1e9)
        print("marker at %.3f s (match %.2f), end marker %s (match %.2f); clock ratio %.6f" % (
            a / sr, sa, "%.3f s" % (b / sr) if b is not None else "not found", sb, rate))
        start = a + (t0 - mark_ns) / 1e9 * sr * rate
        length = int(n_out / FPS * sr)
        s0 = int(round(start))
        seg = np.zeros((length, 2))
        lo, hi = max(0, s0), min(len(x), s0 + length)
        if hi > lo:
            seg[lo - s0:hi - s0] = x[lo:hi]
        peak = np.max(np.abs(seg))
        print("sound from %.3f s, %d samples, peak %.3f" % (start / sr, length, peak))
        if peak > 0.99:
            seg *= 0.99 / peak
        audio = os.path.join(work, "audio.wav")
        from scipy.io import wavfile
        wavfile.write(audio, sr, (np.clip(seg, -1, 1) * 32767).astype(np.int16))
    dur = n_out / FPS
    vf = "scale=1920:-2:flags=lanczos"
    af = None
    if fade:
        vf += ",fade=t=in:st=0:d=0.6,fade=t=out:st=%.3f:d=1.2" % (dur - 1.2)
        af = "afade=t=in:st=0:d=0.4,afade=t=out:st=%.3f:d=1.2" % (dur - 1.2)
    cmd = ["ffmpeg", "-loglevel", "error", "-y", "-framerate", str(FPS), "-i", os.path.join(work, "o%06d.jpg")]
    if audio:
        cmd += ["-i", audio]
    cmd += ["-vf", vf, "-c:v", "libx264", "-preset", "slow", "-crf", crf, "-pix_fmt", "yuv420p", "-r", str(FPS)]
    if audio:
        cmd += ["-c:a", "aac", "-b:a", "224k"] + (["-af", af] if af else [])
    cmd += ["-movflags", "+faststart", out]
    subprocess.run(cmd, check=True)
    print("wrote", out, "%.1f s" % dur)


if __name__ == "__main__":
    main()
