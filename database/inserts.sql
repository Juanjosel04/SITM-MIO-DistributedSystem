INSERT INTO zones(name, description)
VALUES
('North Zone', 'Northern operation zone'),
('South Zone', 'Southern operation zone'),
('East Zone', 'Eastern operation zone'),
('West Zone', 'Western operation zone');

INSERT INTO routes(short_name, description, status)
VALUES
('T31', 'Terminal Route 31', 'ACTIVE'),
('P10B', 'Express Route P10B', 'ACTIVE'),
('T40', 'Terminal Route 40', 'ACTIVE');

INSERT INTO administrators(username, password, status)
VALUES
('admin', 'admin123', 'ACTIVE');

INSERT INTO controller_users(username, password, assigned_zone, status)
VALUES
('controller_north', '1234', 1, 'ACTIVE'),
('controller_south', '1234', 2, 'ACTIVE');

INSERT INTO buses(code, plate, status, route_id, zone_id)
VALUES
('BUS-001', 'ABC123', 'ACTIVE', 1, 1),
('BUS-002', 'DEF456', 'ACTIVE', 2, 2);