// UC-13 bước 6-9 và trang chờ thanh toán: đồng hồ giữ ghế, nút tăng/giảm số lượng bắp nước,
// tự tính lại tổng tiền khi đổi lựa chọn, tự tải lại khi chờ VNPay, xác nhận trước khi hủy đơn, chặn bấm gửi hai lần.
(function () {
    var timer = document.getElementById('holdTimer');
    if (timer) {
        var clock = timer.querySelector('[data-clock]');
        var deadline = Date.now() + (parseInt(timer.dataset.remaining, 10) || 0) * 1000;
        var tick = function () {
            var seconds = Math.max(0, Math.round((deadline - Date.now()) / 1000));
            clock.textContent = String(Math.floor(seconds / 60)).padStart(2, '0') + ':'
                + String(seconds % 60).padStart(2, '0');
            timer.classList.toggle('is-urgent', seconds <= 60);
            if (seconds === 0) {
                // E1: server xác nhận hết hạn, nhả ghế và đưa khách về sơ đồ ghế kèm thông báo
                window.location.href = timer.dataset.expiredUrl;
                return;
            }
            window.setTimeout(tick, 1000);
        };
        tick();
    }

    // UC-14: đã chuyển sang VNPay, đơn chỉ đổi trạng thái khi server nhận IPN nên tải lại định kỳ để hiện kết quả
    var awaiting = document.querySelector('[data-auto-refresh]');
    if (awaiting) {
        window.setTimeout(function () {
            window.location.reload();
        }, Math.max(3, parseInt(awaiting.dataset.autoRefresh, 10) || 5) * 1000);
    }

    var form = document.getElementById('checkoutForm');
    if (form) {
        setupAutoQuote(form);
    }

    // Bước 6-8: mỗi khi đổi bắp nước / voucher / điểm thì gửi ngầm action=update (server kiểm tra và tính lại như
    // khi bấm nút), rồi thay các vùng tổng tiền và thông báo bằng bản mới của trang checkout - không tải lại trang
    function setupAutoQuote(form) {
        var REGION_IDS = ['generalAlert', 'concessionFeedback', 'voucherFeedback', 'voucherOptions', 'pointsInfo',
            'pointsFeedback', 'orderTotals'];
        var status = document.getElementById('quoteStatus');
        // Không dùng form.action: form có nút name="action" nên thuộc tính đó trả về chính nút này
        var url = form.getAttribute('action');
        var debounce = null;
        var sequence = 0;

        function schedule(delay) {
            window.clearTimeout(debounce);
            debounce = window.setTimeout(send, delay);
        }

        function showStatus(text) {
            status.textContent = text;
            status.hidden = !text;
        }

        function send() {
            var data = new URLSearchParams(new FormData(form));
            data.set('action', 'update');
            var current = ++sequence;
            showStatus('Đang cập nhật tổng tiền...');
            fetch(url, {method: 'POST', body: data, credentials: 'same-origin', headers: {'X-Requested-With': 'fetch'}})
                .then(function (response) {
                    // Hết giờ giữ ghế / hết phiên đăng nhập: server chuyển hướng sang trang khác thì đi theo
                    if (response.redirected && response.url.indexOf(url) < 0) {
                        window.location.href = response.url;
                        return null;
                    }
                    if (!response.ok) {
                        throw new Error('HTTP ' + response.status);
                    }
                    return response.text();
                })
                .then(function (html) {
                    // Khách đã đổi tiếp trong lúc chờ: bỏ kết quả cũ, chờ kết quả của lần gửi mới nhất
                    if (html === null || current !== sequence) {
                        return;
                    }
                    applyPage(new DOMParser().parseFromString(html, 'text/html'));
                    showStatus('');
                })
                .catch(function () {
                    if (current === sequence) {
                        showStatus('Không cập nhật được tổng tiền. Vui lòng thử lại.');
                    }
                });
        }

        function applyPage(doc) {
            REGION_IDS.forEach(function (id) {
                var fresh = doc.getElementById(id);
                var old = document.getElementById(id);
                if (fresh && old) {
                    old.replaceWith(document.importNode(fresh, true));
                }
            });
            // Đồng bộ ô nhập với giá trị server đã chấp nhận, trừ ô khách đang gõ dở
            form.querySelectorAll('input[name^="qty_"], #points, #voucherCode').forEach(function (input) {
                var fresh = doc.querySelector('[name="' + input.name + '"]');
                if (!fresh) {
                    return;
                }
                input.classList.toggle('is-invalid', fresh.classList.contains('is-invalid'));
                if (input !== document.activeElement) {
                    input.value = fresh.value;
                }
            });
            form.querySelectorAll('.concession-card').forEach(function (card) {
                var fresh = doc.getElementById(card.id);
                if (fresh) {
                    card.classList.toggle('is-picked', fresh.classList.contains('is-picked'));
                }
            });
        }

        form.querySelectorAll('.qty-stepper').forEach(function (stepper) {
            var input = stepper.querySelector('input');
            stepper.querySelectorAll('button[data-step]').forEach(function (button) {
                button.addEventListener('click', function () {
                    var max = parseInt(input.max, 10) || 0;
                    var value = (parseInt(input.value, 10) || 0) + parseInt(button.dataset.step, 10);
                    input.value = Math.min(max, Math.max(0, value));
                    schedule(300);
                });
            });
        });

        form.addEventListener('input', function (event) {
            var name = event.target.name || '';
            if (name.indexOf('qty_') === 0) {
                schedule(500);
            } else if (name === 'points') {
                schedule(600);
            } else if (name === 'voucherCode') {
                // Chờ khách gõ xong mã rồi mới kiểm tra, tránh báo "mã không hợp lệ" khi mới gõ vài ký tự
                schedule(800);
            }
        });
        form.addEventListener('change', function (event) {
            if (event.target.name === 'voucherCode' || event.target.name === 'points') {
                schedule(0);
            }
        });
        // Chọn voucher từ ví: điền mã vào ô rồi tính lại ngay. Gắn trên form vì vùng danh sách được thay sau mỗi lần tính.
        form.addEventListener('click', function (event) {
            var apply = event.target.closest('[data-voucher-apply]');
            var clear = event.target.closest('[data-voucher-clear]');
            if (!apply && !clear) {
                return;
            }
            var input = document.getElementById('voucherCode');
            input.value = apply ? apply.dataset.voucherApply : '';
            input.classList.remove('is-invalid');
            schedule(0);
        });
        // Enter trong ô nhập chỉ áp dụng ngay, không gửi form (tránh vô tình xác nhận đơn)
        form.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' && event.target.tagName === 'INPUT') {
                event.preventDefault();
                schedule(0);
            }
        });
    }

    document.querySelectorAll('form[data-confirm]').forEach(function (cancelForm) {
        cancelForm.addEventListener('submit', function (event) {
            if (!window.confirm(cancelForm.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-submit-once]').forEach(function (button) {
        button.form.addEventListener('submit', function (event) {
            // Khách bấm "Hủy" ở hộp thoại xác nhận (data-confirm) thì không gửi nên không khóa nút
            if (event.submitter !== button || event.defaultPrevented) {
                return;
            }
            // Vô hiệu hóa sau khi trình duyệt đã lấy name/value của nút vào dữ liệu gửi đi
            window.setTimeout(function () {
                button.disabled = true;
            }, 0);
        });
    });

    var toast = document.getElementById('toast');
    if (toast) {
        window.setTimeout(function () {
            toast.remove();
        }, 4000);
    }
})();
