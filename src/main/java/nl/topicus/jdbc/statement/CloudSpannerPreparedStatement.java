package nl.topicus.jdbc.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ReadContext;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.update.Update;
import nl.topicus.jdbc.CloudSpannerConnection;
import nl.topicus.jdbc.CloudSpannerDriver;
import nl.topicus.jdbc.MetaDataStore.TableKeyMetaData;
import nl.topicus.jdbc.resultset.CloudSpannerResultSet;
import nl.topicus.jdbc.statement.AbstractTablePartWorker.DMLOperation;

/**
 * 
 * @author loite
 *
 */
public class CloudSpannerPreparedStatement extends AbstractCloudSpannerPreparedStatement
{
	private static final String INVALID_WHERE_CLAUSE_DELETE_MESSAGE = "The DELETE statement does not contain a valid WHERE clause. DELETE statements must contain a WHERE clause specifying the value of the primary key of the record(s) to be deleted in the form 'ID=value' or 'ID1=value1 AND ID2=value2'";

	private static final String INVALID_WHERE_CLAUSE_UPDATE_MESSAGE = "The UPDATE statement does not contain a valid WHERE clause. UPDATE statements must contain a WHERE clause specifying the value of the primary key of the record(s) to be deleted in the form 'ID=value' or 'ID1=value1 AND ID2=value2'";

	static final String PARSE_ERROR = "Error while parsing sql statement ";

	private String sql;

	/**
	 * Flag indicating that an INSERT INTO ... ON DUPLICATE KEY UPDATE statement
	 * should be forced to do only an update
	 */
	private boolean forceUpdate;

	private List<Mutations> batchMutations = new ArrayList<>();

	public CloudSpannerPreparedStatement(String sql, CloudSpannerConnection connection, DatabaseClient dbClient)
	{
		super(connection, dbClient);
		this.sql = sql;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		throw new SQLException("The executeQuery(String sql)-method may not be called on a PreparedStatement");
	}

	@Override
	public ResultSet executeQuery() throws SQLException
	{
		Statement statement;
		try
		{
			statement = CCJSqlParserUtil.parse(sanitizeSQL(sql));
		}
		catch (JSQLParserException | TokenMgrError e)
		{
			throw new SQLException(PARSE_ERROR + sql + ": " + e.getLocalizedMessage(), e);
		}
		if (statement instanceof Select)
		{
			com.google.cloud.spanner.Statement.Builder builder = createSelectBuilder(statement);
			try (ReadContext context = getReadContext())
			{
				com.google.cloud.spanner.ResultSet rs = context.executeQuery(builder.build());
				return new CloudSpannerResultSet(this, rs);
			}
		}
		throw new SQLException("SQL statement not suitable for executeQuery. Expected SELECT-statement.");
	}

	private com.google.cloud.spanner.Statement.Builder createSelectBuilder(Statement statement)
	{
		String namedSql = convertPositionalParametersToNamedParameters(sql);
		com.google.cloud.spanner.Statement.Builder builder = com.google.cloud.spanner.Statement.newBuilder(namedSql);
		setSelectParameters(((Select) statement).getSelectBody(), builder);

		return builder;
	}

	private String convertPositionalParametersToNamedParameters(String sql)
	{
		boolean inString = false;
		StringBuilder res = new StringBuilder(sql);
		int i = 0;
		int parIndex = 1;
		while (i < res.length())
		{
			char c = res.charAt(i);
			if (c == '\'')
			{
				inString = !inString;
			}
			else if (c == '?' && !inString)
			{
				res.replace(i, i + 1, "@p" + parIndex);
				parIndex++;
			}
			i++;
		}

		return res.toString();
	}

