<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-22: nội dung báo cáo một ca, dùng chung cho trang nhân viên (staff/shifts.jsp) và quản lý (manager/shifts.jsp).
     Attribute: report (ShiftReport), formAction, csrfToken, errors, countedCash, cashNote,
     canClose (nhân viên đang trong ca), canForceClose (Manager kết hộ), receiptBase (link hóa đơn, chỉ trang nhân viên) --%>
<c:set var="paymentLabels" value="${{'Cash':'Tiền mặt','Transfer':'Chuyển khoản / QR','Card':'Thẻ (POS)'}}"/>
<c:set var="inputClass" value="form-input w-full bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<c:set var="cardClass" value="bg-surface-container-low rounded-xl border border-white/10 p-space-lg"/>
<c:set var="r" value="${report}"/>
<c:set var="sh" value="${r.shift}"/>

<div class="space-y-space-lg">
    <%-- Thông tin ca --%>
    <div class="${cardClass} flex flex-wrap items-center gap-x-space-lg gap-y-2">
        <span class="text-title-lg">Ca #${sh.shiftId} · Quầy ${fn:substring(sh.counterNo, 1, 2)}</span>
        <span class="text-body-sm text-on-surface-variant">Nhân viên: <b class="text-on-surface"><c:out value="${sh.staffName}"/></b></span>
        <span class="text-body-sm text-on-surface-variant">
            Bắt đầu ${fn:substring(sh.startedAt, 11, 16)} ${fn:substring(sh.startedAt, 8, 10)}/${fn:substring(sh.startedAt, 5, 7)}/${fn:substring(sh.startedAt, 0, 4)}
            <c:choose>
                <c:when test="${r.closed}">· Kết thúc ${fn:substring(sh.endedAt, 11, 16)} ${fn:substring(sh.endedAt, 8, 10)}/${fn:substring(sh.endedAt, 5, 7)}</c:when>
                <c:otherwise>· Tự kết lúc ${fn:substring(r.plannedEnd, 11, 16)}</c:otherwise>
            </c:choose>
        </span>
        <c:choose>
            <c:when test="${not r.closed}"><span class="cl-pill cl-pill--success">Đang mở</span></c:when>
            <c:when test="${r.autoClosed}"><span class="cl-pill cl-pill--warning">Hệ thống tự kết khi hết giờ</span></c:when>
            <c:otherwise><span class="cl-pill cl-pill--muted">Đã kết · <c:out value="${sh.closedByName}"/></span></c:otherwise>
        </c:choose>
    </div>

    <c:if test="${not empty errors.general}">
        <div class="form-error-banner"><c:out value="${errors.general}"/></div>
    </c:if>

    <c:if test="${r.orderCount == 0}">
        <p class="text-body-sm text-on-surface-variant flex items-center gap-space-sm">
            <span class="material-symbols-outlined text-[18px]">info</span>Ca chưa có giao dịch nào, mọi số liệu bằng 0.
        </p>
    </c:if>

    <%-- Doanh thu --%>
    <div class="grid grid-cols-2 lg:grid-cols-6 gap-space-md">
        <div class="${cardClass}"><div class="text-body-sm text-on-surface-variant">Số đơn</div>
            <div class="text-headline-sm">${r.orderCount}</div></div>
        <div class="${cardClass}"><div class="text-body-sm text-on-surface-variant">Số vé</div>
            <div class="text-headline-sm">${r.ticketCount}</div></div>
        <div class="${cardClass}"><div class="text-body-sm text-on-surface-variant">Doanh thu vé</div>
            <div class="text-title-lg"><fmt:formatNumber value="${r.ticketRevenue}" maxFractionDigits="0"/>đ</div></div>
        <div class="${cardClass}"><div class="text-body-sm text-on-surface-variant">Bắp nước (${r.concessionItems} món)</div>
            <div class="text-title-lg"><fmt:formatNumber value="${r.concessionRevenue}" maxFractionDigits="0"/>đ</div></div>
        <div class="${cardClass}"><div class="text-body-sm text-on-surface-variant">Giảm giá (voucher, điểm)</div>
            <div class="text-title-lg">-<fmt:formatNumber value="${r.discountTotal}" maxFractionDigits="0"/>đ</div></div>
        <div class="${cardClass} border-primary/40"><div class="text-body-sm text-on-surface-variant">Thực thu</div>
            <div class="text-title-lg text-primary"><fmt:formatNumber value="${r.collectedTotal}" maxFractionDigits="0"/>đ</div></div>
    </div>

    <div class="grid lg:grid-cols-2 gap-space-lg">
        <%-- Theo hình thức thanh toán --%>
        <section class="${cardClass}">
            <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">account_balance_wallet</span>Theo hình thức thanh toán
            </h2>
            <dl class="space-y-2 text-body-md">
                <div class="flex justify-between"><dt>Tiền mặt</dt><dd><fmt:formatNumber value="${r.cashTotal}" maxFractionDigits="0"/>đ</dd></div>
                <div class="flex justify-between"><dt>Chuyển khoản / QR</dt><dd><fmt:formatNumber value="${r.transferTotal}" maxFractionDigits="0"/>đ</dd></div>
                <div class="flex justify-between"><dt>Thẻ (POS)</dt><dd><fmt:formatNumber value="${r.cardTotal}" maxFractionDigits="0"/>đ</dd></div>
                <div class="flex justify-between text-on-surface-variant"><dt>Hoàn tiền tại quầy</dt><dd><fmt:formatNumber value="${r.refundTotal}" maxFractionDigits="0"/>đ</dd></div>
            </dl>
            <p class="mt-space-md text-body-sm ${r.reconciled ? 'text-emerald-300' : 'text-amber-300'} flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-[18px]">${r.reconciled ? 'check_circle' : 'warning'}</span>
                ${r.reconciled ? 'Khớp: vé + bắp nước - giảm giá = tiền mặt + chuyển khoản + thẻ.'
                               : 'Lệch: tổng theo hình thức thanh toán không khớp doanh thu, cần kiểm tra lại các đơn.'}
            </p>
        </section>

        <%-- Đối chiếu két tiền --%>
        <section class="${cardClass}">
            <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">point_of_sale</span>Đối chiếu két tiền mặt
            </h2>
            <dl class="space-y-2 text-body-md">
                <div class="flex justify-between"><dt>Tiền đầu ca</dt><dd><fmt:formatNumber value="${sh.openingCash}" maxFractionDigits="0"/>đ</dd></div>
                <div class="flex justify-between"><dt>+ Tiền mặt thu trong ca</dt><dd><fmt:formatNumber value="${r.cashTotal}" maxFractionDigits="0"/>đ</dd></div>
                <c:if test="${r.refundTotal > 0}">
                    <div class="flex justify-between"><dt>- Tiền mặt hoàn cho khách</dt><dd><fmt:formatNumber value="${r.refundTotal}" maxFractionDigits="0"/>đ</dd></div>
                </c:if>
                <div class="flex justify-between font-label-lg pt-2 border-t border-white/10"><dt>Phải có trong két</dt>
                    <dd><fmt:formatNumber value="${r.expectedCash}" maxFractionDigits="0"/>đ</dd></div>
                <c:if test="${r.counted}">
                    <div class="flex justify-between"><dt>Đếm được</dt><dd><fmt:formatNumber value="${sh.countedCash}" maxFractionDigits="0"/>đ</dd></div>
                </c:if>
            </dl>
            <c:choose>
                <%-- signum(): EL so BigDecimal bằng equals nên 0.00 == 0 là false --%>
                <c:when test="${r.counted and r.cashDifference.signum() == 0}">
                    <p class="mt-space-md text-emerald-300 flex items-center gap-space-sm"><span class="material-symbols-outlined">check_circle</span>Két khớp</p>
                </c:when>
                <c:when test="${r.counted}">
                    <p class="mt-space-md text-amber-300 flex items-center gap-space-sm">
                        <span class="material-symbols-outlined">warning</span>
                        ${r.cashDifference > 0 ? 'Thừa' : 'Thiếu'} <b><fmt:formatNumber value="${r.cashDifference > 0 ? r.cashDifference : -r.cashDifference}" maxFractionDigits="0"/>đ</b>
                    </p>
                </c:when>
                <c:when test="${r.closed}">
                    <p class="mt-space-md text-on-surface-variant flex items-center gap-space-sm"><span class="material-symbols-outlined">hourglass_empty</span>Chưa đếm két</p>
                </c:when>
            </c:choose>
            <c:if test="${not empty sh.cashNote}">
                <p class="mt-1 text-body-sm text-on-surface-variant">Ghi chú: <c:out value="${sh.cashNote}"/></p>
            </c:if>

            <%-- Kết ca (nhân viên đang trong ca) hoặc nhập số đếm cho ca đã kết chưa đếm --%>
            <c:if test="${canClose or (r.closed and not r.counted)}">
                <form method="post" action="${formAction}" class="mt-space-md space-y-space-sm"
                      onsubmit="return confirm('${canClose ? 'Kết ca và chốt báo cáo? Sau khi kết ca không bán tiếp được.' : 'Lưu số tiền đếm két? Chỉ nhập được một lần.'}');">
                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                    <input type="hidden" name="action" value="${canClose ? 'close' : 'count'}"/>
                    <input type="hidden" name="shiftId" value="${sh.shiftId}"/>
                    <label class="block text-label-md text-on-surface-variant" for="countedCash">Tiền mặt đếm được trong két (đ)</label>
                    <input class="${inputClass} h-11" type="text" inputmode="numeric" id="countedCash" name="countedCash" required
                           value="<c:out value='${countedCash}'/>" placeholder="VD: 2800000"/>
                    <c:if test="${not empty errors.countedCash}"><p class="form-error"><c:out value="${errors.countedCash}"/></p></c:if>
                    <label class="block text-label-md text-on-surface-variant" for="cashNote">Ghi chú (bắt buộc nếu thừa/thiếu)</label>
                    <textarea class="${inputClass} py-2" id="cashNote" name="cashNote" rows="2" maxlength="500"
                              placeholder="VD: Thối nhầm cho khách 50.000đ"><c:out value="${cashNote}"/></textarea>
                    <c:if test="${not empty errors.cashNote}"><p class="form-error"><c:out value="${errors.cashNote}"/></p></c:if>
                    <button type="submit"
                            class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg">
                        <span class="material-symbols-outlined">${canClose ? 'logout' : 'save'}</span>${canClose ? 'Kết ca' : 'Lưu số đếm két'}
                    </button>
                </form>
            </c:if>
            <c:if test="${canForceClose}">
                <form method="post" action="${formAction}" class="mt-space-md"
                      onsubmit="return confirm('Kết ca hộ nhân viên? Số liệu sẽ được chốt, két chưa đếm.');">
                    <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                    <input type="hidden" name="action" value="forceClose"/>
                    <input type="hidden" name="shiftId" value="${sh.shiftId}"/>
                    <button type="submit"
                            class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-error-container text-on-error-container font-label-lg">
                        <span class="material-symbols-outlined">stop_circle</span>Kết ca hộ nhân viên
                    </button>
                </form>
            </c:if>
        </section>
    </div>

    <%-- Đơn trong ca --%>
    <section class="${cardClass}">
        <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
            <span class="material-symbols-outlined text-primary-container">receipt_long</span>Đơn bán trong ca (${r.orderCount})
        </h2>
        <c:choose>
            <c:when test="${empty r.orders}">
                <p class="text-body-sm text-on-surface-variant">Chưa có đơn nào.</p>
            </c:when>
            <c:otherwise>
                <div class="overflow-x-auto">
                    <table class="w-full text-left text-body-md">
                        <thead class="text-label-md text-on-surface-variant border-b border-white/10">
                        <tr><th class="py-2">Đơn</th><th>Giờ</th><th>Ghế</th><th>Khách</th><th>Hình thức</th><th class="text-right">Thực thu</th></tr>
                        </thead>
                        <tbody class="divide-y divide-white/5">
                        <c:forEach var="o" items="${r.orders}">
                            <tr>
                                <td class="py-2">
                                    <c:choose>
                                        <c:when test="${not empty receiptBase}"><a class="text-primary hover:underline" href="${receiptBase}${o.bookingId}">#${o.bookingId}</a></c:when>
                                        <c:otherwise>#${o.bookingId}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>${fn:substring(o.bookingTime, 11, 16)}</td>
                                <td><c:out value="${o.seats}"/></td>
                                <td><c:out value="${empty o.customerName ? 'Khách vãng lai' : o.customerName}"/></td>
                                <td>${paymentLabels[o.paymentMethod]}</td>
                                <td class="text-right"><fmt:formatNumber value="${o.finalAmount}" maxFractionDigits="0"/>đ</td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>
