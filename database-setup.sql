-- QuickServ Database Setup
-- Run these commands in MySQL to set up the database

-- Create database (if not exists)
CREATE DATABASE IF NOT EXISTS quickserv;
USE quickserv;

-- Drop existing tables if they exist (to avoid conflicts)
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS providers;
DROP TABLE IF EXISTS services;
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
    price DECIMAL(10,2),
    price_unit VARCHAR(50) DEFAULT 'per hour',
    location VARCHAR(255),
    image_url VARCHAR(500),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

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
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Create bookings table
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    booking_date_time DATETIME NOT NULL,
    customer_notes TEXT,
    provider_notes TEXT,
    status ENUM('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED') DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (provider_id) REFERENCES users(id),
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
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
