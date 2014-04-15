package moonkev.zmq.spring.integration.schema;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

public class ZmqOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser  {

	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"moonkev.zmq.spring.integration.ZmqSendingMessageHandler");
		
		builder.addPropertyValue("address", element.getAttribute("address"));
		builder.addPropertyValue("socketType", element.getAttribute("socket-type"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "bind");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "context-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "converter");
		
		return builder.getBeanDefinition();
	}
}
