// UC-36: quét mã vé. Máy quét mã vạch USB hoạt động như bàn phím: gõ nội dung QR rồi nhấn Enter. QR trong email
// là cả đoạn thông tin vé nhiều dòng (mỗi dòng một Enter), nên không gửi ngay khi Enter mà đợi máy quét gõ xong
// (không còn phím nào trong 300 ms). Gõ tay mã vé rồi Enter cũng gửi như vậy. Server tự tìm mã trong nội dung quét.
(function () {
    'use strict';

    var IDLE_MS = 300;
    var form = document.getElementById('scanForm');
    var input = document.getElementById('code');
    var timer = null;

    // Gửi khi đã ngừng gõ IDLE_MS sau một lần Enter (mã sai vẫn gửi để server báo "Không tìm thấy vé")
    function submitWhenIdle() {
        clearTimeout(timer);
        timer = setTimeout(function () {
            timer = null;
            if (input.value.trim()) {
                form.submit();
            }
        }, IDLE_MS);
    }

    if (form && input) {
        input.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') {
                return;
            }
            // Giữ xuống dòng (nội dung QR nhiều dòng) và đợi máy quét gõ xong rồi mới gửi
            e.preventDefault();
            input.value += '\n';
            submitWhenIdle();
        });
        input.addEventListener('input', function () {
            if (timer) {
                submitWhenIdle();
            }
        });
        // Sẵn sàng quét khách tiếp theo: con trỏ luôn ở ô mã vé, chọn sẵn nội dung cũ để quét đè
        if (!document.querySelector('[data-reprint]:focus')) {
            input.focus();
            input.select();
        }
    }

    // A3: quét bằng webcam (thư viện html5-qrcode tải khi cần)
    var cameraButton = document.getElementById('cameraButton');
    var cameraBox = document.getElementById('cameraBox');
    var cameraMessage = document.getElementById('cameraMessage');
    var scanner = null;
    var CAMERA_ERROR = 'Không truy cập được camera. Vui lòng nhập mã vé.';

    function startCamera() {
        cameraBox.classList.remove('hidden');
        scanner = new window.Html5Qrcode('cameraView');
        scanner.start({facingMode: 'environment'}, {fps: 10, qrbox: 240}, function (text) {
            scanner.stop().catch(function () {});
            input.value = text;
            form.submit();
        }, function () {
        }).catch(function () {
            cameraMessage.textContent = CAMERA_ERROR;
            input.focus();
        });
    }

    if (cameraButton) {
        cameraButton.addEventListener('click', function () {
            if (scanner) {
                return;
            }
            if (window.Html5Qrcode) {
                startCamera();
                return;
            }
            var script = document.createElement('script');
            script.src = 'https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js';
            script.onload = startCamera;
            script.onerror = function () {
                cameraBox.classList.remove('hidden');
                cameraMessage.textContent = CAMERA_ERROR;
            };
            document.head.appendChild(script);
        });
    }

    // A2: hộp thoại in lại
    var modal = document.getElementById('reprintModal');
    if (modal) {
        var toggle = function (open) {
            modal.classList.toggle('hidden', !open);
            modal.classList.toggle('flex', open);
        };
        document.querySelectorAll('[data-reprint]').forEach(function (button) {
            button.addEventListener('click', function () {
                document.getElementById('reprintTicket').value = button.dataset.reprint;
                document.getElementById('reprintCode').textContent = button.dataset.code;
                toggle(true);
                document.getElementById('reason').focus();
            });
        });
        modal.querySelector('[data-close-reprint]').addEventListener('click', function () {
            toggle(false);
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                toggle(false);
            }
        });
    }

    // Lượt in vừa ghi nhận: mở hộp thoại in của trình duyệt
    if (document.querySelector('#printArea[data-auto-print]')) {
        window.addEventListener('load', function () {
            window.print();
        });
    }
})();
