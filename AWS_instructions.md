# How to deploy to AWS and run with spark-submit
Dumb manual processs - would be better with spark-steps (todo).  
  
S3 is used to read/write data and to provide the compiled .jar. A cluster is created with Elastic Map Reduce, then SSH into the master node to issue the spark-submit commands.    

## Step 1 - create a bucket
1. Log into your AWS account.
2. Go to [Amazon S3](https://s3.console.aws.amazon.com/s3/) and create yourself a bucket.
3. Create folders `data`, `output`, `jar` and (optionally) `logs`.
4. From the `archive.zip`, upload `brazil_covid19_cities.csv` and `brazil_covid19.csv` into the `data` folder just created.

#### Step 2 - set up a cluster
1. Go to [AWS Elastic Map Reduce](https://eu-west-3.console.aws.amazon.com/elasticmapreduce/) and click on `Create Cluster`
2. During the setup, ensure that the following selections are made:
    - Under "Software setup", select `Spark 2.4.8 on HadoopSpark: Spark 2.4.8 on Hadoop 2.10.1 YARN and Zeppelin 0.10.0` and leave the release as `emr-5.36.0`
    - Under `Security and Access`, ensure you have selected an EC2 key pair (create one if needed). This is necessary to be able to SSH into the master node later on.
    - (Optional) Under `General Configuration` the logs output path can be chaged to the logs folder previously created; logging can also be turned off if desired.
3. All other settings can be left as default
4. Click on `Create Cluster` then go and make a cup of coffee while it spins up.  

#### Step 3 - add permissions to be able to run the job successfully
1. Once the cluster is created, navigate through to the master node EC2 instance (hardware tab, then click through until you reach the EC2 instance summary)
2. Click on `Security` tab and edit the inbound rules.
3. Add a new rule to allow inbound SSH traffic (port 22) from 0.0.0.0/0 (or from just your IP address if preferred). 
4. Back on the instance summary page, click on the `IAM Role`.
5. Under `Permissions policies`, add `AmazonS3FullAccess` to the IAM role. Alternatively, if preferred you can set permissions to allow read/write access only to the bucket created earlier.

#### Step 4 - package the Spark application and load to AWS
1. In the (local) repo, open `build.sc` and set the SPARK_VERSION and scalaVersion variables to be compatible with the AWS cluster.
2. Assuming the instructions above have been followed exactly, these will be :
    - SPARK_VERSION = "2.4.8"
    - scalaVersion = "2.11.12"  
(both provided as comments in `build.sc`).
3. After making the changes to `build.sc`, recompile the jar by running `./mill report.assembly`.
4. The .jar file will be located at `out/report/assembly/dest/out.jar`
5. Upload this new jar file to the `jar` folder in your bucket. 

#### Step 5 - ssh into the master node
1. Locate the master node's public DNS; this is found on the cluster's configuration page, under `Summary`.
2. SSH into `hadoop@{your-master-node-public-dns}`, using the private key from the key pair specified during the cluster setup.
3. Accept the security warning that will probably pop up.
4. You should now be logged in to Amazon EMR - you will see some funky EMR ascii-art in the shell. The commands below can now be be run from the command line on the master node.

#### Step 6 - run the commands
1. Run the first job (data import and transform) :
`spark-submit --class ReportCli s3://{your-bucket-name}/jar/out.jar import s3://{your-bucket-name}/data/brazil_covid19_cities.csv s3://{your-bucket-name}/output/new_brazil_covid19`
2. Once successfully run, the resulting .csv file will be found in the `output/new_brazil_covid_19` folder of your bucket.
3. Copy or move the output .csv file to the `data` folder of the bucket and rename it to `new_brazil_covid19.csv`. It important to rename, as spark-submit fails if the default name jammed full of hypens is passed in. 
4. Run the diff report : `spark-submit --class ReportCli s3://{your-bucket-name}/jar/out.jar diff s3://{your-bucket-name}/data/brazil_covid19.csv s3://{your-bucket-name}/data/new_brazil_covid19.csv  s3://{your-bucket-name}/output/diff`
5. Once successfully run, the results will appear in `output/diff` folder in your bucket.
6. Remember to terminate the cluster once you've finished :)