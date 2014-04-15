package moonkev.zmq.spring.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import moonkev.zmq.spring.integration.msgpack.MsgpackContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration(locations="/zmq-channel-adapter-test.xml")
public class ZmqChannelAdapterTest extends AbstractTestNGSpringContextTests {

	static interface Gateway {
		void send(Object data);
	}
	
	@Autowired
	Gateway msgpackContainerGateway;
	
	@Autowired
	Gateway msgpackMapGateway;
	
	@Autowired
	Gateway jsonMapGateway;
	
	@Test
	public void msgpackContainerChannelTest() throws Exception {
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
			msgpackContainerGateway.send(container);
		}
		Thread.sleep(1000);
	}
	
	@Test
	public void msgpackMapChannelTest() throws Exception {
		for (int i = 0; i < 100; ++i) {
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put("Count", i);
			map.put("One", 1);
			map.put("Two", 2.0);
			map.put("Three", "MessagePack!");
			msgpackMapGateway.send(map);
		}
		Thread.sleep(1000);
	}
	
	@Test
	public void jsonMapTest() throws Exception {
		for (int i = 0; i < 100; ++i) {
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put("Count", i);
			map.put("One", 1);
			map.put("Two", 2.0);
			map.put("Three", "JSON!");
			jsonMapGateway.send(map);
		}
		Thread.sleep(1000);
	}
}