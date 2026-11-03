// UC-19: màn hình bán tại quầy. Chỉ hỗ trợ thao tác (đánh dấu ghế, tạm tính, hiện ô tiền mặt);
// giá, voucher, điểm và tiền thối luôn do server tính lại khi bấm "Tính tiền" / "Xác nhận".
(function () {
    var toast = document.getElementById('toast');
    if (toast) {
        setTimeout(function () { toast.remove(); }, 5000);
    }

    var form = document.getElementById('saleForm');
    if (!form) {
        return;
    }
    var confirmButton = document.getElementById('confirmButton');
    var confirmHint = document.getElementById('confirmHint');
    var liveTotal = document.getElementById('liveTotal');
    var cashField = document.getElementById('cashField');
    var referenceField = document.getElementById('referenceField');
    // Hình thức thu, tiền khách đưa và mã giao dịch không đổi số tiền cần thu; server vẫn kiểm tra lại khi xác nhận
    var PAYMENT_FIELDS = ['paymentMethod', 'cashReceived', 'paymentReference'];

    function money(value) {
        return Math.round(value).toLocaleString('vi-VN') + 'đ';
    }

    function refreshTotal() {
        if (!liveTotal) {
            return;
        }
        var total = 0;
        form.querySelectorAll('input[name="seatIds"]:checked').forEach(function (seat) {
            total += Number(seat.dataset.price) || 0;
        });
        form.querySelectorAll('input[name^="qty_"]').forEach(function (qty) {
            total += (Number(qty.dataset.price) || 0) * Math.max(0, parseInt(qty.value, 10) || 0);
        });
        liveTotal.textContent = money(total);
    }

    // Đơn đã đổi sau lần tính tiền thì phải tính lại trước khi xác nhận, tránh thu sai số tiền
    function markChanged() {
        if (confirmButton && !confirmButton.disabled) {
            confirmButton.disabled = true;
            confirmHint.textContent = 'Đơn vừa thay đổi, bấm Tính tiền lại trước khi xác nhận.';
        }
    }

    form.querySelectorAll('input[name="seatIds"]').forEach(function (seat) {
        seat.addEventListener('change', function () {
            seat.parentElement.classList.toggle('is-selected', seat.checked);
        });
    });
    form.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {
        radio.addEventListener('change', function () {
            cashField.hidden = radio.value !== 'Cash';
            // Thẻ và chuyển khoản cần mã giao dịch; hiện nhãn / nội dung chuyển khoản đúng hình thức đang chọn
            referenceField.hidden = radio.value === 'Cash';
            referenceField.querySelectorAll('[data-method]').forEach(function (part) {
                part.hidden = part.dataset.method !== radio.value;
            });
        });
    });
    form.addEventListener('input', function (event) {
        if (!PAYMENT_FIELDS.includes(event.target.name)) {
            markChanged();
        }
        refreshTotal();
    });
    form.addEventListener('change', function (event) {
        if (!PAYMENT_FIELDS.includes(event.target.name)) {
            markChanged();
        }
        refreshTotal();
    });
    form.addEventListener('submit', function (event) {
        if (event.submitter && event.submitter.value === 'confirm'
                && !window.confirm('Xác nhận đã thu đủ tiền của khách?')) {
            event.preventDefault();
        }
    });
    refreshTotal();
})();
