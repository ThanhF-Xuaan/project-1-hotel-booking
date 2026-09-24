-- Baseline for fresh databases. Existing databases require verified manual baselining.

-- ==============================================================================
-- 1. CỤM QUẢN LÝ KHÁCH SẠN & NHÂN SỰ 
-- ==============================================================================

-- 1. Quản lý Vùng (Miền Bắc, Miền Nam, Đà Nẵng,...)
CREATE TABLE regions (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Quản lý Phòng ban Tiêu chuẩn (Front Office, Housekeeping, F&B, Maintenance...)
CREATE TABLE departments (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hotels (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- THÊM MỚI: Khách sạn này thuộc Vùng nào?
    region_id SMALLINT NOT NULL REFERENCES regions(id),

    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(20),

    check_in_time TIME NOT NULL DEFAULT '14:00:00',
    check_out_time TIME NOT NULL DEFAULT '12:00:00',

    service_fee_percent NUMERIC(5,2) NOT NULL DEFAULT 0,

    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_fee_percent
    CHECK (
        service_fee_percent >= 0
        AND service_fee_percent <= 100
    )
);

CREATE TABLE roles (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- Vài chục role
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- Vài trăm quyền
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_permission_action_resource UNIQUE (action, resource)
);

CREATE TABLE role_permissions (
    role_id SMALLINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id SMALLINT REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE staffs (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    keycloak_id UUID UNIQUE NOT NULL, 

    role_id SMALLINT REFERENCES roles(id),

    -- THIẾT KẾ CHUẨN DATA SCOPE
    scope_type VARCHAR(20) NOT NULL, -- 'CHAIN', 'REGION', 'PROPERTY'
    
    -- Nếu scope_type = 'PROPERTY', nó lưu hotel_id.
    -- Nếu scope_type = 'REGION', nó lưu region_id.
    -- Nếu scope_type = 'CHAIN', nó bị ép bằng NULL.
    scope_entity_id INT,             
    
    -- Phòng ban (Chỉ bắt buộc hoặc có ý nghĩa khi nhân viên thuộc cấp PROPERTY)
    department_id SMALLINT REFERENCES departments(id),          

    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(250) NOT NULL,

    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_staff_scope
    CHECK (
        scope_type IN ('CHAIN', 'REGION', 'PROPERTY')
    ),
    
    -- Ràng buộc chốt chặn tính logic của Scope Entity
    CONSTRAINT chk_scope_logic
    CHECK (
        (scope_type = 'CHAIN' AND scope_entity_id IS NULL) OR
        (scope_type IN ('REGION', 'PROPERTY') AND scope_entity_id IS NOT NULL)
    ),

    -- Ràng buộc chốt chặn phòng ban: Cấp CHAIN và REGION không thuộc phòng ban cơ sở nào cả
    CONSTRAINT chk_department_logic
    CHECK (
        (scope_type IN ('CHAIN', 'REGION') AND department_id IS NULL) OR
        (scope_type = 'PROPERTY') -- Cấp Property có thể có hoặc không có department_id tùy vị trí
    )
);



-- ==============================================================================
-- 2. CỤM QUẢN LÝ LOẠI PHÒNG & KHO PHÒNG
-- ==============================================================================
-- ==============================================================================
-- TAX CATEGORY
-- Quản lý các nhóm hàng hóa/dịch vụ chịu thuế
-- ==============================================================================

CREATE TABLE tax_categories (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    category_code VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(150) NOT NULL,

    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tax_category_status
    CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE'
        )
    )
);

-- ==============================================================================
-- VAT RULES
-- Quản lý chính sách VAT theo nhóm hàng hóa/dịch vụ
-- ==============================================================================

CREATE TABLE vat_rules (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    tax_category_id INT NOT NULL,

    vat_code VARCHAR(50) NOT NULL UNIQUE,
    vat_name VARCHAR(150) NOT NULL,

    vat_percent NUMERIC(5,2) NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vat_rules_tax_category
    FOREIGN KEY (tax_category_id)
    REFERENCES tax_categories(id),

    CONSTRAINT chk_vat_percent
    CHECK (
        vat_percent >= 0
        AND vat_percent <= 100
    ),

    CONSTRAINT chk_vat_date_range
    CHECK (
        end_date IS NULL
        OR start_date <= end_date
    ),

    CONSTRAINT chk_vat_status
    CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE'
        )
    )
);


CREATE TABLE hotel_age_policies (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_id SMALLINT NOT NULL
        REFERENCES hotels(id)
        ON DELETE CASCADE,

    guest_type VARCHAR(20) NOT NULL,
    
    min_age SMALLINT NOT NULL,
    max_age SMALLINT NOT NULL,

    -- [BỔ SUNG]: Đồng bộ cột created_at với toàn hệ thống
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_hotel_guest_type
    UNIQUE (hotel_id, guest_type),

    CONSTRAINT chk_policy_guest_type
    CHECK (
        guest_type IN ('ADULT', 'CHILD', 'INFANT')
    ),

    CONSTRAINT chk_policy_age_range
    CHECK (min_age <= max_age)
);

