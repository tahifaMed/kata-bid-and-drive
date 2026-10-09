#!/usr/bin/env python3
"""PostToolUse hook: run Prettier on frontend files Claude just edited or wrote."""
import json
import os
import subprocess
import sys

FORMATTED_SUFFIXES = (".ts", ".vue", ".js", ".json", ".css")

project_dir = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
file_path = json.load(sys.stdin).get("tool_input", {}).get("file_path", "")
frontend_dir = os.path.join(project_dir, "frontend")

if file_path.startswith(frontend_dir + os.sep) and file_path.endswith(FORMATTED_SUFFIXES):
    subprocess.run(
        ["npx", "prettier", "--write", file_path],
        cwd=frontend_dir,
        capture_output=True,
    )

sys.exit(0)
