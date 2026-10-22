<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- UC-33: Manage System Configurations --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình hệ thống - CineLuxe Admin</title>
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
                <a href="${ctx}/admin/configurations" aria-current="page"
                   class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-white bg-white/10 border border-white/10">
                    <span class="material-symbols-outlined text-lg">tune</span>Cấu hình hệ thống
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

    <main class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <%-- Menu cho màn hình nhỏ --%>
        <nav class="flex sm:hidden gap-2 text-sm">
            <a href="${ctx}/admin/users" class="flex-1 text-center px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-[#8e919f]">Tài khoản</a>
            <span class="flex-1 text-center px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-white">Cấu hình</span>
        </nav>

        <div>
            <h1 class="text-2xl font-extrabold text-white">Cấu hình hệ thống</h1>
            <p class="text-sm text-[#8e919f] mt-1">
                Thay đổi được áp dụng ngay cho các thao tác phát sinh sau khi lưu; đơn đặt vé, ghế đang giữ và voucher đã phát hành không bị ảnh hưởng.
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

        <form method="post" action="${ctx}/admin/configurations" novalidate class="space-y-6" id="configForm">
            <input type="hidden" name="csrfToken" value="${csrfToken}"/>

            <c:forEach var="group" items="${groups}">
                <section class="bg-[#171b26] border border-white/10 rounded-2xl shadow-xl overflow-hidden">
                    <h2 class="flex items-center gap-2 px-5 py-3.5 border-b border-white/10 text-sm font-bold uppercase tracking-wider text-[#ffb95f]">
                        <span class="material-symbols-outlined text-xl">${group.key.icon}</span>
                        <c:out value="${group.key.label}"/>
                    </h2>
                    <div class="divide-y divide-white/5">
                        <c:forEach var="item" items="${group.value}">
                            <c:set var="error" value="${errors[item.key]}"/>
                            <div class="px-5 py-4 grid grid-cols-1 md:grid-cols-12 gap-3 md:items-start">
                                <div class="md:col-span-7">
                                    <label for="cfg-${item.key}" class="text-sm font-semibold text-white">
                                        <c:out value="${item.param.label}"/>
                                    </label>
                                    <p class="text-xs text-[#8e919f] mt-1"><c:out value="${item.description}"/></p>
                                    <p class="text-[11px] text-gray-500 mt-1.5">
                                        <span class="font-mono">${item.key}</span>
                                        <c:choose>
                                            <c:when test="${item.stored}"> · Cập nhật lúc ${item.updatedAtText}</c:when>
                                            <c:otherwise> · <span class="text-amber-400">Chưa lưu trong DB, hệ thống đang dùng mặc định</span></c:otherwise>
                                        </c:choose>
                                    </p>
                                </div>
                                <div class="md:col-span-5">
                                    <div class="flex items-stretch rounded-xl border ${empty error ? 'border-white/10 focus-within:border-[#e50914]/60' : 'border-red-500/70'} bg-[#0f121b] overflow-hidden">
                                        <input type="text" inputmode="${item.param.decimal ? 'decimal' : 'numeric'}"
                                               id="cfg-${item.key}" name="${item.key}" value="<c:out value='${item.value}'/>"
                                               data-stored="<c:out value='${item.storedValue}'/>" maxlength="20"
                                               aria-invalid="${not empty error}" aria-describedby="err-${item.key}"
                                               class="config-input flex-1 min-w-0 bg-transparent px-3.5 py-2.5 text-sm text-white text-right outline-none"/>
                                        <span class="px-3 flex items-center text-xs text-[#8e919f] bg-white/5 border-l border-white/10"><c:out value="${item.param.unit}"/></span>
                                    </div>
                                    <p class="text-[11px] text-gray-500 mt-1 text-right">
                                        Từ ${item.param.min.toPlainString()} đến ${item.param.max.toPlainString()}<c:if test="${item.param.decimal}">, tối đa 2 chữ số thập phân</c:if>
                                    </p>
                                    <c:if test="${not empty error}">
                                        <p id="err-${item.key}" class="text-xs text-red-400 mt-1"><c:out value="${error}"/></p>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </section>
            </c:forEach>

            <div class="flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
                <a href="${ctx}/admin/configurations"
                   class="text-center px-5 py-2.5 rounded-xl text-sm font-semibold text-[#dfe2f1] bg-white/5 border border-white/10 hover:bg-white/10 transition">
                    Khôi phục giá trị đang lưu
                </a>
                <button type="submit"
                        class="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold text-white bg-[#e50914] hover:bg-[#ff1f2a] shadow-[0_0_15px_rgba(229,9,20,0.35)] transition">
                    <span class="material-symbols-outlined text-lg">save</span>Lưu cấu hình
                </button>
            </div>
        </form>
    </main>

    <script src="${ctx}/assets/js/system-config.js"></script>
</body>
</html>
