-- 1. Tạo bảng danh mục chuỗi master_items dùng chung cho toàn hệ thống
CREATE TABLE IF NOT EXISTS master_items (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    item_type VARCHAR(50) NOT NULL CHECK (item_type IN ('PRODUCT', 'SERVICE')),
    default_base_price DECIMAL(15,2) NOT NULL,
    tax_category_id INT REFERENCES tax_categories(id),
    pricing_type VARCHAR(50) CHECK (pricing_type IN ('PER_STAY', 'PER_NIGHT', 'PER_PERSON', 'PER_PERSON_PER_NIGHT', 'PER_UNIT', 'PER_USE')),
    unit VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_master_items_code ON master_items(code);
CREATE INDEX IF NOT EXISTS idx_master_items_type_status ON master_items(item_type, status, is_deleted);

DROP TRIGGER IF EXISTS trg_master_items_updated_at ON master_items;
CREATE TRIGGER trg_master_items_updated_at
BEFORE UPDATE ON master_items
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 2. Bổ sung các cột override và tồn kho cơ sở vào bảng menus
ALTER TABLE menus ADD COLUMN IF NOT EXISTS master_item_id INT REFERENCES master_items(id);
ALTER TABLE menus ADD COLUMN IF NOT EXISTS stock_quantity INT DEFAULT 0;
ALTER TABLE menus ADD COLUMN IF NOT EXISTS custom_name VARCHAR(255);

-- 3. Di chuyển dữ liệu danh mục hiện tại từ menus, services sang master_items (nếu chưa có)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'services') THEN
        INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, pricing_type, status)
        SELECT 
            'MASTER_' || m.id AS code,
            m.name,
            m.description,
            m.menu_type,
            m.base_price,
            m.tax_category_id,
            s.pricing_type,
            m.status
        FROM menus m
        LEFT JOIN services s ON s.id = m.id
        WHERE NOT EXISTS (SELECT 1 FROM master_items mi WHERE mi.code = 'MASTER_' || m.id);
    ELSE
        INSERT INTO master_items (code, name, description, item_type, default_base_price, tax_category_id, status)
        SELECT 
            'MASTER_' || m.id AS code,
            m.name,
            m.description,
            m.menu_type,
            m.base_price,
            m.tax_category_id,
            m.status
        FROM menus m
        WHERE NOT EXISTS (SELECT 1 FROM master_items mi WHERE mi.code = 'MASTER_' || m.id);
    END IF;
END $$;

-- 4. Liên kết menus với master_items
UPDATE menus m
SET master_item_id = mi.id
FROM master_items mi
WHERE mi.code = 'MASTER_' || m.id AND m.master_item_id IS NULL;

-- 5. Sao chép tồn kho từ catalog_items vào menus
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'catalog_items') THEN
        UPDATE menus m
        SET stock_quantity = ci.stock_quantity
        FROM catalog_items ci
        WHERE ci.id = m.id;
    END IF;
END $$;

-- 6. Xóa bỏ 2 bảng con 1-1 không cần thiết
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS catalog_items;

-- 7. Tạo Index cho master_item_id trên menus
CREATE INDEX IF NOT EXISTS idx_menus_master_item ON menus(master_item_id);
