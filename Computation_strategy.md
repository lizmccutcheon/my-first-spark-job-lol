# Strategy and Explanation
## Strategy for computation of new_brazil_covid19.csv
1. Create a schema to control the types for data load, particularly to ensure all numerical data is Double and not String.
2. Load the .csv into a dataframe.
3. Drop "name" and "code" fields as they will not be used for anything.
4. Do a GROUP BY on "date" and "state" fields, summing the "deaths" and "cases".
5. Look up the "region" from the "state". This is purely cosmetic, but will allow us to use the same schema for both files when importing them for the diff report. Use a left outer join to avoid dropping any data from the data frame for rows where the "state" data is null/bad.
6. Sort by "date", "region", and "state". Again purely cosmetic, but helps for readability of output file.
7. Write the final dataframe out to .csv, using `.coalesce(1)` to specify that the output should be in a single file.

## Strategy for comparison of brazil_covid_19.csv and new_brazil_covid19.csv
1. Create a schema to control the data types for the two files on import.
    - ensures both dataframes have the same types
    - ensures all numerical data is Double type
2. Load both files into dataframes
3. Create a key in each data frame by concatenating the "date" and "state" fields.
4. Rename all fields in each data frame by adding a suffix to each :
    - "_old" for dataframe containing `brazil_covid19.csv` data
    - "_new" for dataframe containing `new_brazil_covid19.csv` data
5. Join the two dataframes on their key columns to create a comparison data frame. Use a full outer join to ensure that all source and destination rows are retained.
6. Using the original data frames, do calculations to be used in diff report : 
    - number of rows in source/old dataframe (do a count)
    - number of rows in dest/new dataframe (do a count)
7. Using the comparison dataframe, do calculations to be used in the diff report : 
    - number of rows where the key is present in the source but not the destination (filter dest key null/empty string, then do a count)
    - number of rows where the key is present in the destination but not the source (filter source key null/empty string, then do a count)
    - number of rows which are an exact match i.e. cases/deaths same for a given key (filter source cases = dest cases AND source deaths = dest deaths, then do a count) 
    - number of rows which are not an exact match i.e. cases and/or deaths vary for a given key (filter source cases != dest cases OR source deaths != dest deaths, then do a count)
8. Put the 6 metrics calculated above into a list, then transform the list into a dataframe with the `.toDF` method, providing the metric names for the report as arguments.
9. Write the dataframe to .json using `.write.json`. No need to use `.coalesc(1)` as there will only be one line in the report anyway.
    
## Explanation of report output  
Discrepancies between the source (`brazil_covid19.csv`) and destination (`new_brazil_covid19.csv`) files :
1. Source file contains 12258 rows while destination contains 11421 rows
    - This is a difference of 837 rows.
    - This is expected as the date ranges for two files is not the same: 25 Feb 2020 to 23 May 2021 for source file and 27 March 2020 to 23 May 2021.
    - 31 extra days in source file multiplied by 27 states in brazil results in 837 extra records in the source file, entirely accounting for the difference.
2. 837 rows are present in the source but not the destination
    - As noted above, we expect exactly this number of extra rows in the source.
3. 4037 rows are not an exact match (i.e. around 35% of rows)
    - This is not terribly surprising, as the destination file is generated from the sum of cases/deaths reported by every city in a state for each date.
    - An exact match therefore relies on every city in a state reporting their daily cases/deaths :
        - with 100% accuracy  
        - in a timely manner
        - actually reporting them at all
    - It is not clear from the metadata on Kaggle whether the data has been compiled on a daily basis or some time later after missing or incorrect data was backfilled/corrected. If the former, this would explain the discrepancies.
    - In light of the above considerations, and with experience of working in a government health department during covid, I'm quite impressed the mismatches are only 35%.
4. In general, the data is unaturally clean and consistant thanks to the nice folks at Kaggle. In real-life, I would expect to spend a lot more time cleaning and filtering out rubbish data to achieve these results.



