-- ============================================================
-- Gwent Card Catalog - Seed Data
-- Ejecutar UNA sola vez en la base de datos gwent:
--   mysql -u root -p gwent < seed-data.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS gw_carta_catalogo (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(255)    NOT NULL,
    faccion             VARCHAR(30)     NOT NULL,
    tipo                VARCHAR(20)     NOT NULL,
    fila                VARCHAR(25)     NULL,
    fuerza              INT             NULL,
    fuerza_transformada INT             NULL,
    habilidad           VARCHAR(30)     NOT NULL,
    es_heroe            TINYINT(1)      NOT NULL DEFAULT 0,
    max_copias          INT             NOT NULL DEFAULT 1,
    imagen_url          VARCHAR(255)    NULL,
    created_at          DATETIME        NOT NULL,
    modified_at         DATETIME        NULL,
    deleted_at          DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Columnas nuevas para ejecuciones sobre base existente (idempotente si ya existen)
ALTER TABLE gw_carta_catalogo ADD COLUMN IF NOT EXISTS fuerza_transformada INT NULL AFTER fuerza;
ALTER TABLE gw_carta_partida  ADD COLUMN IF NOT EXISTS transformado      TINYINT(1) NOT NULL DEFAULT 0 AFTER fila_en_campo;
ALTER TABLE gw_carta_partida  ADD COLUMN IF NOT EXISTS es_slot_lateral   TINYINT(1) NOT NULL DEFAULT 0 AFTER transformado;

CREATE TABLE IF NOT EXISTS gw_carta_jugador (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    jugador_id        VARCHAR(36) NOT NULL,
    carta_catalogo_id BIGINT      NOT NULL,
    cantidad          INT         NOT NULL DEFAULT 1,
    unlocked_at       DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jugador_carta (jugador_id, carta_catalogo_id),
    CONSTRAINT fk_carta_jugador_catalogo FOREIGN KEY (carta_catalogo_id)
        REFERENCES gw_carta_catalogo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO gw_carta_catalogo (nombre, faccion, tipo, fila, fuerza, fuerza_transformada, habilidad, es_heroe, max_copias, imagen_url, created_at, modified_at, deleted_at) VALUES

-- ===== REINO DEL NORTE =====
-- Heroes: max 1 copia. Unidades normales: max 3 copias.
('Geralt de Rivia',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO', 15, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Triss Merigold',           'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  7, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Yennefer de Vengerberg',   'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, NULL, 'MEDICO',                true,  1, NULL, NOW(), NULL, NULL),
('Ciri',                     'REINO_DEL_NORTE', 'UNIDAD',   'AGIL',            15, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Dandelion',                'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        2, NULL, 'CUERNO_DEL_COMANDANTE', false, 1, NULL, NOW(), NULL, NULL),
('Espia de Temeria',         'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, NULL, 'ESPIA',                 false, 3, NULL, NOW(), NULL, NULL),
('Francotirador de Temeria', 'REINO_DEL_NORTE', 'UNIDAD',   'ASEDIO',           6, NULL, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Medico de campo',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  5, NULL, 'MEDICO',                false, 3, NULL, NOW(), NULL, NULL),
('Guerrero de la guardia',   'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  4, NULL, 'VINCULO_ESTRECHO',      false, 3, NULL, NOW(), NULL, NULL),

-- ===== NILFGAARD =====
('Emhyr var Emreis',         'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  8, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Espia Imperial',           'NILFGAARD',       'UNIDAD',   'DISTANCIA',        0, NULL, 'ESPIA',                 false, 3, NULL, NOW(), NULL, NULL),
('Ballestero imperial',      'NILFGAARD',       'UNIDAD',   'DISTANCIA',        6, NULL, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Legionario nilfgaardiano', 'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  4, NULL, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),

-- ===== MONSTRUOS =====
('Draug',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Garra',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, NULL, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),
('Bruja de medianoche',      'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, NULL, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),
('Imlerith',                 'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, NULL, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Bestia del pantano',       'MONSTRUOS',       'UNIDAD',   'ASEDIO',           4, NULL, 'NINGUNA',               false, 3, NULL, NOW(), NULL, NULL),

-- ===== SCOIA_TAEL =====
('Francotirador elfico',     'SCOIA_TAEL',      'UNIDAD',   'DISTANCIA',        5, NULL, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Guerrero dryade',          'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, NULL, 'NINGUNA',               false, 3, NULL, NOW(), NULL, NULL),
('Aglais',                   'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, NULL, 'MEDICO',                true,  1, NULL, NOW(), NULL, NULL),

-- ===== SKELLIGE =====
('Crach an Craite',          'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  9, NULL, 'REFUERZO_MORAL',        true,  1, NULL, NOW(), NULL, NULL),
-- "Berserker skellige" existente: ahora tiene habilidad BERSERKER y fuerza_transformada=8
('Berserker skellige',       'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  6,    8, 'BERSERKER',             false, 3, NULL, NOW(), NULL, NULL),
('Musico berserker',         'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  6, NULL, 'VINCULO_ESTRECHO',      false, 3, NULL, NOW(), NULL, NULL),

-- ===== NEUTRAL =====
-- Especiales y clima: max 1 copia. Ballista (unidad): max 3.
('Ballista',                 'NEUTRAL',         'UNIDAD',   'ASEDIO',           6, NULL, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Senuelo',                  'NEUTRAL',         'ESPECIAL',  NULL,            NULL, NULL, 'DECOY',                 false, 1, NULL, NOW(), NULL, NULL),
('Tormenta de escarcha',     'NEUTRAL',         'CLIMA',     'CUERPO_A_CUERPO', NULL, NULL, 'NINGUNA',             false, 1, NULL, NOW(), NULL, NULL),
('Lluvia acida',             'NEUTRAL',         'CLIMA',     'DISTANCIA',       NULL, NULL, 'NINGUNA',             false, 1, NULL, NOW(), NULL, NULL),
('Niebla espesa',            'NEUTRAL',         'CLIMA',     'ASEDIO',          NULL, NULL, 'NINGUNA',             false, 1, NULL, NOW(), NULL, NULL),
('Toque de Axii',            'NEUTRAL',         'ESPECIAL',  NULL,              NULL, NULL, 'ESCUDO_IMPENETRABLE', false, 1, NULL, NOW(), NULL, NULL),
('Buen clima',               'NEUTRAL',         'ESPECIAL',  NULL,              NULL, NULL, 'CLIMA_LIMPIO',        false, 1, NULL, NOW(), NULL, NULL),

-- ===== NUEVAS CARTAS =====
-- Cuerno del Comandante: ocupa slot lateral de la fila elegida, dobla unidades propias
('Cuerno del Comandante',    'NEUTRAL',         'ESPECIAL',  NULL,              NULL, NULL, 'CUERNO_DEL_COMANDANTE', false, 1, NULL, NOW(), NULL, NULL),
-- Scorch instantáneo: destruye la(s) unidad(es) más fuerte(s) de ambos campos si total >= 10
('Scorch',                   'NEUTRAL',         'ESPECIAL',  NULL,              NULL, NULL, 'SCORCH',              false, 1, NULL, NOW(), NULL, NULL),
-- Mardroeme: ocupa slot lateral, transforma Berserkers en su fila
('Mardroeme',                'SKELLIGE',        'ESPECIAL',  NULL,              NULL, NULL, 'MARDROEME',           false, 1, NULL, NOW(), NULL, NULL),
-- Clan Dimun Pirate: unidad Skellige con Scorch al jugar (campo completo)
('Clan Dimun Pirate',        'SKELLIGE',        'UNIDAD',   'ASEDIO',           6, NULL, 'SCORCH',                false, 3, NULL, NOW(), NULL, NULL),
-- Tormenta de Skellige: clima que afecta DISTANCIA y ASEDIO simultáneamente
('Tormenta de Skellige',     'SKELLIGE',        'CLIMA',     NULL,              NULL, NULL, 'TORMENTA_SKELLIGE',   false, 1, NULL, NOW(), NULL, NULL),

-- ===== LIDERES (tipo=LIDER, fila=NULL, fuerza=NULL, es_heroe=true, max_copias=1) =====
-- REINO_DEL_NORTE
('Foltest: Rey de Temeria',               'REINO_DEL_NORTE', 'LIDER', NULL, NULL, NULL, 'LIDER_PICK_FOG',           true, 1, NULL, NOW(), NULL, NULL),
('Foltest: Senor Comandante del Norte',   'REINO_DEL_NORTE', 'LIDER', NULL, NULL, NULL, 'LIDER_CLEAR_WEATHER',      true, 1, NULL, NOW(), NULL, NULL),
('Foltest: El Sitiador',                  'REINO_DEL_NORTE', 'LIDER', NULL, NULL, NULL, 'LIDER_DOUBLE_SIEGE',       true, 1, NULL, NOW(), NULL, NULL),
('Foltest: Hijo de Medell',               'REINO_DEL_NORTE', 'LIDER', NULL, NULL, NULL, 'LIDER_SCORCH_RANGED',      true, 1, NULL, NOW(), NULL, NULL),
-- NILFGAARD
('Emhyr: El Blanco Llama',               'NILFGAARD',       'LIDER', NULL, NULL, NULL, 'LIDER_CANCEL_LEADER',      true, 1, NULL, NOW(), NULL, NULL),
('Emhyr: Senor de las Aguas',            'NILFGAARD',       'LIDER', NULL, NULL, NULL, 'LIDER_STEAL_GRAVEYARD',    true, 1, NULL, NOW(), NULL, NULL),
('Emhyr: Su Majestad Imperial',           'NILFGAARD',       'LIDER', NULL, NULL, NULL, 'LIDER_PICK_RAIN',          true, 1, NULL, NOW(), NULL, NULL),
('Emhyr: Emperador de Nilfgaard',         'NILFGAARD',       'LIDER', NULL, NULL, NULL, 'LIDER_REVEAL_HAND',        true, 1, NULL, NOW(), NULL, NULL),
-- MONSTRUOS
('Eredin: Rey de la Caceria',            'MONSTRUOS',       'LIDER', NULL, NULL, NULL, 'LIDER_RESTORE_GRAVEYARD',  true, 1, NULL, NOW(), NULL, NULL),
('Eredin: Rompenieblas',                 'MONSTRUOS',       'LIDER', NULL, NULL, NULL, 'LIDER_DISCARD_DRAW',       true, 1, NULL, NOW(), NULL, NULL),
('Eredin: El Traicionero',               'MONSTRUOS',       'LIDER', NULL, NULL, NULL, 'LIDER_DOUBLE_SPIES',       true, 1, NULL, NOW(), NULL, NULL),
('Eredin: Comandante de los Jinetes Rojos','MONSTRUOS',     'LIDER', NULL, NULL, NULL, 'LIDER_DOUBLE_CAC',         true, 1, NULL, NOW(), NULL, NULL),
-- SCOIA_TAEL
('Francesca: La Mas Hermosa',            'SCOIA_TAEL',      'LIDER', NULL, NULL, NULL, 'LIDER_DOUBLE_RANGED',      true, 1, NULL, NOW(), NULL, NULL),
('Francesca: Enid an Gleanna',           'SCOIA_TAEL',      'LIDER', NULL, NULL, NULL, 'LIDER_SCORCH_CAC',         true, 1, NULL, NOW(), NULL, NULL),
('Francesca: Elfa de Sangre Pura',       'SCOIA_TAEL',      'LIDER', NULL, NULL, NULL, 'LIDER_PICK_FROST',         true, 1, NULL, NOW(), NULL, NULL),
('Francesca: Flor del Valle',            'SCOIA_TAEL',      'LIDER', NULL, NULL, NULL, 'LIDER_DRAW_EXTRA',         true, 1, NULL, NOW(), NULL, NULL),
-- SKELLIGE
('Crach an Craite: Senor de Ard Skellig','SKELLIGE',        'LIDER', NULL, NULL, NULL, 'LIDER_SHUFFLE_GRAVEYARDS', true, 1, NULL, NOW(), NULL, NULL),
('Birna Bran',                           'SKELLIGE',        'LIDER', NULL, NULL, NULL, 'LIDER_HALF_WEATHER',       true, 1, NULL, NOW(), NULL, NULL);

-- ===== COLUMNAS NUEVAS EN gw_partida (idempotente) =====
ALTER TABLE gw_partida ADD COLUMN IF NOT EXISTS lider_usado_jugador_uno  TINYINT(1)   NOT NULL DEFAULT 0;
ALTER TABLE gw_partida ADD COLUMN IF NOT EXISTS lider_usado_jugador_dos  TINYINT(1)   NOT NULL DEFAULT 0;
ALTER TABLE gw_partida ADD COLUMN IF NOT EXISTS cartas_reveladas_j1      VARCHAR(200) NULL;
ALTER TABLE gw_partida ADD COLUMN IF NOT EXISTS cartas_reveladas_j2      VARCHAR(200) NULL;
