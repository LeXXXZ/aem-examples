package com.lexxxz.aem.examples.core.event-handlers;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.lexxxz.aem.examples.core.event-handlers.LoggerJobConsumer.MY_LOGGING_JOBTOPIC;

@Component (
        immediate = true, service = EventListener.class
)
public class NodePropertyRemovalListener implements EventListener {

    public static final String SERVICE_NAME = "subServiceName";
    public static final String EVENT_PATH = "eventPath";
    public static final String TIMESTAMP = "timestamp";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    JobManager jobManager;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Activate
    public void activate(ComponentContext context) throws Exception {
        ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME));
        try {
            Session session = resolver.adaptTo(Session.class);
            session.getWorkspace().getObservationManager().addEventListener(
                    this, //handler
                    Event.PROPERTY_REMOVED, //binary combination of event types
                    "/content/we-retail/language-masters/en/jcr:content", //path
                    true, //is Deep?
                    null, //uuids filter
                    null, //nodetypes filter
                    false); //noLocal
        } catch (RepositoryException e){
            logger.error("Error while registration of EventListener",e);
        }
    }

    @Override
    public void onEvent(EventIterator eventIterator) {
        try {
            while (eventIterator.hasNext()){
                Event event = eventIterator.nextEvent();
                logger.info("Property has been deleted at : {}", event.getPath());
                startJob(event);
            }
        } catch(RepositoryException e){
            logger.error("Error while treating events",e);
        }
    }

    private void startJob(Event event) throws RepositoryException {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(EVENT_PATH, event.getPath());
        props.put(TIMESTAMP, (new Date()).getTime());
        jobManager.addJob(MY_LOGGING_JOBTOPIC, props);
    }

    @Deactivate
    public void deactivate() throws LoginException {
        ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME));
        try {
            Session session = resolver.adaptTo(Session.class);
            ObservationManager observationManager = session.getWorkspace().getObservationManager();
            observationManager.removeEventListener(this);
        } catch (UnsupportedRepositoryOperationException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
}
