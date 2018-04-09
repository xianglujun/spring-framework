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

package org.springframework.core.env;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * Abstract base class for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * 转换服务类
     */
    protected ConfigurableConversionService conversionService = new DefaultConversionService();

    /**
     * 配置占位符帮助类，主要用于替换配置中的占位符信息
     */
    private PropertyPlaceholderHelper nonStrictHelper;

    /**
     * 严格的占位符替换帮助类
     */
    private PropertyPlaceholderHelper strictHelper;

    private boolean ignoreUnresolvableNestedPlaceholders = false;

    /**
     * 系统默认占位符前缀
     */
    private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

    /**
     * 系统默认占位符后缀
     */
    private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

    /**
     * 用于分割占位符和其默认值的标志，当默认值为null的时候，该分隔符可以省略
     */
    private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

    /**
     * 必须的配置列表
     */
    private final Set<String> requiredProperties = new LinkedHashSet<String>();


    @Override
    public ConfigurableConversionService getConversionService() {
        return this.conversionService;
    }

    @Override
    public void setConversionService(ConfigurableConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Set the prefix that placeholders replaced by this resolver must begin with.
     * <p>The default is "${".
     *
     * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
     */
    @Override
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Set the suffix that placeholders replaced by this resolver must end with.
     * <p>The default is "}".
     *
     * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
     */
    @Override
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * Specify the separating character between the placeholders replaced by this
     * resolver and their associated default value, or {@code null} if no such
     * special character should be processed as a value separator.
     * <p>The default is ":".
     *
     * @see org.springframework.util.SystemPropertyUtils#VALUE_SEPARATOR
     */
    @Override
    public void setValueSeparator(String valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    /**
     * Set whether to throw an exception when encountering an unresolvable placeholder
     * nested within the value of a given property. A {@code false} value indicates strict
     * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
     * that unresolvable nested placeholders should be passed through in their unresolved
     * ${...} form.
     * <p>The default is {@code false}.
     *
     * @since 3.2
     */
    @Override
    public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
        this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
    }

    @Override
    public void setRequiredProperties(String... requiredProperties) {
        for (String key : requiredProperties) {
            this.requiredProperties.add(key);
        }
    }

    @Override
    public void validateRequiredProperties() {
        MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
        for (String key : this.requiredProperties) {
            if (this.getProperty(key) == null) {
                ex.addMissingRequiredProperty(key);
            }
        }
        if (!ex.getMissingRequiredProperties().isEmpty()) {
            throw ex;
        }
    }


    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null ? value : defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        T value = getProperty(key, targetType);
        return (value != null ? value : defaultValue);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        String value = getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
        return value;
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
        T value = getProperty(key, valueType);
        if (value == null) {
            throw new IllegalStateException(String.format("required key [%s] not found", key));
        }
        return value;
    }

    @Override
    public String resolvePlaceholders(String text) {
        if (this.nonStrictHelper == null) {
            this.nonStrictHelper = createPlaceholderHelper(true);
        }
        return doResolvePlaceholders(text, this.nonStrictHelper);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        if (this.strictHelper == null) {
            this.strictHelper = createPlaceholderHelper(false);
        }
        return doResolvePlaceholders(text, this.strictHelper);
    }

    /**
     * Resolve placeholders within the given string, deferring to the value of
     * {@link #setIgnoreUnresolvableNestedPlaceholders} to determine whether any
     * unresolvable placeholders should raise an exception or be ignored.
     * <p>Invoked from {@link #getProperty} and its variants, implicitly resolving
     * nested placeholders. In contrast, {@link #resolvePlaceholders} and
     * {@link #resolveRequiredPlaceholders} do <emphasis>not</emphasis> delegate
     * to this method but rather perform their own handling of unresolvable
     * placeholders, as specified by each of those methods.
     *
     * @see #setIgnoreUnresolvableNestedPlaceholders
     * @since 3.2
     */
    protected String resolveNestedPlaceholders(String value) {
        return (this.ignoreUnresolvableNestedPlaceholders ?
                resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
    }

    private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
        return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
                this.valueSeparator, ignoreUnresolvablePlaceholders);
    }

    private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
        return helper.replacePlaceholders(text, new PropertyPlaceholderHelper.PlaceholderResolver() {
            @Override
            public String resolvePlaceholder(String placeholderName) {
                return getPropertyAsRawString(placeholderName);
            }
        });
    }


    /**
     * Retrieve the specified property as a raw String,
     * i.e. without resolution of nested placeholders.
     *
     * @param key the property name to resolve
     * @return the property value or {@code null} if none found
     */
    protected abstract String getPropertyAsRawString(String key);

}
