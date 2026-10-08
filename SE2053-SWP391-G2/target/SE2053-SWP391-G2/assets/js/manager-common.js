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

// Chặn file poster > 5MB ngay trên trình duyệt (server từ chối cả request nếu vượt giới hạn)
var posterFile = document.getElementById('posterFile');
if (posterFile) {
    var MAX_POSTER_BYTES = 5 * 1024 * 1024;
    var posterError = document.createElement('p');
    posterError.className = 'form-error';
    posterError.hidden = true;
    posterError.textContent = 'Poster vượt quá dung lượng tối đa 5MB.';
    posterFile.insertAdjacentElement('afterend', posterError);
    posterFile.addEventListener('change', function () {
        var file = posterFile.files[0];
        var tooLarge = !!file && file.size > MAX_POSTER_BYTES;
        posterError.hidden = !tooLarge;
        if (tooLarge) {
            posterFile.value = '';
        }
    });
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
