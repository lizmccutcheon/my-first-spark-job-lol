import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._ 

case class DiffReport(spark: SparkSession){
    import spark.implicits._

    // function to import the data files
    def importCovid19Data(input_path:String) = {
        // schema to control data types
        val schema = new StructType()
            .add("date", StringType, true)
            .add("region", StringType, true)
            .add("state", StringType, true)
            .add("cases", DoubleType, true)
            .add("deaths", DoubleType, true)
        
        val df = spark.read
                    .format("csv")
                    .option("header", "true") 
                    .schema(schema)
                    .load(input_path)
        df
    }

    // function to create key by concatenating "date" and "state" fields
    def createKeyInDataFrame(df:DataFrame) = {
        val df_with_key = df.withColumn("key",
            concat(col("date"),lit("_"),col("state"))
        )
        df_with_key
    }

    // function to suffix all fields with "source" or "dest"
    def addColumnSuffix(df:DataFrame, suffix:String) = {
        val df_with_suffix = df.withColumnRenamed("date","date_" +suffix)
                    .withColumnRenamed("region","region_" +suffix)
                    .withColumnRenamed("state","state_" +suffix)
                    .withColumnRenamed("cases","cases_" +suffix)
                    .withColumnRenamed("deaths","deaths_" +suffix)
                    .withColumnRenamed("key","key_" +suffix) 
                    // this is silly method but it does the job
        df_with_suffix
    }

    // function to create a comparison dataframe containing data from
    // both dataframes joined on their common key
    def createComparisonDF(df_old:DataFrame, df_new:DataFrame) = {
        val data_old = addColumnSuffix(createKeyInDataFrame(df_old),"old")
        val data_new = addColumnSuffix(createKeyInDataFrame(df_new),"new")

        val data_outer_joined = data_old.join(data_new,
            data_old("key_old") === data_new("key_new"),
            "fullouter" // full outer join to preserve all data
        )
        data_outer_joined
    }

    def createDiffReport(filepath_old:String, filepath_new:String, output_path:String)= {
        val df_old = importCovid19Data(filepath_old)
        val df_new = importCovid19Data(filepath_new)

        // number of rows in source (old)
        val number_rows_old = df_old.count()

        // number of rows in destination (new)
        val number_rows_new = df_new.count()

        val comparisonDF = createComparisonDF(df_old, df_new)

        // number of rows in source but not destination
        val num_old_not_new = comparisonDF
            .filter("key_new is null or key_new ==''")
            .count()

        // number of rows in destination but not source   
        val num_new_not_old = comparisonDF
            .filter("key_old is null or key_old ==''")
            .count()

        // number of rows with exact data match between source and destination
        val num_matches = comparisonDF
            .filter("key_old == key_new and " +
              "cases_old == cases_new and " +
              "deaths_old == deaths_new")
            .count()

        // number of mismatched rows (keys match but case/death data different)
        val num_mismatches = comparisonDF
            .filter("key_old == key_new and " +
              "(cases_old != cases_new) or (deaths_old !=deaths_new)")
            .count()

        
        // build the json report from the stats calculated above
        val df_diff = List((number_rows_old, number_rows_new, num_old_not_new,
            num_new_not_old,num_matches,num_mismatches))
                .toDF("source rows - total","dest rows - total","source rows not in dest",
                    "dest rows not in source", "matches", "mismatches")

        df_diff.write.json(output_path)

    }




}
