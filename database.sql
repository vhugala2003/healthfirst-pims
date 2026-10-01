CREATE DATABASE IF NOT EXISTS healthfirst_pims
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE healthfirst_pims;

CREATE TABLE IF NOT EXISTS users (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('Admin', 'Cashier') NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS suppliers (
  supplier_id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  contact_person VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(100) NOT NULL,
  address TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medicines (
  medicine_id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  company VARCHAR(100) NOT NULL,
  medicine_type VARCHAR(50) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  quantity_in_stock INT NOT NULL DEFAULT 0,
  reorder_level INT NOT NULL DEFAULT 10,
  expiry_date DATE NOT NULL,
  supplier_id INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_medicine_price CHECK (price >= 0),
  CONSTRAINT chk_medicine_stock CHECK (quantity_in_stock >= 0),
  CONSTRAINT chk_medicine_reorder CHECK (reorder_level >= 0),
  CONSTRAINT fk_medicines_supplier FOREIGN KEY (supplier_id)
    REFERENCES suppliers(supplier_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS sales (
  sale_id INT PRIMARY KEY AUTO_INCREMENT,
  sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount DECIMAL(10, 2) NOT NULL,
  user_id INT NOT NULL,
  CONSTRAINT chk_sale_total CHECK (total_amount >= 0),
  CONSTRAINT fk_sales_user FOREIGN KEY (user_id)
    REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS sale_items (
  sale_item_id INT PRIMARY KEY AUTO_INCREMENT,
  sale_id INT NOT NULL,
  medicine_id INT NOT NULL,
  quantity_sold INT NOT NULL,
  price_at_sale DECIMAL(10, 2) NOT NULL,
  CONSTRAINT chk_sale_item_quantity CHECK (quantity_sold > 0),
  CONSTRAINT chk_sale_item_price CHECK (price_at_sale >= 0),
  CONSTRAINT uq_sale_medicine UNIQUE (sale_id, medicine_id),
  CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id)
    REFERENCES sales(sale_id) ON DELETE CASCADE,
  CONSTRAINT fk_sale_items_medicine FOREIGN KEY (medicine_id)
    REFERENCES medicines(medicine_id) ON DELETE RESTRICT
);

INSERT INTO users (username, password, role, full_name) VALUES
  ('admin', 'pbkdf2_sha256$120000$aGVhbHRoZmlyc3QtYWRtaW4tc2VlZA==$NlNMkN9sm7h9e0zMOGyoK22VWNrkf5jJkUsK59r2K5E=', 'Admin', 'HealthFirst Administrator'),
  ('cashier', 'pbkdf2_sha256$120000$aGVhbHRoZmlyc3QtY2FzaGllci1zZWVk$6yjFnuWgWatKF4wNrTsnxh/rFxfRLRLuFmr4l/28dL0=', 'Cashier', 'Demo Cashier')
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
  ('Cedar Medical Supply', 'Lerato Mokoena', '+27 11 555 0142', 'orders@cedarmed.example', '18 Foundry Road, Johannesburg'),
  ('Northstar Pharma', 'Daniel Jacobs', '+27 21 555 0188', 'sales@northstar.example', '4 Harbour Park, Cape Town'),
  ('WellSpring Distributors', 'Amara Ndlovu', '+27 31 555 0117', 'hello@wellspring.example', '72 Riverside Drive, Durban');

INSERT INTO medicines
  (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id)
SELECT samples.name, samples.company, samples.medicine_type, samples.price,
       samples.quantity_in_stock, samples.reorder_level, samples.expiry_date,
       suppliers.supplier_id
FROM (
  SELECT 'Paracetamol 500 mg' AS name, 'HealthCore' AS company, 'Tablet' AS medicine_type,
         32.50 AS price, 120 AS quantity_in_stock, 25 AS reorder_level,
         DATE_ADD(CURRENT_DATE, INTERVAL 18 MONTH) AS expiry_date, 'Cedar Medical Supply' AS supplier_name
  UNION ALL SELECT 'Amoxicillin 250 mg', 'MediNova', 'Capsule', 86.00, 42, 15,
         DATE_ADD(CURRENT_DATE, INTERVAL 11 MONTH), 'Northstar Pharma'
  UNION ALL SELECT 'Cetirizine 10 mg', 'ClearDay', 'Tablet', 48.75, 18, 20,
         DATE_ADD(CURRENT_DATE, INTERVAL 8 MONTH), 'Cedar Medical Supply'
  UNION ALL SELECT 'Cough Relief 100 ml', 'WellSpring', 'Syrup', 64.90, 9, 12,
         DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 'WellSpring Distributors'
  UNION ALL SELECT 'Hydrocortisone 1%', 'DermaCare', 'Cream', 55.00, 31, 10,
         DATE_ADD(CURRENT_DATE, INTERVAL 22 MONTH), 'Northstar Pharma'
  UNION ALL SELECT 'Vitamin C 500 mg', 'BrightLife', 'Tablet', 39.95, 76, 20,
         DATE_ADD(CURRENT_DATE, INTERVAL 16 MONTH), 'WellSpring Distributors'
) AS samples
JOIN suppliers ON suppliers.name = samples.supplier_name
WHERE NOT EXISTS (
  SELECT 1 FROM medicines existing WHERE existing.name = samples.name
);