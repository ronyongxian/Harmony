package ace.distribute.transaction;

public abstract class SubTransaction<T> {

	private long transactionId = System.nanoTime();
	private T commitData,rollbackData;
	
	public SubTransaction(T commitData,
			T rollbackData) {
		super();
		this.commitData = commitData;
		this.rollbackData = rollbackData;
	}
	
	public abstract boolean commit(long mainTransactionId,long subTransactionId,  T commitData);

    public abstract boolean rollback(long mainTransactionId,long subTransactionId, T rollbackData);
	
	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}

	public T getCommitData() {
		return commitData;
	}

	public void setCommitData(T commitData) {
		this.commitData = commitData;
	}

	public T getRollbackData() {
		return rollbackData;
	}

	public void setRollbackData(T rollbackData) {
		this.rollbackData = rollbackData;
	}
}