CREATE TABLE menus (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id INT REFERENCES hotels(id),
    tax_category_id INT REFERENCES tax_categories(id),
    
    -- Cột phân loại (Discriminator Column) để Code Backend biết nó là gì
    menu_type VARCHAR(50) NOT NULL, -- Giá trị: 'PRODUCT' hoặc 'SERVICE'
    
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(15,2) NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE catalog_items (
    id INT PRIMARY KEY REFERENCES menus(id), -- Tham chiếu 1-1 về menus
    
    -- Thuộc tính riêng của hàng hóa (Có quản lý số lượng)
    stock_quantity INT DEFAULT 0
);


CREATE TABLE services (
    id INT PRIMARY KEY REFERENCES menus(id), -- Tham chiếu 1-1 về menus
    
    -- Thuộc tính riêng của dịch vụ (Có tần suất tính tiền)
    pricing_type VARCHAR(50) NOT NULL -- Giá trị: 'PER_STAY', 'PER_NIGHT', 'PER_PERSON'
);


CREATE TABLE room_types (
   id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- Danh mục loại phòng dùng chung toàn hệ thống

   code VARCHAR(50) UNIQUE NOT NULL,
   name VARCHAR(150) NOT NULL,

   status VARCHAR(50) DEFAULT 'ACTIVE',
   is_deleted BOOLEAN DEFAULT FALSE,

   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE hotel_room_types (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_id SMALLINT NOT NULL
        REFERENCES hotels(id),

    room_type_id SMALLINT NOT NULL
        REFERENCES room_types(id),

    tax_category_id INT NOT NULL
        REFERENCES tax_categories(id),

    -- Sức chứa tiêu chuẩn (Làm mốc tính base_price)
    standard_adults SMALLINT NOT NULL DEFAULT 2,
    standard_children SMALLINT NOT NULL DEFAULT 0,

    -- Sức chứa tối đa của từng đối tượng (Validation)
    max_adults SMALLINT NOT NULL DEFAULT 2,
    max_children SMALLINT NOT NULL DEFAULT 1,
    max_infants SMALLINT NOT NULL DEFAULT 1,

    -- Giới hạn phòng & Giường phụ
    max_total_guests SMALLINT NOT NULL DEFAULT 3,
    -- CẤU HÌNH GIƯỜNG (MỚI & CŨ)
    max_beds SMALLINT NOT NULL DEFAULT 1,      -- Trần vật lý (Physical Limit)
    extra_beds SMALLINT NOT NULL DEFAULT 0, -- Hạn mức giường phụ (Sales Limit) base_quantity + extra_beds <= max_beds

    base_price NUMERIC(15,2) NOT NULL,

    total_quantity INT NOT NULL DEFAULT 0,

    status VARCHAR(50) DEFAULT 'ACTIVE',

    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_hotel_room_type
    UNIQUE (
        hotel_id,
        room_type_id
    ),

    CONSTRAINT chk_total_quantity
    CHECK (
        total_quantity >= 0
    ),

    -- ==========================================
    -- RÀNG BUỘC LOGIC CHO CÁC TRƯỜNG SỨC CHỨA
    -- ==========================================
    CONSTRAINT chk_adults_capacity
    CHECK (
        max_adults >= standard_adults
    ),

    CONSTRAINT chk_children_capacity
    CHECK (
        max_children >= standard_children
    ),

    CONSTRAINT chk_total_guests_capacity
    CHECK (
        max_total_guests >= max_adults
    )
);


CREATE TABLE room_instances (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_id SMALLINT NOT NULL
        REFERENCES hotels(id),

    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id),

    room_number VARCHAR(20) NOT NULL,

    current_status VARCHAR(50)
        NOT NULL DEFAULT 'READY',

    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_room_number
    UNIQUE (
        hotel_id,
        room_number
    ),

    CONSTRAINT chk_room_instance_status
    CHECK (
        current_status IN (
            'READY',
            'OCCUPIED',
            'CLEANING',
            'MAINTENANCE'
        )
    )
);


CREATE TABLE room_maintenance_blocks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    room_instance_id INT NOT NULL REFERENCES room_instances(id),
    
    -- Phân loại: OOO (Trừ quỹ phòng - Không bán được) | OOS (Sửa lặt vặt - Vẫn có thể bán nếu ép)
    block_type VARCHAR(20) NOT NULL DEFAULT 'OOO',

    -- Ngày bắt đầu và ngày kết thúc sửa chữa (Date range)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- Lý do bảo trì (VD: Sơn lại tường, Hỏng đường ống nước)
    reason TEXT NOT NULL,

    -- Trạng thái của lệnh bảo trì
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Nếu có Ticket sửa chữa từ phòng Kỹ thuật thì link vào đây
    maintenance_ticket_id BIGINT,

    -- (Optional) Lưu ID của staff thao tác để dễ tra cứu sau này
    created_by_staff_id INT REFERENCES staffs(id),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_block_type CHECK (block_type IN ('OOO', 'OOS')),
    CONSTRAINT chk_block_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_block_dates CHECK (start_date <= end_date)
);

