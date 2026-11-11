"""No method may hand its own arguments straight back to itself: such a call can never end (it runs until the stack
overflows and the game closes).

Why this exists: r186 moved every display's text over to client/Texts.of with a replace across the client's sources, and
the replace rewrote the two lines inside Texts.of too, so it called itself. Nothing here runs the client, so it went out
in eleven releases and closed Justin's game on the first frame that drew a display (10 November 2026).

    python3 tools/check_self_calls.py        every .java under src/; exit code 1 if it finds one

A method with no parameters that calls itself is only listed (a state machine stepping on does that and ends).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NOT_METHODS = {"if", "for", "while", "switch", "catch", "synchronized", "return", "new", "else", "do", "try"}
THROWS = re.compile(r"\)\s*(?:throws\s+[\w.,\s]+?)?\s*$")
NAME = re.compile(r"(\w+|[\]>])\s+(\w+)\s*$")
NOT_TYPES = {"record", "new", "class", "interface", "enum", "return", "else", "throw"}     # (a record's header is not a method)


def strip(src):
    """The source with comments and string and character literals blanked (same length, same lines)."""
    out, i, n = [], 0, len(src)
    while i < n:
        c, d = src[i], src[i:i + 2]
        if d == "//":
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif d == "/*":
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j]))
            i = j
        elif src[i:i + 3] == '"""':
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j]))
            i = j
        elif c in "\"'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            out.append(c + " " * (j - i - 1) + c)
            i = j + 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


def body(src, start):
    """The text between the brace at start and its match."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
    return src[start + 1:]


def names(params):
    out = []
    depth = 0
    cur = ""
    for ch in params + ",":
        if ch in "<(":
            depth += 1
        elif ch in ">)":
            depth -= 1
        if ch == "," and depth == 0:
            cur = cur.strip()
            if cur:
                out.append(re.sub(r"\[\s*\]", "", cur).split()[-1].lstrip("."))
            cur = ""
        else:
            cur += ch
    return out


def methods(src):
    """(name, parameter names, the position of the body's opening brace) for each method declared in src: every brace
    that follows a closing parenthesis (and a throws clause), whose parentheses follow a name that follows a type."""
    for m in re.finditer(r"\{", src):
        head = src[max(0, m.start() - 400):m.start()]
        t = THROWS.search(head)
        if not t:
            continue
        close = max(0, m.start() - 400) + t.start()
        depth, i = 0, close
        while i >= 0:
            if src[i] == ")":
                depth += 1
            elif src[i] == "(":
                depth -= 1
                if depth == 0:
                    break
            i -= 1
        if i < 0:
            continue
        n = NAME.search(src[max(0, i - 200):i])
        if not n or n.group(2) in NOT_METHODS or n.group(1) in NOT_TYPES:
            continue
        yield n.group(2), names(src[i + 1:close]), m.start()


def check(path):
    src = strip(path.read_text(encoding="utf-8", errors="replace"))
    found, noted = [], []
    for name, params, at in methods(src):
        if name not in src[at:at + 200000]:
            continue
        inner = body(src, at)
        args = r"\s*,\s*".join(re.escape(p) for p in params)
        call = re.compile(r"(?<![\w.])(?:(?:this|%s)\s*\.\s*)?%s\s*\(\s*%s\s*\)" % (re.escape(path.stem), re.escape(name), args))
        hit = next((h for h in call.finditer(inner) if not re.search(r"\bnew\s*$", inner[max(0, h.start() - 12):h.start()])), None)
        if hit:
            line = src.count("\n", 0, at + 1 + hit.start()) + 1
            (found if params else noted).append((line, name, params))
    return found, noted


def main():
    bad = notes = files = 0
    for path in sorted((ROOT / "src").rglob("*.java")):
        files += 1
        found, noted = check(path)
        for line, name, params in found:
            bad += 1
            print(f"ENDLESS  {path.relative_to(ROOT)}:{line}  {name}({', '.join(params)}) calls itself with its own arguments")
        for line, name, params in noted:
            notes += 1
            print(f"note     {path.relative_to(ROOT)}:{line}  {name}() calls itself")
    print(f"{files} files: {bad} endless, {notes} noted")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
