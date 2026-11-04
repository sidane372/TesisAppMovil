"""Export the physical dictionary and ER diagram from verified MySQL metadata."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def cell(value):
    return str(value).replace('|','&#124;').replace('`','').replace('\n',' ')

def export(report_path):
    report=json.loads(report_path.read_text(encoding='utf-8'))
    if not report.get('success'):
        raise ValueError('Cannot publish a verified dictionary from a failed run')
    for migration in report['migrations']:
        path=ROOT/migration['file']
        if hashlib.sha256(path.read_bytes()).hexdigest()!=migration['sha256']:
            raise ValueError(f'Migration changed after verification: {path.name}')
    meta=report['metadata']
    tables={t['table']:t for t in report['schema_before_fixtures']}
    columns=defaultdict(list)
    for column in meta['columns']:
        if column['table'] in tables: columns[column['table']].append(column)
    constraints=defaultdict(list)
    for constraint in meta['constraints']: constraints[constraint['table']].append(constraint)
    checks={check['name']:check['expression'] for check in meta['checks']}
    indexes=defaultdict(lambda:defaultdict(list))
    for index in meta['indexes']: indexes[index['table']][index['name']].append(index)
    foreign=defaultdict(lambda:defaultdict(list))
    for row in meta['foreign_keys']: foreign[row['table']][row['name']].append(row)
    lines=['# Diccionario físico MySQL — Etapa 2','',
           f"Generado a partir de INFORMATION_SCHEMA de MySQL {report['mysql_version']}, ejecución {report['completed_at_utc']}. La evidencia y los SHA-256 de migraciones están en tests/evidence/resultado_mysql.json.",'',
           f"Alcance: {len(tables)} tablas, {sum(map(len,columns.values()))} columnas físicas y {len(meta['views'])} vista. Los valores mostrados son definición de esquema, no registros de personas. Room conserva un esquema independiente v1 descrito al final.",'',
           'Convenciones: importes BIGINT en céntimos PEN, cantidades DECIMAL(10,3), fechas DATETIME(6) en UTC salvo TIME de horario recurrente local America/Lima. Las filas con FK usan ON DELETE/UPDATE RESTRICT; la validación de roles/propietarios del actor corresponde al backend. Claves financieras en ascii_bin; correo normalizado sensible a acentos. Las columnas calculadas no se escriben desde el backend.','']
    for table in sorted(tables):
        lines += [f'## {table}','',tables[table]['comment'],'',
                  '| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |','|---|---|---|---|---|---|']
        for column in columns[table]:
            value=column['generation'] or column['default']
            if value is None: value='NULL' if column['nullable']=='YES' else 'Sin valor explícito'
            lines.append('| '+' | '.join(cell(v) for v in (column['name'],column['type'],column['nullable'],column['key'] or '—',value,column['comment']))+' |')
        lines += ['', '**Claves e índices**','', '| Nombre | Clase | Columnas en orden |','|---|---|---|']
        for name,rows in sorted(indexes[table].items()):
            kind='PK' if name=='PRIMARY' else 'UNIQUE' if rows[0]['unique'] else 'INDEX'
            lines.append(f"| {name} | {kind} | {', '.join(r['column'] for r in sorted(rows,key=lambda r:r['ordinal']))} |")
        if foreign[table]:
            lines+=['','**Claves foráneas**','', '| Restricción | Columnas → destino | Borrado / actualización |','|---|---|---|']
            for name,rows in sorted(foreign[table].items()):
                rows=sorted(rows,key=lambda r:r['ordinal'])
                lines.append(f"| {name} | ({', '.join(r['column'] for r in rows)}) → {rows[0]['parent_table']}({', '.join(r['parent_column'] for r in rows)}) | {rows[0]['delete_rule']} / {rows[0]['update_rule']} |")
        table_checks=[c for c in constraints[table] if c['type']=='CHECK']
        if table_checks:
            lines+=['','**Restricciones CHECK ejecutadas**','', '| Nombre | Expresión MySQL |','|---|---|']
            for check in table_checks:
                lines.append(f"| {check['name']} | {cell(checks[check['name']])} |")
        lines.append('')
    lines+=['## Vista v_saldo_contrato','',
            'Separa abonos aplicados y reembolsos confirmados. Los cobros externos no aplicados por incidencia y los intentos rechazados no reducen el saldo. Primero agrega devoluciones por pago y luego pagos por contrato para evitar multiplicaciones al hacer joins. No oculta un saldo negativo con GREATEST: el Service debe impedir sobrepago bajo bloqueo del contrato. SQL SECURITY INVOKER usa permisos del consultante.','',
            '| Campo | Definición |','|---|---|',
            '| id_contrato | Contrato consultado, sujeto a autorización por propietario |',
            '| total_centimos | Total del snapshot aceptado |',
            '| abonado_centimos | Suma de intentos con aplicado_en no nulo |',
            '| reembolsado_centimos | Suma de reembolsos APROBADO asociados a pagos aplicados |',
            '| saldo_centimos | Total − abonado + reembolsado |','',
            'Las sumas de MySQL sobre BIGINT se devuelven con precisión decimal; el backend debe mapearlas a entero exacto/BigDecimal con escala cero, sin float/double. La política comercial después de una cancelación se interpreta con el estado del contrato; la vista es contabilidad de abonos, no una orden de seguir cobrando un contrato cancelado.','',
            '## Triggers','', '| Trigger | Tabla | Momento / evento |','|---|---|---|']
    for trigger in meta['triggers']:
        lines.append(f"| {trigger['name']} | {trigger['table']} | {trigger['timing']} {trigger['event']} |")
    lines+=['','## Room local existente','',
            '| Columna servicio_cache | Tipo SQLite / Java | Regla |','|---|---|---|',
            '| id | INTEGER / long | PK oficial del servicio |',
            '| nombre | TEXT / String | NOT NULL, nombre público |',
            '| tipo | TEXT / String | BANDA o SOLISTA en el mapper |',
            '| ciudad | TEXT / String | Ciudad pública |',
            '| tarifaCentimos | INTEGER / Long | NULL = A cotizar; entero en céntimos |',
            '| sincronizadoEn | INTEGER / long | epoch millis del refresco local |','',
            'Room no se migra en esta etapa; ninguna tabla financiera central se copia automáticamente a Android. Contraseñas, tokens, direcciones de eventos y datos de tarjeta no forman parte de esta caché.','']
    (ROOT/'database/diccionario_datos.md').write_text('\n'.join(lines),encoding='utf-8')
    diagram=['erDiagram']
    for table in sorted(tables):
        diagram.append(f'    {table} {{')
        fkcols={row['column'] for group in foreign[table].values() for row in group}
        for column in columns[table]:
            name=column['name']
            if column['key']=='PRI' or name in fkcols or name in ('nombre','codigo','tipo','estado','estado_publicacion','total_centimos','importe_centimos','inicio_utc','fin_utc','vence_en'):
                key=' PK' if column['key']=='PRI' else ' FK' if name in fkcols else ''
                type_name=column['type'].split('(')[0].split()[0]
                diagram.append(f'        {type_name} {name}{key}')
        diagram.append('    }')
    for table in sorted(tables):
        for name,rows in sorted(foreign[table].items()):
            fkcols={row['column'] for row in rows}
            nullable=any(c['nullable']=='YES' for c in columns[table] if c['name'] in fkcols)
            unique=any(idx[0]['unique'] and {i['column'] for i in idx}.issubset(fkcols) for idx in indexes[table].values())
            diagram.append(f"    {rows[0]['parent_table']} {'|o' if nullable else '||'}--{'o|' if unique else 'o{'} {table} : {name}")
    p=ROOT/'database/diagramas/modelo_relacional.mmd'; p.parent.mkdir(parents=True,exist_ok=True); p.write_text('\n'.join(diagram)+'\n',encoding='utf-8')
    return {'tables':len(tables),'columns':sum(map(len,columns.values())), 'foreign_keys':sum(len(groups) for groups in foreign.values()),
            'checks':len(meta['checks']),'triggers':len(meta['triggers']),'views':len(meta['views']),
            'indexes':sum(len(groups) for groups in indexes.values())}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report',type=Path,default=ROOT/'database/tests/evidence/resultado_mysql.json')
    args=parser.parse_args(); print(json.dumps(export(args.report)))
