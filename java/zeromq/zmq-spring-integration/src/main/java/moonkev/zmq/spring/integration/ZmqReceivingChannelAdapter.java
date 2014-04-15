package moonkev.zmq.spring.integration;

import java.lang.reflect.Field;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ZmqReceivingChannelAdapter extends MessageProducerSupport implements Runnable {
		
	private ZmqContextManager contextManager;
	
	private String address;
	
	private boolean bind = false;
	
	private String topic = "";
	
	private Integer socketType;
	
	private Converter<byte[], Object> converter;
	
	private Thread socketThread;
			
	public void run() {
		Socket socket = contextManager.context().createSocket(socketType);
		if (bind) {
			socket.bind(address);
		} else {
			socket.connect(address);
		}
		
		if (socketType == ZMQ.SUB) {
			socket.subscribe(topic.getBytes(ZMQ.CHARSET));
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				Object payload = null;
				if (converter != null) {
					payload = converter.convert(socket.recv());
				} else {
					payload = socket.recvStr();
				}
				sendMessage(MessageBuilder.withPayload(payload).build());
			} catch (Exception e) {
				if (!contextManager.isRunning()){
					break;
				}
			}
		}
		socket.close();
	}
		
	protected void onInit() {
		super.onInit();
		Assert.notNull(socketType, "You must provide a socket type");
		Assert.notNull(address, "You must provide a valid ZMQ address");
	}
	
	protected void doStart() {
		socketThread = new Thread(this);
		socketThread.start();
	}
	
	public void setContextManager(ZmqContextManager contextManager) {
		this.contextManager = contextManager;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public void setBind(boolean bind) {
		this.bind = bind;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public void setSocketType(String socketTypeName) {
		Field socketTypeField = ReflectionUtils.findField(ZMQ.class, socketTypeName);
		if (socketTypeField == null  || socketTypeField.getType() != int.class) {
			throw new BeanCreationException(String.format("%s is not a valid ZMQ socket type", socketTypeName));
		}
		socketType = (Integer) ReflectionUtils.getField(socketTypeField, null);
	}
	
	public void setConverter(Converter<byte[], Object> converter) {
		this.converter = converter;
	}
}
