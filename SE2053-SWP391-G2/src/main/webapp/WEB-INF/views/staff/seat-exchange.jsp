<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-20: đổi ghế hỏng. Ba chế độ: tìm vé (mặc định) -> phương án cho vé (options) -> phiếu kết quả (record) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="seatLabels" value="${{'Standard':'Ghế thường','VIP':'Ghế VIP','Sweetbox':'Ghế đôi'}}"/>
<c:set var="typeLabels" value="${{'Same':'Đổi ghế cùng hạng','Upgrade':'Nâng lên ghế VIP miễn phí','Downgrade':'Xuống ghế thường, trả tiền chênh lệch','Split':'Ghế đôi chuyển sang 2 ghế cạnh nhau','Refund':'Hoàn tiền vé'}}"/>
<c:set var="inputClass" value="form-input w-full bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<c:set var="cardClass" value="bg-surface-container-low rounded-xl border border-white/10 p-space-lg"/>
<c:set var="chipClass" value="inline-flex items-center px-3 py-1.5 rounded-lg bg-surface-container-high cursor-pointer text-body-md has-[:checked]:bg-primary-container has-[:checked]:text-on-primary-container"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Đổi ghế hỏng - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <style>
        @media print {
            header, .no-print { display: none !important; }
            body, .receipt { background: #fff !important; color: #000 !important; }
        }
    </style>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<div class="no-print"><jsp:include page="/WEB-INF/views/common/staff-header.jsp"/></div>

<main class="max-w-5xl mx-auto px-space-lg pt-space-lg pb-16">
    <c:if test="${not empty flashSuccess or not empty flashError}">
        <div id="toast" role="status"
             class="no-print fixed top-20 right-8 z-[60] max-w-md flex items-start gap-space-sm px-space-lg py-space-md rounded-xl shadow-2xl border
                    ${not empty flashSuccess ? 'bg-emerald-950 border-emerald-500/40 text-emerald-300' : 'bg-error-container border-error/40 text-on-error-container'}">
            <span class="material-symbols-outlined">${not empty flashSuccess ? 'check_circle' : 'error'}</span>
            <span class="font-label-lg"><c:out value="${not empty flashSuccess ? flashSuccess : flashError}"/></span>
        </div>
    </c:if>

    <div class="no-print flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-lg">
        <div>
            <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">event_seat</span>Đổi ghế hỏng
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">Đổi chỗ cho khách khi ghế bị hỏng, hoặc hoàn tiền vé nếu khách không muốn đổi.</p>
        </div>
        <c:if test="${not empty options or not empty record}">
            <a class="inline-flex items-center gap-1 text-on-surface-variant hover:text-primary" href="${ctx}/staff/seat-exchange">
                <span class="material-symbols-outlined text-[18px]">arrow_back</span>Tìm vé khác
            </a>
        </c:if>
    </div>

    <c:choose>
        <%-- ===== Phiếu kết quả ===== --%>
        <c:when test="${not empty record}">
            <article class="receipt ${cardClass} max-w-lg mx-auto space-y-space-md">
                <header class="text-center">
                    <div class="font-headline-sm tracking-wide">CINELUXE</div>
                    <div class="text-body-sm text-on-surface-variant">Phiếu ${record.exchangeType == 'Refund' ? 'hoàn tiền' : 'đổi ghế'} #${record.exchangeId} ·
                        ${fn:substring(record.createdAt, 11, 16)} ${fn:substring(record.createdAt, 8, 10)}/${fn:substring(record.createdAt, 5, 7)}/${fn:substring(record.createdAt, 0, 4)}</div>
                    <div class="text-body-sm text-on-surface-variant">Nhân viên: <c:out value="${record.staffName}"/></div>
                </header>
                <section class="border-t border-dashed border-white/20 pt-space-md space-y-1">
                    <div class="font-label-lg"><c:out value="${record.movieTitle}"/></div>
                    <div class="text-body-sm text-on-surface-variant"><c:out value="${record.roomName}"/> ·
                        ${fn:substring(record.startTime, 11, 16)} ngày ${fn:substring(record.startTime, 8, 10)}/${fn:substring(record.startTime, 5, 7)}</div>
                    <div class="text-body-md mt-space-sm"><b>${typeLabels[record.exchangeType]}</b></div>
                    <div class="text-body-sm text-on-surface-variant">Lý do: <c:out value="${record.reason}"/></div>
                </section>
                <section class="border-t border-dashed border-white/20 pt-space-md space-y-1 text-body-md">
                    <div class="flex justify-between"><span>Vé cũ (ghế ${record.oldSeat})</span>
                        <span class="text-on-surface-variant"><c:out value="${record.oldTicketCode}"/> · ${record.exchangeType == 'Refund' ? 'Đã hủy' : 'Đã đổi'}</span></div>
                    <c:forEach var="t" items="${record.newTickets}">
                        <div class="flex justify-between"><span>Vé mới · ghế <b>${t[0]}</b></span><b>${t[1]}</b></div>
                    </c:forEach>
                    <c:if test="${record.cashRefund > 0}">
                        <div class="flex justify-between text-title-md"><span>Tiền mặt trả khách</span>
                            <span><fmt:formatNumber value="${record.cashRefund}" maxFractionDigits="0"/>đ</span></div>
                    </c:if>
                    <c:if test="${record.pointsReturned > 0}">
                        <div class="flex justify-between"><span>Hoàn lại điểm</span><span>+${record.pointsReturned} điểm</span></div>
                    </c:if>
                    <c:if test="${not empty record.voucherCode}">
                        <div class="flex justify-between"><span>Voucher đền bù <fmt:formatNumber value="${record.voucherValue}" maxFractionDigits="0"/>đ</span>
                            <b><c:out value="${record.voucherCode}"/></b></div>
                        <div class="text-body-sm text-on-surface-variant text-right">Đã thêm vào ví của <c:out value="${record.customerName}"/>, hạn tới
                            ${fn:substring(record.voucherExpiry, 8, 10)}/${fn:substring(record.voucherExpiry, 5, 7)}/${fn:substring(record.voucherExpiry, 0, 4)}</div>
                    </c:if>
                </section>
            </article>
            <div class="no-print flex gap-space-md mt-space-lg max-w-lg mx-auto">
                <button type="button" onclick="window.print()"
                        class="flex-1 flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg">
                    <span class="material-symbols-outlined">print</span>In phiếu
                </button>
                <a href="${ctx}/staff/seat-exchange"
                   class="flex-1 flex items-center justify-center gap-space-sm py-3 rounded-full bg-surface-container-highest font-label-lg">Xong</a>
            </div>
        </c:when>

        <%-- ===== Phương án cho một vé ===== --%>
        <c:when test="${not empty options}">
            <c:set var="tk" value="${options.ticket}"/>
            <div class="${cardClass} mb-space-lg flex flex-wrap items-center gap-x-space-lg gap-y-1">
                <span class="text-title-lg">Vé <c:out value="${tk.ticketCode}"/></span>
                <span class="cl-pill cl-pill--danger">Ghế ${tk.seatLabel} · ${seatLabels[tk.seatType]}</span>
                <span class="text-body-sm text-on-surface-variant"><c:out value="${tk.movieTitle}"/> · <c:out value="${tk.roomName}"/> ·
                    ${fn:substring(tk.startTime, 11, 16)} ngày ${fn:substring(tk.startTime, 8, 10)}/${fn:substring(tk.startTime, 5, 7)}</span>
                <span class="text-body-sm text-on-surface-variant">Khách: <c:out value="${tk.member ? tk.customerName : 'Khách vãng lai'}"/></span>
                <span class="text-body-sm text-on-surface-variant">Giá vé: <fmt:formatNumber value="${tk.price}" maxFractionDigits="0"/>đ</span>
            </div>
            <c:if test="${not empty errors.general}"><div class="form-error-banner mb-space-md"><c:out value="${errors.general}"/></div></c:if>

            <form method="post" action="${ctx}/staff/seat-exchange" class="space-y-space-lg" id="exchangeForm">
                <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                <input type="hidden" name="ticketId" value="${tk.ticketId}"/>
                <input type="hidden" name="code" value="<c:out value='${tk.ticketCode}'/>"/>

                <section class="${cardClass}">
                    <label class="block text-label-md text-on-surface-variant mb-1" for="reason">Tình trạng ghế hỏng <span class="req">*</span></label>
                    <textarea class="${inputClass} py-2" id="reason" name="reason" rows="2" maxlength="500" required
                              placeholder="VD: Gãy tựa lưng, không ngả được"><c:out value="${reason}"/></textarea>
                    <c:if test="${not empty errors.reason}"><p class="form-error"><c:out value="${errors.reason}"/></p></c:if>
                    <p class="text-body-sm text-on-surface-variant mt-1">Ghế ${tk.seatLabel} sẽ bị khóa bảo trì và tạo báo cáo sự cố cho quản lý.</p>
                </section>

                <c:if test="${not empty errors.target}"><div class="form-error-banner"><c:out value="${errors.target}"/></div></c:if>

                <%-- Nhóm ghế theo phương án; value gửi lên dạng "S:12", "U:12", "D:12", "P:12-13" --%>
                <c:forEach var="group" items="${[['S', options.same, 'Ghế cùng hạng (ưu tiên)', true],
                                                 ['U', options.upgrade, 'Hết ghế thường: nâng lên ghế VIP miễn phí', true],
                                                 ['P', options.pairs, 'Hết ghế đôi: chuyển sang 2 ghế cạnh nhau', true],
                                                 ['D', options.downgrade, 'Hết ghế VIP: xuống ghế thường, trả tiền chênh lệch', true]]}">
                    <c:if test="${not empty group[1]}">
                        <section class="${cardClass}">
                            <h2 class="text-title-md mb-1">${group[2]}</h2>
                            <p class="text-body-sm text-on-surface-variant mb-space-md">
                                <c:choose>
                                    <c:when test="${group[0] == 'D'}">Trả khách <b><fmt:formatNumber value="${options.vipDifference.min(options.refundCash)}" maxFractionDigits="0"/>đ</b> tiền mặt chênh lệch (cần đang trong ca)</c:when>
                                    <c:otherwise>Không thu thêm tiền</c:otherwise>
                                </c:choose>
                                <c:if test="${group[3] and options.voucherEligible}"> · tặng voucher <b><fmt:formatNumber value="${options.voucherValue}" maxFractionDigits="0"/>đ</b></c:if>
                            </p>
                            <div class="flex flex-wrap gap-2">
                                <c:forEach var="choice" items="${group[1]}">
                                    <c:set var="val" value="${group[0]}:${choice.value}"/>
                                    <label class="${chipClass}">
                                        <input type="radio" class="sr-only" name="target" value="${val}" ${target == val ? 'checked' : ''}/>
                                        <c:out value="${choice.label}"/><c:if test="${group[0] == 'P'}"><span class="ml-1 text-body-sm opacity-70">(${seatLabels[choice.seatType]})</span></c:if>
                                    </label>
                                </c:forEach>
                            </div>
                        </section>
                    </c:if>
                </c:forEach>

                <c:if test="${not empty options.same or not empty options.upgrade or not empty options.pairs or not empty options.downgrade}">
                    <button type="submit" name="action" value="exchange"
                            onclick="return confirm('Xác nhận đổi ghế cho khách?');"
                            class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg">
                        <span class="material-symbols-outlined">swap_horiz</span>Xác nhận đổi ghế
                    </button>
                </c:if>

                <%-- Khách không bị ép đổi ghế: luôn được chọn hoàn tiền --%>
                    <section class="${cardClass} border-amber-500/40">
                        <h2 class="text-title-md mb-1 flex items-center gap-space-sm text-amber-200">
                            <span class="material-symbols-outlined">warning</span>${options.noSeatLeft ? 'Phòng không còn ghế phù hợp để đổi' : 'Khách không muốn đổi ghế'}
                        </h2>
                        <ul class="text-body-md space-y-1 my-space-md">
                            <li>Hoàn tiền mặt từ két: <b><fmt:formatNumber value="${options.refundCash}" maxFractionDigits="0"/>đ</b>
                                <span class="text-body-sm text-on-surface-variant">(số tiền khách thực trả cho vé, không gồm phần giảm giá voucher và bắp nước)</span></li>
                            <c:if test="${tk.member and options.refundPoints > 0}"><li>Trả lại <b>${options.refundPoints} điểm</b> khách đã dùng</li></c:if>
                            <c:if test="${options.voucherEligible}"><li>Tặng voucher đền bù <b><fmt:formatNumber value="${options.voucherValue}" maxFractionDigits="0"/>đ</b></li></c:if>
                            <li class="text-body-sm text-on-surface-variant">Vé sẽ bị hủy. Cần đang trong ca để lấy tiền trong két.</li>
                        </ul>
                        <button type="submit" name="action" value="refund"
                                onclick="return confirm('Hoàn tiền vé và hủy vé này?');"
                                class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-error-container text-on-error-container font-label-lg">
                            <span class="material-symbols-outlined">currency_exchange</span>Hoàn tiền vé
                        </button>
                    </section>
            </form>
        </c:when>

        <%-- ===== Tìm vé ===== --%>
        <c:otherwise>
            <form method="get" action="${ctx}/staff/seat-exchange" class="${cardClass} flex flex-wrap items-end gap-space-md mb-space-lg">
                <div class="flex-1 min-w-[240px]">
                    <label class="block text-label-md text-on-surface-variant mb-1" for="code">Mã vé của khách</label>
                    <input class="${inputClass} h-11 uppercase" id="code" name="code" maxlength="20" required
                           value="<c:out value='${param.lookup}'/>" placeholder="VD: TKB9KC7C94NQ"/>
                </div>
                <button type="submit" class="h-11 px-space-lg rounded-full bg-primary-container text-on-primary-container font-label-lg">Tìm vé</button>
            </form>

            <section>
                <h2 class="text-title-md mb-1">Vé cần đổi</h2>
                <p class="text-body-sm text-on-surface-variant mb-space-md">Vé của các suất chưa kết thúc đang nằm trên ghế đã báo hỏng.</p>
                <c:choose>
                    <c:when test="${empty affected}">
                        <div class="${cardClass} text-center text-on-surface-variant">Không có vé nào trên ghế hỏng.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="bg-surface-container-low rounded-xl border border-white/10 overflow-x-auto">
                            <table class="w-full text-left text-body-md">
                                <thead class="text-label-md text-on-surface-variant border-b border-white/10">
                                <tr><th class="px-space-md py-2">Mã vé</th><th>Phim</th><th>Suất</th><th>Phòng</th><th>Ghế</th><th>Khách</th><th></th></tr>
                                </thead>
                                <tbody class="divide-y divide-white/5">
                                <c:forEach var="t" items="${affected}">
                                    <tr class="hover:bg-surface-container">
                                        <td class="px-space-md py-2"><c:out value="${t.ticketCode}"/></td>
                                        <td><c:out value="${t.movieTitle}"/></td>
                                        <td>${fn:substring(t.startTime, 11, 16)} ${fn:substring(t.startTime, 8, 10)}/${fn:substring(t.startTime, 5, 7)}</td>
                                        <td><c:out value="${t.roomName}"/></td>
                                        <td>${t.seatLabel} <span class="text-body-sm text-on-surface-variant">(${seatLabels[t.seatType]})</span></td>
                                        <td><c:out value="${t.member ? t.customerName : 'Khách vãng lai'}"/></td>
                                        <td class="px-space-md text-right">
                                            <a class="text-primary hover:underline font-label-lg" href="${ctx}/staff/seat-exchange?code=${t.ticketCode}">Đổi ghế</a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:otherwise>
    </c:choose>
</main>
<script>
    (function () {
        var toast = document.getElementById('toast');
        if (toast) {
            setTimeout(function () { toast.remove(); }, 5000);
        }
    })();
</script>
</body>
</html>
