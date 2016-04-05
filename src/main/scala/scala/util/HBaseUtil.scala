package scala.util

import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName, HBaseConfiguration}
import org.apache.hadoop.hbase.client._


/**
  * Created by C.J.YOU on 2016/3/21.
  */
object HBaseUtil {

  case class RowTelecomData(row:String, value:String)

  /**
    * 获取hbase的连接器
    * @return connection
    */
  def getConnection: Connection ={

    val hbaseConf = HBaseConfiguration.create
    hbaseConf.set("fs.defaultFS", "hdfs://ns1"); // nameservices的路径
    hbaseConf.set("dfs.nameservices", "ns1");  //
    hbaseConf.set("dfs.ha.namenodes.ns1", "nn1,nn2"); //namenode的路径
    hbaseConf.set("dfs.namenode.rpc-address.ns1.nn1", "server3:9000"); // namenode 通信地址
    hbaseConf.set("dfs.namenode.rpc-address.ns1.nn2", "server4:9000"); // namenode 通信地址
    // 设置namenode自动切换的实现类
    hbaseConf.set("dfs.client.failover.proxy.provider.ns1", "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");
    hbaseConf.set("hbase.rootdir", "hdfs://ns1/hbase")
    hbaseConf.set("hbase.zookeeper.quorum", "server0,server1,server2")
    hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")

    val connection = ConnectionFactory.createConnection(hbaseConf)
    sys.addShutdownHook {
      connection.close()
    }

    connection
  }

  /**
    * 创建hbase表
    * @param tableName 表名
    * @param columnFamilys 列族的声明
    * @param connection 连接器
    */
  def  createHbaseTable(tableName: TableName,columnFamilys:List[String],connection: Connection): Unit ={
   // connection.getAdmin.createTable(new HTableDescriptor(tableName).addFamily(new HColumnDescriptor(columnFamily).setMaxVersions(3)))
    val admin = connection.getAdmin
    val htd = new HTableDescriptor(tableName)
    for (family <- columnFamilys) {
      val hcd = new HColumnDescriptor(family)
      htd.addFamily(hcd.setMaxVersions(3))
    }
    admin.createTable(htd)
    admin.close()
  }

  /**
    * row key 是否存在
    */
  def existRowKey(row:String,table:Table): Boolean ={

    val get = new Get(row.getBytes())
    val result = table.get(get)
    if (result.isEmpty) {
      return false
    }
    true
  }

  /**
    * 存入数据到对应的table中
    */
  def put(tableName:String,data:RowTelecomData): Boolean ={

    val hbaseTableName = TableName.valueOf(tableName)
    val connection = getConnection
    if (!connection.getAdmin.tableExists(hbaseTableName))
      createHbaseTable(hbaseTableName, List("info"), connection)
    val table = connection.getTable(hbaseTableName)
    if (existRowKey(data.row, table)) {
       return  false
    }
    val put = new Put(Bytes.toBytes(data.row))
    put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("ts"), Bytes.toBytes(data.value))
    table.put(put)
    true
  }

}