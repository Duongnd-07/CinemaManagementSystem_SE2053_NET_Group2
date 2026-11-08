// UC-35: sơ đồ ghế chỉ xem trên bảng kiểm tra phòng. Chuỗi layout cùng định dạng RoomLayoutForm (UC-25):
// S/V/C ghế dùng được, s/v/c ghế đang khóa, "=" nửa phải ghế đôi, "." lối đi. data-reported: vị trí ghế
// đang có báo cáo sự cố chưa xử lý (vd "A5,B7").
(function () {
    'use strict';

    var gridEl = document.getElementById('inspectionGrid');
    if (!gridEl) {
        return;
    }
    var lines = (gridEl.dataset.layout || '').split('/');
    var reported = {};
    (gridEl.dataset.reported || '').split(',').forEach(function (position) {
        if (position) {
            reported[position] = true;
        }
    });
    var cols = lines.length ? lines[0].length : 0;
    var TYPE_LABELS = {S: 'Ghế thường', V: 'Ghế VIP', C: 'Ghế đôi'};

    function label(text, kind) {
        var el = document.createElement('span');
        el.className = 'grid-label grid-label-' + kind;
        el.textContent = text;
        return el;
    }

    gridEl.style.setProperty('--cols', cols);
    gridEl.appendChild(label('', 'corner'));
    for (var c = 0; c < cols; c++) {
        gridEl.appendChild(label(String(c + 1), 'col'));
    }
    gridEl.appendChild(label('', 'corner'));
    lines.forEach(function (line, r) {
        var row = String.fromCharCode(65 + r);
        gridEl.appendChild(label(row, 'row'));
        for (var i = 0; i < line.length; i++) {
            var code = line.charAt(i);
            if (code === '=') {
                continue;
            }
            var cell = document.createElement('span');
            var classes = ['cell'];
            if (code === '.') {
                classes.push('cell-aisle');
                cell.title = row + (i + 1) + ' - Lối đi';
            } else {
                var type = code.toUpperCase();
                var position = row + (i + 1);
                classes.push('cell-' + type);
                if (type === 'C') {
                    classes.push('cell-wide');
                }
                var off = code !== type;
                if (off) {
                    classes.push('cell-off');
                }
                if (reported[position]) {
                    classes.push('lock-report');
                }
                cell.title = (type === 'C' ? position + '-' + (i + 2) : position) + ' - ' + TYPE_LABELS[type]
                    + (reported[position] ? ' (đang có báo cáo sự cố)' : off ? ' (đang khóa)' : '');
            }
            cell.className = classes.join(' ');
            cell.style.cursor = 'default';
            gridEl.appendChild(cell);
        }
        gridEl.appendChild(label(row, 'row'));
    });
})();
