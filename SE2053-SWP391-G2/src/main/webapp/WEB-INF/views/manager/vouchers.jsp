<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Voucher &amp; khuyến mãi - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>
<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div role="status"
             class="fixed top-20 right-8 z-[60] flex items-center gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300' : 'bg-error-container/80 border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-xl">
        <div>
            <div class="flex items-center gap-space-sm mb-space-xs">
                <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
                <span class="text-outline text-body-sm">/</span>
                <span class="text-on-surface-variant text-body-sm">Voucher &amp; khuyến mãi</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Voucher &amp; khuyến mãi</h1>
        </div>
        <a href="${ctx}/manager/vouchers?add=1${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
           class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg whitespace-nowrap">
            <span class="material-symbols-outlined text-[20px]">add</span>
            <span>Tạo voucher</span>
        </a>
    </div>

    <section class="grid grid-cols-1 md:grid-cols-5 gap-space-md mb-space-xl" aria-label="Tổng quan voucher">
        <div class="bg-surface-container-low rounded-xl border border-emerald-400/10 p-space-lg">
            <span class="material-symbols-outlined text-emerald-300">verified</span>
            <p class="text-on-surface-variant mt-2">Voucher khuyến mãi còn hiệu lực</p>
            <p class="text-headline-lg text-emerald-300 mt-space-xs">${promotionActive}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-red-400/10 p-space-lg">
            <span class="material-symbols-outlined text-red-300">event_busy</span>
            <p class="text-on-surface-variant mt-2">Voucher khuyến mãi hết hạn</p>
            <p class="text-headline-lg mt-space-xs">${promotionExpired}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-sky-400/10 p-space-lg">
            <span class="material-symbols-outlined text-sky-300">schedule</span>
            <p class="text-on-surface-variant mt-2">Voucher sắp bắt đầu</p>
            <p class="text-headline-lg mt-space-xs">${promotionScheduled}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-primary/10 p-space-lg">
            <span class="material-symbols-outlined text-primary">sell</span>
            <p class="text-on-surface-variant mt-2">Voucher hoàn vé còn hiệu lực</p>
            <p class="text-headline-lg text-primary mt-space-xs">${refund80Active}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-orange-400/10 p-space-lg">
            <span class="material-symbols-outlined text-orange-300">receipt_long</span>
            <p class="text-on-surface-variant mt-2">Voucher hoàn vé hết hạn</p>
            <p class="text-headline-lg mt-space-xs">${refund80Expired}</p>
        </div>
    </section>

    <form method="get" action="${ctx}/manager/vouchers"
          class="flex flex-col md:flex-row gap-space-md mb-space-md">
        <input class="flex-1 bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none"
               type="search" name="q" value="<c:out value='${keyword}'/>" maxlength="32"
               placeholder="Tìm theo mã voucher"/>
        <select class="bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none"
                name="type" aria-label="Loại voucher">
            <option value="">Tất cả loại voucher</option>
            <c:forEach var="type" items="${voucherTypes}">
                <option value="${type}" ${typeFilter == type ? 'selected' : ''}>
                    ${type == 'Refund80' ? 'Hoàn vé' : 'Khuyến mãi'}
                </option>
            </c:forEach>
        </select>
        <button class="bg-surface-container-high hover:bg-surface-bright px-5 py-2 rounded-lg border border-white/10" type="submit">
            <span class="material-symbols-outlined align-middle mr-1 text-[18px]">search</span>Lọc
        </button>
    </form>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-5">Mã voucher</th>
                    <th class="py-4 px-5">Phân loại</th>
                    <th class="py-4 px-5">Phạm vi</th>
                    <th class="py-4 px-5">Mức giảm</th>
                    <th class="py-4 px-5">Đơn tối thiểu</th>
                    <th class="py-4 px-5">Đã dùng / đang giữ</th>
                    <th class="py-4 px-5">Lượt dùng/người</th>
                    <th class="py-4 px-5">Ngày tạo</th>
                    <th class="py-4 px-5">Bắt đầu</th>
                    <th class="py-4 px-5">Hạn sử dụng</th>
                    <th class="py-4 px-5">Trạng thái</th>
                    <th class="py-4 px-5 text-right">Thao tác</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md">
                <c:forEach var="voucher" items="${voucherPage.items}">
                    <tr class="hover:bg-surface-container transition-colors">
                        <td class="py-4 px-5 font-label-lg"><c:out value="${voucher.voucherCode}"/></td>
                        <td class="py-4 px-5">
                            <c:out value="${voucher.voucherType == 'Refund80' ? 'Hoàn vé' : 'Khuyến mãi'}"/>
                        </td>
                        <td class="py-4 px-5">
                            <c:choose>
                                <c:when test="${empty voucher.movieId}">Toàn bộ phim</c:when>
                                <c:otherwise><c:out value="${voucher.movieTitle}"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-5">
                            <c:choose>
                                <c:when test="${voucher.voucherType == 'Refund80'}">
                                    <c:out value="${voucher.discountValue}"/>đ
                                </c:when>
                                <c:when test="${voucher.discountType == 'Percent'}">
                                    <c:out value="${voucher.discountValue}"/>%
                                </c:when>
                                <c:otherwise><c:out value="${voucher.discountValue}"/>đ</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-5"><c:out value="${voucher.minOrderAmount}"/>đ</td>
                        <td class="py-4 px-5">
                            <c:choose>
                                <c:when test="${voucher.voucherType == 'Promotion'}">
                                    <c:out value="${voucher.usageCount}"/> / <c:out value="${voucher.usageLimit}"/>
                                </c:when>
                                <c:otherwise>Cá nhân</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-5">
                            <c:choose>
                                <c:when test="${voucher.voucherType == 'Promotion'}">
                                    <c:out value="${voucher.perUserUsageLimit}"/>
                                </c:when>
                                <c:otherwise>Cá nhân</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-5"><c:out value="${voucher.createdAt}"/></td>
                        <td class="py-4 px-5"><c:out value="${voucher.startDate}"/></td>
                        <td class="py-4 px-5"><c:out value="${voucher.expiryDate}"/></td>
                        <td class="py-4 px-5">
                            <c:choose>
                                <c:when test="${voucher.status == 'Active'}">
                                    <span class="inline-flex items-center rounded-full border border-emerald-400/30 bg-emerald-400/10 px-3 py-1 text-xs font-semibold text-emerald-300">Đang hiệu lực</span>
                                </c:when>
                                <c:when test="${voucher.status == 'Used'}">
                                    <span class="inline-flex items-center rounded-full border border-orange-400/30 bg-orange-400/10 px-3 py-1 text-xs font-semibold text-orange-300">Đã hết</span>
                                </c:when>
                                <c:when test="${voucher.status == 'Expired'}">
                                    <span class="inline-flex items-center rounded-full border border-red-400/30 bg-red-400/10 px-3 py-1 text-xs font-semibold text-red-300">Đã hết hạn</span>
                                </c:when>
                                <c:when test="${voucher.status == 'Scheduled'}">
                                    <span class="inline-flex items-center rounded-full border border-sky-400/30 bg-sky-400/10 px-3 py-1 text-xs font-semibold text-sky-300">Sắp bắt đầu</span>
                                </c:when>
                                <c:otherwise><span class="inline-flex rounded-full bg-white/10 px-3 py-1 text-xs text-on-surface-variant"><c:out value="${voucher.status}"/></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="py-4 px-5 text-right">
                            <c:if test="${voucher.voucherType == 'Promotion'}">
                                <div class="flex justify-end gap-2">
                                    <a href="${ctx}/manager/vouchers?edit=${voucher.voucherId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
                                       class="px-3 py-1.5 text-sm rounded-full bg-surface-container-high border border-white/10 hover:border-primary">Sửa</a>
                                    <form method="post" action="${ctx}/manager/vouchers${listQuery}" class="inline">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                        <input type="hidden" name="action" value="delete"/>
                                        <input type="hidden" name="voucherId" value="${voucher.voucherId}"/>
                                        <button type="submit" class="px-3 py-1.5 text-sm rounded-full bg-red-500/15 border border-red-500/30 text-red-300 hover:bg-red-500/25"
                                                onclick="return confirm('Xóa voucher ${fn:escapeXml(voucher.voucherCode)}?');">
                                            Xóa
                                        </button>
                                    </form>
                                </div>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty voucherPage.items}">
                    <tr><td colspan="12" class="py-12 px-6 text-center text-on-surface-variant">
                        <c:out value="${msgNoResult}"/>
                    </td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:url var="pageBase" value="/manager/vouchers">
            <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
            <c:if test="${not empty typeFilter}"><c:param name="type" value="${typeFilter}"/></c:if>
        </c:url>
        <ui:pagination pageData="${voucherPage}" baseUrl="${pageBase}" label="voucher"/>
    </div>
