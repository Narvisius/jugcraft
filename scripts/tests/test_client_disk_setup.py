"""Validate the hosted-only cleanup boundary without deleting any installed SDK."""
import os
from pathlib import Path
import shutil
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]
BASH = shutil.which("bash")
if os.name == "nt" and Path("C:/Program Files/Git/bin/bash.exe").is_file():
    BASH = "C:/Program Files/Git/bin/bash.exe"

HARNESS = r'''
sudo() {
  printf 'CLEANUP %s\n' "$*"
  [[ "$*" == 'rm -rf -- /usr/local/lib/android /usr/share/dotnet /opt/ghc /usr/local/.ghcup' ]] || return 99
  return "$CLEANUP_EXIT"
}
df() { echo DISK_REPORT; }
source scripts/prepare_client_disk.sh
'''


@unittest.skipUnless(BASH, "Bash is required for the Ubuntu CI setup script")
class ClientDiskSetupTests(unittest.TestCase):
    def run_case(self, actions, runner, cleanup_exit=0):
        return subprocess.run(
            [BASH, "-c", HARNESS], cwd=ROOT,
            env={**os.environ, "GITHUB_ACTIONS": actions, "RUNNER_ENVIRONMENT": runner,
                 "CLEANUP_EXIT": str(cleanup_exit)},
            text=True, capture_output=True, timeout=20,
        )

    def test_local_machine_never_reaches_privileged_cleanup(self):
        result = self.run_case("", "github-hosted")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertNotIn("CLEANUP", result.stdout)

    def test_self_hosted_runner_never_reaches_privileged_cleanup(self):
        result = self.run_case("true", "self-hosted")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertNotIn("CLEANUP", result.stdout)

    def test_hosted_runner_only_removes_unused_fixed_sdk_directories(self):
        result = self.run_case("true", "github-hosted")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(1, result.stdout.count("CLEANUP"))
        self.assertIn("DISK_REPORT", result.stdout)

    def test_cleanup_failure_stops_setup(self):
        result = self.run_case("true", "github-hosted", cleanup_exit=42)
        self.assertEqual(42, result.returncode, result.stdout + result.stderr)
        self.assertNotIn("DISK_REPORT", result.stdout)


if __name__ == "__main__":
    unittest.main()
