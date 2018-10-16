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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface.
 * Reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). <code>&lt;beans&gt;</code> doesn't need to be the root
 * element of the XML document: This class will parse all bean definition elements
 * in the XML file, not regarding the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	/**
	 * 别名元素节点
	 */
	public static final String ALIAS_ELEMENT = "alias";

	/**
	 * name 元素节点名称
	 */
	public static final String NAME_ATTRIBUTE = "name";

	/**
	 * alias属性定义
	 */
	public static final String ALIAS_ATTRIBUTE = "alias";

	/**
	 * import节点
	 */
	public static final String IMPORT_ELEMENT = "import";

	/**
	 * resource属性
	 */
	public static final String RESOURCE_ATTRIBUTE = "resource";

	protected final Log logger = LogFactory.getLog(getClass());

	private XmlReaderContext readerContext;


	/**
	 * Parses bean definitions according to the "spring-beans" DTD.
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at <code>&lt;beans&gt;</code> level; then parses
	 * the contained bean definitions.
	 */
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;

		logger.debug("Loading bean definitions");
		Element root = doc.getDocumentElement();

		// 该处创建了一个委派对象，主要用于对Xml文件流进行处理
		BeanDefinitionParserDelegate delegate = createHelper(readerContext, root);

		// 这是一个hook方法，该方法内部为空，在执行前对xml信息进行一些处理
		preProcessXml(root);

		// 真正开始解析XML文件并解析为BeanDefinition的地方
		parseBeanDefinitions(root, delegate);

		// 该处也是一个hook方法，该处针对的是在解析BeanDefinition完成后，
		// 对xml进行一些特定的处理
		postProcessXml(root);
	}

	/**
	 * 创建{@link BeanDefinitionParserDelegate}, 这里是最主要解析并获取BeanDefintion的地方
	 * @param readerContext 读取资源的上下文容器
	 * @param root 需要解析的根节点
	 * @return
	 */
	protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root) {
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		delegate.initDefaults(root);
		return delegate;
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
	 * source metadata from the supplied {@link Element}.
	 */
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}


	/**
	 * Parse the elements at the root level in the document:
	 * "import", "alias", "bean".
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// 该处会判断是否使用了默认的命名空间，也即是 .../beans
		// 这里判断一个Element是否为isDefaultNameSpace而言，是通过标签是否包含了前缀来判断
		// 比如<aop:config/>指向的就是aop的命名空间
		if (delegate.isDefaultNamespace(root)) {
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;

					// 判断该元素是否为命名空间定义的元素
					if (delegate.isDefaultNamespace(ele)) {
						// 这里主要解析的是/beans下的, 并且是在http://www.springframework.org/schema/beans
						// 中定义的元素
						parseDefaultElement(ele, delegate);
					}
					else {
						// 解析自定义元素
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		else {
			// 解析自定义元素
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * 按照约定的{@link BeanDefinitionParserDelegate#BEANS_NAMESPACE_URI}进行解析
	 * @param ele
	 * @param delegate
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {

	  // 该处为了解析<import>节点, 这里实际上是根据节点的定义顺序
		// 来实现进行解析和加载, 因此, 这个方法可能会被递归调用很多次
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}

		// 解析一些别名的空间
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}

		// 解析<bean>节点,你会发现，其实这里只是解析了bean节点，没有做其他任何
		// 节点的解析操作
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {

	  // 加载import节点中的resource属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// Resolve system properties: e.g. "${user.dir}"
		location = SystemPropertyUtils.resolvePlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

		// Discover whether the location is an absolute or relative URI
		boolean absoluteLocation = false;
		try {

			// 该处是对import节点的resource的属性配置进行解析,
			// 主要是判断是否为url, classpath*:, classpath:以及代表了URI的资源
			// 如果表现的为url, classpath*:, classpath:以及对应的URI的资源，则返回为true
			absoluteLocation = ResourcePatternUtils.isUrl(location)
					|| ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// Absolute or relative?
		if (absoluteLocation) {
			try {
				// 如果是绝对路径的话，则还是通过XmlBeanDefinitionReader进行读取
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		else {
			// 如果不是绝对路径，则考虑
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				// 我觉得该处的处理其实还是比较的简单，对于相对路径而言，则只是替换最后的部分
				// 例如: org/spring/beans.xml --> org/spring/other.xml
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					// 如果Reource存在的情况下，则还是会通过XmlBeanDefinitionReader的方法加载xml资源的定义
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					// 如果对应的文件不存在, 这里时期是一个小小的设计在这里，如果
					// 是通过URL标识的方式，则这几过去baseLoction，然后替换掉最后的部分，
					// 最终还是会通过XmlBeanDefintionReader进行加载. 该处有一点需要澄清，
					// 因为我是从FileSystemXmlApplicationContext开始看的，因此这里会有一点误解的地方.
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}

		// 获取实际加载的Resource列表
		Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
		// 这里我很懵逼了? 这个有什么用吗？ 该处好像只是一个通知，告知哪些文件被import了
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 */
	protected void processAliasRegistration(Element ele) {
		// 获取name属性
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		// 获取alias属性
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				getReaderContext().getRegistry().registerAlias(name, alias);
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}

			// 该处发出通知，告知alias已经被注册
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {

		// 此处可以看到哦，实际上BeanDefinitionParseDelegate实际上会降对BeanDefinition的引用
		// 交由BeanDefinitionHolder来进行处理。在BeanDefinitionHolder中，实际只是包含了
		// BeanDefinition还有一些必要的beanName, aliases的相关参数
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);

		if (bdHolder != null) {
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				// 最终将BeanDefinition注册到BeanFactory中，是通过工具类去实现的
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// Send registration event. 该处主要是发送beanDefinition注册成功的时间消息
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
