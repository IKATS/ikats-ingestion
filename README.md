# ![IKATS Logo](https://ikats.github.io/img/Logo-ikats-icon.png) IKATS Ingestion

![Docker Automated build](https://img.shields.io/docker/automated/ikats/ingestion.svg)
![Docker Build Status](https://img.shields.io/docker/build/ikats/ingestion.svg)
![MicroBadger Size](https://img.shields.io/microbadger/image-size/ikats/ingestion.svg)

This module provides features in order to ingest data in IKATS.  
The application is deployed on a Tommee server and runs independantly of IKATS datamodel.
It can be reached by two means:
1. an IKATS operator: [import TS](https://ikats.github.io/doc/operators/importTs.html)
2. or via HTTP API (see below).


<a id="http-api"></a>
## HTTP API 

* The ingestion tool's **HTTP API** manages some **ingestion sessions** as jobs.
* Currently, the tool can only run one **ingestion session** at the same time.
* Practical <a href="#use-cases">Use cases</a> are presented as user guidelines.

The API is available from the **URL base**: `http://host:port/ikats-ingestion/api`
where host is the IP of the application server host.

Services available from HTTP API are:
* <a href="#create-service">Create and start ingestion session</a>
* <a href="#restart-service">Restart a session</a>
* <a href="#list-service">Get the list of the sessions handled</a>
* <a href="#read-service">Get the session by ID</a>
* <a href="#stats-service">Get statistics about one session with ID</a>


<a id="create-service"></a>
## Create and start an ingestion session 

Request:
* __API resource__: `/sessions`  
* __HTTP Url__: `http://host:port/ikats-ingestion/api/sessions`
* __HTTP Verb__: `POST`  
* __HTTP Body__: provides the DATA: with the <a href="#json-session-desc">JSON describing the session</a>.

Nominal response: 
* header code HTTP `201`
* header *location* property, useful to grep the session `<ID>` <a id="session-id"></a> from the value.  
  Location example: `http://host:port/ikats-ingestion/api/sessions/<ID>`

Error response:
* header code HTTP: error code `500|400|...`
* body: HTML content for additional error information.

  See also <a href="#tuto-http-requests">how to submit HTTP requests</a>

**Ingestion session description** <a id="json-session-desc"></a>

The JSON data to be sent shall contain the following properties:

* **dataset** The name of the future dataset into IKATS database  
* **description** A description of that dataset for the end user
* **rootPath** The root path of the dataset on the import server where files are located  
  * Could be absolute, in that case, represent the path on the server
  * If relative, a configuration property will be used as prefix to the path (default: `/IKATSDATA`)
* **pathPattern** Regex pattern rules for defining tags and metric of dataset:<br>
  * The path is described with a regex
  * The root of the absolute path is the `rootPath`, and is not included in the pattern
  * The metric and tags should be matched into regex named groups. Metric and tags will be saved as Meta-data useful for querying timeseries in IKATS.
  * The pattern **must** define one **metric** with: `(?<metric>.*)`, it defines mandatory information (for OpenTSDB ) 
  * Each tag is defined with a regex group defined with: `(?<tagname>.*)`  
    Examples: patterns encoded in JSON string **(`\` needs to be doubled `\\`)**:
    > `\\/(?<equipement>\\w*)\\/(?<metric>.*?)_(?<validity>bad|good)\\.csv`
 
* **funcIdPattern** Pattern configuring how is generated the _Functional Identifier_. This substitution pattern defines sections `${metric}` or `${<tagname>}` refering to **pathPattern** groups, and replaced by respective group values matched by **pathPattern**. It follows Apache Commons Lang `StrSubstitutor` variable format, with any `<tagname>` or `metric` as variables names.  
  Examples:
  > `${equipement}_${metric}_${validity}`

* **importer** Fully Qualified Name of the java importer used to transfer the Time-Serie data to the IKATS dedicated database.
  * This is a *plugin definiton* adapted to a database. 
    The *default plugin* is in example below: applicable to **OpenTSDB** database.
  * **serializer** Set the Fully Qualified Name of the input serializer.
    * This is a *plugin definition* adapted to a specific file format. 
      The *default plugin* is in example below

**Example of the JSON document:** <a id="json-session-desc-ex"></a>

```json
  {
    "dataset": "Dataset_name",
    "description": "Dataset exemple",
    "rootPath": "data_dataset_example",
    "pathPattern": "\\/(?<equipement>\\w*)\\/(?<metric>.*?)_(?<validity>bad|good)\\.csv",
    "funcIdPattern": "${equipement}_${metric}_${validity}",
    "serializer": "fr.cs.ikats.datamanager.client.opentsdb.importer.CommonDataJsonIzer",
    "importer": "fr.cs.ikats.ingestion.process.opentsdb.OpenTsdbImportTaskFactory"
  }
```


<a id="restart-service"></a>
## Restart a session 

* __API resource__: `/sessions/{id}/restart`
* __HTTP URL__: `http://host:port/ikats-ingestion/api/sessions/{id}/restart`
  * Where `{id}` is the id of the session.
* __HTTP Verb__: `PUT`

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>


<a id="list-service"></a>
## Get sessions list 

**Warning:** Do not use it for a large dataset, the current output is the full data of the sessions !

* __API resource__: `/sessions`  
* __HTTP URL__: `http://host:port/ikats-ingestion/api/sessions`
* __HTTP Verb__: `GET`  

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>


<a id="read-service"></a>
## Get ingestion session 

Request:
* __API resource__: `/sessions/<ID>`  
* __HTTP Url__: `http://host:port/ikats-ingestion/api/sessions/x`
* __HTTP Verb__: `GET`
  
Response: the session JSON content

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>


<a id="stats-service"></a>
## Get statistics about an ingestion session 

Request:
* __API resource__: `/sessions/<ID>/stats`  
* __HTTP Url__: `http://host:port/ikats-ingestion/api/sessions/x/stats`
* __HTTP Verb__: `GET`
  
Response: the session statistics JSON content

See also <a href="#tuto-http-requests">how to submit HTTP requests</a>


<a id="use-cases"></a>
## Use case: launch new ingestion session <a id="use-case-new-session"></a>

1. <a href="#check-server-started">Make sure that the application server is started</a> or 
<a href="#start-server">start it</a>
2. <a href="#check-services-started">Make sure that the ingestion tool services are started</a> or start them from the Tomee admin page.
3. Prepare the imported folder:
  * IKATS convention: prepared under /var/lib/ikats/IKATSDATA/ filer, must be visible from <a href="#server-host">server host</a>
4. Prepare your new session JSON content in your favourite text editor (<a href="#json-session-desc">JSON describing the session</a>)
5. Submit the service <a href="#create-service">Create and start ingestion session</a>


## Use Case: start another ingestion while one is running

You can only run one ingestion at once! There are 2 workarounds:

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

<a id="tuto-http-requests"></a>
### How to submit HTTP requests 

#### From linux bash
Use the command *http* installed by [httpie](https://httpie.org/)
1. For a body with JSON content: edit the JSON content in a file `req.json` 

2. Launch the http command and read the response header and body

  Example:  
  `http POST http://host:port/ikats-ingestion/api/sessions < req.json`
  ```
  HTTP/1.1 201
  Content-Length: 0
  Date: Fri, 12 May 2018 07:02:50 GMT
  Location: http://host:port/ikats-ingestion/api/sessions/6
  Server: Apache TomEE
  ```

#### From firefox
1. Browse your `<HTTP URL>`: for a specific body or a verb different from GET, this does not yet work, and you have to go to **step 2** 
2. Open __Development tools__ and select the __Net__ frame
  1. From the browser: refresh the page
  2. Select the __Net__ tab of the development tool: view this request and edit it:
    * Select the __Header__ frame
    * and click on  __Modify and resend__: from that new panel, you can customize your HTTP request
       * edit the **HTTP Verb**: `PUT|POST|DELETE|GET`
       * add the json content in request: 
         1. complete the Request **Request Header** with property content-type `Content-Type: application/json`
         2. edit the Request **HTTP body**: `<JSON content>`
       * change the **HTTP URL** if needed.
       * `Submit`
    * Check the Response Header from the submitted requests: visible from the **Header**.  
      For example when required to retrieve the session ID: read the header response *location* property.


<a id="check-server-started"></a>
### Check the server status

Check that the server is started: browse the `http://host:port`: TomEE welcome page should be available.
  
For further informations, select `Server status` from the welcome page, and enter the well-known __admin__ login/password to reach __admin page__:   
* Follow the link `Lister les applications` 
* The __ingesting tool services__ should be visible at line having the path `/ikats-ingestion` <a id="ingestion-mgmt-online"></a>
  
  
### Start and stop the server

<a id="start-server"></a>
#### Start server

To start the application server you have to run from the <a href="#server-host">server host</a>

```bash
cd /home/ikats/ingestion # will be the directory where the ingestion database file will be stored
cd apache-tomee-plume-7.0.3
bin/./catalina.sh jpda start # to tell the server to start with JPDA remote capabilities activated
```
 
<a id="stop-server"></a>
#### Stop server 

To stop the server from the <a href="#server-host">server host</a> use:

```bash
/home/ikats/ingestion/apache-tomee-plume-7.0.3/bin/./shutdown.sh
```

