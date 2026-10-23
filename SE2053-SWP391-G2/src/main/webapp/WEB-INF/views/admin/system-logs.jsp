<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- UC-32: View System Logs --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhật ký hệ thống - CineLuxe Admin</title>
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
        input[type="date"] {
            color-scheme: dark;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0c0f17] text-[#dfe2f1]">
    <header class="h-16 bg-[#131722]/90 backdrop-blur-md border-b border-white/10 sticky top-0 z-40 flex items-center justify-between px-4 sm:px-8 shadow-lg">
        <div class="flex items-center gap-6">
            <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#e50914] to-[#ff4d58] flex items-center justify-center shadow-[0_0_15px_rgba(229,9,20,0.5)]">
                    <span class="material-symbols-outlined text-white text-xl">admin_panel_settings</span>
                </div>
                <span class="text-lg font-extrabold tracking-wider text-white">CINE<span class="text-[#e50914]">ADMIN</span></span>
            </div>
            <nav class="hidden sm:flex items-center gap-1 text-sm">
                <a href="${ctx}/admin/users"
                   class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-[#8e919f] hover:text-white hover:bg-white/5 transition">
                    <span class="material-symbols-outlined text-lg">group</span>Tài khoản
                </a>
                <a href="${ctx}/admin/configurations"
                   class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-[#8e919f] hover:text-white hover:bg-white/5 transition">
                    <span class="material-symbols-outlined text-lg">tune</span>Cấu hình hệ thống
                </a>
                <a href="${ctx}/admin/logs" aria-current="page"
                   class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-white bg-white/10 border border-white/10">
                    <span class="material-symbols-outlined text-lg">receipt_long</span>Nhật ký hệ thống
                </a>
            </nav>
        </div>

        <div class="flex items-center gap-4">
            <div class="hidden sm:flex items-center gap-2.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10">
                <span class="material-symbols-outlined text-[#ffb95f] text-lg">shield_person</span>
                <span class="text-xs font-semibold text-white">
                    <c:out value="${sessionScope.user.fullName != null ? sessionScope.user.fullName : 'Quản trị viên'}"/>
                </span>
                <span class="text-[10px] bg-red-950/80 border border-red-500/40 text-red-300 px-1.5 py-0.5 rounded font-bold">Admin</span>
            </div>
            <a href="${ctx}/logout"
               class="flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-[#e50914] bg-white/5 hover:bg-red-500/10 border border-white/10 hover:border-red-500/30 px-3 py-1.5 rounded-xl transition">
                <span class="material-symbols-outlined text-base">logout</span>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <%-- Menu cho màn hình nhỏ --%>
        <nav class="flex sm:hidden gap-2 text-sm">
            <a href="${ctx}/admin/users" class="flex-1 text-center px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-[#8e919f]">Tài khoản</a>
            <a href="${ctx}/admin/configurations" class="flex-1 text-center px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-[#8e919f]">Cấu hình</a>
            <span class="flex-1 text-center px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-white">Nhật ký</span>
        </nav>

        <div>
            <h1 class="text-2xl font-extrabold text-white">Nhật ký hệ thống</h1>
            <p class="text-sm text-[#8e919f] mt-1">
                Theo dõi đăng nhập, thay đổi tài khoản, cấu hình, dữ liệu rạp, giao dịch và lỗi hệ thống. Nhật ký chỉ được xem, không thể sửa hoặc xóa.
            </p>
        </div>

        <c:if test="${not empty flashError}">
            <div class="p-4 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-3 shadow-lg" role="alert">
                <span class="material-symbols-outlined text-red-400 text-2xl">error</span>
                <span class="font-medium"><c:out value="${flashError}"/></span>
            </div>
        </c:if>

        <%-- Bộ lọc: GET để link chuyển trang và F5 giữ nguyên kết quả --%>
        <form method="get" action="${ctx}/admin/logs" novalidate
              class="bg-[#171b26] border border-white/10 rounded-2xl shadow-xl p-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
            <div>
                <label for="from" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Từ ngày</label>
                <input type="date" id="from" name="from" value="<c:out value='${form.fromDate}'/>"
                       aria-invalid="${not empty errors.fromDate}"
                       class="w-full rounded-xl bg-[#0f121b] border ${empty errors.fromDate ? 'border-white/10' : 'border-red-500/70'} px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60"/>
                <c:if test="${not empty errors.fromDate}">
                    <p class="text-xs text-red-400 mt-1"><c:out value="${errors.fromDate}"/></p>
                </c:if>
            </div>
            <div>
                <label for="to" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Đến ngày</label>
                <input type="date" id="to" name="to" value="<c:out value='${form.toDate}'/>"
                       aria-invalid="${not empty errors.toDate}"
                       class="w-full rounded-xl bg-[#0f121b] border ${empty errors.toDate ? 'border-white/10' : 'border-red-500/70'} px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60"/>
                <c:if test="${not empty errors.toDate}">
                    <p class="text-xs text-red-400 mt-1"><c:out value="${errors.toDate}"/></p>
                </c:if>
            </div>
            <div>
                <label for="level" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Mức độ</label>
                <select id="level" name="level"
                        class="w-full rounded-xl bg-[#0f121b] border border-white/10 px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60">
                    <option value="">Tất cả</option>
                    <c:forEach var="level" items="${levels}">
                        <option value="${level.key}" ${form.level == level.key ? 'selected' : ''}>${level.key} - <c:out value="${level.value}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label for="type" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Loại sự kiện</label>
                <select id="type" name="type"
                        class="w-full rounded-xl bg-[#0f121b] border border-white/10 px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60">
                    <option value="">Tất cả</option>
                    <c:forEach var="type" items="${eventTypes}">
                        <option value="${type.key}" ${form.eventType == type.key ? 'selected' : ''}><c:out value="${type.value}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div>
                <label for="user" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Người dùng</label>
                <input type="text" id="user" name="user" value="<c:out value='${form.user}'/>" maxlength="100"
                       placeholder="Tên hoặc email"
                       class="w-full rounded-xl bg-[#0f121b] border border-white/10 px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60 placeholder:text-gray-600"/>
            </div>
            <div>
                <label for="q" class="block text-xs font-semibold text-[#8e919f] mb-1.5">Từ khóa</label>
                <input type="text" id="q" name="q" value="<c:out value='${form.keyword}'/>" maxlength="100"
                       placeholder="Hành động, mô tả"
                       class="w-full rounded-xl bg-[#0f121b] border border-white/10 px-3 py-2 text-sm text-white outline-none focus:border-[#e50914]/60 placeholder:text-gray-600"/>
            </div>
            <div class="md:col-span-2 lg:col-span-6 flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
                <a href="${ctx}/admin/logs"
                   class="text-center px-5 py-2.5 rounded-xl text-sm font-semibold text-[#dfe2f1] bg-white/5 border border-white/10 hover:bg-white/10 transition">
                    Xóa bộ lọc
                </a>
                <button type="submit"
                        class="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold text-white bg-[#e50914] hover:bg-[#ff1f2a] shadow-[0_0_15px_rgba(229,9,20,0.35)] transition">
                    <span class="material-symbols-outlined text-lg">search</span>Tìm kiếm
                </button>
            </div>
        </form>

        <c:if test="${not empty logPage}">
            <section class="bg-[#171b26] border border-white/10 rounded-2xl shadow-xl overflow-hidden">
                <c:choose>
                    <c:when test="${empty logPage.items}">
                        <%-- E1 --%>
                        <div class="px-5 py-14 text-center">
                            <span class="material-symbols-outlined text-5xl text-gray-600">manage_search</span>
                            <p class="mt-3 text-sm text-[#8e919f]"><c:out value="${emptyMessage}"/></p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="overflow-x-auto">
                            <table class="w-full text-sm">
                                <thead class="bg-white/5 text-[11px] uppercase tracking-wider text-[#8e919f]">
                                    <tr>
                                        <th class="text-left font-semibold px-4 py-3 whitespace-nowrap">Thời gian</th>
                                        <th class="text-left font-semibold px-4 py-3">Mức độ</th>
                                        <th class="text-left font-semibold px-4 py-3 whitespace-nowrap">Loại sự kiện</th>
                                        <th class="text-left font-semibold px-4 py-3">Người dùng</th>
                                        <th class="text-left font-semibold px-4 py-3">Hành động</th>
                                        <th class="text-left font-semibold px-4 py-3">Đối tượng</th>
                                        <th class="text-left font-semibold px-4 py-3">IP</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-white/5">
                                    <c:forEach var="log" items="${logPage.items}">
                                        <c:set var="levelClass" value="${log.level == 'ERROR' ? 'bg-red-950/80 border-red-500/40 text-red-300'
                                                : log.level == 'WARNING' ? 'bg-amber-950/80 border-amber-500/40 text-amber-300'
                                                : 'bg-sky-950/80 border-sky-500/40 text-sky-300'}"/>
                                        <tr class="log-row hover:bg-white/5 cursor-pointer transition" tabindex="0"
                                            data-time="<c:out value='${log.createdAtText}'/>"
                                            data-level="<c:out value='${log.level} - ${log.levelLabel}'/>"
                                            data-type="<c:out value='${log.eventType} - ${log.eventTypeLabel}'/>"
                                            data-action="<c:out value='${log.action}'/>"
                                            data-user="<c:out value='${log.actorDisplay}'/>"
                                            data-email="<c:out value='${log.actorEmail}'/>"
                                            data-role="<c:out value='${log.actorRole}'/>"
                                            data-user-id="<c:out value='${log.userId}'/>"
                                            data-target="<c:out value='${log.targetText}'/>"
                                            data-ip="<c:out value='${log.ipAddress}'/>"
                                            data-description="<c:out value='${log.description}'/>">
                                            <td class="px-4 py-3 whitespace-nowrap text-xs text-[#8e919f] font-mono">${log.createdAtText}</td>
                                            <td class="px-4 py-3">
                                                <span class="text-[10px] border px-1.5 py-0.5 rounded font-bold ${levelClass}">${log.level}</span>
                                            </td>
                                            <td class="px-4 py-3 whitespace-nowrap text-xs"><c:out value="${log.eventTypeLabel}"/></td>
                                            <td class="px-4 py-3">
                                                <div class="text-xs ${empty log.actorName ? 'text-[#8e919f]' : 'text-white font-semibold'}"><c:out value="${log.actorDisplay}"/></div>
                                                <c:if test="${not empty log.actorRole}">
                                                    <div class="text-[11px] text-gray-500"><c:out value="${log.actorRole}"/></div>
                                                </c:if>
                                            </td>
                                            <td class="px-4 py-3">
                                                <div class="text-xs text-white font-semibold"><c:out value="${log.action}"/></div>
                                                <c:if test="${not empty log.description}">
                                                    <div class="text-[11px] text-[#8e919f] max-w-md truncate"><c:out value="${log.description}"/></div>
                                                </c:if>
                                            </td>
                                            <td class="px-4 py-3 whitespace-nowrap text-xs font-mono"><c:out value="${log.targetText}"/></td>
                                            <td class="px-4 py-3 whitespace-nowrap text-xs font-mono text-[#8e919f]"><c:out value="${log.ipAddress}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>

                        <%-- A3: phân trang hiển thị tối đa 2 trang hai bên trang hiện tại --%>
                        <c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&' : '?'}"/>
                        <c:set var="startPage" value="${logPage.page - 2 < 1 ? 1 : logPage.page - 2}"/>
                        <c:set var="endPage" value="${logPage.page + 2 > logPage.totalPages ? logPage.totalPages : logPage.page + 2}"/>
                        <div class="flex flex-col sm:flex-row items-center justify-between gap-3 p-4 border-t border-white/10 text-xs text-[#8e919f]">
                            <div>Hiển thị ${logPage.fromItem}-${logPage.toItem} trong tổng số ${logPage.totalItems} dòng nhật ký</div>
                            <div class="flex items-center gap-1.5">
                                <c:choose>
                                    <c:when test="${logPage.page > 1}">
                                        <a class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-white hover:bg-white/10" href="${baseUrl}${sep}page=${logPage.page - 1}">Trước</a>
                                    </c:when>
                                    <c:otherwise><span class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 opacity-40">Trước</span></c:otherwise>
                                </c:choose>
                                <c:if test="${startPage > 1}">
                                    <a class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-white hover:bg-white/10" href="${baseUrl}${sep}page=1">1</a>
                                    <c:if test="${startPage > 2}"><span class="px-1">…</span></c:if>
                                </c:if>
                                <c:forEach var="p" begin="${startPage}" end="${endPage}">
                                    <a class="px-3 py-1.5 rounded-lg border ${p == logPage.page ? 'bg-[#e50914] border-[#e50914] text-white font-bold' : 'bg-white/5 border-white/10 text-white hover:bg-white/10'}"
                                       href="${baseUrl}${sep}page=${p}">${p}</a>
                                </c:forEach>
                                <c:if test="${endPage < logPage.totalPages}">
                                    <c:if test="${endPage < logPage.totalPages - 1}"><span class="px-1">…</span></c:if>
                                    <a class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-white hover:bg-white/10" href="${baseUrl}${sep}page=${logPage.totalPages}">${logPage.totalPages}</a>
                                </c:if>
                                <c:choose>
                                    <c:when test="${logPage.page < logPage.totalPages}">
                                        <a class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-white hover:bg-white/10" href="${baseUrl}${sep}page=${logPage.page + 1}">Sau</a>
                                    </c:when>
                                    <c:otherwise><span class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 opacity-40">Sau</span></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>
    </main>

    <%-- A1: chi tiết một dòng nhật ký, nội dung do system-logs.js điền từ data-* của dòng được chọn --%>
    <dialog id="logDetail" class="w-full max-w-2xl rounded-2xl bg-[#171b26] border border-white/10 text-[#dfe2f1] p-0 shadow-2xl backdrop:bg-black/70">
        <div class="flex items-center justify-between px-5 py-4 border-b border-white/10">
            <h2 class="text-base font-bold text-white">Chi tiết nhật ký</h2>
            <button type="button" data-close class="p-1 rounded-lg text-[#8e919f] hover:text-white hover:bg-white/10" title="Đóng">
                <span class="material-symbols-outlined text-xl">close</span>
            </button>
        </div>
        <dl class="px-5 py-4 grid grid-cols-1 sm:grid-cols-3 gap-x-4 gap-y-3 text-sm">
            <dt class="text-[#8e919f]">Thời gian</dt><dd class="sm:col-span-2 font-mono" data-field="time"></dd>
            <dt class="text-[#8e919f]">Mức độ</dt><dd class="sm:col-span-2" data-field="level"></dd>
            <dt class="text-[#8e919f]">Loại sự kiện</dt><dd class="sm:col-span-2" data-field="type"></dd>
            <dt class="text-[#8e919f]">Hành động</dt><dd class="sm:col-span-2 font-semibold text-white" data-field="action"></dd>
            <dt class="text-[#8e919f]">Người dùng</dt><dd class="sm:col-span-2" data-field="user"></dd>
            <dt class="text-[#8e919f]">Email</dt><dd class="sm:col-span-2" data-field="email"></dd>
            <dt class="text-[#8e919f]">Vai trò</dt><dd class="sm:col-span-2" data-field="role"></dd>
            <dt class="text-[#8e919f]">Mã người dùng</dt><dd class="sm:col-span-2 font-mono" data-field="userId"></dd>
            <dt class="text-[#8e919f]">Đối tượng</dt><dd class="sm:col-span-2 font-mono" data-field="target"></dd>
            <dt class="text-[#8e919f]">Địa chỉ IP</dt><dd class="sm:col-span-2 font-mono" data-field="ip"></dd>
            <dt class="text-[#8e919f]">Mô tả</dt><dd class="sm:col-span-2 whitespace-pre-wrap break-words" data-field="description"></dd>
        </dl>
        <div class="px-5 py-4 border-t border-white/10 flex justify-end">
            <button type="button" data-close
                    class="px-5 py-2 rounded-xl text-sm font-semibold text-[#dfe2f1] bg-white/5 border border-white/10 hover:bg-white/10 transition">Đóng</button>
        </div>
    </dialog>

    <script src="${ctx}/assets/js/system-logs.js"></script>
</body>
</html>
