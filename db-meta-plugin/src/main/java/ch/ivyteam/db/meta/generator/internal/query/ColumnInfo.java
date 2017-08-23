package ch.ivyteam.db.meta.generator.internal.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.generator.internal.ConstantBuilder;
import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;

/**
 * This class contains information to generate java code
 * depending on a {@link SqlTableColumn}.
 * @author fs
 * @since 11.01.2012
 */
public abstract class ColumnInfo
{
  private final TableInfo tableInfo;

  /**
   * @param meta
   * @param tableInfo
   * @return -
   */
  public static List<ColumnInfo> getColumns(SqlMeta meta, TableInfo tableInfo)
  {
    SqlView view = JavaClassGeneratorUtil.getQueryView(meta, tableInfo.getTable());
    if (view != null)
    {
      return getColumns(meta, tableInfo, view);
    }
    return getColumns(tableInfo);
  }

  private static List<ColumnInfo> getColumns(TableInfo tableInfo)
  {
    List<ColumnInfo> result = new ArrayList<ColumnInfo>();
    for(SqlTableColumn column: tableInfo.getTable().getColumns())
    {
      if (! column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.HIDE_FIELD_ON_QUERY))
      {
        result.add(new TableColumnInfo(tableInfo, column));
      }
    }
    return result;
  }

  private static List<ColumnInfo> getColumns(SqlMeta meta, TableInfo tableInfo, SqlView view)
  {
    List<ColumnInfo> result = new ArrayList<ColumnInfo>();
    int pos = 0;
    for (SqlViewColumn column : view.getColumns())
    {
      if (! column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.HIDE_FIELD_ON_QUERY))
      {
        result.add(new ViewColumnInfo(meta, view.getTableAliases(), tableInfo, column, view.getSelects().get(0).getExpressions().get(pos).getExpression()));
      }
      pos++;
    }
    return result;
  }

  /**
   * @param columnInfos
   * @return -
   */
  public static Set<EnumerationInfo> getEnumerationInfos(List<ColumnInfo> columnInfos)
  {
    Set<EnumerationInfo> enumerationsInfos = new HashSet<EnumerationInfo>();
    for (ColumnInfo column : columnInfos)
    {
      if (column.isEnumeration())
      {
        enumerationsInfos.add(column.getEnumerationInfo());
      }
    }
    return enumerationsInfos;
  }

  /**
   * Constructor
   * @param tableInfo -
   */
  public ColumnInfo(TableInfo tableInfo)
  {
    this.tableInfo = tableInfo;
  }

  /**
   * @return -
   */
  public String getName()
  {
    return getColumn().getId();
  }

  /**
   * @return Name of the constant to this column fild
   */
  public String getJavaColumnConstantName()
  {
    StringBuilder builder = new StringBuilder();
    builder.append(getTableInfo().getJavaDataClassName());
    builder.append(".COLUMN_");
    builder.append(new ConstantBuilder(getColumn().getId()).toConstant());
    return builder.toString();
  }

  /**
   * @return additional comments
   */
  public String getAdditionalComments()
  {
    return null;
  }

  /**
   * @return -
   */
  public boolean isGroupAndOrderBySupported()
  {
    return true;
  }

  /**
   * @return -
   */
  public boolean isSumSupported()
  {
    return !isForeignOrPrimaryKey() &&
      ( supportsIntegerOption() || supportsDecimalOption());
  }

  /**
   * @return -
   */
  public boolean isAvgSupported()
  {
    return !isForeignOrPrimaryKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsDateTimeOption()
    );
  }

  /**
   * @return -
   */
  public boolean isMinSupported()
  {
    return !isForeignKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsStringOption() ||
      supportsDateTimeOption()
    );
  }

  /**
   * @return -
   */
  public boolean isMaxSupported()
  {
    return !isForeignKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsStringOption() ||
      supportsDateTimeOption()
    );
  }

  /**
   * @return -
   */
  public boolean supportsDecimalOption()
  {
    DataType dataType = getDataType();
    return dataType == DataType.DECIMAL ||
           dataType == DataType.FLOAT ||
           dataType == DataType.NUMBER;
  }

  /**
   * @return true when the column's values can be mapped to an int data type; false otherwise
   */
  public boolean supportsIntegerOption()
  {
    if (isEnumeration())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.INTEGER || dataType == DataType.BIGINT;
  }

  /**
   * @return -
   */
  public boolean supportsStringOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.CHAR ||
           dataType == DataType.VARCHAR;
  }
  
  public boolean supportsClobOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.CLOB;
  }

  /**
   * @return -
   */
  public boolean supportsDateTimeOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.DATE ||
           dataType == DataType.DATETIME;
  }

  /**
   * @return -
   */
  public boolean supportsBooleanOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.BIT;
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.query.ColumnInfo#getDataType()
   */
  private DataType getDataType()
  {
    if (getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.FILTER_QUERY_DATA_TYPE))
    {
      return DataType.valueOf(getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.FILTER_QUERY_DATA_TYPE));
    }
    return getColumn().getDataType().getDataType();
  }

  /**
   * @return column
   */
  protected abstract SqlTableColumn getColumn();

  /**
   * @return true, if this field is linked to an Enumeration
   */
  public boolean isEnumeration()
  {
    return getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.ENUM);
  }

  /**
   * @return Java type name of the enumeration
   */
  public EnumerationInfo getEnumerationInfo()
  {
    String enumName = getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.ENUM);
    return new EnumerationInfo(enumName);
  }

  private boolean isForeignOrPrimaryKey()
  {
    return isForeignKey() || isPrimaryKey();
  }

  private boolean isForeignKey()
  {
    return getColumn().getReference() != null;
  }

  private boolean isPrimaryKey()
  {
    return tableInfo.getTable().getPrimaryKey().getPrimaryKeyColumns().contains(getColumn().getId());
  }
  
  /**
   * @return table info
   */
  protected TableInfo getTableInfo()
  {
    return tableInfo;
  }
  
  public boolean isDeprecated()
  {
    return getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.DEPRECATED);
  }

  public String getDeprecatedUseColumnInstead()
  {
    String useColumnNameInstead = getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.DEPRECATED);
    if (StringUtils.isBlank(useColumnNameInstead))
    {
      throw new IllegalStateException("No column defined which should be used instead of deprecated column "+getName()); 
    }
    SqlTableColumn useColumnInstead = tableInfo.getTable().findColumn(useColumnNameInstead);
    if (useColumnInstead == null)
    {
      throw new IllegalStateException("Column '"+useColumnNameInstead+"' defined to used instead of deprecated column "+getName()+" not found");
    }
    return useColumnInstead.getId();
  }

  /**
   * @author rwei
   * @since Jan 30, 2012
   */
  private static class TableColumnInfo extends ColumnInfo
  {
    private final SqlTableColumn column;

    /**
     * Constructor
     * @param tableInfo
     * @param column
     */
    public TableColumnInfo(TableInfo tableInfo, SqlTableColumn column)
    {
      super(tableInfo);
      this.column = column;
    }

    /**
     * @see ch.ivyteam.db.meta.generator.internal.query.ColumnInfo#getColumn()
     */
    @Override
    protected SqlTableColumn getColumn()
    {
      return column;
    }
  }

  /**
   * @author rwei
   * @since Jan 30, 2012
   */
  private static class ViewColumnInfo extends ColumnInfo
  {
    private SqlViewColumn column;
    private SqlMeta meta;
    private SqlAtom expression;
    private Map<String, String> tableAliases;

    /**
     * Constructor
     * @param meta
     * @param tableAliases 
     * @param tableInfo
     * @param column
     * @param expression 
     */
    public ViewColumnInfo(SqlMeta meta, Map<String, String> tableAliases, TableInfo tableInfo, SqlViewColumn column, SqlAtom expression)
    {
      super(tableInfo);
      this.meta = meta;
      this.tableAliases = tableAliases;
      this.column = column;
      this.expression = expression;
    }

    /**
     * @see ch.ivyteam.db.meta.generator.internal.query.ColumnInfo#getName()
     */
    @Override
    public String getName()
    {
      String id = column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_FIELD_NAME);
      if (id != null)
      {
        return id;
      }
      return column.getId();
    }

    /**
     * @see ch.ivyteam.db.meta.generator.internal.query.ColumnInfo#getAdditionalComments()
     */
    @Override
    public String getAdditionalComments()
    {
      if (expression instanceof SqlFullQualifiedColumnName)
      {
        SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName)expression;
        if (!getTableInfo().getTable().getId().equals(columnName.getTable()))
        {
          return JavaClassGeneratorUtil.convertToJavaDoc(
                  "<p>This is a virtual column. It contains the same value as the column "+
                  "<code>"+columnName.getColumn()+"</code> of the referenced <code>"+
                  JavaClassGeneratorUtil.removeTablePrefix(columnName.getTable())+"</code>.</p>", 5);
        }
      }
      return null;
    }

    /**
     * @see ch.ivyteam.db.meta.generator.internal.query.ColumnInfo#getColumn()
     */
    @Override
    protected SqlTableColumn getColumn()
    {
      if (expression instanceof SqlFullQualifiedColumnName)
      {
        SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName)expression;
        return getColumn(columnName);
      }
      else if (expression instanceof SqlCaseExpr)
      {
        SqlCaseExpr caseExpr = (SqlCaseExpr)expression;
        return getColumn(caseExpr.getWhenThenList().get(0).getColumnName());
      }
      else if (expression instanceof SqlComplexCaseExpr)
      {
        SqlComplexCaseExpr caseExpr = (SqlComplexCaseExpr) expression;
        SqlAtom atom = caseExpr.getWhenThenList().get(0).getAction();
        if (atom instanceof SqlFullQualifiedColumnName)
        {
          return getColumn((SqlFullQualifiedColumnName) atom);
        }
        throw new IllegalStateException("Unsupported view expression "+expression);
      }
      else
      {
        throw new IllegalStateException("Unsupported view expression "+expression);
      }
    }

    private SqlTableColumn getColumn(SqlFullQualifiedColumnName columnName)
    {
      SqlTable table = findTable(columnName.getTable());
      SqlTableColumn tableColumn = table.findColumn(columnName.getColumn());
      if (tableColumn == null)
      {
        throw new IllegalStateException("Unknown column  "+columnName);
      }
      return tableColumn;
    }

    private SqlTable findTable(String tableOrAlias)
    {
      SqlTable table = meta.findTable(tableOrAlias);
      if (table != null)
      {
        return table;
      }
      String tableName = tableAliases.get(tableOrAlias);
      if (tableName == null)
      {
        throw new IllegalStateException("Unknown table "+tableOrAlias);
      }
      table = meta.findTable(tableName);
      if (table == null)
      {
        throw new IllegalStateException("Unknown table "+tableOrAlias);
      }
      return table;
    }

    @Override
    public String getJavaColumnConstantName()
    {
      StringBuilder builder = new StringBuilder();
      builder.append(getTableInfo().getJavaDataClassName());
      builder.append(".QueryView.VIEW_COLUMN_");
      builder.append(new ConstantBuilder(column.getId()).toConstant());
      return builder.toString();
    }
  }
}
