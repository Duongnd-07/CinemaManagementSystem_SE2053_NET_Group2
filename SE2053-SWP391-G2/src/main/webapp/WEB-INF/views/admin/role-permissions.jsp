<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-31: Manage Roles & Permissions --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Phân quyền - CineLuxe Admin</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" rel="stylesheet"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0c0f17;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0c0f17] text-[#dfe2f1]">
    <header class="h-16 bg-[#131722]/90 backdrop-blur-md border-b border-white/10 sticky top-0 z-40 flex items-center justify-between px-4 sm:px-8 shadow-lg">
        <a href="${ctx}/admin/users" class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#e50914] to-[#ff4d58] flex items-center justify-center shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                <span class="material-symbols-outlined text-white text-xl">admin_panel_settings</span>
            </div>
            <span class="text-lg font-extrabold tracking-wider text-white">CINE<span class="text-[#e50914]">ADMIN</span></span>
        </a>

        <div class="flex items-center gap-4">
            <jsp:include page="/WEB-INF/views/common/notification-bell.jsp">
                <jsp:param name="href" value="${ctx}/admin/notifications"/>
            </jsp:include>
            <jsp:include page="/WEB-INF/views/common/admin-user-menu.jsp">
                <jsp:param name="active" value="permissions"/>
            </jsp:include>
        </div>
    </header>

    <main class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div>
            <h1 class="text-2xl font-extrabold text-white">Phân quyền theo vai trò</h1>
            <p class="text-sm text-[#8e919f] mt-1">
                Thay đổi có hiệu lực ngay ở lần truy cập tiếp theo, kể cả với người đang đăng nhập.
                Khu vực Admin và Khách hàng được cố định theo vai trò nên không nằm trong bảng này.
            </p>
        </div>

        <c:if test="${not empty flashSuccess}">
            <div class="flash-alert p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center gap-3 shadow-lg" role="status">
                <span class="material-symbols-outlined text-emerald-400 text-2xl">check_circle</span>
                <span class="font-medium"><c:out value="${flashSuccess}"/></span>
            </div>
        </c:if>
        <c:if test="${not empty flashError}">
            <div class="p-4 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-3 shadow-lg" role="alert">
                <span class="material-symbols-outlined text-red-400 text-2xl">error</span>
                <span class="font-medium"><c:out value="${flashError}"/></span>
            </div>
        </c:if>

        <form method="post" action="${ctx}/admin/permissions" class="space-y-6">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>

            <c:forEach var="group" items="${groups}">
                <section class="bg-[#171b26] border border-white/10 rounded-2xl shadow-xl overflow-hidden">
                    <h2 class="flex items-center gap-2 px-5 py-3.5 border-b border-white/10 text-sm font-bold uppercase tracking-wider text-[#ffb95f]">
                        <span class="material-symbols-outlined text-xl">${group.key.icon}</span>
                        <c:out value="${group.key.label}"/>
                    </h2>
                    <div class="overflow-x-auto">
                        <table class="w-full text-sm">
                            <thead>
                                <tr class="text-xs text-[#8e919f] border-b border-white/10">
                                    <th scope="col" class="px-5 py-3 text-left font-semibold">Chức năng</th>
                                    <c:forEach var="role" items="${group.key.roles}">
                                        <th scope="col" class="w-24 px-3 py-3 text-center font-semibold"><c:out value="${role}"/></th>
                                    </c:forEach>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-white/5">
                                <c:forEach var="row" items="${group.value}">
                                    <tr>
                                        <td class="px-5 py-3.5">
                                            <p class="font-semibold text-white"><c:out value="${row.permission.label}"/></p>
                                            <p class="text-xs text-[#8e919f] mt-0.5"><c:out value="${row.permission.description}"/></p>
                                        </td>
                                        <c:forEach var="cell" items="${row.cells}">
                                            <td class="px-3 py-3.5 text-center transition-colors">
                                                <c:choose>
                                                    <c:when test="${cell.locked}">
                                                        <span class="material-symbols-outlined text-lg text-[#8e919f]"
                                                              title="Luôn bật vì đây là trang đích sau khi đăng nhập"
                                                              aria-label="${row.permission.label} - ${cell.role}: luôn bật">lock</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <input type="checkbox" name="grant" value="${cell.key}"
                                                               data-stored="${cell.granted}" ${cell.granted ? 'checked' : ''}
                                                               aria-label="<c:out value='${row.permission.label} - ${cell.role}'/>"
                                                               class="permission-input w-4 h-4 accent-[#e50914] cursor-pointer"/>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </section>
            </c:forEach>

            <div class="flex flex-col-reverse sm:flex-row sm:items-center sm:justify-end gap-3">
                <span id="changeCount" class="text-xs text-[#ffb95f] sm:mr-auto" aria-live="polite"></span>
                <a href="${ctx}/admin/permissions"
                   class="text-center px-5 py-2.5 rounded-xl text-sm font-semibold text-[#dfe2f1] bg-white/5 border border-white/10 hover:bg-white/10 transition">
                    Khôi phục giá trị đang lưu
                </a>
                <button type="submit"
                        class="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold text-white bg-[#e50914] hover:bg-[#ff1f2a] shadow-[0_0_15px_rgba(229,9,20,0.35)] transition">
                    <span class="material-symbols-outlined text-lg">save</span>Lưu phân quyền
                </button>
            </div>
        </form>
    </main>

    <script src="${ctx}/assets/js/role-permissions.js"></script>
</body>
</html>
