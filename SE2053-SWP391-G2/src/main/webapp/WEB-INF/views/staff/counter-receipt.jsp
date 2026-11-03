<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-19 bước 8: hóa đơn và vé của đơn bán tại quầy; nút In dùng hộp thoại in của trình duyệt --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="paymentLabels" value="${{'Cash':'Tiền mặt','Transfer':'Chuyển khoản / QR','Card':'Thẻ (máy POS)'}}"/>
<c:set var="r" value="${receipt}"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Hóa đơn #${r.bookingId} - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        @media print {
            header, .no-print { display: none !important; }
            body, .receipt { background: #fff !important; color: #000 !important; }
            .receipt { border: none !important; box-shadow: none !important; }
        }
    </style>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<div class="no-print"><jsp:include page="/WEB-INF/views/common/staff-header.jsp"/></div>

<main class="max-w-lg mx-auto px-space-lg pt-space-lg pb-16">
    <c:if test="${not empty flashSuccess}">
        <div class="no-print mb-space-md flex items-center gap-space-sm px-space-md py-space-sm rounded-xl bg-emerald-950 border border-emerald-500/40 text-emerald-300">
            <span class="material-symbols-outlined">check_circle</span><c:out value="${flashSuccess}"/>
            <c:if test="${not empty changeDue and changeDue > 0}">
                <span class="ml-auto">Tiền thối: <b><fmt:formatNumber value="${changeDue}" maxFractionDigits="0"/>đ</b></span>
            </c:if>
        </div>
    </c:if>

    <article class="receipt bg-surface-container-low rounded-xl border border-white/10 p-space-lg space-y-space-md">
        <header class="text-center">
            <div class="font-headline-sm tracking-wide">CINELUXE</div>
            <div class="text-body-sm text-on-surface-variant">Hóa đơn #${r.bookingId} ·
                ${fn:substring(r.bookingTime, 11, 16)} ${fn:substring(r.bookingTime, 8, 10)}/${fn:substring(r.bookingTime, 5, 7)}/${fn:substring(r.bookingTime, 0, 4)}</div>
            <div class="text-body-sm text-on-surface-variant">
                Nhân viên: <c:out value="${r.staffName}"/><c:if test="${not empty r.counterNo}"> · Quầy ${fn:substring(r.counterNo, 1, 2)}</c:if>
            </div>
        </header>

        <section class="border-t border-dashed border-white/20 pt-space-md">
            <div class="font-label-lg"><c:out value="${r.movieTitle}"/></div>
            <div class="text-body-sm text-on-surface-variant">
                <c:out value="${r.roomName}"/> · ${fn:substring(r.startTime, 11, 16)} ngày
                ${fn:substring(r.startTime, 8, 10)}/${fn:substring(r.startTime, 5, 7)}/${fn:substring(r.startTime, 0, 4)}
            </div>
            <table class="w-full mt-space-sm text-body-md">
                <c:forEach var="t" items="${r.tickets}">
                    <tr>
                        <td>Ghế <b><c:out value="${t.name}"/></b> <span class="text-body-sm text-on-surface-variant">· <c:out value="${t.code}"/></span></td>
                        <td class="text-right"><fmt:formatNumber value="${t.amount}" maxFractionDigits="0"/>đ</td>
                    </tr>
                </c:forEach>
                <c:forEach var="item" items="${r.concessions}">
                    <tr>
                        <td><c:out value="${item.name}"/> × ${item.quantity}</td>
                        <td class="text-right"><fmt:formatNumber value="${item.amount}" maxFractionDigits="0"/>đ</td>
                    </tr>
                </c:forEach>
            </table>
        </section>

        <section class="border-t border-dashed border-white/20 pt-space-md space-y-1 text-body-md">
            <div class="flex justify-between"><span>Tổng</span><span><fmt:formatNumber value="${r.totalPrice}" maxFractionDigits="0"/>đ</span></div>
            <c:if test="${r.discountAmount > 0}">
                <div class="flex justify-between">
                    <span>Giảm giá<c:if test="${not empty r.voucherCode}"> (voucher <c:out value="${r.voucherCode}"/>)</c:if><c:if test="${r.pointsRedeemed > 0}"> (${r.pointsRedeemed} điểm)</c:if></span>
                    <span>-<fmt:formatNumber value="${r.discountAmount}" maxFractionDigits="0"/>đ</span>
                </div>
            </c:if>
            <div class="flex justify-between text-title-lg"><span>Đã thu</span><span><fmt:formatNumber value="${r.finalAmount}" maxFractionDigits="0"/>đ</span></div>
            <div class="flex justify-between text-body-sm text-on-surface-variant">
                <span>Hình thức</span><span>${paymentLabels[r.paymentMethod]}</span>
            </div>
            <c:if test="${not empty r.paymentReference}">
                <div class="flex justify-between text-body-sm text-on-surface-variant">
                    <span>${r.paymentMethod == 'Card' ? 'Mã chuẩn chi' : 'Mã giao dịch'}</span><span><c:out value="${r.paymentReference}"/></span>
                </div>
            </c:if>
            <c:if test="${not empty r.customerName}">
                <div class="flex justify-between text-body-sm text-on-surface-variant">
                    <span>Khách thành viên</span><span><c:out value="${r.customerName}"/> · +${r.pointsEarned} điểm</span>
                </div>
            </c:if>
        </section>
    </article>

    <div class="no-print flex gap-space-md mt-space-lg">
        <button type="button" onclick="window.print()"
                class="flex-1 flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg">
            <span class="material-symbols-outlined">print</span>In vé & hóa đơn
        </button>
        <a href="${ctx}/staff/counter"
           class="flex-1 flex items-center justify-center gap-space-sm py-3 rounded-full bg-surface-container-highest font-label-lg">
            <span class="material-symbols-outlined">add_shopping_cart</span>Bán đơn mới
        </a>
    </div>
</main>
</body>
</html>
