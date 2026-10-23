// UC-13 bước 1-4: chọn ghế trên sơ đồ, tạm tính tiền, cập nhật trạng thái ghế định kỳ (dùng API trạng thái của UC-09),
// xác nhận độ tuổi (BR-AGE-01) và yêu cầu đăng nhập khi Guest bấm Tiếp tục (AF1). Server kiểm tra lại toàn bộ khi giữ ghế.
(function () {
    var root = document.getElementById('seatPicker');
    if (!root) {
        return;
    }
    var STATUS_CLASSES = ['is-available', 'is-pending', 'is-sold', 'is-maintenance'];
    var form = document.getElementById('holdForm');
    var continueBtn = document.getElementById('continueBtn');
    var selectedList = document.getElementById('selectedSeats');
    var selectedCount = document.getElementById('selectedCount');
    var selectedTotal = document.getElementById('selectedTotal');
    var liveAlert = document.getElementById('liveAlert');
    var fullyBookedBanner = document.getElementById('fullyBookedBanner');
    var liveDot = root.querySelector('.live-dot');
    var ageModal = document.getElementById('ageModal');
    var ageConfirmed = document.getElementById('ageConfirmed');
    var loginModal = document.getElementById('loginModal');
    var maxSeats = parseInt(root.dataset.maxSeats, 10) || 8;
    var intervalMs = Math.max(5, parseInt(root.dataset.refreshSeconds, 10) || 10) * 1000;
    var money = new Intl.NumberFormat('vi-VN');

    function parseIds(value) {
        return (value || '').split(',').filter(function (part) {
            return part.trim() !== '';
        });
    }

    var seats = {};
    root.querySelectorAll('button.seat[data-seat-id]').forEach(function (el) {
        seats[el.dataset.seatId] = el;
    });
    // Ghế chính khách đang giữ (quay lại từ bước checkout) coi như còn trống với khách
    var ownSeats = {};
    parseIds(root.dataset.ownSeats).forEach(function (id) {
        ownSeats[id] = true;
    });
    var selected = [];

    function setStatus(el, status) {
        STATUS_CLASSES.forEach(function (cls) {
            el.classList.remove(cls);
        });
        el.classList.add('is-' + status.toLowerCase());
        el.disabled = status !== 'AVAILABLE';
    }

    function showAlert(message) {
        liveAlert.querySelector('[data-message]').textContent = message;
        liveAlert.hidden = false;
    }

    function render() {
        var labels = [];
        var total = 0;
        selected.forEach(function (id) {
            labels.push(seats[id].dataset.label);
            total += parseFloat(seats[id].dataset.price) || 0;
        });
        selectedList.textContent = labels.length ? labels.join(', ') : selectedList.dataset.empty;
        selectedCount.textContent = selected.length;
        selectedTotal.textContent = money.format(total);
        if (continueBtn) {
            continueBtn.disabled = selected.length === 0;
        }
    }

    function select(id) {
        var el = seats[id];
        if (!el || el.disabled || selected.indexOf(id) >= 0 || selected.length >= maxSeats) {
            return false;
        }
        selected.push(id);
        el.classList.add('is-selected');
        el.setAttribute('aria-pressed', 'true');
        return true;
    }

    function deselect(id) {
        var index = selected.indexOf(id);
        if (index >= 0) {
            selected.splice(index, 1);
        }
        seats[id].classList.remove('is-selected');
        seats[id].setAttribute('aria-pressed', 'false');
    }

    // ===== Luật ghế lẻ: cùng thuật toán với SeatGapRule.java (server kiểm tra lại khi giữ ghế) =====
    // Ghế còn trống trước khi chọn (nút không bị disabled) chia thành đoạn theo từng dãy ghế liền nhau; ghế đang chọn
    // chia đoạn thành các khoảng trống. Khoảng trống đúng 1 ghế đơn là ghế lẻ, trừ khi đoạn chỉ thừa đúng 1 ghế.
    function col(el) {
        return parseInt(el.dataset.col, 10);
    }

    function width(el) {
        return parseInt(el.dataset.width, 10) || 1;
    }

    function isOrphanGap(gap) {
        return gap.length === 1 && width(gap[0]) === 1;
    }

    function orphanInRun(run, picked) {
        var count = run.filter(function (el) {
            return picked[el.dataset.seatId];
        }).length;
        if (count === 0 || run.length - count === 1) {
            return null;
        }
        var gap = [];
        for (var i = 0; i < run.length; i++) {
            if (picked[run[i].dataset.seatId]) {
                if (isOrphanGap(gap)) {
                    return gap[0].dataset.label;
                }
                gap = [];
            } else {
                gap.push(run[i]);
            }
        }
        return isOrphanGap(gap) ? gap[0].dataset.label : null;
    }

    function findOrphanSeat() {
        var picked = {};
        selected.forEach(function (id) {
            picked[id] = true;
        });
        var rows = {};
        Object.keys(seats).forEach(function (id) {
            var name = seats[id].dataset.row;
            (rows[name] = rows[name] || []).push(seats[id]);
        });
        var names = Object.keys(rows);
        for (var r = 0; r < names.length; r++) {
            var row = rows[names[r]].sort(function (a, b) {
                return col(a) - col(b);
            });
            var run = [];
            var previous = null;
            for (var i = 0; i < row.length; i++) {
                var free = !row[i].disabled;
                var adjacent = previous !== null && col(previous) + width(previous) === col(row[i]);
                if (!free || !adjacent) {
                    var orphan = orphanInRun(run, picked);
                    if (orphan) {
                        return orphan;
                    }
                    run = [];
                }
                if (free) {
                    run.push(row[i]);
                }
                previous = row[i];
            }
            var last = orphanInRun(run, picked);
            if (last) {
                return last;
            }
        }
        return null;
    }

    Object.keys(ownSeats).forEach(function (id) {
        if (seats[id]) {
            setStatus(seats[id], 'AVAILABLE');
        }
    });
    parseIds(root.dataset.preselected).forEach(select);
    render();

    root.addEventListener('click', function (event) {
        var el = event.target.closest('button.seat[data-seat-id]');
        if (!el || el.disabled) {
            return;
        }
        var id = el.dataset.seatId;
        if (selected.indexOf(id) >= 0) {
            deselect(id);
        } else if (!select(id)) {
            showAlert('Mỗi đơn chỉ được chọn tối đa ' + maxSeats + ' ghế.');
        }
        render();
    });

    // ===== Cập nhật trạng thái ghế =====
    var timer = null;
    var stopped = false;

    function layoutChanged(data) {
        var ids = Object.keys(data.seats);
        return ids.length !== Object.keys(seats).length || ids.some(function (id) {
            return !seats[id];
        });
    }

    function applyStatus(data) {
        if (data.error) {
            showAlert(data.error);
            if (continueBtn) {
                continueBtn.disabled = true;
            }
            stopped = true;
            return;
        }
        if (layoutChanged(data)) {
            stopped = true;
            window.location.reload();
            return;
        }
        var lost = [];
        Object.keys(data.seats).forEach(function (id) {
            var status = data.seats[id];
            if (ownSeats[id] && status === 'PENDING') {
                status = 'AVAILABLE';
            }
            setStatus(seats[id], status);
            if (status !== 'AVAILABLE' && selected.indexOf(id) >= 0) {
                deselect(id);
                lost.push(seats[id].dataset.label);
            }
        });
        if (lost.length) {
            // E2: ghế vừa bị người khác giữ/mua
            showAlert(root.dataset.takenMessage + ' (' + lost.join(', ') + ')');
            render();
        }
        fullyBookedBanner.hidden = !data.fullyBooked;
        liveDot.classList.remove('is-stale');
    }

    function refresh() {
        timer = null;
        fetch(root.dataset.statusUrl, {cache: 'no-store', headers: {'Accept': 'application/json'}})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(applyStatus)
            .catch(function () {
                liveDot.classList.add('is-stale');
            })
            .then(schedule);
    }

    function schedule() {
        if (!stopped && !document.hidden && timer === null) {
            timer = window.setTimeout(refresh, intervalMs);
        }
    }

    document.addEventListener('visibilitychange', function () {
        if (document.hidden && timer !== null) {
            window.clearTimeout(timer);
            timer = null;
        } else if (!document.hidden) {
            schedule();
        }
    });
    schedule();

    // ===== Xác nhận độ tuổi, đăng nhập, gửi form =====
    if (ageModal) {
        document.getElementById('ageConfirmBtn').addEventListener('click', function () {
            ageConfirmed.value = '1';
            ageModal.hidden = true;
        });
    }

    if (loginModal) {
        loginModal.querySelector('[data-close-modal]').addEventListener('click', function () {
            loginModal.hidden = true;
        });
    }

    if (form && continueBtn) {
        form.addEventListener('submit', function (event) {
            if (selected.length === 0) {
                event.preventDefault();
                return;
            }
            var orphan = findOrphanSeat();
            if (orphan) {
                event.preventDefault();
                showAlert(root.dataset.orphanMessage.replace('{0}', orphan));
                return;
            }
            if (continueBtn.dataset.guest === 'true') {
                event.preventDefault();
                var link = document.getElementById('loginLink');
                var returnUrl = link.dataset.returnUrl + '&seats=' + selected.join(',');
                link.href = link.dataset.loginUrl + encodeURIComponent(returnUrl);
                loginModal.hidden = false;
                return;
            }
            if (ageModal && ageConfirmed.value !== '1') {
                event.preventDefault();
                ageModal.hidden = false;
                return;
            }
            form.querySelectorAll('input[name="seatId"]').forEach(function (input) {
                input.remove();
            });
            selected.forEach(function (id) {
                var input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'seatId';
                input.value = id;
                form.appendChild(input);
            });
            continueBtn.disabled = true;
        });
    }

    var toast = document.getElementById('toast');
    if (toast) {
        window.setTimeout(function () {
            toast.remove();
        }, 4000);
    }
})();
