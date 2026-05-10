-- ParkEase Database Initialization
-- Each microservice gets its own schema to honour per-service database ownership

CREATE DATABASE IF NOT EXISTS parkease_auth     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS parkease_parking  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS parkease_booking  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS parkease_payment  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS parkease_notif_analytics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to root (already owns all; this is a reminder for future service accounts)
GRANT ALL PRIVILEGES ON parkease_auth.*              TO 'root'@'%';
GRANT ALL PRIVILEGES ON parkease_parking.*           TO 'root'@'%';
GRANT ALL PRIVILEGES ON parkease_booking.*           TO 'root'@'%';
GRANT ALL PRIVILEGES ON parkease_payment.*           TO 'root'@'%';
GRANT ALL PRIVILEGES ON parkease_notif_analytics.*   TO 'root'@'%';
FLUSH PRIVILEGES;
