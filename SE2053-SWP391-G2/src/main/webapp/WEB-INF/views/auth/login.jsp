<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - CineLuxe Cinema</title>
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
        <!-- Logo thương hiệu CineLuxe -->
        <div class="text-center mb-8">
            <a href="${pageContext.request.contextPath}/" class="inline-flex items-center gap-2 group">
                <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">movie</span>
                <span class="text-3xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
            </a>
            <p class="text-sm text-[#e9bcb6] mt-2">Hệ thống Rạp chiếu phim Đẳng cấp Thượng lưu</p>
        </div>

        <!-- Khung Form Đăng nhập -->
        <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-sm">
            <h1 class="text-2xl font-bold text-white mb-2 text-center">Đăng nhập</h1>
            <p class="text-xs text-center text-[#8e919f] mb-6">Đăng nhập tài khoản để tiếp tục trải nghiệm dịch vụ</p>

            <!-- Thông báo Flash Thành công nếu có -->
            <c:if test="${not empty flashSuccess}">
                <div class="mb-5 p-3.5 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-emerald-400 text-xl flex-shrink-0">check_circle</span>
                    <span><c:out value="${flashSuccess}"/></span>
                </div>
            </c:if>

            <!-- Thông báo Flash Lỗi nếu có -->
            <c:if test="${not empty flashError}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl flex-shrink-0">error</span>
                    <span><c:out value="${flashError}"/></span>
                </div>
            </c:if>

            <!-- Thông báo Lỗi xác thực tài khoản/mật khẩu -->
            <c:if test="${not empty errors.auth}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl flex-shrink-0">lock_reset</span>
                    <span><c:out value="${errors.auth}"/></span>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/login" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="returnUrl" value="<c:out value='${form.returnUrl}'/>"/>

                <!-- 1. Email hoặc Số điện thoại -->
                <div>
                    <label for="identifier" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Email hoặc Số điện thoại <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">person</span>
                        <input type="text" id="identifier" name="identifier" value="<c:out value='${form.identifier}'/>"
                               placeholder="vidu@email.com hoặc 0912345678" required autofocus
                               class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border ${not empty errors.identifier || not empty errors.auth ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                    </div>
                    <c:if test="${not empty errors.identifier}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-xs">info</span>
                            <c:out value="${errors.identifier}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 2. Mật khẩu -->
                <div>
                    <div class="flex items-center justify-between mb-1.5">
                        <label for="password" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider">
                            Mật khẩu <span class="text-[#e50914]">*</span>
                        </label>
                    </div>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock</span>
                        <input type="password" id="password" name="password"
                               placeholder="Nhập mật khẩu" required
                               class="w-full pl-11 pr-11 py-2.5 bg-[#0f131d] border ${not empty errors.password || not empty errors.auth ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                        <button type="button" onclick="togglePasswordVisibility()"
                                class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition">
                            <span id="eyeIcon" class="material-symbols-outlined text-xl">visibility</span>
                        </button>
                    </div>
                    <c:if test="${not empty errors.password}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-xs">info</span>
                            <c:out value="${errors.password}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 3. Ghi nhớ đăng nhập & Quên mật khẩu -->
                <div class="flex items-center justify-between pt-1">
                    <label class="flex items-center gap-2 text-xs text-[#8e919f] hover:text-gray-300 cursor-pointer select-none">
                        <input type="checkbox" name="rememberMe" ${form.rememberMe ? 'checked' : ''}
                               class="w-4 h-4 rounded bg-[#0f131d] border-white/20 text-[#e50914] focus:ring-0 focus:ring-offset-0 cursor-pointer"/>
                        <span>Ghi nhớ đăng nhập</span>
                    </label>
                    <a href="${pageContext.request.contextPath}/forgot-password" class="text-xs text-[#ffb95f] hover:text-white transition">
                        Quên mật khẩu?
                    </a>
                </div>

                <!-- 4. Nút Đăng nhập -->
                <button type="submit"
                        class="w-full py-3 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.5)] hover:shadow-[0_0_25px_0_rgba(229,9,20,0.7)] active:scale-[0.99] flex items-center justify-center gap-2 mt-2">
                    <span class="material-symbols-outlined text-lg">login</span>
                    Đăng nhập
                </button>
            </form>

            <!-- Hộp chọn tài khoản test nhanh 4 vai trò (Tiện ích hỗ trợ kiểm thử) -->
            <div class="mt-6 pt-5 border-t border-white/10">
                <div class="flex items-center justify-between mb-2.5">
                    <span class="text-[11px] font-semibold text-[#8e919f] uppercase tracking-wider flex items-center gap-1">
                        <span class="material-symbols-outlined text-sm text-[#ffb95f]">smart_toy</span>
                        Tài khoản mẫu 4 vai trò:
                    </span>
                </div>
                <div class="grid grid-cols-2 gap-2 text-[11px]">
                    <button type="button" onclick="fillTestAccount('admin@cinema.vn', 'Admin@123')"
                            class="p-2 bg-[#0f131d] hover:bg-white/5 border border-white/10 rounded-lg text-left transition text-gray-300 hover:text-white flex items-center justify-between">
                        <div>
                            <div class="font-bold text-red-400">1. Admin</div>
                            <div class="text-[10px] text-gray-400">admin@cinema.vn</div>
                        </div>
                        <span class="material-symbols-outlined text-xs text-gray-400">arrow_forward</span>
                    </button>
                    <button type="button" onclick="fillTestAccount('manager@cinema.vn', 'Manager@123')"
                            class="p-2 bg-[#0f131d] hover:bg-white/5 border border-white/10 rounded-lg text-left transition text-gray-300 hover:text-white flex items-center justify-between">
                        <div>
                            <div class="font-bold text-amber-400">2. Manager</div>
                            <div class="text-[10px] text-gray-400">manager@cinema.vn</div>
                        </div>
                        <span class="material-symbols-outlined text-xs text-gray-400">arrow_forward</span>
                    </button>
                    <button type="button" onclick="fillTestAccount('staff@cinema.vn', 'Staff@123')"
                            class="p-2 bg-[#0f131d] hover:bg-white/5 border border-white/10 rounded-lg text-left transition text-gray-300 hover:text-white flex items-center justify-between">
                        <div>
                            <div class="font-bold text-blue-400">3. Staff</div>
                            <div class="text-[10px] text-gray-400">staff@cinema.vn</div>
                        </div>
                        <span class="material-symbols-outlined text-xs text-gray-400">arrow_forward</span>
                    </button>
                    <button type="button" onclick="fillTestAccount('duongpt@gmail.com', 'Customer@123')"
                            class="p-2 bg-[#0f131d] hover:bg-white/5 border border-white/10 rounded-lg text-left transition text-gray-300 hover:text-white flex items-center justify-between">
                        <div>
                            <div class="font-bold text-emerald-400">4. Customer</div>
                            <div class="text-[10px] text-gray-400">duongpt@gmail.com</div>
                        </div>
                        <span class="material-symbols-outlined text-xs text-gray-400">arrow_forward</span>
                    </button>
                </div>
            </div>

            <!-- Chuyển hướng sang đăng ký -->
            <div class="mt-6 text-center text-xs text-[#8e919f]">
                Chưa có tài khoản thành viên?
                <a href="${pageContext.request.contextPath}/register" class="text-[#e50914] font-semibold hover:underline ml-1">
                    Đăng ký ngay
                </a>
            </div>
        </div>

        <!-- Footer -->
        <div class="text-center text-xs text-[#8e919f] mt-8">
            &copy; 2026 CineLuxe Cinema. Mọi quyền được bảo lưu.
        </div>
    </div>

    <script>
        function togglePasswordVisibility() {
            const passwordInput = document.getElementById('password');
            const eyeIcon = document.getElementById('eyeIcon');
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                eyeIcon.textContent = 'visibility_off';
            } else {
                passwordInput.type = 'password';
                eyeIcon.textContent = 'visibility';
            }
        }

        function fillTestAccount(identifier, password) {
            document.getElementById('identifier').value = identifier;
            document.getElementById('password').value = password;
        }
    </script>
</body>
</html>
