/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple object instantiation strategy for use in a BeanFactory.
 *
 * <p>Does not support Method Injection, although it provides hooks for subclasses
 * to override to add Method Injection support, for example by overriding methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {

	/**
	 * 对实例进行初始化
	 * @param beanDefinition the bean definition
	 * @param beanName name of the bean when it's created in this context.
	 * The name can be <code>null</code> if we're autowiring a bean that
	 * doesn't belong to the factory.
	 * @param owner owning BeanFactory
	 * @return
	 */
	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
		// Don't override the class with CGLIB if no overrides.
		// 没有方法需要覆盖, 这里MethodOverrides包括了很多的类型
		if (beanDefinition.getMethodOverrides().isEmpty()) {
			// 这里采用指定的构造器或者生成对象的工厂方法来对Bean进行实例化
			Constructor<?> constructorToUse;
			synchronized (beanDefinition.constructorArgumentLock) {
				constructorToUse = (Constructor<?>) beanDefinition.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
					final Class clazz = beanDefinition.getBeanClass();
					if (clazz.isInterface()) {
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							constructorToUse = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor>() {
								public Constructor run() throws Exception {
									return clazz.getDeclaredConstructor((Class[]) null);
								}
							});
						}
						else {
							constructorToUse =	clazz.getDeclaredConstructor((Class[]) null);
						}
						// 这里就设置了使用什么样的方式实例化
						beanDefinition.resolvedConstructorOrFactoryMethod = constructorToUse;
					}
					catch (Exception ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
			// 通过BeanUtils来进行实例化, 这个BeanUtils是通过Constructor来实例化Bean, 在BeanUtils
			// 中可以看到具体的Constructor.newInstance()的实现。
			return BeanUtils.instantiateClass(constructorToUse);
		}
		else {
			// Must generate CGLIB subclass.
			// 使用CGLIB来实例化对象, 如果徐璈方法的覆盖, 则通过CGLIB进行加载
			return instantiateWithMethodInjection(beanDefinition, beanName, owner);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use a no-arg constructor.
	 */
	protected Object instantiateWithMethodInjection(
			RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {

		throw new UnsupportedOperationException(
				"Method Injection not supported in SimpleInstantiationStrategy");
	}

	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			final Constructor<?> ctor, Object[] args) {

		if (beanDefinition.getMethodOverrides().isEmpty()) {
			if (System.getSecurityManager() != null) {
				// use own privileged to change accessibility (when security is on)
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						ReflectionUtils.makeAccessible(ctor);
						return null;
					}
				});
			}
			return BeanUtils.instantiateClass(ctor, args);
		}
		else {
			return instantiateWithMethodInjection(beanDefinition, beanName, owner, ctor, args);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use the given constructor and parameters.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition beanDefinition,
			String beanName, BeanFactory owner, Constructor ctor, Object[] args) {

		throw new UnsupportedOperationException(
				"Method Injection not supported in SimpleInstantiationStrategy");
	}

	public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
			Object factoryBean, final Method factoryMethod, Object[] args) {

		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						ReflectionUtils.makeAccessible(factoryMethod);
						return null;
					}
				});
			}
			else {
				ReflectionUtils.makeAccessible(factoryMethod);
			}
			
			// It's a static method if the target is null.
			return factoryMethod.invoke(factoryBean, args);
		}
		catch (IllegalArgumentException ex) {
			throw new BeanDefinitionStoreException(
					"Illegal arguments to factory method [" + factoryMethod + "]; " +
					"args: " + StringUtils.arrayToCommaDelimitedString(args));
		}
		catch (IllegalAccessException ex) {
			throw new BeanDefinitionStoreException(
					"Cannot access factory method [" + factoryMethod + "]; is it public?");
		}
		catch (InvocationTargetException ex) {
			throw new BeanDefinitionStoreException(
					"Factory method [" + factoryMethod + "] threw exception", ex.getTargetException());
		}
	}

}
