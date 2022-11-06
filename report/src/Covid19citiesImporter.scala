import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._ 

case class Covid19citiesImporter(spark: SparkSession){
    
    import spark.implicits._

    // Make a dodgy dataframe to look up region from state. Completely unnecessary for analysis but the
    // files will look the same, and we then can use the same schema for import when generating the diff report
    val sc = spark.sparkContext
    val lookupRegion = sc.parallelize(Seq(
        ("DF","Centro-Oeste"), ("GO","Centro-Oeste"), ("MS","Centro-Oeste"),
        ("MT","Centro-Oeste"), ("AL","Nordeste"), ("BA","Nordeste"),
        ("CE","Nordeste"), ("MA","Nordeste"), ("PB","Nordeste"),
        ("PE","Nordeste"), ("PI","Nordeste"), ("RN","Nordeste"),
        ("SE","Nordeste"), ("AC","Norte"), ("AM","Norte"),
        ("AP","Norte"), ("PA","Norte"), ("RO","Norte"),
        ("RR","Norte"), ("TO","Norte"), ("ES","Sudeste"),
        ("MG","Sudeste"), ("RJ","Sudeste"), ("SP","Sudeste"),
        ("PR","Sul"), ("RS","Sul"), ("SC","Sul")
    )).toDF("state_new","region")

    // function to do the region lookup. Use leftouter join to preserve any rows missing the State
    def addRegionColumn(df_main:DataFrame, df_lookup:DataFrame) = {
        df_main.join(df_lookup,df_main("state") === df_lookup("state_new"), "leftouter")
    }

    def importCovid19Cities(input_csv_path:String, output_csv_path:String) = {
        // set up a schema so the types can be controlled on import
        val schema = new StructType()
            .add("date", StringType, true)
            .add("state", StringType, true)
            .add("name", StringType, true)
            .add("code", StringType, true)
            .add("cases", DoubleType, true)
            .add("deaths", DoubleType, true)
        
        // load brazil_covid19_cities.csv into a dataframe
        val df = spark.read
                    .format("csv")
                    .option("header", "true") 
                    .schema(schema)
                    .load(input_csv_path)

        // drop these useless fields
        val df_1 = df.drop("name","code")

        // group by date and state, summing cases and deaths
        val df_2 = df_1.groupBy("date","state")
                    .agg(sum("cases").as("cases"),sum("deaths").as("deaths"))

        // look up the Region from the State
        val df_3 = addRegionColumn(df_2,lookupRegion)
                    .drop("state_new")

        // re-order the fields and sort by date, region, state
        val df_4 = df_3.select("date","region","state","cases","deaths")
                    .orderBy("date","region","state")
        
        // write out dataframe in csv format
        df_4.coalesce(1) // to a single file
            .write.format("csv")
            .option("header", "true") 
            .save(output_csv_path)
        
    }
}
