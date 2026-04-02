-- QuickServ Database Setup
-- Run these commands in MySQL to set up the database

-- Create database (if not exists)
CREATE DATABASE IF NOT EXISTS quickserv;
USE quickserv;

-- Drop existing tables if they exist (to avoid conflicts)
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS provider_categories;
DROP TABLE IF EXISTS providers;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS subcategories;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role ENUM('CUSTOMER', 'PROVIDER', 'ADMIN') DEFAULT 'CUSTOMER',
    location VARCHAR(255),
    profile_photo_url VARCHAR(1000),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500)
);

-- Create services table
CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    provider_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    subcategory_id BIGINT NULL,
    price DECIMAL(10,2),
    price_unit VARCHAR(50) DEFAULT 'per hour',
    location VARCHAR(255),
    available_time VARCHAR(255),
    discount_percent DECIMAL(5,2),
    coupon_code VARCHAR(100),
    image_url VARCHAR(500),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE subcategories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    UNIQUE KEY uq_subcategory_name_per_category (name, category_id),
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

ALTER TABLE services
    ADD CONSTRAINT fk_services_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories(id);

-- Create providers table
CREATE TABLE providers (
    provider_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    category_id BIGINT NOT NULL,
    experience TEXT,
    service_charge DECIMAL(10,2),
    availability VARCHAR(255) DEFAULT 'Mon-Sun 9AM-6PM',
    rating DOUBLE DEFAULT 0.0,
    total_reviews INT DEFAULT 0,
    provider_locations VARCHAR(1000),
    services_offered VARCHAR(1500),
    profile_photo_url VARCHAR(1000),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE provider_categories (
    provider_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (provider_id, category_id),
    FOREIGN KEY (provider_id) REFERENCES providers(provider_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Create bookings table
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    booking_date_time DATETIME NOT NULL,
    customer_notes TEXT,
    customer_address VARCHAR(500),
    customer_latitude DECIMAL(10,7),
    customer_longitude DECIMAL(10,7),
    provider_notes TEXT,
    provider_address VARCHAR(500),
    provider_latitude DECIMAL(10,7),
    provider_longitude DECIMAL(10,7),
    status ENUM('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED') DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (provider_id) REFERENCES users(id),
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert sample categories
INSERT INTO categories (name, description, icon_url) VALUES
('Electrician', 'Electrical services and repairs', '⚡'),
('Plumber', 'Plumbing services and repairs', '🔧'),
('AC Repair', 'Air conditioning services', '❄️'),
('Beautician', 'Beauty and grooming services', '💄'),
('Tutor', 'Educational tutoring services', '📚');

-- Insert sample users (passwords are hashed)
-- Password for all sample users is 'password'
INSERT INTO users (name, email, password, phone, role, location) VALUES
('John Doe', 'john@example.com', '$2a$10$examplehashedpassword', '+91 9876543210', 'CUSTOMER', 'Mumbai'),
('Jane Smith', 'jane@example.com', '$2a$10$examplehashedpassword', '+91 9876543211', 'PROVIDER', 'Delhi'),
('Mike Johnson', 'mike@example.com', '$2a$10$examplehashedpassword', '+91 9876543212', 'PROVIDER', 'Mumbai'),
('Admin User', 'admin@example.com', '$2a$10$examplehashedpassword', '+91 9876543213', 'ADMIN', 'Mumbai');

-- Insert sample providers
INSERT INTO providers (user_id, category_id, experience, service_charge, availability) VALUES
(2, 1, '5+ years experience in residential and commercial electrical work', 500.00, 'Mon-Fri 9AM-6PM'),
(3, 2, 'Certified plumber with 8 years experience', 400.00, 'Mon-Sat 8AM-7PM');

-- Insert sample services
INSERT INTO services (title, description, provider_id, category_id, price, location) VALUES
('Electrical Wiring', 'Complete home electrical wiring and installation', 2, 1, 500.00, 'Mumbai'),
('Pipe Repair', 'Emergency pipe repair and maintenance', 3, 2, 400.00, 'Delhi');

-- Insert sample subcategories
INSERT INTO subcategories (name, category_id)
SELECT 'AC Installation', c.id FROM categories c WHERE c.name = 'AC Repair'
UNION ALL SELECT 'AC Repair', c.id FROM categories c WHERE c.name = 'AC Repair'
UNION ALL SELECT 'AC Gas Refill', c.id FROM categories c WHERE c.name = 'AC Repair'
UNION ALL SELECT 'AC Maintenance', c.id FROM categories c WHERE c.name = 'AC Repair'
UNION ALL SELECT 'Hair Cut', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Hair Spa', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Facial', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Manicure', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Pedicure', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Bridal Makeup', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Threading', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Waxing', c.id FROM categories c WHERE c.name = 'Beautician'
UNION ALL SELECT 'Furniture Repair', c.id FROM categories c WHERE c.name = 'Carpenter'
UNION ALL SELECT 'Door Installation', c.id FROM categories c WHERE c.name = 'Carpenter'
UNION ALL SELECT 'Window Repair', c.id FROM categories c WHERE c.name = 'Carpenter'
UNION ALL SELECT 'Custom Furniture', c.id FROM categories c WHERE c.name = 'Carpenter'
UNION ALL SELECT 'Cabinet Fixing', c.id FROM categories c WHERE c.name = 'Carpenter'
UNION ALL SELECT 'Home Deep Cleaning', c.id FROM categories c WHERE c.name = 'Cleaner'
UNION ALL SELECT 'Kitchen Cleaning', c.id FROM categories c WHERE c.name = 'Cleaner'
UNION ALL SELECT 'Bathroom Cleaning', c.id FROM categories c WHERE c.name = 'Cleaner'
UNION ALL SELECT 'Sofa Cleaning', c.id FROM categories c WHERE c.name = 'Cleaner'
UNION ALL SELECT 'Carpet Cleaning', c.id FROM categories c WHERE c.name = 'Cleaner'
UNION ALL SELECT 'Fan Installation', c.id FROM categories c WHERE c.name = 'Electrician'
UNION ALL SELECT 'Light Installation', c.id FROM categories c WHERE c.name = 'Electrician'
UNION ALL SELECT 'Switch Repair', c.id FROM categories c WHERE c.name = 'Electrician'
UNION ALL SELECT 'Wiring Repair', c.id FROM categories c WHERE c.name = 'Electrician'
UNION ALL SELECT 'Inverter Installation', c.id FROM categories c WHERE c.name = 'Electrician'
UNION ALL SELECT 'House Shifting', c.id FROM categories c WHERE c.name = 'Moving & Shifting'
UNION ALL SELECT 'Office Shifting', c.id FROM categories c WHERE c.name = 'Moving & Shifting'
UNION ALL SELECT 'Packing & Unpacking', c.id FROM categories c WHERE c.name = 'Moving & Shifting'
UNION ALL SELECT 'Loading & Unloading', c.id FROM categories c WHERE c.name = 'Moving & Shifting'
UNION ALL SELECT 'Interior Painting', c.id FROM categories c WHERE c.name = 'Painter'
UNION ALL SELECT 'Exterior Painting', c.id FROM categories c WHERE c.name = 'Painter'
UNION ALL SELECT 'Wall Texture Design', c.id FROM categories c WHERE c.name = 'Painter'
UNION ALL SELECT 'Wallpaper Installation', c.id FROM categories c WHERE c.name = 'Painter'
UNION ALL SELECT 'Waterproofing', c.id FROM categories c WHERE c.name = 'Painter'
UNION ALL SELECT 'Tap Repair', c.id FROM categories c WHERE c.name = 'Plumber'
UNION ALL SELECT 'Pipe Leakage Fix', c.id FROM categories c WHERE c.name = 'Plumber'
UNION ALL SELECT 'Toilet Repair', c.id FROM categories c WHERE c.name = 'Plumber'
UNION ALL SELECT 'Drain Cleaning', c.id FROM categories c WHERE c.name = 'Plumber'
UNION ALL SELECT 'Water Motor Repair', c.id FROM categories c WHERE c.name = 'Plumber';
