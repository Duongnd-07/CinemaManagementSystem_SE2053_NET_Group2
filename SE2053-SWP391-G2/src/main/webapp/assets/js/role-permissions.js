// UC-31: đánh dấu ô đã đổi so với quyền đang lưu và tự ẩn thông báo thành công.
(function () {
    var counter = document.getElementById('changeCount');

    function refresh() {
        var changed = 0;
        document.querySelectorAll('.permission-input').forEach(function (input) {
            var isChanged = input.checked !== (input.dataset.stored === 'true');
            input.closest('td').classList.toggle('bg-amber-500/10', isChanged);
            if (isChanged) {
                changed++;
            }
        });
        counter.textContent = changed > 0 ? changed + ' thay đổi chưa lưu' : '';
    }

    document.querySelectorAll('.permission-input').forEach(function (input) {
        input.addEventListener('change', refresh);
    });
    refresh();

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
