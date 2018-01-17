
public class ChildNode {

	private ChildStatus childStatus=ChildStatus.PENDING;
	private String transportAddress;
	
	public ChildNode(String transportAddress) {
		this.transportAddress=transportAddress;
	}

	public ChildStatus getChildStatus() {
		return childStatus;
	}

	public void setChildStatus(ChildStatus childStatus) {
		this.childStatus = childStatus;
	}

	public String getTransportAddress() {
		return transportAddress;
	}

	public void setTransportAddress(String transportAddress) {
		this.transportAddress = transportAddress;
	}
	
	
}