CREATE TABLE room_availability (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id),

    date DATE NOT NULL,

    total_rooms INT NOT NULL,

    booked_rooms INT NOT NULL DEFAULT 0,

    locked_rooms INT NOT NULL DEFAULT 0,

    -- DÀNH CHO VẬN HÀNH: Phòng đang hỏng/sửa chữa (Out of Order)
    ooo_rooms INT NOT NULL DEFAULT 0,

    available_count INT GENERATED ALWAYS AS (
        total_rooms
        - booked_rooms
        - locked_rooms
        - ooo_rooms
    ) STORED,

    version BIGINT NOT NULL DEFAULT 0,

    locked_until TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_room_availability
    UNIQUE (
        hotel_room_type_id,
        date
    ),

    CONSTRAINT chk_total_rooms
    CHECK (
        total_rooms >= 0
    ),

    CONSTRAINT chk_booked_rooms
    CHECK (
        booked_rooms >= 0
    ),

    CONSTRAINT chk_locked_rooms
    CHECK (
        locked_rooms >= 0
    ),

    CONSTRAINT chk_inventory_consistency
    CHECK (
        booked_rooms
        + locked_rooms
        <= total_rooms
    )
);



CREATE TABLE room_beds (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    size VARCHAR(50),

    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_room_bed_name
        UNIQUE (name)
);


CREATE TABLE hotel_room_type_beds (
    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id),

    room_bed_id SMALLINT NOT NULL
        REFERENCES room_beds(id),

    base_quantity SMALLINT NOT NULL DEFAULT 1,

    PRIMARY KEY (
        hotel_room_type_id,
        room_bed_id
    )
);





CREATE TABLE room_features (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    
    -- Giữ nullable hoặc thêm NOT NULL tùy bạn, nhưng phải tuân thủ bộ lọc CHECK bên dưới
    category VARCHAR(50), 

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Ràng buộc trạng thái hoạt động
    CONSTRAINT chk_room_feature_status
    CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE'
        )
    ),

    -- THÀNH PHẦN BỔ SUNG: Bức tường bảo vệ danh mục khớp hoàn toàn với Java Enum
    CONSTRAINT chk_room_feature_category
    CHECK (
        category IN (
            'VIEW',
            'BATHROOM',
            'BEDROOM',
            'MEDIA',
            'ENTERTAINMENT',
            'AMENITY',
            'COMFORT',
            'INTERNET',
            'OTHER'
        )
    )
);



CREATE TABLE hotel_room_type_features (

    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id)
        ON DELETE CASCADE,

    room_feature_id SMALLINT NOT NULL
        REFERENCES room_features(id),

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (
        hotel_room_type_id,
        room_feature_id
    )
);

CREATE TABLE hotel_room_type_inclusions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_room_type_id INT REFERENCES hotel_room_types(id),
    
    -- Sử dụng Đa hình để liên kết đến bảng Services hoặc Pos_Products
    reference_type VARCHAR(50) NOT NULL, -- Giá trị: 'SERVICE' (VD: Massage), 'POS_PRODUCT' (VD: Rượu vang)
    reference_id INT NOT NULL, -- catalog/service
    
    quantity INT DEFAULT 1, -- Tặng mấy chai? Mấy vé massage?
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
); 


-- ==============================================================================
-- 3. CỤM CẤU HÌNH GIÁ
-- ==============================================================================

