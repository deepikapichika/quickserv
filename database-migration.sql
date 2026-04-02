ALTER TABLE providers
    ADD COLUMN IF NOT EXISTS business_name VARCHAR(255) NULL AFTER user_id,
    ADD COLUMN IF NOT EXISTS services_offered VARCHAR(1500) NULL AFTER provider_locations,
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(1000) NULL AFTER services_offered,
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7) NULL AFTER profile_photo_url,
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7) NULL AFTER latitude;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(1000) NULL AFTER location,
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7) NULL AFTER profile_photo_url,
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7) NULL AFTER latitude;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS customer_address VARCHAR(500) NULL AFTER customer_notes,
    ADD COLUMN IF NOT EXISTS customer_latitude DECIMAL(10,7) NULL AFTER customer_address,
    ADD COLUMN IF NOT EXISTS customer_longitude DECIMAL(10,7) NULL AFTER customer_latitude,
    ADD COLUMN IF NOT EXISTS provider_address VARCHAR(500) NULL AFTER provider_notes,
    ADD COLUMN IF NOT EXISTS provider_latitude DECIMAL(10,7) NULL AFTER provider_address,
    ADD COLUMN IF NOT EXISTS provider_longitude DECIMAL(10,7) NULL AFTER provider_latitude;

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE
);
