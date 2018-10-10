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
import java.io.StringReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.capabilities.CapabilitiesSource;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.resourceresolver.MockResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Test the JSONCapabilitiesWriter */
public class CapabilitesServletTest {

    private CapabilitiesServlet servlet;

    @Rule
    public final OsgiContext context = new OsgiContext();
    
    private BundleContext bundleContext;
    
    private static final String [] AUTHORIZED_PATHS_PATTERNS = {
        ".*/ok$",
        "/var/.*"
    };
    
    private static final String [] NAMESPACE_PATTERNS = {
        ".*"
    };
    
    @Before
    public void setup() throws IOException {
        
        // Configure allowed path patterns
        final ConfigurationAdmin ca = context.getService(ConfigurationAdmin.class);
        assertNotNull("Expecting a ConfigurationAdmin service", ca);
        final Configuration cfg = ca.getConfiguration(CapabilitiesServlet.class.getName());
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("resourcePathPatterns", AUTHORIZED_PATHS_PATTERNS);
        cfg.update(props);
        
        servlet = new CapabilitiesServlet();
        bundleContext = MockOsgi.newBundleContext();
        
        final CapabilitiesSource [] sources = {
            new MockSource("F", 2),
            new MockSource("G", 43)
        };
        for(CapabilitiesSource src : sources) {
            // Not sure why both are needed, but tests fails otherwise
            context.registerService(src);
            servlet.bindSource(src);
        }
        context.registerInjectActivateService(servlet);
    }
    
    private MockSlingHttpServletRequest requestFor(ResourceResolver resolver, String path) {
        final MockSlingHttpServletRequest req = new MockSlingHttpServletRequest(resolver);
        final Map<String, Object> props = new HashMap<>();
        props.put(CapabilitiesServlet.NAMESPACES_PROP, NAMESPACE_PATTERNS);
        final MockResource res = new MockResource(path, props, resolver);
        req.setResource(res);
        return req;
    }
    
    @Test
    public void testServlet() throws ServletException, IOException {
        final ResourceResolver resolver = MockSling.newResourceResolver(bundleContext);
        MockSlingHttpServletResponse resp = new MockSlingHttpServletResponse();
        
        servlet.service(requestFor(resolver, "/var/somewhere"), resp);
        
        assertEquals(200, resp.getStatus());

        // Just verify that both sources are taken into account
        // the JSON format details are tested elsewhere
        final JsonReader r = Json.createReader(new StringReader(resp.getOutputAsString()));
        final JsonObject rootJson = r.readObject();
        final JsonObject json = rootJson.getJsonObject(JSONCapabilitiesWriter.CAPS_KEY).getJsonObject("data");
        assertEquals("VALUE_1_F", json.getJsonObject("F").getString("KEY_1_F"));
        assertEquals("VALUE_42_G", json.getJsonObject("G").getString("KEY_42_G"));
    }
    
    @Test
    public void verifyServiceProperties() throws ServletException, IOException, InvalidSyntaxException {
        final ServiceRegistration reg = bundleContext.registerService(Servlet.class.getName(), servlet, null);
        
        try {
            assertNotNull("Expecting a non-null ServiceRegistration", reg);
            assertEquals("sling/capabilities", reg.getReference().getProperty("sling.servlet.resourceTypes"));
            assertEquals("GET", reg.getReference().getProperty("sling.servlet.methods"));
            assertEquals("json", reg.getReference().getProperty("sling.servlet.extensions"));
        } finally {
            reg.unregister();
        }
    }
}