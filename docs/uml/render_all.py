#!/usr/bin/env python3
"""Regenera los dos UML desde sus fuentes; herramientas exclusivas de documentación."""
from pathlib import Path
import runpy, shutil, subprocess
root=Path(__file__).resolve().parent
dot=shutil.which('dot')
if dot is None:raise RuntimeError('Instala Graphviz para regenerar el UML de clases')
for fmt in ['svg','png']:
    subprocess.run([dot,'-Gdpi=180','-T'+fmt,str(root/'clases.dot'),'-o',str(root/('clases.'+fmt))],check=True)
runpy.run_path(str(root/'render_states.py'),run_name='__main__')
print('UML de clases y estados generados: SVG + PNG')
