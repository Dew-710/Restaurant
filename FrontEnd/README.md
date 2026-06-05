# Frontend

## Chạy local

Frontend mặc định chạy ở `http://localhost:3000`.

1. Tạo file `.env.local` trong thư mục `FrontEnd` nếu muốn cấu hình rõ ràng.
2. Các biến nên có khi chạy local:
	 - `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`
	 - `NEXT_PUBLIC_APP_URL=http://localhost:3000`
3. Chạy app bằng `pnpm dev` hoặc `npm run dev` tùy máy bạn đang dùng.

## Khi chuyển sang tunnel/domain

Chỉ cần sửa các chỗ sau:

- `.env.local`
	- `NEXT_PUBLIC_API_BASE_URL`: trỏ về backend tunnel/domain
	- `NEXT_PUBLIC_APP_URL`: trỏ về domain/tunnel frontend
	- `NEXT_PUBLIC_PAYOS_RETURN_URL` và `NEXT_PUBLIC_PAYOS_CANCEL_URL` nếu muốn cố định callback PayOS
- `next.config.mjs`
	- `experimental.allowedOrigins`: thêm domain/tunnel mới nếu Next.js chặn origin lạ
- `components/sepay-payment.tsx`
	- nếu có URL backend hardcode fallback, đổi sang backend tunnel/domain mới

## Ghi chú

Frontend đã đọc URL backend từ biến môi trường, nên khi chuyển môi trường chỉ cần đổi `.env.local` trước. Chỉ sửa code nếu có thêm domain mới cần whitelist trong `next.config.mjs`.
