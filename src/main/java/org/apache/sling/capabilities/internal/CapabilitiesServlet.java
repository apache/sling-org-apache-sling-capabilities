/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.capabilities.internal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.capabilities.CapabilitiesSource;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = Servlet.class,
property = {
    "sling.servlet.resourceTypes=sling/capabilities",
    "sling.servlet.methods=GET",
    "sling.servlet.extensions=json"
})
@Designate(ocd = CapabilitiesServlet.Config.class)
public class CapabilitiesServlet extends SlingSafeMethodsServlet {
    
    @ObjectClassDefinition(
        name = "Apache Sling Capabilities Servlet",
        description = "Provides information about Sling capabilities"
    )
    public static @interface Config {
        @AttributeDefinition(
            name = "Resource Path Patterns",
            description = "A set of (java) regular expression patterns that the resource path must match for this servlet to execute"
        )
        String [] resourcePathPatterns() default {};
    }
    
    private final List<CapabilitiesSource> sources = new CopyOnWriteArrayList<>();
    private RegexFilter pathFilter;
    public static final String NAMESPACES_PROP = "namespace_patterns";

    @Activate
    protected void activate(Config cfg, ComponentContext ctx) {
        pathFilter = new RegexFilter(cfg.resourcePathPatterns());
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + sources.size() + " " + CapabilitiesSource.class.getSimpleName() + " active";
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        
        // Resource Path must match a configurable set of patterns, to prevent
        // users from getting this information by just creating a resource anywhere
        // with our resource type. The idea is that those paths will have suitable
        // access control (readonly for users) to control exactly which capabilities
        // are exposed.
        final String path = request.getResource().getPath();
        if(!pathFilter.accept(path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid path " + path);
            return;
        }
        
        // Resource must define which namespaces are exposed, also for
        // security reasons, to make sure administrators think about
        // what's exposed
        final ValueMap m = request.getResource().adaptTo(ValueMap.class);
        final String [] namespacePatterns = m.get(NAMESPACES_PROP, String[].class);
        if(namespacePatterns == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing property " + NAMESPACES_PROP);
            return;
        }
        
        // All good, get capabilities
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new JSONCapabilitiesWriter().writeJson(response.getWriter(), sources, new RegexFilter(namespacePatterns));
        response.getWriter().flush();
    }

    @Reference(
        policy=ReferencePolicy.DYNAMIC,
        cardinality=ReferenceCardinality.MULTIPLE,
        policyOption=ReferencePolicyOption.GREEDY)
    void bindSource(CapabilitiesSource src) {
        sources.add(src);
    }

    void unbindSource(CapabilitiesSource src) {
        sources.remove(src);
    }
}