<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeNav" value="profile" scope="request"/>
<c:set var="inputBase" value="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Thông tin cá nhân - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0f131d;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen bg-[#0f131d] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>

    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-24 right-8 z-[60] flex items-center gap-2 px-5 py-3 rounded-xl shadow-2xl border text-sm font-semibold
                    ${not empty flashSuccess ? 'bg-emerald-950/90 border-emerald-500/40 text-emerald-300' : 'bg-red-950/90 border-red-500/40 text-red-300'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <main class="flex-1 max-w-7xl w-full mx-auto px-4 lg:px-8 py-8">
        <!-- Banner tóm tắt tài khoản -->
        <div class="relative overflow-hidden rounded-xl bg-[#171b26] p-6 md:p-8 mb-6 flex items-center gap-5 shadow-[0_8px_24px_-4px_rgba(0,0,0,0.6)]">
            <div class="absolute -right-20 -bottom-20 w-80 h-80 bg-[#e50914]/10 rounded-full blur-3xl pointer-events-none"></div>
            <div class="relative z-10 w-20 h-20 rounded-full bg-[#e50914]/20 ring-2 ring-[#e50914]/50 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-[#e50914] text-4xl">person</span>
            </div>
            <div class="relative z-10 min-w-0">
                <h1 class="text-2xl md:text-3xl font-bold text-white break-words"><c:out value="${profile.fullName}"/></h1>
                <p class="text-sm text-[#e9bcb6] break-all">
                    <c:out value="${profile.email}"/> &bull; <c:out value="${profile.phone}"/>
                </p>
            </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            <!-- Menu quản lý tài khoản -->
            <aside class="lg:col-span-3 flex flex-col gap-2 bg-[#171b26] rounded-xl p-4 shadow-[0_8px_24px_-4px_rgba(0,0,0,0.6)] lg:sticky lg:top-28">
                <div class="px-3 py-2 text-[#e9bcb6] text-xs font-bold uppercase tracking-wider">Quản lý tài khoản</div>
                <nav class="flex flex-col gap-1">
                    <a href="${ctx}/customer/profile"
                       class="flex items-center gap-3 px-4 py-3 rounded-lg bg-[#262a35] text-white text-sm font-semibold">
                        <span class="material-symbols-outlined text-[#e50914]">account_circle</span>
                        Thông tin cá nhân
                    </a>
                    <a href="${ctx}/customer/loyalty"
                       class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-[#262a35] hover:text-white text-sm font-semibold">
                        <span class="material-symbols-outlined text-[#ffb95f]">stars</span>
                        Điểm thành viên
                    </a>
                    <a href="${ctx}/customer/vouchers"
                       class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-[#262a35] hover:text-white text-sm font-semibold">
                        <span class="material-symbols-outlined text-[#ffb95f]">confirmation_number</span>
                        Ví voucher
                    </a>
                    <a href="${ctx}/customer/bookings"
                       class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-[#262a35] hover:text-white text-sm font-semibold">
                        <span class="material-symbols-outlined text-[#ffb95f]">receipt_long</span>
                        Lịch sử đặt vé
                    </a>
                    <a href="${ctx}/customer/notifications"
                       class="flex items-center gap-3 px-4 py-3 rounded-lg text-gray-300 hover:bg-[#262a35] hover:text-white text-sm font-semibold">
                        <span class="material-symbols-outlined text-[#ffb95f]">notifications</span>
                        Thông báo
                    </a>
                </nav>
                <div class="h-px bg-white/5 my-2"></div>
                <a href="${ctx}/logout"
                   class="flex items-center gap-3 px-4 py-3 rounded-lg text-[#ffb4ab] hover:bg-[#ffb4ab]/10 text-sm font-semibold transition">
                    <span class="material-symbols-outlined">logout</span>
                    Đăng xuất
                </a>
            </aside>

            <div class="lg:col-span-9 flex flex-col gap-6">
                <div>
                    <h2 class="text-2xl font-bold text-white">Thông tin cá nhân</h2>
                    <p class="text-sm text-[#e9bcb6]">Xem và cập nhật thông tin tài khoản thành viên của bạn.</p>
                </div>

                <!-- Thông tin tài khoản -->
                <section class="bg-[#171b26] border border-white/5 rounded-xl p-6 shadow-[0_8px_24px_-4px_rgba(0,0,0,0.6)]">
                    <h3 class="text-lg font-semibold text-white mb-5 flex items-center gap-2">
                        <span class="material-symbols-outlined text-[#e50914]">badge</span>
                        Thông tin tài khoản
                    </h3>
                    <form action="${ctx}/customer/profile" method="POST" class="grid grid-cols-1 md:grid-cols-2 gap-5" novalidate>
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="action" value="profile"/>

                        <div class="md:col-span-2">
                            <label for="fullName" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                                Họ và tên <span class="text-[#e50914]">*</span>
                            </label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">person</span>
                                <input type="text" id="fullName" name="fullName" value="<c:out value='${form.fullName}'/>"
                                       placeholder="Nhập họ và tên" maxlength="50" autocomplete="name"
                                       class="${inputBase} ${not empty errors.fullName ? 'border-red-500' : 'border-white/10'}"/>
                            </div>
                            <c:if test="${not empty errors.fullName}">
                                <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                                    <span class="material-symbols-outlined text-sm">info</span>
                                    <c:out value="${errors.fullName}"/>
                                </p>
                            </c:if>
                        </div>

                        <div>
                            <label for="email" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">Email</label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500 text-xl">mail</span>
                                <input type="email" id="email" value="<c:out value='${profile.email}'/>" readonly
                                       class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d]/60 border border-white/5 rounded-xl text-gray-400 text-sm cursor-not-allowed focus:outline-none"/>
                            </div>
                            <p class="text-xs text-[#8e919f] mt-1 flex items-center gap-1">
                                <span class="material-symbols-outlined text-sm">lock</span>
                                Email là định danh tài khoản nên không thể thay đổi.
                            </p>
                        </div>

                        <div>
                            <label for="phone" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                                Số điện thoại <span class="text-[#e50914]">*</span>
                            </label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">call</span>
                                <input type="tel" id="phone" name="phone" value="<c:out value='${form.phone}'/>"
                                       placeholder="Nhập số điện thoại (10 chữ số)" maxlength="10" autocomplete="tel"
                                       class="${inputBase} ${not empty errors.phone ? 'border-red-500' : 'border-white/10'}"/>
                            </div>
                            <c:if test="${not empty errors.phone}">
                                <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                                    <span class="material-symbols-outlined text-sm">info</span>
                                    <c:out value="${errors.phone}"/>
                                </p>
                            </c:if>
                        </div>

                        <div class="md:col-span-2 flex justify-end">
                            <button type="submit"
                                    class="px-5 py-2.5 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm flex items-center gap-2 transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.45)]">
                                <span class="material-symbols-outlined text-lg">save</span>
                                Lưu thay đổi
                            </button>
                        </div>
                    </form>
                </section>

                <!-- AF1: Đổi mật khẩu -->
                <section id="change-password" class="bg-[#171b26] border border-white/5 rounded-xl p-6 shadow-[0_8px_24px_-4px_rgba(0,0,0,0.6)]">
                    <h3 class="text-lg font-semibold text-white mb-1 flex items-center gap-2">
                        <span class="material-symbols-outlined text-[#ffb95f]">key</span>
                        Đổi mật khẩu
                    </h3>
                    <p class="text-xs text-[#8e919f] mb-5">
                        Mật khẩu mới từ 8 ký tự, gồm chữ thường, chữ hoa, chữ số và không trùng mật khẩu hiện tại.
                    </p>
                    <form action="${ctx}/customer/profile#change-password" method="POST" class="grid grid-cols-1 md:grid-cols-3 gap-5" novalidate>
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="action" value="password"/>

                        <div>
                            <label for="currentPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                                Mật khẩu hiện tại <span class="text-[#e50914]">*</span>
                            </label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock</span>
                                <input type="password" id="currentPassword" name="currentPassword" autocomplete="current-password"
                                       placeholder="Nhập mật khẩu hiện tại"
                                       class="${inputBase} ${not empty passwordErrors.currentPassword ? 'border-red-500' : 'border-white/10'}"/>
                            </div>
                            <c:if test="${not empty passwordErrors.currentPassword}">
                                <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                                    <span class="material-symbols-outlined text-sm">info</span>
                                    <c:out value="${passwordErrors.currentPassword}"/>
                                </p>
                            </c:if>
                        </div>

                        <div>
                            <label for="newPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                                Mật khẩu mới <span class="text-[#e50914]">*</span>
                            </label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock_reset</span>
                                <input type="password" id="newPassword" name="newPassword" autocomplete="new-password"
                                       placeholder="Tối thiểu 8 ký tự"
                                       class="${inputBase} ${not empty passwordErrors.newPassword ? 'border-red-500' : 'border-white/10'}"/>
                            </div>
                            <c:if test="${not empty passwordErrors.newPassword}">
                                <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                                    <span class="material-symbols-outlined text-sm">info</span>
                                    <c:out value="${passwordErrors.newPassword}"/>
                                </p>
                            </c:if>
                        </div>

                        <div>
                            <label for="confirmPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                                Xác nhận mật khẩu mới <span class="text-[#e50914]">*</span>
                            </label>
                            <div class="relative">
                                <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">verified_user</span>
                                <input type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password"
                                       placeholder="Nhập lại mật khẩu mới"
                                       class="${inputBase} ${not empty passwordErrors.confirmPassword ? 'border-red-500' : 'border-white/10'}"/>
                            </div>
                            <c:if test="${not empty passwordErrors.confirmPassword}">
                                <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                                    <span class="material-symbols-outlined text-sm">info</span>
                                    <c:out value="${passwordErrors.confirmPassword}"/>
                                </p>
                            </c:if>
                        </div>

                        <div class="md:col-span-3 flex justify-end">
                            <button type="submit"
                                    class="px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 text-white font-bold rounded-xl text-sm flex items-center gap-2 transition">
                                <span class="material-symbols-outlined text-lg">key</span>
                                Đổi mật khẩu
                            </button>
                        </div>
                    </form>
                </section>
            </div>
        </div>
    </main>

    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
    <script src="${ctx}/assets/js/profile.js"></script>
</body>
</html>
