-- MySQL 타겟 데이터베이스 초기화 스크립트

-- 데이터베이스 생성 (이미 존재하므로 생략)
-- CREATE DATABASE IF NOT EXISTS targetdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE targetdb;

-- 사용자 테이블 생성 (영어 테이블명/컬럼명)
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    gender ENUM('MALE', 'FEMALE'),
    occupation VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL
);

-- 카테고리 테이블 생성
CREATE TABLE IF NOT EXISTS categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(50) NOT NULL,
    parent_category_id INT,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (parent_category_id) REFERENCES categories(category_id)
);

-- 상품 테이블 생성
CREATE TABLE IF NOT EXISTS products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    category_id INT,
    price DECIMAL(10,2),
    sales_status ENUM('ON_SALE', 'OUT_OF_STOCK', 'DISCONTINUED') DEFAULT 'ON_SALE',
    stock_quantity INT DEFAULT 0,
    product_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- 주문 테이블 생성
CREATE TABLE IF NOT EXISTS orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    order_status ENUM('ORDER_RECEIVED', 'PREPARING', 'SHIPPING', 'DELIVERED', 'CANCELLED') DEFAULT 'ORDER_RECEIVED',
    payment_method ENUM('CREDIT_CARD', 'BANK_TRANSFER', 'CASH') DEFAULT 'CREDIT_CARD',
    total_amount DECIMAL(12,2),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivery_address VARCHAR(200),
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 주문상세 테이블 생성
CREATE TABLE IF NOT EXISTS order_details (
    order_detail_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2),
    subtotal DECIMAL(12,2),
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 리뷰 테이블 생성
CREATE TABLE IF NOT EXISTS reviews (
    review_id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    review_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 공지사항 테이블 생성
CREATE TABLE IF NOT EXISTS notices (
    notice_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    author VARCHAR(50),
    view_count INT DEFAULT 0,
    is_important BOOLEAN DEFAULT FALSE,
    publish_start_date TIMESTAMP,
    publish_end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL
);

-- 쿠폰 테이블 생성
CREATE TABLE IF NOT EXISTS coupons (
    coupon_id INT PRIMARY KEY AUTO_INCREMENT,
    coupon_name VARCHAR(100) NOT NULL,
    coupon_code VARCHAR(50) UNIQUE,
    discount_type ENUM('FIXED_AMOUNT', 'PERCENTAGE') DEFAULT 'FIXED_AMOUNT',
    discount_value DECIMAL(10,2),
    minimum_order_amount DECIMAL(10,2),
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    migrated_at TIMESTAMP NULL
);

-- 배송 테이블 생성
CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    delivery_company VARCHAR(50),
    tracking_number VARCHAR(50),
    delivery_status ENUM('PREPARING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'FAILED') DEFAULT 'PREPARING',
    shipped_date TIMESTAMP NULL,
    delivered_date TIMESTAMP NULL,
    recipient_name VARCHAR(50),
    delivery_address VARCHAR(200),
    contact_number VARCHAR(20),
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 문의 테이블 생성
CREATE TABLE IF NOT EXISTS inquiries (
    inquiry_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    inquiry_type ENUM('PRODUCT', 'ORDER', 'DELIVERY', 'REFUND', 'OTHER') DEFAULT 'OTHER',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    answer_content TEXT,
    status ENUM('RECEIVED', 'IN_PROGRESS', 'ANSWERED', 'CLOSED') DEFAULT 'RECEIVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP NULL,
    migrated_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 인덱스 생성
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_migrated_at ON users(migrated_at);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sales_status ON products(sales_status);
CREATE INDEX idx_products_migrated_at ON products(migrated_at);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_migrated_at ON orders(migrated_at);

CREATE INDEX idx_order_details_order_id ON order_details(order_id);
CREATE INDEX idx_order_details_product_id ON order_details(product_id);
CREATE INDEX idx_order_details_migrated_at ON order_details(migrated_at);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_migrated_at ON reviews(migrated_at);

CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX idx_deliveries_delivery_status ON deliveries(delivery_status);
CREATE INDEX idx_deliveries_migrated_at ON deliveries(migrated_at);

CREATE INDEX idx_inquiries_user_id ON inquiries(user_id);
CREATE INDEX idx_inquiries_status ON inquiries(status);
CREATE INDEX idx_inquiries_migrated_at ON inquiries(migrated_at);

-- 전체 테이블에 대한 migrated_at 인덱스 (성능 최적화)
CREATE INDEX idx_categories_migrated_at ON categories(migrated_at);
CREATE INDEX idx_notices_migrated_at ON notices(migrated_at);
CREATE INDEX idx_coupons_migrated_at ON coupons(migrated_at);

-- 테스트용 기본 데이터 (필요시)
-- INSERT INTO users (name, email, gender, occupation) VALUES
-- ('Test User', 'test@example.com', 'MALE', 'TESTER');

-- 테이블 통계 정보 업데이트
ANALYZE TABLE users, categories, products, orders, order_details, reviews, notices, coupons, deliveries, inquiries;
