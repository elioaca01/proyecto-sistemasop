"""Empaqueta exclusivamente HEAD y registra hashes para comprobar la entrega."""
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path, PurePosixPath
import subprocess
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
PREFIX = "proyecto-sistemasop/"


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args])


def main():
    if git("status", "--porcelain").strip():
        raise SystemExit("Primero guardar los cambios del proyecto en un commit.")
    revision = git("rev-parse", "HEAD").decode().strip()
    archive = git("archive", "--format=zip", "--prefix=" + PREFIX, revision)
    pdf_hashes = {}
    with ZipFile(BytesIO(archive)) as zipped:
        for name in zipped.namelist():
            parts = PurePosixPath(name).parts[1:]
            if any(p in {".git", ".maven-cache", ".superpowers", "target"} for p in parts):
                raise SystemExit("Archivo excluido presente: " + name)
        for name in ("avilaos-informe.pdf", "avilaos-defensa-resumen.pdf"):
            data = zipped.read(PREFIX + "output/pdf/" + name)
            pdf_hashes[name] = sha256(data).hexdigest()
        count = sum(not entry.is_dir() for entry in zipped.infolist())
    output = ROOT / "output/entrega"
    output.mkdir(parents=True, exist_ok=True)
    filename = "proyecto-sistemasop-" + revision[:7] + ".zip"
    (output / filename).write_bytes(archive)
    manifest = {
        "sourceRevision": revision,
        "archive": filename,
        "sha256": sha256(archive).hexdigest(),
        "files": count,
        "pdfSha256": pdf_hashes,
        "note": "Fuente versionada sin historial .git, caches ni target; el historial se conserva en el repositorio.",
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(output / filename)
    print("SHA256:", manifest["sha256"], "| archivos:", count)


if __name__ == "__main__":
    main()
