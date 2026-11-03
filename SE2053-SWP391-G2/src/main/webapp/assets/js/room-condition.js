// UC-35: chỉ hiện ô mô tả khi hạng mục được chọn "Không đạt".
(function () {
    document.querySelectorAll('[data-check-item]').forEach(function (item) {
        var note = item.querySelector('[data-item-note]');

        function refresh() {
            var failed = item.querySelector('input[type="radio"][value="FAIL"]').checked;
            note.hidden = !failed;
        }

        item.querySelectorAll('input[type="radio"]').forEach(function (radio) {
            radio.addEventListener('change', refresh);
        });
        refresh();
    });
})();
