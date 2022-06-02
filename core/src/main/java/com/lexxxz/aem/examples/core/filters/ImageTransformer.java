package com.lexxxz.aem.examples.core.filters;

import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.*;
import java.io.IOException;


@Component (service = Filter.class,
property = {
        "sling.filter.scope=" + "request",
        "sling.filter.pattern=" + "/content/we-retail/.*",
        "sling.filter.extensions=" + "jpeg"
})
public class ImageTransformer implements Filter{

    @Reference
    private SlingSettingsService slingSettingsService;

    private int mode = 0;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (this.slingSettingsService.getRunModes().contains("mode1")) {
            mode = 1;
        } else if (this.slingSettingsService.getRunModes().contains("mode2")) {
            mode = 2;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
        this.transformImage(servletResponse, request);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void transformImage(ServletResponse servletResponse, SlingHttpServletRequest request) {
        try {
            Image image = new Image(request.getResource());
            Layer layer = image.getLayer(false, false, false);
            if (mode == 1) {
                layer.grayscale();
            } else if (mode == 2) {
                layer.rotate(180);
            } else {
                return;
            }
            final String mimeType = request.getResponseContentType();
            layer.write(mimeType, 100, servletResponse.getOutputStream());
            servletResponse.flushBuffer();
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    @Override
    public void destroy() {

    }

}
