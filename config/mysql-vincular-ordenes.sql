-- Migración incremental. Ejecutar una sola vez tras mysql-compras.sql y mysql-ordenes.sql.
-- Revisar columnas existentes antes de ejecutar. No borra ni reasigna compras.
ALTER TABLE portfolios
    ADD COLUMN usuario_id BIGINT NULL,
    ADD CONSTRAINT uq_portfolio_usuario UNIQUE (usuario_id),
    ADD CONSTRAINT fk_portfolio_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE operaciones
    ADD COLUMN orden_detalle_id BIGINT NULL,
    ADD CONSTRAINT uq_operacion_detalle UNIQUE (orden_detalle_id),
    ADD CONSTRAINT fk_operacion_detalle FOREIGN KEY (orden_detalle_id) REFERENCES orden_detalles(id);

-- Los NULL permiten conservar carteras/compras anteriores hasta vincularlas explícitamente.
-- La aplicación exige usuario_id y crea el enlace al detalle para cada compra nueva en MySQL.
