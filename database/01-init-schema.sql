-- ==============================================================================
-- 1. CỤM QUẢN LÝ KHÁCH SẠN & NHÂN SỰ 
-- ==============================================================================

CREATE TABLE hotels (
    id SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

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
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id SMALLINT REFERENCES roles(id) ON DELETE CASCADE,
    permission_id SMALLINT REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE staffs (
    -- Khóa chính nội bộ (Dùng để JOIN các bảng cho cực nhanh, đánh Index nhẹ)
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- [QUAN TRỌNG NHẤT] Mã liên kết trực tiếp với Keycloak User ID (trường 'sub' trong JWT)
    -- Vừa làm cầu nối xác thực, vừa đóng vai trò là 'public_id' an toàn để giao tiếp ra ngoài
    keycloak_id UUID UNIQUE NOT NULL, 

    -- Thông tin phân quyền nghiệp vụ (Business Authorization)
    hotel_id SMALLINT REFERENCES hotels(id),
    role_id SMALLINT REFERENCES roles(id),

    -- Định danh nghiệp vụ (Nên dùng email thay vì username chung chung)
    email VARCHAR(255) UNIQUE NOT NULL,

    -- Dữ liệu hiển thị (Profile data)
    -- Dù Keycloak có lưu tên, ta VẪN NÊN lưu bản sao ở đây để phục vụ Query/Filter 
    -- (VD: Tìm nhân viên tên 'A' của khách sạn 'B' trực tiếp bằng SQL thay vì phải gọi API Keycloak)
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(250) NOT NULL,

    -- Trạng thái nghiệp vụ (Keycloak có trạng thái Enable/Disable riêng cho việc Login, 
    -- còn đây là trạng thái làm việc tại khách sạn)
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE room_availability (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id),

    date DATE NOT NULL,

    total_rooms INT NOT NULL,

    booked_rooms INT NOT NULL DEFAULT 0,

    locked_rooms INT NOT NULL DEFAULT 0,

    available_count INT GENERATED ALWAYS AS (
        total_rooms
        - booked_rooms
        - locked_rooms
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
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
    
    hotel_id INT NOT NULL 
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

CREATE TABLE guests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- Trả ra Frontend / App để tra cứu hồ sơ khách hàng
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

    CONSTRAINT chk_identity_type
    CHECK (
        identity_type IS NULL
        OR identity_type IN (
            'CCCD',
            'PASSPORT',
            'DRIVER_LICENSE',
            'OTHER'
        )
    )
);




CREATE TABLE bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    hotel_id SMALLINT NOT NULL
        REFERENCES hotels(id),

    guest_id BIGINT NOT NULL
        REFERENCES guests(id),

    booking_number VARCHAR(50) UNIQUE NOT NULL,

    subtotal_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    total_vat_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    status VARCHAR(50) NOT NULL
        DEFAULT 'CONFIRMED',

    issued_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_status
    CHECK (
        status IN (
            'CONFIRMED',
            'CHECKED_IN',
            'CHECKED_OUT',
            'CANCELLED',
            'NO_SHOW'
        )
    ),

    CONSTRAINT chk_service_fee_rate
    CHECK (
        service_fee_rate >= 0
        AND service_fee_rate <= 100
    )
);



CREATE TABLE booking_details (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    booking_id BIGINT NOT NULL
        REFERENCES bookings(id)
        ON DELETE CASCADE,

    hotel_room_type_id INT NOT NULL
        REFERENCES hotel_room_types(id),

    room_type_name VARCHAR(150) NOT NULL,

    quantity SMALLINT NOT NULL DEFAULT 1,

    adult_count SMALLINT NOT NULL DEFAULT 1,
    child_count SMALLINT NOT NULL DEFAULT 0,
    infant_count SMALLINT NOT NULL DEFAULT 0,

    guest_count SMALLINT NOT NULL DEFAULT 1,

    -- Thời gian lưu trú dự kiến
    check_in_date DATE NOT NULL,

    check_out_date DATE NOT NULL,

    selection_deadline TIMESTAMP WITH TIME ZONE,

    -- Thời gian thực tế
    actual_check_in_at TIMESTAMP WITH TIME ZONE,

    actual_check_out_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_date
    CHECK (
        check_in_date < check_out_date
    ),

    CONSTRAINT chk_quantity
    CHECK (
        quantity > 0
    ),

    CONSTRAINT chk_guest_count
    CHECK (
        guest_count > 0
    ),

    CONSTRAINT chk_actual_stay
    CHECK (
        actual_check_out_at IS NULL
        OR actual_check_in_at IS NULL
        OR actual_check_in_at <= actual_check_out_at
    )
);



CREATE TABLE booking_rooms (
    booking_detail_id BIGINT NOT NULL
        REFERENCES booking_details(id)
        ON DELETE CASCADE,

    room_instance_id INT NOT NULL
        REFERENCES room_instances(id),

    assigned_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (
        booking_detail_id,
        room_instance_id
    )
);




CREATE TABLE booking_guests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    booking_detail_id BIGINT NOT NULL
        REFERENCES booking_details(id)
        ON DELETE CASCADE,

    guest_id BIGINT NULL
        REFERENCES guests(id),

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(250) NOT NULL,

    birth_date DATE,

    guest_type VARCHAR(20) NOT NULL,

    -- Đã bỏ NOT NULL, chuyển CHECK constraint xuống dưới cho chuẩn format
    identity_type VARCHAR(20),

    identity_number VARCHAR(50),

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_guest_type
    CHECK (
        guest_type IN (
            'ADULT',
            'CHILD',
            'INFANT'
        )
    ), -- ĐÃ THÊM DẤU PHẨY

    -- Kiểm tra giá trị của loại giấy tờ (chỉ áp dụng nếu có nhập)
    CONSTRAINT chk_booking_guest_identity_type
    CHECK (
        identity_type IS NULL 
        OR identity_type IN ('CCCD', 'PASSPORT', 'DRIVER_LICENSE', 'OTHER')
    ), -- ĐÃ THÊM DẤU PHẨY

    -- Chốt chặn: Đã là Người lớn thì BẮT BUỘC phải có giấy tờ
    CONSTRAINT chk_adult_requires_identity
    CHECK (
        guest_type != 'ADULT' 
        OR (identity_type IS NOT NULL AND identity_number IS NOT NULL AND identity_number <> '')
    )
);

