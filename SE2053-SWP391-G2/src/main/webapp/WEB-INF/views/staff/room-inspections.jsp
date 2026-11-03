<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- UC-35: kiểm tra & dọn phòng chiếu trước mỗi suất chiếu. Trái: suất chiếu hôm nay; phải: bảng kiểm tra (?showtimeId) --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="inputClass" value="form-input bg-surface-container-high text-on-surface px-4 py-2 rounded-lg border border-white/10 focus:border-primary outline-none w-full"/>
<!DOCTYPE html>
<html lang="vi" class="dark">
<head>
    <title>Kiểm tra phòng chiếu - CineLuxe Staff</title>
    <jsp:include page="/WEB-INF/views/common/manager-head.jsp"/>
    <link href="${ctx}/assets/css/cineluxe.css" rel="stylesheet"/>
    <link href="${ctx}/assets/css/room-editor.css?v=2" rel="stylesheet"/>
</head>
<body class="bg-surface font-body-md text-on-surface min-h-screen">
<jsp:include page="/WEB-INF/views/common/staff-header.jsp"/>

<main class="max-w-7xl mx-auto px-space-lg pt-space-lg pb-16">
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
                <span class="material-symbols-outlined text-primary-container">cleaning_services</span>Kiểm tra &amp; dọn phòng chiếu
            </h1>
            <p class="text-body-sm text-on-surface-variant mt-1">
                Kiểm tra ghế và dọn phòng trước mỗi suất chiếu. Ghế hoặc thiết bị hỏng thì báo qua mục Sự cố thiết bị.
            </p>
        </div>
        <c:if test="${urgentCount > 0}">
            <div class="px-4 py-2 rounded-xl bg-amber-500/15 border border-amber-500/40 text-amber-300 font-label-lg flex items-center gap-2">
                <span class="material-symbols-outlined">warning</span><c:out value="${urgentMessage}"/>
            </div>
        </c:if>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)] gap-space-lg items-start">
        <!-- Suất chiếu hôm nay -->
        <section class="bg-surface-container-low border border-white/5 rounded-xl overflow-hidden">
            <h2 class="font-headline-sm text-on-surface px-space-lg py-space-md border-b border-white/5">Suất chiếu hôm nay</h2>
            <ul class="divide-y divide-white/5">
                <c:forEach var="slot" items="${slots}">
                    <c:set var="isSelected" value="${not empty selected and selected.showtimeId == slot.showtimeId}"/>
                    <li>
                        <a href="${ctx}/staff/room-inspections?showtimeId=${slot.showtimeId}"
                           class="block px-space-lg py-space-md transition-colors ${isSelected ? 'bg-primary-container/20' : slot.urgent ? 'bg-amber-500/10 hover:bg-amber-500/15' : 'hover:bg-surface-container'}">
                            <div class="flex items-start justify-between gap-space-sm">
                                <div class="min-w-0">
                                    <div class="font-label-lg text-on-surface">
                                        ${fn:substring(slot.startTime, 11, 16)} - ${fn:substring(slot.endTime, 11, 16)}
                                        · <c:out value="${slot.roomName}"/>
                                    </div>
                                    <div class="text-body-sm text-on-surface-variant truncate"><c:out value="${slot.movieTitle}"/></div>
                                    <div class="text-body-sm text-outline">Mở kiểm tra từ ${fn:substring(slot.windowOpen, 11, 16)}</div>
                                </div>
                                <div class="flex flex-col items-end gap-1 shrink-0">
                                    <jsp:include page="/WEB-INF/views/staff/inspection-status.jsp">
                                        <jsp:param name="status" value="${slot.status}"/>
                                        <jsp:param name="late" value="${slot.late}"/>
                                    </jsp:include>
                                </div>
                            </div>
                        </a>
                    </li>
                </c:forEach>
                <c:if test="${empty slots}">
                    <li class="px-space-lg py-12 text-center text-on-surface-variant">Hôm nay không có suất chiếu nào.</li>
                </c:if>
            </ul>
        </section>

        <!-- Bảng kiểm tra -->
        <section class="bg-surface-container-low border border-white/5 rounded-xl p-space-lg min-w-0">
            <c:choose>
                <c:when test="${empty selected}">
                    <p class="text-on-surface-variant py-12 text-center">Chọn một suất chiếu ở danh sách bên trái để kiểm tra phòng.</p>
                </c:when>
                <c:otherwise>
                    <c:set var="s" value="${selected}"/>
                    <c:set var="seatsValue" value="${not empty draft ? draft.seatsChecked : s.seatsChecked}"/>
                    <c:set var="cleanedValue" value="${not empty draft ? draft.roomCleaned : s.roomCleaned}"/>
                    <c:set var="equipmentValue" value="${not empty draft ? draft.equipmentOk : s.equipmentOk}"/>
                    <c:set var="noteValue" value="${not empty draft ? draft.note : s.note}"/>
                    <c:set var="canSave" value="${not s.inspected and s.windowOpenNow}"/>
                    <c:set var="canEdit" value="${s.editable}"/>

                    <div class="flex flex-wrap items-start justify-between gap-space-md mb-space-md">
                        <div>
                            <h2 class="font-headline-sm text-on-surface"><c:out value="${s.roomName}"/> · ${fn:substring(s.startTime, 11, 16)}</h2>
                            <p class="text-body-sm text-on-surface-variant"><c:out value="${s.movieTitle}"/></p>
                            <p class="text-body-sm text-outline">
                                Khung kiểm tra: ${fn:substring(s.windowOpen, 11, 16)} - ${fn:substring(s.startTime, 11, 16)}
                                (sau ${fn:substring(s.startTime, 11, 16)} vẫn lưu được nhưng ghi nhận chậm)
                            </p>
                        </div>
                        <jsp:include page="/WEB-INF/views/staff/inspection-status.jsp">
                            <jsp:param name="status" value="${s.status}"/>
                            <jsp:param name="late" value="${s.late}"/>
                        </jsp:include>
                    </div>

                    <c:if test="${s.inspected}">
                        <p class="text-body-sm text-on-surface-variant mb-space-md">
                            Kiểm tra bởi <strong class="text-on-surface"><c:out value="${s.staffName}"/></strong>
                            lúc ${fn:substring(s.inspectedAt, 11, 16)}
                            <c:if test="${not empty s.updatedAt}">· sửa lúc ${fn:substring(s.updatedAt, 11, 16)}</c:if>
                            <c:if test="${canEdit}"><span class="text-secondary">· còn sửa / hoàn tác được trong 5 phút sau khi lưu</span></c:if>
                        </p>
                    </c:if>
                    <c:if test="${s.status == 'Missed'}">
                        <div class="form-error-banner mb-space-md">Suất chiếu đã kết thúc mà không được kiểm tra phòng.</div>
                    </c:if>
                    <c:if test="${not s.inspected and s.status == 'NotInspected' and not s.windowOpenNow}">
                        <div class="mb-space-md px-4 py-3 rounded-lg bg-white/5 border border-white/10 text-on-surface-variant text-body-sm">
                            Chưa đến thời gian kiểm tra suất chiếu này. Thời gian kiểm tra bắt đầu lúc
                            ${fn:substring(s.windowOpen, 11, 16)} ${fn:substring(s.windowOpen, 8, 10)}/${fn:substring(s.windowOpen, 5, 7)}.
                        </div>
                    </c:if>

                    <form method="post" action="${ctx}/staff/room-inspections" class="space-y-space-md">
                        <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                        <input type="hidden" name="showtimeId" value="${s.showtimeId}"/>
                        <input type="hidden" name="action" value="${s.inspected ? 'update' : 'save'}"/>
                        <fieldset class="space-y-2" ${canSave or canEdit ? '' : 'disabled'}>
                            <label class="tool"><input type="checkbox" name="seatsChecked" ${seatsValue ? 'checked' : ''}/>
                                <span><strong>Đã kiểm tra ghế</strong> <span class="req">*</span> - kiểm tra từng ghế, ghế hỏng thì báo sự cố</span></label>
                            <label class="tool"><input type="checkbox" name="roomCleaned" ${cleanedValue ? 'checked' : ''}/>
                                <span><strong>Đã dọn dẹp phòng</strong> <span class="req">*</span></span></label>
                            <label class="tool"><input type="checkbox" name="equipmentOk" ${equipmentValue ? 'checked' : ''}/>
                                <span><strong>Thiết bị khác bình thường</strong> (xem nhanh máy chiếu, âm thanh, điều hòa)</span></label>
                            <div>
                                <label class="block text-label-md text-on-surface-variant mb-1" for="note">Ghi chú</label>
                                <textarea class="${inputClass}" id="note" name="note" rows="2" maxlength="${maxNote}"><c:out value="${noteValue}"/></textarea>
                            </div>
                        </fieldset>

                        <!-- A1/A2: báo sự cố qua UC-21 -->
                        <div class="rounded-lg border border-white/10 p-space-md space-y-2">
                            <div class="flex flex-wrap items-center justify-between gap-2">
                                <span class="font-label-lg text-on-surface">Sự cố phát hiện khi kiểm tra</span>
                                <c:if test="${canSave or canEdit}">
                                    <span class="flex items-center gap-3">
                                        <a class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright font-label-md"
                                           href="${ctx}/staff/equipment-reports?add=1&amp;roomId=${s.roomId}&amp;category=Seat" target="_blank" rel="noopener">
                                            <span class="material-symbols-outlined text-[18px]">event_seat</span>Báo ghế hỏng
                                        </a>
                                        <a class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-bright font-label-md"
                                           href="${ctx}/staff/equipment-reports?add=1&amp;roomId=${s.roomId}" target="_blank" rel="noopener">
                                            <span class="material-symbols-outlined text-[18px]">report</span>Báo thiết bị khác
                                        </a>
                                        <a class="text-primary text-body-sm underline" href="${ctx}/staff/room-inspections?showtimeId=${s.showtimeId}">Tải lại</a>
                                    </span>
                                </c:if>
                            </div>
                            <p class="text-body-sm text-outline">
                                Báo sự cố mở ở tab mới. Gửi xong quay lại đây và bấm "Tải lại": các báo cáo bạn gửi cho phòng này
                                sẽ được gắn vào lần kiểm tra khi bấm xác nhận, kết quả thành "Có sự cố".
                            </p>
                            <ul class="text-body-sm space-y-1">
                                <c:forEach var="report" items="${linkedReports}">
                                    <li class="text-on-surface">#SC-${report.reportId} · <c:out value="${report.equipmentCategory}"/>
                                        <c:if test="${not empty report.seatLabel}">ghế <c:out value="${report.seatLabel}"/></c:if>
                                        · <span class="text-emerald-400">đã gắn</span></li>
                                </c:forEach>
                                <c:forEach var="report" items="${pendingReports}">
                                    <li class="text-on-surface">#SC-${report.reportId} · <c:out value="${report.equipmentCategory}"/>
                                        <c:if test="${not empty report.seatLabel}">ghế <c:out value="${report.seatLabel}"/></c:if>
                                        · <span class="text-secondary">sẽ gắn khi ${s.inspected ? 'cập nhật' : 'xác nhận'}</span></li>
                                </c:forEach>
                                <c:if test="${empty linkedReports and empty pendingReports}">
                                    <li class="text-on-surface-variant">Chưa có báo cáo sự cố nào.</li>
                                </c:if>
                            </ul>
                            <c:if test="${affectedTickets > 0}">
                                <p class="text-body-sm text-amber-300">
                                    <c:out value="${affectedMessage}"/>
                                    <a class="underline" href="${ctx}/staff/seat-exchange">Đổi ghế hỏng</a>
                                </p>
                            </c:if>
                        </div>

                        <c:if test="${canSave or canEdit}">
                            <div class="flex justify-end">
                                <button type="submit"
                                        class="px-6 py-2.5 rounded-full bg-primary text-on-primary hover:bg-primary/90 font-label-lg transition-all">
                                    ${s.inspected ? 'Cập nhật kết quả' : 'Xác nhận kiểm tra'}
                                </button>
                            </div>
                        </c:if>
                    </form>

                    <c:if test="${canEdit}">
                        <form method="post" action="${ctx}/staff/room-inspections" class="flex justify-end mt-space-sm"
                              data-confirm="Hoàn tác kết quả kiểm tra? Suất chiếu sẽ trở về Chưa kiểm tra (báo cáo sự cố đã gửi vẫn giữ nguyên).">
                            <input type="hidden" name="csrfToken" value="${csrfToken}"/>
                            <input type="hidden" name="showtimeId" value="${s.showtimeId}"/>
                            <input type="hidden" name="action" value="undo"/>
                            <button type="submit" class="text-body-sm text-error hover:underline">Hoàn tác</button>
                        </form>
                    </c:if>

                    <c:if test="${not empty seatLayout}">
                        <div class="mt-space-lg pt-space-lg border-t border-white/5">
                            <h3 class="font-label-lg text-on-surface mb-space-sm">Sơ đồ ghế</h3>
                            <div class="room-screen">MÀN HÌNH</div>
                            <div class="room-grid-wrap">
                                <div id="inspectionGrid" class="room-grid"
                                     data-layout="<c:out value='${seatLayout.layout}'/>" data-reported="<c:out value='${reportedSeats}'/>"></div>
                            </div>
                            <ul class="room-legend">
                                <li><span class="cell cell-S"></span>Thường</li>
                                <li><span class="cell cell-V"></span>VIP</li>
                                <li><span class="cell cell-C cell-wide"></span>Ghế đôi</li>
                                <li><span class="cell cell-S cell-off"></span>Đang khóa</li>
                                <li><span class="cell cell-S cell-off lock-report"></span>Đang có báo cáo sự cố</li>
                            </ul>
                        </div>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>

<script src="${ctx}/assets/js/manager-common.js"></script>
<script src="${ctx}/assets/js/room-inspection.js"></script>
</body>
</html>
