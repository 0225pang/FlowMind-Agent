from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def run_step(name: str, args: list[str], env: dict[str, str] | None = None) -> None:
    print(f"[desktop check] {name}")
    merged_env = os.environ.copy()
    if env:
        merged_env.update(env)
    subprocess.run(args, cwd=ROOT, env=merged_env, check=True)


def main() -> None:
    python = sys.executable
    run_step("compile", [python, "-m", "compileall", "-q", "."])
    run_step("api fallback smoke", [python, "smoke_test.py"])
    run_step("pyside offscreen smoke", [python, "pyside_smoke_test.py"], {"QT_QPA_PLATFORM": "offscreen"})
    print("[desktop check] all checks passed")


if __name__ == "__main__":
    main()
