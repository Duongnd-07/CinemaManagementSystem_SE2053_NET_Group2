<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Xác nhận hủy vé - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-2xl w-full mx-auto px-4 py-14">
        <section class="rounded-2xl border border-white/10 bg-[#171b26] p-7">
            <p class="text-sm text-[#e9bcb6]">Đơn vé #<c:out value="${ticket.ticketId}"/></p>
            <h1 class="mt-2 text-2xl font-bold text-white">Xác nhận hủy vé</h1>
            <p class="mt-4 leading-7 text-gray-300">
                Vé chỉ được hủy nếu còn ít nhất ${minimumHours} giờ trước giờ chiếu. Khi hủy thành công, ghế được mở bán lại
                và một voucher có giá trị bằng ${voucherPercent}% giá vé sẽ được cấp vào ví của bạn; không hoàn tiền mặt.
            </p>
            <p class="mt-4 text-sm text-gray-400">Thời gian chiếu: <c:out value="${ticket.showtimeStart}"/></p>
            <div class="mt-7 flex flex-wrap gap-3">
                <form action="${ctx}/customer/cancel-ticket" method="post">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"/>
                    <input type="hidden" name="ticketId" value="<c:out value='${ticket.ticketId}'/>"/>
                    <button type="submit" class="rounded-lg bg-[#e50914] px-5 py-3 font-bold text-white hover:bg-[#b80710]">
                        Xác nhận hủy vé
                    </button>
                </form>
                <a href="${ctx}/customer/bookings" class="rounded-lg border border-white/10 px-5 py-3 text-gray-200 hover:bg-white/10">Giữ vé</a>
            </div>
        </section>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
