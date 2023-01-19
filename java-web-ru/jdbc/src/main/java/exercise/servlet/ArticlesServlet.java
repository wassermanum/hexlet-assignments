package exercise.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;

import exercise.TemplateEngineUtil;


public class ArticlesServlet extends HttpServlet {

    private String getId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return null;
        }
        String[] pathParts = pathInfo.split("/");
        return ArrayUtils.get(pathParts, 1, null);
    }

    private String getAction(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return "list";
        }
        String[] pathParts = pathInfo.split("/");
        return ArrayUtils.get(pathParts, 2, getId(request));
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
                throws IOException, ServletException {

        String action = getAction(request);

        switch (action) {
            case "list":
                try {
                    showArticles(request, response);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                try {
                    showArticle(request, response);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private void showArticles(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException, ServletException, SQLException {

        ServletContext context = request.getServletContext();
        Connection connection = (Connection) context.getAttribute("dbConnection");
        // BEGIN
        String page = request.getParameter("page");
        String param = Objects.nonNull(page) ? page : "1";
        int offset = Integer.parseInt(param) * 10;
        String query = "SELECT id, title FROM articles ORDER BY id LIMIT 10 OFFSET ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, offset - 10);
        ResultSet rs = preparedStatement.executeQuery();

        request.setAttribute("page", Integer.parseInt(param));

        List<Map<String, String>> articles = new ArrayList<>();

        while (rs.next()) {
            articles.add(Map.of("id", rs.getString("id"),
                    "title", rs.getString("title")));
        }
        request.setAttribute("articles", articles);
        // END
        TemplateEngineUtil.render("articles/index.html", request, response);
    }

    private void showArticle(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException, ServletException, SQLException {

        ServletContext context = request.getServletContext();
        Connection connection = (Connection) context.getAttribute("dbConnection");
        // BEGIN
        String query = "SELECT title, body FROM articles WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, Integer.parseInt(Objects.requireNonNull(getId(request))));
        ResultSet rs = preparedStatement.executeQuery();

        Map<String, String> article = new HashMap<>();
        if (rs.next()) {
            article.put("title", rs.getString("title"));
            article.put("body", rs.getString("body"));
        } else {
            response.setStatus(404);
        }
        request.setAttribute("article", article);
        // END
        TemplateEngineUtil.render("articles/show.html", request, response);
    }
}
