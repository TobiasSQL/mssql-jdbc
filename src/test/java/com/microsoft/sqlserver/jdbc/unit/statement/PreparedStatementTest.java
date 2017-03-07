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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import com.microsoft.sqlserver.testframework.AbstractTest;

@RunWith(JUnitPlatform.class)
public class PreparedStatementTest extends AbstractTest {
    //private static final String tableName = "[" + RandomUtil.getIdentifier("PreparedStatement") + "]";
    
    /**
     * Test ParameterMetaData#isWrapperFor and ParameterMetaData#unwrap.
     * 
     * @throws SQLException
     */
    @Test
    public void testBatchedUnprepare() throws SQLException {
        try (SQLServerConnection con = (SQLServerConnection)DriverManager.getConnection(connectionString)) {
            try {
                String query = "/*unpreparetest*/SELECT * FROM sys.tables;";

                int prevDiscardActionCount = 0;

                // Exceed the threshold a few times.                
                for(int i = 0; i <= 25; ++i){

                    // Verify current queue depth is expected.
                    assertSame(prevDiscardActionCount, con.outstandingPreparedStatementDiscardActionCount());
                    
                    try (SQLServerPreparedStatement pstmt = (SQLServerPreparedStatement)con.prepareStatement(query)) {
                        pstmt.execute();
                    }

                    // Verify clean-up is happening as expected.
                    if(prevDiscardActionCount == SQLServerConnection.PREPARED_STATEMENT_CLEANUP_THRESHOLD){
                        prevDiscardActionCount = 0;
                    }
                    else{
                        ++prevDiscardActionCount;               
                    }

                    assertSame(prevDiscardActionCount, con.outstandingPreparedStatementDiscardActionCount());
                }                
            } 
            finally {
            }

        }
    }

}
