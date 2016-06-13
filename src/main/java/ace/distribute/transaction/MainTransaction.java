package ace.distribute.transaction;

import java.util.ArrayList;
import java.util.List;

public class MainTransaction {
    
	private List<SubTransaction> tList = new ArrayList<SubTransaction>();
    private final long transactionId = System.nanoTime();
    private int commitRetryTimes = 3;
    private int rollbackRetryTimes = 3;
    
    public MainTransaction() {
		
	}
    
    public MainTransaction(int commitRetryTimes, int rollbackRetryTimes) {
		super();
		this.commitRetryTimes = commitRetryTimes;
		this.rollbackRetryTimes = rollbackRetryTimes;
	}

    public void addTransaction(SubTransaction t){
    	tList.add(t);
    }

    public boolean commit(){
    	for(int i=0,l=tList.size();i<l;i++){
    		SubTransaction commitTransaction = tList.get(i);
    		
    		int commitTimes = 1;
    		while(commitTimes<=commitRetryTimes){
    			
    			boolean commitResult = commitTransaction.commit(transactionId,commitTransaction.getTransactionId(), commitTransaction.getCommitData());
    			if(commitResult){
    				break;
    			}else if(commitTimes==commitRetryTimes && !commitResult){
    				for(int m=0;m<i;m++){
    					SubTransaction rollbackTransaction = tList.get(m);
    					int rollbackTimes = 1;
    					while(rollbackTimes<=rollbackRetryTimes && !rollbackTransaction.rollback(transactionId,rollbackTransaction.getTransactionId(), rollbackTransaction.getRollbackData())){
    						rollbackTimes++;
    					}
    					
    				}
    				return false;
    			}
    			commitTimes++;
    		}
    		
    	}
    	return true;
    }

}
