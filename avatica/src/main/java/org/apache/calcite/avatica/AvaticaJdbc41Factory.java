/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.avatica;

import java.io.InputStream;
import java.io.Reader;
import java.sql.NClob;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Implementation of {@link AvaticaFactory} for JDBC 4.1 (corresponds to JDK
 * 1.7).
 */
@SuppressWarnings("UnusedDeclaration")
class AvaticaJdbc41Factory implements AvaticaFactory {
  private final int major;
  private final int minor;

  /** Creates a JDBC factory. */
  public AvaticaJdbc41Factory() {
    this(4, 1);
  }

  /** Creates a JDBC factory with given major/minor version number. */
  protected AvaticaJdbc41Factory(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  public int getJdbcMajorVersion() {
    return major;
  }

  public int getJdbcMinorVersion() {
    return minor;
  }

  public AvaticaConnection newConnection(
      UnregisteredDriver driver,
      AvaticaFactory factory,
      String url,
      Properties info) {
    return new AvaticaJdbc41Connection(driver, factory, url, info);
  }

  public AvaticaDatabaseMetaData newDatabaseMetaData(
      AvaticaConnection connection) {
    return new AvaticaJdbc41DatabaseMetaData(connection);
  }

  public AvaticaStatement newStatement(AvaticaConnection connection,
      Meta.StatementHandle h, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) {
    return new AvaticaJdbc41Statement(connection, h, resultSetType,
        resultSetConcurrency, resultSetHoldability);
  }

  public AvaticaPreparedStatement newPreparedStatement(
      AvaticaConnection connection, Meta.StatementHandle h,
      Meta.Signature signature, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException {
    return new AvaticaJdbc41PreparedStatement(connection, h, signature,
        resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  public AvaticaResultSet newResultSet(AvaticaStatement statement,
      Meta.Signature signature, TimeZone timeZone, Iterable<Object> iterable) {
    final ResultSetMetaData metaData =
        newResultSetMetaData(statement, signature);
    return new AvaticaResultSet(statement, signature, metaData, timeZone,
        iterable);
  }

  public AvaticaResultSetMetaData newResultSetMetaData(
      AvaticaStatement statement, Meta.Signature signature) {
    return new AvaticaResultSetMetaData(statement, null, signature);
  }

  /** Implementation of Connection for JDBC 4.1. */
  private static class AvaticaJdbc41Connection extends AvaticaConnection {
    AvaticaJdbc41Connection(UnregisteredDriver driver,
        AvaticaFactory factory,
        String url,
        Properties info) {
      super(driver, factory, url, info);
    }
  }

  /** Implementation of Statement for JDBC 4.1. */
  private static class AvaticaJdbc41Statement extends AvaticaStatement {
    public AvaticaJdbc41Statement(AvaticaConnection connection,
        Meta.StatementHandle h, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) {
      super(connection, h, resultSetType, resultSetConcurrency,
          resultSetHoldability);
    }
  }

  /** Implementation of PreparedStatement for JDBC 4.1. */
  private static class AvaticaJdbc41PreparedStatement
      extends AvaticaPreparedStatement {
    AvaticaJdbc41PreparedStatement(AvaticaConnection connection,
        Meta.StatementHandle h, Meta.Signature signature, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
      super(connection, h, signature, resultSetType, resultSetConcurrency,
          resultSetHoldability);
    }

    public void setRowId(
        int parameterIndex,
        RowId x) throws SQLException {
      getParameter(parameterIndex).setRowId(slots, parameterIndex, x);
    }

    public void setNString(
        int parameterIndex, String value) throws SQLException {
      getParameter(parameterIndex).setNString(slots, parameterIndex, value);
    }

    public void setNCharacterStream(
        int parameterIndex,
        Reader value,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setNCharacterStream(slots, parameterIndex, value, length);
    }

    public void setNClob(
        int parameterIndex,
        NClob value) throws SQLException {
      getParameter(parameterIndex).setNClob(slots, parameterIndex, value);
    }

    public void setClob(
        int parameterIndex,
        Reader reader,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setClob(slots, parameterIndex, reader, length);
    }

    public void setBlob(
        int parameterIndex,
        InputStream inputStream,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setBlob(slots, parameterIndex, inputStream, length);
    }

    public void setNClob(
        int parameterIndex,
        Reader reader,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setNClob(slots, parameterIndex, reader, length);
    }

    public void setSQLXML(
        int parameterIndex, SQLXML xmlObject) throws SQLException {
      getParameter(parameterIndex).setSQLXML(slots, parameterIndex, xmlObject);
    }

    public void setAsciiStream(
        int parameterIndex,
        InputStream x,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setAsciiStream(slots, parameterIndex, x, length);
    }

    public void setBinaryStream(
        int parameterIndex,
        InputStream x,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setBinaryStream(slots, parameterIndex, x, length);
    }

    public void setCharacterStream(
        int parameterIndex,
        Reader reader,
        long length) throws SQLException {
      getParameter(parameterIndex)
          .setCharacterStream(slots, parameterIndex, reader, length);
    }

    public void setAsciiStream(
        int parameterIndex, InputStream x) throws SQLException {
      getParameter(parameterIndex).setAsciiStream(slots, parameterIndex, x);
    }

    public void setBinaryStream(
        int parameterIndex, InputStream x) throws SQLException {
      getParameter(parameterIndex).setBinaryStream(slots, parameterIndex, x);
    }

    public void setCharacterStream(
        int parameterIndex, Reader reader) throws SQLException {
      getParameter(parameterIndex)
          .setCharacterStream(slots, parameterIndex, reader);
    }

    public void setNCharacterStream(
        int parameterIndex, Reader value) throws SQLException {
      getParameter(parameterIndex)
          .setNCharacterStream(slots, parameterIndex, value);
    }

    public void setClob(
        int parameterIndex,
        Reader reader) throws SQLException {
      getParameter(parameterIndex).setClob(slots, parameterIndex, reader);
    }

    public void setBlob(
        int parameterIndex, InputStream inputStream) throws SQLException {
      getParameter(parameterIndex).setBlob(slots, parameterIndex, inputStream);
    }

    public void setNClob(
        int parameterIndex, Reader reader) throws SQLException {
      getParameter(parameterIndex).setNClob(slots, parameterIndex, reader);
    }
  }

  /** Implementation of DatabaseMetaData for JDBC 4.1. */
  private static class AvaticaJdbc41DatabaseMetaData
      extends AvaticaDatabaseMetaData {
    AvaticaJdbc41DatabaseMetaData(AvaticaConnection connection) {
      super(connection);
    }
  }
}

// End AvaticaJdbc41Factory.java
