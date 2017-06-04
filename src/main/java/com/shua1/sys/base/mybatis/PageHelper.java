package com.shua1.sys.base.mybatis;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.RowBounds;

@Intercepts({@org.apache.ibatis.plugin.Signature(type=Executor.class, method="query", args={MappedStatement.class, Object.class, RowBounds.class, org.apache.ibatis.session.ResultHandler.class})})
public class PageHelper
  implements Interceptor
{
  private static final ThreadLocal<Page> localPage = new ThreadLocal();
  private static final List<ResultMapping> EMPTY_RESULTMAPPING = new ArrayList(0);
  private static String dialect = "";
  private static boolean offsetAsPageNum = false;
  private static boolean rowBoundsWithCount = false;
  
  public static void startPage(int pageNum, int pageSize)
  {
    startPage(pageNum, pageSize, true);
  }
  
  public static void startPage(int pageNum, int pageSize, boolean count)
  {
    localPage.set(new Page(pageNum, pageSize, count ? 0 : -1));
  }
  
  public Object intercept(Invocation invocation)
    throws Throwable
  {
    Object[] args = invocation.getArgs();
    RowBounds rowBounds = (RowBounds)args[2];
    if ((localPage.get() == null) && (rowBounds == RowBounds.DEFAULT)) {
      return invocation.proceed();
    }
    args[2] = RowBounds.DEFAULT;
    MappedStatement ms = (MappedStatement)args[0];
    Object parameterObject = args[1];
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    
    Page page = (Page)localPage.get();
    localPage.remove();
    if (page == null) {
      if (offsetAsPageNum) {
        page = new Page(rowBounds.getOffset(), rowBounds.getLimit(), rowBoundsWithCount ? 0 : -1);
      } else {
        page = new Page(rowBounds, rowBoundsWithCount ? 0 : -1);
      }
    }
    MappedStatement qs = newMappedStatement(ms, new BoundSqlSqlSource(boundSql));
    args[0] = qs;
    MetaObject msObject = SystemMetaObject.forObject(qs);
    String sql = (String)msObject.getValue("sqlSource.boundSql.sql");
    if (page.getTotal() > -1L)
    {
      msObject.setValue("sqlSource.boundSql.sql", getCountSql(sql));
      
      Object result = invocation.proceed();
      int totalCount = ((Integer)((List)result).get(0)).intValue();
      page.setTotal(totalCount);
      int totalPage = totalCount / page.getPageSize() + (totalCount % page.getPageSize() == 0 ? 0 : 1);
      page.setPages(totalPage);
      
      msObject.setValue("sqlSource.boundSql.sql", getPageSql(sql, page));
      
      msObject.setValue("resultMaps", ms.getResultMaps());
      
      result = invocation.proceed();
      
      page.addAll((List)result);
      
      return page;
    }
    msObject.setValue("sqlSource.boundSql.sql", getPageSql(sql, page));
    
    msObject.setValue("resultMaps", ms.getResultMaps());
    
    Object result = invocation.proceed();
    
    page.addAll((List)result);
    
    return page;
  }
  
  private class BoundSqlSqlSource
    implements SqlSource
  {
    BoundSql boundSql;
    
    public BoundSqlSqlSource(BoundSql boundSql)
    {
      this.boundSql = boundSql;
    }
    
    public BoundSql getBoundSql(Object parameterObject)
    {
      return this.boundSql;
    }
  }
  
  private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource)
  {
    MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_分页", newSqlSource, ms.getSqlCommandType());
    builder.resource(ms.getResource());
    builder.fetchSize(ms.getFetchSize());
    builder.statementType(ms.getStatementType());
    builder.keyGenerator(ms.getKeyGenerator());
    if ((ms.getKeyProperties() != null) && (ms.getKeyProperties().length != 0))
    {
      StringBuffer keyProperties = new StringBuffer();
      for (String keyProperty : ms.getKeyProperties()) {
        keyProperties.append(keyProperty).append(",");
      }
      keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
      builder.keyProperty(keyProperties.toString());
    }
    builder.timeout(ms.getTimeout());
    builder.parameterMap(ms.getParameterMap());
    
    List<ResultMap> resultMaps = new ArrayList();
    ResultMap resultMap = new ResultMap.Builder(ms.getConfiguration(), ms.getId(), Integer.TYPE, EMPTY_RESULTMAPPING).build();
    resultMaps.add(resultMap);
    builder.resultMaps(resultMaps);
    builder.resultSetType(ms.getResultSetType());
    builder.cache(ms.getCache());
    builder.flushCacheRequired(ms.isFlushCacheRequired());
    builder.useCache(ms.isUseCache());
    return builder.build();
  }
  
  private String getCountSql(String sql)
  {
    return "select count(0) from (" + sql + ") tmp_count";
  }
  
  private String getPageSql(String sql, Page page)
  {
    StringBuilder pageSql = new StringBuilder(200);
    if ("mysql".equals(dialect))
    {
      pageSql.append(sql);
      pageSql.append(" limit " + page.getStartRow() + "," + page.getPageSize());
      System.out.println(pageSql.toString());
    }
    else if ("hsqldb".equals(dialect))
    {
      pageSql.append(sql);
      pageSql.append(" LIMIT " + page.getPageSize() + " OFFSET " + page.getStartRow());
    }
    else if ("oracle".equals(dialect))
    {
      pageSql.append("select * from ( select temp.*, rownum row_id from ( ");
      pageSql.append(sql);
      pageSql.append(" ) temp where rownum <= ").append(page.getEndRow());
      pageSql.append(") where row_id > ").append(page.getStartRow());
    }
    return pageSql.toString();
  }
  
  public Object plugin(Object target)
  {
    if ((target instanceof Executor)) {
      return Plugin.wrap(target, this);
    }
    return target;
  }
  
  public void setProperties(Properties p)
  {
    dialect = p.getProperty("dialect");
    if ((dialect == null) || (dialect.equals(""))) {
      throw new RuntimeException("Mybatis分页插件PageHelper无法获取dialect参数!");
    }
    String offset = p.getProperty("offsetAsPageNum");
    if ((offset != null) && (offset.toUpperCase().equals("TRUE"))) {
      offsetAsPageNum = true;
    }
    String withcount = p.getProperty("rowBoundsWithCount");
    if ((withcount != null) && (withcount.toUpperCase().equals("TRUE"))) {
      rowBoundsWithCount = true;
    }
  }
}
