// UC-25: trình thiết kế sơ đồ ghế. Lưới được lưu vào ô ẩn #layout theo định dạng của RoomLayoutForm:
// các hàng ngăn bởi "/", mỗi ký tự một cột: S/V/C ghế đang dùng, s/v/c ghế bị khóa, "=" nửa phải của ghế đôi
// (ô ngay sau C/c), "." lối đi. Ghế đôi là một ghế chiếm 2 ô. Các chặn ở đây chỉ để báo sớm cho Manager;
// server kiểm tra lại toàn bộ khi lưu.
(function () {
    'use strict';

    var form = document.getElementById('roomForm');
    var gridEl = document.getElementById('roomGrid');
    if (!form || !gridEl) {
        return;
    }
    var layoutInput = document.getElementById('layout');
    var rowsInput = document.getElementById('rows');
    var colsInput = document.getElementById('cols');
    var messageEl = document.getElementById('editorMessage');
    var MAX_ROWS = parseInt(gridEl.dataset.maxRows, 10);
    var MAX_COLS = parseInt(gridEl.dataset.maxCols, 10);
    var AISLE = '.';
    var RIGHT = '=';
    var TYPE_LABELS = {S: 'Ghế thường', V: 'Ghế VIP', C: 'Ghế đôi'};
    var LOCK_LABELS = {
        sold: 'đã bán vé cho suất chiếu sắp tới',
        report: 'có báo cáo sự cố chưa xử lý',
        history: 'đã có lịch sử vé/giữ chỗ/sự cố'
    };

    // Vị trí ô trái của ghế (vd "A5") -> lý do giới hạn
    var locks = {};
    (gridEl.dataset.locks || '').split(',').forEach(function (item) {
        var parts = item.split(':');
        if (parts.length === 2) {
            locks[parts[0]] = parts[1];
        }
    });

    var grid = [];
    var painting = false;
    var messageTimer = null;

    function rowName(r) {
        return String.fromCharCode(65 + r);
    }

    function position(r, c) {
        return rowName(r) + (c + 1);
    }

    function clamp(value, max) {
        var n = parseInt(value, 10);
        return isNaN(n) ? 1 : Math.min(Math.max(n, 1), max);
    }

    function currentTool() {
        var checked = form.querySelector('input[name="tool"]:checked');
        return checked ? checked.value : 'S';
    }

    function say(text) {
        messageEl.textContent = text;
        clearTimeout(messageTimer);
        messageTimer = setTimeout(function () {
            messageEl.textContent = '';
        }, 4000);
    }

    function isCouple(code) {
        return code === 'C' || code === 'c';
    }

    function isDisabled(code) {
        return code !== AISLE && code !== RIGHT && code === code.toLowerCase();
    }

    // Cột trái của ghế đang chiếm ô (r, c); -1 nếu là lối đi
    function seatLeft(r, c) {
        var code = grid[r][c];
        if (code === AISLE) {
            return -1;
        }
        return code === RIGHT ? c - 1 : c;
    }

    function lockAt(r, left) {
        return left < 0 ? null : locks[position(r, left)];
    }

    function seatName(r, left) {
        return isCouple(grid[r][left]) ? position(r, left) + '-' + (left + 2) : position(r, left);
    }

    // Xóa một ghế khỏi lưới (cả 2 ô nếu là ghế đôi)
    function clearSeat(r, left) {
        if (isCouple(grid[r][left])) {
            grid[r][left + 1] = AISLE;
        }
        grid[r][left] = AISLE;
    }

    // Đặt loại mới cho ghế ở ô trái left, giữ trạng thái khóa nếu cần
    function setType(r, left, type, keepDisabled) {
        grid[r][left] = keepDisabled ? type.toLowerCase() : type;
    }

    function load() {
        var rows = clamp(rowsInput.value, MAX_ROWS);
        var cols = clamp(colsInput.value, MAX_COLS);
        var lines = (layoutInput.value || '').split('/');
        grid = [];
        for (var r = 0; r < rows; r++) {
            var line = lines[r] || '';
            var row = [];
            for (var c = 0; c < cols; c++) {
                var ch = line.charAt(c);
                row.push(/^[SVCsvc.=]$/.test(ch) ? ch : 'S');
            }
            // Sửa chuỗi hỏng: "=" phải đứng ngay sau C/c, C/c phải có "=" theo sau
            for (c = 0; c < cols; c++) {
                if (row[c] === RIGHT && !(c > 0 && isCouple(row[c - 1]))) {
                    row[c] = 'S';
                }
                if (isCouple(row[c]) && row[c + 1] !== RIGHT) {
                    row[c] = row[c] === 'C' ? 'S' : 's';
                }
            }
            grid.push(row);
        }
        rowsInput.value = rows;
        colsInput.value = cols;
    }

    // Đổi ô theo công cụ; trả về thông báo nếu thao tác bị chặn hoặc bị điều chỉnh
    function applyTool(r, c, tool) {
        var left = seatLeft(r, c);
        var lock = lockAt(r, left);
        var code = left < 0 ? AISLE : grid[r][left];

        if (tool === AISLE) {
            if (left < 0) {
                return null;
            }
            if (lock) {
                return 'Ghế ' + seatName(r, left) + ' ' + LOCK_LABELS[lock] + ' nên không thể xóa, chỉ có thể khóa.';
            }
            clearSeat(r, left);
            return null;
        }

        if (tool === 'X') {
            if (left < 0) {
                grid[r][c] = 's';
                return null;
            }
            if (!isDisabled(code) && lock === 'sold') {
                return 'Ghế ' + seatName(r, left) + ' đã bán vé cho suất chiếu sắp tới nên không thể khóa.';
            }
            grid[r][left] = code.toLowerCase();
            return null;
        }

        if (tool === 'C') {
            return applyCouple(r, c, left, code, lock);
        }

        // Ghế thường / VIP
        if (left >= 0 && code.toUpperCase() !== tool && lock === 'sold') {
            return 'Ghế ' + seatName(r, left) + ' đã bán vé cho suất chiếu sắp tới nên không thể đổi loại ghế.';
        }
        var keep = lock === 'report';
        if (left < 0) {
            grid[r][c] = tool;
        } else if (isCouple(code)) {
            // Tách ghế đôi: ô trái giữ ghế cũ (đổi loại), ô phải thành ghế mới cùng loại
            setType(r, left, tool, keep);
            grid[r][left + 1] = tool;
        } else {
            setType(r, left, tool, keep);
        }
        return keep ? 'Ghế ' + position(r, left) + ' có báo cáo sự cố chưa xử lý nên vẫn giữ trạng thái khóa.' : null;
    }

    // Ghế đôi chiếm ô (r, c) và ô bên phải; ô bấm vào là nửa phải của ghế đôi thì dùng chính ghế đó
    function applyCouple(r, c, left, code, lock) {
        if (left >= 0 && isCouple(code)) {
            if (lock === 'report' || !isDisabled(code)) {
                return null;
            }
            grid[r][left] = 'C';
            return null;
        }
        var cols = grid[r].length;
        if (c + 1 >= cols) {
            return 'Ghế đôi cần 2 ô liền nhau, ô ' + position(r, c) + ' là cột cuối.';
        }
        if (left >= 0 && lock === 'sold') {
            return 'Ghế ' + seatName(r, left) + ' đã bán vé cho suất chiếu sắp tới nên không thể đổi loại ghế.';
        }
        var rightLeft = seatLeft(r, c + 1);
        if (rightLeft >= 0 && rightLeft !== left && lockAt(r, rightLeft)) {
            return 'Ghế ' + seatName(r, rightLeft) + ' ' + LOCK_LABELS[lockAt(r, rightLeft)]
                + ' nên không thể bị ghế đôi thay thế.';
        }
        if (rightLeft >= 0 && rightLeft !== left) {
            clearSeat(r, rightLeft);
        }
        var keep = lock === 'report';
        grid[r][c] = keep ? 'c' : 'C';
        grid[r][c + 1] = RIGHT;
        return keep ? 'Ghế ' + position(r, c) + ' có báo cáo sự cố chưa xử lý nên vẫn giữ trạng thái khóa.' : null;
    }

    function paint(cells) {
        var tool = currentTool();
        var notice = null;
        cells.forEach(function (rc) {
            notice = applyTool(rc[0], rc[1], tool) || notice;
        });
        if (notice) {
            say(notice);
        }
        render();
    }

    function cellTitle(r, c) {
        var code = grid[r][c];
        if (code === AISLE) {
            return position(r, c) + ' - Lối đi';
        }
        var text = seatName(r, c) + ' - ' + TYPE_LABELS[code.toUpperCase()]
            + (isCouple(code) ? ' (2 người)' : '') + (isDisabled(code) ? ' (đang khóa)' : '');
        var lock = locks[position(r, c)];
        return lock ? text + ' - ' + LOCK_LABELS[lock] : text;
    }

    function render() {
        var rows = grid.length;
        var cols = rows ? grid[0].length : 0;
        gridEl.style.setProperty('--cols', cols);
        gridEl.textContent = '';
        var counts = {total: 0, capacity: 0, S: 0, V: 0, C: 0, off: 0, aisle: 0};

        gridEl.appendChild(label('', 'corner'));
        for (var c = 0; c < cols; c++) {
            gridEl.appendChild(label(String(c + 1), 'col', c));
        }
        gridEl.appendChild(label('', 'corner'));
        for (var r = 0; r < rows; r++) {
            gridEl.appendChild(label(rowName(r), 'row', r));
            for (c = 0; c < cols; c++) {
                var code = grid[r][c];
                if (code === RIGHT) {
                    // Nửa phải đã nằm trong nút của ghế đôi (chiếm 2 cột)
                    continue;
                }
                var cell = document.createElement('button');
                cell.type = 'button';
                cell.dataset.r = r;
                cell.dataset.c = c;
                cell.title = cellTitle(r, c);
                var classes = ['cell'];
                if (code === AISLE) {
                    classes.push('cell-aisle');
                    counts.aisle++;
                } else {
                    var type = code.toUpperCase();
                    classes.push('cell-' + type);
                    if (type === 'C') {
                        classes.push('cell-wide');
                    }
                    counts.total++;
                    counts.capacity += type === 'C' ? 2 : 1;
                    if (isDisabled(code)) {
                        classes.push('cell-off');
                        counts.off++;
                    } else {
                        counts[type]++;
                    }
                    var lock = locks[position(r, c)];
                    if (lock) {
                        classes.push('lock-' + lock);
                    }
                }
                cell.className = classes.join(' ');
                cell.setAttribute('aria-label', cell.title);
                gridEl.appendChild(cell);
            }
            gridEl.appendChild(label(rowName(r), 'row', r));
        }
        Object.keys(counts).forEach(function (key) {
            var el = document.querySelector('[data-stat="' + key + '"]');
            if (el) {
                el.textContent = counts[key];
            }
        });
        layoutInput.value = grid.map(function (row) {
            return row.join('');
        }).join('/');
    }

    function label(text, kind, index) {
        var el = document.createElement(kind === 'corner' ? 'span' : 'button');
        el.className = 'grid-label grid-label-' + kind;
        el.textContent = text;
        if (kind !== 'corner') {
            el.type = 'button';
            el.dataset.kind = kind;
            el.dataset.index = index;
            el.title = kind === 'row' ? 'Áp dụng công cụ cho cả hàng ' + text : 'Áp dụng công cụ cho cả cột ' + text;
        }
        return el;
    }

    // Đổi kích thước, giữ nguyên các ô cũ; ô mới là ghế thường. Không cho cắt mất (hay cắt đôi) ghế bị giới hạn;
    // ghế đôi bị cắt ở cột cuối thì thành ghế đơn cùng trạng thái.
    function resize() {
        var rows = clamp(rowsInput.value, MAX_ROWS);
        var cols = clamp(colsInput.value, MAX_COLS);
        var blocked = [];
        grid.forEach(function (row, r) {
            row.forEach(function (code, c) {
                if (code === AISLE || code === RIGHT || !locks[position(r, c)]) {
                    return;
                }
                var lastCol = isCouple(code) ? c + 1 : c;
                if (r >= rows || lastCol >= cols) {
                    blocked.push(seatName(r, c));
                }
            });
        });
        if (blocked.length) {
            say('Không thể thu nhỏ lưới vì sẽ xóa hoặc cắt ghế đã có lịch sử: ' + blocked.slice(0, 8).join(', ')
                + (blocked.length > 8 ? '...' : '') + '.');
            rowsInput.value = grid.length;
            colsInput.value = grid[0].length;
            return false;
        }
        var next = [];
        for (var r = 0; r < rows; r++) {
            var row = [];
            for (var c = 0; c < cols; c++) {
                row.push(grid[r] && grid[r][c] !== undefined ? grid[r][c] : 'S');
            }
            var last = row[cols - 1];
            if (isCouple(last)) {
                row[cols - 1] = last === 'C' ? 'S' : 's';
            }
            next.push(row);
        }
        grid = next;
        rowsInput.value = rows;
        colsInput.value = cols;
        render();
        return true;
    }

    function cellFromPoint(x, y) {
        var el = document.elementFromPoint(x, y);
        return el && el.classList.contains('cell') && el.dataset.r !== undefined ? el : null;
    }

    // Ô của cả hàng / cột; ghế đôi tô theo từng cặp từ trái sang
    function lineCells(kind, index, tool) {
        var cells = [];
        grid.forEach(function (row, r) {
            if (kind === 'row' && r !== index) {
                return;
            }
            for (var c = 0; c < row.length; c++) {
                if (kind === 'col' && c !== index) {
                    continue;
                }
                cells.push([r, c]);
                if (kind === 'row' && tool === 'C') {
                    c++;
                }
            }
        });
        return cells;
    }

    // Bấm/kéo để tô (chuột và cảm ứng)
    gridEl.addEventListener('pointerdown', function (e) {
        var target = e.target;
        if (target.dataset.kind) {
            paint(lineCells(target.dataset.kind, parseInt(target.dataset.index, 10), currentTool()));
            return;
        }
        if (target.dataset.r === undefined) {
            return;
        }
        e.preventDefault();
        painting = true;
        paint([[parseInt(target.dataset.r, 10), parseInt(target.dataset.c, 10)]]);
    });
    gridEl.addEventListener('pointermove', function (e) {
        if (!painting) {
            return;
        }
        var cell = cellFromPoint(e.clientX, e.clientY);
        if (cell) {
            var r = parseInt(cell.dataset.r, 10);
            var c = parseInt(cell.dataset.c, 10);
            var before = grid[r].join('');
            applyTool(r, c, currentTool());
            if (grid[r].join('') !== before) {
                render();
            }
        }
    });
    document.addEventListener('pointerup', function () {
        painting = false;
    });

    document.getElementById('applySize').addEventListener('click', resize);
    [rowsInput, colsInput].forEach(function (input) {
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                resize();
            }
        });
    });

    // Kích thước gõ nhưng chưa áp dụng thì áp dụng trước khi gửi, để lưới khớp với số hàng/cột
    form.addEventListener('submit', function (e) {
        var rows = clamp(rowsInput.value, MAX_ROWS);
        var cols = clamp(colsInput.value, MAX_COLS);
        if ((rows !== grid.length || cols !== grid[0].length) && !resize()) {
            e.preventDefault();
            return;
        }
        render();
    });

    load();
    render();
})();
