<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="statusLabels" value="${{'Active':'Đang bán','Out of Stock':'Hết hàng','Hidden':'Đã ẩn'}}"/>
<c:set var="statusPills" value="${{'Active':'cl-pill--success','Out of Stock':'cl-pill--warning','Hidden':'cl-pill--muted'}}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quản lý bắp nước - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
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
                <span class="text-on-surface-variant text-body-sm">Đồ ăn &amp; Thức uống</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Quản lý Bắp nước</h1>
        </div>
        <div class="flex items-center gap-space-md">
            <form method="get" action="${ctx}/manager/concessions" class="relative w-72">
                <c:if test="${not empty statusFilter}"><input type="hidden" name="status" value="${fn:escapeXml(statusFilter)}"/></c:if>
                <span class="material-symbols-outlined absolute left-3 top-2.5 text-outline text-[20px]">search</span>
                <input class="w-full bg-surface-container-high text-on-surface text-body-md pl-10 pr-4 py-2 rounded-md outline-none focus:ring-2 focus:ring-primary/40 border border-white/10"
                       type="text" name="q" value="<c:out value='${keyword}'/>" maxlength="100"
                       placeholder="Tìm theo tên món..."/>
            </form>
            <a href="${ctx}/manager/concessions?add=1${fn:escapeXml(fn:replace(listQuery, '?', '&'))}"
               class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap">
                <span class="material-symbols-outlined text-[20px]">add</span>
                <span>Thêm món mới</span>
            </a>
        </div>
    </div>

    <%-- Bộ lọc trạng thái --%>
    <div class="flex flex-wrap items-center gap-space-sm bg-surface-container-low p-space-md rounded-xl mb-space-lg">
        <c:set var="tabOn" value="bg-primary-container text-on-primary-container shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]"/>
        <c:set var="tabOff" value="bg-surface-container-high text-on-surface-variant hover:text-on-surface"/>
        <c:url var="allTabUrl" value="/manager/concessions">
            <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
        </c:url>
        <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${empty statusFilter ? tabOn : tabOff}"
           href="${fn:escapeXml(allTabUrl)}">Tất cả (${concessionPage.allCount})</a>
        <c:forEach var="st" items="${statuses}">
            <c:set var="cnt" value="${concessionPage.statusCounts[st]}"/>
            <c:url var="tabUrl" value="/manager/concessions">
                <c:param name="status" value="${st}"/>
                <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
            </c:url>
            <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${statusFilter == st ? tabOn : tabOff}"
               href="${fn:escapeXml(tabUrl)}">
                ${statusLabels[st]} (${not empty cnt ? cnt : 0})
            </a>
        </c:forEach>
    </div>

    <%-- Lưới card món ăn --%>
    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-space-lg">
        <c:forEach var="item" items="${concessionPage.concessions}">
            <div class="cl-card overflow-hidden flex flex-col ${item.status == 'Active' ? '' : 'cl-card--dimmed'}">
                <div class="cl-thumb">
                    <c:choose>
                        <c:when test="${not empty item.imageUrl}">
                            <img alt="" loading="lazy" src="<c:out value='${item.imageUrl}'/>"/>
                        </c:when>
                        <c:otherwise>
                            <span class="material-symbols-outlined text-[56px]">fastfood</span>
                        </c:otherwise>
                    </c:choose>
                    <span class="cl-pill ${statusPills[item.status]} absolute top-3 left-3"><c:out value="${statusLabels[item.status]}"/></span>
                </div>
                <div class="p-space-md flex flex-col gap-space-xs flex-1">
                    <div class="font-headline-sm text-on-surface"><c:out value="${item.name}"/></div>
                    <p class="text-body-sm text-on-surface-variant line-clamp-2 min-h-[40px]"><c:out value="${item.description}"/></p>
                    <div class="flex items-end justify-between mt-auto pt-space-sm">
                        <span class="cl-price"><fmt:formatNumber value="${item.price}" maxFractionDigits="0"/>đ</span>
                        <span class="text-body-sm ${item.stockQuantity == 0 ? 'text-amber-400' : 'text-on-surface-variant'}">Tồn kho: ${item.stockQuantity}</span>
                    </div>
                    <div class="flex items-center gap-2 pt-space-sm mt-space-sm border-t border-white/5">
                        <a class="flex-1 flex items-center justify-center gap-1 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg transition-all"
                           href="${ctx}/manager/concessions?edit=${item.concessionId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}">
                            <span class="material-symbols-outlined text-[18px]">edit</span>Sửa
                        </a>
                        <form method="post" action="${ctx}/manager/concessions${listQuery}" class="flex-1"
                              data-confirm="Ẩn món &quot;${fn:escapeXml(item.name)}&quot; khỏi danh sách bán?">
                            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                            <input type="hidden" name="action" value="hide"/>
                            <input type="hidden" name="concessionId" value="${item.concessionId}"/>
                            <button type="submit" ${item.status == 'Hidden' ? 'disabled' : ''}
                                    class="w-full flex items-center justify-center gap-1 py-2 rounded-lg bg-error/10 hover:bg-error/20 text-error font-label-lg transition-all disabled:opacity-30 disabled:cursor-not-allowed">
                                <span class="material-symbols-outlined text-[18px]">visibility_off</span>Ẩn
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
    <c:if test="${empty concessionPage.concessions}">
        <div class="cl-card py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></div>
    </c:if>

    <div class="flex items-center justify-between mt-space-lg text-body-sm text-on-surface-variant">
        <div>Hiển thị ${concessionPage.fromItem}-${concessionPage.toItem} trong tổng số ${concessionPage.totalItems} món</div>
        <div class="flex items-center gap-2">
            <c:url var="pageBase" value="/manager/concessions">
                <c:if test="${not empty keyword}"><c:param name="q" value="${keyword}"/></c:if>
                <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
            </c:url>
            <c:set var="sep" value="${fn:contains(pageBase, '?') ? '&' : '?'}"/>
            <c:choose>
                <c:when test="${concessionPage.page > 1}">
                    <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${fn:escapeXml(pageBase)}${sep}page=${concessionPage.page - 1}">Trước</a>
                </c:when>
                <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Trước</span></c:otherwise>
            </c:choose>
            <c:forEach var="p" begin="1" end="${concessionPage.totalPages}">
                <a class="px-3 py-1.5 rounded ${p == concessionPage.page ? 'bg-primary text-on-primary font-bold' : 'bg-surface-container-high text-on-surface hover:bg-surface-bright'}"
                   href="${fn:escapeXml(pageBase)}${sep}page=${p}">${p}</a>
            </c:forEach>
            <c:choose>
                <c:when test="${concessionPage.page < concessionPage.totalPages}">
                    <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${fn:escapeXml(pageBase)}${sep}page=${concessionPage.page + 1}">Sau</a>
                </c:when>
                <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Sau</span></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