CREATE TABLE pricing_rule_types (
    code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE discount_rule_types (
    code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE holiday_calendars (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_holiday_name_date UNIQUE (name, date)
);

CREATE TABLE pricing_rules (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    hotel_room_type_id INT NOT NULL 
        REFERENCES hotel_room_types(id),
        
    holiday_calendar_id INT NULL 
        REFERENCES holiday_calendars(id),
    
    -- TRỎ KHÓA NGOẠI ĐẾN BẢNG CONFIG, BỎ CHECK CONSTRAINT
    rule_type VARCHAR(50) NOT NULL 
        REFERENCES pricing_rule_types(code),
    
    adjustment_type VARCHAR(20) NOT NULL,
    adjustment_value NUMERIC(15,2) NOT NULL,
    
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_pricing_rule_date 
        CHECK (start_date <= end_date),
        
    CONSTRAINT chk_pricing_adjustment_type 
        CHECK (adjustment_type IN ('PERCENT', 'FIXED'))
);

CREATE TABLE campaigns (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    hotel_id SMALLINT NOT NULL 
        REFERENCES hotels(id), -- Chiến dịch thuộc về 1 khách sạn cụ thể
        
    name VARCHAR(150) NOT NULL,
    
    description TEXT,
    
    start_date DATE NOT NULL,
    
    end_date DATE NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    
    is_deleted BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- DB Level Validation: Ngày bắt đầu không được lớn hơn ngày kết thúc
    CONSTRAINT chk_campaign_dates 
    CHECK (start_date <= end_date)
);

CREATE TABLE discount_rules (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    hotel_room_type_id INT NOT NULL 
        REFERENCES hotel_room_types(id),
        
    campaign_id INT 
        REFERENCES campaigns(id),
    
    -- TRỎ KHÓA NGOẠI ĐẾN BẢNG CONFIG, BỎ CHECK CONSTRAINT
    rule_type VARCHAR(50) NOT NULL 
        REFERENCES discount_rule_types(code),
    
    -- Xóa bỏ min_nights. Thay bằng cột điều kiện động JSONB
    conditions JSONB NOT NULL DEFAULT '{}'::jsonb,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(15,2) NOT NULL,
    
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_discount_type 
        CHECK (discount_type IN ('PERCENT', 'FIXED')),
        
    CONSTRAINT chk_discount_value 
        CHECK (discount_value > 0),
        
    CONSTRAINT chk_discount_rule_dates 
        CHECK (start_date <= end_date)
);  



CREATE TABLE surcharge_rules (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    
    -- Cột này đang bị thiếu trong file của bạn
    age_policy_id SMALLINT REFERENCES hotel_age_policies(id), 
    
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('EXTRA_PERSON', 'EXTRA_BED', 'EARLY_CHECKIN', 'LATE_CHECKOUT')),
    pricing_type VARCHAR(20) NOT NULL DEFAULT 'PER_NIGHT', --PER_STAY
    conditions JSONB DEFAULT '{}'::jsonb,
    adjustment_type VARCHAR(20) NOT NULL CHECK (adjustment_type IN ('PERCENT', 'FIXED')),
    adjustment_value NUMERIC(15,2) NOT NULL CHECK (adjustment_value >= 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_surcharge_dates CHECK (start_date <= end_date)
);

-- ==============================================================================
-- 4. CỤM GIAO DỊCH ĐẶT PHÒNG (Giao dịch dùng BIGINT)
-- ==============================================================================

-- Thuộc Schema: crm (Customer Relationship Management)

-- ==============================================================================
-- 4. CỤM GIAO DỊCH ĐẶT PHÒNG (Giao dịch dùng BIGINT)
-- ==============================================================================

-- Thuộc Schema: crm (Customer Relationship Management)
CREATE TABLE guests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL, 
    birth_date DATE,
    identity_type VARCHAR(20),
    identity_number VARCHAR(50),
    nationality VARCHAR(100),
    email VARCHAR(150),
    phone VARCHAR(20) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_identity_type CHECK (
        identity_type IS NULL OR identity_type IN ('CCCD', 'PASSPORT', 'DRIVER_LICENSE', 'OTHER')
    )
);

CREATE TABLE companies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_code VARCHAR(50) UNIQUE,
    address TEXT,
    contact_name VARCHAR(100),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id SMALLINT NOT NULL REFERENCES hotels(id),
    guest_id BIGINT NOT NULL REFERENCES guests(id),
    company_id BIGINT REFERENCES companies(id),
    
    booking_type VARCHAR(20) NOT NULL DEFAULT 'FIT', -- 'FIT', 'GIT', 'CORPORATE'
    booking_number VARCHAR(50) UNIQUE NOT NULL,

    subtotal_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',

    issued_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_status CHECK (
        status IN ('CONFIRMED', 'CANCELLED', 'NO_SHOW')
    ),
    CONSTRAINT chk_booking_service_fee_rate CHECK ( 
        service_fee_rate >= 0 AND service_fee_rate <= 100
    ),
    CONSTRAINT chk_booking_owner CHECK (
        (booking_type = 'FIT' AND company_id IS NULL) OR
        (booking_type IN ('GIT', 'CORPORATE') AND company_id IS NOT NULL)
    )
);

CREATE TABLE booking_details (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    hotel_room_type_id INT NOT NULL REFERENCES hotel_room_types(id),
    room_type_name VARCHAR(150) NOT NULL,

    quantity SMALLINT NOT NULL DEFAULT 1,

    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    selection_deadline TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_date CHECK (check_in_date < check_out_date),
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);

