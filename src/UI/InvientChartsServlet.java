package UI;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.EventListener;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.ui.Window;

/**
 * Extended Servlet implementation to include required InvientCharts js-files in
 * DOM
 * 
 * @author Thomas Mattsson / Vaadin OY
 */
public class InvientChartsServlet extends ApplicationServlet implements
		EventListener {

	private static final long serialVersionUID = 7460979897524628312L;

	@Override
	protected void writeAjaxPageHtmlVaadinScripts(Window window,
			String themeName, Application application, BufferedWriter page,
			String appUrl, String themeUri, String appId,
			HttpServletRequest request) throws ServletException, IOException {

		// Add own scripts
		page.write("<script type=\"text/javascript\">\n");
		page.write("//<![CDATA[\n");
		page.write("document.write(\"<script language='javascript' src='"
				+ request.getContextPath()
				+ "/VAADIN/jquery/jquery-1.4.4.min.js'><\\/script>\");\n");
		page.write("document.write(\"<script language='javascript' src='"
				+ request.getContextPath()
				+ "/VAADIN/js/highcharts.js'><\\/script>\");\n");
		page.write("document.write(\"<script language='javascript' src='"
				+ request.getContextPath()
				+ "/VAADIN/js/modules/exporting.js'><\\/script>\");\n");
		page.write("//]]>\n</script>\n");

		// Let Vaadin handle everything else
		super.writeAjaxPageHtmlVaadinScripts(window, themeName, application,
				page, appUrl, themeUri, appId, request);

	}
}
