/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * Annotation that defines ordering. The value is optional, and represents order value
 * as defined in the {@link Ordered} interface. Lower values have higher priority.
 * The default value is {@code Ordered.LOWEST_PRECEDENCE}, indicating
 * lowest priority (losing to any other specified order value).
 *
 * <p><b>NOTE:</b> Annotation-based ordering is supported for specific kinds of
 * components only, e.g. for annotation-based AspectJ aspects. Spring container
 * strategies, on the other hand, are typically based on the {@link Ordered}
 * interface in order to allow for configurable ordering of each <i>instance</i>.
 *
 * 注解用于定义顺序。value值是可选并且默认的顺序是时定义在{@link Ordered}接口之中，越小的值具有更高的优先级.
 * 默认值是@{@code Ordered.LOWEST_PRECEDENCE},暗示着具有最低的优先级.
 *
 * 注意:
 *  基于注解的顺序仅仅支持指定类型的容器组件。例如基于注解的AspectJ切面。另一方面，最主要是基于{@link Ordered}接口，为
 *  每个实例指定顺序。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Order {

    /**
     * The order value. Default is {@link Ordered#LOWEST_PRECEDENCE}.
     *
     * @see Ordered#getOrder()
     */
    int value() default Ordered.LOWEST_PRECEDENCE;

}
