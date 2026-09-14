"""Regenera docs/TECNICO.pdf desde Markdown. Requiere: pip install reportlab."""
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.platypus import SimpleDocTemplate, Paragraph, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
root=Path(__file__).resolve().parents[1]
sections=[]
for section in (root/'docs/TECNICO.md').read_text(encoding='utf-8').split('\n## ')[1:]:
    heading,*parts=section.split('\n### ')
    title=heading.strip().split('. ',1)[1]
    paragraphs=[]
    for part in parts:
        subtitle,body=part.split('\n',1)
        paragraphs.append((subtitle.strip(),body.strip()))
    sections.append((title,paragraphs))
docpath=root/'docs/TECNICO.pdf'
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyCustom',fontName='Helvetica',fontSize=10,leading=14,spaceAfter=11,textColor=colors.HexColor('#283344')))
styles.add(ParagraphStyle(name='SubCustom',fontName='Helvetica-Bold',fontSize=11,leading=14,spaceAfter=5,textColor=colors.HexColor('#145c70')))
styles.add(ParagraphStyle(name='TitleCustom',fontName='Helvetica-Bold',fontSize=22,leading=27,spaceAfter=18,textColor=colors.HexColor('#17394b')))
story=[]; md=[]
for i,(title,paras) in enumerate(sections):
 if i: story.append(PageBreak())
 story.append(Paragraph(f'{i+1:02d} / {escape(title)}',styles['TitleCustom']))
 md.append(f'\n## {i+1}. {title}\n')
 for subtitle,body in paras:
  story.extend([Paragraph(escape(subtitle),styles['SubCustom']),Paragraph(escape(body),styles['BodyCustom'])])
  md.append(f'\n### {subtitle}\n\n{body}\n')
def page(canvas,doc):
 canvas.setStrokeColor(colors.HexColor('#b3d3da')); canvas.line(45,805,550,805)
 canvas.setFont('Helvetica',8); canvas.setFillColor(colors.HexColor('#516577'))
 canvas.drawString(45,818,'INVERSORAR  /  DOCUMENTACIÓN TÉCNICA  /  13.09.2026')
 canvas.drawString(45,29,'comp_portafolio | Compra + Portfolio + simulación de apoyo')
 canvas.drawRightString(550,29,f'{doc.page} / 6')
SimpleDocTemplate(str(docpath),pagesize=A4,rightMargin=45,leftMargin=45,topMargin=53,bottomMargin=48).build(story,onFirstPage=page,onLaterPages=page)
