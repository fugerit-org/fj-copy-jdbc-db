# JDBC Database Table Copy Tool

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](https://github.com/fugerit-org/fj-copy-jdbc-db/blob/master/CHANGELOG.md) 
[![Maven Central](https://img.shields.io/maven-central/v/org.fugerit.java/fj-copy-jdbc-db.svg)](https://mvnrepository.com/artifact/org.fugerit.java/fj-copy-jdbc-db)
[![license](https://img.shields.io/badge/License-Apache%20License%202.0-teal.svg)](https://opensource.org/licenses/Apache-2.0)
[![code of conduct](https://img.shields.io/badge/conduct-Contributor%20Covenant-purple.svg)](https://github.com/fugerit-org/fj-universe/blob/main/CODE_OF_CONDUCT.md)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_fj-copy-jdbc-db&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=fugerit-org_fj-copy-jdbc-db)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=fugerit-org_fj-copy-jdbc-db&metric=coverage)](https://sonarcloud.io/summary/new_code?id=fugerit-org_fj-copy-jdbc-db)

[![Java runtime version](https://img.shields.io/badge/run%20on-java%208+-%23113366.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://universe.fugerit.org/src/docs/versions/java8.html)
[![Java build version](https://img.shields.io/badge/build%20on-java%208+-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://universe.fugerit.org/src/docs/versions/java8.html)
[![Apache Maven](https://img.shields.io/badge/Apache%20Maven-3.9.0+-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)](https://universe.fugerit.org/src/docs/versions/maven3_9.html)

**fj-copy-jdbc-db** is a high-performance, robust, and highly dynamic Java utility designed to copy table data from a source database query to a destination database table using standard JDBC metadata APIs.

---

## Features

- **Metadata-Driven Execution**: Dynamically extracts column names, mappings, and database data types directly from the source query result set. No hardcoded schemas are required.
- **High-Performance Batching**: Executes inserts using optimized prepared statement batches to minimize database roundtrips.
- **Robust LOB & Type Mapping**: Gracefully translates standard primitive datatypes, nullable fields, and large objects (`CLOB`, `BLOB`) using native Java streams.
- **Configurable Control**: Supports custom execution modes, including configurable batch sizes and optional target table truncation before copy operations.

---

## Quickstart

### Build

```shell
mvn clean install
```

### Build & Run Tests

```shell
mvn clean install -P test
```

---

## API Usage

### Simple Copy

Copies all columns returned by a source query directly to the target table using default settings (batch size of 1000, no target truncation):

```java
import java.sql.Connection;
import org.fugerit.java.db.copy.CopyJDBC;

Connection srcConn = ...;  // Source DB Connection
Connection destConn = ...; // Destination DB Connection

String srcQuery = "SELECT id, name, created_at, details FROM my_source_table";
String destTable = "my_destination_table";

int rowsCopied = CopyJDBC.copy(srcConn, destConn, srcQuery, destTable);
log.info("Copied {} rows successfully!", rowsCopied);
```

### Advanced Copy with Configurations

Use the builder options inside `CopyConfig` to control target truncation and customize the size of your execution batches:

```java
import java.sql.Connection;
import org.fugerit.java.db.copy.CopyJDBC;
import org.fugerit.java.db.copy.CopyConfig;

Connection srcConn = ...;
Connection destConn = ...;

String srcQuery = "SELECT * FROM employees WHERE department = 'IT'";
String destTable = "it_employees_backup";

CopyConfig config = CopyConfig.builder()
    .batchSize(5000)        // Run prepared statements in batches of 5000
    .truncateDest(true)     // Delete all existing destination rows before copying
    .build();

int rowsCopied = CopyJDBC.copy(srcConn, destConn, srcQuery, destTable, config);
log.info("Copied {} rows into the backup table!", rowsCopied);
```

### Copy driven by Properties (CopyJDBCTool)

For integration with properties files or custom configuration managers, you can use the `CopyJDBCTool` utility. This allows you to define connection and copying configurations in a standard `.properties` file and execute the copy directly.

#### 1. Define the Configuration File (`copy-jdbc-db.properties`)

```properties
# Source connection settings (prefixed with 'src-')
src-db-cf-mode=DC
src-db-mode-dc-drv=org.h2.Driver
src-db-mode-dc-url=jdbc:h2:mem:src_db_params;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM './src/test/resources/config/init-src.sql';
src-db-mode-dc-usr=TESTDB
src-db-mode-dc-pwd=TESTDB

# Destination connection settings (prefixed with 'dest-')
dest-db-cf-mode=DC
dest-db-mode-dc-drv=org.h2.Driver
dest-db-mode-dc-url=jdbc:h2:mem:dest_db_params;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM './src/test/resources/config/init-dest.sql';
dest-db-mode-dc-usr=TESTDB
dest-db-mode-dc-pwd=TESTDB

# Transfer settings
src-query=SELECT * FROM TEST_TABLE
dest-table=TEST_TABLE
```

#### 2. Load Properties and Execute in Java

```java
import java.util.Properties;
import org.fugerit.java.core.util.PropsIO;
import org.fugerit.java.db.copy.CopyJDBCTool;

Properties params = PropsIO.loadFromClassLoaderSafe("config/copy-jdbc-db.properties");

long rowsCopied = CopyJDBCTool.copyFromParams(params);
log.info("Transferred {} rows successfully!", rowsCopied);
```



