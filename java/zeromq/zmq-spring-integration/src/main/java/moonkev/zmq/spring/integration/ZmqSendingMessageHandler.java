package moonkev.zmq.spring.integration;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ZmqSendingMessageHandler extends AbstractMessageHandler implements Lifecycle, Runnable {

	private ZmqContextManager contextManager;
	
	private String address;
	
	private boolean bind = false;
	
	private Integer socketType;
	
	private volatile boolean running = false;
	
	private Converter<Object, byte[]> converter;
		
	private Thread socketThread;
		
    private BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<byte[]>();
    
    protected final Object lifecycleMonitor = new Object();
	
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		if (converter != null) {
			messageQueue.offer(converter.convert(payload));
		} else if (payload.getClass() == byte[].class) {
			messageQueue.offer((byte[]) payload);
		} else if (payload.getClass() == String.class) {
			messageQueue.offer(((String) payload).getBytes(ZMQ.CHARSET));
		} else {
			throw new MessageHandlingException(message, "Unable to find suitable conversion strategy for message");
		}
	}
	
	public void run() {
		
		Socket socket = contextManager.context().createSocket(socketType);
		if (bind) {
			socket.bind(address);
		} else {
			socket.connect(address);
		}
		
		while (!Thread.currentThread().isInterrupted()) {
			try {
				byte[] payload = messageQueue.poll(Long.MAX_VALUE, TimeUnit.DAYS);
				socket.send(payload);
			} catch (Exception e) {
                if (!contextManager.isRunning()) {
                	break;
                }
			}
		}
		socket.close();
	}

	public void start() {
		synchronized (lifecycleMonitor) {
			if (!running) {
				socketThread = new Thread(this);
				contextManager.registerThread(socketThread);
				socketThread.start();
				running = true;
			}
		}
	}
	
	public void stop() {
		synchronized (lifecycleMonitor) {
			if (running) {
				running = false;
			}
		}
	}
	
	public boolean isRunning() {
		return running;
	}

	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(socketType, "You must provide a socket type");
		Assert.notNull(address, "You must provide a valid ZMQ address");
	}
	
	public void setBind(boolean bind) {
		this.bind = bind;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public void setSocketType(String socketTypeName) {
		Field socketTypeField = ReflectionUtils.findField(ZMQ.class, socketTypeName);
		if (socketTypeField == null  || socketTypeField.getType() != int.class) {
			throw new BeanCreationException(String.format("%s is not a valid ZMQ socket type", socketTypeName));
		}
		socketType = (Integer) ReflectionUtils.getField(socketTypeField, null);
	}
	
	public void setConverter(Converter<Object, byte[]> converter) {
		this.converter = converter;
	}
	
	public void setContextManager(ZmqContextManager contextManager) {
		this.contextManager = contextManager;
	}
}
