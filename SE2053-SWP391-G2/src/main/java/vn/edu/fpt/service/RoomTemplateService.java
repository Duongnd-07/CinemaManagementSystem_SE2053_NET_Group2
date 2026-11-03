package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.RoomTemplateDAO;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.RoomTemplate;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatType;
import vn.edu.fpt.util.Messages;

// UC-25 (A3-A5): phòng mẫu. Dùng lại RoomLayoutForm và trình chỉnh sửa sơ đồ ghế của phòng chiếu;
// với phòng mẫu, form.roomId là TemplateID, form.name là tên phòng mẫu, form.format là định dạng gợi ý.
// Tạo phòng từ mẫu là chép mẫu tại thời điểm đó: sửa/xóa mẫu sau này không ảnh hưởng phòng đã tạo.
public class RoomTemplateService {
    public static final String TEMPLATE_SAVED = "Đã lưu phòng mẫu.";
    public static final String TEMPLATE_DELETED = "Đã xóa phòng mẫu.";
    public static final String TEMPLATE_NAME_DUPLICATE = "Tên phòng mẫu đã tồn tại trong hệ thống.";
    public static final String TEMPLATE_NOT_FOUND = "Phòng mẫu không còn tồn tại. Vui lòng tải lại trang.";
    private static final String TEMPLATE_NAME_LABEL = "Tên phòng mẫu";
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final RoomTemplateDAO templateDAO = new RoomTemplateDAO();
    private final RoomLayoutService layoutService = new RoomLayoutService();

    // Danh sách kèm số ghế / sức chứa tính từ layout
    public List<RoomTemplate> listTemplates() throws SQLException {
        List<RoomTemplate> templates = templateDAO.findAll();
        for (RoomTemplate template : templates) {
            Map<String, Seat> seats = layoutService.validateLayout(String.valueOf(template.getRows()),
                    String.valueOf(template.getCols()), template.getLayout(), new HashMap<>());
            if (seats != null) {
                template.setSeatCount(seats.size());
                template.setCapacity(seats.values().stream().mapToInt(s -> SeatType.capacityOf(s.getSeatType())).sum());
            }
        }
        return templates;
    }

    // Phòng mẫu mới: lưới mặc định 8 x 12 ghế thường (giống phòng mới)
    public RoomLayoutForm newForm() {
        RoomLayoutForm form = layoutService.newForm();
        form.setStatus("");
        return form;
    }

    public RoomLayoutForm loadForm(int templateId) throws SQLException, ValidationException {
        RoomTemplate template = find(templateId);
        RoomLayoutForm form = new RoomLayoutForm();
        form.setRoomId(template.getTemplateId());
        form.setName(template.getTemplateName());
        form.setFormat(template.getFormat() == null ? "" : template.getFormat());
        form.setRows(String.valueOf(template.getRows()));
        form.setCols(String.valueOf(template.getCols()));
        form.setLayout(template.getLayout());
        return form;
    }

    // A3: form Thêm phòng điền sẵn lưới và định dạng gợi ý của mẫu; tên để trống, trạng thái Hoạt động
    public RoomLayoutForm newRoomFromTemplate(int templateId) throws SQLException, ValidationException {
        RoomTemplate template = find(templateId);
        RoomLayoutForm form = layoutService.newForm();
        if (template.getFormat() != null && MovieService.FORMATS.contains(template.getFormat())) {
            form.setFormat(template.getFormat());
        }
        form.setStatus(RoomStatus.ACTIVE);
        form.setRows(String.valueOf(template.getRows()));
        form.setCols(String.valueOf(template.getCols()));
        form.setLayout(template.getLayout());
        return form;
    }

    // Thêm (form.roomId <= 0) hoặc sửa phòng mẫu; trả về TemplateID
    public int save(RoomLayoutForm form) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getRoomId() > 0 && templateDAO.findById(form.getRoomId()) == null) {
            throw notFound();
        }
        validateInfo(form, errors);
        Map<String, Seat> seats = layoutService.validateLayout(form.getRows(), form.getCols(), form.getLayout(), errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        RoomTemplate template = toTemplate(form, seats);
        try {
            if (form.getRoomId() <= 0) {
                return templateDAO.insert(template);
            }
            template.setTemplateId(form.getRoomId());
            if (!templateDAO.update(template)) {
                throw notFound();
            }
            return template.getTemplateId();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    // A4: lưu sơ đồ ghế hiện tại của một phòng thành phòng mẫu mới; trả về TemplateID
    public int saveFromRoom(int roomId, String name) throws SQLException, ValidationException {
        RoomLayoutForm source = layoutService.loadTemplateSource(roomId);
        RoomLayoutForm form = new RoomLayoutForm();
        form.setName(name);
        form.setFormat(source.getFormat());
        form.setRows(source.getRows());
        form.setCols(source.getCols());
        form.setLayout(source.getLayout());
        if (form.getFormat().isEmpty()) {
            form.setFormat(MovieService.FORMATS.get(0));
        }
        return save(form);
    }

    public String delete(int templateId) throws SQLException, ValidationException {
        RoomTemplate template = find(templateId);
        if (!templateDAO.delete(templateId)) {
            throw notFound();
        }
        return template.getTemplateName();
    }

    private RoomTemplate find(int templateId) throws SQLException, ValidationException {
        RoomTemplate template = templateId > 0 ? templateDAO.findById(templateId) : null;
        if (template == null) {
            throw notFound();
        }
        return template;
    }

    // Tên phòng mẫu: bắt buộc, tối đa 50 ký tự, không trùng giữa các phòng mẫu; định dạng gợi ý là 2D/3D/IMAX
    private void validateInfo(RoomLayoutForm form, Map<String, String> errors) throws SQLException {
        String name = form.getName() == null ? "" : form.getName().trim();
        String format = form.getFormat() == null ? "" : form.getFormat().trim();
        form.setName(name);
        form.setFormat(format);
        if (name.isEmpty()) {
            errors.put("name", Messages.format(Messages.MSG02, TEMPLATE_NAME_LABEL));
        } else if (name.length() > RoomLayoutService.MAX_NAME) {
            errors.put("name", Messages.format(Messages.MSG08, RoomLayoutService.MAX_NAME));
        } else if (templateDAO.existsByName(name, Math.max(form.getRoomId(), 0))) {
            errors.put("name", TEMPLATE_NAME_DUPLICATE);
        }
        if (!MovieService.FORMATS.contains(format)) {
            errors.put("format", Messages.ROOM_FORMAT_INVALID);
        }
    }

    // Lưu lại đúng chuỗi layout đã chuẩn hóa (trim) để mở lại không bị lệch
    private RoomTemplate toTemplate(RoomLayoutForm form, Map<String, Seat> seats) {
        RoomTemplate template = new RoomTemplate();
        template.setTemplateName(form.getName());
        template.setFormat(form.getFormat());
        template.setRows(Integer.parseInt(form.getRows().trim()));
        template.setCols(Integer.parseInt(form.getCols().trim()));
        template.setLayout(form.getLayout().trim());
        template.setSeatCount(seats.size());
        return template;
    }

    private SQLException translate(SQLException e) throws ValidationException {
        if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
            throw new ValidationException(Map.of("name", TEMPLATE_NAME_DUPLICATE));
        }
        return e;
    }

    private ValidationException notFound() {
        return new ValidationException(Map.of("general", TEMPLATE_NOT_FOUND));
    }
}