CREATE TABLE booking_rooms (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_detail_id BIGINT NOT NULL REFERENCES booking_details(id) ON DELETE CASCADE,
    room_instance_id INT NULL REFERENCES room_instances(id),

    adult_count SMALLINT NOT NULL DEFAULT 1,
    child_count SMALLINT NOT NULL DEFAULT 0,
    infant_count SMALLINT NOT NULL DEFAULT 0,
    guest_count SMALLINT NOT NULL DEFAULT 1,

    status VARCHAR(50) NOT NULL DEFAULT 'EXPECTED',
    
    actual_check_in_at TIMESTAMP WITH TIME ZONE,
    actual_check_out_at TIMESTAMP WITH TIME ZONE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        
    CONSTRAINT uk_booking_room_instance UNIQUE NULLS NOT DISTINCT (booking_detail_id, room_instance_id),

    CONSTRAINT chk_booking_room_status CHECK (
        status IN ('EXPECTED', 'CHECKED_IN', 'CHECKED_OUT', 'NO_SHOW', 'CANCELLED')
    ),

    CONSTRAINT chk_actual_stay CHECK (
        actual_check_out_at IS NULL OR actual_check_in_at IS NULL OR actual_check_in_at <= actual_check_out_at
    ),

    CONSTRAINT chk_room_guest_count CHECK (guest_count > 0)
);

CREATE TABLE booking_guests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_room_id BIGINT NOT NULL REFERENCES booking_rooms(id) ON DELETE CASCADE,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(250) NOT NULL,

    birth_date DATE,
    guest_type VARCHAR(20) NOT NULL,
    identity_type VARCHAR(20),
    identity_number VARCHAR(50),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_guest_type CHECK (guest_type IN ('ADULT', 'CHILD', 'INFANT')),
    CONSTRAINT chk_booking_guest_identity_type CHECK (
        identity_type IS NULL OR identity_type IN ('CCCD', 'PASSPORT', 'DRIVER_LICENSE')
    ),
    CONSTRAINT chk_adult_requires_identity CHECK (
        guest_type != 'ADULT' OR (identity_type IS NOT NULL AND identity_number IS NOT NULL AND identity_number <> '')
    )
);

CREATE TABLE booking_daily_rates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_room_id BIGINT REFERENCES booking_rooms(id),
    
    stay_date DATE NOT NULL,
    
    base_price DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) DEFAULT 0,
    surcharge_amount DECIMAL(15,2) DEFAULT 0,
    
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    
    tax_category_id INT REFERENCES tax_categories(id),
    vat_percent DECIMAL(5,2) NOT NULL,
    vat_amount DECIMAL(15,2) NOT NULL,
    
    net_price DECIMAL(15,2) NOT NULL,
    
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_charges (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_room_id BIGINT NOT NULL REFERENCES booking_rooms(id) ON DELETE CASCADE,
    booking_guest_id BIGINT NULL REFERENCES booking_guests(id),

    charge_type VARCHAR(50) NOT NULL,
    item_name VARCHAR(150),
    description TEXT,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_rate NUMERIC(5,2) NOT NULL,
    vat_amount NUMERIC(15,2) NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,

    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_charge_type CHECK (
        charge_type IN ('EARLY_CHECKIN', 'LATE_CHECKOUT', 'PENALTY', 'OTHER')
    ),
    CONSTRAINT chk_booking_charge_qty CHECK (quantity > 0),
    CONSTRAINT chk_booking_charge_vat CHECK (vat_rate BETWEEN 0 AND 100)
);

CREATE TABLE room_slots (
   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   room_instance_id INT NOT NULL REFERENCES room_instances(id),

   maintenance_block_id BIGINT NULL REFERENCES room_maintenance_blocks(id) ON DELETE SET NULL,
   
   slot_date DATE NOT NULL,
   booking_room_id BIGINT NULL REFERENCES booking_rooms(id) ON DELETE SET NULL,

   status VARCHAR(50) NOT NULL DEFAULT 'READY',

   locked_at TIMESTAMP WITH TIME ZONE,
   reserved_at TIMESTAMP WITH TIME ZONE,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT uk_room_slot UNIQUE (room_instance_id, slot_date),
   CONSTRAINT chk_room_slot_status CHECK (
       status IN ('READY', 'BLOCKED', 'RESERVED', 'OCCUPIED', 'DIRTY', 'CLEANING', 'MAINTENANCE')
   ),
   CONSTRAINT chk_room_slot_logical CHECK (slot_date >= DATE '2000-01-01')
);

-- ==============================================================================
-- 5. CỤM THANH TOÁN & HÓA ĐƠN
-- ==============================================================================

CREATE TABLE payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),

    total_amount NUMERIC(15,2) NOT NULL,
    payment_purpose VARCHAR(50) NOT NULL DEFAULT 'FULL_PAYMENT',
    payment_method VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50),
    transaction_reference VARCHAR(100),

    status VARCHAR(50) DEFAULT 'PENDING',
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_method CHECK (
        payment_method IN ('CASH', 'BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'VNPAY', 'MOMO', 'ZALOPAY', 'OTHER')
    ),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'CANCELLED')
    ),
    CONSTRAINT chk_payment_purpose CHECK (
        payment_purpose IN ('DEPOSIT', 'FULL_PAYMENT', 'INCIDENTAL_DEPOSIT', 'REFUND')
    )
);

CREATE TABLE transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    booking_id BIGINT NOT NULL REFERENCES bookings(id),

    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    reference_code VARCHAR(100),

    status VARCHAR(50) DEFAULT 'COMPLETED',
    issued_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('PAYMENT', 'REFUND'))
);

