/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.support.MethodMatchers;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * 这里获取拦截器列表以及动态拦截通知
	 * @param config the AOP configuration in the form of an Advised object
	 * @param method the proxied method
	 * @param targetClass the target class
	 * @return
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, Class targetClass) {

		// This is somewhat tricky... we have to process introductions first,
		// but we need to preserve(保存,保护, 维持) order in the ultimate(最终的, 根本的) list.
		List<Object> interceptorList = new ArrayList<Object>(config.getAdvisors().length);

		// 根据IntroductionAdvisor对targetClass进行过滤，判断是否被introductions过滤
		boolean hasIntroductions = hasMatchingIntroductions(config, targetClass);

		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

		// 遍历所有的通知器
		for (Advisor advisor : config.getAdvisors()) {
			// 判断是否为切面通知器
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 匹配Class
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(targetClass)) {

					// 获取当前advisor能够支持的Interceptor列表
					MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();

					// 判断是否支持拦截的方法
					if (MethodMatchers.matches(mm, method, targetClass, hasIntroductions)) {
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			// 如果是IntroductionAdvisor通知器
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(targetClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 * 该方法用于在通知器列表中, 判断是否包含了{@link IntroductionAdvisor}通知器, 如果包含了, 判断{@code targetClass}是否能够被匹配。
	 */
	private static boolean hasMatchingIntroductions(Advised config, Class targetClass) {
		for (int i = 0; i < config.getAdvisors().length; i++) {
			Advisor advisor = config.getAdvisors()[i];
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(targetClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