	private void setSelectParameters(SelectBody body, com.google.cloud.spanner.Statement.Builder builder)
	{
		body.accept(new SelectVisitorAdapter()
		{
			@Override
			public void visit(PlainSelect plainSelect)
			{
				setWhereParameters(plainSelect.getWhere(), builder);
				if (plainSelect.getLimit() != null)
				{
					setWhereParameters(plainSelect.getLimit().getRowCount(), builder);
				}
				if (plainSelect.getOffset() != null && plainSelect.getOffset().isOffsetJdbcParameter())
				{
					ValueBinderExpressionVisitorAdapter<com.google.cloud.spanner.Statement.Builder> binder = new ValueBinderExpressionVisitorAdapter<>(
							getParameterStore(), builder.bind("p" + getParameterStore().getHighestIndex()), null);
					binder.setValue(getParameterStore().getParameter(getParameterStore().getHighestIndex()));
					getParameterStore().setType(getParameterStore().getHighestIndex(), Types.BIGINT);
				}
			}
		});
	}

	private void setWhereParameters(Expression where, com.google.cloud.spanner.Statement.Builder builder)
	{
		if (where != null)
		{
			where.accept(new ExpressionVisitorAdapter()
			{

				@Override
				public void visit(JdbcParameter parameter)
				{
					parameter.accept(new ValueBinderExpressionVisitorAdapter<>(getParameterStore(),
							builder.bind("p" + parameter.getIndex()), null));
				}

				@Override
				public void visit(SubSelect subSelect)
				{
					setSelectParameters(subSelect.getSelectBody(), builder);
				}

			});
		}
	}

	@Override
	public void addBatch() throws SQLException
	{
		if (getConnection().getAutoCommit())
		{
			throw new SQLFeatureNotSupportedException(
					"Batching of statements is only allowed when not running in autocommit mode");
		}
		if (isDDLStatement(sql))
		{
			throw new SQLFeatureNotSupportedException("DDL statements may not be batched");
		}
		if (isSelectStatement(sql))
		{
			throw new SQLFeatureNotSupportedException("SELECT statements may not be batched");
		}
		Mutations mutations = createMutations(sql);
		batchMutations.add(mutations);
		getParameterStore().clearParameters();
	}

	@Override
	public void clearBatch() throws SQLException
	{
		batchMutations.clear();
		getParameterStore().clearParameters();
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		int[] res = new int[batchMutations.size()];
		int index = 0;
		for (Mutations mutation : batchMutations)
		{
			res[index] = (int) writeMutations(mutation);
			index++;
		}
		batchMutations.clear();
		getParameterStore().clearParameters();
		return res;
	}

	@Override
	public int executeUpdate() throws SQLException
	{
		if (isDDLStatement(sql))
		{
			String ddl = formatDDLStatement(sql);
			return executeDDL(ddl);
		}
		Mutations mutations = createMutations(sql);
		return (int) writeMutations(mutations);
	}

	private Mutations createMutations(String sql) throws SQLException
	{
		return createMutations(sql, false);
	}

