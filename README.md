# Render DB Montage Export

## Compile and package

Work on a platform with Maven installed, i.e. your local workstation.  Keep in mind that you need the render artifacts at fluid version `0.3.0-SNAPSHOT` (`1.0.0` when released) that are not present on a public repository yet, i.e. you need to compile and install them first:

```bash
cd ~/workspace
git clone https://github.com/saalfeldlab/render.git
cd ~/workspace/render
git checkout acac9847cf157e677bc79847b587d65e8676c6f6
mvn clean install
```

Then build the fat jar for Spark

```bash
cd ~/workspace/render-db-export
mvn clean package
```

## Run

Log into the cluster with the same paths available or copy the resulting jar and scripts to the appropriate location

```bash
ssh login1
cd ~/workspace/render-db-export
```
    
Edit one of the example launch scripts `run-example.sh` and set the number of nodes that you want to use (e.g. 20 or 40), and all other parameters that will be passed to the job.  Then

```bash
./run-example.sh
```

And follow how the job gets started and running using the preferred combination of qstat, log-file reading, or the web-interface (at the master node:8080).

