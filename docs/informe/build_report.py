#!/usr/bin/env python3
"""Genera informe y resumen desde Markdown, UML y resultados medidos por Java."""
from pathlib import Path
import html, json, math, re
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4, A3, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, PageBreak, NextPageTemplate, Table, TableStyle, Image
from reportlab.pdfgen.canvas import Canvas
from pypdf import PdfReader

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/pdf';OUT.mkdir(parents=True,exist_ok=True)
DATA=ROOT/'docs/verification/data/final-comparisons.json'
INK=colors.HexColor('#183648');TEAL=colors.HexColor('#087F8C');PALE=colors.HexColor('#E7F1F4');BORDER=colors.HexColor('#C8D7DE')
SIZES={'portrait':A4,'wide':landscape(A3),'tall':A3}
experiments=json.loads(DATA.read_text(encoding='utf-8'))
assert experiments['dataVersion']==1
rows=experiments['experiments'];assert len(rows)==8
by_name={r['name']:r for r in rows};assert len(by_name)==8
policies=rows[:4];assert all(r['kind']=='policy' and r['horizonCycles']==12 for r in policies)
base=policies[0]['configuration']
for r in policies:
 c=r['configuration'];assert c['processes']==base['processes'] and c['buffers']==base['buffers']
 assert c['cycleMillis']==500 and c['networkLatency']==2 and all(m['ramCapacity']==64 and m['quantum']==2 for m in c['computers'])

def inline(text):
    text=text.replace('→',' -> ').replace('—','-').replace('–','-').replace('\u2011','-')
    text=html.escape(text)
    text=re.sub(r'\*\*(.+?)\*\*',r'<b>\1</b>',text)
    return re.sub(r'`(.+?)`',r'<font name="Courier" size="9">\1</font>',text)

def styles(summary=False):
    return {
      'body':ParagraphStyle('body',fontName='Helvetica',fontSize=10 if summary else 10.5,leading=13 if summary else 14.2,textColor=INK,spaceAfter=5 if summary else 8),
      'h1':ParagraphStyle('h1',fontName='Helvetica-Bold',fontSize=21 if summary else 31,leading=25 if summary else 36,textColor=INK,spaceAfter=12),
      'h2':ParagraphStyle('h2',fontName='Helvetica-Bold',fontSize=13.2 if summary else 18,leading=16 if summary else 23,textColor=TEAL,spaceBefore=7,spaceAfter=7,keepWithNext=True),
      'h3':ParagraphStyle('h3',fontName='Helvetica-Bold',fontSize=11.8,leading=15,textColor=TEAL,spaceBefore=7,spaceAfter=6,keepWithNext=True),
      'cell':ParagraphStyle('cell',fontName='Helvetica',fontSize=9.2,leading=11.7,textColor=INK),
      'small':ParagraphStyle('small',fontName='Helvetica',fontSize=8.6,leading=11.2,textColor=INK,spaceAfter=6)}

