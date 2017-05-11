# How-To use IKATS Ingestion tool

## Quick summary

 * It is required, per cluster, to have installed and started the <a href="#tomee-application-server">Tomee application server</a>, 
hosting the <a href="#ingestion-tool-services">ingestion tool services</a>: usually this requirement is already achieved.
 * There is no GUI/HMI, the tool is invoked using the <a href="#http-api">HTTP API</a> of the tool.
 * The ingestion tool's **HTTP API** manages some **ingestion sessions** as jobs.
 * Currently, the tool can only run one **ingestion session** at the same time.
 * To stop a running **ingestion session**, you could hardly <a href="#stop-server">stop the application server</a>, 
 or soflty stop the <a href="#ingestion-tool-services">the ingestion tool services</a>
 * To restart an **ingestion session**, use HTTP API: <a href="#restart-session">restart a session</a>.
 * Practical <a href="#use-cases">Use cases</a> are presented as user guidelines.

## Use cases <a id="use-cases"></a>
### Use case: launch new ingestion session <a id="use-case-new-session"></a>
1. <a href="#check-server-started">Make sure that the application server is started</a> or 
<a href="#start-server">start it</a>
2. <a href="#check-services-started">Make sure that the ingestion tool services are started</a> or start them from the Tomee admin page.
3. Prepare the imported folder:
    * IKATS convention: prepared under /IKATSDATA/ filer, must be visible from <a href="#server-host">server host</a>
4. Prepare your new session JSON content in your favourite text editor
    * Start from the <a href="#json-session-desc-ex">example</a> of the <a href="#json-session-desc">JSON session description</a>
         * refer to the documented examples for **Airbus** or **EDF** ingestions.
         * *Keep the default plugins*: for **Airbus**, **EDF** leave the fields *serializer* and *importer* unchanged
5. Submit the service <a href="#create-service">Create and start ingestion session</a>

### Use Case: start another ingestion while one is running

We can only run one ingestion at once! There are 2 workarounds:

 * **Solution 1**: starting *another* application server on *another* node
     * required: install the application server on a new node of the same cluster: **not straightforward !!**
     * And follow the use case  <a href="#use-case-new-session">launch new ingest session</a> from the **new node**.
 * **Solution 2**: stopping the running session, launching the second one,  and finally restarting the stopped session.
     1. Stop the services from the  <a href="#ingestion-tool-services">Tomee admin page</a> 
     or more drastically <a href="#stop-server">stop</a> and <a href="#start-server">start</a> the Tomee server.
     2. follow the use case  <a href="#use-case-new-session">launch new ingest session</a>
         * Submit the <a href="#stats-service">stats service</a> to follow the new session by its <a href="#session-id">ID</a>.
     3. Once the session is finished, restart the stopped session, with **stopped ID**.


## Ingestion tool services

### Manage the ingestion tool services <a id="ingestion-tool-services"></a>
Firstly make sure that <a href="#tomee-application-server">Tomee server</a> is installed and started.

The services are provided by the application __ikats-ingestion__  running on the Tomee server.

You can manage the services from the **Tomee admin page**, at line __ikats-ingestion__ as described in <a href="#ingestion-mgmt-online">Check the server status</a>
  * `Démarrage`: is inactive when services are started <a id="check-services-started"></a>
  * `Arrêt`: is inactive when services are not started

### HTTP API 
<a id="http-api"></a>

The API is available from the **URL base**: http://172.28.15.xx:8181/ikats-ingestion/api
where 172.28.15.xx is the IP of the <a href="#server-host">application server host</a> ).

Example: URL base for PREPROD is http://172.28.15.84:8181/ikats-ingestion/api

Services available from HTTP API are:
  * <a href="#create-service">Create and start ingestion session</a>
  * <a href="#restart-service">Restart a session</a>
  * <a href="#list-service">Get the list of the sessions handled</a>
  * <a href="#read-service">Get the session by ID</a>
  * <a href="#stats-service">Get statistics about one session with ID</a>

---

#### Create and start an ingestion session 
<a id="create-service"></a>

Request:
  * __API resource__: `/sessions`  
  * __HTTP Url__: http://172.28.15.xx:8181/ikats-ingestion/api/sessions
  * __HTTP Verb__: `POST`  
  * __HTTP Body__: provides the DATA: with the <a href="#json-session-desc">JSON describing the session</a>.

Response: the URL of the launched session: this is useful to grep its `<ID>` <a id="session-id"></a>: 
  * `http://172.28.15.xx:8181/ikats-ingestion/api/sessions/<ID>`
   
See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

**Ingestion session description** <a id="json-session-desc"></a>

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
   Examples:
	 * For EDF:
	 `\/DAR\/(?<equipement>\w*)\/(?<metric>.*?)(?:_(?<validity>bad|good))?\.csv`
	 * For Airbus:
	 `\/DAR\/(?<AircraftIdentifier>\w*)\/(?<metric>.*?)/raw_(?<FlightIdentifier>.*)\.csv`
 * **funcIdPattern** Pattern configuring how is generated the _Functional Identifier_. This substitution pattern defines
   sections `${metric}` or `${<tagname>}` refering to  **pathPattern** groups, 
   and replaced by respective group values matched by **pathPattern**.
     * It follows Apache Commons Lang `StrSubstitutor` variable format, with any <tagname> or 'metric' as variables names.  
   Examples:
    * For EDF : `${equipement}_${metric}_${validity}`
	* For Airbus : `${AircraftIdentifier}_${FlightIdentifier}_${metric}`
 * **importer** Fully Qualified Name of the java importer used to transfer the Time-Serie data to the IKATS dedicated database.
   * This is a *plugin definiton* adapted to a database. 
     The *default plugin* is in example below: applicable to **OpenTSDB** database.
 * **serializer** Set the Fully Qualified Name of the input serializer.
   * This is a *plugin definition* adapted to a specific file format. 
     The *default plugin* is in example below: applicable to **EDF** or **Airbus** CSV files.

