INSERT INTO users (version, id, name, password_hash, email, phone, created_at, updated_at) VALUES (0, gen_random_uuid(), 'Admin User', '$2a$10$abc123', 'admin@test.com', '+123456789', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO users (version, id, name, password_hash, email, phone, created_at, updated_at) VALUES (0, gen_random_uuid(), 'Regular User', '$2a$10$def456', 'user@test.com', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role) VALUES ((SELECT id FROM users WHERE email = 'admin@test.com'), 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES ((SELECT id FROM users WHERE email = 'user@test.com'), 'USER');

INSERT INTO rooms (version, name, price_per_night, capacity, type, description, amenities, status)
VALUES (0, 'Standard Room', 99.99, 2, 'STANDARD', 'Comfortable standard room', 'WiFi,TV', 'AVAILABLE');

INSERT INTO rooms (version, name, price_per_night, capacity, type, description, amenities, status)
VALUES (0, 'Deluxe Suite', 199.99, 4, 'SUITE', 'Luxurious suite with view', 'WiFi,TV,Minibar,Jacuzzi', 'RESERVED');

INSERT INTO room_images (image_url, room_id) VALUES
('/images/standard1.jpg', (SELECT id FROM rooms WHERE name = 'Standard Room')),
('/images/suite1.jpg', (SELECT id FROM rooms WHERE name = 'Deluxe Suite'));


INSERT INTO bookings (id, room_id, user_id, start_date, end_date, total_price, booking_status)
SELECT
    gen_random_uuid(),
    r.id,
    u.id,
    '2024-01-15 14:00:00',
    '2024-01-20 11:00:00',
    499.95,
    'CONFIRMED'
FROM rooms r, users u
WHERE r.name = 'Standard Room' AND u.email = 'user@test.com';

INSERT INTO processed_events (id, created_at, event_type)
VALUES ('event-001', CURRENT_TIMESTAMP, 'BOOKING_CREATED');

INSERT INTO processed_events (id, created_at, event_type)
VALUES ('event-002', CURRENT_TIMESTAMP, 'PAYMENT_PROCESSED');