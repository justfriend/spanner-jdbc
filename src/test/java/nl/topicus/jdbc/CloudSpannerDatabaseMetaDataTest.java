package nl.topicus.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import nl.topicus.jdbc.test.category.UnitTest;

@Category(UnitTest.class)
public class CloudSpannerDatabaseMetaDataTest
{
	private static final String URL = "jdbc:cloudspanner://localhost;Project=adroit-hall-xxx;Instance=test-instance;Database=testdb;PvtKeyPath=C:\\Users\\MyUserName\\Documents\\CloudSpannerKeys\\cloudspanner3.json;SimulateProductName=PostgreSQL";

	private final CloudSpannerDatabaseMetaData testSubject;

	public CloudSpannerDatabaseMetaDataTest() throws SQLException
	{
		testSubject = createTestSubject(URL);
	}

	private CloudSpannerDatabaseMetaData createTestSubject(String url) throws SQLException
	{
		CloudSpannerConnection connection = MockCloudSpannerConnection.create(url);
		return new CloudSpannerDatabaseMetaData(connection);
	}

	@Test
	public void testAllProceduresAreCallable() throws SQLException
	{
		assertTrue(testSubject.allProceduresAreCallable());
	}

	@Test
	public void testAllTablesAreSelectable() throws SQLException
	{
		assertTrue(testSubject.allTablesAreSelectable());
	}

	@Test
	public void testGetURL() throws SQLException
	{
		assertEquals(URL, testSubject.getURL());
	}

	@Test
	public void testGetUserName() throws SQLException
	{
		assertNull(testSubject.getUserName());
	}

	@Test
	public void testIsReadOnly() throws SQLException
	{
		assertEquals(false, testSubject.isReadOnly());
	}

	@Test
	public void testNullsAreSortedHigh() throws SQLException
	{
		assertEquals(false, testSubject.nullsAreSortedHigh());
	}

	@Test
	public void testNullsAreSortedLow() throws SQLException
	{
		assertTrue(testSubject.nullsAreSortedLow());
	}

	@Test
	public void testNullsAreSortedAtStart() throws SQLException
	{
		assertTrue(testSubject.nullsAreSortedAtStart());
	}

	@Test
	public void testNullsAreSortedAtEnd() throws SQLException
	{
		assertEquals(false, testSubject.nullsAreSortedAtEnd());
	}

	@Test
	public void testGetDatabaseProductName() throws SQLException
	{
		assertEquals("PostgreSQL", testSubject.getDatabaseProductName());
	}

	@Test
	public void testGetDatabaseProductVersion() throws SQLException
	{
		assertEquals("1.0", testSubject.getDatabaseProductVersion());
	}

	@Test
	public void testGetDriverName() throws SQLException
	{
		assertEquals(CloudSpannerDriver.class.getName(), testSubject.getDriverName());
	}

	@Test
	public void testGetDriverVersion() throws SQLException
	{
		assertEquals(CloudSpannerDriver.MAJOR_VERSION + "." + CloudSpannerDriver.MINOR_VERSION,
				testSubject.getDriverVersion());
	}

	@Test
	public void testGetDriverMajorVersion()
	{
		assertEquals(CloudSpannerDriver.MAJOR_VERSION, testSubject.getDriverMajorVersion());
	}

	@Test
	public void testGetDriverMinorVersion()
	{
		assertEquals(CloudSpannerDriver.MINOR_VERSION, testSubject.getDriverMinorVersion());
	}

	@Test
	public void testUsesLocalFiles() throws SQLException
	{
		assertEquals(false, testSubject.usesLocalFiles());
	}

	@Test
	public void testUsesLocalFilePerTable() throws SQLException
	{
		assertEquals(false, testSubject.usesLocalFilePerTable());
	}