CREATE TABLE booking_daily_rates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_detail_id BIGINT REFERENCES booking_details(id),
    
    -- Ngày lưu trú cụ thể (Ví dụ: 2026-12-30)
    stay_date DATE NOT NULL,
    
    -- Dữ liệu tài chính BỊ ĐÓNG BĂNG cho đêm hôm đó
    base_price DECIMAL(15,2) NOT NULL, -- Giá gốc lấy từ pricing_rules
    discount_amount DECIMAL(15,2) DEFAULT 0, -- Tiền giảm giá lấy từ discount_rules
    surcharge_amount DECIMAL(15,2) DEFAULT 0, -- Phụ thu (thêm người/giường)
    
    service_fee_rate NUMERIC(5,2)
        NOT NULL DEFAULT 0,

    service_fee_amount NUMERIC(15,2)
        NOT NULL DEFAULT 0,
    
    -- Dữ liệu thuế
    tax_category_id INT REFERENCES tax_categories(id),
    vat_percent DECIMAL(5,2) NOT NULL, -- Ví dụ: 8.00 hoặc 10.00
    vat_amount DECIMAL(15,2) NOT NULL, -- Tiền thuế tính ra
    
    net_price DECIMAL(15,2) NOT NULL, -- Tổng tiền cuối cùng khách phải trả cho đêm này
    
    -- Trạng thái hạch toán của Kiểm toán đêm (Night Audit)
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING (Chưa ở), POSTED (Đã ở và chốt sổ)
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE booking_charges (

    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    booking_detail_id BIGINT NOT NULL
        REFERENCES booking_details(id)
        ON DELETE CASCADE,

    booking_guest_id BIGINT NULL
        REFERENCES booking_guests(id),

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

    issued_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_booking_charge_type
    CHECK (
        charge_type IN (
            'EARLY_CHECKIN',
            'LATE_CHECKOUT',
            'OTHER'
        )
    ),

    CONSTRAINT chk_booking_charge_qty
    CHECK (
        quantity > 0
    ),

    CONSTRAINT chk_booking_charge_vat
    CHECK (
        vat_rate BETWEEN 0 AND 100
    )
);








CREATE TABLE room_slots (
   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

   room_instance_id INT NOT NULL
       REFERENCES room_instances(id),

   slot_date DATE NOT NULL,

   booking_detail_id BIGINT NULL
       REFERENCES booking_details(id),

   status VARCHAR(50) NOT NULL DEFAULT 'READY',

   -- metadata giúp debug + reconcile
   locked_at TIMESTAMP WITH TIME ZONE,
   reserved_at TIMESTAMP WITH TIME ZONE,

   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT uk_room_slot
       UNIQUE (room_instance_id, slot_date),

   CONSTRAINT chk_room_slot_status
       CHECK (
           status IN (
               'READY',        -- available
               'BLOCKED',      -- temporarily held (search/checkout)
               'RESERVED',     -- payment success but not assigned final room
               'OCCUPIED',     -- checked-in
               'CLEANING',     -- housekeeping
               'MAINTENANCE'   -- out of service
           )
       ),

   CONSTRAINT chk_room_slot_logical
       CHECK (
           slot_date >= DATE '2000-01-01'
       )
);



-- ==============================================================================
-- 5. CỤM THANH TOÁN & HÓA ĐƠN
-- ==============================================================================

