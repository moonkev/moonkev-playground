package moonkev.zmq.spring.integration.schema;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ZmqContextManagerParser extends AbstractBeanDefinitionParser {

	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext context) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"moonkev.zmq.spring.integration.ZmqContextManager");
		builder.addConstructorArgValue(element.getAttribute("io-threads"));
		return builder.getBeanDefinition();
	}
}
