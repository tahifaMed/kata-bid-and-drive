#!/usr/bin/env python3
"""PreToolUse guard for Bash calls. Exit code 2 blocks the call and returns stderr to Claude.

Enforces the git rules in CLAUDE.md: no force push, no --no-verify, no commits or pushes
on main, branch names as <type>/<NNN>-<slug>, Conventional Commit subjects of at most 60 chars.
"""
import json
import re
import subprocess
import sys

BRANCH_RE = r"^(feat|fix|refactor|test|docs|chore)/\d{3}-[a-z0-9-]+$"
SUBJECT_RE = r"^(feat|fix|refactor|test|docs|chore)(\([a-z0-9-]+\))?: [a-z].*[^.]$"


def block(message):
    print(f"BLOCKED: {message}", file=sys.stderr)
    sys.exit(2)


def commit_subject(command):
    """Subject of the git commit on the line that runs it, or None if it cannot be read.

    Only the commit line is inspected, so text that merely mentions 'git commit'
    (for example a script that writes commit messages) is not judged.
    """
    lines = command.split("\n")
    for index, line in enumerate(lines):
        # Only a real command start counts, not git commit text inside quotes
        if not re.search(r"(^|[;&|(]\s*|\$\(\s*)git\s+commit\b", line):
            continue
        heredoc = re.search(r"<<-?'?(\w+)'?", line)
        if heredoc:
            # Body lines run until the terminator; the first non-empty one is the subject
            body = []
            for body_line in lines[index + 1:]:
                if body_line.strip() == heredoc.group(1):
                    break
                body.append(body_line)
            return next((b for b in body if b.strip()), "").strip()
        message = re.search(r"-m\s+(\"([^\"]*)\"|'([^']*)')", line)
        if message:
            return (message.group(2) or message.group(3) or "").split("\n")[0]
    return None


def current_branch():
    # --show-current also reports "main" before the first commit, unlike rev-parse
    result = subprocess.run(
        ["git", "branch", "--show-current"], capture_output=True, text=True
    )
    return result.stdout.strip()


command = json.load(sys.stdin).get("tool_input", {}).get("command", "")

if re.search(r"\bgit\s+push\b.*(--force|--force-with-lease|\s-f\b)", command):
    block("force push is not allowed. Push a new commit instead.")

if "--no-verify" in command:
    block("--no-verify is not allowed. Fix the failing hook instead.")

if re.search(r"\bgit\s+push\b", command):
    if current_branch() == "main" or re.search(r"\bgit\s+push\b.*\bmain\b", command):
        block("never push to main. Push a feature branch and open a PR.")

branch_create = re.search(r"\bgit\s+(?:checkout\s+-b|switch\s+-c)\s+(\S+)", command)
if branch_create and not re.match(BRANCH_RE, branch_create.group(1)):
    block(f"branch '{branch_create.group(1)}' must match <type>/<NNN>-<slug>, e.g. feat/001-create-auction.")

if re.search(r"\bgit\s+commit\b", command):
    if current_branch() == "main":
        block("never commit on main. Create a branch first: <type>/<NNN>-<slug>.")
    subject = commit_subject(command)
    if subject is not None:
        if not re.match(SUBJECT_RE, subject) or len(subject) > 60:
            block(
                f"commit subject '{subject}' must be 'type(scope): summary', lowercase, "
                "no trailing period, at most 60 chars."
            )

sys.exit(0)
