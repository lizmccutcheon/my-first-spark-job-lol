# my-first-spark-job-lol

## About
Basic diff report written in Spark, consisting of 2 jobs : 
1.  `import`, which reads `brazil_covid19_cities.csv`, transforms the data and writes out to a new file `new_brazil_covid19.csv`.
2.  `diff`, which generates a diff report which compares files `brazil_covid19.csv` (the "source" file) and `new_brazil_covid19.csv` (the "destination" file, generated using `import` method).  

Outputs are :  
1. `import`, a new .csv file (`new_brazil_covid19.csv`)
2. `diff`, a single line json report containing a summary of the differences between the source and destination files provided. 

## Acknowledgements
Mill setup and `ReportCli.scala` borrowed from [https://github.com/jlcanela/basicreport](https://github.com/jlcanela/basicreport).
  
This work was originally submitted as part of the assessment for Data Pipeline 2 course, part of the MSc in Data Engineering for AI at Data ScienceTech Institute.

## Source data
[Kaggle - Coronavirus Brazil](https://www.kaggle.com/datasets/unanimad/corona-virus-brazil?select=brazil_covid19_cities.csv)

## Output files
These have been provided in [results](https://github.com/lizmccutcheon/my-first-spark-job-lol/tree/main/results) :
- `new_brazil_covid19.csv`, as generated by the `import` job
- `diff_report.json`, as generated by the `diff` job

## Instructions
### Prepare the environment
- Clone the repo and cd into working directory :
`git clone git@github.com:lizmccutcheon/my-first-spark-job-lol.git`  
`cd my-first-spark-job-lol`
- Extract the source data from `archive.zip` :  
`unzip archive.zip brazil_covid19_cities.csv brazil_covid19.csv -d source_data/`

### Run the project locally using mill
- As specified in `build.sc`, use Spark version 3.1.2 and Scala version 2.12.15.
- To generate `new_brazil_covid19.csv` :   
    - run `./mill report.standalone.run import source_data/brazil_covid19_cities.csv data/new_brazil_covid19`
- To generate the diff report :  
    - move or copy .csv file output from the previous step into `source_data` directory and rename the file to `new_brazil_covid19.csv`
    - run `./mill report.standalone.run diff source_data/brazil_covid19.csv source_data/new_brazil_covid19/ data/diffreport`  

### Run the project locally using spark-submit 
- Package the jar file :  
`./mill report.assembly`
- To generate `new_brazil_covid19.csv` :
    - Run : `spark-submit --class ReportCli out/report/assembly/dest/out.jar import source_data/brazil_covid19_cities.csv data/new_brazil_covid19`
- To generate the diff report :
    - move or copy .csv output from the previous step into `source_data` directory and rename the file to `new_brazil_covid19.csv`
    -`spark-submit --class ReportCli out/report/assembly/dest/out.jar diff source_data/brazil_covid19.csv source_data/new_brazil_covid19.csv data/diff_report`  

### Deploy and run on AWS
Refer to instructions [here](https://github.com/lizmccutcheon/my-first-spark-job-lol/blob/main/AWS_instructions.md)

## Compute and comparison strategy
See brief discussion [here](https://github.com/lizmccutcheon/my-first-spark-job-lol/blob/main/Computation_strategy.md)

## Author
Liz McCutcheon  
November 2022
