// UC-05: gợi ý phim khi gõ tên vào ô tìm kiếm.
// Gắn vào <input data-movie-suggest="URL API gợi ý"> nằm trong một khung position: relative, bên trong <form>.
(function () {
    'use strict';

    var DEBOUNCE_MS = 200;
    var STATUS_LABELS = {'Now Showing': 'Đang chiếu', 'Coming Soon': 'Sắp chiếu'};

    // Bỏ dấu + chữ thường từng ký tự (đ coi như d, giống collation CI_AI của server),
    // kèm vị trí ký tự gốc để tô đậm đúng phần khớp trên tên có dấu
    function fold(text) {
        var folded = '';
        var origin = [];
        for (var i = 0; i < text.length; i++) {
            var ch = text[i].normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace('đ', 'd');
            for (var j = 0; j < ch.length; j++) {
                folded += ch[j];
                origin.push(i);
            }
        }
        return {text: folded, origin: origin};
    }

    function appendHighlighted(parent, title, keyword) {
        var t = fold(title.normalize('NFC'));
        var k = fold(keyword.normalize('NFC')).text;
        var pos = k ? t.text.indexOf(k) : -1;
        if (pos < 0) {
            parent.appendChild(document.createTextNode(title));
            return;
        }
        var start = t.origin[pos];
        var end = t.origin[pos + k.length - 1] + 1;
        var mark = document.createElement('mark');
        mark.textContent = title.slice(start, end);
        parent.appendChild(document.createTextNode(title.slice(0, start)));
        parent.appendChild(mark);
        parent.appendChild(document.createTextNode(title.slice(end)));
    }

    function setup(input) {
        var url = input.getAttribute('data-movie-suggest');
        var detailUrl = input.getAttribute('data-movie-detail');
        var form = input.form;
        var list = document.createElement('ul');
        var items = [];
        var active = -1;
        var timer = null;
        var lastQuery = null;
        var controller = null;

        list.className = 'suggest-list';
        list.id = (input.id || 'movie-search') + '-suggest';
        list.setAttribute('role', 'listbox');
        list.hidden = true;
        input.parentElement.appendChild(list);
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-controls', list.id);
        input.setAttribute('aria-expanded', 'false');

        function close() {
            list.hidden = true;
            active = -1;
            input.setAttribute('aria-expanded', 'false');
            input.removeAttribute('aria-activedescendant');
        }

        function highlight(index) {
            items.forEach(function (li, i) {
                li.classList.toggle('active', i === index);
                li.setAttribute('aria-selected', i === index ? 'true' : 'false');
            });
            active = index;
            if (index >= 0) {
                input.setAttribute('aria-activedescendant', items[index].id);
                items[index].scrollIntoView({block: 'nearest'});
            } else {
                input.removeAttribute('aria-activedescendant');
            }
        }

        // Chọn gợi ý: mở trang chi tiết phim (UC-06) nếu có data-movie-detail, không thì tìm theo đúng tên phim
        function choose(movie) {
            input.value = movie.title;
            close();
            if (detailUrl) {
                window.location.href = detailUrl + encodeURIComponent(movie.id);
            } else {
                form.submit();
            }
        }

        function render(movies, keyword) {
            list.textContent = '';
            items = [];
            active = -1;
            if (movies.length === 0) {
                var empty = document.createElement('li');
                empty.className = 'suggest-empty';
                empty.textContent = 'Không có phim nào khớp với "' + keyword + '"';
                list.appendChild(empty);
            }
            movies.forEach(function (movie, i) {
                var li = document.createElement('li');
                li.id = list.id + '-' + i;
                li.className = 'suggest-item';
                li.setAttribute('role', 'option');

                var thumb = document.createElement('div');
                thumb.className = 'suggest-thumb';
                if (movie.poster) {
                    var img = document.createElement('img');
                    img.src = movie.poster;
                    img.alt = '';
                    img.onerror = function () { img.remove(); };
                    thumb.appendChild(img);
                }

                var info = document.createElement('div');
                info.className = 'suggest-info';
                var title = document.createElement('div');
                title.className = 'suggest-title';
                appendHighlighted(title, movie.title, keyword);
                var meta = document.createElement('div');
                meta.className = 'suggest-meta';
                meta.textContent = [movie.ageRating, STATUS_LABELS[movie.status], movie.year]
                    .filter(Boolean).join(' • ');
                info.appendChild(title);
                info.appendChild(meta);

                li.appendChild(thumb);
                li.appendChild(info);
                // mousedown thay cho click để không bị blur của ô nhập đóng danh sách trước
                li.addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    choose(movie);
                });
                li.addEventListener('mousemove', function () {
                    if (active !== i) {
                        highlight(i);
                    }
                });
                list.appendChild(li);
                items.push(li);
            });
            list.hidden = false;
            input.setAttribute('aria-expanded', 'true');
        }

        function load(keyword) {
            if (controller) {
                controller.abort();
            }
            controller = new AbortController();
            fetch(url + '?q=' + encodeURIComponent(keyword), {signal: controller.signal})
                .then(function (res) {
                    if (!res.ok) {
                        throw new Error('HTTP ' + res.status);
                    }
                    return res.json();
                })
                .then(function (movies) {
                    // Bỏ kết quả cũ nếu người dùng đã gõ tiếp
                    if (input.value.trim() === keyword) {
                        render(movies, keyword);
                    }
                })
                .catch(function (err) {
                    if (err.name !== 'AbortError') {
                        close();
                    }
                });
        }

        input.addEventListener('input', function () {
            var keyword = input.value.trim();
            clearTimeout(timer);
            if (!keyword) {
                lastQuery = null;
                close();
                return;
            }
            if (keyword === lastQuery) {
                return;
            }
            timer = setTimeout(function () {
                lastQuery = keyword;
                load(keyword);
            }, DEBOUNCE_MS);
        });

        input.addEventListener('keydown', function (e) {
            if (list.hidden || items.length === 0) {
                if (e.key === 'Escape') {
                    close();
                }
                return;
            }
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                highlight((active + 1) % items.length);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                highlight(active <= 0 ? items.length - 1 : active - 1);
            } else if (e.key === 'Enter' && active >= 0) {
                e.preventDefault();
                items[active].dispatchEvent(new MouseEvent('mousedown', {cancelable: true}));
            } else if (e.key === 'Escape') {
                close();
            }
        });

        input.addEventListener('focus', function () {
            if (input.value.trim() && list.childNodes.length > 0 && input.value.trim() === lastQuery) {
                list.hidden = false;
                input.setAttribute('aria-expanded', 'true');
            }
        });
        input.addEventListener('blur', close);
    }

    document.querySelectorAll('input[data-movie-suggest]').forEach(setup);
})();
