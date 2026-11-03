<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Điểm thành viên - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-6xl w-full mx-auto px-4 lg:px-8 py-10">
        <div class="flex flex-wrap items-end justify-between gap-4 mb-8">
            <div>
                <p class="text-sm text-[#e9bcb6]">Tài khoản thành viên</p>
                <h1 class="text-3xl font-bold text-white">Điểm thưởng của tôi</h1>
            </div>
            <a href="${ctx}/customer/profile" class="text-sm text-[#ffb4ab] hover:text-white">Quay lại hồ sơ</a>
        </div>
        <section class="rounded-2xl border border-[#e50914]/30 bg-gradient-to-br from-[#32151b] to-[#171b26] p-8 mb-8">
            <p class="text-sm uppercase tracking-widest text-[#e9bcb6]">Số dư điểm hiện tại</p>
            <p class="mt-2 text-5xl font-extrabold text-white"><fmt:formatNumber value="${pointBalance}" pattern="#,##0"/> <span class="text-xl text-[#ffb95f]">điểm</span></p>
            <p class="mt-3 text-sm text-gray-300">Điểm được ghi nhận khi đơn vé thanh toán thành công.</p>
        </section>
        <section class="rounded-2xl border border-white/10 bg-[#171b26] p-6">
            <h2 class="text-xl font-bold text-white mb-5">Lịch sử tích và sử dụng điểm</h2>
            <c:choose>
                <c:when test="${empty history.items}">
                    <div class="rounded-xl bg-[#0f131d] p-8 text-center text-gray-300">
                        <p class="font-semibold"><c:out value="${emptyMessage}"/></p>
                        <p class="mt-2 text-sm">Điểm tích lũy và điểm sử dụng sẽ xuất hiện tại đây sau khi thanh toán.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="overflow-x-auto">
                        <table class="w-full text-sm text-left">
                            <thead class="text-xs uppercase text-gray-400 border-b border-white/10">
                                <tr><th class="py-3 pr-4">Thời gian</th><th class="py-3 pr-4">Giao dịch</th><th class="py-3 pr-4">Đơn vé</th><th class="py-3 text-right">Điểm</th></tr>
                            </thead>
                            <tbody class="divide-y divide-white/5">
                                <c:forEach var="transaction" items="${history.items}">
                                    <tr>
                                        <td class="py-4 pr-4"><c:out value="${transaction.transactionDate}"/></td>
                                        <td class="py-4 pr-4">
                                            <c:choose>
                                                <c:when test="${transaction.transactionType == 'Earned'}">Tích điểm</c:when>
                                                <c:when test="${transaction.transactionType == 'Redeemed'}">Sử dụng điểm</c:when>
                                                <c:otherwise>Điều chỉnh</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="py-4 pr-4">
                                            <c:choose>
                                                <c:when test="${not empty transaction.bookingId}">#<c:out value="${transaction.bookingId}"/></c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="py-4 text-right font-bold ${transaction.points > 0 ? 'text-emerald-300' : 'text-amber-300'}">
                                            <c:if test="${transaction.points > 0}">+</c:if><c:out value="${transaction.points}"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <div class="mt-6 flex justify-between text-sm">
                        <c:if test="${history.page > 1}"><a class="text-[#ffb4ab]" href="${ctx}/customer/loyalty?page=${history.page - 1}">← Trang trước</a></c:if>
                        <span class="ml-auto text-gray-400">Trang <c:out value="${history.page}"/> / <c:out value="${history.totalPages}"/></span>
                        <c:if test="${history.page < history.totalPages}"><a class="ml-5 text-[#ffb4ab]" href="${ctx}/customer/loyalty?page=${history.page + 1}">Trang sau →</a></c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
