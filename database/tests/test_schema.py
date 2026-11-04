"""Integration checks on a fresh, disposable MySQL schema; standard library only.
SQL fixtures are invented test data, not users, research results or provider sandbox payments.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[2]

class Mysql:
    def __init__(self, binary, port, schema):
        if not re.fullmatch(r'sc_test_[a-f0-9]{12}', schema):
            raise ValueError('Only a new sc_test_<12 hex> schema is accepted')
        self.schema = schema
        self.args = [binary, '--no-defaults', '--protocol=TCP', '--host=127.0.0.1',
                     f'--port={port}', '--user=root', '--batch', '--raw', '--skip-column-names',
                     '--unbuffered', '--default-character-set=utf8mb4', '--connect-timeout=5',
                     "--init-command=SET time_zone='+00:00'"]
    def execute(self, sql, error=None, use_schema=True):
        text = (f'USE `{self.schema}`;\n' if use_schema else '') + sql
        result = subprocess.run(self.args, input=text, text=True, encoding='utf-8',
                                capture_output=True, timeout=30)
        if error is not None:
            if result.returncode == 0 or f'ERROR {error} ' not in result.stderr:
                raise AssertionError(f'Expected MySQL error {error}; got {result.returncode}: {result.stderr}')
        elif result.returncode:
            raise AssertionError(result.stderr)
        return result.stdout.strip()
    def scalar(self, sql):
        return self.execute(sql).splitlines()[-1]
    def start(self, sql):
        process = subprocess.Popen(self.args, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, text=True, encoding='utf-8')
        process.stdin.write(f'USE `{self.schema}`;\n'+sql)
        process.stdin.close()
        return process
    def json_rows(self, sql):
        return [json.loads(line) for line in self.execute(sql).splitlines() if line]

DB = None

def fixture():
    key = uuid.uuid4().hex
    output = DB.execute(f"""
INSERT INTO usuario(id_rol,nombres,apellidos,correo,password_hash,estado) VALUES(2,'Cliente SQL','Fixture','c{key}@example.invalid',REPEAT('x',60),'ACTIVO');
SET @cliente=LAST_INSERT_ID();
INSERT INTO usuario(id_rol,nombres,apellidos,correo,password_hash,estado) VALUES(3,'Director SQL','Fixture','p{key}@example.invalid',REPEAT('x',60),'ACTIVO');
SET @proveedor=LAST_INSERT_ID();
INSERT INTO servicio_musical(id_responsable,tipo,nombre,ciudad) VALUES(@proveedor,'BANDA','Fixture SQL','Ambo');
SET @servicio=LAST_INSERT_ID();
INSERT INTO recurso_agenda(id_servicio) VALUES(@servicio); SET @recurso=LAST_INSERT_ID();
INSERT INTO solicitud_cotizacion(id_cliente,id_servicio,id_proveedor,inicio_evento_utc,fin_evento_utc,lugar,ciudad,tipo_evento,clave_idempotencia)
 VALUES(@cliente,@servicio,@proveedor,'2030-01-02 15:00:00','2030-01-02 16:00:00','Lugar de fixture','Ambo','PRUEBA SQL','{key}');
SET @solicitud=LAST_INSERT_ID();
INSERT INTO cotizacion(id_solicitud,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc)
 VALUES(@solicitud,1,'2030-01-02 15:00:00','2030-01-02 16:00:00',10001,'Solo fixture','Solo fixture','fixture-v1',UTC_TIMESTAMP(6)+INTERVAL 1 DAY);
SET @cotizacion=LAST_INSERT_ID();
INSERT INTO detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos) VALUES(@cotizacion,'Concepto SQL',1,10001);
UPDATE cotizacion SET estado='ENVIADA' WHERE id=@cotizacion;
INSERT INTO contrato(codigo,id_cotizacion,id_solicitud,id_servicio,id_cliente,id_proveedor,inicio_evento_utc,fin_evento_utc,inicio_ocupacion_utc,fin_ocupacion_utc,total_centimos,condiciones_aceptadas,politica_version,snapshot,clave_idempotencia,huella_aceptacion)
 VALUES('{key}',@cotizacion,@solicitud,@servicio,@cliente,@proveedor,'2030-01-02 15:00:00','2030-01-02 16:00:00','2030-01-02 14:30:00','2030-01-02 16:30:00',10001,1,'fixture-v1',JSON_OBJECT('origen','FIXTURE_SQL_NO_FINANCIERO'),'{key}',UNHEX(SHA2('{key}',256)));
