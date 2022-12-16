package ch.ethz.systems.netbench.xpt.aifo.ports.EAIFO;


import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.network.Packet;
import ch.ethz.systems.netbench.ext.basic.IpHeader;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import ch.ethz.systems.netbench.xpt.tcpbase.PriorityHeader;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EAIFOOutputPort extends OutputPort {
	private final long maxQueueSize;
	private int windowSize;
	private Lock reentrantLock;
	private double k;
	private int maxRank;
	private Queue qWindow;
	private int count;
	private double avgqlen;
	private double avgcount;
	private double qlen;
	private int sampleCount;
	private boolean isServer;

	EAIFOOutputPort(NetworkDevice ownNetworkDevice, NetworkDevice targetNetworkDevice, Link link, long maxQueueSize, int windowSize, int sampleCount, double kValue) {
		super(ownNetworkDevice, targetNetworkDevice, link, new LinkedBlockingQueue<Packet>());
		//super(ownNetworkDevice, targetNetworkDevice, link, new WFQAIFOOutputQueue<Packet>());
		// ignore this maxQueueSize
		this.isServer = ownNetworkDevice.isServer();
		this.maxQueueSize = maxQueueSize;
		this.windowSize = windowSize;
		this.reentrantLock = new ReentrantLock();
		// ** k is set as 0.1 here, can be changed e.g., among [0.1, 0.5]
		//this.k = 0.1f;
		this.k = kValue;
		maxRank = 0;

		this.qWindow = new LinkedList();
		this.count = 0;

		// avgqlen and avgcount are used for analysis
		this.avgqlen = 0;
		this.avgcount = 0;

		// qlen is the "C" in the algorithm (maximum number of packets we can put into the queue)
		// ignore "this.maxQueueSize"
		//this.qlen = 1000.0f;
		this.qlen = maxQueueSize;

		this.sampleCount = sampleCount;
//        this.sampleCount = 1;
//		this.qlen = 100;
	}

	@Override
	public void enqueue(Packet packet) {
		PriorityHeader header = (PriorityHeader) packet;
		this.reentrantLock.lock();
		try {
			int rank = (int) header.getPriority();
			this.avgcount++;
			this.avgqlen += buffQueue.size();

			boolean admitFlag = myCompareQuantile(rank, buffQueue.size() / this.qlen);
			// boolean admitFlag = compareQuantile(rank, 1.0/(1-k) * (this.qlen - buffQueue.size()) / this.qlen);
			if (this.count == 0) {
				if (qWindow.size() < this.windowSize) {
					qWindow.add(packet);
				} else {
					qWindow.poll();
					qWindow.add(packet);
				}
			}
			this.count++;
			if (this.count == this.sampleCount) {
				this.count = 0;
			}

			if ((buffQueue.size() <= 0.105*this.qlen) || (admitFlag)) {
			// if (admitFlag) {
				IpHeader ipHeader = (IpHeader) packet;

				if (buffQueue.size() + 1 <= this.qlen) {
					// Check whether there is an inversion for the packet enqueued (count)
					if (SimulationLogger.hasInversionsTrackingEnabled()) {

						// Extract the packet rank
						FullExtTcpPacket p = (FullExtTcpPacket) packet;

						// We compute the perceived rank
						Object[] contentPIFO = super.getQueue().toArray();
						int inversion_count = 0;
						if (contentPIFO.length > 0) {
							for (int j = 0; j < contentPIFO.length; j++) {
								int r = (int) ((FullExtTcpPacket) contentPIFO[j]).getPriority();
								if (r > p.getPriority()) {
									inversion_count++;
								}
							}
							if (inversion_count != 0) {
								SimulationLogger.logInversionsPerRank(this.getOwnId(), (int) p.getPriority(), inversion_count);
							}
						}
					}


					guaranteedEnqueue(packet);
				} else {
					SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");
					if (ipHeader.getSourceId() == this.getOwnId()) {
						SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED_AT_SOURCE");
					}
				}
			} else {
				SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED");
				IpHeader ipHeader = (IpHeader) packet;
				if (ipHeader.getSourceId() == this.getOwnId()) {
					SimulationLogger.increaseStatisticCounter("PACKETS_DROPPED_AT_SOURCE");
				}
			}
		} finally {
			this.reentrantLock.unlock();
		}
	}

	public boolean compareQuantile(int priority, double quantile) {
        if (qWindow.size() == 0)
            return true;
        Object[] contentPIFO = qWindow.toArray();
        int count = 0;
        for (int i =0; i<contentPIFO.length; i++) {
            FullExtTcpPacket packet = (FullExtTcpPacket) contentPIFO[(int) i];
            if (priority > packet.getPriority())
                count += 1;
        }
        if (count * 1.0  < quantile * qWindow.size())
            return true;
        return false;
    }

	public boolean myCompareQuantile(int priority, double usage) {
		if (qWindow.size() == 0)
			return true;
		Object[] contentPIFO = qWindow.toArray();
		int count = 0;
		for (int i = 0; i < contentPIFO.length; i++) {
			FullExtTcpPacket packet = (FullExtTcpPacket) contentPIFO[(int) i];
			if (priority > packet.getPriority())
				count += 1;
		}
		double quantile = count * 1.0 / qWindow.size();
		return (Math.pow(1 - quantile, this.k) + Math.pow((10 - 10*usage)/9, this.k) > 1);
		// return ((1 - quantile) + (1 - usage) > 1);
	}

}
