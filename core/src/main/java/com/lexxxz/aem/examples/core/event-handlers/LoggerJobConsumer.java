package com.lexxxz.aem.examples.core.event-handlers;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.Collections;

import static package com.lexxxz.aem.examples.core.event-handlers.NodePropertyRemovalListener.*;

@Component (
        immediate = true,
        service = JobConsumer.class,
        property = JobConsumer.PROPERTY_TOPICS + "=" + LoggerJobConsumer.MY_LOGGING_JOBTOPIC
)
public class LoggerJobConsumer implements JobConsumer {
    private static final String VAR_PATH = "var";
    private static final String LOG_PATH = "log";
    private static final String REMOVED_PROPERTIES_PATH = "removedProperties";
    public static final String MY_LOGGING_JOBTOPIC = "my/logging/jobtopic";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public JobResult process(Job job) {
        String eventPath = job.getProperty(EVENT_PATH, String.class);
        Long timestamp = job.getProperty(TIMESTAMP, Long.class);
        logger.debug("Start job with event at {}", eventPath);
        ResourceResolver resolver = null;
        try {
            resolver = resourceResolverFactory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME));
            Session session = resolver.adaptTo(Session.class);
            Node baseNode = getBaseNode(resolver, session);
            Node newNode = baseNode.addNode("timestamp", "nt:unstructured");
            newNode.setProperty("path", eventPath);
            newNode.setProperty("name", eventPath.substring(eventPath.lastIndexOf("\\")));
            session.save();
        } catch (LoginException e) {
            logger.error("Unable to get resolver", e);
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        }
        return JobResult.OK;
    }

    private Node getBaseNode(ResourceResolver resolver, Session session) throws RepositoryException {
        Resource resource = resolver.getResource("/" + VAR_PATH + "/" + LOG_PATH + "/" + REMOVED_PROPERTIES_PATH);
        if (resource != null) {
            return resource.adaptTo(Node.class);
        } else {
            Resource varResource = resolver.getResource("/" + VAR_PATH);
            Node varNode = varResource.adaptTo(Node.class);
            Node logNode;
            if (session.nodeExists("/" + VAR_PATH + "/" +LOG_PATH)) {
                logNode = varNode.getNode(LOG_PATH);
            } else {
                logNode = varNode.addNode(LOG_PATH);
            }
            Node removedPropertiesNode;
            if (session.nodeExists("/" + VAR_PATH + "/" + LOG_PATH + "/" + REMOVED_PROPERTIES_PATH)) {
                removedPropertiesNode = logNode.getNode(REMOVED_PROPERTIES_PATH);
            } else {
                removedPropertiesNode = logNode.addNode(REMOVED_PROPERTIES_PATH);
            }
            return removedPropertiesNode;
        }
    }
}
