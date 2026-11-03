<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- UC-35: bảng báo cáo tình trạng phòng, dùng chung cho nhân viên và quản lý.
     Attribute: reportPage, pageBase; param showStaff=1 hiện cột nhân viên, showReportLink=1 hiện link báo sự cố UC-21 --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<div class="cl-card overflow-hidden">
    <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
            <thead>
            <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                <th class="py-4 px-5 whitespace-nowrap">Mã</th>
                <th class="py-4 px-5 whitespace-nowrap">Phòng</th>
                <c:if test="${param.showStaff == '1'}"><th class="py-4 px-5 whitespace-nowrap">Nhân viên</th></c:if>
                <th class="py-4 px-5 whitespace-nowrap">Kết quả</th>
                <th class="py-4 px-5">Chi tiết</th>
                <th class="py-4 px-5 whitespace-nowrap">Thời gian</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
            <c:forEach var="report" items="${reportPage.items}">
                <tr class="align-top hover:bg-surface-container transition-colors">
                    <td class="py-4 px-5 text-outline font-label-sm whitespace-nowrap">#TTP-${report.reportId}</td>
                    <td class="py-4 px-5 whitespace-nowrap"><c:out value="${report.roomName}"/></td>
                    <c:if test="${param.showStaff == '1'}"><td class="py-4 px-5 whitespace-nowrap"><c:out value="${report.staffName}"/></td></c:if>
                    <td class="py-4 px-5">
                        <span class="cl-pill ${report.issue ? 'cl-pill--danger' : 'cl-pill--success'}">${report.issue ? 'Có vấn đề' : 'Đạt'}</span>
                    </td>
                    <td class="py-4 px-5 text-body-sm text-on-surface-variant">
                        <c:choose>
                            <c:when test="${report.issue}">
                                <ul class="space-y-1">
                                    <c:forEach var="item" items="${report.failedItems}">
                                        <li><span class="text-on-surface font-label-lg"><c:out value="${item.label}"/>:</span> <c:out value="${item.note}"/></li>
                                    </c:forEach>
                                </ul>
                            </c:when>
                            <c:otherwise>Tất cả hạng mục đạt.</c:otherwise>
                        </c:choose>
                        <c:if test="${not empty report.note}"><p class="mt-1">Ghi chú: <c:out value="${report.note}"/></p></c:if>
                        <c:if test="${report.issue and param.showReportLink == '1' and allowedPermissions.contains('REPORT_EQUIPMENT')}">
                            <a class="inline-block mt-1 text-primary hover:underline" href="${ctx}/staff/equipment-reports?add=1">Báo sự cố cần sửa</a>
                        </c:if>
                    </td>
                    <td class="py-4 px-5 text-on-surface-variant whitespace-nowrap">${report.createdAtText}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty reportPage.items}">
                <tr><td colspan="6" class="py-12 px-6 text-center text-on-surface-variant"><c:out value="${msgNoResult}"/></td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <ui:pagination pageData="${reportPage}" baseUrl="${pageBase}" label="báo cáo"/>
</div>
