/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.capabilities.it;

import java.util.Map;
import javax.inject.Inject;
import javax.servlet.Servlet;
import org.apache.sling.capabilities.CapabilitiesSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CapabilitiesBundleIT extends CapabilitiesTestSupport {
    
    @Inject
    private BundleContext bundleContext;
    
    private Bundle testBundle;

    private static class TestCapabilitiesSource implements CapabilitiesSource {
        public String getNamespace() { return null; }
        public Map<String, Object> getCapabilities() { return null; }
    };

    @Before
    public void setup() {
        final String symbolicName = "org.apache.sling.capabilities";
        testBundle = null;
        for(Bundle b : bundleContext.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                testBundle = b;
                break;
            }
        }
        assertNotNull("Expecting test bundle to be found:" + symbolicName, testBundle);
    }
    
    @Test
    public void testBundleActive() {
        assertEquals("Expecting bundle to be active", Bundle.ACTIVE, testBundle.getState());
    }
    
    @Test
    public void testRegisterSources() throws InvalidSyntaxException {
        // Verify that the bundle setup (exported packages) allows us to 
        // register a source from the outside
        final ServiceRegistration [] reg = new ServiceRegistration[3];
        for(int i=0 ; i < reg.length; i++) {
            reg[i] = bundleContext.registerService(CapabilitiesSource.class.getName(), new TestCapabilitiesSource(), null);
            assertNotNull("Expecting ServiceRegistration", reg[i]);
        }
        
        try {
            // Now check that all sources are registered
            // using toString() trick from CapabilitiesServlet
            final String expected = reg.length + " CapabilitiesSource";
            final ServiceReference[] refs =  bundleContext.getServiceReferences(Servlet.class.getName(), "(sling.servlet.resourceTypes=sling/capabilities)");
            assertTrue("Expecting a single Servlet service", refs != null && refs.length == 1);
            final String str = bundleContext.getService(refs[0]).toString();
            assertTrue("Expecting " + expected + " but got " + str, str.contains(expected));
            bundleContext.ungetService(refs[0]);
        } finally {
            for(int i=0 ; i < reg.length; i++) {
                reg[i].unregister();
            }
        }
    }
}
