"""
RedNotebook FastAPI Server
Run with: uvicorn server:app --host 0.0.0.0 --port 8000
Adjust RN_DIR env var or the default path below.
"""
import os, re
from pathlib import Path
from typing import Dict, List
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="RedNotebook API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

ENTRIES_DIR = Path(os.getenv("RN_DIR", "/path/to/rednotebook/data"))

def parse_month_file(path: Path) -> Dict[int, str]:
    text = path.read_text(encoding="utf-8")
    entries: Dict[int, str] = {}
    pattern = re.compile(r"^(\d+):\s*\n\s*text:\s*'(.*?)'", re.DOTALL | re.MULTILINE)
    for m in pattern.finditer(text):
        entries[int(m.group(1))] = m.group(2).strip()
    return entries

def write_month_file(path: Path, entries: Dict[int, str]) -> None:
    lines = []
    for day in sorted(entries.keys()):
        t = entries[day].replace("'", "\\'")
        lines.append(f"{day}:\n  text: '{t}\n\n    '\n")
    path.write_text("\n".join(lines), encoding="utf-8")

def month_path(year: int, month: int) -> Path:
    return ENTRIES_DIR / f"{year:04d}-{month:02d}.txt"

class EntryBody(BaseModel): text: str
class MonthRef(BaseModel): year: int; month: int
class SearchResult(BaseModel): year: int; month: int; day: int; snippet: str
class Entry(BaseModel): year: int; month: int; day: int; text: str

@app.get("/months", response_model=List[MonthRef])
def get_months():
    return [MonthRef(year=int(f.stem[:4]), month=int(f.stem[5:7]))
            for f in sorted(ENTRIES_DIR.glob("????-??.txt"))]

@app.get("/entries/{year}/{month}", response_model=Dict[str, str])
def get_month_entries(year: int, month: int):
    path = month_path(year, month)
    if not path.exists(): return {}
    return {str(k): v for k, v in parse_month_file(path).items()}

@app.get("/entries/{year}/{month}/{day}", response_model=Entry)
def get_entry(year: int, month: int, day: int):
    path = month_path(year, month)
    if not path.exists(): raise HTTPException(404, "Month not found")
    entries = parse_month_file(path)
    if day not in entries: raise HTTPException(404, "Day not found")
    return Entry(year=year, month=month, day=day, text=entries[day])

@app.put("/entries/{year}/{month}/{day}")
def save_entry(year: int, month: int, day: int, body: EntryBody):
    path = month_path(year, month)
    entries = parse_month_file(path) if path.exists() else {}
    entries[day] = body.text
    write_month_file(path, entries)
    return {"status": "ok"}

@app.delete("/entries/{year}/{month}/{day}")
def delete_entry(year: int, month: int, day: int):
    path = month_path(year, month)
    if not path.exists(): raise HTTPException(404, "Month not found")
    entries = parse_month_file(path)
    if day not in entries: raise HTTPException(404, "Day not found")
    del entries[day]
    write_month_file(path, entries)
    return {"status": "deleted"}

@app.get("/search", response_model=List[SearchResult])
def search(q: str):
    results = []
    for f in sorted(ENTRIES_DIR.glob("????-??.txt")):
        year, month = int(f.stem[:4]), int(f.stem[5:7])
        for day, text in parse_month_file(f).items():
            if q.lower() in text.lower():
                idx = text.lower().find(q.lower())
                start = max(0, idx - 40)
                snippet = ("…" if start > 0 else "") + text[start:idx+80].strip() + "…"
                results.append(SearchResult(year=year, month=month, day=day, snippet=snippet))
    return results
