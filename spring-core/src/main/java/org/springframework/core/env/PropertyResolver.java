/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.core.env;

/**
 * Interface for resolving properties against any underlying source.
 * 实现配置信息的接口定义
 *
 * @author Chris Beams
 * @see Environment
 * @see PropertySourcesPropertyResolver
 * @since 3.1
 */
public interface PropertyResolver {

    /**
     * Return whether the given property key is available for resolution, i.e.,
     * the value for the given key is not {@code null}.
     * <p>
     * 判断当前的容器中是否包含了指定的key
     */
    boolean containsProperty(String key);

    /**
     * Return the property value associated with the given key, or {@code null}
     * if the key cannot be resolved.
     *
     * @param key the property name to resolve
     * @see #getProperty(String, String)
     * @see #getProperty(String, Class)
     * @see #getRequiredProperty(String)
     */
    String getProperty(String key);

    /**
     * Return the property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     *
     * @param key          the property name to resolve
     * @param defaultValue the default value to return if no value is found
     * @see #getRequiredProperty(String)
     * @see #getProperty(String, Class)
     */
    String getProperty(String key, String defaultValue);

    /**
     * Return the property value associated with the given key, or {@code null}
     * if the key cannot be resolved.
     *
     * @param key        the property name to resolve
     * @param targetType the expected type of the property value
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType);

    /**
     * Return the property value associated with the given key, or
     * {@code defaultValue} if the key cannot be resolved.
     * 根据指定的key返回实际的配置的值，如果没有找到对应的配置信息，则返回默认的值
     *
     * @param key          the property name to resolve
     * @param targetType   the expected type of the property value
     * @param defaultValue the default value to return if no value is found
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    /**
     * Convert the property value associated with the given key to a {@code Class}
     * of type {@code T} or {@code null} if the key cannot be resolved.
     * <p>
     * 根据指定key获取到的值，转换为指定的{@link Class}对象,如果没有找到对应的配置，则返回NULL
     *
     * @throws org.springframework.core.convert.ConversionException if class specified
     *                                                              by property value cannot be found  or loaded or if targetType is not assignable
     *                                                              from class specified by property value
     * @see #getProperty(String, Class)
     */
    <T> Class<T> getPropertyAsClass(String key, Class<T> targetType);

    /**
     * Return the property value associated with the given key, converted to the given
     * targetType (never {@code null}).
     *
     * @throws IllegalStateException if the key cannot be resolved
     * @see #getRequiredProperty(String, Class)
     */
    String getRequiredProperty(String key) throws IllegalStateException;

    /**
     * Return the property value associated with the given key, converted to the given
     * targetType (never {@code null}).
     *
     * @throws IllegalStateException if the given key cannot be resolved
     */
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

    /**
     * Resolve ${...} placeholders in the given text, replacing them with corresponding
     * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
     * no default value are ignored and passed through unchanged.
     * <p>
     * 解决给定的字符中的占位符的，使用响应的配置的值替换该站位符，通过{@link #getProperty}的方法来实现。
     * 如果没有被解析的站位符并没有指定默认的值，将会被忽略并不会有任何变化。
     * </p>
     *
     * @param text the String to resolve
     * @return the resolved String (never {@code null})
     * @throws IllegalArgumentException if given text is {@code null}
     * @see #resolveRequiredPlaceholders
     * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String)
     */
    String resolvePlaceholders(String text);

    /**
     * Resolve ${...} placeholders in the given text, replacing them with corresponding
     * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
     * no default value will cause an IllegalArgumentException to be thrown.
     * <p>
     * 解决给定的字符中的占位符的，使用响应的配置的值替换该站位符，通过{@link #getProperty}的方法来实现。
     * 如果没有被解析的站位符并没有指定默认的值，将会产生一个{@link IllegalArgumentException}的异常
     * </p>
     *
     * @return the resolved String (never {@code null})
     * @throws IllegalArgumentException if given text is {@code null}
     *                                  or if any placeholders are unresolvable
     * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String, boolean)
     */
    String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
