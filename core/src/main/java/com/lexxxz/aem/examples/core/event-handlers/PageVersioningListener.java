package com.lexxxz.aem.examples.core.ui.event-handlers;

import com.day.cq.wcm.api.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import java.util.Collections;
import java.util.Iterator;

@Component(
        immediate = true,
        service = EventHandler.class,
        property = {
                EventConstants.EVENT_TOPIC + "=" + PageEvent.EVENT_TOPIC,
                EventConstants.EVENT_FILTER + "path=/content/we-retail/language-masters/en/*/"
        }
)
public class PageVersioningListener implements EventHandler {

    public static final String SUBSERVICE_NAME = "pageVersioningListenerService";
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private PageManagerFactory managerFactory;

    private ResourceResolver resolver;

    @Activate
    public void activate() {
        try {
            resolver = resourceResolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME)
            );
        } catch (LoginException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void handleEvent(Event event) {
        PageEvent pgEvent = PageEvent.fromEvent(event);
        Iterator<PageModification> modifications = pgEvent.getModifications();
        while(modifications.hasNext()) {
            PageModification pageModification = modifications.next();
            PageManager pageManager = managerFactory.getPageManager(resolver);
            Page page = pageManager.getPage(pageModification.getPath());
            Resource contentResource = page.getContentResource();
            if (contentResource.getValueMap().get("jcr:description") != null) {
                Session session = resolver.adaptTo(Session.class);
                try {
                    VersionManager versionManager = session.getWorkspace().getVersionManager();
                    Version version = versionManager.checkpoint(contentResource.getPath());
                    LOGGER.debug("New version {} of the page {} is created", version.getName(), page.getPath());
                } catch (RepositoryException e) {
                    LOGGER.error(e.getMessage());
                } finally {
                    session.logout();
                }
            }
        }
    }
}
