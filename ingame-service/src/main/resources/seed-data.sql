-- ============================================================
-- Gwent Card Catalog - Seed Data
-- Ejecutar UNA sola vez en la base de datos gwent:
--   mysql -u root -p gwent < seed-data.sql
-- ============================================================

INSERT INTO gw_carta_catalogo (nombre, faccion, tipo, fila, fuerza, habilidad, es_heroe, imagen_url, created_at, modified_at, deleted_at) VALUES

-- ===== REINO DEL NORTE =====
('Geralt de Rivia',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO', 15, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Triss Merigold',           'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  7, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Yennefer de Vengerberg',   'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, 'MEDICO',                true,  NULL, NOW(), NULL, NULL),
('Ciri',                     'REINO_DEL_NORTE', 'UNIDAD',   'AGIL',            15, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Dandelion',                'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        2, 'COMANDANTE_INVOCACION', false, NULL, NOW(), NULL, NULL),
('Espía de Temeria',         'REINO_DEL_NORTE', 'UNIDAD',   'DISTANCIA',        7, 'ESPIA',                 false, NULL, NOW(), NULL, NULL),
('Francotirador de Temeria', 'REINO_DEL_NORTE', 'UNIDAD',   'ASEDIO',           6, 'ENLACE_APRETADO',       false, NULL, NOW(), NULL, NULL),
('Médico de campo',          'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  5, 'MEDICO',                false, NULL, NOW(), NULL, NULL),
('Guerrero de la guardia',   'REINO_DEL_NORTE', 'UNIDAD',   'CUERPO_A_CUERPO',  4, 'VÍNCULO_ESTRECHO',      false, NULL, NOW(), NULL, NULL),

-- ===== NILFGAARD =====
('Emhyr var Emreis',         'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  8, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Espía Imperial',           'NILFGAARD',       'UNIDAD',   'DISTANCIA',        0, 'ESPIA',                 false, NULL, NOW(), NULL, NULL),
('Ballestero imperial',      'NILFGAARD',       'UNIDAD',   'DISTANCIA',        6, 'ENLACE_APRETADO',       false, NULL, NOW(), NULL, NULL),
('Legionario nilfgaardiano', 'NILFGAARD',       'UNIDAD',   'CUERPO_A_CUERPO',  4, 'MUSTER',                false, NULL, NOW(), NULL, NULL),

-- ===== MONSTRUOS =====
('Draug',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Garra',                    'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MUSTER',                false, NULL, NOW(), NULL, NULL),
('Bruja de medianoche',      'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MUSTER',                false, NULL, NOW(), NULL, NULL),
('Imlerith',                 'MONSTRUOS',       'UNIDAD',   'CUERPO_A_CUERPO', 10, 'NINGUNA',               true,  NULL, NOW(), NULL, NULL),
('Bestia del pantano',       'MONSTRUOS',       'UNIDAD',   'ASEDIO',           4, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),

-- ===== SCOIA_TAEL =====
('Francotirador élfico',     'SCOIA_TAEL',      'UNIDAD',   'DISTANCIA',        5, 'ENLACE_APRETADO',       false, NULL, NOW(), NULL, NULL),
('Guerrero dryade',          'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),
('Aglaïs',                   'SCOIA_TAEL',      'UNIDAD',   'CUERPO_A_CUERPO',  6, 'MEDICO',                true,  NULL, NOW(), NULL, NULL),

-- ===== SKELLIGE =====
('Crach an Craite',          'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  9, 'REFUERZO_MORAL',        true,  NULL, NOW(), NULL, NULL),
('Berserker skellige',       'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  8, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),
('Músico berserker',         'SKELLIGE',        'UNIDAD',   'CUERPO_A_CUERPO',  6, 'VÍNCULO_ESTRECHO',      false, NULL, NOW(), NULL, NULL),

-- ===== NEUTRAL =====
('Ballista',                 'NEUTRAL',         'UNIDAD',   'ASEDIO',           6, 'ENLACE_APRETADO',       false, NULL, NOW(), NULL, NULL),
('Señuelo',                  'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'DECOY',                 false, NULL, NOW(), NULL, NULL),
('Tormenta de escarcha',     'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),
('Lluvia ácida',             'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),
('Niebla espesa',            'NEUTRAL',         'CLIMA',     NULL,            NULL, 'NINGUNA',               false, NULL, NOW(), NULL, NULL),
('Toque de Axii',            'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'ESCUDO_IMPENETRABLE',   false, NULL, NOW(), NULL, NULL),
('Buen clima',               'NEUTRAL',         'ESPECIAL',  NULL,            NULL, 'NINGUNA',               false, NULL, NOW(), NULL, NULL);
