-- ============================================================
-- Gwent Card Catalog - Seed Data
-- Ejecutar UNA sola vez en la base de datos gwent:
--   mysql -u root -p gwent < seed-data.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS gw_carta_catalogo (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(255)    NOT NULL,
    faccion     VARCHAR(30)     NOT NULL,
    tipo        VARCHAR(20)     NOT NULL,
    fila        VARCHAR(25)     NULL,
    fuerza      INT             NULL,
    habilidad   VARCHAR(30)     NOT NULL,
    es_heroe    TINYINT(1)      NOT NULL DEFAULT 0,
    max_copias  INT             NOT NULL DEFAULT 1,
    imagen_url  VARCHAR(255)    NULL,
    created_at  DATETIME        NOT NULL,
    modified_at DATETIME        NULL,
    deleted_at  DATETIME        NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

INSERT INTO gw_carta_catalogo (nombre, faccion, tipo, fila, fuerza, habilidad, es_heroe, max_copias, imagen_url, created_at, modified_at, deleted_at) VALUES

-- ===== REINO DEL NORTE =====
-- Heroes: max 1 copia. Unidades normales: max 3 copias.
('Geralt de Rivia',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO', 15, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Triss Merigold',           'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  7, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Yennefer de Vengerberg',   'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, 'MEDICO',                true,  1, NULL, NOW(), NULL, NULL),
('Ciri',                     'REINO_DEL_NORTE', 'UNIDAD',   'AGIL',            15, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Dandelion',                'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        2, 'COMANDANTE_INVOCACION', false, 1, NULL, NOW(), NULL, NULL),
('Espia de Temeria',         'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, 'ESPIA',                 false, 3, NULL, NOW(), NULL, NULL),
('Francotirador de Temeria', 'REINO_DEL_NORTE', 'UNIDAD',   'ASEDIO',           6, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Medico de campo',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  5, 'MEDICO',                false, 3, NULL, NOW(), NULL, NULL),
('Guerrero de la guardia',   'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  4, 'VINCULO_ESTRECHO',      false, 3, NULL, NOW(), NULL, NULL),

-- ===== NILFGAARD =====
('Emhyr var Emreis',         'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  8, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Espia Imperial',           'NILFGAARD',       'UNIDAD',   'DISTANCIA',        0, 'ESPIA',                 false, 3, NULL, NOW(), NULL, NULL),
('Ballestero imperial',      'NILFGAARD',       'UNIDAD',   'DISTANCIA',        6, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Legionario nilfgaardiano', 'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  4, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),

-- ===== MONSTRUOS =====
('Draug',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Garra',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),
('Bruja de medianoche',      'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MUSTER',                false, 3, NULL, NOW(), NULL, NULL),
('Imlerith',                 'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, 'NINGUNA',               true,  1, NULL, NOW(), NULL, NULL),
('Bestia del pantano',       'MONSTRUOS',       'UNIDAD',   'ASEDIO',           4, 'NINGUNA',               false, 3, NULL, NOW(), NULL, NULL),

-- ===== SCOIA_TAEL =====
('Francotirador elfico',     'SCOIA_TAEL',      'UNIDAD',   'DISTANCIA',        5, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Guerrero dryade',          'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, 'NINGUNA',               false, 3, NULL, NOW(), NULL, NULL),
('Aglais',                   'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MEDICO',                true,  1, NULL, NOW(), NULL, NULL),

-- ===== SKELLIGE =====
('Crach an Craite',          'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  9, 'REFUERZO_MORAL',        true,  1, NULL, NOW(), NULL, NULL),
('Berserker skellige',       'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  8, 'NINGUNA',               false, 3, NULL, NOW(), NULL, NULL),
('Musico berserker',         'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  6, 'VINCULO_ESTRECHO',      false, 3, NULL, NOW(), NULL, NULL),

-- ===== NEUTRAL =====
-- Especiales y clima: max 1 copia. Ballista (unidad): max 3.
('Ballista',                 'NEUTRAL',         'UNIDAD',   'ASEDIO',           6, 'ENLACE_APRETADO',       false, 3, NULL, NOW(), NULL, NULL),
('Senuelo',                  'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'DECOY',                 false, 1, NULL, NOW(), NULL, NULL),
('Tormenta de escarcha',     'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, 1, NULL, NOW(), NULL, NULL),
('Lluvia acida',             'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, 1, NULL, NOW(), NULL, NULL),
('Niebla espesa',            'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, 1, NULL, NOW(), NULL, NULL),
('Toque de Axii',            'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'ESCUDO_IMPENETRABLE',   false, 1, NULL, NOW(), NULL, NULL),
('Buen clima',               'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'NINGUNA',               false, 1, NULL, NOW(), NULL, NULL),

-- ===== LIDERES (tipo=LIDER, fila=NULL, fuerza=NULL, es_heroe=true, max_copias=1) =====
-- REINO_DEL_NORTE
('Foltest: Rey de Temeria',               'REINO_DEL_NORTE', 'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
('Foltest: Senor Comandante del Norte',   'REINO_DEL_NORTE', 'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
-- NILFGAARD
('Emhyr: El Blanco Llama',               'NILFGAARD',       'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
('Emhyr: Senor de las Aguas',            'NILFGAARD',       'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
-- MONSTRUOS
('Eredin: Rey de la Caceria',            'MONSTRUOS',       'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
('Eredin: Rompenieblas',                 'MONSTRUOS',       'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
-- SCOIA_TAEL
('Francesca: La Mas Hermosa',            'SCOIA_TAEL',      'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
('Francesca: Enid an Gleanna',           'SCOIA_TAEL',      'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
-- SKELLIGE
('Crach an Craite: Senor de Ard Skellig','SKELLIGE',        'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL),
('Birna Bran',                           'SKELLIGE',        'LIDER', NULL, NULL, 'NINGUNA', true, 1, NULL, NOW(), NULL, NULL);
