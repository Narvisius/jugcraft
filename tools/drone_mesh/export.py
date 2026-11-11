"""Exports the drones' detailed meshes for the game (client/DroneMesh.java).

drones.js holds the approved drone models of the October 2026 redesign (the same builders the mockup renders
use). This opens export.html in headless Chromium (Playwright), builds every tier in four variants (parked,
hover, cruise, empty), splits off the rotors and fans that turn, and writes
  assets/jugcraft/drone_mesh/t1.bin .. t9.bin  (gzip; header JDM2, see DroneMesh.load)
  assets/jugcraft/textures/entity/drone_fleet.png  (the labels and hazard stripes, white corner for plain colour)
Run: python tools/drone_mesh/export.py  (needs: pip install playwright; a Chromium playwright can launch).
export.html loads three.js r128 (MIT) from lib/three.min.js. When that file is not there (it is not vendored), this
serves lib/three_subset.js in its place: a stand-in holding only the parts drones.js uses, following r128's geometry
code, so the meshes come out the same.
"""
import asyncio, base64, functools, gzip, http.server, pathlib, threading
from playwright.async_api import async_playwright

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent.parent / "src/main/resources/assets/jugcraft"


class SubsetHandler(http.server.SimpleHTTPRequestHandler):
    def translate_path(self, path):
        if path.split("?")[0].endswith("/lib/three.min.js"):
            path = "/lib/three_subset.js"
        return super().translate_path(path)

    def log_message(self, *args):
        pass


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(args=["--allow-file-access-from-files", "--js-flags=--max-old-space-size=4096"])
        page = await browser.new_page()
        errors = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.on("console", lambda m: print("console:", m.text) if m.type in ("warning", "error") else None)
        url = f"file://{HERE}/export.html"
        if not (HERE / "lib/three.min.js").exists():
            # serve this folder over loopback HTTP so the request for lib/three.min.js can be answered with the subset
            print("lib/three.min.js not found: using lib/three_subset.js")
            server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), functools.partial(SubsetHandler, directory=str(HERE)))
            threading.Thread(target=server.serve_forever, daemon=True).start()
            url = f"http://127.0.0.1:{server.server_address[1]}/export.html"
        await page.goto(url)
        await page.wait_for_function("window.__done===true", timeout=600000)
        if errors:
            raise SystemExit(errors)
        atlas = await page.evaluate("window.__atlas")
        (OUT / "textures/entity/drone_fleet.png").write_bytes(base64.b64decode(atlas.split(",")[1]))
        (OUT / "drone_mesh").mkdir(exist_ok=True)
        for tier in range(1, 10):
            raw = base64.b64decode(await page.evaluate(f"encode({tier})"))
            (OUT / f"drone_mesh/t{tier}.bin").write_bytes(gzip.compress(raw, 9, mtime=0))
            print(tier, len(raw))
        await browser.close()


asyncio.run(main())
