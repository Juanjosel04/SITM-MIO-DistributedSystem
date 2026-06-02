INSERT INTO zones(name, description)
SELECT seed.name, seed.description
FROM (VALUES
    ('North Zone', 'Northern operation zone'),
    ('South Zone', 'Southern operation zone'),
    ('East Zone', 'Eastern operation zone'),
    ('West Zone', 'Western operation zone')
) AS seed(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM zones existing_zone WHERE existing_zone.name = seed.name
);

INSERT INTO routes(short_name, description, status)
SELECT seed.short_name, seed.description, seed.status
FROM (VALUES
    ('T31', 'Terminal Route 31', 'ACTIVE'),
    ('P10B', 'Express Route P10B', 'ACTIVE'),
    ('T40', 'Terminal Route 40', 'ACTIVE')
) AS seed(short_name, description, status)
WHERE NOT EXISTS (
    SELECT 1 FROM routes existing_route WHERE existing_route.short_name = seed.short_name
);

INSERT INTO administrators(username, password, status)
VALUES ('admin', 'admin123', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO controller_users(username, password, assigned_zone, status)
VALUES
    ('controller_north', '1234', 1, 'ACTIVE'),
    ('controller_south', '1234', 2, 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO buses(code, plate, status, route_id, zone_id)
VALUES
    ('BUS-001', 'ABC123', 'ACTIVE', 1, 1),
    ('BUS-002', 'DEF456', 'ACTIVE', 2, 2)
ON CONFLICT (code) DO NOTHING;


INSERT INTO system_users(national_id, full_name, password_hash, role, status)
VALUES
    ('1001234567', 'Administrador General', 'DEMO:admin123', 'ADMIN', 'ACTIVE'),
    ('3001234567', 'Conductor Demo 01', 'DEMO:driver123', 'DRIVER', 'ACTIVE')
ON CONFLICT (national_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO system_users(national_id, full_name, password_hash, role, status)
SELECT
    '20012345' || LPAD(controller_number::TEXT, 2, '0') AS national_id,
    'Controlador Operacional ' || LPAD(controller_number::TEXT, 2, '0') AS full_name,
    'DEMO:controller123' AS password_hash,
    'CONTROLLER' AS role,
    'ACTIVE' AS status
FROM generate_series(1, 40) AS controller_seed(controller_number)
ON CONFLICT (national_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;


INSERT INTO operational_zones(name, description, status)
SELECT
    'Zona ' || LPAD(zone_number::TEXT, 2, '0') AS name,
    'Conjunto logico de rutas asignadas al controlador operacional ' ||
        LPAD(zone_number::TEXT, 2, '0') || '.' AS description,
    'ACTIVE' AS status
FROM generate_series(1, 40) AS zone_seed(zone_number)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;


INSERT INTO zone_route_assignments(zone_id, route_id)
SELECT zone_data.id, route_data.route_id
FROM (
    VALUES
        ('Zona 01', 304),
        ('Zona 01', 1472),
        ('Zona 01', 2241),
        ('Zona 02', 2274),
        ('Zona 02', 131),
        ('Zona 02', 140),
        ('Zona 03', 2102),
        ('Zona 03', 2301),
        ('Zona 03', 2471),
        ('Zona 04', 2472),
        ('Zona 04', 322),
        ('Zona 04', 332),
        ('Zona 05', 333),
        ('Zona 05', 334),
        ('Zona 05', 373),
        ('Zona 06', 376),
        ('Zona 06', 421),
        ('Zona 06', 552),
        ('Zona 07', 303),
        ('Zona 07', 554),
        ('Zona 07', 3011),
        ('Zona 08', 3123),
        ('Zona 08', 3411),
        ('Zona 08', 551),
        ('Zona 09', 3142),
        ('Zona 09', 3173),
        ('Zona 09', 3781),
        ('Zona 10', 302),
        ('Zona 10', 3172),
        ('Zona 10', 318),
        ('Zona 11', 323),
        ('Zona 11', 347),
        ('Zona 11', 375),
        ('Zona 12', 557),
        ('Zona 12', 722),
        ('Zona 12', 355),
        ('Zona 13', 2521),
        ('Zona 13', 357),
        ('Zona 13', 352),
        ('Zona 14', 437),
        ('Zona 14', 553),
        ('Zona 14', 555),
        ('Zona 15', 2243),
        ('Zona 15', 3422),
        ('Zona 15', 3722),
        ('Zona 16', 431),
        ('Zona 16', 452),
        ('Zona 16', 283),
        ('Zona 17', 272),
        ('Zona 17', 2101),
        ('Zona 17', 2402),
        ('Zona 18', 3452),
        ('Zona 18', 651),
        ('Zona 18', 2842),
        ('Zona 19', 1571),
        ('Zona 19', 3132),
        ('Zona 19', 2211),
        ('Zona 20', 2212),
        ('Zona 20', 2242),
        ('Zona 20', 2273),
        ('Zona 21', 385),
        ('Zona 21', 3721),
        ('Zona 21', 306),
        ('Zona 22', 321),
        ('Zona 22', 3441),
        ('Zona 22', 3442),
        ('Zona 23', 370),
        ('Zona 23', 324),
        ('Zona 23', 305),
        ('Zona 24', 3131),
        ('Zona 24', 3133),
        ('Zona 24', 353),
        ('Zona 25', 377),
        ('Zona 25', 2401),
        ('Zona 25', 271),
        ('Zona 26', 2104),
        ('Zona 26', 336),
        ('Zona 26', 2121),
        ('Zona 27', 4272),
        ('Zona 27', 3124),
        ('Zona 27', 371),
        ('Zona 28', 2141),
        ('Zona 28', 3171),
        ('Zona 28', 282),
        ('Zona 29', 2801),
        ('Zona 29', 311),
        ('Zona 29', 2473),
        ('Zona 30', 441),
        ('Zona 30', 150),
        ('Zona 30', 3121),
        ('Zona 31', 3122),
        ('Zona 31', 3191),
        ('Zona 31', 3192),
        ('Zona 32', 142),
        ('Zona 32', 251),
        ('Zona 32', 3351),
        ('Zona 33', 2524),
        ('Zona 33', 3372),
        ('Zona 33', 3141),
        ('Zona 34', 3371),
        ('Zona 34', 427),
        ('Zona 34', 217),
        ('Zona 35', 3112),
        ('Zona 35', 3413),
        ('Zona 35', 2841),
        ('Zona 36', 2471),
        ('Zona 36', 2472),
        ('Zona 37', 2101),
        ('Zona 37', 2102),
        ('Zona 38', 2241),
        ('Zona 38', 2242),
        ('Zona 39', 2273),
        ('Zona 39', 2274),
        ('Zona 40', 304),
        ('Zona 40', 131)
) AS route_data(zone_name, route_id)
JOIN operational_zones zone_data ON zone_data.name = route_data.zone_name
ON CONFLICT (zone_id, route_id) DO NOTHING;

INSERT INTO controller_zone_assignments(controller_user_id, zone_id)
SELECT controller_user.id, zone_data.id
FROM generate_series(1, 40) AS assignment_seed(controller_number)
JOIN system_users controller_user
    ON controller_user.national_id = '20012345' || LPAD(controller_number::TEXT, 2, '0')
JOIN operational_zones zone_data
    ON zone_data.name = 'Zona ' || LPAD(controller_number::TEXT, 2, '0')
ON CONFLICT (controller_user_id, zone_id) DO NOTHING;
