"""Keep the visible refinery plumbing attached to its actual working sockets."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from coastal_metals_models import FORMS, MODEL_FACTORIES, PIPE_PATHS


class CoastalLayoutTests(unittest.TestCase):
    def test_every_fluid_socket_reaches_a_process_body(self):
        for name, factory in MODEL_FACTORIES.items():
            form = FORMS[name]
            ports = [p for p in form["ports"] if p[1].startswith("FLUID")]
            self.assertEqual({(p[1], p[2]) for p in ports}, set(PIPE_PATHS[name]))
            body = factory()  # Deliberately excludes the socket and its added pipe.
            for _, kind, target, col, row, layer, side in ports:
                with self.subTest(machine=name, kind=kind, tank=target):
                    self.assertEqual(side, "BACK")
                    points = [(8 - 16 * col, 8 + 16 * layer,
                               16 * len(form["layers"][0]) - .5)]
                    points = PIPE_PATHS[name][kind, target]
                    for a, b in zip(points, points[1:]):
                        self.assertEqual(sum(x != y for x, y in zip(a, b)), 1)
                    endpoint = points[-1]
                    self.assertTrue(any(all(element[0][i] <= endpoint[i] <=
                                            element[1][i] for i in range(3))
                                        for element in body),
                                    f"Pipe ends outside its machine at {endpoint}")


if __name__ == "__main__":
    unittest.main()
