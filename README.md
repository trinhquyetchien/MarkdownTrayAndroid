# MarkdownTray Android

Companion Android app for [MarkdownTray](https://github.com/trinhquyetchien/MarkdownTrayLinux), designed for seamless mobile note-taking, task tracking, and schedule management directly from your Markdown and JSON files synced via Syncthing or local storage.

---

## 🚀 Tính năng nổi bật (Features)

- 📅 **Lịch biểu trực quan (Interactive Calendar):**
  - Tích hợp giao diện lịch tuần/tháng kéo thả mượt mà trên nền tảng WebView.
  - Tự động đồng bộ hai chiều trực tiếp với tệp `calendar.json`.
- 🔔 **Hệ thống nhắc nhở thông minh (Event Alarms & Notifications):**
  - Đặt lịch báo thức chính xác bằng Android `AlarmManager`.
  - Tự động phục hồi và kích hoạt lại lịch nhắc nhở sau khi thiết bị khởi động lại (`BOOT_COMPLETED`).
- 📝 **Trình xem & Soạn thảo Markdown (Live Editor & Viewer):**
  - **Interactive Checkbox:** Chạm để hoàn thành task `[ ]` -> `[x]` ngay trên giao diện xem, tự động cập nhật vào tệp `.md`.
  - Hỗ trợ cú pháp phong phú: Header, danh sách bullet, danh sách số, trích dẫn blockquote, inline bold, italic, code block, hashtag, và link.
  - Thanh công cụ định dạng nhanh khi gõ nội dung (H1-H3, Todo, Bold, Code, v.v.).
- 📆 **Ghi chú Hôm nay & Deadline:**
  - Truy cập 1-chạm vào nhật ký hằng ngày (`Daily/YYYY-MM-DD.md`).
  - Quản lý hạn chót công việc tập trung tại (`Daily/Deadline.md`).
- 📁 **Quản lý tệp & Thư mục tùy chỉnh (File Manager & Custom Menus):**
  - Quản lý danh mục ghi chú theo dự án tương tự các menu trên phiên bản Linux.
  - Tạo mới thư mục, tạo ghi chú, đổi tên và xóa tệp dễ dàng.
- 🔄 **Đồng bộ Syncthing cục bộ không qua trung gian:**
  - Hoạt động trực tiếp trên thư mục bộ nhớ ngoài (mặc định `/storage/emulated/0/Note` hoặc cấu hình tùy ý).
  - Hiển thị trạng thái đồng bộ tệp thời gian thực (real-time sync status line).
- 🎨 **Thiết kế hiện đại Material 3:**
  - Xây dựng 100% bằng Jetpack Compose.
  - Hỗ trợ Dynamic Color, Dark/Light Theme và Edge-to-Edge.

---

## 🛠 Yêu cầu hệ thống (Requirements)

- **Android version:** Android 8.0 (API Level 26) trở lên.
- **Quyền ứng dụng:**
  - `MANAGE_EXTERNAL_STORAGE` (*Truy cập tất cả tệp*): Cần thiết để ứng dụng đọc/ghi tệp Markdown trong thư mục Syncthing mà không bị giới hạn bởi Scoped Storage.
  - `POST_NOTIFICATIONS`: Nhận thông báo nhắc nhở sự kiện trên Android 13+.
  - `SCHEDULE_EXACT_ALARM`: Đảm bảo chuông nhắc nhở kích hoạt đúng thời điểm.
  - `RECEIVE_BOOT_COMPLETED`: Tự động lên lịch lại khi máy khởi động lại.

---

## 📦 Cấu trúc thư mục dữ liệu (Default Storage Structure)

Mặc định ứng dụng sẽ tìm và lưu trữ tại đường dẫn bộ nhớ:

```text
/storage/emulated/0/Note/
├── Daily/
│   ├── YYYY-MM-DD.md     # Nhật ký công việc theo ngày
│   └── Deadline.md       # Danh sách deadline công việc
├── calendar.json         # Dữ liệu sự kiện & lịch biểu
└── [Thư mục dự án khác]/ # Các tệp Markdown theo danh mục tùy chỉnh
```

> Bạn có thể thay đổi đường dẫn lưu trữ bất kỳ lúc nào trong mục **Cài đặt (Settings)** của ứng dụng.

---

## 💻 Hướng dẫn biên dịch (Build & Installation)

Yêu cầu máy tính có cài sẵn **JDK 17+** và Android SDK.

### 1. Clone repository
```bash
git clone git@github.com:trinhquyetchien/MarkdownTrayAndroid.git
cd MarkdownTrayAndroid
```

### 2. Biên dịch APK Debug
```bash
./gradlew assembleDebug
```
File APK sẽ được tạo tại:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Cài đặt trực tiếp lên thiết bị (qua ADB)
```bash
./gradlew installDebug
```

---

## 🔗 Liên kết liên quan

- Phiên bản Linux (Desktop System Tray): [MarkdownTray Linux](https://github.com/trinhquyetchien/MarkdownTrayLinux)

---

## 📄 Giấy phép (License)

Dự án được phân phối dưới giấy phép [MIT License](LICENSE).
