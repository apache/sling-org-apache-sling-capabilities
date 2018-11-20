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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.capabilities.CapabilitiesSource;

class MockSource implements CapabilitiesSource {

    private final String namespace;
    private final int propsCount;

    MockSource(String namespace, int propsCount) {
        this.namespace = namespace;
        this.propsCount = propsCount;
    }

    @Override
    public Map<String, Object> getCapabilities(ResourceResolver resolver) throws Exception {
        if (namespace.contains("EXCEPTION")) {
            throw new IllegalArgumentException("Simulating a problem");
        }
        return getProps(resolver);
    }

    private Map<String, Object> getProps(ResourceResolver resolver) {
        final Map<String, Object> props = new HashMap<>();
        for (int i = 0; i < propsCount; i++) {
            props.put("KEY_" + i + "_" + namespace, "VALUE_" + i + "_" + namespace);
        }
        props.put(ResourceResolver.class.getSimpleName(), resolver.toString());
        return Collections.unmodifiableMap(props);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

}