SET @contrato=LAST_INSERT_ID();
SELECT @cliente,@proveedor,@servicio,@recurso,@solicitud,@cotizacion,@contrato;
""")
    values = [int(value) for value in output.splitlines()[-1].split('\t')]
    return dict(zip(('cliente','proveedor','servicio','recurso','solicitud','cotizacion','contrato'), values))

def payment(f, amount=4001, applied=False, state=None):
    key = uuid.uuid4().hex
    state = state or ('APROBADO' if applied else 'PENDIENTE')
    verified = 'UTC_TIMESTAMP(6)' if applied or state == 'EN_REVISION' else 'NULL'
    applied_at = 'UTC_TIMESTAMP(6)' if applied else 'NULL'
    return int(DB.scalar(f"""INSERT INTO pago(id_contrato,id_cliente,proveedor,referencia_externa,clave_idempotencia,huella_solicitud,modalidad,importe_centimos,estado,verificado_en,cobrado_en,aplicado_en)
VALUES({f['contrato']},{f['cliente']},'FIXTURE_SQL_NO_PASARELA','{key}','{key}',UNHEX(SHA2('{key}',256)),'SALDO_COMPLETO',{amount},'{state}',{verified},{verified},{applied_at}); SELECT LAST_INSERT_ID();"""))

def clone_contract(f, quote=None, request=None, client=None, error=None):
    key = uuid.uuid4().hex
    return DB.execute(f"""INSERT INTO contrato(codigo,id_cotizacion,id_solicitud,id_servicio,id_cliente,id_proveedor,inicio_evento_utc,fin_evento_utc,inicio_ocupacion_utc,fin_ocupacion_utc,total_centimos,condiciones_aceptadas,politica_version,snapshot,clave_idempotencia,huella_aceptacion)
SELECT '{key}',{quote or f['cotizacion']},{request or f['solicitud']},id_servicio,{client or f['cliente']},id_proveedor,inicio_evento_utc,fin_evento_utc,inicio_ocupacion_utc,fin_ocupacion_utc,total_centimos,condiciones_aceptadas,politica_version,snapshot,'{key}',huella_aceptacion FROM contrato WHERE id={f['contrato']};""",error)

def draft(f, version=2):
    return int(DB.scalar(f"""INSERT INTO cotizacion(id_solicitud,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc)
