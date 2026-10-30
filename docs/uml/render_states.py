#!/usr/bin/env python3
"""UML de estados: un modelo de transiciones genera DOT, SVG y PNG con layout fijo."""
from pathlib import Path
import html, math
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parent
W,H,SCALE=1100,1040,2
BLUE,RED,INK='#476477','#A34529','#183648'
FONT=next((p for p in [Path('/System/Library/Fonts/Supplemental/Arial.ttf'),Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),Path('C:/Windows/Fonts/arial.ttf')] if p.exists()),None)
if FONT is None: raise RuntimeError('Se necesita Arial o DejaVuSans para generar el PNG de UML')
svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><rect width="100%" height="100%" fill="white"/>']
image=Image.new('RGB',(W*SCALE,H*SCALE),'white');draw=ImageDraw.Draw(image)

def text(x,y,lines,size=16,color=INK,background=False):
    if isinstance(lines,str):lines=lines.split('\n')
    font=ImageFont.truetype(str(FONT),int(size*SCALE));width=max(draw.textlength(t,font=font)/SCALE for t in lines)
    if background:
        box=(x-width/2-4,y-size-2,x+width/2+4,y+(len(lines)-1)*21+5)
        draw.rectangle(tuple(int(v*SCALE) for v in box),fill='white')
        svg.append(f'<rect x="{box[0]}" y="{box[1]}" width="{box[2]-box[0]}" height="{box[3]-box[1]}" fill="white"/>')
    for i,line in enumerate(lines):
        yy=y+i*21;svg.append(f'<text x="{x}" y="{yy}" text-anchor="middle" font-family="Arial,DejaVu Sans,sans-serif" font-size="{size}" fill="{color}">{html.escape(line)}</text>')
        draw.text((x*SCALE,yy*SCALE),line,font=font,fill=color,anchor='ms')

def edge(points,dashed=False):
    color=RED if dashed else BLUE
    svg.append('<polyline points="'+' '.join(f'{x},{y}' for x,y in points)+f'" fill="none" stroke="{color}" stroke-width="1.8"'+(' stroke-dasharray="8 6"' if dashed else '')+'/>')
    for (x1,y1),(x2,y2) in zip(points,points[1:]):
        length=math.hypot(x2-x1,y2-y1);ux,uy=(x2-x1)/length,(y2-y1)/length
        if dashed:
            step=0
            while step<length:
                end=min(length,step+8);draw.line([(int((x1+ux*step)*SCALE),int((y1+uy*step)*SCALE)),(int((x1+ux*end)*SCALE),int((y1+uy*end)*SCALE))],fill=color,width=3);step+=14
        else:draw.line([(int(x1*SCALE),int(y1*SCALE)),(int(x2*SCALE),int(y2*SCALE))],fill=color,width=3)
    x,y=points[-1];px,py=points[-2];dx,dy=x-px,y-py;length=math.hypot(dx,dy);ux,uy=dx/length,dy/length
    triangle=[(x,y),(x-10*ux+4*uy,y-10*uy-4*ux),(x-10*ux-4*uy,y-10*uy+4*ux)]
    svg.append('<polygon points="'+' '.join(f'{xx},{yy}' for xx,yy in triangle)+f'" fill="{color}"/>');draw.polygon([(int(xx*SCALE),int(yy*SCALE)) for xx,yy in triangle],fill=color)