ST=styles()
def para(text,style='body'):return Paragraph(inline(text),ST[style])
def table(data,widths):
    values=[[para(str(v),'cell') for v in row] for row in data]
    t=Table(values,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('TEXTCOLOR',(0,0),(-1,0),INK),('FONTNAME',(0,0),(-1,0),'Helvetica-Bold'),('VALIGN',(0,0),(-1,-1),'TOP'),('GRID',(0,0),(-1,-1),.45,BORDER),('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6)]))
    return [t,Spacer(1,8)]
def num(value,digits=3,percent=False):return 'Sin datos' if value is None else f'{value*(100 if percent else 1):.{digits}f}'

def block(name):
    if name=='classes':
        data=[['Clases principales','Responsabilidad','Métodos clave'],
          ['Computer / Kernel','Ensambla componentes; admisión, estados, despacho y terminación.','onTick; submit; configureScheduling'],
          ['Cpu / Ram / IoController','Ejecutar una instrucción; reservar memoria; finalizar E/S por ciclos.','onTick; reserve / release; request / pollCompleted'],
          ['Scheduler / SchedulingPolicy','Una cola de listos y estrategia intercambiable.','select; reorder; shouldPreempt'],
          ['ProcessDefinition / ProcessControlBlock','Atributos iniciales y estado vivo/contexto del proceso.','admit; dispatch; executeInstruction; block; terminate'],
          ['Simulation / GlobalClock / ComputerWorker','Propiedad del clúster, órdenes y confirmación de cada ciclo.','advanceCycle; publishState; executeCycle; step'],
          ['BufferService / BoundedBuffer / SimulatedSemaphore','Protocolo local/remoto, contenido y permisos/esperas de PCB.','prepare; afterInstruction; cleanup; waitFor; signal'],
          ['SimulationConfig / ScenarioJson / JsonConfigurationStore','Validación común, tokens JSON y archivos UTF-8 seguros.','read / write; load / save'],
          ['MetricsCalculator / MetricsSnapshot','Calcular por máquina y agregar bases de ciclos confirmados.','computer; aggregate; calculate'],
          ['UiSession / UiController','Serializar acciones/cierre fuera del EDT; presentar resultados.','loadScenario; saveScenario; refresh'],
          ['DashboardPanel / UtilizationChart','Ensamblar vistas y dibujar historial conservado por el motor.','render; setSnapshot; paintComponent']]
        return table(data,[145,225,153])
    if name=='contracts':
        return table([['Contrato/tipo','Uso'],['Tickable','onTick(long cycle): Computer, Cpu e IoController reciben un ciclo.'],['ClockDriver','Simulation implementa advanceCycle, publishState y shutdown para GlobalClock.'],['SchedulingPolicy','Contrato de selección/reordenamiento/apropiación de las cuatro políticas.'],['ConfigurationStore','load(Path) y save(Path,SimulationConfig), separadas de la sesión.'],['Enums principales','ProcessState, ProcessType, BlockReason, CpuMode, SchedulingPolicyType, TerminationReason; InstructionKind y BufferPhase describen las instrucciones/fases.']],[145,378])
    if name=='formulas':
        return table([['Indicador','Fórmula/población'],['Throughput','Completados normales / ciclos transcurridos; vencidos aparte.'],['CPU','(usuario + núcleo) / ciclos disponibles. Global: computadores × ciclos.'],['Respuesta','Suma(primera ejecución - llegada) / procesos que recibieron CPU.'],['Cumplimiento','Completados / (completados + vencidos); pendientes aparte.'],['Jain','Suma(servicio)² / [admitidos × suma(servicio²)]. Incluir admitidos sin servicio y terminados; excluir vencidos NEW.'],['Espera de semáforo','Ciclos de episodios cerrados / episodios cerrados, incluidos deadlines. Abiertos aparte; excluir IO/red.']],[125,398])
    if name=='jobs':
        return table([['Proceso','Computador','Instrucciones','Plazo relativo','Prioridad']]+[[p['name'],p['computerId'],p['instructionCount'],p['deadlineCycles'],p['priority']] for p in base['processes']],[75,100,115,120,113])
    if name=='policy-results':
        labels=['FCFS','EDF','Round Robin','Prioridades'];data=[['Política','Comp.','Venc.','Throughput','CPU (%)','Cumpl. (%)']]
        for label,r in zip(labels,policies):
            m=r['globalMetrics'];data.append([label,m['completed'],m['expired'],num(m['throughput']),num(m['utilization'],2,True),num(m['deadlineCompliance'],2,True)])
        other=[['Política','Respondidos / total','Respuesta (ciclos)','Jain']]
        for label,r in zip(labels,policies):
            m=r['globalMetrics'];other.append([label,f"{m['responded']} / {len(r['configuration']['processes'])}",num(m['averageResponse']),num(m['fairness'])])
        return table(data,[100,50,50,110,105,108])+table(other,[120,125,155,123])
    if name=='network-results':
        data=[['Caso','Horizonte','Fin de todos','CPU (%)','Resp.','Espera sem.']]
        labels=['P local','P remoto','C/P local','C/P remoto']
        for label,r in zip(labels,rows[4:]):
            m=r['globalMetrics'];data.append([label,r['horizonCycles'],r['allTerminatedAtBoundary'],num(m['utilization'],2,True),num(m['averageResponse']),num(r['semaphoreWaits']['averageWait'])])
        return table(data,[100,75,90,80,75,103])+[para('Horizonte, finalización, respuesta y espera se expresan en ciclos lógicos. Fin de todos coincide aquí con terminación normal: no hubo vencidos. P = productor aislado; C/P = consumidor y productor.','small')]
    raise ValueError('Bloque desconocido: '+name)

def footer(canvas,doc):
    width,height=canvas._pagesize;canvas.saveState();canvas.setStrokeColor(BORDER);canvas.line(36,29,width-36,29);canvas.setFillColor(INK);canvas.setFont('Helvetica',8);canvas.drawString(36,17,'ÁvilaOS | Sistemas Operativos | Proyecto 1');canvas.drawRightString(width-36,17,str(doc.page));canvas.restoreState()

def build(source,destination,summary=False):
    global ST
    ST=styles(summary);story=[];first=True;mode='portrait';paragraph=[]
    def flush():
        if paragraph:story.append(para(' '.join(paragraph)));paragraph.clear()
    for line in source.read_text(encoding='utf-8').splitlines():
        match=re.fullmatch(r'<!-- page:(portrait|wide|tall) -->',line)
        if match:
            flush();mode=match[1]
            if not first:story.extend([NextPageTemplate(mode),PageBreak()])
            first=False;continue
        match=re.fullmatch(r'<!-- block:(.+) -->',line)
        if match:flush();story.extend(block(match[1]));continue
        match=re.fullmatch(r'<!-- image:(clases|estados) -->',line)
        if match:
            flush();size=SIZES[mode];image=Image(str(ROOT/'docs/uml'/(match[1]+'.png')));available=size[0]-72;maxheight=660 if mode=='wide' else 880 if mode=='tall' else 600;scale=min(available/image.imageWidth,maxheight/image.imageHeight);image.drawWidth=image.imageWidth*scale;image.drawHeight=image.imageHeight*scale;image.hAlign='CENTER';story.extend([image,Spacer(1,8)]);continue
        if not line.strip():flush();continue
        if line.startswith('# '):flush();story.append(para(line[2:],'h1'));continue
        if line.startswith('## '):flush();story.append(para(line[3:],'h2'));continue
        if line.startswith('### '):flush();story.append(para(line[4:],'h3'));continue
        if line.startswith('- '):flush();story.append(Paragraph('• '+inline(line[2:]),ST['body']));continue
        paragraph.append(line)
    flush()
    doc=BaseDocTemplate(str(destination),pagesize=A4,leftMargin=36,rightMargin=36,topMargin=48,bottomMargin=40,title='ÁvilaOS - '+('Resumen de defensa' if summary else 'Informe del Proyecto 1'),author='Elio Colmenares')
    for name,size in SIZES.items():doc.addPageTemplates(PageTemplate(id=name,pagesize=size,frames=[Frame(36,40,size[0]-72,size[1]-88,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=footer))
    def canvasmaker(*args,**kwargs):kwargs['invariant']=1;return Canvas(*args,**kwargs)
    doc.build(story,canvasmaker=canvasmaker)
    result=PdfReader(destination)
    if summary and len(result.pages)!=2:raise RuntimeError(f'El resumen debe tener 2 páginas; tiene {len(result.pages)}')
    for n,page in enumerate(result.pages,1):
        text=page.extract_text();assert len(text)>80,(destination,n,'Página vacía o sin contenido textual suficiente')
        assert '\u25a0' not in text,(destination,n,'Glifo faltante')
    print(destination.relative_to(ROOT),len(result.pages),'páginas')

if __name__=='__main__':
    build(ROOT/'docs/informe/informe.md',OUT/'avilaos-informe.pdf')
    build(ROOT/'docs/defensa/resumen-unificado.md',OUT/'avilaos-defensa-resumen.pdf',True)
