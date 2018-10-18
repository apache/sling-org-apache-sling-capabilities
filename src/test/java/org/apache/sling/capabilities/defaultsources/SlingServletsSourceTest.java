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
package org.apache.sling.capabilities.defaultsources;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.Servlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.capabilities.CapabilitiesSource;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class SlingServletsSourceTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    static class MockServlet extends SlingSafeMethodsServlet {
        private final Map<String, Object> props = new HashMap<>();
        private static final String PREFIX = "sling.servlet.";

        MockServlet(String extension, int id) {
            props.put(PREFIX + "extensions", extension);
            props.put(PREFIX + "id", id);
            props.put("some.other.property", id + 12);
        }

        final Map<String, Object> getProps() {
            return props;
        }
    }

    @Before
    public void setup() throws IOException {
        // Configure allowed path patterns
        final ConfigurationAdmin ca = context.getService(ConfigurationAdmin.class);
        assertNotNull("Expecting a ConfigurationAdmin service", ca);
        final Configuration cfg = ca.getConfiguration(SlingServletsSource.class.getName());
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("capabilitiesNamespaceSuffix", "TEST_NS");
        props.put("servletsLdapFilter", "(sling.servlet.extensions=json)");
        cfg.update(props);

        context.registerInjectActivateService(new SlingServletsSource());

        // Need a few (fake) Sling servlets to test
        final String [] ext = { "json", "txt", "json" };
        final int [] id = { 12, 24, 41 };
        for(int i=0 ; i < ext.length; i++) {
            final MockServlet s = new MockServlet(ext[i], id[i]);
            context.registerService(Servlet.class, s, s.getProps());
        }
    }

    @Test
    public void testServletsSource() throws Exception {
        final CapabilitiesSource src = context.getService(CapabilitiesSource.class);
        assertNotNull("Expecting a CapabilitiesSource", src);
        assertEquals("Expecting namespace to match", "org.apache.sling.servlets.TEST_NS", src.getNamespace());

        final Map<String, Object> caps = src.getCapabilities();
        assertNotNull("Expecting to get Capabilities", caps);
        assertEquals("Expecting capabilities for 2 json servlets", 2, caps.size());

        final Pattern keyPattern = Pattern.compile("MockServlet_[0-9]+");
        for(String key : caps.keySet()) {
            assertTrue("Expecting key " + key + " to match", keyPattern.matcher(key).matches());
        }

        for(Object o: caps.values()) {
            assertTrue("Expecting Maps in values", o instanceof Map);
            Map<?, ?> map = (Map<?,?>)o;
            assertEquals("Expecting 2 properties per map", 2, map.size());
            final String id = map.get("sling.servlet.id").toString();
            assertTrue("Expecting id=12 or 41", id.equals("12") || id.equals("41"));
        }
    }
}