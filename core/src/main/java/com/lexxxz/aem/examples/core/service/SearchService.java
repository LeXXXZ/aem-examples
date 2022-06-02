package com.lexxxz.aem.examples.core.service;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.google.common.collect.ImmutableMap;
import org.apache.jackrabbit.oak.stats.StopwatchLogger;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(service= SearchService.class, immediate = true)
public class SearchService {

    @Reference
    private QueryBuilder queryBuilder;


    public  List<Hit> findNodes(final SlingHttpServletRequest request, String path, String searchText, String propertyName) {
        Session session = request.getResourceResolver().adaptTo(Session.class);

        Map<String, Object> parameters = ImmutableMap.<String, Object>builder()
                .put("path",path)
                .put("type", "nt:unstructured")
                .put("property", propertyName)
                .put("property.operation", "like")
                .put("property.value", "%" + searchText + "%")
                .build();

        Query query = queryBuilder.createQuery(PredicateGroup.create(parameters), session);
        List<Hit> hits = new ArrayList<>();
        try (StopwatchLogger stopwatchLogger = new StopwatchLogger(SearchService.class)) {
            stopwatchLogger.start();
            hits = query.getResult().getHits();
            stopwatchLogger.stop("");
        } catch (IOException e) {
            return hits;
        }
        return hits;
    }
}
