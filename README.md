### Status
[![Build Status](https://travis-ci.org/olavloite/spanner-jdbc.svg?branch=master)](https://travis-ci.org/olavloite/spanner-jdbc)
[![Quality Gate](https://sonarcloud.io/api/badges/gate?key=nl.topicus%3Aspanner-jdbc&metric=coverage)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Coverage](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=coverage)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Reliability Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=reliability_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Security Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=security_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)
[![Maintainability Rating](https://sonarcloud.io/api/badges/measure?key=nl.topicus%3Aspanner-jdbc&metric=sqale_rating)](https://sonarcloud.io/dashboard/index/nl.topicus%3Aspanner-jdbc)


# spanner-jdbc
JDBC Driver for Google Cloud Spanner

An open source JDBC Driver for Google Cloud Spanner, the horizontally scalable, globally consistent, relational database service from Google. The JDBC Driver that is supplied by Google is quite limited, as it does not allow any inserts, updates or deletes, nor does it allow DDL-statements.

This driver supports a number of unsupported features of the official JDBC driver:
* DML-statements (INSERT, UPDATE, DELETE)
* DDL-statements (CREATE TABLE, ALTER TABLE, CREATE INDEX, DROP TABLE, ...)
* Transactions (both read/write and read-only)

The driver ofcourse also supports normal SELECT-statements, including parameters. Example usage and tutorials can be found on http://www.googlecloudspanner.com/.

Releases are available on Maven Central. Current release is version 0.18.

Include the following if you want the thick jar version that includes all (shaded) dependencies. This is the recommended version unless you know that the transitive dependencies of the small jar will not conflict with the rest of your project.

<div class="highlight highlight-text-xml"><pre>
&lt;<span class="pl-ent">dependency</span>&gt;
 	&lt;<span class="pl-ent">groupId</span>&gt;nl.topicus&lt;/<span class="pl-ent">groupId</span>&gt;
    	&lt;<span class="pl-ent">artifactId</span>&gt;spanner-jdbc&lt;/<span class="pl-ent">artifactId</span>&gt;
    	&lt;<span class="pl-ent">version</span>&gt;0.18&lt;/<span class="pl-ent">version</span>&gt;
    	&lt;<span class="pl-ent">classifier</span>&gt;shaded&lt;/<span class="pl-ent">classifier</span>&gt;
&lt;/<span class="pl-ent">dependency</span>&gt;
</pre></div>

Include this if you want the light-weight jar.

<div class="highlight highlight-text-xml"><pre>
&lt;<span class="pl-ent">dependency</span>&gt;
 	&lt;<span class="pl-ent">groupId</span>&gt;nl.topicus&lt;/<span class="pl-ent">groupId</span>&gt;
    	&lt;<span class="pl-ent">artifactId</span>&gt;spanner-jdbc&lt;/<span class="pl-ent">artifactId</span>&gt;
    	&lt;<span class="pl-ent">version</span>&gt;0.18&lt;/<span class="pl-ent">version</span>&gt;
&lt;/<span class="pl-ent">dependency</span>&gt;
</pre></div>

You can also use the 'thick-jar'-version with third-party tools such as SQuirreL, SQL Workbench, DbVisualizer or Safe FME. This jar contains all the necessary (shaded) dependencies for the driver. Have a look at this site for more information on how to use the driver with different tools and frameworks: http://www.googlecloudspanner.com/

The thick-jar version can also be found here: https://github.com/olavloite/spanner-jdbc/releases


This driver does allow DML operations, although also limited because of the underlying limitations of Google Cloud Spanner. All data manipulation operations are limited to operations that operate on one record. This means that:
* Inserts can only insert one row at a time
* Updates and deletes must include a where-clause specifying the primary key (and nothing else). E.g. 'WHERE ID=?'.
* As of version 0.16 and newer the driver also supports bulk INSERT/UPDATE/DELETE-statements. Please note that the underlying limitations of Google Cloud Spanner transactions still apply: https://cloud.google.com/spanner/quotas. This means a maximum of 20,000 mutations and 100MB of data in one transaction. You can get the driver to automatically bypass these quotas by setting the connection property AllowExtendedMode=true (see the Wiki-pages of this driver: https://github.com/olavloite/spanner-jdbc/wiki/URL-and-Connection-Properties).

Example of bulk INSERT:  
INSERT INTO TABLE1  
(COL1, COL2, COL3)  
SELECT SOMECOL1, SOMECOL2, SOMECOL3  
FROM TABLE2  
WHERE SOMECOL1>? AND SOMECOL3 LIKE ?  

Example of bulk INSERT-OR-UPDATE:  
INSERT INTO TABLE1  
(COL1, COL2, COL3)  
SELECT COL1, COL2+COL4, COL3*2  
FROM TABLE1  
WHERE COL4=?  
ON DUPLICATE KEY UPDATE  

The above UPDATE example is equal to: UPDATE TABLE1 SET COL2=COL2+COL4 AND COL3=COL3*2 WHERE COL4=? (assuming that COL1 is the primary key of the table).

Example of bulk UPDATE:  
UPDATE TABLE1 SET  
COL1=COL1*1.1,  
COL2=COL3+COL4  
WHERE COL5<1000  

The driver is designed to work with applications using JPA/Hibernate. See https://github.com/olavloite/spanner-hibernate for a Hibernate Dialect implementation for Google Cloud Spanner that works together with this JDBC Driver.

A simple example project using Spring Boot + JPA + Hibernate + this JDBC Driver can be found here: https://github.com/olavloite/spanner-jpa-example

Example usage:

****
spring.datasource.driver-class-name=nl.topicus.jdbc.CloudSpannerDriver

spring.datasource.url=jdbc:cloudspanner://localhost;Project=projectId;Instance=instanceId;Database=databaseName;SimulateProductName=PostgreSQL;PvtKeyPath=key_file;AllowExtendedMode=false

****


The last two properties (SimulateProductName and PvtKeyPath) are optional.
All properties can also be supplied in a Properties object instead of in the URL.

You either need to
* Create an environment variable GOOGLE_APPLICATION_CREDENTIALS that points to a credentials file for a Google Cloud Spanner project.
* OR Supply the parameter PvtKeyPath that points to a file containing the credentials to use.

The server name (in the example above: localhost) is ignored by the driver, but as it is a mandatory part of a JDBC URL it needs to be specified.
The property 'SimulateProductName' indicates what database name should be returned by the method DatabaseMetaData.getDatabaseProductName(). This can be used in combination with for example Spring Batch. Spring Batch automatically generates a schema for batch jobs, parameters etc., but does so only if it recognizes the underlying database. Supplying PostgreSQL as a value for this parameter, ensures the correct schema generation.


Credits
This application uses Open Source components. You can find the source code of their open source projects along with license information below.

A special thanks to Tobias for his great JSqlParser library.
Project: JSqlParser https://github.com/JSQLParser/JSqlParser 
Copyright (C) 2004 - 2017 JSQLParser Tobias
