-- ============================================================
-- CoilDefectCatalog - Script de creación de base de datos
-- Motor: Oracle Database (12c+ requerido por IDENTITY)
-- ============================================================

-- Elimina las tablas si ya existen, en orden inverso a las dependencias
-- (envuelto en bloques PL/SQL porque Oracle no tiene "DROP TABLE IF EXISTS")
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE foto CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE solucion CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE causa CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE defecto CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE patron_visual CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE categoria CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

-- ============================================================
-- Tabla: categoria
-- ============================================================
CREATE TABLE categoria (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL UNIQUE
);

-- ============================================================
-- Tabla: patron_visual
-- ============================================================
CREATE TABLE patron_visual (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR2(50) NOT NULL UNIQUE
);

-- ============================================================
-- Tabla: defecto
-- ============================================================
CREATE TABLE defecto (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR2(150) NOT NULL,
    categoria_id NUMBER NOT NULL,
    patron_visual_id NUMBER NOT NULL,
    descripcion CLOB NOT NULL,
    codigo_iso VARCHAR2(30) NULL,
    CONSTRAINT fk_defecto_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_defecto_patron
        FOREIGN KEY (patron_visual_id) REFERENCES patron_visual(id)
);

-- ============================================================
-- Tabla: causa (1:N simple respecto a defecto)
-- ============================================================
CREATE TABLE causa (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    defecto_id NUMBER NOT NULL,
    descripcion CLOB NOT NULL,
    CONSTRAINT fk_causa_defecto
        FOREIGN KEY (defecto_id) REFERENCES defecto(id) ON DELETE CASCADE
);

-- ============================================================
-- Tabla: solucion (1:N simple respecto a defecto)
-- ============================================================
CREATE TABLE solucion (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    defecto_id NUMBER NOT NULL,
    descripcion CLOB NOT NULL,
    CONSTRAINT fk_solucion_defecto
        FOREIGN KEY (defecto_id) REFERENCES defecto(id) ON DELETE CASCADE
);

-- ============================================================
-- Tabla: foto
-- defecto_id es NULLABLE a propósito: refleja fotos subidas sin
-- clasificar todavía, que se asignan a un defecto más adelante.
-- Oracle no soporta ON DELETE SET NULL + ON UPDATE CASCADE juntos
-- como MySQL; ON DELETE SET NULL es suficiente aquí.
-- ============================================================
CREATE TABLE foto (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    defecto_id NUMBER NULL,
    ruta_archivo VARCHAR2(255) NOT NULL,
    fecha DATE NOT NULL,
    notas CLOB NULL,
    CONSTRAINT fk_foto_defecto
        FOREIGN KEY (defecto_id) REFERENCES defecto(id) ON DELETE SET NULL
);

-- Índices
CREATE INDEX idx_foto_defecto_null ON foto(defecto_id);
CREATE INDEX idx_defecto_categoria ON defecto(categoria_id);


-- ============================================================
-- DATOS DE EJEMPLO
-- ============================================================

INSERT INTO categoria (nombre) VALUES ('Adherencia');
INSERT INTO categoria (nombre) VALUES ('Brillo');
INSERT INTO categoria (nombre) VALUES ('Cráteres');
INSERT INTO categoria (nombre) VALUES ('Descuelgues');
INSERT INTO categoria (nombre) VALUES ('Contaminación');
INSERT INTO categoria (nombre) VALUES ('Espesor irregular');

INSERT INTO patron_visual (nombre) VALUES ('Puntual');
INSERT INTO patron_visual (nombre) VALUES ('Longitudinal');
INSERT INTO patron_visual (nombre) VALUES ('Difuso');

-- categoria_id y patron_visual_id referencian el orden de inserción anterior (1-6 y 1-3)
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Pérdida de adherencia intercapa', 1, 3,
     'Delaminación entre la capa de primer y la capa de acabado, detectable mediante ensayo de corte cruzado.',
     'ISO 4628-1');
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Bajo brillo generalizado', 2, 3,
     'Reducción del brillo especular por debajo del valor de referencia del panel maestro, sin defecto puntual localizado.',
     NULL);
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Cráter por contaminación de silicona', 3, 1,
     'Pequeña depresión circular en la superficie causada por tensión superficial generada por partículas de silicona.',
     'ISO 4628-5');
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Descuelgue por exceso de espesor húmedo', 4, 2,
     'Acumulación de pintura que fluye verticalmente antes del curado, formando una franja longitudinal de espesor irregular.',
     NULL);
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Contaminación por partículas de horno', 5, 1,
     'Inclusión de partículas sólidas (óxido, polvo) procedentes del horno de curado, visibles como puntos en relieve.',
     'ISO 4628-2');
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Espesor de película insuficiente', 6, 2,
     'Zona con espesor de capa seca por debajo de la tolerancia especificada, medido con galga magnética.',
     NULL);
