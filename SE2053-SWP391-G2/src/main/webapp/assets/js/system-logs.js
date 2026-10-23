// UC-32 A1: bấm (hoặc Enter) vào một dòng để xem chi tiết; đóng hộp thoại thì quay lại danh sách với bộ lọc cũ.
(function () {
    var dialog = document.getElementById('logDetail');
    if (!dialog) {
        return;
    }

    function open(row) {
        dialog.querySelectorAll('[data-field]').forEach(function (cell) {
            var value = row.dataset[cell.dataset.field];
            // textContent để không chèn HTML từ nội dung log
            cell.textContent = value && value.trim() !== '' ? value : '—';
        });
        dialog.showModal();
    }

    document.querySelectorAll('.log-row').forEach(function (row) {
        row.addEventListener('click', function () {
            open(row);
        });
        row.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                open(row);
            }
        });
    });

    dialog.querySelectorAll('[data-close]').forEach(function (button) {
        button.addEventListener('click', function () {
            dialog.close();
        });
    });

    // Bấm ra ngoài hộp thoại cũng đóng
    dialog.addEventListener('click', function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });
})();
