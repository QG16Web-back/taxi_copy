package com.qg.taxi.hbase;

import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.index.ColumnQualifier;
import org.apache.hadoop.hbase.index.Constants;
import org.apache.hadoop.hbase.index.IndexSpecification;
import org.apache.hadoop.hbase.index.client.IndexAdmin;
import org.apache.hadoop.hbase.index.coprocessor.master.IndexMasterObserver;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.AccessControlLists;
import org.apache.hadoop.hbase.security.access.Permission;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wilder Gao
 * time:2018/7/30
 * description：
 * motto: All efforts are not in vain
 */
public class HBaseSample {
    private final static Log LOG = LogFactory.getLog(HBaseSample.class.getName());

    @Getter
    private TableName tableName = null;
    private Configuration conf = null;
    @Getter
    private Connection conn = null;

    public HBaseSample(Configuration conf) throws IOException {
        this.conf = conf;
        this.tableName = TableName.valueOf("hbase_sample_table");
        this.conn = ConnectionFactory.createConnection(conf);
    }

    public HBaseSample(Configuration conf, String tableName) throws IOException {
        this.conf = conf;
        this.tableName = TableName.valueOf(tableName);
        this.conn = ConnectionFactory.createConnection(conf);
    }

    public void myTest() throws Exception {
        myScanTest();
    }

