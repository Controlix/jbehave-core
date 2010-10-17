package org.jbehave.core.configuration.groovy;

import static java.text.MessageFormat.format;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jbehave.core.steps.groovy.GroovyResourceFinder;

public class GroovyContext {

    private final GroovyClassLoader classLoader;
    private final List<String> resources;
    private List<Object> instances;

    public GroovyContext() {
        this(new GroovyResourceFinder());
    }

    public GroovyContext(GroovyResourceFinder resourceFinder) {
        this(resourceFinder.findResources());
    }

    public GroovyContext(List<String> resources) {
        this(new GroovyClassLoader(), resources);
    }

    public GroovyContext(GroovyClassLoader classLoader, GroovyResourceFinder resourceFinder) {
        this(classLoader, resourceFinder.findResources());
    }

    public GroovyContext(GroovyClassLoader classLoader, List<String> resources) {
        this.classLoader = classLoader;
        this.resources = resources;
        this.instances = createGroovyInstances();
    }

    public List<Object> getInstances() {
        return instances;
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstanceOfType(Class<T> type) {
        for (Object instance : instances ) {
            if (type.isAssignableFrom(instance.getClass()) ) {
                return (T) instance;
            }
        }
        throw new GroovyInstanceNotFound(type);
    }

    public Object newInstance(String resource) {
        try {
            String name = resource.startsWith("/") ? resource : "/" + resource;
            File file = new File(this.getClass().getResource(name).toURI());
            return classLoader.parseClass(file).newInstance();
        } catch (Exception e) {
            throw new GroovyClassInstantiationFailed(classLoader, resource, e);
        }
    }

    private List<Object> createGroovyInstances() {
        List<Object> instances = new ArrayList<Object>();
        for (String resource : resources) {
            instances.add(newInstance(resource));
        }
        return instances;
    }

    @SuppressWarnings("serial")
    public static final class GroovyClassInstantiationFailed extends RuntimeException {

        public GroovyClassInstantiationFailed(GroovyClassLoader classLoader, String resource, Exception cause) {
            super(format("Failed to create new instance of class from resource {0} using Groovy class loader {1}",
                    resource, classLoader), cause);
        }

    }

    @SuppressWarnings("serial")
    public static final class GroovyInstanceNotFound extends RuntimeException {

        public GroovyInstanceNotFound(Class<?> type) {
            super(type.toString());
        }

    }

}