package nl.topicus.jdbc.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.cloud.spanner.Type;

import nl.topicus.jdbc.CloudSpannerConnection;
import nl.topicus.jdbc.CloudSpannerDatabaseMetaData;
import nl.topicus.jdbc.resultset.CloudSpannerResultSet;
import nl.topicus.jdbc.test.category.UnitTest;

@Category(UnitTest.class)
public class CloudSpannerParameterMetaDataTest
{
	private static final String TYPE_NAME_OTHER = "Other";

	private CloudSpannerPreparedStatement createSelectStatement() throws SQLException
	{
		String sql = "SELECT COL1, COL2, COL3 FROM FOO WHERE COL1<? AND COL4=?";
		CloudSpannerConnection connection = mock(CloudSpannerConnection.class);
		return new CloudSpannerPreparedStatement(sql, connection, null);
	}

	private CloudSpannerPreparedStatement createInsertStatement() throws SQLException
	{
		String sql = "INSERT INTO FOO (COL1, COL2, COL3) VALUES (?, ?, ?)";
		CloudSpannerConnection connection = mock(CloudSpannerConnection.class);
		CloudSpannerDatabaseMetaData metadata = mock(CloudSpannerDatabaseMetaData.class);
		CloudSpannerResultSet columns = mock(CloudSpannerResultSet.class);
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.getColumns(null, null, "FOO", null)).thenReturn(columns);
		when(columns.next()).thenReturn(true, true, true, false);
		when(columns.getString("COLUMN_NAME")).thenReturn("COL1", "COL2", "COL3");
		when(columns.getInt("COLUMN_SIZE")).thenReturn(8, 50, 100);
		when(columns.getInt("DATA_TYPE")).thenReturn(Types.BIGINT, Types.NVARCHAR, Types.NVARCHAR);
		when(columns.getInt("NULLABLE")).thenReturn(ParameterMetaData.parameterNoNulls,
				ParameterMetaData.parameterNoNulls, ParameterMetaData.parameterNullable);

		return new CloudSpannerPreparedStatement(sql, connection, null);
	}

	@Test
	public void testGetParameterCount() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(2, metadata.getParameterCount());
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(3, metadata.getParameterCount());
		}
	}

	@Test
	public void testIsNullable() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(ParameterMetaData.parameterNullableUnknown, metadata.isNullable(1));
			assertEquals(ParameterMetaData.parameterNullableUnknown, metadata.isNullable(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(ParameterMetaData.parameterNoNulls, metadata.isNullable(1));
			assertEquals(ParameterMetaData.parameterNoNulls, metadata.isNullable(2));
			assertEquals(ParameterMetaData.parameterNullable, metadata.isNullable(3));
		}
	}

	@Test
	public void testIsSigned() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(false, metadata.isSigned(1));
			assertEquals(false, metadata.isSigned(2));
			ps.setLong(1, 1000l);
			assertEquals(true, metadata.isSigned(1));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(true, metadata.isSigned(1));
			assertEquals(false, metadata.isSigned(2));
			assertEquals(false, metadata.isSigned(3));
			ps.setLong(1, 1000l);
			assertEquals(true, metadata.isSigned(1));
		}
	}

	@Test
	public void testGetPrecision() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(0, metadata.getPrecision(1));
			assertEquals(0, metadata.getPrecision(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(8, metadata.getPrecision(1));
			assertEquals(50, metadata.getPrecision(2));
			assertEquals(100, metadata.getPrecision(3));
		}
	}

	@Test
	public void testGetScale() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(0, metadata.getScale(1));
			assertEquals(0, metadata.getScale(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(0, metadata.getScale(1));
			assertEquals(0, metadata.getScale(2));
			assertEquals(0, metadata.getScale(3));
		}
	}

	@Test
	public void testGetParameterType() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(Types.OTHER, metadata.getParameterType(1));
			assertEquals(Types.OTHER, metadata.getParameterType(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(Types.BIGINT, metadata.getParameterType(1));
			assertEquals(Types.NVARCHAR, metadata.getParameterType(2));
			assertEquals(Types.NVARCHAR, metadata.getParameterType(3));
		}
	}

	@Test
	public void testGetParameterTypeName() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(TYPE_NAME_OTHER, metadata.getParameterTypeName(1));
			assertEquals(TYPE_NAME_OTHER, metadata.getParameterTypeName(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(Type.int64().getCode().name(), metadata.getParameterTypeName(1));
			assertEquals(Type.string().getCode().name(), metadata.getParameterTypeName(2));
			assertEquals(Type.string().getCode().name(), metadata.getParameterTypeName(3));
		}
	}

	@Test
	public void testGetParameterClassName() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertNull(metadata.getParameterClassName(1));
			assertNull(metadata.getParameterClassName(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(Long.class.getName(), metadata.getParameterClassName(1));
			assertEquals(String.class.getName(), metadata.getParameterClassName(2));
			assertEquals(String.class.getName(), metadata.getParameterClassName(3));
		}
	}

	@Test
	public void testGetParameterMode() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(1));
			assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(2));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			ParameterMetaData metadata = ps.getParameterMetaData();
			assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(1));
			assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(2));
			assertEquals(ParameterMetaData.parameterModeIn, metadata.getParameterMode(3));
		}
	}

	@Test
	public void testToString() throws SQLException
	{
		try (CloudSpannerPreparedStatement ps = createSelectStatement())
		{
			String str = ps.getParameterMetaData().toString();
			assertNotNull(str);
			assertNotEquals("", str);
			assertEquals(-1, str.indexOf("Error while fetching parameter metadata"));
		}
		try (CloudSpannerPreparedStatement ps = createInsertStatement())
		{
			String str = ps.getParameterMetaData().toString();
			assertNotNull(str);
			assertNotEquals("", str);
			assertEquals(-1, str.indexOf("Error while fetching parameter metadata"));
		}
	}

}
