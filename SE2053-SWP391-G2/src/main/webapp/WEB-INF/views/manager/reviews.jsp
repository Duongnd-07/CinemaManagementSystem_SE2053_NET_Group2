<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="statusLabels" value="${{'Visible':'Đang hiển thị','Hidden':'Đã ẩn'}}"/>
<c:set var="statusPills" value="${{'Visible':'cl-pill--success','Hidden':'cl-pill--danger'}}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Đánh giá phim - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface">
<jsp:include page="/WEB-INF/views/common/manager-sidebar.jsp"/>

<main class="pl-64 pt-16">
<div class="px-space-xl pt-space-lg pb-16">

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="mb-space-xl">
        <div class="flex items-center gap-space-sm mb-space-xs">
            <span class="text-label-sm px-2 py-0.5 rounded bg-primary/20 text-primary uppercase">Quản lý rạp</span>
            <span class="text-outline text-body-sm">/</span>
            <span class="text-on-surface-variant text-body-sm">Đánh giá phim</span>
        </div>
        <h1 class="text-headline-lg text-on-surface">Kiểm duyệt đánh giá phim</h1>
    </div>

    <%-- Bộ lọc trạng thái --%>
    <c:set var="allCount" value="0"/>
    <c:forEach var="st" items="${statuses}">
        <c:set var="allCount" value="${allCount + (empty statusCounts[st] ? 0 : statusCounts[st])}"/>
    </c:forEach>
    <div class="flex flex-wrap items-center gap-space-sm bg-surface-container-low p-space-md rounded-xl mb-space-lg">
        <c:set var="tabOn" value="bg-primary-container text-on-primary-container shadow-[0_0_15px_-2px_rgba(229,9,20,0.4)]"/>
        <c:set var="tabOff" value="bg-surface-container-high text-on-surface-variant hover:text-on-surface"/>
        <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${empty statusFilter ? tabOn : tabOff}"
           href="${ctx}/manager/reviews">Tất cả (${allCount})</a>
        <c:forEach var="st" items="${statuses}">
            <c:url var="tabUrl" value="/manager/reviews">
                <c:param name="status" value="${st}"/>
            </c:url>
            <a class="px-4 py-2 rounded-lg font-label-lg text-sm transition-all ${statusFilter == st ? tabOn : tabOff}"
               href="${fn:escapeXml(tabUrl)}">
                ${statusLabels[st]} (${empty statusCounts[st] ? 0 : statusCounts[st]})
            </a>
        </c:forEach>
    </div>

    <div class="cl-card overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-5 whitespace-nowrap">Phim</th>
                    <th class="py-4 px-5 whitespace-nowrap">Người đánh giá</th>
                    <th class="py-4 px-5 whitespace-nowrap">Số sao</th>
                    <th class="py-4 px-5">Nhận xét</th>
                    <th class="py-4 px-5 whitespace-nowrap">Trạng thái</th>
                    <th class="py-4 px-5 whitespace-nowrap">Thời gian gửi</th>
                    <th class="py-4 px-5 whitespace-nowrap"></th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="item" items="${reviewPage.items}">
                    <tr class="hover:bg-surface-container transition-colors align-top">
                        <td class="py-4 px-5 whitespace-nowrap"><c:out value="${item.movieTitle}"/></td>
                        <td class="py-4 px-5 whitespace-nowrap"><c:out value="${item.reviewerName}"/></td>
                        <td class="py-4 px-5 whitespace-nowrap">
                            <c:forEach begin="1" end="5" var="star">
                                <span class="material-symbols-outlined text-[16px] ${star <= item.rating ? 'text-[#ffb95f]' : 'text-[#4a4e5c]'}">star</span>
                            </c:forEach>
                        </td>
                        <td class="py-4 px-5 max-w-sm">
                            <span class="line-clamp-2"><c:out value="${empty item.comment ? '—' : item.comment}"/></span>
                            <c:if test="${item.status == 'Hidden'}">
                                <div class="mt-1 text-body-sm text-on-surface-variant">Lý do ẩn: <c:out value="${item.hiddenReason}"/></div>
                            </c:if>
                        </td>
                        <td class="py-4 px-5"><span class="cl-pill ${statusPills[item.status]}"><c:out value="${statusLabels[item.status]}"/></span></td>
                        <td class="py-4 px-5 text-on-surface-variant whitespace-nowrap">
                            <c:set var="sentAt" value="${item.createdAt}"/>
                            <c:if test="${not empty sentAt}">${fn:substring(sentAt, 8, 10)}/${fn:substring(sentAt, 5, 7)}/${fn:substring(sentAt, 0, 4)} ${fn:substring(sentAt, 11, 16)}</c:if>
                        </td>
                        <td class="py-4 px-5 text-right whitespace-nowrap">
                            <c:choose>
                                <c:when test="${item.status == 'Visible'}">
                                    <a class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-error-container hover:bg-error-container/80 text-on-error-container font-label-lg transition-all"
                                       href="${ctx}/manager/reviews?id=${item.reviewId}${fn:escapeXml(fn:replace(listQuery, '?', '&'))}">
                                        <span class="material-symbols-outlined text-[18px]">visibility_off</span>Ẩn
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <form method="post" action="${ctx}/manager/reviews${fn:escapeXml(listQuery)}" data-confirm="Bỏ ẩn đánh giá này?">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                        <input type="hidden" name="reviewId" value="${item.reviewId}"/>
                                        <input type="hidden" name="action" value="unhide"/>
                                        <button type="submit" class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg transition-all">
                                            <span class="material-symbols-outlined text-[18px]">visibility</span>Bỏ ẩn
                                        </button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty reviewPage.items}">
                    <tr><td colspan="7" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
        <c:url var="pageBase" value="/manager/reviews">
            <c:if test="${not empty statusFilter}"><c:param name="status" value="${statusFilter}"/></c:if>
        </c:url>
        <ui:pagination pageData="${reviewPage}" baseUrl="${pageBase}" label="đánh giá"/>
    </div>
