package com.premierScala.spark
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, lit, when,max,sum,desc,rank,asc}
object PremierScala {
  def main(args:Array[String]) : Unit ={
    calcul_top5_players
  }
  def calcul_top5_players() : Unit ={
    val spark = SparkSession.builder().appName("PremiereClass").master("local").getOrCreate()
    val co = spark.read.format("csv").
      option("delimiter",",").
      option("escape","\"").
      option("nullValue","NA").
      load("hdfs:///user/hdfsjo/brut.csv").
      toDF("ID","Name","Sex","Age","Height","Weight","Team","NOC","Games","Year","Season","City","Sport","Event","Medal")
    val theLastYear = co.groupBy().agg(max("Year")).first().getString(0)
    val alldata = co.select( "Year","ID","Name", "SEX", "Medal").
      withColumn("lastYear", lit(theLastYear)).
      where("year = lastYear and Medal is not null").
      withColumn("nbGold", when(col("Medal")=== "Gold",1)).
      withColumn("nbSilver", when(col("Medal")=== "Silver",1)).
      withColumn("nbBronze", when(col("Medal")=== "Bronze",1)).
      groupBy( "Year","ID","Name", "SEX").
      agg(sum("nbGold").alias("sumGold"), sum("nbSilver").alias("sumSilver"),sum("nbBronze").alias("sumBronze")).
      withColumn("e", rank().over(Window.partitionBy("SEX").orderBy("sumGold","sumSilver","sumBronze")))
    alldata.createOrReplaceTempView("table_all_data")
    val query_latest_rec = """SELECT * FROM table_all_data ORDER BY e DESC limit 5"""
    spark.sql(query_latest_rec).show()
  }
}