INSERT INTO defecto (nombre, categoria_id, patron_visual_id, descripcion, codigo_iso) VALUES
    ('Piel de naranja excesiva', 2, 3,
     'Textura superficial rugosa similar a la cáscara de naranja, causada por mala nivelación de la pintura antes del curado.',
     NULL);

-- Causas (defecto_id referencia el orden de inserción de defecto: 1-7)
INSERT INTO causa (defecto_id, descripcion) VALUES (1, 'Contaminación superficial del sustrato antes de aplicar el primer.');
INSERT INTO causa (defecto_id, descripcion) VALUES (1, 'Tiempo de curado insuficiente entre capas.');
INSERT INTO causa (defecto_id, descripcion) VALUES (2, 'Temperatura de horneado por debajo del rango especificado.');
INSERT INTO causa (defecto_id, descripcion) VALUES (2, 'Envejecimiento del baño de pintura fuera de los parámetros de viscosidad.');
INSERT INTO causa (defecto_id, descripcion) VALUES (3, 'Presencia de silicona en la línea de aplicación, habitualmente por contaminación cruzada de otro proceso.');
INSERT INTO causa (defecto_id, descripcion) VALUES (4, 'Espesor húmedo aplicado superior al recomendado por el fabricante de la pintura.');
INSERT INTO causa (defecto_id, descripcion) VALUES (4, 'Velocidad de línea demasiado baja combinada con viscosidad alta.');
INSERT INTO causa (defecto_id, descripcion) VALUES (5, 'Mantenimiento insuficiente del horno de curado.');
INSERT INTO causa (defecto_id, descripcion) VALUES (5, 'Filtración de aire deficiente en la zona de aplicación.');
INSERT INTO causa (defecto_id, descripcion) VALUES (6, 'Ajuste incorrecto del rodillo aplicador (gap).');
INSERT INTO causa (defecto_id, descripcion) VALUES (6, 'Viscosidad de la pintura por debajo del rango de proceso.');
INSERT INTO causa (defecto_id, descripcion) VALUES (7, 'Mala nivelación por secado superficial demasiado rápido.');
INSERT INTO causa (defecto_id, descripcion) VALUES (7, 'Presión de pulverización incorrecta en boquillas.');

-- Soluciones (defecto_id referencia el orden de inserción de defecto: 1-7)
INSERT INTO solucion (defecto_id, descripcion) VALUES (1, 'Reforzar el proceso de limpieza y desengrase del sustrato antes de imprimar.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (1, 'Ajustar el perfil de temperatura del horno para garantizar curado completo entre capas.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (2, 'Verificar y calibrar las sondas de temperatura del horno.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (2, 'Sustituir o acondicionar el baño de pintura según la ficha técnica del fabricante.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (3, 'Aislar la línea de posibles fuentes de silicona y auditar procesos cercanos.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (4, 'Ajustar el espesor húmedo aplicado según la ficha técnica de la pintura.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (4, 'Incrementar la velocidad de línea o reducir la viscosidad dentro de rango.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (5, 'Establecer un plan de mantenimiento preventivo y limpieza periódica del horno.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (5, 'Revisar y mejorar el sistema de filtración de aire.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (6, 'Recalibrar el gap del rodillo aplicador según especificación.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (6, 'Ajustar la viscosidad de la pintura al rango de proceso indicado.');
INSERT INTO solucion (defecto_id, descripcion) VALUES (7, 'Ajustar el tiempo de nivelación antes de la entrada al horno (flash-off).');
INSERT INTO solucion (defecto_id, descripcion) VALUES (7, 'Revisar y calibrar la presión de las boquillas de pulverización.');

-- Fotos: algunas clasificadas (defecto_id asignado), otras pendientes (NULL)
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (1, '/fotos/2026/adherencia_001.jpg', DATE '2026-03-10', 'Ensayo de corte cruzado, panel lote 4471.');
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (3, '/fotos/2026/crater_001.jpg', DATE '2026-03-12', 'Detalle a 10x, zona central de bobina.');
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (3, '/fotos/2026/crater_002.jpg', DATE '2026-03-12', 'Vista general de la zona afectada.');
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (5, '/fotos/2026/contaminacion_001.jpg', DATE '2026-04-02', NULL);
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (NULL, '/fotos/2026/pendiente_001.jpg', DATE '2026-04-15', 'Pendiente de clasificar - lote 4502.');
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (NULL, '/fotos/2026/pendiente_002.jpg', DATE '2026-04-15', 'Pendiente de clasificar - lote 4502.');
INSERT INTO foto (defecto_id, ruta_archivo, fecha, notas) VALUES
    (NULL, '/fotos/2026/pendiente_003.jpg', DATE '2026-04-16', NULL);

COMMIT;
