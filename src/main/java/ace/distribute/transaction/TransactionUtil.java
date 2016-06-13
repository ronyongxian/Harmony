package ace.distribute.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionUtil {

	private static Logger logger = LoggerFactory.getLogger(TransactionUtil.class);
	
	public static List<Integer> commit(long mainTransactionId,long subTransactionId,
			final Connection conn, final List<String> sqlList, final List<Object[]> paramList){

		return operate(mainTransactionId,subTransactionId, conn, sqlList, paramList, CommandType.COMMIT);
		
	}
	
	public static List<Integer> rollback(long mainTransactionId,long subTransactionId,
			final Connection conn, final List<String> sqlList, final List<Object[]> paramList){
		return operate(mainTransactionId,subTransactionId, conn, sqlList, paramList, CommandType.ROLLBACK);
		
	}
	
	
	private static interface Task{
		List<Integer> execute(Connection conn) throws SQLException;
	}

	private enum CommandType{
		COMMIT,ROLLBACK;
	}

	private static List<Integer> transact(Connection conn,Task task){
		List<Integer> list = null;
		boolean autoCommit = true;
		try {
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			list = task.execute(conn);
			conn.commit();

		} catch (SQLException e) {
			logger.error("update exception",e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				logger.error("rollback exception",e1);
			}
		} finally{
			try {
				conn.setAutoCommit(autoCommit);
			} catch (SQLException e) {
				logger.error("reset connection exception", e);
			}
			close(conn, null, null);
		}

		return list;
	}

	
	private static List<Integer> operate(final long mainTransactionId,final long subTransactionId,
			final Connection conn, final List<String> sqlList, final List<Object[]> paramList,
			final CommandType commandType){

		Task task = new Task() {

			public List<Integer> execute(Connection conn) throws SQLException {
				
				List<Integer> list = new ArrayList<Integer>();
				
				boolean shouldCommit = commandType==CommandType.COMMIT && 
						(insertTran(mainTransactionId,subTransactionId, 1, conn)>0 
								|| updateTran(mainTransactionId,subTransactionId, 0,1, conn)>0);
				boolean shouldRollBack = commandType==CommandType.ROLLBACK &&
						updateTran(mainTransactionId,subTransactionId, 1,0, conn)>0;
						
				if(shouldCommit || shouldRollBack){
					for(int i=0,l=sqlList.size();i<l;i++){
						String sql = sqlList.get(i);
						int numOrId = -1;
						if(sql.contains("insert")){
							numOrId = executeInsertForId(conn, sql, paramList.get(i));
						}else if(sql.contains("update") || sql.contains("delete")){
							numOrId = executeUpdateForAffectRowNum(conn, sql, paramList.get(i));
						}
						
						if(numOrId<1){
							conn.rollback();
							return null;
						}else{
							list.add(numOrId);
						}
					}
					
				}
				
				return list;
			}
		};

		return transact(conn, task);
	}

	private static final String SQL_INSERT_TRANSACTION = "insert into transaction(main_transaction_id,sub_transaction_id,status)values(?,?,?)";
	private static int insertTran(long mainTransactionId,long subTransactionId, int status, 
			Connection conn) throws SQLException{
		return executeUpdateForAffectRowNum(conn, SQL_INSERT_TRANSACTION, 
				new Object[]{mainTransactionId,subTransactionId,status});
	}

	private static final String SQL_UPDATE_TRANSACTION = "update transaction set status=? where main_transaction_id=? and sub_transaction_id=? and status=?";
	private static int updateTran(long mainTransactionId,long subTransactionId, int status,int newStaus, 
			Connection conn) throws SQLException{
		return executeUpdateForAffectRowNum(conn, SQL_UPDATE_TRANSACTION, 
				new Object[]{newStaus, mainTransactionId, subTransactionId,status});
	}

	/**
	 * 
	 * @param conn
	 * @param sql
	 * @param params
	 * @return 
	 * @throws SQLException
	 */
	private static int executeUpdateForAffectRowNum(Connection conn,String sql,Object[] params) throws SQLException{

		int num = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			if(params!=null){
				for(int i=0,l=params.length;i<l;i++){
					pstmt.setObject(i+1, params[i]);
				}
			}

			num = pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.error("update exception",e);
			throw e;
		} finally{
			close(null, pstmt, null);
		}
		
		return num;
	}

	/**
	 * 
	 * @param conn
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	private static int executeInsertForId(Connection conn,String sql,Object[] params) throws SQLException{

		int id = -1;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			if(params!=null){
				for(int i=0,l=params.length;i<l;i++){
					pstmt.setObject(i+1, params[i]);
				}
			}

			if(pstmt.executeUpdate()<1){
				return id;
			}
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("insert exception",e);
			throw e;
		} finally{
			close(null, pstmt, rs);
		}
		
		return id;
	}


	private static void close(Connection conn,PreparedStatement pstmt,ResultSet rs){
		try{
			if(rs!=null){
				rs.close();
			}
			if(pstmt!=null){
				pstmt.close();
			}
			if(conn!=null){
				conn.close();
			}
		}catch(Exception e){
			logger.error("close connection exception", e);
		}
	}



}
