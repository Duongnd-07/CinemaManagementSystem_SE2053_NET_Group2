<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- UC-35: nhãn trạng thái kiểm tra phòng. Tham số: status (NotInspected | OK | HasIssue | Missed), late (true/false).
     Dùng cho trang nhân viên và trang lịch sử của quản lý. --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<span class="inline-flex items-center gap-1 flex-wrap justify-end">
<c:choose>
    <c:when test="${param.status == 'OK'}">
        <span class="px-2 py-0.5 rounded-full text-label-md bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">Đã kiểm tra – Ổn</span>
    </c:when>
    <c:when test="${param.status == 'HasIssue'}">
        <span class="px-2 py-0.5 rounded-full text-label-md bg-amber-500/15 text-amber-400 border border-amber-500/30">Đã kiểm tra – Có sự cố</span>
    </c:when>
    <c:when test="${param.status == 'Missed'}">
        <span class="px-2 py-0.5 rounded-full text-label-md bg-error-container/60 text-on-error-container border border-error/40">Không được kiểm tra</span>
    </c:when>
    <c:otherwise>
        <span class="px-2 py-0.5 rounded-full text-label-md bg-white/5 text-on-surface-variant border border-white/15">Chưa kiểm tra</span>
    </c:otherwise>
</c:choose>
<c:if test="${param.late == 'true'}">
    <span class="px-2 py-0.5 rounded-full text-label-md bg-orange-500/15 text-orange-300 border border-orange-500/30">Chậm</span>
</c:if>
</span>