	@Test
	public void testSupportsMixedCaseIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.supportsMixedCaseIdentifiers());
	}

	@Test
	public void testStoresUpperCaseIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.storesUpperCaseIdentifiers());
	}

	@Test
	public void testStoresLowerCaseIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.storesLowerCaseIdentifiers());
	}

	@Test
	public void testStoresMixedCaseIdentifiers() throws SQLException
	{
		assertTrue(testSubject.storesMixedCaseIdentifiers());
	}

	@Test
	public void testSupportsMixedCaseQuotedIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.supportsMixedCaseQuotedIdentifiers());
	}

	@Test
	public void testStoresUpperCaseQuotedIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.storesUpperCaseQuotedIdentifiers());
	}

	@Test
	public void testStoresLowerCaseQuotedIdentifiers() throws SQLException
	{
		assertEquals(false, testSubject.storesLowerCaseQuotedIdentifiers());
	}

	@Test
	public void testStoresMixedCaseQuotedIdentifiers() throws SQLException
	{
		assertTrue(testSubject.storesMixedCaseQuotedIdentifiers());
	}

	@Test
	public void testGetIdentifierQuoteString() throws SQLException
	{
		assertEquals("`", testSubject.getIdentifierQuoteString());
	}

	@Test
	public void testGetSQLKeywords() throws SQLException
	{
		assertEquals("INTERLEAVE, PARENT", testSubject.getSQLKeywords());
	}

	@Test
	public void testGetNumericFunctions() throws SQLException
	{
		assertEquals("", testSubject.getNumericFunctions());
	}

	@Test
	public void testGetStringFunctions() throws SQLException
	{
		assertEquals("", testSubject.getStringFunctions());
	}

	@Test
	public void testGetSystemFunctions() throws SQLException
	{
		assertEquals("", testSubject.getSystemFunctions());
	}

	@Test
	public void testGetTimeDateFunctions() throws SQLException
	{
		assertEquals("", testSubject.getTimeDateFunctions());
	}

	@Test
	public void testGetSearchStringEscape() throws SQLException
	{
		assertEquals("\\", testSubject.getSearchStringEscape());
	}

	@Test
	public void testGetExtraNameCharacters() throws SQLException
	{
		assertEquals("", testSubject.getExtraNameCharacters());
	}

	@Test
	public void testSupportsAlterTableWithAddColumn() throws SQLException
	{
		assertTrue(testSubject.supportsAlterTableWithAddColumn());
	}

	@Test
	public void testSupportsAlterTableWithDropColumn() throws SQLException
	{
		assertTrue(testSubject.supportsAlterTableWithDropColumn());
	}

	@Test
	public void testSupportsColumnAliasing() throws SQLException
	{
		assertTrue(testSubject.supportsColumnAliasing());
	}

	@Test
	public void testNullPlusNonNullIsNull() throws SQLException
	{
		assertTrue(testSubject.nullPlusNonNullIsNull());
	}

	@Test
	public void testSupportsConvert() throws SQLException
	{
		assertEquals(false, testSubject.supportsConvert());
	}

	@Test
	public void testSupportsConvertWithParameters() throws SQLException
	{
		assertEquals(false, testSubject.supportsConvert(Types.BOOLEAN, Types.BIT));
	}

	@Test
	public void testSupportsTableCorrelationNames() throws SQLException
	{
		assertTrue(testSubject.supportsTableCorrelationNames());
	}

	@Test
	public void testSupportsDifferentTableCorrelationNames() throws SQLException
	{
		assertEquals(false, testSubject.supportsDifferentTableCorrelationNames());
	}

	@Test
	public void testSupportsExpressionsInOrderBy() throws SQLException
	{
		assertTrue(testSubject.supportsExpressionsInOrderBy());
	}

	@Test
	public void testSupportsOrderByUnrelated() throws SQLException
	{
		assertTrue(testSubject.supportsOrderByUnrelated());
	}

	@Test
	public void testSupportsGroupBy() throws SQLException
	{
		assertTrue(testSubject.supportsGroupBy());
	}

	@Test
	public void testUnwrap() throws SQLException
	{
		assertTrue(testSubject.isWrapperFor(DatabaseMetaData.class));
		assertEquals(testSubject, testSubject.unwrap(DatabaseMetaData.class));
	}

	@Test
	public void testSupportsGroupByUnrelated() throws SQLException
	{
		assertTrue(testSubject.supportsGroupByUnrelated());
	}

	@Test
	public void testSupportsGroupByBeyondSelect() throws SQLException
	{
		assertTrue(testSubject.supportsGroupByBeyondSelect());
	}

	@Test
	public void testSupportsLikeEscapeClause() throws SQLException
	{
		assertTrue(testSubject.supportsLikeEscapeClause());
	}

	@Test
	public void testSupportsMultipleResultSets() throws SQLException
	{
		assertEquals(false, testSubject.supportsMultipleResultSets());
	}

	@Test
	public void testSupportsMultipleTransactions() throws SQLException
	{
		assertTrue(testSubject.supportsMultipleTransactions());
	}

	@Test
	public void testSupportsNonNullableColumns() throws SQLException
	{
		assertTrue(testSubject.supportsNonNullableColumns());
	}

	@Test
	public void testSupportsMinimumSQLGrammar() throws SQLException
	{
		assertEquals(false, testSubject.supportsMinimumSQLGrammar());
	}

	@Test
	public void testSupportsCoreSQLGrammar() throws SQLException
	{
		assertEquals(false, testSubject.supportsCoreSQLGrammar());
	}

	@Test
	public void testSupportsExtendedSQLGrammar() throws SQLException
	{
		assertEquals(false, testSubject.supportsExtendedSQLGrammar());
	}

	@Test
	public void testSupportsANSI92EntryLevelSQL() throws SQLException
	{
		assertEquals(false, testSubject.supportsANSI92EntryLevelSQL());
	}

	@Test
	public void testSupportsANSI92IntermediateSQL() throws SQLException
	{
		assertEquals(false, testSubject.supportsANSI92IntermediateSQL());
	}

	@Test
	public void testSupportsANSI92FullSQL() throws SQLException
	{
		assertEquals(false, testSubject.supportsANSI92FullSQL());
	}

	@Test
	public void testSupportsIntegrityEnhancementFacility() throws SQLException
	{
		assertEquals(false, testSubject.supportsIntegrityEnhancementFacility());
	}

	@Test
	public void testSupportsOuterJoins() throws SQLException
	{
		assertTrue(testSubject.supportsOuterJoins());
	}

	@Test
	public void testSupportsFullOuterJoins() throws SQLException
	{
		assertTrue(testSubject.supportsFullOuterJoins());
	}

	@Test
	public void testSupportsLimitedOuterJoins() throws SQLException
	{
		assertTrue(testSubject.supportsLimitedOuterJoins());
	}

	@Test
	public void testGetSchemaTerm() throws SQLException
	{
		assertNull(testSubject.getSchemaTerm());
	}

	@Test
	public void testGetProcedureTerm() throws SQLException
	{
		assertNull(testSubject.getProcedureTerm());
	}

	@Test
	public void testGetCatalogTerm() throws SQLException
	{
		assertNull(testSubject.getCatalogTerm());
	}

	@Test
	public void testIsCatalogAtStart() throws SQLException
	{
		assertEquals(false, testSubject.isCatalogAtStart());
	}

	@Test
	public void testGetCatalogSeparator() throws SQLException
	{
		assertNull(testSubject.getCatalogSeparator());
	}

	@Test
	public void testSupportsSchemasInDataManipulation() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInDataManipulation());
	}

	@Test
	public void testSupportsSchemasInProcedureCalls() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInProcedureCalls());
	}

	@Test
	public void testSupportsSchemasInTableDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInTableDefinitions());
	}

	@Test
	public void testSupportsSchemasInIndexDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInIndexDefinitions());
	}

	@Test
	public void testSupportsSchemasInPrivilegeDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInPrivilegeDefinitions());
	}

	@Test
	public void testSupportsCatalogsInDataManipulation() throws SQLException
	{
		assertEquals(false, testSubject.supportsSchemasInProcedureCalls());
	}

	@Test
	public void testSupportsCatalogsInProcedureCalls() throws SQLException
	{
		assertEquals(false, testSubject.supportsCatalogsInProcedureCalls());
	}

	@Test
	public void testSupportsCatalogsInTableDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsCatalogsInTableDefinitions());
	}

	@Test
	public void testSupportsCatalogsInIndexDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsCatalogsInIndexDefinitions());
	}

	@Test
	public void testSupportsCatalogsInPrivilegeDefinitions() throws SQLException
	{
		assertEquals(false, testSubject.supportsCatalogsInPrivilegeDefinitions());
	}

	@Test
	public void testSupportsPositionedDelete() throws SQLException
	{
		assertEquals(false, testSubject.supportsPositionedDelete());
	}

	@Test
	public void testSupportsPositionedUpdate() throws SQLException
	{
		assertEquals(false, testSubject.supportsPositionedUpdate());
	}

	@Test
	public void testSupportsSelectForUpdate() throws SQLException
	{
		assertEquals(false, testSubject.supportsSelectForUpdate());
	}

	@Test
	public void testSupportsStoredProcedures() throws SQLException
	{
		assertEquals(false, testSubject.supportsStoredProcedures());
	}
}