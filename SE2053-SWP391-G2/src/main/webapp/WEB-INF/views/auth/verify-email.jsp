<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực Email - CineLuxe Cinema</title>
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
        .otp-input {
            letter-spacing: 0.5rem;
            font-size: 1.75rem;
            font-weight: 700;
            text-align: center;
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-4 sm:p-6 bg-[#0f131d]">
    <div class="w-full max-w-md">
        <!-- Logo CineLuxe -->
        <div class="text-center mb-8">
            <a href="${pageContext.request.contextPath}/" class="inline-flex items-center gap-2 group">
                <span class="material-symbols-outlined text-4xl text-[#e50914] transition-transform group-hover:scale-110">mark_email_read</span>
                <span class="text-3xl font-extrabold tracking-widest text-white">CINE<span class="text-[#e50914]">LUXE</span></span>
            </a>
        </div>

        <div class="bg-[#171b26] border border-white/10 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-sm">
            <div class="text-center mb-6">
                <div class="inline-flex items-center justify-center w-14 h-14 bg-[#e50914]/15 border border-[#e50914]/30 rounded-full mb-3 text-[#e50914]">
                    <span class="material-symbols-outlined text-3xl">verified_user</span>
                </div>
                <h1 class="text-2xl font-bold text-white">Xác thực tài khoản</h1>
                <p class="text-sm text-[#e9bcb6] mt-2">
                    Mã xác thực 6 chữ số đã được gửi tới hòm thư:<br>
                    <span class="font-semibold text-white underline decoration-[#e50914]"><c:out value="${email}"/></span>
                </p>
            </div>

            <!-- Flash Thông báo thành công / lỗi -->
            <c:if test="${not empty flashSuccess}">
                <div class="mb-5 p-3.5 bg-emerald-950/80 border border-emerald-500/40 rounded-xl text-emerald-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-emerald-400 text-xl">check_circle</span>
                    <span><c:out value="${flashSuccess}"/></span>
                </div>
            </c:if>

            <c:if test="${not empty flashError}">
                <div class="mb-5 p-3.5 bg-red-950/80 border border-red-500/40 rounded-xl text-red-300 text-sm flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-red-400 text-xl">error</span>
                    <span><c:out value="${flashError}"/></span>
                </div>
            </c:if>

            <!-- Form Nhập OTP -->
            <form action="${pageContext.request.contextPath}/verify-email" method="POST" class="space-y-5">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>

                <div>
                    <label for="otp" class="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2 text-center">
                        Nhập mã xác thực (6 số)
                    </label>
                    <input type="text" id="otp" name="otp" maxlength="6" pattern="[0-9]{6}" inputmode="numeric"
                           placeholder="------" required autofocus autocomplete="one-time-code"
                           class="w-full py-3 bg-[#0f131d] border ${not empty errors.otp ? 'border-red-500' : 'border-white/10'} rounded-xl text-white placeholder-gray-600 otp-input focus:outline-none focus:border-[#e50914] focus:ring-2 focus:ring-[#e50914]/30 transition"/>
                    
                    <c:if test="${not empty errors.otp}">
                        <p class="text-xs text-red-400 mt-2 text-center flex items-center justify-center gap-1">
                            <span class="material-symbols-outlined text-sm">warning</span>
                            <c:out value="${errors.otp}"/>
                        </p>
                    </c:if>
                    <c:if test="${not empty errors.general}">
                        <p class="text-xs text-red-400 mt-2 text-center flex items-center justify-center gap-1">
                            <span class="material-symbols-outlined text-sm">warning</span>
                            <c:out value="${errors.general}"/>
                        </p>
                    </c:if>
                </div>

                <button type="submit"
                        class="w-full py-3 bg-[#e50914] hover:bg-[#c0000c] text-white font-semibold rounded-xl transition duration-200 shadow-lg shadow-red-900/40 flex items-center justify-center gap-2">
                    <span class="material-symbols-outlined text-lg">check_circle</span>
                    <span>Xác nhận & Hoàn tất</span>
                </button>
            </form>

            <!-- Form Gửi lại mã OTP (Resend) -->
            <div class="mt-6 pt-5 border-t border-white/5 flex flex-col items-center justify-center gap-2 text-sm text-gray-400">
                <p>Chưa nhận được mã xác thực?</p>
                <form action="${pageContext.request.contextPath}/verify-email" method="POST" id="resendForm">
                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                    <input type="hidden" name="action" value="resend"/>
                    <button type="submit" id="resendBtn"
                            class="text-[#ffb95f] hover:underline font-semibold flex items-center gap-1 disabled:opacity-50 disabled:no-underline disabled:cursor-not-allowed">
                        <span class="material-symbols-outlined text-base">refresh</span>
                        <span id="resendText">Gửi lại mã OTP</span>
                    </button>
                </form>
            </div>

            <!-- Quay lại form đăng ký -->
            <div class="mt-4 text-center">
                <a href="${pageContext.request.contextPath}/register" class="text-xs text-gray-500 hover:text-gray-300 underline">
                    Quay lại trang đăng ký
                </a>
            </div>
        </div>
    </div>

    <script>
        // Đếm ngược 60s cho nút gửi lại mã OTP
        let timeLeft = 60;
        const resendBtn = document.getElementById('resendBtn');
        const resendText = document.getElementById('resendText');

        function startTimer() {
            resendBtn.disabled = true;
            const timer = setInterval(() => {
                if (timeLeft <= 0) {
                    clearInterval(timer);
                    resendBtn.disabled = false;
                    resendText.textContent = "Gửi lại mã OTP";
                } else {
                    resendText.textContent = "Gửi lại mã (" + timeLeft + "s)";
                    timeLeft--;
                }
            }, 1000);
        }

        // Tự động focus ô nhập và lọc chỉ cho nhập số
        const otpInput = document.getElementById('otp');
        otpInput.addEventListener('input', function(e) {
            this.value = this.value.replace(/[^0-9]/g, '');
        });

        startTimer();
    </script>
</body>
</html>