</div>
</main>

<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex justify-center p-4 overflow-y-auto">
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-4xl p-space-xl shadow-2xl relative my-auto">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/vouchers${listQuery}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <div class="mb-space-lg pr-10">
                <p class="text-label-md text-primary mb-1">${empty editingVoucherId ? 'Voucher mới' : 'Chỉnh sửa voucher'}</p>
                <h2 class="text-headline-md text-on-surface">${empty editingVoucherId ? 'Tạo voucher khuyến mãi' : 'Cập nhật voucher khuyến mãi'}</h2>
                <p class="text-body-sm text-on-surface-variant mt-1">Thiết lập phạm vi áp dụng, mức ưu đãi và giới hạn sử dụng.</p>
            </div>
            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>
            <form method="post" action="${ctx}/manager/vouchers${listQuery}" class="space-y-space-lg">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="voucherId" value="${editingVoucherId}"/>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="voucherCode">Mã voucher</label>
                    <input id="voucherCode" name="voucherCode" class="${inputClass} w-full" maxlength="32"
                           pattern="[A-Za-z0-9-]{4,32}" required
                           value="<c:out value='${form.voucherCode}'/>" placeholder="VD: CINEMA20"/>
                    <c:if test="${not empty errors.voucherCode}"><p class="form-error"><c:out value="${errors.voucherCode}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="createdAt">Ngày tạo</label>
                    <input id="createdAt" class="${inputClass} w-full opacity-70" type="text"
                           value="<c:out value='${voucherCreatedAt}'/>" readonly/>
                </div>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="scope">Phạm vi áp dụng</label>
                    <select id="scope" name="scope" class="${inputClass} w-full" required>
                        <option value="All" ${form.scope == 'All' || empty form.scope ? 'selected' : ''}>Toàn bộ phim</option>
                        <option value="Movie" ${form.scope == 'Movie' ? 'selected' : ''}>Một phim cụ thể</option>
                    </select>
                    <p class="mt-1 text-sm text-on-surface-variant">
                        Voucher theo phim áp dụng cho đơn của phim đã chọn, bao gồm vé và bắp nước.
                    </p>
                    <c:if test="${not empty errors.scope}"><p class="form-error"><c:out value="${errors.scope}"/></p></c:if>
                </div>
                <div id="movieScopeField" class="${form.scope == 'Movie' ? '' : 'hidden'}">
                    <label class="block text-label-md text-on-surface-variant mb-1" for="movieId">Phim áp dụng</label>
                    <select id="movieId" name="movieId" class="${inputClass} w-full">
                        <option value="">-- Chọn phim --</option>
                        <c:forEach var="movie" items="${movies}">
                            <option value="${movie.movieId}" ${form.movieId == movie.movieId ? 'selected' : ''}>
                                <c:out value="${movie.title}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.movieId}"><p class="form-error"><c:out value="${errors.movieId}"/></p></c:if>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="discountType">Loại giảm giá</label>
                        <select id="discountType" name="discountType" class="${inputClass} w-full" required>
                            <option value="Percent" ${form.discountType == 'Percent' ? 'selected' : ''}>Phần trăm</option>
                            <option value="Amount" ${form.discountType == 'Amount' ? 'selected' : ''}>Số tiền (VND)</option>
                        </select>
                        <c:if test="${not empty errors.discountType}"><p class="form-error"><c:out value="${errors.discountType}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="discountValue">Mức giảm</label>
                        <input id="discountValue" name="discountValue" class="${inputClass} w-full"
                               type="number" min="0.01" step="0.01" required
                               value="<c:out value='${form.discountValue}'/>"/>
                        <c:if test="${not empty errors.discountValue}"><p class="form-error"><c:out value="${errors.discountValue}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="minOrderAmount">Đơn tối thiểu (VND)</label>
                        <input id="minOrderAmount" name="minOrderAmount" class="${inputClass} w-full"
                               type="number" min="0" step="1" required
                               value="<c:out value='${form.minOrderAmount}'/>"/>
                        <c:if test="${not empty errors.minOrderAmount}"><p class="form-error"><c:out value="${errors.minOrderAmount}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="startDate">Bắt đầu hiệu lực</label>
                        <input id="startDate" name="startDate" class="${inputClass} w-full"
                               type="datetime-local" required value="<c:out value='${form.startDate}'/>"/>
                        <c:if test="${not empty errors.startDate}"><p class="form-error"><c:out value="${errors.startDate}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="expiryDate">Kết thúc hiệu lực</label>
                        <input id="expiryDate" name="expiryDate" class="${inputClass} w-full"
                               type="datetime-local" required value="<c:out value='${form.expiryDate}'/>"/>
                        <c:if test="${not empty errors.expiryDate}"><p class="form-error"><c:out value="${errors.expiryDate}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="usageLimit">Giới hạn tổng lượt dùng</label>
                        <input id="usageLimit" name="usageLimit" class="${inputClass} w-full"
                               type="number" min="1" step="1" required value="<c:out value='${form.usageLimit}'/>"/>
                        <c:if test="${not empty errors.usageLimit}"><p class="form-error"><c:out value="${errors.usageLimit}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="perUserUsageLimit">Giới hạn lượt dùng/người</label>
                        <input id="perUserUsageLimit" name="perUserUsageLimit" class="${inputClass} w-full"
                               type="number" min="1" step="1" required
                               value="<c:out value='${form.perUserUsageLimit}'/>"/>
                        <p class="mt-1 text-sm text-on-surface-variant">
                            Số lần tối đa mỗi tài khoản được dùng voucher này.
                        </p>
                        <c:if test="${not empty errors.perUserUsageLimit}"><p class="form-error"><c:out value="${errors.perUserUsageLimit}"/></p></c:if>
                    </div>
                </div>
                <div class="flex justify-end gap-space-sm pt-space-sm">
                    <a class="px-space-lg py-2 rounded-full bg-surface-container-high" href="${ctx}/manager/vouchers${listQuery}">Hủy</a>
                    <button class="px-space-lg py-2 rounded-full bg-primary text-on-primary" type="submit">${empty editingVoucherId ? 'Tạo voucher' : 'Cập nhật voucher'}</button>
                </div>
            </form>
        </div>
    </div>
</c:if>
<script>
    const voucherScope = document.getElementById('scope');
    if (voucherScope) {
        const movieScopeField = document.getElementById('movieScopeField');
        const movieSelect = document.getElementById('movieId');
        const updateMovieScope = () => {
            const appliesToMovie = voucherScope.value === 'Movie';
            movieScopeField.classList.toggle('hidden', !appliesToMovie);
            movieSelect.required = appliesToMovie;
        };
        voucherScope.addEventListener('change', updateMovieScope);
        updateMovieScope();
    }
</script>
</body>
</html>