CREATE TABLE payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    booking_id BIGINT NOT NULL
        REFERENCES bookings(id),

    total_amount NUMERIC(15,2) NOT NULL,

    payment_method VARCHAR(50) NOT NULL,

    payment_provider VARCHAR(50),

    transaction_reference VARCHAR(100),

    status VARCHAR(50)
        DEFAULT 'PENDING',

    paid_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_method
    CHECK (
        payment_method IN (
            'CASH',
            'BANK_TRANSFER',
            'CREDIT_CARD',
            'DEBIT_CARD',
            'VNPAY',
            'MOMO',
            'ZALOPAY',
            'OTHER'
        )
    ),

    CONSTRAINT chk_payment_status
    CHECK (
        status IN (
            'PENDING',
            'SUCCESS',
            'FAILED',
            'REFUNDED',
            'CANCELLED'
        )
    )
);


CREATE TABLE transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    payment_id BIGINT NOT NULL
        REFERENCES payments(id),

    booking_id BIGINT NOT NULL
        REFERENCES bookings(id),

    transaction_type VARCHAR(50) NOT NULL,

    amount NUMERIC(15,2) NOT NULL,

    reference_code VARCHAR(100),

    status VARCHAR(50)
        DEFAULT 'COMPLETED',

    issued_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_transaction_type
    CHECK (
        transaction_type IN (
            'PAYMENT',
            'REFUND'
        )
    )
);


CREATE TABLE invoices (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    booking_id BIGINT NOT NULL
        REFERENCES bookings(id),

    invoice_number VARCHAR(50) UNIQUE NOT NULL,

    sub_total NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2)
        NOT NULL DEFAULT 0,

    service_fee_amount NUMERIC(15,2)
        NOT NULL DEFAULT 0,

    vat_amount NUMERIC(15,2)
        NOT NULL DEFAULT 0,

    grand_total NUMERIC(15,2)
        NOT NULL,

    status VARCHAR(50)
        NOT NULL DEFAULT 'DRAFT',

    issued_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_fee_rate
    CHECK (
        service_fee_rate >= 0
        AND service_fee_rate <= 100
    )
);



CREATE TABLE invoice_details (

    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    invoice_id BIGINT NOT NULL
        REFERENCES invoices(id)
        ON DELETE CASCADE,

    reference_id BIGINT NOT NULL,

    line_type VARCHAR(50) NOT NULL,

    description TEXT NOT NULL,

    quantity INT NOT NULL DEFAULT 1,

    unit_price NUMERIC(15,2) NOT NULL,

    subtotal NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2)
        NOT NULL DEFAULT 0,

    service_fee_amount NUMERIC(15,2)
        NOT NULL DEFAULT 0,

    vat_rate NUMERIC(5,2) NOT NULL,

    vat_amount NUMERIC(15,2) NOT NULL,

    total_amount NUMERIC(15,2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_invoice_line_type
    CHECK (
        line_type IN (
            'ROOM_RATE',   -- Tiền phòng
            'PRODUCT',     -- Ăn uống/Minibar
            'SERVICE',     -- Spa/Tour
            'SURCHARGE'    -- Phạt/Phụ thu
        )
    ),

    CONSTRAINT chk_invoice_qty
    CHECK (
        quantity > 0
    ),

    CONSTRAINT chk_invoice_vat
    CHECK (
        vat_rate BETWEEN 0 AND 100
    )
);





-- ==============================================================================
-- 6. CỤM DỊCH VỤ PHÁT SINH
-- ==============================================================================


CREATE TABLE service_orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    
    -- Mã hóa đơn dịch vụ công khai (Ví dụ: ORD-100293)
    order_number VARCHAR(50) UNIQUE NOT NULL, 

    booking_id BIGINT NOT NULL
        REFERENCES bookings(id),

    room_instance_id INT NOT NULL
        REFERENCES room_instances(id),

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

    CONSTRAINT chk_service_order_fee
    CHECK (
        service_fee_rate BETWEEN 0 AND 100
    )
);






CREATE TABLE service_order_details (

    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    service_order_id BIGINT NOT NULL
        REFERENCES service_orders(id)
        ON DELETE CASCADE,

    menu_id INT NOT NULL REFERENCES menus(id), -- Sửa tên menus_id thành menu_id

    item_type VARCHAR(55) NOT NULL, -- Đổi tên từ order_type thành item_type

    item_name VARCHAR(150) NOT NULL,

    quantity INT NOT NULL DEFAULT 1,

    unit_price NUMERIC(15,2) NOT NULL,

    subtotal NUMERIC(15,2) NOT NULL,

    service_fee_rate NUMERIC(5,2) NOT NULL DEFAULT 0,

    service_fee_amount NUMERIC(15,2) NOT NULL DEFAULT 0,

    vat_rate NUMERIC(5,2) NOT NULL,

    vat_amount NUMERIC(15,2) NOT NULL,

    total_amount NUMERIC(15,2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_service_item_type
    CHECK (
        item_type IN ('PRODUCT', 'SERVICE')
    ),

    CONSTRAINT chk_service_detail_qty
    CHECK (
        quantity > 0
    ),

    CONSTRAINT chk_service_detail_vat
    CHECK (
        vat_rate BETWEEN 0 AND 100
    )
);