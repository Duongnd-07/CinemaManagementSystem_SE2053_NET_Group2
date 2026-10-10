<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="pageData" required="true" type="vn.edu.fpt.util.Page" description="Đối tượng Page do Service trả về" %>
<%@ attribute name="baseUrl" required="true" description="URL danh sách kèm các bộ lọc, chưa có tham số page" %>
<%@ attribute name="label" required="true" description="Tên đối tượng hiển thị, ví dụ: phim, thể loại" %>
<c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&' : '?'}"/>
<div class="flex items-center justify-between p-4 bg-surface-container border-t border-white/5 text-body-sm text-on-surface-variant">
    <div>Hiển thị ${pageData.fromItem}-${pageData.toItem} trong tổng số ${pageData.totalItems} ${label}</div>
    <div class="flex items-center gap-2">
        <c:choose>
            <c:when test="${pageData.page > 1}">
                <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${baseUrl}${sep}page=${pageData.page - 1}">Trước</a>
            </c:when>
            <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Trước</span></c:otherwise>
        </c:choose>
        <c:forEach var="p" begin="1" end="${pageData.totalPages}">
            <a class="px-3 py-1.5 rounded ${p == pageData.page ? 'bg-primary text-on-primary font-bold' : 'bg-surface-container-high text-on-surface hover:bg-surface-bright'}"
               href="${baseUrl}${sep}page=${p}">${p}</a>
        </c:forEach>
        <c:choose>
            <c:when test="${pageData.page < pageData.totalPages}">
                <a class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface hover:bg-surface-bright" href="${baseUrl}${sep}page=${pageData.page + 1}">Sau</a>
            </c:when>
            <c:otherwise><span class="px-3 py-1.5 rounded bg-surface-container-high text-on-surface opacity-50">Sau</span></c:otherwise>
        </c:choose>
    </div>
</div>
