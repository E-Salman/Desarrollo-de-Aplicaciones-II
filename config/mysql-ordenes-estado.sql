-- Entrega 2 (mensajería): datos que el Ejecutor de órdenes necesita para ejecutar una orden
-- después de aceptarla. Ejecutar en la base del proyecto, después de mysql-vincular-ordenes.sql.
-- Se puede repetir: cada columna se agrega solo si falta. No borra ni modifica órdenes existentes
-- salvo completar fecha_operacion desde su operación. Los estados existentes ('COMPLETADA') no se migran.

-- tipo: COMPRA o VENTA. Las órdenes anteriores son todas compras.
SET @falta := (SELECT COUNT(*) = 0 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'ordenes' AND column_name = 'tipo');
SET @sql := IF(@falta, 'ALTER TABLE ordenes ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT ''COMPRA'' AFTER usuario_id', 'DO 0');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- fecha_operacion: la fecha que eligió el usuario (fecha_hora es cuándo se cargó la orden).
SET @falta := (SELECT COUNT(*) = 0 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'ordenes' AND column_name = 'fecha_operacion');
SET @sql := IF(@falta, 'ALTER TABLE ordenes ADD COLUMN fecha_operacion DATE NULL AFTER fecha_hora', 'DO 0');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- motivo_rechazo: solo en órdenes RECHAZADA o ERROR.
SET @falta := (SELECT COUNT(*) = 0 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'ordenes' AND column_name = 'motivo_rechazo');
SET @sql := IF(@falta, 'ALTER TABLE ordenes ADD COLUMN motivo_rechazo VARCHAR(255) NULL AFTER estado', 'DO 0');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- Órdenes anteriores: la fecha elegida está en su operación. MySQL Workbench trae activado el "safe
-- update mode", que rechaza este UPDATE (error 1175): se desactiva solo durante esta sentencia.
SET @modo_seguro := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
UPDATE ordenes o
JOIN orden_detalles d ON d.orden_id = o.id
JOIN operaciones op ON op.orden_detalle_id = d.id
SET o.fecha_operacion = op.fecha
WHERE o.fecha_operacion IS NULL;
SET SQL_SAFE_UPDATES = @modo_seguro;