</div>
</main>

<c:if test="${not empty review}">
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4 overflow-y-auto" data-modal>
        <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-lg p-space-xl shadow-2xl relative my-8">
            <a class="absolute right-4 top-4 text-on-surface-variant hover:text-on-surface"
               href="${ctx}/manager/reviews${fn:escapeXml(listQuery)}" title="Đóng">
                <span class="material-symbols-outlined">close</span>
            </a>
            <h3 class="text-headline-md text-on-surface mb-space-md flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary">visibility_off</span>Ẩn đánh giá vi phạm
            </h3>

            <div class="bg-surface-container rounded-lg p-space-md mb-space-md space-y-1">
                <div class="text-body-sm text-on-surface-variant">Phim: <span class="text-on-surface font-label-lg"><c:out value="${review.movieTitle}"/></span></div>
                <div class="text-body-sm text-on-surface-variant">Người đánh giá: <span class="text-on-surface font-label-lg"><c:out value="${review.reviewerName}"/></span></div>
                <div class="text-body-md text-on-surface whitespace-pre-line"><c:out value="${empty review.comment ? '(Không có nhận xét)' : review.comment}"/></div>
            </div>

            <c:if test="${not empty errors.general}">
                <div class="mb-space-md form-error-banner"><c:out value="${errors.general}"/></div>
            </c:if>

            <form method="post" action="${ctx}/manager/reviews${fn:escapeXml(listQuery)}" novalidate class="space-y-space-md">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="reviewId" value="${review.reviewId}"/>
                <input type="hidden" name="action" value="hide"/>
                <div>
                    <label class="block text-label-md text-on-surface-variant mb-1" for="reason">Lý do ẩn</label>
                    <textarea class="${inputClass} py-2" id="reason" name="reason" rows="3" maxlength="255"
                              placeholder="VD: Nội dung xúc phạm, spam, không liên quan đến phim" required></textarea>
                    <c:if test="${not empty errors.reason}"><p class="form-error"><c:out value="${errors.reason}"/></p></c:if>
                </div>
                <div class="flex items-center justify-end gap-space-md pt-space-sm">
                    <a class="px-5 py-2.5 rounded-full bg-surface-container-high text-on-surface hover:bg-surface-bright font-label-lg transition-all"
                       href="${ctx}/manager/reviews${fn:escapeXml(listQuery)}">Đóng</a>
                    <button type="submit"
                            class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all">
                        Xác nhận ẩn
                    </button>
                </div>
            </form>
        </div>
    </div>
</c:if>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
