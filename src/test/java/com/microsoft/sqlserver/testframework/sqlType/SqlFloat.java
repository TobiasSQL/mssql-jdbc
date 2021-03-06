/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */

package com.microsoft.sqlserver.testframework.sqlType;

import java.sql.JDBCType;
import java.util.concurrent.ThreadLocalRandom;

public class SqlFloat extends SqlType {

    // called from real
    SqlFloat(String name,
            JDBCType jdbctype,
            int precision,
            Object min,
            Object max,
            Object nullvalue,
            VariableLengthType variableLengthType) {
        super(name, jdbctype, precision, 0, min, max, nullvalue, variableLengthType);
        generatePrecision();
    }

    public SqlFloat() {
        super("float", JDBCType.DOUBLE, 53, 0, SqlTypeValue.FLOAT.minValue, SqlTypeValue.FLOAT.maxValue, SqlTypeValue.FLOAT.nullValue,
                VariableLengthType.Precision);
        generatePrecision();
    }

    public Object createdata() {
        // TODO: include max value
        if (precision > 24) {
            return Double.longBitsToDouble(ThreadLocalRandom.current().nextLong(((Double) minvalue).longValue(), ((Double) maxvalue).longValue()));
        }
        else {
            return new Float(ThreadLocalRandom.current().nextDouble(new Float(-3.4E38), new Float(+3.4E38)));
        }
    }
}