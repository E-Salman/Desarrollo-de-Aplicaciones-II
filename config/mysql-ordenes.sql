-- Ejecutar en la base del proyecto después de importar instrumentos.sql.
-- Requiere usuarios e instrumentos. No modifica las compras existentes.
-- Ejecutar una sola vez: si las tablas existen, revisar antes su estructura.
CREATE TABLE ordenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'COMPLETADA',
    moneda VARCHAR(10) NOT NULL,
    total DECIMAL(30,10) NOT NULL,

    CONSTRAINT fk_orden_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE orden_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    instrumento_id BIGINT NOT NULL,

    cantidad DECIMAL(30,15) NOT NULL,
    precio_unitario DECIMAL(30,15) NOT NULL,
    subtotal DECIMAL(30,15) NOT NULL,

    CONSTRAINT fk_detalle_orden
        FOREIGN KEY (orden_id)
        REFERENCES ordenes(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_detalle_instrumento
        FOREIGN KEY (instrumento_id)
        REFERENCES instrumentos(id)
) ENGINE=InnoDB;
