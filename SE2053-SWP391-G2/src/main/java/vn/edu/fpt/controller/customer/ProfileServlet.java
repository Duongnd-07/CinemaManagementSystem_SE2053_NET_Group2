package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ChangePasswordForm;
import vn.edu.fpt.service.ProfileForm;
import vn.edu.fpt.service.ProfileService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-10: Manage Profile - Khách hàng xem và cập nhật thông tin cá nhân, đổi mật khẩu
@WebServlet("/customer/profile")
public class ProfileServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/profile.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String ACTION_PASSWORD = "password";

    private final ProfileService profileService = new ProfileService();

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
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setPhone(user.getPhone());
            render(request, response, user, form, Map.of(), Map.of());
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
        boolean passwordAction = ACTION_PASSWORD.equals(request.getParameter("action"));
        ProfileForm profileForm = readProfileForm(request);

        try {
            User updated;
            String message;
            if (passwordAction) {
                updated = profileService.changePassword(userId, readPasswordForm(request));
                // AF1 bước 4: cấp session ID mới cho phiên hiện tại; các phiên khác bị AuthFilter thu hồi
                // vì mật khẩu băm của chúng không còn khớp với DB
                request.changeSessionId();
                message = Messages.PROFILE_PASSWORD_CHANGED;
            } else {
                updated = profileService.updateProfile(userId, profileForm);
                message = Messages.PROFILE_UPDATED;
            }
            // Làm mới user trong session để header hiển thị đúng họ tên mới
            session.setAttribute(AuthFilter.SESSION_USER, updated);
            session.setAttribute(FLASH_SUCCESS, message);
            response.sendRedirect(request.getContextPath() + "/customer/profile");
        } catch (ValidationException e) {
            if (e.getErrors().containsKey("general")) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            redisplay(request, response, userId, passwordAction, profileForm, e.getErrors());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi cập nhật hồ sơ khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void redisplay(HttpServletRequest request, HttpServletResponse response, int userId,
                           boolean passwordAction, ProfileForm submittedProfile, Map<String, String> errors)
            throws ServletException, IOException {
        try {
            User user = profileService.findById(userId);
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            ProfileForm form = submittedProfile;
            if (passwordAction) {
                // Lỗi ở form đổi mật khẩu thì form thông tin vẫn hiển thị dữ liệu đang lưu
                form = new ProfileForm();
                form.setFullName(user.getFullName());
                form.setPhone(user.getPhone());
            }
            render(request, response, user, form,
                    passwordAction ? Map.of() : errors,
                    passwordAction ? errors : Map.of());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi tải hồ sơ khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User user, ProfileForm form,
                        Map<String, String> profileErrors, Map<String, String> passwordErrors)
            throws ServletException, IOException {
        request.setAttribute("profile", user);
        request.setAttribute("form", form);
        request.setAttribute("errors", new LinkedHashMap<>(profileErrors));
        request.setAttribute("passwordErrors", new LinkedHashMap<>(passwordErrors));
        request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private int currentUserId(HttpSession session) {
        User user = (User) session.getAttribute(AuthFilter.SESSION_USER);
        return user == null ? 0 : user.getUserId();
    }

    private ProfileForm readProfileForm(HttpServletRequest request) {
        ProfileForm form = new ProfileForm();
        form.setFullName(request.getParameter("fullName"));
        form.setPhone(request.getParameter("phone"));
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
}