CREATE TABLE invoices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,

    sub_total NUMERIC(15,2) NOT NULL,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(15,2) NOT NULL,

    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_invoice_service_fee_rate CHECK (service_fee_rate >= 0 AND service_fee_rate <= 100)
);

CREATE TABLE invoice_details (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    reference_id BIGINT NOT NULL,
    line_type VARCHAR(50) NOT NULL,

    description TEXT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_rate NUMERIC(5,2) NOT NULL,
    vat_amount NUMERIC(15,2) NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_invoice_line_type CHECK (
        line_type IN ('ROOM_RATE', 'PRODUCT', 'SERVICE', 'SURCHARGE', 'PENALTY')
    ),
    CONSTRAINT chk_invoice_qty CHECK (quantity > 0),
    CONSTRAINT chk_invoice_vat CHECK (vat_rate BETWEEN 0 AND 100)
);

-- ==============================================================================
-- 6. CỤM DỊCH VỤ PHÁT SINH
-- ==============================================================================

CREATE TABLE service_orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL, 
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    room_instance_id INT NOT NULL REFERENCES room_instances(id),

    sub_total NUMERIC(15,2) NOT NULL,
    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(15,2) NOT NULL,

    status VARCHAR(50) DEFAULT 'PENDING',
    issued_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_order_fee CHECK (service_fee_rate BETWEEN 0 AND 100)
);