	private Mutations createMutations(String sql, boolean forceUpdate) throws SQLException
	{
		try
		{
			if (getConnection().isReadOnly())
			{
				throw new SQLException("The connection is in read-only mode. Mutations are not allowed.");
			}
			if (isDDLStatement(sql))
			{
				throw new SQLException("Cannot create mutation for DDL statement. Expected INSERT, UPDATE or DELETE");
			}
			Statement statement = CCJSqlParserUtil.parse(sanitizeSQL(sql));
			if (statement instanceof Insert)
			{
				Insert insertStatement = (Insert) statement;
				if (insertStatement.getSelect() == null)
					return new Mutations(createInsertMutation(insertStatement));
				return new Mutations(createInsertWithSelectStatement(insertStatement, forceUpdate));
			}
			else if (statement instanceof Update)
			{
				Update updateStatement = (Update) statement;
				if (updateStatement.getSelect() != null)
					throw new SQLException(
							"UPDATE statement using SELECT is not supported. Try to re-write the statement as an INSERT INTO ... SELECT A, B, C FROM TABLE WHERE ... ON DUPLICATE KEY UPDATE");
				if (updateStatement.getTables().size() > 1)
					throw new SQLException(
							"UPDATE statement using multiple tables is not supported. Try to re-write the statement as an INSERT INTO ... SELECT A, B, C FROM TABLE WHERE ... ON DUPLICATE KEY UPDATE");

				if (isSingleRowWhereClause(
						getConnection().getTable(unquoteIdentifier(updateStatement.getTables().get(0).getName())),
						updateStatement.getWhere()))
					return new Mutations(createUpdateMutation(updateStatement));
				// Translate into an 'INSERT ... SELECT ... ON DUPLICATE KEY
				// UPDATE'-statement
				String insertSQL = createInsertSelectOnDuplicateKeyUpdateStatement(updateStatement);
				return createMutations(insertSQL, true);
			}
			else if (statement instanceof Delete)
			{
				Delete deleteStatement = (Delete) statement;
				if (deleteStatement.getWhere() == null || isSingleRowWhereClause(
						getConnection().getTable(unquoteIdentifier(deleteStatement.getTable().getName())),
						deleteStatement.getWhere()))
					return new Mutations(createDeleteMutation(deleteStatement));
				return new Mutations(createDeleteWorker(deleteStatement));
			}
			else
			{
				throw new SQLFeatureNotSupportedException(
						"Unrecognized or unsupported SQL-statment: Expected one of INSERT, UPDATE or DELETE. Please note that batching of prepared statements is not supported for SELECT-statements.");
			}
		}
		catch (JSQLParserException | IllegalArgumentException | TokenMgrError e)
		{
			throw new SQLException(PARSE_ERROR + sql + ": " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Does some formatting to DDL statements that might have been generated by
	 * standard SQL generators to make it compatible with Google Cloud Spanner.
	 * 
	 * @param sql
	 *            The sql to format
	 * @return The formatted DDL statement.
	 */
	private String formatDDLStatement(String sql)
	{
		String res = sql.trim().toUpperCase();
		String[] parts = res.split("\\s+");
		if (parts.length >= 2)
		{
			String sqlWithSingleSpaces = String.join(" ", parts);
			if (sqlWithSingleSpaces.startsWith("CREATE TABLE"))
			{
				int primaryKeyIndex = res.indexOf(", PRIMARY KEY (");
				if (primaryKeyIndex > -1)
				{
					int endPrimaryKeyIndex = res.indexOf(')', primaryKeyIndex);
					String primaryKeySpec = res.substring(primaryKeyIndex + 2, endPrimaryKeyIndex + 1);
					res = res.replace(", " + primaryKeySpec, "");
					res = res + " " + primaryKeySpec;
				}
			}
		}

		return res;
	}

	private Mutation createInsertMutation(Insert insert) throws SQLException
	{
		ItemsList items = insert.getItemsList();
		if (!(items instanceof ExpressionList))
		{
			throw new SQLException("Insert statement must specify a list of values");
		}
		if (insert.getColumns() == null || insert.getColumns().isEmpty())
		{
			throw new SQLException("Insert statement must specify a list of column names");
		}
		List<Expression> expressions = ((ExpressionList) items).getExpressions();
		String table = unquoteIdentifier(insert.getTable().getFullyQualifiedName());
		getParameterStore().setTable(table);
		WriteBuilder builder;
		if (insert.isUseDuplicate())
		{
			/**
			 * Do an insert-or-update. BUT: Cloud Spanner does not support
			 * supplying different values for the insert and update statements,
			 * meaning that only the values specified in the INSERT part of the
			 * statement will be considered. Anything specified in the 'ON
			 * DUPLICATE KEY UPDATE ...' statement will be ignored.
			 */
			if (this.forceUpdate)
				builder = Mutation.newUpdateBuilder(table);
			else
				builder = Mutation.newInsertOrUpdateBuilder(table);
		}
		else
		{
			/**
			 * Just do an insert and throw an error if a row with the specified
			 * key alread exists.
			 */
			builder = Mutation.newInsertBuilder(table);
		}
		int index = 0;
		for (Column col : insert.getColumns())
		{
			String columnName = unquoteIdentifier(col.getFullyQualifiedName());
			expressions.get(index).accept(new ValueBinderExpressionVisitorAdapter<>(getParameterStore(),
					builder.set(columnName), columnName));
			index++;
		}
		return builder.build();
	}

	private Mutation createUpdateMutation(Update update) throws SQLException
	{
		if (update.getTables().isEmpty())
			throw new SQLException("No table found in update statement");
		if (update.getTables().size() > 1)
			throw new SQLException("Update statements for multiple tables at once are not supported");
		String table = unquoteIdentifier(update.getTables().get(0).getFullyQualifiedName());
		getParameterStore().setTable(table);
		List<Expression> expressions = update.getExpressions();
		WriteBuilder builder = Mutation.newUpdateBuilder(table);
		int index = 0;
		for (Column col : update.getColumns())
		{
			String columnName = unquoteIdentifier(col.getFullyQualifiedName());
			expressions.get(index).accept(new ValueBinderExpressionVisitorAdapter<>(getParameterStore(),
					builder.set(columnName), columnName));
			index++;
		}
		visitUpdateWhereClause(update.getWhere(), builder);

		return builder.build();
	}

	private Mutation createDeleteMutation(Delete delete) throws SQLException
	{
		String table = unquoteIdentifier(delete.getTable().getFullyQualifiedName());
		getParameterStore().setTable(table);
		Expression where = delete.getWhere();
		if (where == null)
		{
			// Delete all
			return Mutation.delete(table, KeySet.all());
		}
		else
		{
			// Delete one
			DeleteKeyBuilder keyBuilder = new DeleteKeyBuilder(getConnection().getTable(table));
			visitDeleteWhereClause(where, keyBuilder);
			return Mutation.delete(table, keyBuilder.getKeyBuilder().build());
		}
	}

	private void visitDeleteWhereClause(Expression where, DeleteKeyBuilder keyBuilder) throws SQLException
	{
		if (where != null)
		{
			DMLWhereClauseVisitor whereClauseVisitor = new DMLWhereClauseVisitor(getParameterStore())
			{

				@Override
				protected void visitExpression(Column col, Expression expression)
				{
					String columnName = unquoteIdentifier(col.getFullyQualifiedName());
					keyBuilder.set(columnName);
					expression.accept(new KeyBuilderExpressionVisitorAdapter(getParameterStore(), keyBuilder));
				}

			};
			where.accept(whereClauseVisitor);
			if (!whereClauseVisitor.isValid())
			{
				throw new SQLException(INVALID_WHERE_CLAUSE_DELETE_MESSAGE);
			}
		}
	}

	private boolean isSingleRowWhereClause(TableKeyMetaData table, Expression where)
	{
		if (where != null)
		{
			SingleRowWhereClauseValidator validator = new SingleRowWhereClauseValidator(table);
			DMLWhereClauseVisitor whereClauseVisitor = new DMLWhereClauseVisitor(getParameterStore())
			{

				@Override
				protected void visitExpression(Column col, Expression expression)
				{
					String columnName = unquoteIdentifier(col.getFullyQualifiedName());
					validator.set(columnName);
					expression.accept(
							new SingleRowWhereClauseValidatorExpressionVisitorAdapter(getParameterStore(), validator));
				}

			};
			where.accept(whereClauseVisitor);
			return whereClauseVisitor.isValid() && validator.isValid();
		}
		return false;
	}

	private void visitUpdateWhereClause(Expression where, WriteBuilder builder) throws SQLException
	{
		if (where != null)
		{
			DMLWhereClauseVisitor whereClauseVisitor = new DMLWhereClauseVisitor(getParameterStore())
			{

				@Override
				protected void visitExpression(Column col, Expression expression)
				{
					String columnName = unquoteIdentifier(col.getFullyQualifiedName());
					expression.accept(new ValueBinderExpressionVisitorAdapter<>(getParameterStore(),
							builder.set(columnName), columnName));
				}

			};
			where.accept(whereClauseVisitor);
			if (!whereClauseVisitor.isValid())
			{
				throw new SQLException(INVALID_WHERE_CLAUSE_UPDATE_MESSAGE);
			}
		}
		else
		{
			throw new SQLException(INVALID_WHERE_CLAUSE_UPDATE_MESSAGE);
		}
	}

	private static String unquoteIdentifier(String identifier)
	{
		return CloudSpannerDriver.unquoteIdentifier(identifier);
	}

	private int executeDDL(String ddl) throws SQLException
	{
		getConnection().executeDDL(ddl);
		return 0;
	}

	@Override
	public boolean execute() throws SQLException
	{
		Statement statement = null;
		boolean ddl = isDDLStatement(sql);
		if (!ddl)
		{
			try
			{
				statement = CCJSqlParserUtil.parse(sanitizeSQL(sql));
			}
			catch (JSQLParserException | TokenMgrError e)
			{
				throw new SQLException(PARSE_ERROR + sql + ": " + e.getLocalizedMessage(), e);
			}
		}
		if (!ddl && statement instanceof Select)
		{
			lastResultSet = executeQuery();
			lastUpdateCount = -1;
			return true;
		}
		else
		{
			lastUpdateCount = executeUpdate();
			lastResultSet = null;
			return false;
		}
	}

	@Override
	public CloudSpannerParameterMetaData getParameterMetaData() throws SQLException
	{
		// parse the SQL statement without executing it
		try
		{
			if (isDDLStatement(sql))
			{
				throw new SQLException("Cannot get parameter meta data for DDL statement");
			}
			Statement statement = CCJSqlParserUtil.parse(sanitizeSQL(sql));
			if (statement instanceof Insert || statement instanceof Update || statement instanceof Delete)
			{
				// Create mutation, but don't do anything with it. This
				// initializes column names of the parameter store.
				createMutations(sql);
			}
			else if (statement instanceof Select)
			{
				// Create select builder, but don't do anything with it. This
				// initializes column names of the parameter store.
				createSelectBuilder(statement);
			}
		}
		catch (JSQLParserException | TokenMgrError e)
		{
			throw new SQLException(PARSE_ERROR + sql + ": " + e.getLocalizedMessage(), e);
		}
		return new CloudSpannerParameterMetaData(this);
	}

	private InsertWorker createInsertWithSelectStatement(Insert insert, boolean forceUpdate) throws SQLException
	{
		Select select = insert.getSelect();
		if (select == null)
		{
			throw new SQLException("Insert statement must contain a select statement");
		}
		boolean isDuplicate = insert.isUseDuplicate();
		InsertWorker.DMLOperation mode;
		if (forceUpdate)
			mode = DMLOperation.Update;
		else if (isDuplicate)
			mode = DMLOperation.OnDuplicateKeyUpdate;
		else
			mode = DMLOperation.Insert;
		return new InsertWorker(getConnection(), select, insert, getConnection().isAllowExtendedMode(), mode);
	}

	private DeleteWorker createDeleteWorker(Delete delete) throws SQLException
	{
		if (delete.getTable() == null || (delete.getTables() != null && delete.getTables().size() > 0))
		{
			throw new SQLException("DELETE statement must contain only one table");
		}
		return new DeleteWorker(getConnection(), delete, getConnection().isAllowExtendedMode());
	}

	boolean isForceUpdate()
	{
		return forceUpdate;
	}

	void setForceUpdate(boolean forceUpdate)
	{
		this.forceUpdate = forceUpdate;
	}

}
