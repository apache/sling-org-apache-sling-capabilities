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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RegexFilterTest {
    
    @Test
    public void testNoPattern() {
        RegexFilter f = new RegexFilter();
        assertFalse(f.accept("foo"));
    }
    
    @Test
    public void testTwoStrings() {
        RegexFilter f = new RegexFilter("zo", "meu");
        assertFalse(f.accept("gabu"));
        assertTrue(f.accept("zo"));
        assertTrue(f.accept("meu"));
    }
    
    @Test
    public void testOneMatch() {
        RegexFilter f = new RegexFilter("zo", "meu", "[a-b]0");
        assertTrue(f.accept("a0"));
        assertTrue(f.accept("b0"));
        assertTrue(f.accept("meu"));
        assertFalse(f.accept("c4"));
    }
}