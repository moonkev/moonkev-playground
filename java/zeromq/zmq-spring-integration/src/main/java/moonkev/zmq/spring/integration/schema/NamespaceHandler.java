package moonkev.zmq.spring.integration.schema;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

public class NamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter", new ZmqInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new ZmqOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("context-manager", new ZmqContextManagerParser());
	}
}
