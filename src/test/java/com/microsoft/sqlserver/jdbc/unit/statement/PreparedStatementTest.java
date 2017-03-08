/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.unit.statement;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import com.microsoft.sqlserver.testframework.AbstractTest;

@RunWith(JUnitPlatform.class)
public class PreparedStatementTest extends AbstractTest {
    private void executeSQL(SQLServerConnection conn, String sql) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute(sql);
    }

    private int executeSQLReturnFirstInt(SQLServerConnection conn, String sql) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery(sql);
        
        int returnValue = -1;

        if(result.next())
            returnValue = result.getInt(1);

        return returnValue;
    }

    /**
     * Test handling of unpreparing prepared statements.
     * 
     * @throws SQLException
     */
    @Test
    public void testBatchedUnprepare() throws SQLException {
        SQLServerConnection conOuter = null;
        try (SQLServerConnection con = (SQLServerConnection)DriverManager.getConnection(connectionString)) {
            conOuter = con;
            try {
                
                // Clean-up proc cache
                this.executeSQL(con, "DBCC FREEPROCCACHE;");
                
                String queryCacheLookup = "/*unpreparetest%*/SELECT * FROM sys.tables;";
                String query = "/*unpreparetest only sp_executesql*/SELECT * FROM sys.tables;";

                // Verify nothing in cache.
                String verifyTotalCacheUsesQuery = String.format("SELECT CAST(ISNULL(SUM(usecounts), 0) AS INT) FROM sys.dm_exec_cached_plans AS p CROSS APPLY sys.dm_exec_sql_text(p.plan_handle) AS s WHERE s.text LIKE '%%%s'", queryCacheLookup);

                assertSame(0, executeSQLReturnFirstInt(con, verifyTotalCacheUsesQuery));

                int iterations = 25;

                // Verify no prepares for 1 time only uses.
                for(int i = 0; i < iterations; ++i){
                    try (SQLServerPreparedStatement pstmt = (SQLServerPreparedStatement)con.prepareStatement(query)) {
                        pstmt.execute();
                    }
                    assertSame(0, con.outstandingPreparedStatementDiscardActionCount());
                }

                // Verify total cache use.
                assertSame(iterations, executeSQLReturnFirstInt(con, verifyTotalCacheUsesQuery));

                query = "/*unpreparetest, sp_executesql->sp_prepexec->sp_execute- batched sp_unprepare*/SELECT * FROM sys.tables;";
                int prevDiscardActionCount = 0;
     
                // Now verify unprepares are needed.                 
                for(int i = 0; i < iterations; ++i){

                    // Verify current queue depth is expected.
                    assertSame(prevDiscardActionCount, con.outstandingPreparedStatementDiscardActionCount());
                    
                    try (SQLServerPreparedStatement pstmt = (SQLServerPreparedStatement)con.prepareStatement(query)) {
                        pstmt.execute(); // sp_executesql
                
                        pstmt.execute(); // sp_prepexec
                        ++prevDiscardActionCount;

                        pstmt.execute(); // sp_execute
                    }

                    // Verify clean-up is happening as expected.
                    if(prevDiscardActionCount > SQLServerConnection.PREPARED_STATEMENT_CLEANUP_THRESHOLD){
                        prevDiscardActionCount = 0;
                    }

                    assertSame(prevDiscardActionCount, con.outstandingPreparedStatementDiscardActionCount());
                }  

                // Verify total cache use.
                assertSame(iterations * 4, executeSQLReturnFirstInt(con, verifyTotalCacheUsesQuery));
              
            } 
            finally {
            }
        }
        // Verify clean-up happened on connection close.
        assertSame(0, conOuter.outstandingPreparedStatementDiscardActionCount());        
    }

}
