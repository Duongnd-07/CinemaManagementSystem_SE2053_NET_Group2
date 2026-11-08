<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Lịch sử đặt vé - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-6xl w-full mx-auto px-4 lg:px-8 py-10">
        <div class="flex flex-wrap items-end justify-between gap-4 mb-8">
            <div>
                <p class="text-sm text-[#e9bcb6]">Tài khoản thành viên</p>
                <h1 class="text-3xl font-bold text-white">Lịch sử đặt vé</h1>
            </div>
            <a href="${ctx}/customer/profile" class="text-sm text-[#ffb4ab] hover:text-white">Quay lại hồ sơ</a>
        </div>
        <c:if test="${not empty flashSuccess or not empty flashError}">
            <div role="status" class="mb-5 rounded-xl border p-4 ${not empty flashSuccess ? 'border-emerald-500/30 bg-emerald-950/40 text-emerald-200' : 'border-red-500/30 bg-red-950/40 text-red-200'}">
                <c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/>
            </div>
        </c:if>
        <c:choose>
            <c:when test="${empty history.items}">
                <div class="rounded-2xl border border-white/10 bg-[#171b26] p-12 text-center">
                    <span class="material-symbols-outlined text-5xl text-[#e50914]">confirmation_number</span>
                    <p class="mt-4 text-lg font-semibold text-white"><c:out value="${emptyMessage}"/></p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="space-y-4">
                    <c:forEach var="ticket" items="${history.items}">
                        <article class="rounded-2xl border border-white/10 bg-[#171b26] p-5 md:p-6">
                            <div class="flex flex-wrap items-start justify-between gap-4">
                                <div>
                                    <p class="text-xs uppercase tracking-widest text-[#e9bcb6]">Đơn vé #<c:out value="${ticket.bookingId}"/></p>
                                    <h2 class="mt-1 text-xl font-bold text-white"><c:out value="${ticket.movieTitle}"/></h2>
                                    <p class="mt-2 text-sm text-gray-300">Suất chiếu: <c:out value="${ticket.showtimeStart}"/></p>
                                    <p class="text-sm text-gray-300">Ghế: <c:out value="${ticket.seatLabel}"/> · Mã vé: <c:out value="${ticket.ticketCode}"/></p>
                                </div>
                                <span class="rounded-full bg-white/10 px-3 py-1 text-sm text-gray-200">
                                    <c:choose>
                                        <c:when test="${ticket.ticketStatus == 'Upcoming'}">Sắp chiếu</c:when>
                                        <c:when test="${ticket.ticketStatus == 'Watched'}">Đã xem</c:when>
                                        <c:when test="${ticket.ticketStatus == 'Exchanged'}">Đã đổi</c:when>
                                        <c:otherwise>Đã hủy</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                            <div class="mt-4 grid gap-3 border-t border-white/10 pt-4 md:grid-cols-2">
                                <div>
                                    <c:choose>
                                        <c:when test="${ticket.bookingStatus == 'Paid' and (ticket.ticketStatus == 'Upcoming' or ticket.ticketStatus == 'Watched' or ticket.ticketStatus == 'Exchanged')}">
                                            <p class="text-xs ${ticket.ticketStatus == 'Upcoming' ? 'text-emerald-300' : 'text-gray-400'}">
                                                <c:choose>
                                                    <c:when test="${ticket.ticketStatus == 'Upcoming'}">Mã QR vé · Chưa sử dụng</c:when>
                                                    <c:when test="${ticket.ticketStatus == 'Watched'}">Mã QR vé · Đã sử dụng (đã xem)</c:when>
                                                    <c:otherwise>Mã QR vé · Đã sử dụng (đã đổi)</c:otherwise>
                                                </c:choose>
                                            </p>
                                            <code class="mt-1 block break-all rounded-lg bg-[#0f131d] p-3 text-xs ${ticket.ticketStatus == 'Upcoming' ? 'text-emerald-300' : 'text-gray-400'}"><c:out value="${ticket.qrCode}"/></code>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="text-xs text-gray-400">Mã QR chỉ hiển thị với vé đã thanh toán và chưa bị hủy.</p>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="flex flex-col justify-center gap-2 text-sm text-gray-300">
                                    <p>Trạng thái đơn:
                                        <c:choose>
                                            <c:when test="${ticket.bookingStatus == 'Paid'}">Đã thanh toán</c:when>
                                            <c:when test="${ticket.bookingStatus == 'Pending'}">Đang chờ thanh toán</c:when>
                                            <c:otherwise>Đã hủy</c:otherwise>
                                        </c:choose>
                                    </p>
                                    <p>Giá vé: <fmt:formatNumber value="${ticket.ticketPrice}" pattern="#,##0"/>đ</p>
                                    <p>Đặt lúc: <c:out value="${ticket.bookingTime}"/></p>
                                    <c:if test="${ticket.ticketStatus == 'Upcoming' and ticket.bookingStatus == 'Paid' and ticket.channel == 'Online'}">
                                        <a class="mt-2 inline-flex w-fit items-center rounded-lg border border-red-400/30 px-4 py-2 font-semibold text-[#ffb4ab] hover:bg-red-500/10"
                                           href="${ctx}/customer/cancel-ticket?ticketId=${ticket.ticketId}">Yêu cầu hủy vé</a>
                                    </c:if>
                                    <%-- UC-17/GB-07: suất chiếu đã kết thúc và vé còn hiệu lực -> được đánh giá phim --%>
                                    <c:if test="${ticket.reviewEligible}">
                                        <a class="mt-2 inline-flex w-fit items-center gap-1 rounded-lg border border-[#ffb95f]/30 px-4 py-2 font-semibold text-[#ffb95f] hover:bg-[#ffb95f]/10"
                                           href="${ctx}/customer/reviews?movieId=${ticket.movieId}">
                                            <span class="material-symbols-outlined text-base">star</span>Đánh giá phim
                                        </a>
                                    </c:if>
                                </div>
                            </div>
                        </article>
                    </c:forEach>
                </div>
                <div class="mt-6 flex justify-end gap-5 text-sm">
                    <c:if test="${history.page > 1}"><a class="text-[#ffb4ab]" href="${ctx}/customer/bookings?page=${history.page - 1}">← Trang trước</a></c:if>
                    <span class="text-gray-400">Trang <c:out value="${history.page}"/> / <c:out value="${history.totalPages}"/></span>
                    <c:if test="${history.page < history.totalPages}"><a class="text-[#ffb4ab]" href="${ctx}/customer/bookings?page=${history.page + 1}">Trang sau →</a></c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
