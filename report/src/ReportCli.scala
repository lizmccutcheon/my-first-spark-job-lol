

object ReportCli {

    def main(args: Array[String]) = args match {
        case Array("import", input_csv_path, output_csv_path) => SparkContext.runWithSpark(Covid19citiesImporter(_).importCovid19Cities(input_csv_path, output_csv_path))
        case Array("diff", filepath_old, filepath_new, output_path) => SparkContext.runWithSpark(DiffReport(_).createDiffReport(filepath_old, filepath_new, output_path))
        case _ => 
            println(s"command '${args.mkString(" ")}' not recognized")
            println("usage:\n\r\t./mill report.run import <path_to_input_csv> <path_to_output_csv>\n\n\t./mill report.run diff <path_to_source_csv> <path_to_destination_csv> <path_to_diff_report>")
    }
    
}