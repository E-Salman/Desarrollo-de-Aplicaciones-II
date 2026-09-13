-- Esquema MySQL de mysql_componente, verificado el 13/09/2026.
-- SOLO estructura: no contiene datos, usuarios de prueba, credenciales ni contadores de registros.
-- Ejecutar únicamente en una base vacía ya seleccionada (MySQL 8.x).
-- Para una base existente usar las migraciones incrementales documentadas en docs/MYSQL_ESQUEMA.md.

CREATE TABLE `activos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `simbolo` varchar(30) NOT NULL,
  `nombre` varchar(255) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `coingecko_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `simbolo` (`simbolo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instrumentos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `simbolo` varchar(30) NOT NULL,
  `nombre` varchar(255) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `exchange` varchar(50) DEFAULT NULL,
  `estado` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_instrumento_simbolo_exchange` (`simbolo`,`exchange`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apellido` varchar(100) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuario_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bonos` (
  `instrumento_id` bigint NOT NULL,
  `fecha_emision` date DEFAULT NULL,
  `fecha_vencimiento` date DEFAULT NULL,
  `tasa_cupon` decimal(10,6) DEFAULT NULL,
  `valor_nominal` decimal(30,10) DEFAULT NULL,
  `moneda_nominal_id` bigint DEFAULT NULL,
  PRIMARY KEY (`instrumento_id`),
  KEY `fk_bonos_moneda` (`moneda_nominal_id`),
  CONSTRAINT `fk_bonos_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bonos_moneda` FOREIGN KEY (`moneda_nominal_id`) REFERENCES `activos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cedears` (
  `instrumento_id` bigint NOT NULL,
  `ratio` decimal(20,10) DEFAULT NULL,
  PRIMARY KEY (`instrumento_id`),
  CONSTRAINT `fk_cedears_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `instrumento_activos` (
  `instrumento_id` bigint NOT NULL,
  `activo_id` bigint NOT NULL,
  `rol` varchar(30) NOT NULL,
  PRIMARY KEY (`instrumento_id`,`activo_id`,`rol`),
  KEY `fk_instrumento_activos_activo` (`activo_id`),
  CONSTRAINT `fk_instrumento_activos_activo` FOREIGN KEY (`activo_id`) REFERENCES `activos` (`id`),
  CONSTRAINT `fk_instrumento_activos_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `precios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instrumento_id` bigint NOT NULL,
  `fecha_hora` datetime NOT NULL,
  `apertura` decimal(30,10) NOT NULL,
  `maximo` decimal(30,10) NOT NULL,
  `minimo` decimal(30,10) NOT NULL,
  `cierre` decimal(30,10) NOT NULL,
  `volumen` decimal(40,10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_precio_instrumento_fecha` (`instrumento_id`,`fecha_hora`),
  KEY `idx_precios_instrumento_fecha` (`instrumento_id`,`fecha_hora`),
  CONSTRAINT `fk_precios_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `portfolios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `propietario` varchar(255) DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_portfolio_propietario` (`propietario`),
  UNIQUE KEY `uq_portfolio_usuario` (`usuario_id`),
  CONSTRAINT `fk_portfolio_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ordenes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` varchar(30) NOT NULL DEFAULT 'COMPLETADA',
  `moneda` varchar(10) NOT NULL,
  `total` decimal(30,10) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_orden_usuario` (`usuario_id`),
  CONSTRAINT `fk_orden_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `orden_detalles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `orden_id` bigint NOT NULL,
  `instrumento_id` bigint NOT NULL,
  `cantidad` decimal(30,15) NOT NULL,
  `precio_unitario` decimal(30,15) NOT NULL,
  `subtotal` decimal(30,15) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_detalle_orden` (`orden_id`),
  KEY `fk_detalle_instrumento` (`instrumento_id`),
  CONSTRAINT `fk_detalle_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`),
  CONSTRAINT `fk_detalle_orden` FOREIGN KEY (`orden_id`) REFERENCES `ordenes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `operaciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `portfolio_id` bigint NOT NULL,
  `instrumento_id` bigint NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `cantidad` decimal(30,10) NOT NULL,
  `precioUnitario` decimal(30,10) NOT NULL,
  `total` decimal(30,10) NOT NULL,
  `fecha` date NOT NULL,
  `orden_detalle_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_operacion_detalle` (`orden_detalle_id`),
  KEY `idx_operaciones_portfolio` (`portfolio_id`,`fecha`,`id`),
  KEY `fk_operaciones_instrumento` (`instrumento_id`),
  CONSTRAINT `fk_operacion_detalle` FOREIGN KEY (`orden_detalle_id`) REFERENCES `orden_detalles` (`id`),
  CONSTRAINT `fk_operaciones_instrumento` FOREIGN KEY (`instrumento_id`) REFERENCES `instrumentos` (`id`),
  CONSTRAINT `fk_operaciones_portfolio` FOREIGN KEY (`portfolio_id`) REFERENCES `portfolios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
