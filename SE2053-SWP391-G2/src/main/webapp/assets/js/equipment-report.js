// Form báo cáo sự cố: ô Ghế chỉ dùng khi loại thiết bị là "Seat" và chỉ liệt kê ghế của phòng đã chọn.
// Server vẫn kiểm tra lại ghế có thuộc phòng hay không.
(function () {
    var category = document.getElementById('category');
    var room = document.getElementById('roomId');
    var seat = document.getElementById('seatId');
    var seatField = document.getElementById('seatField');
    var seatNotice = document.getElementById('seatNotice');
    if (!category || !room || !seat) {
        return;
    }

    var placeholder = seat.options[0];
    var allSeats = Array.prototype.slice.call(seat.options, 1);

    function refresh() {
        var isSeat = category.value === 'Seat';
        seatField.hidden = !isSeat;
        seatNotice.hidden = !isSeat;

        var selected = seat.value;
        seat.innerHTML = '';
        seat.appendChild(placeholder);
        allSeats.forEach(function (option) {
            if (option.dataset.room === room.value) {
                seat.appendChild(option);
            }
        });
        seat.value = selected;
        if (seat.selectedIndex < 0) {
            seat.selectedIndex = 0;
        }
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
