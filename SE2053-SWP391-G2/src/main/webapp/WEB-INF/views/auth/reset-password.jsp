<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu - CineLuxe Cinema</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: #0f131d;
            color: #dfe2f1;
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-4 sm:p-6 bg-[#0f131d]">
    <div class="w-full max-w-md">
        <!-- Logo CineLuxe -->
        <div class="text-center mb-8">
            <a href="${pageContext.request.contextPath}/" class="inline-flex items-center gap-2 group">
                <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">movie</span>
                <span class="text-3xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
            </a>
            <p class="text-xs uppercase tracking-widest text-[#8e919f] mt-1 font-semibold">Trải nghiệm điện ảnh đỉnh cao</p>
        </div>

        <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-sm">
            <div class="text-center mb-6">
                <div class="inline-flex items-center justify-center w-14 h-14 bg-[#e50914]/15 border border-[#e50914]/30 rounded-full mb-3 text-[#e50914]">
                    <span class="material-symbols-outlined text-3xl">key</span>
                </div>
                <h1 class="text-2xl font-bold text-white">Đặt lại mật khẩu</h1>
                <p class="text-sm text-[#8e919f] mt-2">
                    Vui lòng thiết lập mật khẩu mới có độ bảo mật cao cho tài khoản.
                </p>
            </div>

            <!-- Flash Thông báo lỗi chung -->
            <c:if test="${not empty flashError}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                    <span><c:out value="${flashError}"/></span>
                </div>
            </c:if>

            <c:if test="${not empty errors.general}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                    <span><c:out value="${errors.general}"/></span>
                </div>
            </c:if>

            <!-- Form Đặt lại mật khẩu -->
            <form action="${pageContext.request.contextPath}/reset-password" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="token" value="<c:out value='${form.token}'/>"/>

                <!-- 1. Mật khẩu mới -->
                <div>
                    <label for="newPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Mật khẩu mới <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock</span>
                        <input type="password" id="newPassword" name="newPassword"
                               placeholder="Nhập mật khẩu mới (8-32 ký tự)" required autofocus
                               class="w-full pl-11 pr-11 py-2.5 bg-[#0f131d] border ${not empty errors.newPassword ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                        <button type="button" onclick="togglePasswordVisibility('newPassword', 'eyeIcon1')"
                                class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition">
                            <span id="eyeIcon1" class="material-symbols-outlined text-xl">visibility</span>
                        </button>
                    </div>
                    <c:if test="${not empty errors.newPassword}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-xs">info</span>
                            <c:out value="${errors.newPassword}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 2. Xác nhận mật khẩu mới -->
                <div>
                    <label for="confirmPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Xác nhận mật khẩu mới <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock_reset</span>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               placeholder="Nhập lại mật khẩu mới" required
                               class="w-full pl-11 pr-11 py-2.5 bg-[#0f131d] border ${not empty errors.confirmPassword ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                        <button type="button" onclick="togglePasswordVisibility('confirmPassword', 'eyeIcon2')"
                                class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition">
                            <span id="eyeIcon2" class="material-symbols-outlined text-xl">visibility</span>
                        </button>
                    </div>
                    <c:if test="${not empty errors.confirmPassword}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-xs">info</span>
                            <c:out value="${errors.confirmPassword}"/>
                        </p>
                    </c:if>
                </div>

                <!-- Gợi ý độ mạnh mật khẩu -->
                <div class="p-3 bg-[#0f131d] border border-white/5 rounded-xl text-xs text-[#8e919f] space-y-1">
                    <p class="font-semibold text-gray-300">Quy tắc mật khẩu an toàn:</p>
                    <p class="flex items-center gap-1.5"><span class="material-symbols-outlined text-[14px] text-[#ffb95f]">check_circle</span> Độ dài từ 8 đến 32 ký tự</p>
                    <p class="flex items-center gap-1.5"><span class="material-symbols-outlined text-[14px] text-[#ffb95f]">check_circle</span> Chứa ít nhất 1 chữ hoa (A-Z) và 1 chữ thường (a-z)</p>
                    <p class="flex items-center gap-1.5"><span class="material-symbols-outlined text-[14px] text-[#ffb95f]">check_circle</span> Chứa ít nhất 1 chữ số (0-9)</p>
                </div>

                <button type="submit"
                        class="w-full py-3 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.5)] hover:shadow-[0_0_25px_0_rgba(229,9,20,0.7)] active:scale-[0.99] flex items-center justify-center gap-2 pt-3">
                    <span class="material-symbols-outlined text-lg">check_circle</span>
                    Xác nhận đổi mật khẩu
                </button>
            </form>

            <!-- Quay lại đăng nhập -->
            <div class="mt-6 pt-5 border-t border-white/10 text-center">
                <a href="${pageContext.request.contextPath}/login"
                   class="inline-flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-[#ffb95f] transition">
                    <span class="material-symbols-outlined text-sm">arrow_back</span>
                    Hủy bỏ và quay lại Đăng nhập
                </a>
            </div>
        </div>
    </div>

    <script>
        function togglePasswordVisibility(fieldId, iconId) {
            const input = document.getElementById(fieldId);
            const icon = document.getElementById(iconId);
            if (input.type === 'password') {
                input.type = 'text';
                icon.innerText = 'visibility_off';
            } else {
                input.type = 'password';
                icon.innerText = 'visibility';
            }
        }
    </script>
</body>
</html>