nodes=[('NEW',395,120,210,70,['NEW','Sin reserva de RAM']),('READY',395,300,210,70,['READY','RAM asignada']),('RUNNING',395,500,210,95,['RUNNING','Una instrucción','por ciclo CPU']),('BLOCKED',760,500,210,95,['BLOCKED','RAM y contexto','conservados']),('TERMINATED',370,820,260,110,['TERMINATED','Razón y ciclo conservados','Recursos liberados','por el núcleo'])]
edges=[
('Initial','NEW','creación / llegada',[(500,67),(500,120)],(650,93)),
('NEW','READY','admisión\nRAM disponible',[(500,190),(500,300)],(390,240)),
('READY','RUNNING','despacho\nregistra primera respuesta',[(500,370),(500,500)],(630,435)),
('RUNNING','READY','apropiación',[(395,540),(300,540),(300,335),(395,335)],(190,425)),
('RUNNING','BLOCKED','IO / red /\nwait bloqueante',[(605,540),(760,540)],(682,506)),
('READY','BLOCKED','red previa al despacho\nsin instrucción ni respuesta',[(605,335),(865,335),(865,500)],(735,380)),
('BLOCKED','READY','IO, red o permiso disponible\ncon instrucciones restantes',[(940,500),(940,260),(580,260),(580,300)],(790,217)),
('RUNNING','TERMINATED','última instrucción\nsin IO pendiente',[(500,595),(500,820)],(390,680)),
('BLOCKED','TERMINATED','IO completa y\nno quedan instrucciones',[(865,595),(865,735),(630,850)],(740,650)),
('NEW','TERMINATED','deadline',[(605,155),(1070,155),(1070,870),(630,870)],None),
('READY','TERMINATED','deadline',[(605,350),(1020,350),(1020,890),(630,890)],None),
('RUNNING','TERMINATED','deadline',[(395,575),(260,630),(260,875),(370,875)],None),
('BLOCKED','TERMINATED','deadline',[(970,575),(995,620),(995,910),(630,910)],None),
('TERMINATED','Final','',[(500,930),(500,970)],None)]
for _,_,event,points,label in edges:edge(points,event=='deadline')
for name,x,y,w,h,lines in nodes:
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="#F4F7F9" stroke="{INK}" stroke-width="1.6"/>')
    draw.rounded_rectangle((x*SCALE,y*SCALE,(x+w)*SCALE,(y+h)*SCALE),radius=14*SCALE,fill='#F4F7F9',outline=INK,width=3)
    text(x+w/2,y+(h-(len(lines)-1)*21)/2+6,lines,18)
for _,_,event,_,label in edges:
    if label:text(*label,event,15,background=True)
text(550,31,'UML de estados del proceso',24)
svg.append(f'<circle cx="500" cy="60" r="7" fill="{INK}"/>');draw.ellipse((493*SCALE,53*SCALE,507*SCALE,67*SCALE),fill=INK)
svg.append(f'<circle cx="500" cy="981" r="11" fill="white" stroke="{INK}"/><circle cx="500" cy="981" r="7" fill="{INK}"/>');draw.ellipse((489*SCALE,970*SCALE,511*SCALE,992*SCALE),outline=INK,width=2);draw.ellipse((493*SCALE,974*SCALE,507*SCALE,988*SCALE),fill=INK)
svg.append(f'<rect x="45" y="95" width="295" height="110" rx="8" fill="#FFF4EC" stroke="{RED}"/>');draw.rounded_rectangle((45*SCALE,95*SCALE,340*SCALE,205*SCALE),radius=8*SCALE,fill='#FFF4EC',outline=RED,width=2)
text(192,124,['Flechas rojas discontinuas:','deadline al cierre del ciclo','desde NEW, READY,','RUNNING o BLOCKED.'],16)
text(550,1025,'El bloqueo conserva PC/MAR y RAM; solo una instrucción ejecutada avanza PC/MAR.',14)
svg.append('</svg>');(ROOT/'estados.svg').write_text('\n'.join(svg),encoding='utf-8');image.save(ROOT/'estados.png')
def quote(s):return '"'+s.replace('"','\\"').replace('\n','\\n')+'"'
dot=['// Generado por render_states.py: topología UML; SVG/PNG usan layout fijo.','digraph ProcessStates {','  rankdir=TB; node [shape=box,style=rounded];','  Initial [shape=point]; Final [shape=doublecircle,label=""];']
for name,_,_,_,_,lines in nodes:dot.append(f'  {name} [label={quote(chr(10).join(lines))}];')
for a,b,event,_,_ in edges:dot.append(f'  {a} -> {b} [label={quote(event)}'+(',style=dashed,color="#A34529"' if event=='deadline' else '')+'];')
dot.append('}');(ROOT/'estados.dot').write_text('\n'.join(dot)+'\n',encoding='utf-8')
