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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.capabilities.CapabilitiesSource;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/** Test the JSONCapabilitiesWriter */
public class JSONCapabilitiesWriterTest {

    private final RegexFilter namespaceFilter = new RegexFilter(".*");
    private static ResourceResolver resolver;
    private static final String RESOLVER_STRING = "resolver-" + UUID.randomUUID();

    private JsonObject getJson(ResourceResolver resolver, List<CapabilitiesSource> sources, RegexFilter namespaceFilter) throws IOException {
        final StringWriter w = new StringWriter();
        new JSONCapabilitiesWriter().writeJson(resolver, w, sources, namespaceFilter);
        final JsonReader r = Json.createReader(new StringReader(w.toString()));
        final JsonObject rootJson = r.readObject();
        assertEquals("Expecting 1 root key", 1, rootJson.keySet().size());
        return rootJson.getJsonObject(JSONCapabilitiesWriter.CAPS_KEY).getJsonObject("data");
    }

    @BeforeClass
    public static void setupMocks() {
        resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resolver.toString()).thenReturn(RESOLVER_STRING);
    }

    @Test
    public void testResolverIsUsed() throws IOException {
        final List<CapabilitiesSource> sources = new ArrayList<>();
        sources.add(new MockSource("A", 2));
        final JsonObject json = getJson(resolver, sources, namespaceFilter);
        assertEquals(RESOLVER_STRING, json.getJsonObject("A").getString(ResourceResolver.class.getSimpleName()));
    }
    
    @Test
    public void testWithTwoSources() throws IOException {
        final List<CapabilitiesSource> sources = new ArrayList<>();
        sources.add(new MockSource("A", 2));
        sources.add(new MockSource("B", 1));
        
        final JsonObject json = getJson(resolver, sources, namespaceFilter);
        
        assertEquals("VALUE_0_A", json.getJsonObject("A").getString("KEY_0_A"));
        assertEquals("VALUE_1_A", json.getJsonObject("A").getString("KEY_1_A"));
        assertEquals("VALUE_0_B", json.getJsonObject("B").getString("KEY_0_B"));
        
        assertEquals("Expecting 3 keys at A", 3, json.getJsonObject("A").keySet().size());
        assertEquals("Expecting 2 key at B", 2, json.getJsonObject("B").keySet().size());
    }

    @Test
    public void testWithException() throws IOException {
        final List<CapabilitiesSource> sources = new ArrayList<>();
        sources.add(new MockSource("A", 1));
        sources.add(new MockSource("EXCEPTION", 2));
        sources.add(new MockSource("B", 1));

        final JsonObject json = getJson(resolver, sources, namespaceFilter);
        
        assertEquals("VALUE_0_A", json.getJsonObject("A").getString("KEY_0_A"));
        assertEquals("java.lang.IllegalArgumentException:Simulating a problem", json.getJsonObject("EXCEPTION").getString("_EXCEPTION_"));
        assertEquals("VALUE_0_B", json.getJsonObject("B").getString("KEY_0_B"));
        
        assertEquals("Expecting 1 key at EXCEPTION", 1, json.getJsonObject("EXCEPTION").keySet().size());
   }

    @Test(expected = DuplicateNamespaceException.class)
    public void testDuplicateNamespace() throws IOException {
        final List<CapabilitiesSource> sources = new ArrayList<>();
        sources.add(new MockSource("duplicate", 1));
        sources.add(new MockSource("another", 2));
        sources.add(new MockSource("duplicate", 1));

        getJson(resolver, sources, namespaceFilter);
    }
}