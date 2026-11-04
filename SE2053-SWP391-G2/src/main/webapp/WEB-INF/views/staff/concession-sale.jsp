<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<%-- UC-19: bán riêng bắp nước & combo tại quầy (không kèm vé). Không tra thành viên nên không dùng voucher,
     không đổi điểm, không tích điểm. Servlet chỉ mở trang này khi nhân viên đang có ca --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="paymentLabels" value="${{'Cash':'Tiền mặt','Transfer':'Chuyển khoản / QR'}}"/>
<c:set var="paymentIcons" value="${{'Cash':'payments','Transfer':'qr_code_2'}}"/>
<c:set var="inputClass" value="form-input w-full bg-surface-container-high text-on-surface px-4 rounded-lg border border-white/10 focus:border-primary outline-none"/>
<c:set var="cardClass" value="bg-surface-container-low rounded-xl border border-white/10 p-space-lg"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Bán bắp nước tại quầy - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
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

    <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md mb-space-lg">
        <div>
            <h1 class="text-headline-md text-on-surface flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-amber-400">fastfood</span>Bán bắp nước &amp; combo
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">Khách chỉ mua bắp nước, không mua vé.</p>
        </div>
        <div class="flex items-center gap-space-md bg-surface-container-low border border-white/10 rounded-xl px-space-md py-2">
            <span class="cl-pill cl-pill--success">Ca đang mở</span>
            <span class="text-body-sm">Quầy ${fn:substring(shift.counterNo, 1, 2)} · từ
                ${fn:substring(shift.startedAt, 11, 16)} ${fn:substring(shift.startedAt, 8, 10)}/${fn:substring(shift.startedAt, 5, 7)}</span>
            <a class="flex items-center gap-1 text-error hover:underline font-label-lg" href="${ctx}/staff/shifts">
                <span class="material-symbols-outlined text-[18px]">logout</span>Kết ca
            </a>
        </div>
    </div>

    <c:if test="${shiftEndingSoon}">
        <div class="mb-space-lg flex items-center gap-space-sm px-space-md py-space-sm rounded-xl bg-amber-950/60 border border-amber-500/40 text-amber-200">
            <span class="material-symbols-outlined">alarm</span>
            Ca của bạn sẽ tự kết thúc lúc <b>${fn:substring(shiftEndsAt, 11, 16)}</b>. Hoàn tất đơn đang bán và chuẩn bị đếm két.
        </div>
    </c:if>

    <a class="inline-flex items-center gap-1 text-on-surface-variant hover:text-primary mb-space-md" href="${ctx}/staff/counter">
        <span class="material-symbols-outlined text-[18px]">arrow_back</span>Về bán vé
    </a>

    <form method="post" action="${ctx}/staff/counter" id="saleForm" novalidate
          class="grid lg:grid-cols-[1fr_380px] gap-space-lg items-start">
        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
        <input type="hidden" name="mode" value="concession"/>

        <div class="space-y-space-lg min-w-0">
            <section class="${cardClass}">
                <h2 class="text-title-md mb-space-md flex items-center gap-space-sm">
                    <span class="material-symbols-outlined text-primary-container">fastfood</span>Chọn món
                </h2>
                <c:if test="${not empty errors.concession}"><p class="form-error mb-space-sm"><c:out value="${errors.concession}"/></p></c:if>
                <c:choose>
                    <c:when test="${empty concessions}">
                        <p class="text-body-sm text-on-surface-variant">Hiện không có món nào còn hàng.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="grid sm:grid-cols-2 gap-space-sm">
                            <c:forEach var="item" items="${concessions}">
                                <c:set var="typedQty" value="${form.quantities[item.concessionId]}"/>
                                <div class="flex items-center justify-between gap-space-sm bg-surface-container-high rounded-lg px-space-md py-2">
                                    <div class="min-w-0">
                                        <div class="font-label-lg break-words"><c:out value="${item.name}"/></div>
                                        <c:if test="${not empty item.description}">
                                            <div class="text-body-sm text-on-surface-variant break-words"><c:out value="${item.description}"/></div>
                                        </c:if>
                                        <div class="text-body-sm text-on-surface-variant whitespace-nowrap">
                                            <fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/>đ · còn ${item.maxQuantity}
                                        </div>
                                    </div>
                                    <div class="w-20 shrink-0">
                                        <input class="${inputClass} h-10 text-center" type="number" min="0" max="${item.maxQuantity}"
                                               name="qty_${item.concessionId}" data-price="${item.unitPrice}"
                                               value="${not empty typedQty ? fn:escapeXml(typedQty) : (item.quantity > 0 ? item.quantity : '')}"
                                               placeholder="0" aria-label="Số lượng <c:out value='${item.name}'/>"/>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <div class="flex items-start gap-space-sm px-space-md py-space-sm rounded-xl border border-white/10 bg-surface-container-low text-body-sm text-on-surface-variant">
                <span class="material-symbols-outlined text-[20px]">info</span>
                Đơn chỉ mua bắp nước không tích điểm thành viên, không dùng được voucher và không đổi điểm để trừ tiền.
            </div>
        </div>

        <aside class="${cardClass} space-y-space-md lg:sticky lg:top-20">
            <h2 class="text-title-md flex items-center gap-space-sm">
                <span class="material-symbols-outlined text-primary-container">receipt_long</span>Thanh toán
            </h2>
            <c:choose>
                <c:when test="${not empty quote}">
                    <dl class="space-y-1 text-body-md">
                        <c:forEach var="line" items="${quote.concessions}">
                            <c:if test="${line.quantity > 0}">
                                <div class="flex justify-between gap-space-sm"><dt><c:out value="${line.name}"/> × ${line.quantity}</dt>
                                    <dd class="whitespace-nowrap"><fmt:formatNumber value="${line.subtotal}" maxFractionDigits="0"/>đ</dd></div>
                            </c:if>
                        </c:forEach>
                        <div class="flex justify-between text-title-lg pt-2 border-t border-white/10"><dt>Cần thu</dt>
                            <dd class="text-primary"><fmt:formatNumber value="${quote.finalAmount}" maxFractionDigits="0"/>đ</dd></div>
                    </dl>
                </c:when>
                <c:otherwise>
                    <p class="text-body-sm text-on-surface-variant">Tạm tính: <b class="text-on-surface" id="liveTotal">0đ</b></p>
                    <p class="text-body-sm text-on-surface-variant">Chọn món rồi bấm <b>Tính tiền</b>.</p>
                </c:otherwise>
            </c:choose>

            <fieldset class="space-y-space-sm">
                <legend class="text-label-md text-on-surface-variant mb-1">Hình thức thu tiền</legend>
                <c:forEach var="method" items="${paymentMethods}">
                    <label class="flex items-center gap-space-sm px-space-md py-2 rounded-lg bg-surface-container-high cursor-pointer has-[:checked]:ring-2 has-[:checked]:ring-primary">
                        <input type="radio" name="paymentMethod" value="${method}" ${form.paymentMethod == method ? 'checked' : ''}/>
                        <span class="material-symbols-outlined text-[20px]">${paymentIcons[method]}</span>${paymentLabels[method]}
                    </label>
                </c:forEach>
            </fieldset>
            <div id="cashField" ${form.paymentMethod == 'Cash' ? '' : 'hidden'}>
                <label class="block text-label-md text-on-surface-variant mb-1" for="cashReceived">Tiền khách đưa (đ)</label>
                <input class="${inputClass} h-11" type="text" inputmode="numeric" id="cashReceived" name="cashReceived"
                       value="<c:out value='${form.cashReceived}'/>" placeholder="VD: 100000"/>
                <c:if test="${not empty quote.changeDue}">
                    <p class="mt-1 text-emerald-300">Tiền thối: <b><fmt:formatNumber value="${quote.changeDue}" maxFractionDigits="0"/>đ</b></p>
                </c:if>
            </div>
            <input type="hidden" name="transferNote" value="${form.transferNote}"/>
            <div id="referenceField" class="rounded-lg border border-white/10 bg-surface-container-high p-space-sm text-body-sm"
                 ${form.paymentMethod == 'Transfer' ? '' : 'hidden'}>
                Nội dung chuyển khoản khách cần ghi: <b class="text-primary tracking-wider">${form.transferNote}</b>
                <span class="block text-on-surface-variant">Đợi app ngân hàng báo đã nhận đủ tiền rồi bấm xác nhận thanh toán.</span>
            </div>
            <c:if test="${not empty errors.payment}"><p class="form-error"><c:out value="${errors.payment}"/></p></c:if>

            <button type="submit" name="action" value="quote"
                    class="w-full py-3 rounded-full bg-surface-container-highest hover:bg-surface-bright font-label-lg">Tính tiền</button>
            <button type="submit" name="action" value="confirm" id="confirmButton"
                    ${not empty quote and quote.valid ? '' : 'disabled'}
                    class="w-full flex items-center justify-center gap-space-sm py-3 rounded-full bg-primary-container text-on-primary-container font-label-lg disabled:opacity-40 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined">task_alt</span>Xác nhận đã thu tiền
            </button>
            <p class="text-body-sm text-on-surface-variant" id="confirmHint">
                ${not empty quote and quote.valid ? 'Thu đủ tiền của khách rồi mới bấm xác nhận.' : 'Bấm Tính tiền để kiểm tra đơn trước khi thu tiền.'}
            </p>
            <a class="block text-center text-on-surface-variant hover:text-error font-label-lg"
               href="${ctx}/staff/counter?mode=concession">Hủy giao dịch</a>
        </aside>
    </form>
</main>

<script src="${ctx}/assets/js/counter-sale.js?v=5"></script>
</body>
</html>
