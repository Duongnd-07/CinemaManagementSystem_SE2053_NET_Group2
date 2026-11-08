package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.OnlineSalesDetail;
import vn.edu.fpt.model.OnlineSalesFilter;
import vn.edu.fpt.model.OnlineSalesResult;
import vn.edu.fpt.service.OnlineSalesReportService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.Messages;

// Every action uses this exact permission-mapped servlet path, including CSV and detail.
@WebServlet("/manager/online-reports")
public class OnlineSalesReportServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(OnlineSalesReportServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/online-reports.jsp";
    private final OnlineSalesReportService service = new OnlineSalesReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        Map<String, String> raw = new LinkedHashMap<>();
        for (String key : List.of("from", "to", "page", "groupBy")) {
            raw.put(key, request.getParameter(key));
        }
        request.setAttribute("raw", raw);
        request.setAttribute("activeMenu", "online-reports");
        java.time.LocalDate today = java.time.LocalDate.now(OnlineSalesReportService.VN_ZONE);
        request.setAttribute("today", today);
        request.setAttribute("weekFrom", today.minusDays(6));
        request.setAttribute("monthFrom", today.withDayOfMonth(1));
        request.setAttribute("quarterFrom", today.withMonth((today.getMonthValue() - 1) / 3 * 3 + 1).withDayOfMonth(1));
        request.setAttribute("yearFrom", today.withDayOfYear(1));
        try {
            OnlineSalesFilter filter = service.filter(raw);
            request.setAttribute("f", filter);
            String id = request.getParameter("id");
            String export = request.getParameter("export");
            if (id != null) {
                int bookingId;
                try {
                    if (!id.matches("[1-9][0-9]{0,9}")) {
                        throw new NumberFormatException();
                    }
                    bookingId = Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                OnlineSalesDetail detail = service.detail(bookingId);
                if (detail == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("detail", detail);
            } else {
                if (export != null && !"csv".equals(export)) {
                    throw new ValidationException(Map.of("general", Messages.ONLINE_REPORT_EXPORT));
                }
                OnlineSalesResult result = service.report(filter, export != null);
                request.setAttribute("result", result);
                if (export != null) {
                    String csv = service.csv(filter, result);
                    response.setContentType("text/csv;charset=UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=online-sales-history-" + filter.getFrom() + "-" + filter.getTo() + ".csv");
                    response.getWriter().write(csv);
                    return;
                }
            }
            request.setAttribute("baseUrl", request.getContextPath() + "/manager/online-reports?" + query(filter));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errors", e.getErrors());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không tải được báo cáo bán vé online", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("errors", Map.of("general", Messages.SYSTEM_ERROR));
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private String query(OnlineSalesFilter f) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("from", f.getFrom().toString());
        params.put("to", f.getTo().toString());
        params.put("groupBy", f.getGroupBy());
        return params.entrySet().stream().map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining("&"));
    }
}
