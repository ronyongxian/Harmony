# Harmony
a distribute transaction framework


## use local table to record transaction status
![image](https://github.com/ronyongxian/Harmony/blob/master/doc/QQ%E6%88%AA%E5%9B%BE20160613164557.png)


## retry commit or rollback on fail
MainTransaction mainTran = new MainTransaction(5,3);


## client use case
      
    final Server server = new Server();
		
		MainTransaction mainTran = new MainTransaction(5,3);
		
		double price = server.getPrice(1);
		int num = 1;
		int userId = 1;
		int productId = 1;
		
		Order order = new Order(userId,productId,num,price);
		
		SubTransaction<Order> userSubTran = new SubTransaction<Order>(order, order) {

			@Override
			public boolean rollback(long mainTransactionId,long subTransactionId,
					Order data) {
				// TODO Auto-generated method stub
				boolean result = server.rollbackPay(mainTransactionId,subTransactionId,
						data);
				if(result){
					logger.info("支付回滚成功");
					return true;
				}else{
					logger.info("支付回滚失败");
					return false;
				}
				
			}

			@Override
			public boolean commit(long mainTransactionId,long subTransactionId,
					Order data) {
				// TODO Auto-generated method stub
				boolean result = server.pay(mainTransactionId,subTransactionId,
						data);
				if(result){
					logger.info("支付成功");
					return true;
				}else{
					logger.info("支付失败");
					return false;
				}
			}
		};
		mainTran.addTransaction(userSubTran);
		
		SubTransaction<Order> productSubTran = new SubTransaction<Order>(order, order) {
			
			@Override
			public boolean rollback(long mainTransactionId,long subTransactionId,
					Order data) {
				boolean result = server.rollbackReduce(mainTransactionId,subTransactionId, data);
				if(result){
					logger.info("减少库存回滚成功");
					return true;
				}else{
					logger.info("减少库存回滚失败");
					return false;
				}
			}
			
			@Override
			public boolean commit(long mainTransactionId,long subTransactionId,
					Order data) {
				boolean result = server.reduce(mainTransactionId,subTransactionId,
						data);
				if(result){
					logger.info("减少库存成功");
					return true;
				}else{
					logger.info("减少库存失败");
					return false;
				}
			}
		};
		mainTran.addTransaction(productSubTran);
		
		mainTran.commit();
		
		
## server use case
      
    private static final String SQL_UPDATE_USER = "update user set "
			+"account_balance=account_balance-? "
			+"where id=? and account_balance>=?";
			
	private static final String SQL_INSERT_order_form = "insert into order_form("
			+"user_id,product_id,num,status"
			+")values("
			+"?,?,?,?"
			+")";
			
	public boolean pay(long mainTransactionId,long subTransactionId, 
			Order order) {
		List<String> sqlList = new ArrayList<String>();
		List<Object[]> paramList = new ArrayList<Object[]>();
		sqlList.add(SQL_UPDATE_USER);
		double money = new BigDecimal(order.getNum()).multiply(new BigDecimal(order.getPrice())).doubleValue();
		paramList.add(new Object[]{money,order.getUserId(),money});
		
		sqlList.add(SQL_INSERT_order_form);
		paramList.add(new Object[]{order.getUserId(),order.getProductId(),order.getNum(),1});
		
		return TransactionUtil.commit(mainTransactionId,subTransactionId, 
				getUserConnection(), sqlList, paramList)!=null;

	}
	
	private static final String SQL_UPDATE_user_rollback = "update user set "
			+"account_balance=account_balance+? "
			+"where id=?";
	private static final String SQL_UPDATE_order_form_rollback = "update order_form set "
			+"status=0 "
			+"where id=? ";
	public boolean rollbackPay(long mainTransactionId,long subTransactionId,
			Order order) {
		List<String> sqlList = new ArrayList<String>();
		List<Object[]> paramList = new ArrayList<Object[]>();
		sqlList.add(SQL_UPDATE_user_rollback);
		double money = new BigDecimal(order.getNum()).multiply(new BigDecimal(order.getPrice())).doubleValue();
		paramList.add(new Object[]{money,order.getUserId()});
		
		sqlList.add(SQL_UPDATE_order_form_rollback);
		paramList.add(new Object[]{order.getUserId()});
		
		return TransactionUtil.rollback(mainTransactionId,subTransactionId, 
				getUserConnection(), sqlList, paramList)!=null;
	}
	
	private static final String SQL_UPDATE_STOCK = "update product set "
			+"num=num-? "
			+"where id=? and num>=?";
	public boolean reduce(long mainTransactionId,long subTransactionId,
			Order order){
		List<String> sqlList = new ArrayList<String>();
		List<Object[]> paramList = new ArrayList<Object[]>();
		sqlList.add(SQL_UPDATE_STOCK);
		paramList.add(new Object[]{order.getNum(),order.getProductId(),order.getNum()});
		
		return TransactionUtil.commit(mainTransactionId,subTransactionId, 
				getProductConnection(), sqlList, paramList)!=null;

	}
	
	private static final String SQL_UPDATE_STOCK_rollback = "update product set "
			+"num=num+? "
			+"where id=?";
	public boolean rollbackReduce(long mainTransactionId,long subTransactionId,
			Order order) {
		List<String> sqlList = new ArrayList<String>();
		List<Object[]> paramList = new ArrayList<Object[]>();
		sqlList.add(SQL_UPDATE_STOCK_rollback);
		paramList.add(new Object[]{order.getNum(),order.getProductId()});
		
		return TransactionUtil.rollback(mainTransactionId,subTransactionId, 
				getProductConnection(), sqlList, paramList)!=null;

	}
