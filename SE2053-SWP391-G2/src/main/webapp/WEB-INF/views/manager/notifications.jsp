<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="w-full min-h-11 rounded-lg border border-outline bg-surface-container-high px-4 py-3 text-on-surface focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quản lý thông báo - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <style>
        .uc09-page main { overflow-wrap: anywhere; }
        .uc09-page main :is(button, a, select, input:not([type=radio])) { min-height: 44px; }
        .uc09-page main :is(button, a, input, textarea, select):focus-visible { outline: 2px solid #ffb4aa; outline-offset: 3px; }
        .uc09-page main button:disabled { opacity: .5; cursor: wait; }
        .uc09-history { width: 100%; table-layout: fixed; }
        .uc09-history :is(th, td) { padding: 12px 8px; text-align: left; vertical-align: top; }
        .uc09-pagination > div, .uc09-pagination > div > div:last-child { flex-wrap: wrap; gap: 8px; }
        .uc09-pagination a { display: inline-flex; align-items: center; justify-content: center; min-width: 44px; }
        @media (max-width: 1279px) {
            .uc09-history thead { display: none; }
            .uc09-history, .uc09-history tbody, .uc09-history tr, .uc09-history td { display: block; width: 100%; }
            .uc09-history tr { padding: 12px; }
            .uc09-history td { padding: 5px 0; }
            .uc09-history td::before { content: attr(data-label) ': '; font-weight: 600; }
        }
        @media (max-width: 767px) {
            .uc09-page > aside { display: none; }
            .uc09-page > header { left: 0; padding: 0 16px; }
            .uc09-page > header > span { display: none; }
            .uc09-page > header > div { width: 100%; justify-content: space-between; gap: 8px; }
        }
    </style>
</head>
<body class="uc09-page bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>
<main class="md:ml-64 pt-24 px-4 md:px-space-xl pb-16 space-y-6">
    <nav class="md:hidden flex flex-wrap gap-3 text-primary" aria-label="Điều hướng quản lý">
        <a class="inline-flex items-center" href="${ctx}/manager/dashboard">Trang chủ quản lý</a>
        <a class="inline-flex items-center" href="${ctx}/manager/inbox">Hộp thư của tôi</a>
    </nav>
    <div class="flex flex-wrap justify-between items-start gap-4">
        <div>
            <h1 class="text-headline-md font-bold">Quản lý thông báo</h1>
            <p class="mt-2 text-on-surface-variant">Soạn, xem trước và gửi thông báo đến các tài khoản đang hoạt động.</p>
        </div>
        <a href="${ctx}/manager/inbox" class="inline-flex items-center gap-2 rounded-lg border border-outline px-4 py-2 hover:bg-surface-container-high">
            <span class="material-symbols-outlined" aria-hidden="true">inbox</span>Hộp thư của tôi
        </a>
    </div>
    <c:if test="${not empty flashSuccess}"><p role="status" class="p-4 rounded-xl border border-emerald-400/40 bg-emerald-950 text-emerald-200"><c:out value="${flashSuccess}"/></p></c:if>
    <c:if test="${not empty flashError}"><p role="alert" class="p-4 rounded-xl bg-error-container text-on-error-container"><c:out value="${flashError}"/></p></c:if>
    <c:if test="${not empty errors}">
        <div id="error-summary" tabindex="-1" role="alert" class="rounded-xl bg-error-container text-on-error-container p-4">
            <h2 class="font-bold">Vui lòng kiểm tra lại thông báo</h2>
            <ul class="list-disc pl-5 mt-2">
                <c:forEach var="error" items="${errors}"><li><a href="#<c:out value='${error.key == "general" ? "compose-form" : error.key}'/>" class="underline"><c:out value="${error.value}"/></a></li></c:forEach>
            </ul>
        </div>
    </c:if>
    <section id="compose-section" class="rounded-xl bg-surface-container-low border border-white/10 p-5 md:p-6" ${not empty preview ? 'hidden' : ''}>
        <h2 class="text-headline-sm font-semibold mb-5">Soạn thông báo</h2>
        <form id="compose-form" action="${ctx}/manager/notifications" method="post" class="space-y-5" novalidate>
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
            <input type="hidden" name="action" value="preview"/>
            <input type="hidden" name="filter_target" value="<c:out value='${targetFilter}'/>"/>
            <input type="hidden" name="filter_type" value="<c:out value='${typeFilter}'/>"/>
            <input type="hidden" name="filter_keyword" value="<c:out value='${keyword}'/>"/>
            <input type="hidden" name="filter_page" value="${history.page}"/>
            <fieldset id="target" aria-describedby="target-error">
                <legend class="font-label-lg mb-2">Đối tượng nhận (bắt buộc)</legend>
                <div class="grid sm:grid-cols-2 gap-3">
                    <c:forEach var="target" items="${targets}">
                        <label class="min-h-11 flex items-center gap-3 rounded-lg border border-outline px-4 py-3 cursor-pointer hover:bg-surface-container-high">
                            <input type="radio" name="target" value="<c:out value='${target.key}'/>" ${form.target == target.key ? 'checked' : ''} aria-invalid="${not empty errors.target}"/>
                            <c:out value="${target.value}"/>
                        </label>
                    </c:forEach>
                </div>
                <p id="target-error" class="mt-2 text-error"><c:out value="${errors.target}"/></p>
            </fieldset>
            <div id="phone-field" ${form.target != 'SingleCustomer' ? 'hidden' : ''}>
                <label for="phone" class="block font-label-lg mb-2">Số điện thoại khách hàng (bắt buộc)</label>
                <input id="phone" type="tel" inputmode="numeric" autocomplete="off" name="phone" maxlength="10" value="<c:out value='${form.phone}'/>" class="${inputClass}" aria-invalid="${not empty errors.phone}" aria-describedby="phone-hint phone-error"/>
                <p id="phone-hint" class="mt-2 text-on-surface-variant text-sm">10 chữ số, bắt đầu bằng 0. Chỉ gửi đến khách hàng đang hoạt động.</p>
                <p id="phone-error" class="mt-2 text-error"><c:out value="${errors.phone}"/></p>
            </div>
            <div>
                <label for="type" class="block font-label-lg mb-2">Loại thông báo (bắt buộc)</label>
                <select id="type" name="type" class="${inputClass}" aria-invalid="${not empty errors.type}" aria-describedby="type-error">
                    <option value="">Chọn loại thông báo</option>
                    <c:forEach var="category" items="${categoryLabels}">
                        <c:set var="allowedTargets" value=""/>
                        <c:forEach var="entry" items="${categories}">
                            <c:if test="${entry.value.contains(category.key)}"><c:set var="allowedTargets" value="${allowedTargets} ${entry.key}"/></c:if>
                        </c:forEach>
                        <option value="<c:out value='${category.key}'/>" data-targets="<c:out value='${allowedTargets}'/>" ${form.type == category.key ? 'selected' : ''}><c:out value="${category.value}"/></option>
                    </c:forEach>
                </select>
                <p id="type-error" class="mt-2 text-error"><c:out value="${errors.type}"/></p>
            </div>
            <div>
                <label for="title" class="block font-label-lg mb-2">Tiêu đề (bắt buộc)</label>
                <input id="title" name="title" maxlength="150" value="<c:out value='${form.title}'/>" class="${inputClass}" aria-invalid="${not empty errors.title}" aria-describedby="title-count title-error"/>
                <p id="title-count" class="mt-2 text-sm text-on-surface-variant">0/150 ký tự</p>
                <p id="title-error" class="mt-2 text-error"><c:out value="${errors.title}"/></p>
            </div>
            <div>
                <label for="message" class="block font-label-lg mb-2">Nội dung (bắt buộc)</label>
                <textarea id="message" name="message" maxlength="1000" rows="6" class="${inputClass}" aria-invalid="${not empty errors.message}" aria-describedby="message-count message-error"><c:out value="${form.message}"/></textarea>
                <p id="message-count" class="mt-2 text-sm text-on-surface-variant">0/1000 ký tự</p>
                <p id="message-error" class="mt-2 text-error"><c:out value="${errors.message}"/></p>
            </div>
            <p class="text-sm text-on-surface-variant">Người gửi không nhận thông báo của chính mình. Danh sách người nhận được chốt khi xác nhận gửi.</p>
            <button class="rounded-lg bg-primary-container text-on-primary-container px-6 py-3 font-label-lg hover:brightness-110" type="submit">Xem trước</button>
        </form>
    </section>
    <c:if test="${not empty preview}">
        <section id="preview-section" tabindex="-1" class="rounded-xl border border-primary bg-surface-container-low p-5 md:p-6 space-y-5">
            <h2 class="text-headline-sm font-semibold">Xem trước thông báo</h2>
            <article class="rounded-xl border border-white/10 bg-surface-container-high p-5">
                <p class="text-sm text-on-surface-variant"><c:out value="${preview.typeLabel}"/></p>
                <h3 class="font-bold mt-2"><c:out value="${form.title}"/></h3>
                <p class="mt-3 whitespace-pre-line"><c:out value="${form.message}"/></p>
            </article>
            <p>Sẽ gửi đến <strong><c:out value="${preview.recipientCount}"/></strong> tài khoản · <c:out value="${preview.targetLabel}"/></p>
            <c:if test="${not empty preview.customerName}"><p>Khách hàng: <strong><c:out value="${preview.customerName}"/></strong> · <c:out value="${preview.maskedEmail}"/></p></c:if>
            <p class="text-sm text-on-surface-variant">Số người nhận sẽ được kiểm tra lại khi gửi.</p>
            <form id="send-form" action="${ctx}/manager/notifications" method="post" class="flex flex-wrap gap-3">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                <input type="hidden" name="action" value="send"/>
                <input type="hidden" name="target" value="<c:out value='${form.target}'/>"/>
                <input type="hidden" name="phone" value="<c:out value='${form.phone}'/>"/>
                <input type="hidden" name="type" value="<c:out value='${form.type}'/>"/>
                <input type="hidden" name="title" value="<c:out value='${form.title}'/>"/>
                <textarea name="message" hidden><c:out value="${form.message}"/></textarea>
                <input type="hidden" name="filter_target" value="<c:out value='${targetFilter}'/>"/>
                <input type="hidden" name="filter_type" value="<c:out value='${typeFilter}'/>"/>
                <input type="hidden" name="filter_keyword" value="<c:out value='${keyword}'/>"/>
                <input type="hidden" name="filter_page" value="${history.page}"/>
                <button id="send-button" type="submit" class="rounded-lg bg-primary-container text-on-primary-container px-6 py-3 font-label-lg hover:brightness-110">Xác nhận gửi</button>
                <button id="edit-button" type="button" class="rounded-lg border border-outline px-6 py-3 hover:bg-surface-container-high">Sửa lại</button>
            </form>
        </section>
    </c:if>
    <section class="rounded-xl bg-surface-container-low border border-white/10 p-5 md:p-6">
        <h2 class="text-headline-sm font-semibold mb-5">Lịch sử gửi</h2>
        <form action="${ctx}/manager/notifications" method="get" class="grid sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
            <div>
                <label for="filter-target" class="block mb-2 font-label-lg">Đối tượng</label>
                <select id="filter-target" name="target" class="${inputClass}">
                    <option value="">Tất cả đối tượng</option>
                    <c:forEach var="target" items="${targets}"><option value="<c:out value='${target.key}'/>" ${targetFilter == target.key ? 'selected' : ''}><c:out value="${target.value}"/></option></c:forEach>
                </select>
            </div>
            <div>
                <label for="filter-type" class="block mb-2 font-label-lg">Loại thông báo</label>
                <select id="filter-type" name="type" class="${inputClass}">
                    <option value="">Tất cả loại</option>
                    <c:forEach var="category" items="${categoryLabels}"><option value="<c:out value='${category.key}'/>" ${typeFilter == category.key ? 'selected' : ''}><c:out value="${category.value}"/></option></c:forEach>
                </select>
            </div>
            <div>
                <label for="filter-keyword" class="block mb-2 font-label-lg">Từ khóa tiêu đề</label>
                <input id="filter-keyword" name="keyword" value="<c:out value='${keyword}'/>" class="${inputClass}"/>
            </div>
            <div class="flex items-end gap-3 flex-wrap">
                <button class="rounded-lg border border-outline px-5 py-3 hover:bg-surface-container-high" type="submit">Lọc lịch sử</button>
                <a href="${ctx}/manager/notifications" class="inline-flex items-center text-primary underline">Xóa lọc</a>
            </div>
        </form>
        <c:choose>
            <c:when test="${empty history.items}"><p class="py-10 text-center text-on-surface-variant">Chưa có thông báo nào khớp với bộ lọc.</p></c:when>
            <c:otherwise>
                <table class="uc09-history text-sm">
                    <caption class="sr-only">Lịch sử thông báo và thống kê người nhận</caption>
                    <colgroup><col style="width:13%"/><col style="width:18%"/><col style="width:11%"/><col style="width:18%"/><col style="width:12%"/><col style="width:8%"/><col style="width:10%"/><col style="width:10%"/></colgroup>
                    <thead class="text-on-surface-variant"><tr><th scope="col">Thời gian</th><th scope="col">Tiêu đề</th><th scope="col">Loại</th><th scope="col">Đối tượng</th><th scope="col">Người gửi</th><th scope="col">Đã đọc</th><th scope="col">Trạng thái</th><th scope="col">Hành động</th></tr></thead>
                    <tbody>
                        <c:forEach var="item" items="${history.items}">
                            <tr class="border-t border-white/10">
                                <td data-label="Thời gian"><time datetime="<c:out value='${item.createdAt}'/>"><c:out value="${item.createdAt.format(DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm'))}"/></time></td>
                                <td data-label="Tiêu đề" class="font-semibold"><c:out value="${item.title}"/></td>
                                <td data-label="Loại"><c:out value="${categoryLabels[item.notificationType]}"/></td>
                                <td data-label="Đối tượng"><c:out value="${targets[item.targetType]}"/>
                                    <c:if test="${item.targetType == 'SingleCustomer'}"><div class="mt-1"><c:out value="${item.targetUserName}"/><br/><c:out value="${item.targetUserPhone}"/></div></c:if>
                                </td>
                                <td data-label="Người gửi"><c:out value="${item.senderName}"/></td>
                                <td data-label="Đã đọc">
                                    <c:choose>
                                        <c:when test="${item.recalled}"><span aria-label="Không áp dụng">—</span></c:when>
                                        <c:otherwise><c:out value="${item.readCount}"/>/<c:out value="${item.recipientCount}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td data-label="Trạng thái"><c:out value="${item.recalled ? 'Đã thu hồi' : 'Đã gửi'}"/></td>
                                <td data-label="Hành động">
                                    <c:if test="${not item.recalled}">
                                        <form class="recall-form" action="${ctx}/manager/notifications" method="post">
                                            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                                            <input type="hidden" name="action" value="recall"/>
                                            <input type="hidden" name="id" value="${item.announcementId}"/>
                                            <input type="hidden" name="filter_target" value="<c:out value='${targetFilter}'/>"/>
                                            <input type="hidden" name="filter_type" value="<c:out value='${typeFilter}'/>"/>
                                            <input type="hidden" name="filter_keyword" value="<c:out value='${keyword}'/>"/>
                                            <input type="hidden" name="filter_page" value="${history.page}"/>
                                            <button type="submit" class="text-error rounded-lg border border-error/60 px-3 py-2 hover:bg-error-container">Thu hồi</button>
                                        </form>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
        <div class="uc09-pagination mt-5"><ui:pagination pageData="${history}" baseUrl="${historyUrl}" label="thông báo"/></div>
    </section>
</main>
<script>
(() => {
    const compose = document.getElementById('compose-section');
    const type = document.getElementById('type');
    function updateTarget() {
        const target = document.querySelector('#compose-form input[name="target"]:checked')?.value || '';
        document.getElementById('phone-field').hidden = target !== 'SingleCustomer';
        for (const option of type.options) {
            if (!option.value) continue;
            const allowed = option.dataset.targets.trim().split(/\s+/).includes(target);
            option.hidden = !allowed;
            option.disabled = !allowed;
        }
        if (type.selectedOptions[0]?.disabled) type.value = '';
    }
    document.querySelectorAll('#compose-form input[name="target"]').forEach(radio => radio.addEventListener('change', updateTarget));
    updateTarget();
    for (const [id, limit] of [['title', 150], ['message', 1000]]) {
        const input = document.getElementById(id);
        const update = () => { document.getElementById(id + '-count').textContent = input.value.length + '/' + limit + ' ký tự'; };
        input.addEventListener('input', update);
        update();
    }
    document.getElementById('edit-button')?.addEventListener('click', () => {
        document.getElementById('preview-section').hidden = true;
        compose.hidden = false;
        document.getElementById('title').focus();
    });
    document.getElementById('send-form')?.addEventListener('submit', () => {
        const button = document.getElementById('send-button');
        button.disabled = true;
        button.textContent = 'Đang gửi…';
    });
    window.addEventListener('pageshow', () => {
        const button = document.getElementById('send-button');
        if (button) { button.disabled = false; button.textContent = 'Xác nhận gửi'; }
    });
    document.querySelectorAll('.recall-form').forEach(form => form.addEventListener('submit', event => {
        if (!confirm('Thu hồi sẽ xóa thông báo khỏi mọi hộp thư và không thể hoàn tác. Tiếp tục?')) event.preventDefault();
        else form.querySelector('button').disabled = true;
    }));
    const summary = document.getElementById('error-summary');
    if (summary) summary.focus();
    else document.getElementById('preview-section')?.focus();
})();
</script>
</body>
</html>