</main>

<c:if test="${showForm}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-xl p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/concessions${fn:escapeXml(listQuery)}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-lg flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">fastfood</span>
                ${form.concessionId > 0 ? 'Chỉnh sửa món' : 'Thêm món mới'}
            </h3>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/concessions${fn:escapeXml(listQuery)}" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="concessionId" value="${form.concessionId}"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="name">Tên món <span class="req">*</span></label>
                    <input class="${inputClass}" id="name" name="name" type="text" maxlength="100" autofocus
                           value="<c:out value='${form.name}'/>" placeholder="VD: Combo Bắp Nước Nhỏ"/>
                    <c:if test="${not empty errors.name}"><p class="form-error"><c:out value="${errors.name}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="description">Mô tả</label>
                    <textarea class="${inputClass} py-2" id="description" name="description" rows="2" maxlength="500"
                              placeholder="VD: 1 bắp lớn + 1 nước vừa"><c:out value="${form.description}"/></textarea>
                    <c:if test="${not empty errors.description}"><p class="form-error"><c:out value="${errors.description}"/></p></c:if>
                </div>
                <div class="grid grid-cols-2 gap-space-md">
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="price">Giá bán (VND) <span class="req">*</span></label>
                        <input class="${inputClass}" id="price" name="price" type="text" inputmode="numeric" maxlength="9"
                               value="<c:out value='${form.price}'/>" placeholder="65000"/>
                        <c:if test="${not empty errors.price}"><p class="form-error"><c:out value="${errors.price}"/></p></c:if>
                    </div>
                    <div>
                        <label class="block text-label-md text-on-surface-variant mb-1" for="stockQuantity">Tồn kho <span class="req">*</span></label>
                        <input class="${inputClass}" id="stockQuantity" name="stockQuantity" type="text" inputmode="numeric" maxlength="6"
                               value="<c:out value='${form.stockQuantity}'/>" placeholder="100"/>
                        <c:if test="${not empty errors.stockQuantity}"><p class="form-error"><c:out value="${errors.stockQuantity}"/></p></c:if>
                    </div>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="imageUrl">Ảnh (URL)</label>
                    <input class="${inputClass}" id="imageUrl" name="imageUrl" type="text" maxlength="500"
                           value="<c:out value='${form.imageUrl}'/>" placeholder="https://..."/>
                    <c:if test="${not empty errors.imageUrl}"><p class="form-error"><c:out value="${errors.imageUrl}"/></p></c:if>
                </div>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="itemStatus">Trạng thái <span class="req">*</span></label>
                    <select class="${inputClass}" id="itemStatus" name="itemStatus">
                        <c:forEach var="st" items="${formStatuses}">
                            <option value="${st}" ${form.status == st ? 'selected' : ''}>${st == 'Active' ? 'Đang bán' : 'Ẩn'}</option>
                        </c:forEach>
                    </select>
                    <p class="text-body-sm text-on-surface-variant mt-1">Món đang bán sẽ tự chuyển "Hết hàng" khi tồn kho bằng 0.</p>
                    <c:if test="${not empty errors.status}"><p class="form-error"><c:out value="${errors.status}"/></p></c:if>
                </div>
                <div class="flex items-center justify-end gap-space-md pt-space-md">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/concessions${fn:escapeXml(listQuery)}">Hủy bỏ</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        ${form.concessionId > 0 ? 'Lưu thay đổi' : 'Lưu món'}
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
