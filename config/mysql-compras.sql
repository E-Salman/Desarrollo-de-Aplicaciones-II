-- Ejecutar después de importar instrumentos.sql, en la base del proyecto.
-- Conserva las siete tablas del catálogo. No carga datos de demostración.
CREATE TABLE IF NOT EXISTS portfolios (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  propietario VARCHAR(255) NULL,
  UNIQUE KEY uq_portfolio_propietario (propietario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS operaciones (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  portfolio_id BIGINT NOT NULL,
  instrumento_id BIGINT NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  cantidad DECIMAL(30,10) NOT NULL,
  precioUnitario DECIMAL(30,10) NOT NULL,
  total DECIMAL(30,10) NOT NULL,
  fecha DATE NOT NULL,
  KEY idx_operaciones_portfolio (portfolio_id,fecha,id),
  CONSTRAINT fk_operaciones_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id),
  CONSTRAINT fk_operaciones_instrumento FOREIGN KEY (instrumento_id) REFERENCES instrumentos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
