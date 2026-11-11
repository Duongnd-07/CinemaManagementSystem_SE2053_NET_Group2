<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="rooms" required="true" type="java.util.List" description="Danh sách phòng chiếu (chỉ hiện phòng đang hoạt động)" %>
<%@ attribute name="name" required="true" description="Tên tham số gửi lên server, mỗi phòng được chọn là 1 giá trị" %>
<%@ attribute name="pickerId" required="true" description="id của khung chọn phòng, JS dùng để đọc các phòng đã chọn" %>
<%@ attribute name="selectedIds" type="java.util.List" description="Mã phòng (chuỗi) đã chọn khi hiển thị lại form bị lỗi" %>
<%-- UC-24: chọn nhiều phòng chiếu cùng lúc khi lên lịch suất chiếu --%>
<div id="${pickerId}" data-room-picker>
    <div class="flex flex-wrap items-center justify-between gap-2 mb-1.5">
        <span class="text-label-md text-on-surface-variant font-medium">
            Phòng chiếu <span class="text-primary">*</span>
            <span class="text-outline font-normal ml-1" data-room-count>(chưa chọn phòng nào)</span>
        </span>
        <div class="flex items-center gap-2 text-label-sm">
            <button type="button" class="text-primary hover:underline" data-room-select="matching"
                    title="Chọn mọi phòng có định dạng phim hỗ trợ">Chọn tất cả phòng phù hợp</button>
            <span class="text-outline">·</span>
            <button type="button" class="text-primary hover:underline" data-room-select="none">Bỏ chọn</button>
        </div>
    </div>
    <div role="group" aria-label="Chọn phòng chiếu" class="grid grid-cols-2 md:grid-cols-3 gap-2">
        <c:set var="activeRoomCount" value="0"/>
        <c:forEach var="room" items="${rooms}">
            <c:if test="${room.status == 'Active'}">
                <c:set var="activeRoomCount" value="${activeRoomCount + 1}"/>
                <c:set var="roomFormat" value="${empty room.format ? '2D' : room.format}"/>
                <label data-room-card
                       class="flex items-start gap-2.5 p-2.5 rounded-lg bg-surface-container-high border border-white/5 cursor-pointer transition hover:border-primary/40 has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                    <input type="checkbox" name="${name}" value="${room.roomId}" class="accent-primary mt-1 shrink-0"
                           data-format="<c:out value='${roomFormat}'/>" data-name="<c:out value='${room.roomName}'/>"
                           ${not empty selectedIds and selectedIds.contains(''.concat(room.roomId)) ? 'checked' : ''}/>
                    <span class="min-w-0">
                        <span class="block text-body-sm font-bold text-on-surface truncate"><c:out value="${room.roomName}"/></span>
                        <span class="block text-[11px] text-outline"><c:out value="${roomFormat}"/> · ${room.totalSeats} ghế</span>
                        <span class="hidden text-[11px] text-rose-300" data-room-mismatch>Phim không hỗ trợ định dạng này</span>
                    </span>
                </label>
            </c:if>
        </c:forEach>
        <c:if test="${activeRoomCount == 0}">
            <p class="col-span-full text-body-sm text-outline">Chưa có phòng chiếu nào đang hoạt động.</p>
        </c:if>
    </div>
    <p class="hidden text-rose-400 text-label-sm mt-1" data-room-required>Vui lòng chọn ít nhất một phòng chiếu.</p>
</div>
