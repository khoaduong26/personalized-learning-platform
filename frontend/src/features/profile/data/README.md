# Dữ liệu địa chỉ Việt Nam

`vietnam-addresses.json` chứa 34 tỉnh/thành phố và 3.321 xã/phường/đặc khu theo bộ dữ liệu `v5.2.0` của [Vietnamese Provinces Database](https://github.com/thanglequoc/vietnamese-provinces-database/tree/v5.2.0).

Nguồn JSON là [vn_only_simplified_json_generated_data_vn_units_minified.json](https://github.com/thanglequoc/vietnamese-provinces-database/blob/v5.2.0/json/vn_only_simplified_json_generated_data_vn_units_minified.json). Bản phát hành được tạo ngày 20/09/2026. Bản dùng trong dự án chỉ giữ mã, tên tỉnh và mã, tên các đơn vị trực thuộc, đồng thời đổi tên khóa sang `code`, `name`, `wards`. Đây là dữ liệu cộng đồng tổng hợp, cần đối chiếu nguồn hành chính khi cập nhật.

Giấy phép MIT và thông tin tác giả được giữ trong `LICENSE.txt`. Dữ liệu được đóng gói cùng frontend, không gọi API địa chỉ bên ngoài khi người dùng nhập hồ sơ.

Mã tỉnh/phường chỉ dùng cho trạng thái lựa chọn trên giao diện. Khi lưu hồ sơ, tên tỉnh được lưu vào `city`, địa chỉ chi tiết và tên phường/xã được ghép vào `address`. Entity và hợp đồng API hồ sơ giữ nguyên.
