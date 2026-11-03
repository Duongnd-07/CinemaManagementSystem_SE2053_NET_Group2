<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-36: quét mã QR / nhập mã vé của vé điện tử, in vé giấy. Chỉ dùng được khi nhân viên đang mở ca. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none w-full"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Quét &amp; in vé - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/ticket-print.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<div class="screen-only">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-6xl mx-auto px-space-lg pt-space-lg pb-16">
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="mb-space-lg">
        <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
            <span class="material-symbols-outlined text-primary-container">qr_code_scanner</span>Quét &amp; in vé
        </h1>
        <p class="text-body-sm text-on-surface-variant mt-1">
            Quét mã QR trên vé điện tử khách nhận qua email (hoặc nhập mã vé) để in vé giấy. In vé không thay đổi trạng thái vé.
        </p>
    </div>

    <c:choose>
        <c:when test="${empty shift}">
            <div class="bg-surface-container-low border border-amber-500/40 rounded-xl p-space-xl text-center space-y-space-md">
                <span class="material-symbols-outlined text-[40px] text-amber-400">lock_clock</span>
                <p class="font-headline-sm text-on-surface">Bạn cần mở ca làm việc trước khi soát vé.</p>
                <a class="inline-flex items-center gap-1 px-5 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg"
                   href="${ctx}/staff/counter">
                    <span class="material-symbols-outlined text-[18px]">point_of_sale</span>Mở ca tại Bán vé tại quầy
                </a>
            </div>
        </c:when>
        <c:otherwise>
            <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg mb-space-lg">
                <form id="scanForm" method="get" action="${ctx}/staff/ticket-print" class="space-y-space-sm">
                    <label class="block text-label-md text-on-surface-variant" for="code">Mã vé</label>
                    <div class="flex flex-col md:flex-row gap-space-sm">
                        <textarea class="${inputClass} font-mono" id="code" name="code" rows="2" autofocus autocomplete="off"
                                  placeholder="Quét mã QR bằng máy quét hoặc nhập mã vé (vd TKAB23CD45EF) rồi nhấn Enter"><c:out value="${code}"/></textarea>
                        <div class="flex md:flex-col gap-space-sm shrink-0">
                            <button type="submit" class="px-5 py-2 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg">Tìm vé</button>
                            <button type="button" id="cameraButton"
                                    class="px-5 py-2 rounded-full bg-surface-container-high hover:bg-surface-bright text-on-surface font-label-lg">Quét bằng camera</button>
                        </div>
                    </div>
                    <div id="cameraBox" class="hidden">
                        <div id="cameraView" class="max-w-sm"></div>
                        <p id="cameraMessage" class="text-body-sm text-secondary mt-1" aria-live="polite"></p>
                    </div>
                    <c:if test="${not empty lookupError}"><p class="form-error"><c:out value="${lookupError}"/></p></c:if>
                </form>
            </section>

            <c:if test="${not empty booking}">
                <c:set var="b" value="${booking}"/>
                <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg">
                    <div class="flex flex-wrap items-start justify-between gap-space-md mb-space-md">
                        <div>
                            <h2 class="font-headline-sm text-on-surface"><c:out value="${b.movieTitle}"/>
                                <span class="text-label-md px-2 py-0.5 rounded bg-white/5 border border-white/10"><c:out value="${b.ageRating}"/></span></h2>
                            <p class="text-body-sm text-on-surface-variant">
                                ${fn:substring(b.startTime, 11, 16)} ${fn:substring(b.startTime, 8, 10)}/${fn:substring(b.startTime, 5, 7)}/${fn:substring(b.startTime, 0, 4)}
                                · <c:out value="${b.roomName}"/> (<c:out value="${b.roomFormat}"/>)
                            </p>
                        </div>
                        <div class="text-right text-body-sm text-on-surface-variant">
                            <div>Đơn <strong class="text-on-surface">#${b.bookingId}</strong> · ${b.counterSale ? 'Bán tại quầy' : 'Đặt online'}</div>
                            <div>Khách: <c:out value="${empty b.customerName ? 'Khách vãng lai' : b.customerName}"/></div>
                        </div>
                    </div>

                    <c:if test="${not empty b.blockReason}">
                        <div class="form-error-banner mb-space-md"><c:out value="${b.blockReason}"/></div>
                    </c:if>

                    <form method="post" action="${ctx}/staff/ticket-print" id="printForm">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="action" value="print"/>
                        <input type="hidden" name="bookingId" value="${b.bookingId}"/>
                        <table class="w-full text-left border-collapse">
                            <thead>
                            <tr class="border-b border-white/10 text-on-surface-variant text-label-md uppercase tracking-wider">
                                <th class="py-2 pr-3 w-10">In</th>
                                <th class="py-2 pr-3">Ghế</th>
                                <th class="py-2 pr-3">Mã vé</th>
                                <th class="py-2 pr-3 text-right">Giá</th>
                                <th class="py-2">Trạng thái in</th>
                            </tr>
                            </thead>
                            <tbody class="divide-y divide-white/5 text-body-md">
                            <c:forEach var="t" items="${b.tickets}">
                                <tr class="${t.printable ? '' : 'opacity-70'}">
                                    <td class="py-2 pr-3">
                                        <input type="checkbox" name="ticketIds" value="${t.ticketId}"
                                               ${t.printable ? 'checked' : 'disabled'} aria-label="In vé ghế ${t.seatLabel}"/>
                                    </td>
                                    <td class="py-2 pr-3 font-label-lg">${t.seatLabel}
                                        <span class="text-body-sm text-outline">${t.seatType == 'VIP' ? 'VIP' : t.seatType == 'Sweetbox' ? 'Ghế đôi' : 'Thường'}</span></td>
                                    <td class="py-2 pr-3 font-mono"><c:out value="${t.ticketCode}"/></td>
                                    <td class="py-2 pr-3 text-right"><fmt:formatNumber value="${t.price}" maxFractionDigits="0"/>đ</td>
                                    <td class="py-2 text-body-sm">
                                        <c:choose>
                                            <c:when test="${t.printable}"><span class="text-emerald-400">Chưa in</span></c:when>
                                            <c:otherwise><span class="text-on-surface-variant"><c:out value="${t.blockReason}"/></span></c:otherwise>
                                        </c:choose>
                                        <c:if test="${t.reprintable}">
                                            <button type="button" class="ml-2 text-primary underline" data-reprint="${t.ticketId}"
                                                    data-code="<c:out value='${t.ticketCode}'/>">In lại</button>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                        <c:if test="${not empty b.concessions}">
                            <p class="mt-space-md text-body-sm text-on-surface-variant">
                                Bắp nước của đơn: <c:forEach var="item" items="${b.concessions}" varStatus="st"><c:out value="${item}"/>${st.last ? '' : ', '}</c:forEach>
                            </p>
                        </c:if>
                        <c:if test="${b.anyPrintable}">
                            <div class="flex justify-end mt-space-md">
                                <button type="submit"
                                        class="inline-flex items-center gap-1 px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg">
                                    <span class="material-symbols-outlined text-[18px]">print</span>In vé
                                </button>
                            </div>
                        </c:if>
                    </form>
                </section>

                <%-- A2: hộp thoại in lại, bắt buộc lý do --%>
                <div id="reprintModal" class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 hidden items-center justify-center p-4">
                    <div class="bg-surface-container-low border border-white/10 rounded-2xl w-full max-w-md p-space-xl shadow-2xl">
                        <h3 class="text-headline-md text-on-surface mb-space-md">In lại vé <span id="reprintCode" class="font-mono"></span></h3>
                        <form method="post" action="${ctx}/staff/ticket-print" class="space-y-space-md">
                            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                            <input type="hidden" name="action" value="reprint"/>
                            <input type="hidden" name="bookingId" value="${b.bookingId}"/>
                            <input type="hidden" name="ticketId" id="reprintTicket"/>
                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1" for="reason">Lý do in lại <span class="req">*</span></label>
                                <input class="${inputClass}" id="reason" name="reason" type="text" minlength="${minReason}" maxlength="${maxReason}"
                                       placeholder="VD: Khách làm mất vé giấy" required/>
                                <p class="mt-1 text-body-sm text-outline">Vé in lại có nhãn "BẢN IN LẠI" và được ghi vào nhật ký hệ thống.</p>
                            </div>
                            <div class="flex justify-end gap-space-md">
                                <button type="button" data-close-reprint
                                        class="px-5 py-2.5 rounded-full bg-surface-container-high hover:bg-surface-bright font-label-lg">Hủy bỏ</button>
                                <button type="submit" class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg">In lại</button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</main>