VALUES({f['solicitud']},{version},'2030-01-02 15:00:00','2030-01-02 16:00:00',10001,'Solo fixture','Solo fixture','fixture-v1',UTC_TIMESTAMP(6)+INTERVAL 1 DAY); SELECT LAST_INSERT_ID();"""))

def candidate(f, resources, start='2030-01-02 14:30:00', end='2030-01-02 16:30:00', pause=False):
    lock = (ROOT/'database/queries/bloquear_recurso.sql').read_text(encoding='utf-8-sig')
    conflict = (ROOT/'database/queries/ocupaciones_en_conflicto.sql').read_text(encoding='utf-8-sig')
    sql = ['START TRANSACTION;']
    for resource in sorted(resources):
        sql.append(lock.replace(':id_recurso',str(resource)))
    sql.append("SELECT 'LOCK_ADQUIRIDO';")
    if pause: sql.append('DO SLEEP(1);')
    sql.append('SET @conflictos=0;')
    for resource in sorted(resources):
        query = conflict.replace('SELECT id, id_reserva, id_bloqueo, id_retencion','SELECT COUNT(*) INTO @actuales')
        sql.append(query.replace(':id_recurso',str(resource)).replace(':fin_utc',f"'{end}'").replace(':inicio_utc',f"'{start}'"))
        sql.append('SET @conflictos=@conflictos+@actuales;')
    sql.append(f"INSERT INTO bloqueo_agenda(id_servicio,id_actor,inicio_utc,fin_utc,motivo) SELECT {f['servicio']},{f['proveedor']},'{start}','{end}','Fixture concurrencia SQL' WHERE @conflictos=0;")
    sql.append('SET @bloqueo=LAST_INSERT_ID();')
    for resource in sorted(resources):
        sql.append(f"INSERT INTO ocupacion_agenda(id_recurso,id_bloqueo,inicio_utc,fin_utc) SELECT {resource},@bloqueo,'{start}','{end}' WHERE @conflictos=0;")
    sql += ['COMMIT;',"SELECT IF(@conflictos=0,'ACEPTADO','CONFLICTO');"]
    return '\n'.join(sql)

class SchemaTests(unittest.TestCase):
    def test_01_reference_roles_without_demo_accounts(self):
        self.assertEqual('4', DB.scalar('SELECT COUNT(*) FROM rol;'))
        self.assertEqual('0', DB.scalar('SELECT COUNT(*) FROM usuario;'))
    def test_02_innodb_utf8_and_enforced_checks(self):
        self.assertEqual('0',DB.scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND (engine<>'InnoDB' OR table_collation<>'utf8mb4_0900_ai_ci');"))
        self.assertGreater(int(DB.scalar("SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND constraint_type='CHECK' AND enforced='YES';")),40)
    def test_03_email_normalization_unique(self):
        f=fixture(); email=DB.scalar(f"SELECT correo FROM usuario WHERE id={f['cliente']};")
        DB.execute(f"INSERT INTO usuario(id_rol,nombres,apellidos,correo,password_hash) VALUES(2,'Duplicado','Fixture',' {email.upper()} ',REPEAT('x',60));",1062)
    def test_04_orphan_foreign_key_rejected(self):
        DB.execute("INSERT INTO servicio_musical(id_responsable,tipo,nombre,ciudad) VALUES(9223372036854775807,'BANDA','Huerfano','Ambo');",1452)
    def test_05_one_solo_service_per_musician(self):
        key=uuid.uuid4().hex
        solo=int(DB.scalar(f"INSERT INTO usuario(id_rol,nombres,apellidos,correo,password_hash) VALUES(4,'Solista SQL','Fixture','solo{key}@example.invalid',REPEAT('x',60)); SELECT LAST_INSERT_ID();"))
        DB.execute(f"INSERT INTO servicio_musical(id_responsable,tipo,nombre,ciudad) VALUES({solo},'SOLISTA','Solo SQL','Ambo');")
        DB.execute(f"INSERT INTO servicio_musical(id_responsable,tipo,nombre,ciudad) VALUES({solo},'SOLISTA','Otro SQL','Ambo');",1062)
    def test_06_negative_tariff_rejected(self):
        f=fixture(); DB.execute(f"UPDATE servicio_musical SET tarifa_centimos=-1 WHERE id={f['servicio']};",3819)
    def test_07_reversed_interval_rejected(self):
        f=fixture(); DB.execute(f"INSERT INTO bloqueo_agenda(id_servicio,id_actor,inicio_utc,fin_utc,motivo) VALUES({f['servicio']},{f['proveedor']},'2030-01-02 17:00','2030-01-02 16:00','Fixture');",3819)
    def test_08_quote_version_unique_within_request(self):
        f=fixture()
        DB.execute(f"INSERT INTO cotizacion(id_solicitud,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc) SELECT id_solicitud,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc FROM cotizacion WHERE id={f['cotizacion']};",1062)
    def test_09_sending_quote_requires_exact_detail_sum(self):
        f=fixture(); quote=draft(f)
        DB.execute(f"INSERT INTO detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos) VALUES({quote},'Diferencia',1,10000);")
        DB.execute(f"UPDATE cotizacion SET estado='ENVIADA' WHERE id={quote};",1644)
    def test_10_decimal_quantity_rounding_is_exact(self):
        f=fixture(); quote=draft(f)
        self.assertEqual('152', DB.scalar(f"INSERT INTO detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos) VALUES({quote},'Fraccion exacta',1.500,101); SELECT total_centimos FROM detalle_cotizacion WHERE id=LAST_INSERT_ID();"))
    def test_11_sent_quote_and_details_are_immutable(self):
        f=fixture()
        DB.execute(f"UPDATE cotizacion SET subtotal_centimos=2 WHERE id={f['cotizacion']};",1644)
        DB.execute(f"UPDATE detalle_cotizacion SET precio_unitario_centimos=2 WHERE id_cotizacion={f['cotizacion']};",1644)
        DB.execute(f"UPDATE cotizacion SET estado='BORRADOR' WHERE id={f['cotizacion']};",1644)
    def test_12_one_contract_even_across_quote_versions(self):
        f=fixture(); quote=draft(f)
        DB.execute(f"INSERT INTO detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos) VALUES({quote},'Version dos',1,10001); UPDATE cotizacion SET estado='ENVIADA' WHERE id={quote};")
        clone_contract(f,quote=quote,error=1062)
    def test_13_contract_cannot_mix_request_and_quote(self):
        a,b=fixture(),fixture()
        request=int(DB.scalar(f"INSERT INTO solicitud_cotizacion(id_cliente,id_servicio,id_proveedor,inicio_evento_utc,fin_evento_utc,lugar,ciudad,tipo_evento,clave_idempotencia) VALUES({a['cliente']},{a['servicio']},{a['proveedor']},'2030-01-02 15:00','2030-01-02 16:00','Fixture','Ambo','SQL','{uuid.uuid4().hex}'); SELECT LAST_INSERT_ID();"))
        quote=draft(b)
        DB.execute(f"INSERT INTO detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos) VALUES({quote},'Otra solicitud',1,10001); UPDATE cotizacion SET estado='ENVIADA' WHERE id={quote};")
        clone_contract(a,quote=quote,request=request,error=1452)
    def test_14_contract_snapshot_cannot_change_or_delete(self):
        f=fixture()
        DB.execute(f"UPDATE contrato SET snapshot=JSON_OBJECT('alterado',1) WHERE id={f['contrato']};",1644)
        DB.execute(f"DELETE FROM contrato WHERE id={f['contrato']};",1644)
    def test_15_payment_cannot_name_another_client(self):
        a,b=fixture(),fixture()
        DB.execute(f"INSERT INTO pago(id_contrato,id_cliente,proveedor,clave_idempotencia,huella_solicitud,modalidad,importe_centimos) VALUES({a['contrato']},{b['cliente']},'FIXTURE','clave',UNHEX(SHA2('x',256)),'SALDO_COMPLETO',1);",1452)
    def test_16_approval_needs_verification_and_application(self):
        f=fixture(); payment_id=payment(f)
        DB.execute(f"UPDATE pago SET estado='APROBADO' WHERE id={payment_id};",3819)
        DB.execute(f"UPDATE pago SET moneda='USD' WHERE id={payment_id};",3819)
    def test_17_payment_and_webhook_deduplication(self):
        f=fixture(); payment_id=payment(f)
        DB.execute(f"INSERT INTO pago(id_contrato,id_cliente,proveedor,referencia_externa,clave_idempotencia,huella_solicitud,modalidad,importe_centimos) SELECT id_contrato,id_cliente,proveedor,CONCAT(referencia_externa,'-otro'),clave_idempotencia,huella_solicitud,modalidad,importe_centimos FROM pago WHERE id={payment_id};",1062)
        DB.execute(f"INSERT INTO pago(id_contrato,id_cliente,proveedor,referencia_externa,clave_idempotencia,huella_solicitud,modalidad,importe_centimos) SELECT id_contrato,id_cliente,proveedor,referencia_externa,'otra-clave',huella_solicitud,modalidad,importe_centimos FROM pago WHERE id={payment_id};",1062)
        key=uuid.uuid4().hex
        event=f"INSERT INTO evento_pago(proveedor,id_evento_externo,id_pago,payload_hash,autenticado_en) VALUES('FIXTURE_SQL','{key}',{payment_id},UNHEX(SHA2('fixture',256)),UTC_TIMESTAMP(6));"
        DB.execute(event); DB.execute(event,1062)
    def test_18_exact_balance_does_not_multiply_refunds(self):
        f=fixture(); p1=payment(f,4001,True); payment(f,1000,True); payment(f,2000,False,'RECHAZADO'); payment(f,999,False,'EN_REVISION')
        for i in range(2):
            DB.execute(f"INSERT INTO reembolso(id_pago,id_contrato,proveedor,clave_idempotencia,referencia_externa,importe_centimos,motivo,estado,confirmado_en) VALUES({p1},{f['contrato']},'FIXTURE_SQL_NO_PASARELA','r{i}','r{i}',500,'Fixture SQL','APROBADO',UTC_TIMESTAMP(6));")
        self.assertEqual('10001\t5001\t1000\t6000', DB.scalar(f"SELECT total_centimos,abonado_centimos,reembolsado_centimos,saldo_centimos FROM v_saldo_contrato WHERE id_contrato={f['contrato']};"))
    def test_19_payment_transaction_rolls_back_on_invalid_reservation(self):
        f=fixture()
        DB.execute(f"START TRANSACTION; INSERT INTO pago(id_contrato,id_cliente,proveedor,clave_idempotencia,huella_solicitud,modalidad,importe_centimos) VALUES({f['contrato']},{f['cliente']},'FIXTURE_SQL','rollback',UNHEX(SHA2('x',256)),'SALDO_COMPLETO',1); INSERT INTO reserva(id_contrato,id_servicio) VALUES({f['contrato']},9223372036854775807); COMMIT;",1452)
        self.assertEqual('0',DB.scalar(f"SELECT COUNT(*) FROM pago WHERE id_contrato={f['contrato']};"))
    def test_20_one_reservation_per_contract(self):
        f=fixture(); sql=f"INSERT INTO reserva(id_contrato,id_servicio) VALUES({f['contrato']},{f['servicio']});"
        DB.execute(sql); DB.execute(sql,1062)
    def test_21_occupation_requires_one_origin(self):
        f=fixture(); DB.execute(f"INSERT INTO ocupacion_agenda(id_recurso,inicio_utc,fin_utc) VALUES({f['recurso']},'2030-01-02 15:00','2030-01-02 16:00');",3819)
    def test_22_expired_hold_does_not_block(self):
        f=fixture(); p=payment(f)
        DB.execute(f"INSERT INTO retencion_agenda(id_contrato,id_servicio,id_pago,inicio_utc,fin_utc,creada_en,vence_en) VALUES({f['contrato']},{f['servicio']},{p},'2030-01-02 14:30','2030-01-02 16:30',UTC_TIMESTAMP(6)-INTERVAL 2 HOUR,UTC_TIMESTAMP(6)-INTERVAL 1 HOUR); SET @retencion=LAST_INSERT_ID(); INSERT INTO ocupacion_agenda(id_recurso,id_retencion,inicio_utc,fin_utc,vence_en) SELECT {f['recurso']},id,inicio_utc,fin_utc,vence_en FROM retencion_agenda WHERE id=@retencion;")
        self.assertEqual('ACEPTADO', DB.execute(candidate(f,[f['recurso']])).splitlines()[-1])
    def test_23_adjacent_intervals_are_allowed(self):
        f=fixture(); DB.execute(candidate(f,[f['recurso']]))
        self.assertEqual('ACEPTADO',DB.execute(candidate(f,[f['recurso']],start='2030-01-02 16:30:00',end='2030-01-02 18:00:00')).splitlines()[-1])
    def _concurrent(self,a,b,resources_a,resources_b):
        first=DB.start(candidate(a,resources_a,pause=True))
        try:
            while True:
                line=first.stdout.readline()
                if 'LOCK_ADQUIRIDO' in line: break
                if not line: self.fail(first.stderr.read())
            started=time.monotonic()
            second=DB.execute(candidate(b,resources_b))
            elapsed=time.monotonic()-started
            remainder=first.stdout.read(); errors=first.stderr.read(); first.wait(timeout=10)
            self.assertEqual(0,first.returncode,errors)
            self.assertEqual('ACEPTADO',remainder.strip().splitlines()[-1])
            self.assertEqual('CONFLICTO',second.splitlines()[-1])
            self.assertGreater(elapsed,0.4,'Second connection should wait for resource lock')
        finally:
            if first.poll() is None: first.kill(); first.wait()
    def test_24_two_connections_cannot_occupy_same_resource(self):
        f=fixture(); self._concurrent(f,f,[f['recurso']],[f['recurso']])
        self.assertEqual('1', DB.scalar(f"SELECT COUNT(*) FROM ocupacion_agenda WHERE id_recurso={f['recurso']};"))
    def test_25_shared_musician_blocks_different_bands(self):
        a,b=fixture(),fixture(); key=uuid.uuid4().hex
        resource=int(DB.scalar(f"INSERT INTO usuario(id_rol,nombres,apellidos,correo,password_hash) VALUES(4,'Solista SQL','Fixture','s{key}@example.invalid',REPEAT('x',60)); SET @solista=LAST_INSERT_ID(); INSERT INTO integrante(id_servicio,id_solista_vinculado,nombre,instrumento,estado_vinculo) VALUES({a['servicio']},@solista,'SQL','Instrumento','ACEPTADO'),({b['servicio']},@solista,'SQL','Instrumento','ACEPTADO'); INSERT INTO recurso_agenda(id_musico) VALUES(@solista); SELECT LAST_INSERT_ID();"))
        self._concurrent(a,b,[a['recurso'],resource],[b['recurso'],resource])
    def test_26_pending_contract_cannot_be_reviewed_or_finished_early(self):
        f=fixture(); DB.execute(f"INSERT INTO valoracion(id_contrato,puntuacion) VALUES({f['contrato']},5);",1644)
        DB.execute(f"UPDATE contrato SET estado='FINALIZADO',confirmado_en=UTC_TIMESTAMP(6),finalizado_en='2030-01-02 16:00' WHERE id={f['contrato']};",1644)
    def test_27_historical_foreign_keys_restrict_deletion(self):
        f=fixture(); DB.execute(f"DELETE FROM usuario WHERE id={f['cliente']};",1451)

    def test_28_active_hold_blocks_same_interval(self):
        f=fixture(); p=payment(f)
        DB.execute(f"INSERT INTO retencion_agenda(id_contrato,id_servicio,id_pago,inicio_utc,fin_utc,vence_en) VALUES({f['contrato']},{f['servicio']},{p},'2030-01-02 14:30','2030-01-02 16:30',UTC_TIMESTAMP(6)+INTERVAL 10 MINUTE); SET @retencion=LAST_INSERT_ID(); INSERT INTO ocupacion_agenda(id_recurso,id_retencion,inicio_utc,fin_utc,vence_en) SELECT {f['recurso']},id,inicio_utc,fin_utc,vence_en FROM retencion_agenda WHERE id=@retencion;")
        self.assertEqual('CONFLICTO',DB.execute(candidate(f,[f['recurso']])).splitlines()[-1])
    def test_29_refund_reference_is_unique_across_payments(self):
        f=fixture(); a=payment(f,4001,True); b=payment(f,1000,True); key=uuid.uuid4().hex
        DB.execute(f"INSERT INTO reembolso(id_pago,id_contrato,proveedor,clave_idempotencia,referencia_externa,importe_centimos,motivo) VALUES({a},{f['contrato']},'FIXTURE_SQL_NO_PASARELA','a','{key}',1,'Fixture');")
        DB.execute(f"INSERT INTO reembolso(id_pago,id_contrato,proveedor,clave_idempotencia,referencia_externa,importe_centimos,motivo) VALUES({b},{f['contrato']},'FIXTURE_SQL_NO_PASARELA','b','{key}',1,'Fixture');",1062)
    def test_30_only_one_cover_image_per_service(self):
        f=fixture()
        DB.execute(f"INSERT INTO imagen_servicio(id_servicio,ruta_objeto,mime,tamano_bytes,es_portada) VALUES({f['servicio']},'fixtures/a.jpg','image/jpeg',1,1);")
        DB.execute(f"INSERT INTO imagen_servicio(id_servicio,ruta_objeto,mime,tamano_bytes,es_portada) VALUES({f['servicio']},'fixtures/b.jpg','image/jpeg',1,1);",1062)
    def test_31_session_hash_unique_and_expiry_ordered(self):
        f=fixture(); key=uuid.uuid4().hex
        sql=f"INSERT INTO sesion(id_usuario,familia,refresh_hash,vence_en) VALUES({f['cliente']},'{uuid.uuid4()}',UNHEX(SHA2('{key}',256)),UTC_TIMESTAMP(6)+INTERVAL 1 DAY);"
        DB.execute(sql); DB.execute(sql,1062)
        DB.execute(f"INSERT INTO sesion(id_usuario,familia,refresh_hash,vence_en) VALUES({f['cliente']},'{uuid.uuid4()}',UNHEX(SHA2('otro-{key}',256)),UTC_TIMESTAMP(6)-INTERVAL 1 DAY);",3819)

class EvidenceResult(unittest.TextTestResult):
    def __init__(self,*args,**kwargs):
        super().__init__(*args,**kwargs); self.records=[]
    def startTest(self,test): super().startTest(test); self.started=time.monotonic()
    def addSuccess(self,test):
        super().addSuccess(test); self.records.append({'test':test.id(),'status':'passed','seconds':round(time.monotonic()-self.started,3)})
    def addFailure(self,test,err):
        super().addFailure(test,err); self.records.append({'test':test.id(),'status':'failed','detail':str(err[1])})
    def addError(self,test,err):
        super().addError(test,err); self.records.append({'test':test.id(),'status':'error','detail':str(err[1])})

def main():
    global DB
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql-bin',required=True); parser.add_argument('--port',type=int,required=True)
    parser.add_argument('--database',required=True); parser.add_argument('--report',type=Path,required=True)
    args=parser.parse_args(); DB=Mysql(args.mysql_bin,args.port,args.database)
    report={'started_at_utc':datetime.now(timezone.utc).isoformat(),'database':args.database,'kind':'MYSQL_SQL_FIXTURES_NOT_PROVIDER_SANDBOX','migrations':[]}
    try:
        report['mysql_version']=DB.execute('SELECT VERSION();',use_schema=False)
        DB.execute(f'CREATE DATABASE `{args.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;',use_schema=False)
        for path in sorted((ROOT/'database/migrations').glob('V*.sql')):
            DB.execute(path.read_text(encoding='utf-8-sig'))
            report['migrations'].append({'file':path.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'status':'applied'})
        report['schema_before_fixtures']=DB.json_rows("SELECT JSON_OBJECT('table',table_name,'rows',table_rows,'engine',engine,'comment',table_comment) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' ORDER BY table_name;")
        result=unittest.TextTestRunner(verbosity=2,resultclass=EvidenceResult).run(unittest.defaultTestLoader.loadTestsFromTestCase(SchemaTests))
        report['tests']=result.records; report['success']=result.wasSuccessful()
        report['metadata']={
            'columns':DB.json_rows("SELECT JSON_OBJECT('table',table_name,'name',column_name,'type',column_type,'nullable',is_nullable,'default',column_default,'extra',extra,'generation',generation_expression,'comment',column_comment,'key',column_key) FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position;"),
            'constraints':DB.json_rows("SELECT JSON_OBJECT('table',table_name,'name',constraint_name,'type',constraint_type,'enforced',enforced) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() ORDER BY table_name,constraint_name;"),
            'foreign_keys':DB.json_rows("SELECT JSON_OBJECT('table',k.table_name,'name',k.constraint_name,'column',k.column_name,'parent_table',k.referenced_table_name,'parent_column',k.referenced_column_name,'ordinal',k.ordinal_position,'delete_rule',r.delete_rule,'update_rule',r.update_rule) FROM information_schema.key_column_usage k JOIN information_schema.referential_constraints r ON r.constraint_schema=k.constraint_schema AND r.constraint_name=k.constraint_name AND r.table_name=k.table_name WHERE k.constraint_schema=DATABASE() ORDER BY k.table_name,k.constraint_name,k.ordinal_position;"),
            'indexes':DB.json_rows("SELECT JSON_OBJECT('table',table_name,'name',index_name,'unique',non_unique=0,'column',column_name,'ordinal',seq_in_index) FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index;"),
            'checks':DB.json_rows("SELECT JSON_OBJECT('name',constraint_name,'expression',check_clause) FROM information_schema.check_constraints WHERE constraint_schema=DATABASE() ORDER BY constraint_name;"),
            'views':DB.json_rows("SELECT JSON_OBJECT('name',table_name,'definition',view_definition,'security',security_type) FROM information_schema.views WHERE table_schema=DATABASE() ORDER BY table_name;"),
            'triggers':DB.json_rows("SELECT JSON_OBJECT('table',event_object_table,'name',trigger_name,'event',event_manipulation,'timing',action_timing) FROM information_schema.triggers WHERE trigger_schema=DATABASE() ORDER BY trigger_name;")}
    except Exception as error:
        report['success']=False; report['setup_error']=str(error); raise
    finally:
        report['completed_at_utc']=datetime.now(timezone.utc).isoformat()
        args.report.parent.mkdir(parents=True,exist_ok=True)
        args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return 0 if report['success'] else 1

if __name__=='__main__': raise SystemExit(main())
