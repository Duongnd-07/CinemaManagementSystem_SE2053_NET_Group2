// UC-09: tải lại trạng thái ghế định kỳ để sơ đồ phản ánh ghế vừa được giữ/bán/bảo trì mà không cần F5
(function () {
    var root = document.getElementById('seatMap');
    if (!root) {
        return;
    }
    var STATUS_CLASSES = ['is-available', 'is-pending', 'is-sold', 'is-maintenance'];
    var statusUrl = root.dataset.statusUrl;
    var intervalMs = Math.max(5, parseInt(root.dataset.refreshSeconds, 10) || 10) * 1000;
    var banner = document.getElementById('fullyBookedBanner');
    var errorBox = document.getElementById('refreshError');
    var updatedAt = document.getElementById('updatedAt');
    var liveDot = root.querySelector('.live-dot');
    var timer = null;
    var loading = false;
    var stopped = false;

    var seats = {};
    root.querySelectorAll('[data-seat-id]').forEach(function (el) {
        seats[el.dataset.seatId] = el;
    });
    var labels = {};
    root.querySelectorAll('.legend-list [data-status]').forEach(function (li) {
        labels[li.dataset.status] = li.dataset.label;
    });

    function applySeat(el, status) {
        var cls = 'is-' + status.toLowerCase();
        if (el.classList.contains(cls)) {
            return;
        }
        STATUS_CLASSES.forEach(function (c) {
            el.classList.remove(c);
        });
        el.classList.add(cls);
        var text = el.dataset.name + ' · ' + (labels[status] || status);
        el.title = text;
        el.setAttribute('aria-label', text);
    }

    // Manager thêm/bớt ghế của phòng thì bố cục lưới đã khác, tải lại cả trang
    function layoutChanged(data) {
        var ids = Object.keys(data.seats);
        if (ids.length !== Object.keys(seats).length) {
            return true;
        }
        return ids.some(function (id) {
            return !seats[id];
        });
    }

    function render(data) {
        if (data.error) {
            // E2 hoặc suất chiếu không còn hợp lệ: chặn xem sơ đồ, dừng cập nhật
            errorBox.querySelector('[data-message]').textContent = data.error;
            errorBox.hidden = false;
            banner.hidden = true;
            root.hidden = true;
            stop();
            return;
        }
        if (layoutChanged(data)) {
            stop();
            window.location.reload();
            return;
        }
        Object.keys(data.seats).forEach(function (id) {
            applySeat(seats[id], data.seats[id]);
        });
        Object.keys(data.counts).forEach(function (status) {
            var counter = document.getElementById('count-' + status);
            if (counter) {
                counter.textContent = data.counts[status];
            }
        });
        banner.hidden = !data.fullyBooked;
        updatedAt.textContent = data.updatedAt;
        liveDot.classList.remove('is-stale');
    }

    function refresh() {
        timer = null;
        if (loading) {
            return;
        }
        loading = true;
        fetch(statusUrl, {cache: 'no-store', headers: {'Accept': 'application/json'}})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(render)
            .catch(function () {
                // Lỗi mạng/server tạm thời: giữ sơ đồ cũ, đánh dấu chưa cập nhật và thử lại ở chu kỳ sau
                liveDot.classList.add('is-stale');
            })
            .then(function () {
                loading = false;
                schedule();
            });
    }

    function schedule() {
        if (!stopped && !document.hidden && timer === null) {
            timer = window.setTimeout(refresh, intervalMs);
        }
    }

    function stop() {
        stopped = true;
        if (timer !== null) {
            window.clearTimeout(timer);
            timer = null;
        }
    }

    // Tab ẩn thì ngừng gọi server, quay lại thì cập nhật ngay
    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            if (timer !== null) {
                window.clearTimeout(timer);
                timer = null;
            }
        } else if (!stopped && timer === null && !loading) {
            refresh();
        }
    });

    schedule();
})();
