package com.shua1.sys.base.mybatis;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.session.RowBounds;

public class Page<E>
  extends ArrayList<E>
{
  private static final long serialVersionUID = 1L;
  public static final int NO_SQL_COUNT = -1;
  public static final int SQL_COUNT = 0;
  private int pageNum;
  private int pageSize;
  private int startRow;
  private int endRow;
  private long total;
  private int pages;
  
  public Page(List<E> dataList)
  {
    super(dataList);
  }
  
  public Page(int pageNum, int pageSize)
  {
    this(pageNum, pageSize, 0);
  }
  
  public Page(int offset, int pageSize, boolean falg)
  {
    this(offset, pageSize, 0, falg);
  }
  
  public Page(int pageNum, int pageSize, int total)
  {
    super(pageSize);
    this.pageSize = pageSize;
    this.pageNum = pageNum;
    this.total = total;
    this.startRow = (pageNum > 0 ? (pageNum - 1) * pageSize : 0);
    this.endRow = (pageNum * pageSize);
  }
  
  public Page(int offset, int pageSize, int total, boolean falg)
  {
    super(pageSize);
    this.pageSize = pageSize;
    this.startRow = offset;
    this.total = total;
    this.endRow = (this.startRow + this.pageSize);
  }
  
  public Page(RowBounds rowBounds, int total)
  {
    super(rowBounds.getLimit());
    this.pageSize = rowBounds.getLimit();
    this.startRow = rowBounds.getOffset();
    
    this.total = total;
    this.endRow = (this.startRow + this.pageSize);
  }
  
  public List<E> getResult()
  {
    return this;
  }
  
  public int getPages()
  {
    return this.pages;
  }
  
  public void setPages(int pages)
  {
    this.pages = pages;
  }
  
  public int getEndRow()
  {
    return this.endRow;
  }
  
  public void setEndRow(int endRow)
  {
    this.endRow = endRow;
  }
  
  public int getPageNum()
  {
    return this.pageNum;
  }
  
  public void setPageNum(int pageNum)
  {
    this.pageNum = pageNum;
  }
  
  public int getPageSize()
  {
    return this.pageSize;
  }
  
  public void setPageSize(int pageSize)
  {
    this.pageSize = pageSize;
  }
  
  public int getStartRow()
  {
    return this.startRow;
  }
  
  public void setStartRow(int startRow)
  {
    this.startRow = startRow;
  }
  
  public long getTotal()
  {
    return this.total;
  }
  
  public void setTotal(long total)
  {
    this.total = total;
  }
  
  public String toString()
  {
    return "Page{pageNum=" + this.pageNum + ", pageSize=" + this.pageSize + ", startRow=" + this.startRow + ", endRow=" + this.endRow + ", total=" + this.total + ", pages=" + this.pages + '}';
  }
}
