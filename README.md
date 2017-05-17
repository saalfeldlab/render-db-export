# Render DB Montage Export

## Compile and package

On a platform with Maven installed, i.e. your local workstation.  Keep in mind that you need the render artifacts that are not present on a public repository, i.e. need to be compiled and installed first:

```bash
cd ~/workspace/render
mvn clean install
```

Then the fat jar for Spark

```bash
cd ~/workspace/render-db-export
mvn clean package
```

## Run

Log into the cluster with the same paths available or copy the resulting jar and scripts to the appropriate location

```bash
ssh login1
cd ~/workspace/render-align
```
    
Edit one of the example launch scripts `run-example.sh` and set the number of nodes that you want to use (e.g. 20 or 40), and all other parameters that will be passed to the job.  Then

```bash
./run-example.sh
```

And follow how the job gets started and running using the preferred combination of qstat, log-file reading, or the web-interface (at the master node:8080).

