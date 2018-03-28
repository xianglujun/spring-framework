/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

/**
 * Custom extension of CGLIB's {@link DefaultNamingPolicy}, modifying
 * the tag in generated class names from "ByCGLIB" to "BySpringCGLIB".
 *
 * <p>This is primarily designed to avoid clashes between a regular CGLIB
 * version (used by some other library) and Spring's embedded variant,
 * in case the same class happens to get proxied for different purposes.
 * <p>
 * 在spring 3.2版本之后，spring不再依赖cglib.jar包，而是通过将cglib的包中的内容，集成到
 * org.springframework.cglib.包下，这样来避免spring因为依赖cglibjar包版本问题而引起的冲突
 *
 * @author Juergen Hoeller
 * @since 3.2.8
 */
public class SpringNamingPolicy extends DefaultNamingPolicy {

    public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

    @Override
    protected String getTag() {
        return "BySpringCGLIB";
    }

}
