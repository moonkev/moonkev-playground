package moonkev.zmq.spring.integration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.Lifecycle;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class ZmqSendingMessageHandler extends AbstractMessageHandler implements Lifecycle, Runnable, ZmqContextShutdownListener {

	private ZmqContextManager contextManager;
	
	private String address;
	
	private boolean bind = false;
	
	private Integer socketType;
	
	private volatile boolean running = false;
	
	private byte[] topicBytes = null;
			
	private Converter<Object, byte[]> converter;
		
	private Thread socketThread;
		
	private BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>();
    
	protected final Object lifecycleMonitor = new Object();
    
	protected final Object startupMonitor = new Object();
	
	protected void handleMessageInternal(Message<?> message) throws Exception {
		messageQueue.offer(message);
	}
	
	public void run() {
		
		Socket socket = null;
		
		synchronized (startupMonitor) {
			socket = contextManager.context().createSocket(socketType);
			try {
				if (bind) {
					socket.bind(address);
				} else {
					socket.connect(address);
				}
			} finally {
				startupMonitor.notify();
			}
		}
		
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Message<?> message = messageQueue.poll(Long.MAX_VALUE, TimeUnit.DAYS);
				byte[] payload = converter.convert(message.getPayload());
				if (topicBytes == null) {
					socket.send(payload);
				} else {
					byte[] msgTopic = null;
					if (message.getHeaders().containsKey("zmq.topic")) {
						msgTopic = message.getHeaders().get("zmq.topic", String.class).getBytes(ZMQ.CHARSET);
					} else {
						msgTopic = topicBytes;
					}
					byte[] topicPayload = new byte[msgTopic.length + payload.length];
					System.arraycopy(msgTopic, 0, topicPayload, 0, msgTopic.length);
					System.arraycopy(payload, 0, topicPayload, msgTopic.length, payload.length);
					socket.send(topicPayload);
				}
			} catch (Exception e) {
                if (!contextManager.isRunning()) {
                	break;
                }
                logger.error("Exception in zmq sending message handler", e);
			}
		}
		socket.close();
	}

	public void start() {
		synchronized (lifecycleMonitor) {
			if (!running) {
				socketThread = new Thread(this);
				contextManager.registerShutdownListener(this);
				socketThread.start();
				try {
					synchronized (startupMonitor) {
						startupMonitor.wait();
					}
				} catch (InterruptedException e) {
					throw new BeanCreationException("Lifecycle.start() Interupted while creating zmq socket thread.", e);
				}
				running = true;
			}
		}
	}
	
	public void stop() {
		synchronized (lifecycleMonitor) {
			if (running) {
				socketThread.interrupt();
				running = false;
			}
		}
	}
	
	public void shutdownZmq() {
		this.stop();
	}
	
	public boolean isRunning() {
		return running;
	}

	protected void onInit() throws Exception {
		super.onInit();
		Assert.notNull(socketType, "You must provide a socket type");
		Assert.notNull(address, "You must provide a valid ZMQ address");
		Assert.notNull(converter, "You must provide a converter");
	}
	
	public void setBind(boolean bind) {
		this.bind = bind;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public void setTopic(String topic) {
		this.topicBytes = topic.getBytes(ZMQ.CHARSET);
	}
	
	public void setSocketType(String socketTypeName) {
		socketType = ZmqEndpointUtil.setSocketType(socketTypeName);
	}
	
	public void setConverter(Converter<Object, byte[]> converter) {
		this.converter = converter;
	}
	
	public void setContextManager(ZmqContextManager contextManager) {
		this.contextManager = contextManager;
	}
}
