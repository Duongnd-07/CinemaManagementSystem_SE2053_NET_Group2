document.querySelectorAll('select[data-auto-submit]').forEach(function (select) {
    select.addEventListener('change', function () {
        select.form.submit();
    });
});

document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();
        }
    });
});

// UC-23: poster chỉ chọn từ ảnh trong máy, ảnh xem trước luôn là ảnh sẽ được lưu.
// Chọn file -> xem trước file; bỏ file -> quay lại poster hiện tại (ô ẩn posterUrl); "Bỏ poster" -> phim không có poster.
var posterPicker = document.querySelector('[data-poster-picker]');
if (posterPicker) {
    var MAX_POSTER_BYTES = 5 * 1024 * 1024;
    var posterFile = document.getElementById('posterFile');
    var posterUrl = document.getElementById('posterUrl');
    var posterPreview = document.getElementById('posterPreview');
    var posterPlaceholder = document.getElementById('posterPlaceholder');
    var posterSource = document.getElementById('posterSource');
    var posterClear = document.getElementById('posterClear');
    var initialPosterUrl = posterPicker.dataset.initialUrl;
    var initialPosterSrc = posterPicker.dataset.initialSrc;
    var posterObjectUrl = null;

    // Chặn file poster > 5MB ngay trên trình duyệt (server từ chối cả request nếu vượt giới hạn)
    var posterError = document.createElement('p');
    posterError.className = 'form-error';
    posterError.hidden = true;
    posterError.textContent = 'Poster vượt quá dung lượng tối đa 5MB.';
    posterFile.insertAdjacentElement('afterend', posterError);

    var showPoster = function (src, text) {
        posterPreview.hidden = !src;
        posterPlaceholder.hidden = !!src;
        if (src) {
            posterPreview.src = src;
        } else {
            posterPreview.removeAttribute('src');
        }
        posterSource.textContent = text;
    };

    var refreshPoster = function () {
        if (posterObjectUrl) {
            URL.revokeObjectURL(posterObjectUrl);
            posterObjectUrl = null;
        }
        var file = posterFile.files[0];
        if (file) {
            posterObjectUrl = URL.createObjectURL(file);
            showPoster(posterObjectUrl, 'Dùng file: ' + file.name);
            posterClear.textContent = 'Bỏ file đã chọn';
            posterClear.hidden = false;
            return;
        }
        var value = posterUrl.value.trim();
        posterClear.textContent = 'Bỏ poster';
        posterClear.hidden = !value;
        if (value && value === initialPosterUrl && initialPosterSrc) {
            showPoster(initialPosterSrc, 'Đang dùng poster hiện tại');
        } else {
            showPoster('', 'Chưa có poster');
        }
    };

    posterPreview.addEventListener('error', function () {
        if (posterPreview.getAttribute('src')) {
            showPoster('', 'Không tải được ảnh poster');
        }
    });
    posterFile.addEventListener('change', function () {
        var file = posterFile.files[0];
        var tooLarge = !!file && file.size > MAX_POSTER_BYTES;
        posterError.hidden = !tooLarge;
        if (tooLarge) {
            posterFile.value = '';
        }
        refreshPoster();
    });
    posterClear.addEventListener('click', function () {
        // Đang chọn file thì chỉ bỏ file (quay về poster hiện tại); không thì xóa poster hiện tại -> phim không có poster
        if (posterFile.files.length) {
            posterFile.value = '';
        } else {
            posterUrl.value = '';
        }
        posterError.hidden = true;
        refreshPoster();
    });
    refreshPoster();
}

var toast = document.getElementById('toast');
if (toast) {
    setTimeout(function () {
        toast.remove();
    }, 4000);
}

document.addEventListener('keydown', function (event) {
    var closeLink = document.querySelector('[data-modal] a[title="Đóng"]');
    if (event.key === 'Escape' && closeLink) {
        window.location.href = closeLink.href;
    }
});
