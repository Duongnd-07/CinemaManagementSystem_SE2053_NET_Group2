package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ChangePasswordForm;
import vn.edu.fpt.service.EmailChangeService;
import vn.edu.fpt.service.PendingEmailChange;
import vn.edu.fpt.service.ProfileForm;
import vn.edu.fpt.service.ProfileService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-10: Manage Profile - Khách hàng xem và cập nhật thông tin cá nhân, đổi mật khẩu, đổi email qua OTP
@WebServlet("/customer/profile")
public class ProfileServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/profile.jsp";
    private static final String PATH = "/customer/profile";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String SESSION_PENDING_EMAIL = "pendingEmailChange";
    private static final String ACTION_PASSWORD = "password";
    private static final String ACTION_EMAIL_REQUEST = "email-request";
    private static final String ACTION_EMAIL_RESEND = "email-resend";
    private static final String ACTION_EMAIL_VERIFY = "email-verify";
    private static final String ACTION_EMAIL_CANCEL = "email-cancel";
    private static final String ANCHOR_PASSWORD = "#change-password";
    private static final String ANCHOR_EMAIL = "#change-email";

    private final ProfileService profileService = new ProfileService();
    private final EmailChangeService emailChangeService = new EmailChangeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);
        try {
            User user = profileService.findById(currentUserId(session));
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            render(request, response, user, formOf(user), new ViewErrors());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi tải hồ sơ khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        int userId = currentUserId(session);
        String action = request.getParameter("action");
        action = action == null ? "" : action;
        boolean emailAction = action.startsWith("email-");
        ProfileForm profileForm = readProfileForm(request);

        try {
            String message = switch (action) {
                case ACTION_PASSWORD -> changePassword(request, session, userId);
                case ACTION_EMAIL_REQUEST -> requestEmailChange(request, session, userId);
                case ACTION_EMAIL_RESEND -> resendEmailOtp(session, userId);
                case ACTION_EMAIL_VERIFY -> verifyEmailChange(request, session, userId);
                case ACTION_EMAIL_CANCEL -> cancelEmailChange(session);
                default -> updateProfile(session, userId, profileForm);
            };
            session.setAttribute(FLASH_SUCCESS, message);
            String anchor = emailAction ? ANCHOR_EMAIL : ACTION_PASSWORD.equals(action) ? ANCHOR_PASSWORD : "";
            response.sendRedirect(request.getContextPath() + PATH + anchor);
        } catch (ValidationException e) {
            Map<String, String> errors = e.getErrors();
            if (errors.containsKey("general")) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            if (errors.containsKey(EmailChangeService.ERROR_PENDING)) {
                // Yêu cầu đổi email không thể tiếp tục: hủy để khách bắt đầu lại từ bước nhập email mới
                session.removeAttribute(SESSION_PENDING_EMAIL);
                session.setAttribute(FLASH_ERROR, errors.get(EmailChangeService.ERROR_PENDING));
                response.sendRedirect(request.getContextPath() + PATH + ANCHOR_EMAIL);
                return;
            }
            ViewErrors viewErrors = new ViewErrors();
            if (ACTION_PASSWORD.equals(action)) {
                viewErrors.password.putAll(errors);
            } else if (emailAction) {
                viewErrors.email.putAll(errors);
                viewErrors.newEmail = request.getParameter("newEmail");
            } else {
                viewErrors.profile.putAll(errors);
            }
            redisplay(request, response, userId, ACTION_PASSWORD.equals(action) || emailAction ? null : profileForm,
                    viewErrors);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi cập nhật hồ sơ khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private String updateProfile(HttpSession session, int userId, ProfileForm form)
            throws SQLException, ValidationException {
        User updated = profileService.updateProfile(userId, form);
        // Làm mới user trong session để header hiển thị đúng họ tên mới
        session.setAttribute(AuthFilter.SESSION_USER, updated);
        return Messages.PROFILE_UPDATED;
    }

    private String changePassword(HttpServletRequest request, HttpSession session, int userId)
            throws SQLException, ValidationException {
        User updated = profileService.changePassword(userId, readPasswordForm(request));
        // AF1 bước 4: cấp session ID mới cho phiên hiện tại; các phiên khác bị AuthFilter thu hồi
        // vì mật khẩu băm của chúng không còn khớp với DB
        request.changeSessionId();
        session.setAttribute(AuthFilter.SESSION_USER, updated);
        return Messages.PROFILE_PASSWORD_CHANGED;
    }

    private String requestEmailChange(HttpServletRequest request, HttpSession session, int userId)
            throws SQLException, ValidationException {
        PendingEmailChange pending = emailChangeService.requestChange(userId,
                request.getParameter("newEmail"), request.getParameter("emailPassword"));
        session.setAttribute(SESSION_PENDING_EMAIL, pending);
        return Messages.format(Messages.EMAIL_CHANGE_OTP_SENT, pending.getNewEmail());
    }

    private String resendEmailOtp(HttpSession session, int userId) throws SQLException, ValidationException {
        PendingEmailChange pending = pendingEmail(session);
        emailChangeService.resendOtp(userId, pending);
        return Messages.format(Messages.EMAIL_CHANGE_OTP_RESENT, pending.getNewEmail());
    }

    private String verifyEmailChange(HttpServletRequest request, HttpSession session, int userId)
            throws SQLException, ValidationException {
        User updated = emailChangeService.confirmChange(userId, pendingEmail(session), request.getParameter("otp"));
        session.removeAttribute(SESSION_PENDING_EMAIL);
        session.setAttribute(AuthFilter.SESSION_USER, updated);
        return Messages.EMAIL_CHANGED;
    }

    private String cancelEmailChange(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_EMAIL);
        return Messages.EMAIL_CHANGE_CANCELLED;
    }

    private void redisplay(HttpServletRequest request, HttpServletResponse response, int userId,
                           ProfileForm submittedProfile, ViewErrors errors)
            throws ServletException, IOException {
        try {
            User user = profileService.findById(userId);
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            // Lỗi ở form khác thì form thông tin vẫn hiển thị dữ liệu đang lưu
            ProfileForm form = submittedProfile == null ? formOf(user) : submittedProfile;
            render(request, response, user, form, errors);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi tải hồ sơ khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User user, ProfileForm form,
                        ViewErrors errors) throws ServletException, IOException {
        HttpSession session = request.getSession();
        PendingEmailChange pending = pendingEmail(session);
        request.setAttribute("profile", user);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors.profile);
        request.setAttribute("passwordErrors", errors.password);
        request.setAttribute("emailErrors", errors.email);
        request.setAttribute("newEmail", errors.newEmail);
        request.setAttribute("pendingEmail",
                pending != null && pending.getUserId() == user.getUserId() ? pending.getNewEmail() : null);
        request.setAttribute("today", LocalDate.now().toString());
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private ProfileForm formOf(User user) {
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setDateOfBirth(user.getDateOfBirth() == null ? "" : user.getDateOfBirth().toString());
        form.setGender(user.getGender());
        return form;
    }

    private PendingEmailChange pendingEmail(HttpSession session) {
        return (PendingEmailChange) session.getAttribute(SESSION_PENDING_EMAIL);
    }

    private int currentUserId(HttpSession session) {
        User user = (User) session.getAttribute(AuthFilter.SESSION_USER);
        return user == null ? 0 : user.getUserId();
    }

    private ProfileForm readProfileForm(HttpServletRequest request) {
        ProfileForm form = new ProfileForm();
        form.setFullName(request.getParameter("fullName"));
        form.setPhone(request.getParameter("phone"));
        form.setDateOfBirth(request.getParameter("dateOfBirth"));
        form.setGender(request.getParameter("gender"));
        return form;
    }

    private ChangePasswordForm readPasswordForm(HttpServletRequest request) {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setCurrentPassword(request.getParameter("currentPassword"));
        form.setNewPassword(request.getParameter("newPassword"));
        form.setConfirmPassword(request.getParameter("confirmPassword"));
        return form;
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    // Lỗi validate tách theo từng form để mỗi form chỉ hiện lỗi của chính nó
    private static final class ViewErrors {
        private final Map<String, String> profile = new LinkedHashMap<>();
        private final Map<String, String> password = new LinkedHashMap<>();
        private final Map<String, String> email = new LinkedHashMap<>();
        private String newEmail;
    }
}