</div>

<%-- Vé giấy của lượt in vừa rồi: chỉ hiện khi in (khổ 80 mm), trang tự mở hộp thoại in --%>
<c:if test="${not empty printJob}">
    <div id="printArea" data-auto-print="true">
        <c:forEach var="t" items="${printJob.tickets}">
            <div class="paper-ticket">
                <div class="pt-brand">CINELUXE CINEMA</div>
                <div class="pt-sub">VÉ XEM PHIM</div>
                <c:if test="${t.reprintNo > 0}"><div class="pt-reprint">BẢN IN LẠI (lần ${t.reprintNo})</div></c:if>
                <div class="pt-movie"><c:out value="${printJob.movieTitle}"/> <span>(<c:out value="${printJob.ageRating}"/>)</span></div>
                <table class="pt-table">
                    <tr><td>Suất chiếu</td><td>${fn:substring(printJob.startTime, 11, 16)} ${fn:substring(printJob.startTime, 8, 10)}/${fn:substring(printJob.startTime, 5, 7)}/${fn:substring(printJob.startTime, 0, 4)}</td></tr>
                    <tr><td>Phòng</td><td><c:out value="${printJob.roomName}"/> (<c:out value="${printJob.roomFormat}"/>)</td></tr>
                    <tr><td>Ghế</td><td class="pt-seat">${t.seatLabel} · ${t.seatType == 'VIP' ? 'VIP' : t.seatType == 'Sweetbox' ? 'Ghế đôi' : 'Thường'}</td></tr>
                    <tr><td>Giá vé</td><td><fmt:formatNumber value="${t.price}" maxFractionDigits="0"/>đ</td></tr>
                    <tr><td>Mã vé</td><td class="pt-code">${t.ticketCode}</td></tr>
                    <tr><td>Mã đơn</td><td>#${printJob.bookingId}</td></tr>
                </table>
                <div class="pt-foot">In lúc ${fn:substring(t.printedAt, 11, 16)} ${fn:substring(t.printedAt, 8, 10)}/${fn:substring(t.printedAt, 5, 7)}/${fn:substring(t.printedAt, 0, 4)} · NV: <c:out value="${t.printedBy}"/></div>
            </div>
        </c:forEach>
        <c:if test="${printJob.concessionSlip}">
            <div class="paper-ticket">
                <div class="pt-brand">CINELUXE CINEMA</div>
                <div class="pt-sub">PHIẾU NHẬN BẮP NƯỚC</div>
                <div class="pt-movie">Đơn #${printJob.bookingId}</div>
                <table class="pt-table">
                    <c:forEach var="item" items="${printJob.concessions}"><tr><td colspan="2"><c:out value="${item}"/></td></tr></c:forEach>
                </table>
                <div class="pt-foot">Đưa phiếu này tại quầy bắp nước để nhận hàng.</div>
            </div>
        </c:if>
    </div>
</c:if>

<script src="${ctx}/assets/js/ticket-print.js"></script>
</body>
</html>
