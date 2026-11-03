<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - CineLuxe Cinema</title>
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
                    <span class="material-symbols-outlined text-3xl">lock_reset</span>
                </div>
                <h1 class="text-2xl font-bold text-white">Quên mật khẩu?</h1>
                <p class="text-sm text-[#8e919f] mt-2">
                    Nhập email đã đăng ký để nhận liên kết khôi phục mật khẩu.
                </p>
            </div>

            <!-- Flash Thông báo thành công -->
            <c:if test="${not empty flashSuccess}">
                <div class="mb-5 p-4 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm">
                    <div class="flex items-center gap-2.5 font-semibold text-emerald-200 mb-1">
                        <span class="material-symbols-outlined text-emerald-400 text-xl">mark_email_read</span>
                        <span>Đã gửi email khôi phục</span>
                    </div>
                    <p class="text-xs text-emerald-300/90 leading-relaxed mt-1">
                        <c:out value="${flashSuccess}"/>
                    </p>
                    <c:if test="${not empty sentEmail}">
                        <div class="mt-2.5 pt-2 border-t border-emerald-500/20 text-xs text-emerald-200">
                            Email nhận: <span class="font-bold underline text-white"><c:out value="${sentEmail}"/></span>
                        </div>
                    </c:if>
                </div>
            </c:if>

            <!-- Flash Thông báo lỗi chung -->
            <c:if test="${not empty flashError}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                    <span><c:out value="${flashError}"/></span>
                </div>
            </c:if>

            <!-- Form Quên mật khẩu -->
            <form action="${pageContext.request.contextPath}/forgot-password" method="POST" class="space-y-4">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>

                <div>
                    <label for="email" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1.5">
                        Địa chỉ Email <span class="text-[#e50914]">*</span>
                    </label>
                    <div class="relative">
                        <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-xl">mail</span>
                        <input type="email" id="email" name="email"
                               value="<c:out value='${form.email}'/>"
                               placeholder="nhap-email@example.com" required autofocus
                               class="w-full pl-11 pr-4 py-2.5 bg-[#0f131d] border ${not empty errors.email ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-500 text-sm focus:outline-none focus:border-[#e50914] focus:ring-1 focus:ring-[#e50914] transition"/>
                    </div>
                    <c:if test="${not empty errors.email}">
                        <p class="text-xs text-red-400 mt-1 flex items-center gap-1">
                            <span class="material-symbols-outlined text-xs">info</span>
                            <c:out value="${errors.email}"/>
                        </p>
                    </c:if>
                </div>

                <button type="submit"
                        class="w-full py-3 bg-[#e50914] hover:bg-[#b80710] text-white font-bold rounded-xl text-sm transition shadow-[0_0_20px_-2px_rgba(229,9,20,0.5)] hover:shadow-[0_0_25px_0_rgba(229,9,20,0.7)] active:scale-[0.99] flex items-center justify-center gap-2 pt-3">
                    <span class="material-symbols-outlined text-lg">send</span>
                    Gửi link khôi phục
                </button>
            </form>

            <!-- Quay lại đăng nhập -->
            <div class="mt-6 pt-5 border-t border-white/10 text-center">
                <a href="${pageContext.request.contextPath}/login"
                   class="inline-flex items-center gap-1.5 text-xs text-[#8e919f] hover:text-[#ffb95f] transition">
                    <span class="material-symbols-outlined text-sm">arrow_back</span>
                    Quay lại màn hình đăng nhập
                </a>
            </div>
        </div>
    </div>
</body>
</html>
