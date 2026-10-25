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
            <span>Tạo campaign</span>
        </a>
    </div>

    <section class="grid grid-cols-1 md:grid-cols-5 gap-space-md mb-space-xl" aria-label="Tổng quan voucher">
        <div class="bg-surface-container-low rounded-xl border border-white/5 p-space-lg">
            <p class="text-on-surface-variant">Voucher khuyến mãi còn hiệu lực</p>
            <p class="text-headline-lg text-primary mt-space-xs">${promotionActive}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-white/5 p-space-lg">
            <p class="text-on-surface-variant">Voucher khuyến mãi hết hạn</p>
            <p class="text-headline-lg mt-space-xs">${promotionExpired}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-white/5 p-space-lg">
            <p class="text-on-surface-variant">Campaign sắp bắt đầu</p>
            <p class="text-headline-lg mt-space-xs">${promotionScheduled}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-white/5 p-space-lg">
            <p class="text-on-surface-variant">Voucher hoàn vé còn hiệu lực</p>
            <p class="text-headline-lg text-primary mt-space-xs">${refund80Active}</p>
        </div>
        <div class="bg-surface-container-low rounded-xl border border-white/5 p-space-lg">
            <p class="text-on-surface-variant">Voucher hoàn vé hết hạn</p>
            <p class="text-headline-lg mt-space-xs">${refund80Expired}</p>
        </div>
    </section>

    <form method="get" action="${ctx}/manager/vouchers"
          class="flex flex-col md:flex-row gap-space-md mb-space-md">
        <input class="flex-1 bg-surface-container-high text-on-surface px-4 py-2 rounded-md border border-white/10"
               type="search" name="q" value="<c:out value='${keyword}'/>" maxlength="32"
               placeholder="Tìm theo mã voucher"/>
        <select class="bg-surface-container-high text-on-surface px-4 py-2 rounded-md border border-white/10"
                name="type" aria-label="Loại voucher">
            <option value="">Tất cả loại voucher</option>
            <c:forEach var="type" items="${voucherTypes}">
                <option value="${type}" ${typeFilter == type ? 'selected' : ''}>
                    ${type == 'Refund80' ? 'Hoàn vé' : 'Khuyến mãi'}
                </option>
            </c:forEach>
        </select>
        <button class="bg-surface-container-high hover:bg-surface-bright px-5 py-2 rounded-md" type="submit">Lọc</button>
    </form>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-5">Mã voucher</th>
                    <th class="py-4 px-5">Phân loại</th>
                    <th class="py-4 px-5">Mức giảm</th>
                    <th class="py-4 px-5">Đơn tối thiểu</th>
                    <th class="py-4 px-5">Đã dùng / đang giữ</th>
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
                        <td class="py-4 px-5"><c:out value="${voucher.createdAt}"/></td>
                        <td class="py-4 px-5"><c:out value="${voucher.startDate}"/></td>
                        <td class="py-4 px-5"><c:out value="${voucher.expiryDate}"/></td>
                        <td class="py-4 px-5"><c:out value="${voucher.status}"/></td>
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
                    <tr><td colspan="10" class="py-12 px-6 text-center text-on-surface-variant">
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
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto">
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/vouchers${listQuery}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h2 class="text-headline-md text-on-surface mb-space-lg">${empty editingVoucherId ? 'Tạo campaign khuyến mãi' : 'Cập nhật campaign khuyến mãi'}</h2>
            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>
            <form method="post" action="${ctx}/manager/vouchers${listQuery}" class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="voucherId" value="${editingVoucherId}"/>
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
                        <label class="block text-label-md text-on-surface-variant mb-1" for="usageLimit">Giới hạn lượt dùng</label>
                        <input id="usageLimit" name="usageLimit" class="${inputClass} w-full"
                               type="number" min="1" step="1" required value="<c:out value='${form.usageLimit}'/>"/>
                        <c:if test="${not empty errors.usageLimit}"><p class="form-error"><c:out value="${errors.usageLimit}"/></p></c:if>
                    </div>
                </div>
                <div class="flex justify-end gap-space-sm pt-space-sm">
                    <a class="px-space-lg py-2 rounded-full bg-surface-container-high" href="${ctx}/manager/vouchers${listQuery}">Hủy</a>
                    <button class="px-space-lg py-2 rounded-full bg-primary text-on-primary" type="submit">${empty editingVoucherId ? 'Tạo campaign' : 'Cập nhật campaign'}</button>
                </div>
            </form>
        </div>
    </div>
</c:if>
</body>
</html>