CREATE TABLE service_order_details (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_order_id BIGINT NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    menu_id INT NOT NULL REFERENCES menus(id), 
    item_type VARCHAR(55) NOT NULL, 

    item_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL,
    subtotal NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_rate NUMERIC(5,2) NOT NULL,
    vat_amount NUMERIC(15,2) NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_item_type CHECK (item_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT chk_service_detail_qty CHECK (quantity > 0),
    CONSTRAINT chk_service_detail_vat CHECK (vat_rate BETWEEN 0 AND 100)
);


CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- Ai là người thực hiện? (Có thể NULL nếu là Job hệ thống tự chạy như Night Audit)
    staff_id INT REFERENCES staffs(id) ON DELETE SET NULL, 
    
    -- Hành động gì? (VD: CREATE, UPDATE, DELETE, LOGIN, APPROVE, REFUND)
    action_type VARCHAR(50) NOT NULL, 
    
    -- Tác động lên Bảng/Thực thể nào? (VD: 'bookings', 'staffs', 'pricing_rules')
    entity_name VARCHAR(100) NOT NULL, 
    
    -- ID của dòng dữ liệu bị tác động (Dùng VARCHAR để cover được cả UUID nếu sau này cần)
    entity_id VARCHAR(100) NOT NULL, 
    
    -- Dữ liệu trước khi sửa (Lưu dưới dạng JSON, NULL nếu là CREATE)
    old_values JSONB, 
    
    -- Dữ liệu sau khi sửa (Lưu dưới dạng JSON, NULL nếu là DELETE)
    new_values JSONB, 
    
    -- Truy vết IP của thiết bị thực hiện thao tác
    ip_address VARCHAR(50),
    
    -- Tùy chọn thêm: User Agent (Trình duyệt/App) để trace rủi ro lộ thiết bị
    user_agent TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 7. FUNCTIONS, TRIGGERS & ADVANCED CONSTRAINTS (BTREE_GIST)
-- ==============================================================================

-- Bật extension (Bắt buộc phải có để dùng EXCLUDE với các cột INT, VARCHAR)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ------------------------------------------------------------------------------
-- 7.1. TỰ ĐỘNG CẬP NHẬT CỘT `updated_at` CHO TẤT CẢ CÁC BẢNG
-- ------------------------------------------------------------------------------
-- PostgreSQL mặc định chỉ set CURRENT_TIMESTAMP lúc INSERT. 
-- Để cột updated_at tự nhảy giờ khi có lệnh UPDATE, ta cần Trigger.

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Viết một khối DO ẩn danh (Anonymous DO block) để tự động quét toàn bộ database
-- và gắn Trigger này vào BẤT KỲ bảng nào có chứa cột 'updated_at'.
DO $$
DECLARE
    t record;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema = 'public'
    LOOP
        EXECUTE format('
            CREATE OR REPLACE TRIGGER set_updated_at
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
        ', t.table_name);
    END LOOP;
END;
$$ LANGUAGE plpgsql;


-- ------------------------------------------------------------------------------
-- 7.2. TỰ ĐỘNG SINH MÃ ĐẶT PHÒNG (BOOKING NUMBER) ĐẸP MẮT
-- ------------------------------------------------------------------------------
-- Ví dụ: BKG-20260620-0001
CREATE SEQUENCE IF NOT EXISTS booking_number_seq START 1;

CREATE OR REPLACE FUNCTION generate_booking_number()
RETURNS TRIGGER AS $$
BEGIN
    -- Nếu Backend truyền xuống mã rỗng, DB sẽ tự động sinh mã
    IF NEW.booking_number IS NULL OR NEW.booking_number = '' THEN
        NEW.booking_number := 'BKG-' 
                           || to_char(CURRENT_DATE, 'YYYYMMDD') 
                           || '-' 
                           || LPAD(nextval('booking_number_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_booking_number
BEFORE INSERT ON bookings
FOR EACH ROW
EXECUTE FUNCTION generate_booking_number();


-- ------------------------------------------------------------------------------
-- 7.3. TỰ ĐỘNG SINH MÃ HÓA ĐƠN DỊCH VỤ (SERVICE ORDER NUMBER)
-- ------------------------------------------------------------------------------
-- Ví dụ: SRV-20260620-0001
CREATE SEQUENCE IF NOT EXISTS service_order_seq START 1;

CREATE OR REPLACE FUNCTION generate_service_order_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.order_number IS NULL OR NEW.order_number = '' THEN
        NEW.order_number := 'SRV-' 
                         || to_char(CURRENT_DATE, 'YYYYMMDD') 
                         || '-' 
                         || LPAD(nextval('service_order_seq')::text, 4, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_generate_service_order_number
BEFORE INSERT ON service_orders
FOR EACH ROW
EXECUTE FUNCTION generate_service_order_number();


-- ------------------------------------------------------------------------------
-- 7.4. TRIGGER BẢO VỆ DỮ LIỆU CỐT LÕI (CHỐNG XÓA CỨNG - SOFT DELETE)
-- ------------------------------------------------------------------------------
-- Thay vì xóa hẳn (DELETE) một khách sạn hay loại phòng gây lỗi khóa ngoại, 
-- Trigger này sẽ chặn lệnh DELETE và chuyển nó thành UPDATE is_deleted = TRUE (Soft Delete).

CREATE OR REPLACE FUNCTION prevent_hard_delete_and_soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- [ĐÃ SỬA]: Với các bảng có cột status, chuyển về INACTIVE. Nếu bảng không có cột status thì sẽ lỗi.
    -- Giải pháp an toàn nhất là CHỈ update `is_deleted = TRUE`. Logic 'INACTIVE' nên để Backend xử lý hoặc chia ra trigger riêng.
    
    EXECUTE format('UPDATE %I SET is_deleted = TRUE WHERE id = $1', TG_TABLE_NAME)
    USING OLD.id;
    
    -- Trả về NULL để hủy bỏ lệnh DELETE cứng ban đầu
    RETURN NULL; 
END;
$$ LANGUAGE plpgsql;

-- Áp dụng cho bảng Khách sạn (hotels)
CREATE OR REPLACE TRIGGER soft_delete_hotels
BEFORE DELETE ON hotels
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();

-- Áp dụng cho bảng Loại phòng hệ thống (room_types)
CREATE OR REPLACE TRIGGER soft_delete_room_types
BEFORE DELETE ON room_types
FOR EACH ROW
EXECUTE FUNCTION prevent_hard_delete_and_soft_delete();


-- ------------------------------------------------------------------------------
-- 7.5. EXCLUDE CONSTRAINTS (CHỐNG CHỒNG LẤP THỜI GIAN BẰNG BTREE_GIST)
-- ------------------------------------------------------------------------------

-- Quy tắc 1: Bảng `pricing_rules` (Luật giá)
-- Trong cùng 1 khoảng thời gian, 1 Loại phòng (hotel_room_type_id) 
-- không được phép có 2 luật giá cùng Cấp độ (rule_type) đè lên nhau.
ALTER TABLE pricing_rules
ADD CONSTRAINT ex_pricing_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 2: Bảng `campaigns` (Chiến dịch khuyến mãi)
-- Trong cùng 1 Khách sạn (hotel_id), không được phép có 2 Campaign trùng tên đang diễn ra cùng lúc.
ALTER TABLE campaigns
ADD CONSTRAINT ex_campaign_name_overlap
EXCLUDE USING gist (
    hotel_id WITH =,
    name WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 3: Bảng `surcharge_rules` (Phụ phí)
-- Phụ phí CHỈ chồng lấp nếu Khác hạng phòng HOẶC Khác độ tuổi (age_policy_id) HOẶC Khác Loại phụ phí (rule_type).
-- Ví dụ: Phụ phí Thêm người lớn KHÁC Phụ phí Thêm trẻ em. 
ALTER TABLE surcharge_rules
ADD CONSTRAINT ex_surcharge_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    -- Cần cast ID về dạng INT bình thường (bỏ qua nếu age_policy_id có thể NULL)
    COALESCE(age_policy_id, 0) WITH =, 
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);

-- Quy tắc 4: Bảng `discount_rules` (Luật giảm giá)
-- Cùng 1 loại phòng, cùng 1 rule_type, không được trùng thời gian.
ALTER TABLE discount_rules
ADD CONSTRAINT ex_discount_rule_overlap
EXCLUDE USING gist (
    hotel_room_type_id WITH =,
    rule_type WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (is_deleted = FALSE);


-- Một phòng không thể có 2 phiếu bảo trì (OOO/OOS) đè lên nhau cùng lúc
ALTER TABLE room_maintenance_blocks
ADD CONSTRAINT ex_room_maintenance_overlap
EXCLUDE USING gist (
    room_instance_id WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (status = 'ACTIVE');

-- ==============================================================================
-- 8. ĐÁNH INDEX (TỐI ƯU HIỆU NĂNG TRUY VẤN)
-- ==============================================================================

-- A. ĐÁNH INDEX CHO BẢNG MENUS & CATALOG (Tối ưu tìm kiếm dịch vụ)
CREATE INDEX idx_menus_hotel_id ON menus(hotel_id);
CREATE INDEX idx_menus_tax_category_id ON menus(tax_category_id);
CREATE INDEX idx_menus_type ON menus(menu_type); 

-- B. ĐÁNH INDEX CHO CỤM IAM & TỔ CHỨC (Tối ưu hóa quá trình Login & Phân quyền)
CREATE INDEX idx_staffs_keycloak_id ON staffs(keycloak_id); -- BẮT BUỘC: Vì Keycloak đăng nhập dùng UUID này
CREATE INDEX idx_staffs_scope ON staffs(scope_type, scope_entity_id); -- BẮT BUỘC: Để Backend quét Data Scope nhanh

-- C. ĐÁNH INDEX CHO KHO PHÒNG & GIÁ (Chống thắt cổ chai khi Khách hàng Search phòng online)
CREATE INDEX idx_hotel_room_types_hotel_id ON hotel_room_types(hotel_id);
CREATE INDEX idx_room_instances_hotel_room_type ON room_instances(hotel_id, hotel_room_type_id);
CREATE INDEX idx_room_availability_date ON room_availability(date);
CREATE INDEX idx_room_availability_locked_until ON room_availability(locked_until);
CREATE INDEX idx_room_slots_date ON room_slots(slot_date);

-- D. ĐÁNH INDEX CHO LUỒNG ĐẶT PHÒNG (BOOKINGS & ROOM ASSIGNMENT)
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_company_id ON bookings(company_id); -- [MỚI] Tối ưu tìm kiếm cho Khách đoàn/Corporate
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_booking_details_booking_id ON booking_details(booking_id);
CREATE INDEX idx_booking_details_dates ON booking_details(check_in_date, check_out_date);

CREATE INDEX idx_booking_rooms_detail_id ON booking_rooms(booking_detail_id);
CREATE INDEX idx_booking_rooms_status ON booking_rooms(status); -- Tối ưu cho việc Lễ tân lọc phòng "EXPECTED", "CHECKED_IN"

-- E. ĐÁNH INDEX CHO LUỒNG TÀI CHÍNH & NIGHT AUDIT (Chống giật lag lúc Checkout)
CREATE INDEX idx_payments_booking_id ON payments(booking_id);

-- [ĐÃ SỬA]: Chuyển thành booking_room_id cho khớp với thiết kế Folio ở v7
CREATE INDEX idx_booking_daily_rates_room_id ON booking_daily_rates(booking_room_id);
CREATE INDEX idx_booking_daily_rates_status ON booking_daily_rates(status); -- Tối ưu cho Job Night Audit quét trạng thái 'PENDING'

CREATE INDEX idx_booking_charges_room_id ON booking_charges(booking_room_id);

CREATE INDEX idx_service_orders_booking_id ON service_orders(booking_id);
CREATE INDEX idx_service_order_details_menu_id ON service_order_details(menu_id);

-- F. ĐÁNH INDEX CHO HÓA ĐƠN & BÁO CÁO BI (Business Intelligence)
CREATE INDEX idx_invoices_booking_id ON invoices(booking_id);
CREATE INDEX idx_invoice_details_invoice_id ON invoice_details(invoice_id);
CREATE INDEX idx_invoice_details_line_type ON invoice_details(line_type); -- Gom báo cáo siêu tốc theo loại doanh thu (BI/Dashboard)

-- G. ĐÁNH INDEX CHO LUẬT CẤU HÌNH (RULES)
CREATE INDEX idx_surcharge_rules_age_policy_id ON surcharge_rules(age_policy_id);
CREATE INDEX idx_vat_rules_tax_category_id ON vat_rules(tax_category_id);

-- ĐÁNH INDEX CHO BẢNG AUDIT 

-- Tối ưu cho query: "Tìm lịch sử thay đổi của Đơn đặt phòng ID = 12345"
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);

-- Tối ưu cho query: "Nhân viên Nguyễn Văn A đã làm những gì trong tháng này?"
CREATE INDEX idx_audit_logs_staff ON audit_logs(staff_id);

-- Tối ưu cho query lọc theo thời gian (Bảo mật)
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
