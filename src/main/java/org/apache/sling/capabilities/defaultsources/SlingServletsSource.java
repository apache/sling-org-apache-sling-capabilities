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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.apache.sling.capabilities.CapabilitiesSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/** Default CapabilitiesSource that provides information on available Sling
 *  Servlets, exposing their sling.servlet.* properties so that clients can
 *  find out which behaviors are available.
 */
@Component(service = CapabilitiesSource.class)
@Designate(
    ocd = SlingServletsSource.Config.class,
    factory = true
)
public class SlingServletsSource implements CapabilitiesSource {

    @ObjectClassDefinition(
        name = "Sling Servlets Capabilities Source",
        description = "Provides information about available Sling Servlets"
    )
    public static @interface Config {
        @AttributeDefinition(
            name = "LDAP filter",
            description = "OSGi LDAP filter to select servlets to consider for the provided capabilites"
        )
        String servletsLdapFilter() default "";
        
        @AttributeDefinition(
            name = "Capabilities Namespace",
            description = "Unique namespace that identifies this set of capabilities"
        )
        String capabilitiesNamespace();
    }
    
    private String namespace;
    private String ldapFilter;
    private BundleContext bundleContext;
    
    private static final String SLING_SERVLET_PROPERTY_PREFIX = "sling.servlet.";
    
    @Activate
    public void activate(Config cfg, ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
        this.namespace = cfg.capabilitiesNamespace();
        this.ldapFilter = cfg.servletsLdapFilter();
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public Map<String, Object> getCapabilities() throws Exception {
        final Map<String, Object> result = new HashMap<>();
        final ServiceReference [] refs = bundleContext.getServiceReferences(Servlet.class.getName(), ldapFilter);
        if(refs != null) {
            for(ServiceReference ref : refs) {
                result.put(uniqueKey(ref), getCapabilities(ref));
            }
        }
        return result;
    }
    
    private static Map<String, Object> getCapabilities(ServiceReference ref) {
        final Map<String, Object> result = new HashMap<>();
        for(String key : ref.getPropertyKeys()) {
            if(key.startsWith(SLING_SERVLET_PROPERTY_PREFIX)) {
                final Object value = ref.getProperty(key);
                if(value != null) {
                    result.put(key, maybeConvertToSingleValue(value));
                }
            }
        };
        return result;
    }
    
    /** Convert input to a single value if it's an array of size one */
    private static Object maybeConvertToSingleValue(Object input) {
        Object result = input;
        if(input instanceof Object[]) {
            Object[] arr = (Object[])input;
            if(arr.length == 1) {
                result = arr[0];
            }
        }
        return result;
    }
    
    /** Compute a somewhat representative unique key for ref */
    private String uniqueKey(ServiceReference ref) {
        final StringBuilder result = new StringBuilder();
        final Object service = bundleContext.getService(ref);
        try {
            result.append(service.getClass().getSimpleName());
            result.append("_").append(service.hashCode());
        } finally {
            bundleContext.ungetService(ref);
        }
        return result.toString();
    }
}