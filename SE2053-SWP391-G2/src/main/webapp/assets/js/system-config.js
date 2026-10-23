// UC-33: đánh dấu ô đã sửa so với giá trị đang lưu và tự ẩn thông báo thành công.
(function () {
    function markChanged(input) {
        var changed = input.value.trim() !== input.dataset.stored;
        input.classList.toggle('text-[#ffb95f]', changed);
        input.classList.toggle('font-semibold', changed);
    }

    document.querySelectorAll('.config-input').forEach(function (input) {
        markChanged(input);
        input.addEventListener('input', function () {
            markChanged(input);
        });
    });

    document.querySelectorAll('.flash-alert').forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = 'opacity 0.5s ease';
            alert.style.opacity = '0';
            setTimeout(function () {
                alert.remove();
            }, 500);
        }, 5000);
    });
})();
