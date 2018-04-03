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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Helper for resolving synthetic {@link Method#isBridge bridge Methods} to the
 * {@link Method} being bridged.
 *
 * <p>Given a synthetic {@link Method#isBridge bridge Method} returns the {@link Method}
 * being bridged. A bridge method may be created by the compiler when extending a
 * parameterized type whose methods have parameterized arguments. During runtime
 * invocation the bridge {@link Method} may be invoked and/or used via reflection.
 * When attempting to locate annotations on {@link Method Methods}, it is wise to check
 * for bridge {@link Method Methods} as appropriate and find the bridged {@link Method}.
 * <p>
 * 该类主要是为了解决虚拟机生成的桥接方法(为了兼容较低版本的JDK，由虚拟机产生的方法)
 *
 * <p>See <a href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12.4.5">
 * The Java Language Specification</a> for more details on the use of bridge methods.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 */
public abstract class BridgeMethodResolver {

    /**
     * Find the original method for the supplied {@link Method bridge Method}.
     * <p>It is safe to call this method passing in a non-bridge {@link Method} instance.
     * In such a case, the supplied {@link Method} instance is returned directly to the caller.
     * Callers are <strong>not</strong> required to check for bridging before calling this method.
     * <p>
     * 从提供的桥接方法中获取的该桥接方法的原始方法。
     * 当通过非桥接方法实例来调用这个方法时，是一个安全的行为，不会抛出任何异常，直接返回方法本身
     * 在这种情况下，该方法直接返回{@link Method}实例给调用者
     * 在调用该方法之前，不需要对是否为桥接方法进行校验
     *
     * @param bridgeMethod the method to introspect
     * @return the original method (either the bridged method or the passed-in method
     * if no more specific one could be found)
     */
    public static Method findBridgedMethod(Method bridgeMethod) {

        // 判断是否为桥接方法,如果为非桥接方法，直接返回
        if (bridgeMethod == null || !bridgeMethod.isBridge()) {
            return bridgeMethod;
        }

        // Gather all methods with matching name and parameter size.
        List<Method> candidateMethods = new ArrayList<Method>();

        // 获取当前方法所在类，以及父类的所有方法列表
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(bridgeMethod.getDeclaringClass());
        for (Method candidateMethod : methods) {
            // 判断candicateMethod是否为bridgeMethod的原始方法
            if (isBridgedCandidateFor(candidateMethod, bridgeMethod)) {
                candidateMethods.add(candidateMethod);
            }
        }

        // Now perform simple quick check.
        // 如果Class中本身就只有一个方法匹配，则直接认为是
        // 该桥接方法的源方法
        if (candidateMethods.size() == 1) {
            return candidateMethods.get(0);
        }

        // Search for candidate match.
        // 如果存在多个同名且参数相同的方法的时候，就需要更深层次的去比较和获取
        Method bridgedMethod = searchCandidates(candidateMethods, bridgeMethod);
        if (bridgedMethod != null) {
            // Bridged method found...
            return bridgedMethod;
        } else {
            // A bridge method was passed in but we couldn't find the bridged method.
            // Let's proceed with the passed-in method and hope for the best...
            return bridgeMethod;
        }
    }

    /**
     * Returns {@code true} if the supplied '{@code candidateMethod}' can be
     * consider a validate candidate for the {@link Method} that is {@link Method#isBridge() bridged}
     * by the supplied {@link Method bridge Method}. This method performs inexpensive
     * checks and can be used quickly filter for a set of possible matches.
     * <p>
     * 判断是否为桥接方法:
     * 1. 根据Method判断 candidate {@link Method}是否为桥接方法，如果本身为桥接方法，直接返回false
     * 2. 判断candidate {@link Method} 和 bridge {@link Method}方法是否相等，相等则返回false
     * 3. 判断方法名称是否一致
     * 4. 判断方法参数是否一致
     */
    private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
        return (!candidateMethod.isBridge() && !candidateMethod.equals(bridgeMethod) &&
                candidateMethod.getName().equals(bridgeMethod.getName()) &&
                candidateMethod.getParameterTypes().length == bridgeMethod.getParameterTypes().length);
    }

    /**
     * Searches for the bridged method in the given candidates.
     * <p>
     * 从{@link Method}列表中获取到bridge {@link Method}的原始方法，该方法可能会返回null
     *
     * @param candidateMethods the List of candidate Methods
     * @param bridgeMethod     the bridge method
     * @return the bridged method, or {@code null} if none found
     */
    private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
        if (candidateMethods.isEmpty()) {
            return null;
        }
        Method previousMethod = null;
        boolean sameSig = true;
        for (Method candidateMethod : candidateMethods) {
            if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
                return candidateMethod;
            } else if (previousMethod != null) {
                sameSig = sameSig &&
                        Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
            }
            previousMethod = candidateMethod;
        }
        return (sameSig ? candidateMethods.get(0) : null);
    }

    /**
     * Determines whether or not the bridge {@link Method} is the bridge for the
     * supplied candidate {@link Method}.
     * <p>
     * 判断是否bridge {@link Method} 是提供的candidate {@link Method}的桥接方法
     */
    static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
        if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
            return true;
        }
        Method method = findGenericDeclaration(bridgeMethod);
        return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
    }

    /**
     * Searches for the generic {@link Method} declaration whose erased signature
     * matches that of the supplied bridge method.
     *
     * @throws IllegalStateException if the generic declaration cannot be found
     */
    private static Method findGenericDeclaration(Method bridgeMethod) {
        // Search parent types for method that has same signature as bridge.
        Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
        while (superclass != null && !Object.class.equals(superclass)) {
            Method method = searchForMatch(superclass, bridgeMethod);
            if (method != null && !method.isBridge()) {
                return method;
            }
            superclass = superclass.getSuperclass();
        }

        // Search interfaces.
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
        for (Class<?> ifc : interfaces) {
            Method method = searchForMatch(ifc, bridgeMethod);
            if (method != null && !method.isBridge()) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns {@code true} if the {@link Type} signature of both the supplied
     * {@link Method#getGenericParameterTypes() generic Method} and concrete {@link Method}
     * are equal after resolving all types against the declaringType, otherwise
     * returns {@code false}.
     * <p>
     * 当 {@link Type}通过{@link Method#getGenericParameterTypes()} generic Method} 和具体的方法对象相等，则返回ture
     * 其他情况都返回false.
     *
     * @param genericMethod 泛型方法，则是需要判断是否为桥接方法的原始{@link Method}对象
     * @param candidateMethod 桥接方法
     * @param declaringClass 桥接方法所在的类对象
     */
    private static boolean isResolvedTypeMatch(
            Method genericMethod, Method candidateMethod, Class<?> declaringClass) {

        Type[] genericParameters = genericMethod.getGenericParameterTypes();
        Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
        if (genericParameters.length != candidateParameters.length) {
            return false;
        }
        for (int i = 0; i < candidateParameters.length; i++) {
            ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
            Class<?> candidateParameter = candidateParameters[i];
            if (candidateParameter.isArray()) {
                // An array type: compare the component type.
                if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().resolve(Object.class))) {
                    return false;
                }
            }
            // A non-array type: compare the type itself.
            if (!candidateParameter.equals(genericParameter.resolve(Object.class))) {
                return false;
            }
        }
        return true;
    }

    /**
     * If the supplied {@link Class} has a declared {@link Method} whose signature matches
     * that of the supplied {@link Method}, then this matching {@link Method} is returned,
     * otherwise {@code null} is returned.
     */
    private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
        return ReflectionUtils.findMethod(type, bridgeMethod.getName(), bridgeMethod.getParameterTypes());
    }

    /**
     * Compare the signatures of the bridge method and the method which it bridges. If
     * the parameter and return types are the same, it is a 'visibility' bridge method
     * introduced in Java 6 to fix http://bugs.sun.com/view_bug.do?bug_id=6342411.
     * See also http://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html
     *
     * @return whether signatures match as described
     */
    public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
        if (bridgeMethod == bridgedMethod) {
            return true;
        }
        return (Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes()) &&
                bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType()));
    }

}
