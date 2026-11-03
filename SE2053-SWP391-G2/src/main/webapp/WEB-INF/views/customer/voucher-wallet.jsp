<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Ví voucher - CineLuxe Cinema</title>
    <jsp:include page="/WEB-INF/views/common/public-head.jsp"/>
</head>
<body class="min-h-screen bg-[#0f131d] text-[#dfe2f1] flex flex-col">
    <jsp:include page="/WEB-INF/views/common/public-header.jsp"/>
    <main class="flex-1 max-w-6xl w-full mx-auto px-4 lg:px-8 py-10">
        <div class="flex flex-wrap items-end justify-between gap-4 mb-8">
            <div>
                <p class="text-sm text-[#e9bcb6]">Ưu đãi của bạn</p>
                <h1 class="text-3xl font-bold text-white">Ví voucher</h1>
            </div>
            <a href="${ctx}/customer/profile" class="text-sm text-[#ffb4ab] hover:text-white">Quay lại hồ sơ</a>
        </div>
        <c:choose>
            <c:when test="${empty vouchers}">
                <div class="rounded-2xl border border-white/10 bg-[#171b26] p-12 text-center">
                    <span class="material-symbols-outlined text-5xl text-[#e50914]">confirmation_number</span>
                    <p class="mt-4 text-lg font-semibold text-white"><c:out value="${walletEmptyMessage}"/></p>
                    <p class="mt-2 text-sm text-gray-400">Voucher khuyến mãi và voucher hoàn vé sẽ được lưu tại đây.</p>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="category" items="${voucherCategories}">
                    <c:set var="categoryCount" value="0"/>
                    <c:forEach var="voucher" items="${vouchers}">
                        <c:if test="${voucher.status == category}"><c:set var="categoryCount" value="${categoryCount + 1}"/></c:if>
                    </c:forEach>
                    <c:if test="${categoryCount > 0}">
                        <section class="mb-8">
                            <h2 class="text-xl font-bold text-white mb-4">
                                <c:choose>
                                    <c:when test="${category == 'Active'}">Đang hiệu lực</c:when>
                                    <c:when test="${category == 'Used'}">Đã sử dụng</c:when>
                                    <c:when test="${category == 'Expired'}">Đã hết hạn</c:when>
                                    <c:otherwise>Không còn hiệu lực</c:otherwise>
                                </c:choose>
                                <span class="ml-2 text-sm text-gray-400">(<c:out value="${categoryCount}"/>)</span>
                            </h2>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <c:forEach var="voucher" items="${vouchers}">
                                    <c:if test="${voucher.status == category}">
                                        <article class="rounded-2xl border border-white/10 bg-[#171b26] p-5 ${category != 'Active' ? 'opacity-65' : ''}">
                                            <div class="flex items-start justify-between gap-3">
                                                <div>
                                                    <p class="text-xs uppercase tracking-widest text-[#e9bcb6]">
                                                        <c:out value="${voucher.voucherType == 'Refund80' ? 'Voucher hoàn vé' : 'Khuyến mãi'}"/>
                                                    </p>
                                                    <h3 class="mt-1 text-xl font-bold text-white"><c:out value="${voucher.voucherCode}"/></h3>
                                                </div>
                                                <span class="rounded-full bg-white/10 px-3 py-1 text-xs text-gray-200">
                                                    <c:choose>
                                                        <c:when test="${category == 'Active'}">Đang hiệu lực</c:when>
                                                        <c:when test="${category == 'Used'}">Đã sử dụng</c:when>
                                                        <c:when test="${category == 'Expired'}">Đã hết hạn</c:when>
                                                        <c:otherwise>Không còn hiệu lực</c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </div>
                                            <p class="mt-4 text-2xl font-extrabold text-[#ffb95f]">
                                                <c:choose>
                                                    <c:when test="${voucher.discountType == 'Percent'}"><fmt:formatNumber value="${voucher.discountValue}" pattern="#,##0.##"/>%</c:when>
                                                    <c:otherwise><fmt:formatNumber value="${voucher.discountValue}" pattern="#,##0"/>đ</c:otherwise>
                                                </c:choose>
                                            </p>
                                            <div class="mt-4 space-y-1 text-sm text-gray-300">
                                                <p>Đơn tối thiểu: <fmt:formatNumber value="${voucher.minOrderAmount}" pattern="#,##0"/>đ</p>
                                                <p>Hạn sử dụng: <c:out value="${voucher.expiryDate}"/></p>
                                            </div>
                                        </article>
                                    </c:if>
                                </c:forEach>
                            </div>
                        </section>
                    </c:if>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </main>
    <jsp:include page="/WEB-INF/views/common/public-footer.jsp"/>
</body>
</html>
