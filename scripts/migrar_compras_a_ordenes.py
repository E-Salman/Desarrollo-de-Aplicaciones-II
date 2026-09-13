"""Vincula compras anteriores con órdenes sin alterar sus cantidades/importes.

Requiere el cliente mysql y un archivo privado de opciones. Sin --apply sólo revisa.
Ejecutar después de mysql-vincular-ordenes.sql y de asociar los portfolios a usuarios.
"""
import argparse
import json
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--mysql", default="mysql", help="Ruta al cliente mysql")
parser.add_argument("--defaults-file", required=True, help="Archivo privado de conexión MySQL")
parser.add_argument("--database", required=True)
parser.add_argument("--apply", action="store_true", help="Aplicar la migración; por defecto sólo inspecciona")


def run(args):
    command = [args.mysql, "--defaults-file=" + args.defaults_file, "--database=" + args.database,
               "--batch", "--raw", "--skip-column-names", "--default-character-set=utf8mb4"]

    def sql(text):
        result = subprocess.run(command, input=text, capture_output=True, text=True, encoding="utf-8")
        if result.returncode:
            # Al fallar el cliente sin --force, se cierra la conexión y revierte la transacción abierta.
            raise RuntimeError(result.stderr.strip())
        return result.stdout

    rows = [json.loads(line) for line in sql("""
        SELECT JSON_OBJECT('id',o.id,'tipo',o.tipo,'usuario',p.usuario_id,
          'moneda',(SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.simbolo) END
             FROM instrumento_activos ia JOIN activos a ON a.id=ia.activo_id
             WHERE ia.instrumento_id=o.instrumento_id AND ia.rol='COTIZACION'))
        FROM operaciones o JOIN portfolios p ON p.id=o.portfolio_id
        WHERE o.orden_detalle_id IS NULL ORDER BY o.id;
        """).splitlines() if line]
    for row in rows:
        if row["tipo"] != "COMPRA" or row["usuario"] is None or not row["moneda"] or len(row["moneda"]) > 10:
            raise RuntimeError("Revisar identidad, tipo y moneda de la operación " + str(row["id"]))
    print("Compras pendientes de vincular:", len(rows))
    if not args.apply or not rows:
        return
    statements = ["START TRANSACTION;"]
    for row in rows:
        operation_id = int(row["id"])
        statements.append(f"""
            SELECT id FROM operaciones WHERE id={operation_id} FOR UPDATE;
            INSERT INTO ordenes(usuario_id,fecha_hora,estado,moneda,total)
            SELECT p.usuario_id,CAST(o.fecha AS DATETIME),'COMPLETADA',
                (SELECT CASE WHEN COUNT(*)=1 THEN MAX(a.simbolo) END
                 FROM instrumento_activos ia JOIN activos a ON a.id=ia.activo_id
                 WHERE ia.instrumento_id=o.instrumento_id AND ia.rol='COTIZACION'),o.total
            FROM operaciones o JOIN portfolios p ON p.id=o.portfolio_id
            WHERE o.id={operation_id} AND o.tipo='COMPRA' AND o.orden_detalle_id IS NULL;
            SET @orden_creada=IF(ROW_COUNT()=1,LAST_INSERT_ID(),NULL);
            INSERT INTO orden_detalles(orden_id,instrumento_id,cantidad,precio_unitario,subtotal)
            SELECT @orden_creada,instrumento_id,cantidad,precioUnitario,total
            FROM operaciones WHERE id={operation_id} AND orden_detalle_id IS NULL AND @orden_creada IS NOT NULL;
            SET @detalle_creado=IF(ROW_COUNT()=1,LAST_INSERT_ID(),NULL);
            UPDATE operaciones SET orden_detalle_id=@detalle_creado
            WHERE id={operation_id} AND orden_detalle_id IS NULL AND @detalle_creado IS NOT NULL;
            """)
    statements.append("COMMIT;")
    sql("\n".join(statements))
    print("Migración confirmada. Las compras ya vinculadas no se duplican al repetirla.")


if __name__ == "__main__":
    run(parser.parse_args())
