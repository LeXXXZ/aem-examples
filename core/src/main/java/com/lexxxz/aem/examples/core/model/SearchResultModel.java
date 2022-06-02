package com.lexxxz.aem.examples.core.model;

import com.day.cq.search.result.Hit;
import com.lexxxz.aem.examples.core.service.SearchService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Model(adaptables = { SlingHttpServletRequest.class }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SearchResultModel {

    @Inject @Via("resource")
    private String text;

    @Inject @Via("resource")
    private String path;

    @Inject @Via("resource")
    private String propertyName;

    @Inject
    SearchService service;

    @Self
    private SlingHttpServletRequest request;

    private ArrayList<String> results = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<Hit> nodes = new ArrayList<>();
        nodes = service.findNodes(request, path, text, propertyName);
        if (nodes.isEmpty()) {
            results.add(String.format("No hits found for '%s' path", path));
            return;
        }
        results = (ArrayList<String>) nodes.stream().map(hit -> {
                try {
                    return hit.getPath();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return "";
            }).collect(Collectors.toList());
    }

    public String getText() {
        return text;
    }

    public String getPath() {
        return path;
    }

    public ArrayList<String> getResults() {
        return results;
    }
}
