package moonkev.zmq.spring.integration;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.context.Lifecycle;
import org.zeromq.ZContext;

public class ZmqContextManager implements Lifecycle {

	private ZContext context;
		
	private volatile boolean running = false;
		
	private Collection<Thread> socketThreads = new HashSet<Thread>();
	
	public ZmqContextManager(int ioThreads) {
		context = new ZContext(ioThreads);
	}
	
	public ZContext context() {
		return context;
	}

	public void start() {
		running = true;
	}
	
	public void stop() {
		running = false;
		for (Thread thread : socketThreads) {
			thread.interrupt();
		}
		context.getContext().term();
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void registerThread(Thread thread) {
		socketThreads.add(thread);
	}
}
