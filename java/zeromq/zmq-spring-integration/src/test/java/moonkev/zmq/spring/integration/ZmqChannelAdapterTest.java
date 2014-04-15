package moonkev.zmq.spring.integration;

import java.util.ArrayList;
import java.util.HashMap;

import moonkev.zmq.spring.integration.msgpack.MsgpackContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(locations="/zmq-channel-adapter-test.xml")
public class ZmqChannelAdapterTest extends AbstractTestNGSpringContextTests {

	static interface Gateway {
		void send(MsgpackContainer container);
	}
	
	@Autowired
	Gateway gateway;
	
	@Test
	public void channelTest() throws Exception {
		for (int i = 0; i < 100; ++i) {
			MsgpackContainer container = new MsgpackContainer();
			container.doubleField = 25.5;
			container.intField = i;
			container.stringField = "A field";
			container.listField = new ArrayList<String>();
			container.listField.add("First");
			container.listField.add("Second");
			container.mapField = new HashMap<String, String>();
			container.mapField.put("Alpha", "X");
			container.mapField.put("Omega", "Y");
			gateway.send(container);
		}
		Thread.sleep(1000);
	}
}