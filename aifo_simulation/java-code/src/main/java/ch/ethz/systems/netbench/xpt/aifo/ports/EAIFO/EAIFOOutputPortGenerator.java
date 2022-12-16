package ch.ethz.systems.netbench.xpt.aifo.ports.EAIFO;

import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.network.OutputPort;
import ch.ethz.systems.netbench.core.run.infrastructure.OutputPortGenerator;

public class EAIFOOutputPortGenerator extends OutputPortGenerator {
	private final long maxQueueSize;
	private final int windowSize;
	private final int sampleCount;
	private final double kValue;

	public EAIFOOutputPortGenerator(long maxQueueSize, int windowSize, int sampleCount, double kValue) {
		this.maxQueueSize = maxQueueSize;
		this.windowSize = windowSize;
		this.sampleCount = sampleCount;
		this.kValue = kValue;
	}

	@Override
	public OutputPort generate(NetworkDevice ownNetworkDevice, NetworkDevice towardsNetworkDevice, Link link) {
		return new EAIFOOutputPort(ownNetworkDevice, towardsNetworkDevice, link, maxQueueSize, windowSize, sampleCount, kValue);
	}
}