**Example of the JSON document:** <a id="json-session-desc-ex"></a>

	{
		"dataset": "DS_AIRBUS_26",
		"description": "Dataset AIB pour correlation avec 26 parametres",
		"rootPath": "DS_AIRBUS_PART_1_DONE",
		"pathPattern": "\\/DAR\\/(?<AircraftId>\\w*)\\/(?<metric>.*?)/raw_(?<FlightId>.*)\\.csv",
		"funcIdPattern": "T0511_${FlightId}_${metric}",
		"serializer": "fr.cs.ikats.datamanager.client.opentsdb.importer.CommonDataJsonIzer",
		"importer": "fr.cs.ikats.ingestion.process.opentsdb.OpenTsdbImportTaskFactory"
	}

---

#### Restart a session 
<a id="restart-service"></a>

  * __API resource__: `/sessions/{id}/restart`
  * __HTTP URL__: `http://172.28.15.xx:8181/ikats-ingestion/api/sessions/{id}/restart`
    * Where `{id}` is the id of the session.
  * __HTTP Verb__: `PUT`

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

---

#### Get sessions list 
<a id="list-service"></a>

**Warning:** Do not use it for a large dataset, the current output is the full data of the sessions !

  * __API resource__: `/sessions`  
  * __HTTP URL__: `http://172.28.15.xx:8181/ikats-ingestion/api/sessions`
  * __HTTP Verb__: `GET`  

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

---

#### Get ingestion session 
<a id="read-service"></a>

Request:
  * __API resource__: `/sessions/<ID>`  
  * __HTTP Url__: http://172.28.15.xx:8181/ikats-ingestion/api/sessions/x
  * __HTTP Verb__: `GET`
  
Response: the session JSON content
   
See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

---

#### Get statistics about an ingestion session 
<a id="stats-service"></a>

Request:
  * __API resource__: `/sessions/<ID>/stats`  
  * __HTTP Url__: http://172.28.15.xx:8181/ikats-ingestion/api/sessions/x/stats
  * __HTTP Verb__: `GET`
  
Response: the session statistics JSON content
   
See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

---

### How to submit HTTP requests 
<a id="tuto-http-requests"></a>

#### From firefox
1. Browse your `<HTTP URL>`: for specific body or verb different from GET, this does not yet work, and you have to go to **step 2** 
2. Open __Development tools__ and select the __Net__ frame
   1. From the browser: refresh the page
   2. From the __Net__ view of the development tool: retrieve this request and edit it:
     * Select the __Header__ frame,
     * And click on  __Modify and resend__: from that new panel, you can customize your HTTP request
        * edit the **HTTP Verb**: `PUT|POST|DELETE|GET`
        * edit the Request **HTTP body**: this is the good place to paste your `<JSON content>`
        * change the **HTTP URL** if needed.
        
#### From linux bash
Use the command http installed by [httpie](https://httpie.org/)
  

## The Tomee application server 
<a id="tomee-application-server"></a>

### Install requirements 
The application server is [Apache Tomee](http://tomee.apache.org/download-ng.html). 

 * __Host__: for each IKATS cluster: fourth node<a id="server-host"></a>
   * server IP for cluster **PREPROD**: 172.28.15.**89**
   * server IP for cluster **INT**: 172.28.15.**84**
   * etc.
 * __Installation version__: version with "plume" flavor version 7.0.3
 * __Intallation path__: ```/home/ikats/ingestion/apache-tomee-plume-7.0.3```
 * __Configuration__ is provided into the git repo ```ikats_tools/ingestion/tomee-conf```  
   Configuration provides:
    * a JMX access
    * a remote debug (see ```JPDA_PORT``` in ```bin/setenv.sh```)

### Check the server status
<a id="check-server-started"></a>
Check that the server is started: browse the `http://172.28.15.xx`: TomEE welcome page should be available.
  
For further informations, select `Server status` from the welcome page, and enter the well-known __admin__ login/password to reach __admin page__:   
  * Follow the link `Lister les applications` 
  * The __ingesting tool services__ should be visible at line having the path `/ikats-ingestion` <a id="ingestion-mgmt-online"></a>
  
  
### Start and stop the server

#### Start server
<a id="start-server"></a>

To start the application server you have to run from the <a href="#server-host">server host</a>

```bash
cd /home/ingestion # will be the directory where the ingestion database file will be stored  
bin/./catalina.sh jpda start # to tell the server to start with JPDA remote capabilities activated
```
 
#### Stop server 
<a id="stop-server"></a>

To stop the server from the <a href="#server-host">server host</a> use:

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
