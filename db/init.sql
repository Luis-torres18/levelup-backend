-- =====================================================================
--  Level-Up Gamer - Script de base de datos (MySQL 8)
--  Crea las 2 bases, sus tablas y los datos iniciales.
--  * En Docker se ejecuta solo la PRIMERA vez que se crea el volumen de MySQL.
--  * También puedes ejecutarlo a mano (MySQL Workbench / consola): es idempotente.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
--  Base de usuarios: la usan ms-usuarios (registro/login) y
--  ms-gestion-usuarios (CRUD del ADMIN)
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS levelup_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE levelup_usuarios;

CREATE TABLE IF NOT EXISTS usuarios (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(50)  NOT NULL,
    correo         VARCHAR(100) NOT NULL,
    contrasena     VARCHAR(100) NOT NULL COMMENT 'Hash BCrypt, nunca texto plano',
    telefono       VARCHAR(20)  NULL,
    region         VARCHAR(60)  NOT NULL,
    comuna         VARCHAR(60)  NOT NULL,
    rol            VARCHAR(20)  NOT NULL DEFAULT 'CLIENTE' COMMENT 'ADMIN o CLIENTE',
    descuento_duoc BIT(1)       NOT NULL DEFAULT b'0',
    creado_el      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_correo (correo),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'CLIENTE'))
) ENGINE = InnoDB;

-- Administrador inicial
--   correo:     admin@duocuc.cl
--   contraseña: Admin123!      (cámbiala después desde la pantalla de Administración)
INSERT IGNORE INTO usuarios (nombre, correo, contrasena, telefono, region, comuna, rol, descuento_duoc)
VALUES ('Administrador', 'admin@duocuc.cl',
        '$2a$10$KFSKWvbuTU2klG8UGzBdrOocLLT6ijTEJQAHM.Bs.OtI6MfO4Pslu',
        NULL, 'Metropolitana de Santiago', 'Santiago', 'ADMIN', b'1');

-- ---------------------------------------------------------------------
--  Base de productos: la usa ms-productos
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS levelup_productos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE levelup_productos;

CREATE TABLE IF NOT EXISTS productos (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    categoria   VARCHAR(60)   NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    descripcion VARCHAR(500)  NOT NULL,
    imagen      VARCHAR(1000) NOT NULL,
    precio      INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_productos_precio CHECK (precio > 0)
) ENGINE = InnoDB;

-- Catálogo inicial (INSERT IGNORE: si el id ya existe no lo duplica)
INSERT IGNORE INTO productos (id, categoria, nombre, descripcion, imagen, precio) VALUES
  (1, 'Juegos de Mesa', 'Catan', 'Clásico juego de estrategia para colonizar la isla de Catan (3-4 jugadores).', 'https://devirinvestments.s3.eu-west-1.amazonaws.com/img/catalog/product/8436017220100-1200-frontflat.jpg', 29990),
  (2, 'Juegos de Mesa', 'Carcassonne', 'Coloca losetas y domina fortalezas medievales con tu estrategia.', 'https://www.geekz.cl/web/image/product.template/21455/image', 24990),
  (3, 'Accesorios', 'Control Xbox Series X', 'Agarre texturizado y precisión inalámbrica para Xbox y PC.', 'https://i5.walmartimages.com/seo/Microsoft-Xbox-One-Bluetooth-Wireless-Controller-Black_b30e1557-556d-4638-a692-7b42cb425b52_1.3d21d0fb85ffc29ebc3435b2d1bd3d75.jpeg', 59990),
  (4, 'Accesorios', 'HyperX Cloud II', 'Audio envolvente virtual 7.1 con máxima comodidad de espuma viscoelástica.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSdO6mRCcRglH7n3EMSCivY3XAlU0QezZArTzefhyhxrnk54EqblxI_yQCN&s=10', 79990),
  (5, 'Consolas', 'PlayStation 5', 'Gráficos 4K con trazado de rayos y retroalimentación háptica inmersiva.', 'https://www.weplay.cl/pub/media/wysiwyg/PRODUCTOS/IMAGENES/PLAYSTATION/711719570820_2.jpg', 549990),
  (6, 'Computadores', 'PC ASUS ROG Strix', 'Componentes de vanguardia para jugar sin límites competitivos.', 'https://rimage.ripley.cl/home.ripley/Attachment/WOP/1/2000408648833/full_image-2000408648833', 1299990),
  (7, 'Sillas Gamers', 'Secretlab Titan', 'Ergonomía superior diseñada para sesiones intensas de juego.', 'https://m.media-amazon.com/images/I/41wKF+jkOAL._AC_.jpg', 349990),
  (8, 'Mouse', 'Logitech G502 HERO', 'Sensor de alta precisión de 25.600 DPI con 11 botones personalizables.', 'https://http2.mlstatic.com/D_NQ_NP_913004-MLA99443804514_112025-O.webp', 49990);
