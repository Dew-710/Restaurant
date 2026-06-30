-- ============================================================
--  Restaurant Management System - Dữ liệu Demo sát thực tế
--  Luồng: Đặt bàn -> Check-in -> Gọi món -> Lịch sử thanh toán & Ví điện tử
-- ============================================================

-- Clean old data
TRUNCATE TABLE 
    users, 
    categories, 
    menu_items, 
    tables, 
    bookings, 
    orders, 
    order_items, 
    payments, 
    payment_transactions, 
    payment_transaction_orders, 
    wallets, 
    wallet_transactions, 
    password_reset_tokens 
RESTART IDENTITY CASCADE;

-- 1. NGƯỜI DÙNG (Mật khẩu mặc định: 'password123', riêng admin là 'admin123')
INSERT INTO users (username, password_hash, full_name, phone, email, role, status, created_at, updated_at)
VALUES
    ('admin', '$2a$10$xDOtWL.3fJvvGdOeJUqUCOT5/XRDvjHQWoFMC0q15h6CPLH5sWAFC', 'System Admin', '0900000000', 'admin@restaurant.com', 'ADMIN', 'ACTIVE', NOW(), NOW()),
    ('staff_dung', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'NV Nguyễn Dũng', '0911111111', 'dung.staff@restaurant.com', 'STAFF', 'ACTIVE', NOW(), NOW()),
    ('staff_lan', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'NV Trần Lan', '0911111112', 'lan.staff@restaurant.com', 'STAFF', 'ACTIVE', NOW(), NOW()),
    ('staff_hoa', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'NV Lê Hoa', '0911111113', 'hoa.staff@restaurant.com', 'STAFF', 'ACTIVE', NOW(), NOW()),
    ('customer_le', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'Khách Lê Hoàng', '0922222222', 'lehoanhdung710@gmail.com', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
    ('customer_vy', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'Khách Mai Vy', '0922222223', 'vy.customer@gmail.com', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
    ('customer_minh', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'Khách Phạm Minh', '0922222224', 'minh.customer@gmail.com', 'CUSTOMER', 'ACTIVE', NOW(), NOW()),
    ('customer_nam', '$2a$10$8.VAg5iK6uK1I/3M1eYpP.9Z0r5vYn66t.Jz6Z.r6Y6r6Y6r6Y6r6', 'Khách Trần Nam', '0922222225', 'nam.customer@gmail.com', 'CUSTOMER', 'ACTIVE', NOW(), NOW());

-- 2. THỂ LOẠI (CATEGORIES)
INSERT INTO categories (name, description, image_url, display_order, is_active, created_at, updated_at)
VALUES
    ('Món Khai Vị', 'Bắt đầu bữa ăn nhẹ nhàng và kích thích vị giác', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c', 1, TRUE, NOW(), NOW()),
    ('Món Chính', 'Những món ăn tinh hoa và đậm đà bản sắc nhà hàng', 'https://images.unsplash.com/photo-1544025162-d76694265947', 2, TRUE, NOW(), NOW()),
    ('Đồ Uống', 'Nước giải khát, nước ép trái cây và cà phê thơm ngon', 'https://images.unsplash.com/photo-1497534446932-c925b458314e', 3, TRUE, NOW(), NOW()),
    ('Món Tráng Miệng', 'Chè ngọt thanh, bánh flan mịn màng và kem dừa mát lạnh', 'https://images.unsplash.com/photo-1563729784474-d77dbb933a9e', 4, TRUE, NOW(), NOW()),
    ('Lẩu & Súp', 'Lẩu và súp ấm nóng, bổ dưỡng phù hợp tụ họp nhóm', 'https://images.unsplash.com/photo-1547928576-a4a33237eceb', 5, TRUE, NOW(), NOW()),
    ('Món Nướng', 'Thịt nướng xèo xèo, thơm phức đậm đà sốt BBQ đặc trưng', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1', 6, TRUE, NOW(), NOW());

-- 3. MÓN ĂN (MENU ITEMS)
INSERT INTO menu_items (name, price, description, image_url, category_id, is_available, preparation_time, calories, allergens, created_at, updated_at)
VALUES
    -- Món Khai Vị (Category 1)
    ('Súp Bào Ngư', 120000, 'Súp nóng hổi, bào ngư thơm ngọt bồi bổ sức khỏe', 'https://images.unsplash.com/photo-1547928576-a4a33237eceb', 1, TRUE, 15, 180, ARRAY['Hải sản', 'Nấm']::text[], NOW(), NOW()),
    ('Gỏi Xoài Tôm Khô', 65000, 'Vị xoài chua ngọt hòa quyện cùng tôm khô đậm đà', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c', 1, TRUE, 10, 120, ARRAY['Tôm', 'Đậu phộng']::text[], NOW(), NOW()),
    ('Salad Hoàng Đế', 85000, 'Rau romaine tươi giòn, sốt Caesar đặc trưng, kèm bánh mỳ nướng và bacon', 'https://images.unsplash.com/photo-1550304943-4f24f54ddde9', 1, TRUE, 10, 150, ARRAY['Sữa', 'Trứng']::text[], NOW(), NOW()),
    ('Khoai Tây Chiên Bơ Tỏi', 45000, 'Khoai tây chiên giòn rụm lắc bơ tỏi thơm lừng', 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877', 1, TRUE, 8, 320, ARRAY['Bơ']::text[], NOW(), NOW()),

    -- Món Chính (Category 2)
    ('Bò Lúc Lắc', 155000, 'Thịt bò xào bản gang mềm ngọt kèm khoai tây chiên và hành tây', 'https://images.unsplash.com/photo-1544025162-d76694265947', 2, TRUE, 15, 450, ARRAY['Bò']::text[], NOW(), NOW()),
    ('Cơm Chiên Hải Sản', 95000, 'Cơm chiên tơi xốp giòn rụm kết hợp mực, tôm tươi và đậu hà lan', 'https://images.unsplash.com/photo-1600891964599-f61ba0e24092', 2, TRUE, 12, 500, ARRAY['Tôm', 'Mực', 'Trứng']::text[], NOW(), NOW()),
    ('Cá Hồi Áp Chảo Sốt Chanh Leo', 210000, 'Cá hồi Na Uy áp chảo da giòn kết hợp sốt chanh leo chua ngọt thơm mát', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288', 2, TRUE, 20, 400, ARRAY['Cá']::text[], NOW(), NOW()),
    ('Mỳ Ý Sốt Bò Bằm', 110000, 'Mỳ ống Ý luộc vừa tới quyện trong sốt cà chua thịt bò bằm đậm đà', 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8', 2, TRUE, 12, 420, ARRAY['Gluten']::text[], NOW(), NOW()),

    -- Đồ Uống (Category 3)
    ('Trà Đào Cam Sả', 35000, 'Trà đào thơm ngậy kết hợp cam vàng và sả thanh mát', 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd', 3, TRUE, 5, 95, NULL, NOW(), NOW()),
    ('Nước Suối', 10000, 'Nước tinh khiết đóng chai', 'https://images.unsplash.com/photo-1560023907-5f67b36f9852', 3, TRUE, 1, 0, NULL, NOW(), NOW()),
    ('Cà Phê Muối', 39000, 'Cà phê phin truyền thống hòa quyện lớp kem mặn béo ngậy', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93', 3, TRUE, 5, 150, ARRAY['Sữa']::text[], NOW(), NOW()),
    ('Nước Ép Cam Tươi', 45000, 'Nước cam vắt nguyên chất bổ dưỡng, giàu Vitamin C', 'https://images.unsplash.com/photo-1613478223719-2ab802602423', 3, TRUE, 5, 120, NULL, NOW(), NOW()),

    -- Món Tráng Miệng (Category 4)
    ('Chè Khúc Bạch', 35000, 'Khúc bạch mềm dẻo béo ngậy, nước đường phèn hạt chia và hạnh nhân lát giòn', 'https://images.unsplash.com/photo-1563729784474-d77dbb933a9e', 4, TRUE, 5, 180, ARRAY['Sữa', 'Hạnh nhân']::text[], NOW(), NOW()),
    ('Bánh Flan Caramen', 25000, 'Bánh flan làm từ trứng sữa mềm mịn phủ lớp caramen ngọt đắng', 'https://images.unsplash.com/photo-1528975604071-b4dc52a2d18c', 4, TRUE, 3, 160, ARRAY['Sữa', 'Trứng']::text[], NOW(), NOW()),
    ('Kem Dừa Côn Đảo', 49000, 'Kem dừa ngọt mát đựng trong quả dừa xiêm kèm dừa nạo và đậu phộng rang', 'https://images.unsplash.com/photo-1505394033-f3e0dd17c0d1', 4, TRUE, 5, 250, ARRAY['Dừa', 'Đậu phộng']::text[], NOW(), NOW()),

    -- Lẩu & Súp (Category 5)
    ('Lẩu Thái Hải Sản', 350000, 'Nước lẩu chua cay chuẩn vị Thái kèm mực, tôm, nghêu, chả cá và rau nấm tươi', 'https://images.unsplash.com/photo-1547928576-a4a33237eceb', 5, TRUE, 20, 950, ARRAY['Hải sản', 'Gluten']::text[], NOW(), NOW()),
    ('Lẩu Nấm Gà Ta', 320000, 'Nước lẩu hầm gà ta thanh ngọt kèm nửa con gà chặt và nấm tươi các loại', 'https://images.unsplash.com/photo-1607532941433-304659e8198a', 5, TRUE, 20, 850, ARRAY['Nấm']::text[], NOW(), NOW()),

    -- Món Nướng (Category 6)
    ('Sườn Nướng Tảng BBQ', 280000, 'Sườn heo tảng ướp sốt BBQ đậm đà nướng chín mềm, ăn kèm khoai tây chiên', 'https://images.unsplash.com/photo-1544025162-d76694265947', 6, TRUE, 25, 750, NULL, NOW(), NOW()),
    ('Ba Chỉ Bò Cuộn Nấm Kim Châm', 125000, 'Thịt ba chỉ bò Mỹ lát mỏng cuộn nấm kim châm ngọt giòn nướng thơm phức', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1', 6, TRUE, 15, 380, ARRAY['Bò', 'Nấm']::text[], NOW(), NOW());

-- 4. DANH SÁCH BÀN (TABLES)
INSERT INTO tables (table_name, capacity, status, table_type, location, qr_code, created_at, updated_at)
VALUES
    ('Bàn 01', 4, 'VACANT', 'INDOOR', 'Khu A', 'QR-01', NOW(), NOW()),
    ('Bàn 02', 2, 'OCCUPIED', 'WINDOW', 'Khu B', 'QR-02', NOW(), NOW()),
    ('Bàn 03', 6, 'RESERVED', 'VIP', 'Phòng 101', 'QR-03', NOW(), NOW()),
    ('Bàn 04', 2, 'VACANT', 'INDOOR', 'Khu A', 'QR-04', NOW(), NOW()),
    ('Bàn 05', 4, 'OCCUPIED', 'INDOOR', 'Khu B', 'QR-05', NOW(), NOW()),
    ('Bàn 06', 8, 'VACANT', 'VIP', 'Phòng 102', 'QR-06', NOW(), NOW()),
    ('Bàn 07', 4, 'OCCUPIED', 'OUTDOOR', 'Sân Vườn', 'QR-07', NOW(), NOW()),
    ('Bàn 08', 4, 'CLEANING', 'OUTDOOR', 'Sân Vườn', 'QR-08', NOW(), NOW()),
    ('Bàn 09', 2, 'VACANT', 'WINDOW', 'Khu B', 'QR-09', NOW(), NOW()),
    ('Bàn 10', 12, 'VACANT', 'VIP', 'Phòng Hội Nghị', 'QR-10', NOW(), NOW()),
    ('Bàn 11', 4, 'VACANT', 'INDOOR', 'Khu A', 'QR-11', NOW(), NOW()),
    ('Bàn 12', 6, 'OCCUPIED', 'WINDOW', 'Khu A', 'QR-12', NOW(), NOW());

-- 5. LỊCH SỬ ĐẶT BÀN (BOOKINGS)
INSERT INTO bookings (customer_id, table_id, booking_date, booking_time, guests, note, status, booking_code, created_at, updated_at)
VALUES
    -- BK001 (Đã hoàn thành cách đây 5 ngày)
    ((SELECT id FROM users WHERE username = 'customer_le'), (SELECT id FROM tables WHERE table_name = 'Bàn 01'), CURRENT_DATE - INTERVAL '5 days', '18:30:00', 4, 'Ăn tiệc gia đình, không ăn cay', 'COMPLETED', 'BK001', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    -- BK002 (Đã hoàn thành cách đây 3 ngày)
    ((SELECT id FROM users WHERE username = 'customer_vy'), (SELECT id FROM tables WHERE table_name = 'Bàn 05'), CURRENT_DATE - INTERVAL '3 days', '19:00:00', 4, 'Bàn gần cửa sổ kính thoáng mát', 'COMPLETED', 'BK002', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    -- BK003 (Đã hoàn thành cách đây 2 ngày)
    ((SELECT id FROM users WHERE username = 'customer_minh'), (SELECT id FROM tables WHERE table_name = 'Bàn 03'), CURRENT_DATE - INTERVAL '2 days', '12:00:00', 6, 'Họp đối tác bàn công việc, phòng riêng tư', 'COMPLETED', 'BK003', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    -- BK004 (Đang CONFIRMED tối nay)
    ((SELECT id FROM users WHERE username = 'customer_le'), (SELECT id FROM tables WHERE table_name = 'Bàn 03'), CURRENT_DATE, '19:00:00', 6, 'Tiệc mừng sinh nhật khách, vui lòng trang trí cơ bản', 'CONFIRMED', 'BK004', NOW(), NOW()),
    -- BK005 (Đang CONFIRMED tối nay)
    ((SELECT id FROM users WHERE username = 'customer_nam'), (SELECT id FROM tables WHERE table_name = 'Bàn 07'), CURRENT_DATE, '18:00:00', 4, 'Khu vực ngoài trời thoáng đãng sát hồ nước', 'CONFIRMED', 'BK005', NOW(), NOW()),
    -- BK006 (CONFIRMED ngày mai)
    ((SELECT id FROM users WHERE username = 'customer_vy'), (SELECT id FROM tables WHERE table_name = 'Bàn 06'), CURRENT_DATE + INTERVAL '1 day', '19:30:00', 8, 'Họp đại gia đình cuối tuần ấm cúng', 'CONFIRMED', 'BK006', NOW(), NOW()),
    -- BK007 (Đang PENDING 2 ngày nữa)
    ((SELECT id FROM users WHERE username = 'customer_minh'), (SELECT id FROM tables WHERE table_name = 'Bàn 10'), CURRENT_DATE + INTERVAL '2 days', '11:30:00', 12, 'Tiệc liên hoan thành lập công ty', 'PENDING', 'BK007', NOW(), NOW()),
    -- BK008 (Đã CANCELLED hôm qua)
    ((SELECT id FROM users WHERE username = 'customer_nam'), (SELECT id FROM tables WHERE table_name = 'Bàn 02'), CURRENT_DATE - INTERVAL '1 day', '13:00:00', 2, 'Kỷ niệm ngày cưới 2 người', 'CANCELLED', 'BK008', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- 6. ĐƠN HÀNG (ORDERS)
INSERT INTO orders (id, customer_id, staff_id, table_id, booking_id, order_time, status, total_amount, payment_status, checkout_url, qr_code, created_at, updated_at)
VALUES
    -- Đơn hàng 1 (Lịch sử thanh toán đã hoàn thành cách đây 5 ngày)
    (1, 
     (SELECT id FROM users WHERE username = 'customer_le'), 
     (SELECT id FROM users WHERE username = 'staff_dung'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 01'), 
     (SELECT id FROM bookings WHERE booking_code = 'BK001'), 
     NOW() - INTERVAL '5 days 1 hour', 'SERVED', 310000.00, 'PAID', NULL, NULL, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
     
    -- Đơn hàng 2 (Lịch sử thanh toán đã hoàn thành cách đây 3 ngày)
    (2, 
     (SELECT id FROM users WHERE username = 'customer_vy'), 
     (SELECT id FROM users WHERE username = 'staff_lan'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 05'), 
     (SELECT id FROM bookings WHERE booking_code = 'BK002'), 
     NOW() - INTERVAL '3 days 1 hour', 'SERVED', 489000.00, 'PAID', NULL, NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
     
    -- Đơn hàng 3 (Lịch sử thanh toán đã hoàn thành bằng PayOS cách đây 2 ngày)
    (3, 
     (SELECT id FROM users WHERE username = 'customer_minh'), 
     (SELECT id FROM users WHERE username = 'staff_hoa'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 03'), 
     (SELECT id FROM bookings WHERE booking_code = 'BK003'), 
     NOW() - INTERVAL '2 days 2 hours', 'SERVED', 770000.00, 'PAID', NULL, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
     
    -- Đơn hàng 4 (Đơn hiện tại - Bàn 02 đang dùng bữa, chờ thanh toán)
    (4, 
     (SELECT id FROM users WHERE username = 'customer_le'), 
     (SELECT id FROM users WHERE username = 'staff_dung'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 02'), 
     NULL, 
     NOW() - INTERVAL '1 hour', 'SERVED', 190000.00, 'PENDING', 'https://api.payos.vn/v2/payment-requests/pay04', 'https://upload.wikimedia.org/wikipedia/commons/d/d0/QR_code_for_mobile_English_Wikipedia.svg', NOW(), NOW()),
     
    -- Đơn hàng 5 (Đơn hiện tại - Bàn 05 đang chuẩn bị món ăn)
    (5, 
     (SELECT id FROM users WHERE username = 'customer_vy'), 
     (SELECT id FROM users WHERE username = 'staff_lan'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 05'), 
     NULL, 
     NOW() - INTERVAL '20 minutes', 'PREPARING', 645000.00, 'PENDING', NULL, NULL, NOW(), NOW()),
     
    -- Đơn hàng 6 (Đơn hiện tại - Bàn 12 món ăn đã READY phục vụ)
    (6, 
     (SELECT id FROM users WHERE username = 'customer_nam'), 
     (SELECT id FROM users WHERE username = 'staff_hoa'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 12'), 
     NULL, 
     NOW() - INTERVAL '40 minutes', 'READY', 375000.00, 'PENDING', NULL, NULL, NOW(), NOW()),
     
    -- Đơn hàng 7 (Đơn mới đặt ngày hôm nay cho Bàn 07 qua Booking BK005)
    (7, 
     (SELECT id FROM users WHERE username = 'customer_nam'), 
     (SELECT id FROM users WHERE username = 'staff_dung'), 
     (SELECT id FROM tables WHERE table_name = 'Bàn 07'), 
     (SELECT id FROM bookings WHERE booking_code = 'BK005'), 
     NOW() - INTERVAL '5 minutes', 'PLACED', 475000.00, 'PENDING', NULL, NULL, NOW(), NOW());

-- Cập nhật chuỗi sequence của bảng orders để các bản ghi tiếp theo tăng tự động đúng từ 8 trở đi
SELECT setval('orders_id_seq', 7);

-- 7. CHI TIẾT ĐƠN HÀNG (ORDER ITEMS)
INSERT INTO order_items (order_id, menu_item_id, quantity, price, notes, status, round_number, is_confirmed, created_at, updated_at)
VALUES
    -- Đơn hàng 1 (Tổng: 310,000 VND)
    (1, (SELECT id FROM menu_items WHERE name = 'Súp Bào Ngư'), 1, 120000.00, 'Ăn ít cay', 'SERVED', 1, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (1, (SELECT id FROM menu_items WHERE name = 'Cơm Chiên Hải Sản'), 1, 95000.00, 'Cơm chiên nhiều hải sản', 'SERVED', 1, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (1, (SELECT id FROM menu_items WHERE name = 'Khoai Tây Chiên Bơ Tỏi'), 1, 45000.00, NULL, 'SERVED', 2, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (1, (SELECT id FROM menu_items WHERE name = 'Nước Suối'), 5, 10000.00, 'Đá kèm sẵn', 'SERVED', 1, TRUE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    
    -- Đơn hàng 2 (Tổng: 489,000 VND)
    (2, (SELECT id FROM menu_items WHERE name = 'Lẩu Nấm Gà Ta'), 1, 320000.00, 'Nước lẩu thanh ngọt', 'SERVED', 1, TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (2, (SELECT id FROM menu_items WHERE name = 'Salad Hoàng Đế'), 1, 85000.00, NULL, 'SERVED', 1, TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (2, (SELECT id FROM menu_items WHERE name = 'Cà Phê Muối'), 1, 39000.00, 'Ít đường sữa', 'SERVED', 1, TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (2, (SELECT id FROM menu_items WHERE name = 'Nước Ép Cam Tươi'), 1, 45000.00, 'Không bỏ đá', 'SERVED', 2, TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

    -- Đơn hàng 3 (Tổng: 770,000 VND)
    (3, (SELECT id FROM menu_items WHERE name = 'Sườn Nướng Tảng BBQ'), 2, 280000.00, 'Nướng chín kĩ BBQ', 'SERVED', 1, TRUE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (3, (SELECT id FROM menu_items WHERE name = 'Mỳ Ý Sốt Bò Bằm'), 1, 110000.00, 'Nhiều sốt phô mai sợi', 'SERVED', 1, TRUE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (3, (SELECT id FROM menu_items WHERE name = 'Trà Đào Cam Sả'), 2, 35000.00, 'Ít ngọt thanh', 'SERVED', 1, TRUE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

    -- Đơn hàng 4 (Tổng: 190,000 VND)
    (4, (SELECT id FROM menu_items WHERE name = 'Súp Bào Ngư'), 1, 120000.00, NULL, 'SERVED', 1, TRUE, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'),
    (4, (SELECT id FROM menu_items WHERE name = 'Trà Đào Cam Sả'), 2, 35000.00, NULL, 'SERVED', 1, TRUE, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour'),

    -- Đơn hàng 5 (Tổng: 645,000 VND)
    (5, (SELECT id FROM menu_items WHERE name = 'Lẩu Thái Hải Sản'), 1, 350000.00, 'Ăn cay cực độ', 'PREPARING', 1, TRUE, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'),
    (5, (SELECT id FROM menu_items WHERE name = 'Ba Chỉ Bò Cuộn Nấm Kim Châm'), 2, 125000.00, NULL, 'PREPARING', 1, TRUE, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'),
    (5, (SELECT id FROM menu_items WHERE name = 'Nước Ép Cam Tươi'), 1, 45000.00, 'Có kèm đá', 'SERVED', 1, TRUE, NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'),
    (5, (SELECT id FROM menu_items WHERE name = 'Bánh Flan Caramen'), 1, 25000.00, 'Ngọt vừa phải', 'PENDING', 2, FALSE, NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes'),

    -- Đơn hàng 6 (Tổng: 375,000 VND)
    (6, (SELECT id FROM menu_items WHERE name = 'Ba Chỉ Bò Cuộn Nấm Kim Châm'), 2, 125000.00, NULL, 'READY', 1, TRUE, NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes'),
    (6, (SELECT id FROM menu_items WHERE name = 'Gỏi Xoài Tôm Khô'), 1, 65000.00, NULL, 'SERVED', 1, TRUE, NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes'),
    (6, (SELECT id FROM menu_items WHERE name = 'Nước Ép Cam Tươi'), 2, 45000.00, NULL, 'SERVED', 1, TRUE, NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '40 minutes'),

    -- Đơn hàng 7 (Tổng: 475,000 VND)
    (7, (SELECT id FROM menu_items WHERE name = 'Lẩu Nấm Gà Ta'), 1, 320000.00, NULL, 'CONFIRMED', 1, TRUE, NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes'),
    (7, (SELECT id FROM menu_items WHERE name = 'Bò Lúc Lắc'), 1, 155000.00, NULL, 'CONFIRMED', 1, TRUE, NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes');

-- 8. LỊCH SỬ THANH TOÁN (PAYMENTS)
INSERT INTO payments (order_id, amount, method, paid_at, status, transaction_id, notes, created_at, updated_at)
VALUES
    (1, 310000.00, 'CASH', NOW() - INTERVAL '5 days', 'COMPLETED', 'TXN-CASH-10029', 'Khách hàng thanh toán tiền mặt trực tiếp tại quầy', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (2, 489000.00, 'CARD', NOW() - INTERVAL '3 days', 'COMPLETED', 'TXN-CARD-49219', 'Thanh toán quẹt thẻ ATM qua cổng POS Vietcombank', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (3, 770000.00, 'PAYOS', NOW() - INTERVAL '2 days', 'COMPLETED', 'TXN-PAYOS-77182', 'Thanh toán quét mã QR động PayOS chuyển khoản nhanh', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

-- 9. GIAO DỊCH TÍCH HỢP PAYOS (PAYMENT TRANSACTIONS)
INSERT INTO payment_transactions (id, payos_payment_id, internal_reference, payment_order_code, amount, currency, status, payment_method, description, expires_at, paid_at, reference, created_by, created_at, updated_at)
VALUES
    (1, 'PAYOS-TXN-77182', 'REF-ORD-003', 99281, 770000.00, 'VND', 'PAID', 'BANK_TRANSFER', 'Thanh toan hoa don don hang 3', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'FT26172618', (SELECT id FROM users WHERE username = 'customer_minh'), NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

INSERT INTO payment_transaction_orders (payment_transaction_id, order_id, amount_applied, created_at)
VALUES
    (1, 3, 770000.00, NOW() - INTERVAL '2 days');

SELECT setval('payment_transactions_id_seq', 1);

-- 10. VÍ ĐIỆN TỬ THÀNH VIÊN (WALLETS)
INSERT INTO wallets (user_id, balance, currency, status, created_at, updated_at)
VALUES
    ((SELECT id FROM users WHERE username = 'customer_le'), 1500000.00, 'VND', 'ACTIVE', NOW(), NOW()),
    ((SELECT id FROM users WHERE username = 'customer_vy'), 500000.00, 'VND', 'ACTIVE', NOW(), NOW()),
    ((SELECT id FROM users WHERE username = 'customer_minh'), 0.00, 'VND', 'ACTIVE', NOW(), NOW()),
    ((SELECT id FROM users WHERE username = 'customer_nam'), 200000.00, 'VND', 'ACTIVE', NOW(), NOW());

-- 11. BIẾN ĐỘNG SỐ DƯ VÍ (WALLET TRANSACTIONS)
INSERT INTO wallet_transactions (wallet_id, amount, before_balance, after_balance, type, description, created_at)
VALUES
    ((SELECT id FROM wallets WHERE user_id = (SELECT id FROM users WHERE username = 'customer_le')), 2000000.00, 0.00, 2000000.00, 'TOP_UP', 'Nạp tiền vào tài khoản ví qua chuyển khoản ngân hàng Vietcombank', NOW() - INTERVAL '5 days'),
    ((SELECT id FROM wallets WHERE user_id = (SELECT id FROM users WHERE username = 'customer_le')), -500000.00, 2000000.00, 1500000.00, 'PAYMENT', 'Thanh toán tự động hóa đơn ăn uống bàn Bàn 01', NOW() - INTERVAL '4 days'),
    ((SELECT id FROM wallets WHERE user_id = (SELECT id FROM users WHERE username = 'customer_vy')), 500000.00, 0.00, 500000.00, 'TOP_UP', 'Nạp tiền mặt trực tiếp tại quầy thanh toán của nhà hàng', NOW() - INTERVAL '3 days'),
    ((SELECT id FROM wallets WHERE user_id = (SELECT id FROM users WHERE username = 'customer_nam')), 200000.00, 0.00, 200000.00, 'TOP_UP', 'Nạp tiền khuyến mãi tặng thưởng thành viên mới đăng ký tài khoản ví', NOW() - INTERVAL '1 day');
