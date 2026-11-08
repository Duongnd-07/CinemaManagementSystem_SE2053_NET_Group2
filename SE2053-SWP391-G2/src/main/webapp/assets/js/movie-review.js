// UC-17: chọn số sao bằng click (ô radio bị ẩn vẫn nhận giá trị gửi lên server)
document.querySelectorAll('[data-star-picker]').forEach(function (picker) {
    var stars = picker.querySelectorAll('[data-star]');
    var radios = picker.querySelectorAll('input[type="radio"]');

    var paint = function (value) {
        stars.forEach(function (star) {
            var active = Number(star.dataset.star) <= value;
            star.classList.toggle('text-[#ffb95f]', active);
            star.classList.toggle('text-[#4a4e5c]', !active);
        });
    };

    radios.forEach(function (radio) {
        radio.addEventListener('change', function () {
            paint(Number(radio.value));
        });
    });
});

// Đếm ký tự nhận xét (giới hạn 1000, khớp Comment NVARCHAR(1000) phía server)
document.querySelectorAll('[data-review-form] textarea[name="comment"]').forEach(function (textarea) {
    var counter = document.createElement('p');
    counter.className = 'text-xs text-[#8e919f] text-right';
    var updateCounter = function () {
        counter.textContent = textarea.value.trim().length + '/1000';
    };
    textarea.insertAdjacentElement('afterend', counter);
    textarea.addEventListener('input', updateCounter);
    updateCounter();
});

document.querySelectorAll('[data-review-form]').forEach(function (form) {
    var feedback = document.createElement('p');
    feedback.className = 'text-sm text-red-300';
    feedback.setAttribute('role', 'alert');
    feedback.hidden = true;
    form.appendChild(feedback);
    form.addEventListener('submit', function (event) {
        var comment = form.querySelector('textarea[name="comment"]');
        var length = comment.value.trim().length;
        var message = !form.querySelector('input[name="rating"]:checked') ? form.dataset.ratingMessage
            : length < 10 || length > 1000 ? form.dataset.commentMessage : '';
        feedback.textContent = message;
        feedback.hidden = !message;
        if (message) {
            event.preventDefault();
        }
    });
});

// Xác nhận trước khi xóa đánh giá (trang khách hàng không nạp manager-common.js)
document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();
        }
    });
});