    public void test() throws Exception {
        try {
            testCreateTable();
//      jcTestHBase();
            testMultiSplit();
            testPut();
            createIndex();
            testScanDataByIndex();
            testModifyTable();
            testGet();
            testScanData();
            testSingleColumnValueFilter();
            testFilterList();
            testDelete();
            dropIndex();
            dropTable();
            testCreateMOBTable();
            testMOBDataInsertion();
            testMOBDataRead();
            dropTable();
        } catch (Exception e) {
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e1) {
                    LOG.error("Failed to close the connection ", e1);
                }
            }
        }
    }

    /**
     * Create user info table
     */
    public void testCreateTable() {
        LOG.info("Entering testCreateTable.");

        // Specify the table descriptor.
        HTableDescriptor htd = new HTableDescriptor(tableName);

        // Set the column family name to info.
        HColumnDescriptor hcd = new HColumnDescriptor("info");

        // Set data encoding methods. HBase provides DIFF,FAST_DIFF,PREFIX
        // and PREFIX_TREE
        hcd.setDataBlockEncoding(DataBlockEncoding.FAST_DIFF);

        // Set compression methods, HBase provides two default compression
        // methods:GZ and SNAPPY
        // GZ has the highest compression rate,but low compression and
        // decompression effeciency,fit for cold data
        // SNAPPY has low compression rate, but high compression and
        // decompression effeciency,fit for hot data.
        // it is advised to use SANPPY
        hcd.setCompressionType(Compression.Algorithm.SNAPPY);

        htd.addFamily(hcd);

        Admin admin = null;
        try {
            // Instantiate an Admin object.
            admin = conn.getAdmin();
            if (!admin.tableExists(tableName)) {
                LOG.info("Creating table...");
                admin.createTable(htd);
                LOG.info(admin.getClusterStatus());
                LOG.info(admin.listNamespaceDescriptors());
                LOG.info("Table created successfully.");
            } else {
                LOG.warn("table already exists");
            }
        } catch (IOException e) {
            LOG.error("Create table failed.", e);
        } finally {
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Failed to close admin ", e);
                }
            }
        }
        LOG.info("Exiting testCreateTable.");
    }

    public void testMultiSplit() {
        LOG.info("Entering testMultiSplit.");

        Table table = null;
        Admin admin = null;
        try {
            admin = conn.getAdmin();

            // initilize a HTable object
            table = conn.getTable(tableName);
            Set<HRegionInfo> regionSet = new HashSet<>();
            List<HRegionLocation> regionList = conn.getRegionLocator(tableName).getAllRegionLocations();
            for (HRegionLocation hrl : regionList) {
                regionSet.add(hrl.getRegionInfo());
            }
            byte[][] sk = new byte[4][];
            sk[0] = "A".getBytes();
            sk[1] = "D".getBytes();
            sk[2] = "F".getBytes();
            sk[3] = "H".getBytes();
            for (HRegionInfo regionInfo : regionSet) {
                ((HBaseAdmin) admin).multiSplit(regionInfo.getRegionName(), sk);
            }
            LOG.info("MultiSplit successfully.");
        } catch (Exception e) {
            LOG.error("MultiSplit failed ", e);
        } finally {
            if (table != null) {
                try {
                    // Close table object
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting testMultiSplit.");
    }

    /**
     * Insert data
     */
    public void testPut() {
        LOG.info("Entering testPut.");

        // Specify the column family name.
        byte[] familyName = Bytes.toBytes("info");
        // Specify the column name.
        byte[][] qualifiers = {Bytes.toBytes("name"), Bytes.toBytes("gender"), Bytes.toBytes("age"),
                Bytes.toBytes("address")};

        Table table = null;
        try {
            // Instantiate an HTable object.
            table = conn.getTable(tableName);
            List<Put> puts = new ArrayList<Put>();
            // Instantiate a Put object.
            Put put = new Put(Bytes.toBytes("012005000201"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Zhang San"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("19"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Shenzhen, Guangdong"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000202"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Li Wanting"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Female"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("23"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Shijiazhuang, Hebei"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000203"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Wang Ming"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("26"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Ningbo, Zhejiang"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000204"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Li Gang"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("18"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Xiangyang, Hubei"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000205"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Zhao Enru"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Female"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("21"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Shangrao, Jiangxi"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000206"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Chen Long"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("32"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Zhuzhou, Hunan"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000207"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Zhou Wei"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Female"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("29"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Nanyang, Henan"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000208"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Yang Yiwen"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Female"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("30"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Kaixian, Chongqing"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000209"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Xu Bing"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("26"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Weinan, Shaanxi"));
            puts.add(put);

            put = new Put(Bytes.toBytes("012005000210"));
            put.addColumn(familyName, qualifiers[0], Bytes.toBytes("Xiao Kai"));
            put.addColumn(familyName, qualifiers[1], Bytes.toBytes("Male"));
            put.addColumn(familyName, qualifiers[2], Bytes.toBytes("25"));
            put.addColumn(familyName, qualifiers[3], Bytes.toBytes("Dalian, Liaoning"));
            puts.add(put);

            // Submit a put request.
            table.put(puts);

            LOG.info("Put successfully.");
        } catch (IOException e) {
            LOG.error("Put failed ", e);
        } finally {
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testPut.");
    }

    public void createIndex() {
        LOG.info("Entering createIndex.");

        String indexName = "index_name";

        // Create index instance
        IndexSpecification iSpec = new IndexSpecification(indexName);

        iSpec.addIndexColumn(new HColumnDescriptor("info"), "name", ColumnQualifier.ValueType.String);

        IndexAdmin iAdmin = null;
        Admin admin = null;
        try {
            // Instantiate IndexAdmin Object
            iAdmin = new IndexAdmin(conf);

            // Create Secondary Index
            iAdmin.addIndex(tableName, iSpec);

            admin = conn.getAdmin();

            // Specify the encryption type of indexed column
            HTableDescriptor htd = admin.getTableDescriptor(tableName);
            admin.disableTable(tableName);
            htd = admin.getTableDescriptor(tableName);
            // Instantiate index column description.
            HColumnDescriptor indexColDesc = new HColumnDescriptor(
                    IndexMasterObserver.DEFAULT_INDEX_COL_DESC);

            // Set the description of index as the HTable description.
            htd.setValue(Constants.INDEX_COL_DESC_BYTES, indexColDesc.toByteArray());
            admin.modifyTable(tableName, htd);
            admin.enableTable(tableName);

            LOG.info("Create index successfully.");

        } catch (IOException e) {
            LOG.error("Create index failed.", e);
        } finally {
            if (admin != null) {
                try {
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
            if (iAdmin != null) {
                try {
                    // Close IndexAdmin Object
                    iAdmin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting createIndex.");
    }

    /**
     * Scan data by secondary index.
     */
    public void testScanDataByIndex() {
        LOG.info("Entering testScanDataByIndex.");

        Table table = null;
        ResultScanner scanner = null;
        try {
            table = conn.getTable(tableName);

            // Create a filter for indexed column.
            Filter filter = new SingleColumnValueFilter(Bytes.toBytes("info"), Bytes.toBytes("name"),
                    CompareFilter.CompareOp.EQUAL, "Li Gang".getBytes());
            Scan scan = new Scan();
            scan.setFilter(filter);
            scanner = table.getScanner(scan);
            LOG.info("Scan indexed data.");

            for (Result result : scanner) {
                for (Cell cell : result.rawCells()) {
                    LOG.info(Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                            + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                            + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                            + Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            LOG.info("Scan data by index successfully.");
        } catch (IOException e) {
            LOG.error("Scan data by index failed ", e);
        } finally {
            if (scanner != null) {
                // Close the scanner object.
                scanner.close();
            }
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                LOG.error("Close table failed ", e);
            }
        }

        LOG.info("Exiting testScanDataByIndex.");
    }

    /**
     * Modify a Table
     */
    public void testModifyTable() {
        LOG.info("Entering testModifyTable.");

        // Specify the column family name.
        byte[] familyName = Bytes.toBytes("education");

        Admin admin = null;
        try {
            // Instantiate an Admin object.
            admin = conn.getAdmin();

            // Obtain the table descriptor.
            HTableDescriptor htd = admin.getTableDescriptor(tableName);

            // Check whether the column family is specified before modification.
            if (!htd.hasFamily(familyName)) {
                // Create the column descriptor.
                HColumnDescriptor hcd = new HColumnDescriptor(familyName);
                htd.addFamily(hcd);

                // Disable the table to get the table offline before modifying
                // the table.
                admin.disableTable(tableName);
                // Submit a modifyTable request.
                admin.modifyTable(tableName, htd);
                // Enable the table to get the table online after modifying the
                // table.
                admin.enableTable(tableName);
            }
            LOG.info("Modify table successfully.");
        } catch (IOException e) {
            LOG.error("Modify table failed ", e);
        } finally {
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting testModifyTable.");
    }

    /**
     * Get Data
     */
    public void testGet() {
        LOG.info("Entering testGet.");

        // Specify the column family name.
        byte[] familyName = Bytes.toBytes("info");
        // Specify the column name.
        byte[][] qualifier = {Bytes.toBytes("name"), Bytes.toBytes("address")};
        // Specify RowKey.
        byte[] rowKey = Bytes.toBytes("012005000201");

        Table table = null;
        try {
            // Create the Configuration instance.
            table = conn.getTable(tableName);

            // Instantiate a Get object.
            Get get = new Get(rowKey);

            // Set the column family name and column name.
            get.addColumn(familyName, qualifier[0]);
            get.addColumn(familyName, qualifier[1]);

            // Submit a get request.
            Result result = table.get(get);

            // Print query results.
            for (Cell cell : result.rawCells()) {
                LOG.info(Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                        + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                        + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                        + Bytes.toString(CellUtil.cloneValue(cell)));
            }
            LOG.info("Get data successfully.");
        } catch (IOException e) {
            LOG.error("Get data failed ", e);
        } finally {
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testGet.");
    }

    /**
     * funrily 用于测试速度的方法
     */
    public void myScanTest() {
        System.out.println("准备进行HBase数据测试！");
        Table table = null;
        ResultScanner rScanner = null;
        try {
            long start = System.currentTimeMillis();
            table = conn.getTable(tableName);
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes("location"), Bytes.toBytes("GEOHASH7"));
            // 注意：hbase
            scan.setStartRow(Bytes.toBytes("0201"));
            scan.setStopRow(Bytes.toBytes("0359z"));
            scan.setCaching(2000);

            // 添加拦截器
//            Filter filter = new SingleColumnValueFilter(Bytes.toBytes("info"), Bytes.toBytes("name"),
//                    CompareOp.EQUAL, "Li Gang".getBytes());

            // 查询rowkey是否包含部分字符串
            Filter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,
                    new SubstringComparator("AY5E09"));
            scan.setFilter(filter);

            // 根据属性来拦截 全表查询2017年2月1日8点1分的数据
//		  RegexStringComparator comp = new RegexStringComparator("2017-02-01 08:01");
//		  SingleColumnValueFilter filter = new SingleColumnValueFilter(Bytes.toBytes("info"), Bytes.toBytes("GPS_TIME"), CompareOp.EQUAL, comp);
//		  scan.setFilter(filter);

            // 根据rowkey前缀查询 在全表 查询rowkey以100开头的数据 共111111条
//		  scan.setFilter(new PrefixFilter(Bytes.toBytes("20")));
            rScanner = table.getScanner(scan);
            int i = 0;
            for (Result r = rScanner.next(); r != null; r = rScanner.next()) {
                i += r.size();
                for (Cell cell : r.rawCells()) {
                    System.out.println("数据 : " + i + " === " + Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                            + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                            + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                            + Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            System.out.println("条数：" + i);
            long end = System.currentTimeMillis();
            System.out.println("时间：" + (end - start));
        } catch (IOException e) {
            LOG.error("Scan data failed ", e);
        } finally {
            if (rScanner != null) {
                // Close the scanner object.
                rScanner.close();
            }
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
    }



    public void testScanData() {
        LOG.info("Entering testScanData.");

        Table table = null;
        // Instantiate a ResultScanner object.
        ResultScanner rScanner = null;
        try {
            // Create the Configuration instance.
            table = conn.getTable(tableName);

            // Instantiate a Get object.
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));

            // Set the cache size.
            scan.setCaching(1000);

            // Submit a scan request.
            rScanner = table.getScanner(scan);

            // Print query results.
            for (Result r = rScanner.next(); r != null; r = rScanner.next()) {
                for (Cell cell : r.rawCells()) {
                    LOG.info("数据 : " + Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                            + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                            + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                            + Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            LOG.info("Scan data successfully.");
        } catch (IOException e) {
            LOG.error("Scan data failed ", e);
        } finally {
            if (rScanner != null) {
                // Close the scanner object.
                rScanner.close();
            }
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testScanData.");
    }

    public void testSingleColumnValueFilter() {
        LOG.info("Entering testSingleColumnValueFilter.");

        Table table = null;

        // Instantiate a ResultScanner object.
        ResultScanner rScanner = null;

        try {
            // Create the Configuration instance.
            table = conn.getTable(tableName);

            // Instantiate a Get object.
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));

            // Set the filter criteria.
            SingleColumnValueFilter filter = new SingleColumnValueFilter(Bytes.toBytes("info"),
                    Bytes.toBytes("name"), CompareFilter.CompareOp.EQUAL, Bytes.toBytes("Xu Bing"));

            scan.setFilter(filter);

            // Submit a scan request.
            rScanner = table.getScanner(scan);

            // Print query results.
            for (Result r = rScanner.next(); r != null; r = rScanner.next()) {
                for (Cell cell : r.rawCells()) {
                    LOG.info(Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                            + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                            + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                            + Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            LOG.info("Single column value filter successfully.");
        } catch (IOException e) {
            LOG.error("Single column value filter failed ", e);
        } finally {
            if (rScanner != null) {
                // Close the scanner object.
                rScanner.close();
            }
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testSingleColumnValueFilter.");
    }

    public void testFilterList() {
        LOG.info("Entering testFilterList.");

        Table table = null;

        // Instantiate a ResultScanner object.
        ResultScanner rScanner = null;

        try {
            // Create the Configuration instance.
            table = conn.getTable(tableName);

            // Instantiate a Get object.
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));

            // Instantiate a FilterList object in which filters have "and"
            // relationship with each other.
            FilterList list = new FilterList(FilterList.Operator.MUST_PASS_ALL);
            // Obtain data with age of greater than or equal to 20.
            list.addFilter(new SingleColumnValueFilter(Bytes.toBytes("info"), Bytes.toBytes("age"),
                    CompareFilter.CompareOp.GREATER_OR_EQUAL, Bytes.toBytes(new Long(20))));
            // Obtain data with age of less than or equal to 29.
            list.addFilter(new SingleColumnValueFilter(Bytes.toBytes("info"), Bytes.toBytes("age"),
                    CompareFilter.CompareOp.LESS_OR_EQUAL, Bytes.toBytes(new Long(29))));

            scan.setFilter(list);

            // Submit a scan request.
            rScanner = table.getScanner(scan);
            // Print query results.
            for (Result r = rScanner.next(); r != null; r = rScanner.next()) {
                for (Cell cell : r.rawCells()) {
                    LOG.info(Bytes.toString(CellUtil.cloneRow(cell)) + ":"
                            + Bytes.toString(CellUtil.cloneFamily(cell)) + ","
                            + Bytes.toString(CellUtil.cloneQualifier(cell)) + ","
                            + Bytes.toString(CellUtil.cloneValue(cell)));
                }
            }
            LOG.info("Filter list successfully.");
        } catch (IOException e) {
            LOG.error("Filter list failed ", e);
        } finally {
            if (rScanner != null) {
                // Close the scanner object.
                rScanner.close();
            }
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testFilterList.");
    }

    /**
     * deleting data
     */
    public void testDelete() {
        LOG.info("Entering testDelete.");

        byte[] rowKey = Bytes.toBytes("012005000201");

        Table table = null;
        try {
            // Instantiate an HTable object.
            table = conn.getTable(tableName);

            // Instantiate an Delete object.
            Delete delete = new Delete(rowKey);

            // Submit a delete request.
            table.delete(delete);

            LOG.info("Delete table successfully.");
        } catch (IOException e) {
            LOG.error("Delete table failed ", e);
        } finally {
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
        LOG.info("Exiting testDelete.");
    }

    public void dropIndex() {
        LOG.info("Entering dropIndex.");

        String indexName = "index_name";

        IndexAdmin iAdmin = null;
        try {
            // Instantiate IndexAdmin Object
            iAdmin = new IndexAdmin(conf);

            // Delete Secondary Index
            iAdmin.dropIndex(tableName, indexName);

            LOG.info("Drop index successfully.");
        } catch (IOException e) {
            LOG.error("Drop index failed ", e);
        } finally {
            if (iAdmin != null) {
                try {
                    // Close Secondary Index
                    iAdmin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting dropIndex.");
    }

    /**
     * Delete user table
     */
    public void dropTable() {
        LOG.info("Entering dropTable.");

        Admin admin = null;
        try {
            admin = conn.getAdmin();
            if (admin.tableExists(tableName)) {
                // Disable the table before deleting it.
                admin.disableTable(tableName);

                // Delete table.
                admin.deleteTable(tableName);
            }
            LOG.info("Drop table successfully.");
        } catch (IOException e) {
            LOG.error("Drop table failed ", e);
        } finally {
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting dropTable.");
    }

    public void grantACL() {
        LOG.info("Entering grantACL.");

        String user = "huawei";
        String permissions = "RW";

        String familyName = "info";
        String qualifierName = "name";

        Table mt = null;
        Admin hAdmin = null;
        try {
            // Create ACL Instance
            mt = conn.getTable(AccessControlLists.ACL_TABLE_NAME);

            Permission perm = new Permission(Bytes.toBytes(permissions));

            hAdmin = conn.getAdmin();
            HTableDescriptor ht = hAdmin.getTableDescriptor(tableName);

            // Judge whether the table exists
            if (hAdmin.tableExists(mt.getName())) {
                // Judge whether ColumnFamily exists
                if (ht.hasFamily(Bytes.toBytes(familyName))) {
                    // grant permission
                    AccessControlClient.grant(conn, tableName, user, Bytes.toBytes(familyName),
                            (qualifierName == null ? null : Bytes.toBytes(qualifierName)), perm.getActions());
                } else {
                    // grant permission
                    AccessControlClient.grant(conn, tableName, user, null, null, perm.getActions());
                }
            }
            LOG.info("Grant ACL successfully.");
        } catch (Throwable e) {
            LOG.error("Grant ACL failed ", e);
        } finally {
            if (mt != null) {
                try {
                    // Close
                    mt.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }

            if (hAdmin != null) {
                try {
                    // Close Admin Object
                    hAdmin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting grantACL.");
    }

    public void testMOBDataRead() {
        LOG.info("Entering testMOBDataRead.");
        ResultScanner scanner = null;
        Table table = null;
        Admin admin = null;
        try {

            // get table object representing table tableName
            table = conn.getTable(tableName);
            admin = conn.getAdmin();
            admin.flush(table.getName());
            Scan scan = new Scan();
            // get table scanner
            scanner = table.getScanner(scan);
            for (Result result : scanner) {
                byte[] value = result.getValue(Bytes.toBytes("mobcf"), Bytes.toBytes("cf1"));
                String string = Bytes.toString(value);
                LOG.info("value:" + string);
            }
            LOG.info("MOB data read successfully.");
        } catch (Exception e) {
            LOG.error("MOB data read failed ", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (table != null) {
                try {
                    // Close table object
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting testMOBDataRead.");
    }

    public void testMOBDataInsertion() {
        LOG.info("Entering testMOBDataInsertion.");

        Table table = null;
        try {
            // set row name to "row"
            Put p = new Put(Bytes.toBytes("row"));
            byte[] value = new byte[1000];
            // set the column value of column family mobcf with the value of "cf1"
            p.addColumn(Bytes.toBytes("mobcf"), Bytes.toBytes("cf1"), value);
            // get the table object represent table tableName
            table = conn.getTable(tableName);
            // put data
            table.put(p);
            LOG.info("MOB data inserted successfully.");

        } catch (Exception e) {
            LOG.error("MOB data inserted failed ", e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (Exception e1) {
                    LOG.error("Close table failed ", e1);
                }
            }
        }
        LOG.info("Exiting testMOBDataInsertion.");
    }

    public void testCreateMOBTable() {
        LOG.info("Entering testCreateMOBTable.");

        Admin admin = null;
        try {
            // Create Admin instance
            admin = conn.getAdmin();
            HTableDescriptor tabDescriptor = new HTableDescriptor(tableName);
            HColumnDescriptor mob = new HColumnDescriptor("mobcf");
            // Open mob function
            mob.setMobEnabled(true);
            // Set mob threshold
            mob.setMobThreshold(10L);
            tabDescriptor.addFamily(mob);
            admin.createTable(tabDescriptor);
            LOG.info("MOB Table is created successfully.");

        } catch (Exception e) {
            LOG.error("MOB Table is created failed ", e);
        } finally {
            if (admin != null) {
                try {
                    // Close the Admin object.
                    admin.close();
                } catch (IOException e) {
                    LOG.error("Close admin failed ", e);
                }
            }
        }
        LOG.info("Exiting testCreateMOBTable.");
    }

    /**
     * Wilder 的测试代码
     *
     * @throws IOException
     */
    public void jcTestHBase() throws IOException {
        Table table = conn.getTable(tableName);
//	 Get get = new Get(Bytes.toBytes("012005000208"));
//	 get.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));
//	 Result r = table.get(get);
//	 System.out.println(Bytes.toString(r.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));

        Put put = new Put(Bytes.toBytes("TheRealMT"));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),
                Bytes.toBytes("Wilder Gao"));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("gender"),
                Bytes.toBytes("Male"));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("age"),
                Bytes.toBytes("21"));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("address"),
                Bytes.toBytes("your heart"));

        table.put(put);

        Get get = new Get(Bytes.toBytes("TheRealMT"));
        Result result = table.get(get);
        System.out.println("name:" + Bytes.toString(result.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));
        System.out.println("gender:" + Bytes.toString(result.getValue(Bytes.toBytes("info"), Bytes.toBytes("gender"))));
        System.out.println("address:" + Bytes.toString(result.getValue(Bytes.toBytes("info"), Bytes.toBytes("address"))));
        System.out.println("age:" + Bytes.toString(result.getValue(Bytes.toBytes("info"), Bytes.toBytes("age"))));
    }

    public void getGeoHash() {
        Table table = null;
        ResultScanner rScanner = null;
        try {
            long start = System.currentTimeMillis();
            table = conn.getTable(tableName);
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes("other"), Bytes.toBytes("CAR_STAT1"));
            scan.setStartRow(Bytes.toBytes("02011001"));
            scan.setStopRow(Bytes.toBytes("02011145"));
            scan.setCaching(2000);
            Filter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,
                    new SubstringComparator("AF41Z0"));
            scan.setFilter(filter);
            rScanner = table.getScanner(scan);
            List<String> builderList = new ArrayList<>();
            int stat = 4;
            StringBuilder string = new StringBuilder();
            for (Result r = rScanner.next(); r != null; r = rScanner.next()) {
                for (Cell cell : r.rawCells()) {
                    if (Integer.parseInt(Bytes.toString(CellUtil.cloneValue(cell))) == stat) {
                        string.append(" ").append(Bytes.toString(CellUtil.cloneRow(cell)).substring(8, 15));
                    } else {
                        if (string.length() == 0) {
                            string = new StringBuilder("null");
                        }
                        builderList.add(string.toString());
                        stat = Integer.parseInt(Bytes.toString(CellUtil.cloneValue(cell)));
                        string = new StringBuilder(Bytes.toString(CellUtil.cloneRow(cell)).substring(8, 15));
                        System.out.println();
                        System.out.print(Integer.parseInt(Bytes.toString(CellUtil.cloneValue(cell))));
                    }
                }
            }
            builderList.add(string.toString());
            System.out.println(builderList);
            System.out.println(builderList.size());
            for (String s : builderList) {
                System.out.println(s);
            }
            long end = System.currentTimeMillis();
            System.out.println("时间：" + (end - start));
        } catch (IOException e) {
            LOG.error("Scan data failed ", e);
        } finally {
            if (rScanner != null) {
                // Close the scanner object.
                rScanner.close();
            }
            if (table != null) {
                try {
                    // Close the HTable object.
                    table.close();
                } catch (IOException e) {
                    LOG.error("Close table failed ", e);
                }
            }
        }
    }
}
