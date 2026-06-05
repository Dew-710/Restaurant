# Backend

## Chạy local

Backend mặc định chạy ở `http://localhost:8080`.

1. Cấu hình database trong `src/main/resources/application-postgres.properties`.
2. Kiểm tra các biến môi trường nếu muốn ghi đè mặc định:
	 - `FRONTEND_URL` mặc định là `http://localhost:3000`
	 - `WEBSOCKET_ALLOWED_ORIGINS` nếu cần mở thêm origin khác
	 - `PAYOS_RETURN_URL` và `PAYOS_CANCEL_URL` nếu test thanh toán trên domain/tunnel
3. Chạy backend bằng Maven wrapper trong thư mục `BackEnd`.

## Khi chuyển sang tunnel/domain

Chỉ cần sửa các chỗ sau:

- `src/main/resources/application.properties`
	- `frontend.url` nếu frontend không còn ở `http://localhost:3000`
	- `payos.return-url` và `payos.cancel-url` nếu callback PayOS phải trỏ sang domain/tunnel mới
- `src/main/java/com/restaurant/backend/Config/SecurityConfig.java`
	- thêm origin mới vào `configuration.setAllowedOriginPatterns(...)` nếu frontend chạy trên domain/tunnel khác
- `src/main/java/com/restaurant/backend/Service/impl/TableServiceImpl.java`
	- biến `FRONTEND_URL` dùng để tạo QR code cho menu, cần trỏ đúng domain/tunnel frontend

## Ghi chú

Nếu đổi domain, nhớ cập nhật cả backend URL và frontend URL để tránh lỗi CORS, QR code sai link, hoặc PayOS không chấp nhận callback localhost.
