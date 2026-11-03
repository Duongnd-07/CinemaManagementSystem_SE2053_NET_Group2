<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- UC-25 A5: danh sách phòng mẫu --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Phòng mẫu - CineAdmin</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
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
                <span class="text-on-surface-variant text-body-sm">Phòng mẫu</span>
            </div>
            <h1 class="text-headline-lg text-on-surface">Phòng chiếu &amp; Sơ đồ ghế</h1>
        </div>
        <a href="${ctx}/manager/room-templates?add=1"
           class="flex items-center gap-space-sm bg-primary hover:bg-primary/90 text-on-primary px-space-lg py-2 rounded-full font-label-lg shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)] transition-all whitespace-nowrap self-start md:self-auto">
            <span class="material-symbols-outlined text-[20px]">add</span>
            <span>Thêm phòng mẫu</span>
        </a>
    </div>

    <jsp:include page="/WEB-INF/views/manager/room-tabs.jsp">
        <jsp:param name="active" value="templates"/>
    </jsp:include>

    <div class="bg-surface-container-low rounded-xl overflow-hidden shadow-xl border border-white/5">
        <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
                <thead>
                <tr class="bg-surface-container border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                    <th class="py-4 px-6">Tên phòng mẫu</th>
                    <th class="py-4 px-6">Định dạng gợi ý</th>
                    <th class="py-4 px-6">Kích thước lưới</th>
                    <th class="py-4 px-6">Số ghế / sức chứa</th>
                    <th class="py-4 px-6">Cập nhật</th>
                    <th class="py-4 px-6 text-right">Hành động</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-white/5 text-body-md text-on-surface">
                <c:forEach var="template" items="${templates}">
                    <tr class="hover:bg-surface-container transition-colors">
                        <td class="py-4 px-6 font-headline-sm"><c:out value="${template.templateName}"/></td>
                        <td class="py-4 px-6">
                            <span class="px-2 py-0.5 rounded-md text-label-md bg-primary/15 text-primary border border-primary/30"><c:out value="${empty template.format ? '—' : template.format}"/></span>
                        </td>
                        <td class="py-4 px-6 text-on-surface-variant">${template.rows} hàng × ${template.cols} cột</td>
                        <td class="py-4 px-6 text-on-surface-variant">
                            ${template.seatCount} ghế
                            <span class="block text-body-sm text-outline">Sức chứa ${template.capacity} người</span>
                        </td>
                        <td class="py-4 px-6 text-on-surface-variant text-body-sm">
                            ${fn:substring(template.updatedAt, 11, 16)} ${fn:substring(template.updatedAt, 8, 10)}/${fn:substring(template.updatedAt, 5, 7)}/${fn:substring(template.updatedAt, 0, 4)}
                        </td>
                        <td class="py-4 px-6 text-right">
                            <div class="inline-flex items-center gap-2">
                                <a class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                                   href="${ctx}/manager/rooms?add=1&amp;template=${template.templateId}" title="Tạo phòng chiếu mới từ phòng mẫu này">
                                    <span class="material-symbols-outlined text-[18px]">add_home</span>Tạo phòng
                                </a>
                                <a class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-surface-bright text-on-surface-variant hover:text-on-surface transition-all font-label-md"
                                   href="${ctx}/manager/room-templates?edit=${template.templateId}" title="Sửa phòng mẫu">
                                    <span class="material-symbols-outlined text-[18px]">edit</span>Sửa
                                </a>
                                <form method="post" action="${ctx}/manager/room-templates" class="inline"
                                      data-confirm="Xóa phòng mẫu &quot;${fn:escapeXml(template.templateName)}&quot;? Các phòng đã tạo từ mẫu này không bị ảnh hưởng.">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                                    <input type="hidden" name="action" value="delete"/>
                                    <input type="hidden" name="templateId" value="${template.templateId}"/>
                                    <button type="submit"
                                            class="inline-flex items-center gap-1 px-3 py-2 rounded-lg bg-surface-container-high hover:bg-error-container text-on-surface-variant hover:text-on-error-container transition-all font-label-md">
                                        <span class="material-symbols-outlined text-[18px]">delete</span>Xóa
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty templates}">
                    <tr><td colspan="6" class="py-12 px-6 text-center text-on-surface-variant">Chưa có phòng mẫu nào. Hãy bấm "Thêm phòng mẫu" hoặc "Lưu thành phòng mẫu" trên sơ đồ ghế của một phòng.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
    <p class="mt-space-md text-body-sm text-outline">
        Phòng mẫu chỉ là sơ đồ ghế để dùng lại khi tạo phòng mới, không có trạng thái và không bao giờ được dùng cho suất chiếu.
        Tạo phòng từ mẫu là chép mẫu tại thời điểm đó: sửa hoặc xóa mẫu sau này không làm thay đổi phòng đã tạo.
    </p>
</div>
</main>

<script src="${ctx}/assets/js/manager-common.js"></script>
</body>
</html>
