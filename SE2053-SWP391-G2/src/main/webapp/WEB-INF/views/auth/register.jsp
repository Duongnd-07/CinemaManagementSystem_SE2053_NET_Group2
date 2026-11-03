<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký thành viên - CineLuxe Cinema</title>
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
            <p class="text-sm text-[#e9bcb6] mt-2">Trải nghiệm điện ảnh đỉnh cao & Ưu đãi thành viên</p>
        </div>

        <!-- Khung Form Đăng ký -->
        <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-sm">
            <h1 class="text-2xl font-bold text-white mb-2 text-center">Tạo tài khoản thành viên</h1>
            <p class="text-xs text-center text-[#8e919f] mb-6">Đăng ký để tích điểm đổi vé và nhận ưu đãi độc quyền</p>

            <!-- Thông báo Flash lỗi / thành công nếu có -->
            <c:if test="${not empty flashError}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                    <span><c:out value="${flashError}"/></span>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/register" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>

                <!-- 1. Họ và tên -->
                <div>
                    <label for="fullName" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Họ và tên <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">person</span>
                        <input type="text" id="fullName" name="fullName" value="<c:out value='${form.fullName}'/>"
                               placeholder="Nhập họ và tên" maxlength="50" required
                               class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border ${not empty errors.fullName ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                    </div>
                    <c:if test="${not empty errors.fullName}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.fullName}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 2. Email -->
                <div>
                    <label for="email" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Email <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">mail</span>
                        <input type="email" id="email" name="email" value="<c:out value='${form.email}'/>"
                               placeholder="Nhập địa chỉ email" maxlength="100" required
                               class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border ${not empty errors.email ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                    </div>
                    <c:if test="${not empty errors.email}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.email}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 3. Số điện thoại -->
                <div>
                    <label for="phone" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Số điện thoại <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">call</span>
                        <input type="tel" id="phone" name="phone" value="<c:out value='${form.phone}'/>"
                               placeholder="Nhập số điện thoại (10 chữ số)" maxlength="10" required
                               class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border ${not empty errors.phone ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                    </div>
                    <c:if test="${not empty errors.phone}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.phone}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 4. Mật khẩu -->
                <div>
                    <label for="password" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Mật khẩu <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock</span>
                        <input type="password" id="password" name="password" placeholder="Tạo mật khẩu (tối thiểu 8 ký tự)"
                               required
                               class="w-full pl-11 pr-11 py-2.5 bg-[#0f131d] border ${not empty errors.password ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                        <button type="button" onclick="togglePassword('password', 'pwdToggleIcon')" class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
                            <span id="pwdToggleIcon" class="material-symbols-outlined text-xl">visibility</span>
                        </button>
                    </div>
                    <c:if test="${not empty errors.password}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.password}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 5. Xác nhận mật khẩu -->
                <div>
                    <label for="confirmPassword" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Xác nhận mật khẩu <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">lock_clock</span>
                        <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Nhập lại mật khẩu"
                               required
                               class="w-full pl-11 pr-11 py-2.5 bg-[#0f131d] border ${not empty errors.confirmPassword ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                        <button type="button" onclick="togglePassword('confirmPassword', 'confirmPwdToggleIcon')" class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white">
                            <span id="confirmPwdToggleIcon" class="material-symbols-outlined text-xl">visibility</span>
                        </button>
                    </div>
                    <c:if test="${not empty errors.confirmPassword}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.confirmPassword}"/>
                        </p>
                    </c:if>
                </div>

                <!-- 6. Điều khoản & Chính sách (Agreement - SRS UC-01) -->
                <div class="pt-1">
                    <label class="flex items-start gap-2.5 cursor-pointer">
                        <input type="checkbox" name="agreedTerms" id="agreedTerms" ${form.agreedTerms ? 'checked' : ''}
                               class="mt-0.5 w-4 h-4 rounded border-gray-600 bg-[#0f131d] text-[#e50914] focus:ring-[#e50914] focus:ring-offset-0"/>
                        <span class="text-xs text-gray-300 leading-relaxed">
                            Tôi đồng ý với <a href="#" class="text-[#e50914] hover:underline font-semibold">Điều khoản dịch vụ</a> và <a href="#" class="text-[#e50914] hover:underline font-semibold">Chính sách bảo mật</a> của CineLuxe.
                        </span>
                    </label>
                    <c:if test="${not empty errors.agreedTerms}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-sm">info</span>
                            <c:out value="${errors.agreedTerms}"/>
                        </p>
                    </c:if>
                </div>

                <!-- Nút Submit Đăng ký -->
                <div class="pt-2">
                    <button type="submit"
                            class="w-full py-3 bg-[#e50914] hover:bg-[#c0000c] text-white font-semibold rounded-xl transition duration-200 shadow-lg shadow-red-900/40 flex items-center justify-center gap-2">
                        <span>Đăng ký & Nhận mã OTP</span>
                        <span class="material-symbols-outlined text-lg">arrow_forward</span>
                    </button>
                </div>
            </form>

            <!-- Chuyển sang đăng nhập -->
            <div class="mt-6 text-center text-sm text-gray-400 border-t border-white/5 pt-5">
                <span>Đã có tài khoản?</span>
                <a href="${pageContext.request.contextPath}/login" class="text-[#e50914] hover:underline font-semibold ml-1">Đăng nhập</a>
            </div>
        </div>

        <div class="text-center text-xs text-gray-500 mt-6">
            Bằng cách đăng ký, bạn đồng ý với Điều khoản dịch vụ và Chính sách quyền riêng tư của CineLuxe.
        </div>
    </div>

    <script>
        function togglePassword(inputId, iconId) {
            const input = document.getElementById(inputId);
            const icon = document.getElementById(iconId);
            if (input.type === 'password') {
                input.type = 'text';
                icon.textContent = 'visibility_off';
            } else {
                input.type = 'password';
                icon.textContent = 'visibility';
            }
        }
    </script>
</body>
</html>
