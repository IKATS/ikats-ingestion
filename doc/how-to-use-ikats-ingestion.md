# How-To use IKATS Ingestion tool

Remarks:

 * Currently, the tool could only run one session at time
 * There is no GUI/HMI, the tool is invoked using an HTTP API
 * To stop a running ingestion, you should stop the application server (Tomee)
 * To restart an ingestion session, there is an API call available

## HTTP API Usage

The API is available at the URL : http://172.28.15.xx:8181/ikats-ingestion/api

### Create and start an ingest session

API resource: `/sessions`  
HTTP Verb: `POST`  
Data: JSON which describe the ingest session. See format in next paragraph

#### Ingest session description

The JSON data to be sent shall contain the following properties:

 * **dataset** The name of the dataset into IKATS database  
 * **description** A description of that dataset for the end user
 * **rootPath** The root path of the dataset on the import server where files are located  
   * Could be absolute, in that case, represent the path on the server
   * If relative, a configuration property will be used as prefix to the path (default: `/IKATSDATA`)
 * **pathPattern** Pattern rules for defining tags and metric of dataset:<br>
   * The path is described with a regex
   * The root of the absolute path is the `rootPath`, and is not included in the pattern
   * The metric and tags should be matched into regex named groups
   * The regex **should have one metric** group defined with: `(?<metric>.*)`
   * Each tag is defined with a regex group defined with: `(?<tagname>.*)`  
   Examples :
	 * For EDF : `\/DAR\/(?<equipement>\w*)\/(?<metric>.*?)(?:_(?<validity>bad|good))?\.csv`
	 * For Airbus : `\/DAR\/(?<AircraftIdentifier>\w*)\/(?<metric>.*?)/raw_(?<FlightIdentifier>.*)\.csv`
 * **funcIdPattern** Pattern for the Functional Identifier.  
   Follow Apache Commons Lang `StrSubstitutor` variable format, with tags names / 'metric' as variables names.  
   Examples :
    * For EDF : `${equipement}_${metric}_${validity}`
	* For Airbus : `${AircraftIdentifier}_${FlightIdentifier}_${metric}`
 * **importer** Fully Qualified Name of the java importer used to transfer the Time-Serie data to the IKATS dedicated database.
 * **serializer** Set the Fully Qualified Name of the input serializer  

**Example of the JSON document:**  

	{
		"dataset": "DS_AIRBUS_26",
		"description": "Dataset AIB pour correlation avec 26 parametres",
		"rootPath": "DS_AIRBUS_PART_1_DONE",
		"pathPattern": "\\/DAR\\/(?<AircraftId>\\w*)\\/(?<metric>.*?)/raw_(?<FlightId>.*)\\.csv",
		"funcIdPattern": "T0511_${FlightId}_${metric}",
		"serializer": "fr.cs.ikats.datamanager.client.opentsdb.importer.CommonDataJsonIzer",
		"importer": "fr.cs.ikats.ingestion.process.opentsdb.OpenTsdbImportTaskFactory"
	}

### Restart a session

API resource: `/sessions/{id}/restart`  
HTTP Verb: `PUT`

Where `{id}` is the id of the session

### Get sessions list

API resource: `/sessions`  
HTTP Verb: `GET`  

**Warning:** Do not use it for a large dataset, the current output is the full data of the sessions

## Use case: Launch an ingest session

TBD

## Use Case: start another ingestion while one is running

We could only run one ingestion at time. There could we 2 workarounds :

 1. Start another application server on another node
 * Stop the session and launch the second one

How to achieve the later: simply [shutdown the server, then restart](#start-stop-server) it, the first session is in STOPPED state. You could launch another one.  

## Usage of the Tomee application server

### Information
The application server is [Apache Tomee](http://tomee.apache.org/download-ng.html) and is installed on the fourth nodes in the IKATS clusters with "plume" flavor version 7.0.3

 * Intallation path : ```/home/ikats/ingestion/apache-tomee-plume-7.0.3```
 * Configuration is provided into the git repo ```ikats_tools/ingestion/tomee-conf```  
   Configuration provides:
    * a JMX access
    * a remote debug (see ```JPDA_PORT``` in ```bin/setenv.sh```)
     
### Start and stop the server <a name="#start-stop-server"></a>

To start the application server you have run:

```bash
cd /home/ingestion # will be the directory where the ingestion database file will be stored  
bin/./catalina.sh jpda start # to tell the server to start with JPDA remote capabilities activated
```
 
To stop the server use :

```bash
/home/ikats/ingestion/apache-tomee-plume-7.0.2/bin/./shutdown.sh
```

## IKATS Ingestion Maven deployement

### Prerequisites

IKATS Ingestion uses other java parts from the ```ikats-base``` git repository (see dependencies in the pom.xml).
Make sure that the corresponding artifacts are build and installed in the local Maven repository.

In order to run compile and install the components to be packaged with the ingestion tool run:

```bash
cd ikats-base/ikats-main
mvn clean install
```
or 
```bash
mvn -DskipTests clean install
```
### Maven profiles and "target"

TBD


