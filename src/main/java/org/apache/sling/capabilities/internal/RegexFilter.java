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

import java.util.regex.Pattern;

/** Filter that checks if a given String matches at least one of a set of
 *  regexps.
 */
class RegexFilter {
    
    private final Pattern [] patterns;
    
    RegexFilter(String ... regexp) {
        if(regexp == null || regexp.length == 0) {
            patterns = new Pattern[0];
        } else {
            patterns = new Pattern[regexp.length];
            for(int i=0; i < regexp.length; i++) {
                patterns[i] = Pattern.compile(regexp[i]);
            }
        }
    }
    
    boolean accept(String candidate) {
        for(Pattern p : patterns) {
            if(p.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }
}