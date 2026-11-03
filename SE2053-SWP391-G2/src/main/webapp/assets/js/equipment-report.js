// Form báo cáo sự cố: loại "Ghế" hiện danh sách ghế của phòng đã chọn; máy chiếu / âm thanh / điều hòa hiện
// thiết bị cụ thể của phòng (nếu phòng đã khai báo). Chọn được nhiều cái, mỗi cái thành một báo cáo.
// Ô bị ẩn được disable để không gửi lên; server vẫn kiểm tra lại ghế/thiết bị có thuộc phòng hay không.
(function () {
    var category = document.getElementById('category');
    var room = document.getElementById('roomId');
    var seatField = document.getElementById('seatField');
    var unitField = document.getElementById('unitField');
    var seatNotice = document.getElementById('seatNotice');
    if (!category || !room || !seatField || !unitField) {
        return;
    }

    // Hiện/ẩn từng ô theo điều kiện; trả về số ô đang hiện
    function filter(field, matches) {
        var visible = 0;
        Array.prototype.forEach.call(field.querySelectorAll('label[data-room]'), function (item) {
            var show = matches(item);
            // class inline-flex của Tailwind đè thuộc tính hidden nên phải ẩn bằng style
            item.style.display = show ? '' : 'none';
            item.querySelector('input').disabled = !show;
            if (show) {
                visible++;
            }
        });
        return visible;
    }

    function refresh() {
        var isSeat = category.value === 'Seat';
        var seats = filter(seatField, function (item) {
            return isSeat && item.dataset.room === room.value;
        });
        var units = filter(unitField, function (item) {
            return item.dataset.room === room.value && item.dataset.category === category.value;
        });
        seatField.hidden = !isSeat || seats === 0;
        unitField.hidden = units === 0;
        seatNotice.hidden = !isSeat;
        refreshOpenReports();
    }

    // Chỉ hiện các sự cố đang mở của phòng đã chọn; ẩn cả khối nếu phòng không có sự cố nào
    function refreshOpenReports() {
        var box = document.getElementById('openReports');
        if (!box) {
            return;
        }
        var visible = 0;
        Array.prototype.forEach.call(box.querySelectorAll('li'), function (item) {
            var match = room.value !== '' && item.dataset.room === room.value;
            item.hidden = !match;
            if (match) {
                visible++;
            }
        });
        box.hidden = visible === 0;
    }

    category.addEventListener('change', refresh);
    room.addEventListener('change', refresh);
    refresh();
})();
