properties([
  parameters([
    choice(name: 'CLUSTER', choices: "INT-B\nINT\nPREPROD", description: 'Target cluster'),
    string(name: 'BRANCH_TO_USE', defaultValue: 'master', description: 'Branch to use for ikats' ),
  ])
])

// Changing IP according to target
targetName=""
targetIP=""

credentials = 'dccb5beb-b71f-4646-bf5d-837b243c0f87'

node{
  echo "\u27A1 Deploying "+params.BRANCH_TO_USE+" on "+params.CLUSTER

  currentBuild.result = "SUCCESS"

  try {

    stage('clean') {
      echo "\u27A1 Cleaning"
      deleteDir()

      // Building the target name
      switch (params.CLUSTER){
        case "PREPROD":
          targetName="preprod"
          targetIP="172.28.15.89"
          break
        case "INT":
          targetName="int"
          targetIP="172.28.15.84"
          break
        case "INT-B":
          targetName="int-b"
          targetIP="172.28.15.14"
          break
        default:
          throw new Exception("Wrong cluster: "+params.CLUSTER)
      }
    }

    stage('Start Tomee') {
      echo "\u27A1 Starting Tomee"
      TOMEE_UP = sh (
        script: "nc -z ${targetIP} 8181 > /dev/null",
        returnStatus: true
      ) == 0
      if (TOMEE_UP) {
        echo 'Tomee already started'
      }
      else {
        echo 'Tomee is not started. Please start it using `/home/ikats/ingestion/tomee/bin/startup.sh` on the corresponding node'
        throw new Exception()
      }
    }

    stage('Build ikats-base') {
      echo "\u27A1 Pulling Ikats Java code"

      dir('ikats-base') {

        git url: "https://thor.si.c-s.fr/git/ikats-base", branch: "master", credentialsId: credentials

        dir ('ikats-main'){
          withMaven(
            // Maven installation declared in the Jenkins "Global Tool Configuration"
            maven: 'Maven 3.3.9',
            // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
            // Maven settings and global settings can also be defined in Jenkins Global Tools Configuration
            //mavenSettingsConfig: 'my-maven-settings',
            mavenLocalRepo: "${WORKSPACE}/.repository") {
            sh 'mvn -DskipTests clean install'
          }
        }
      }
    }

    stage('Build ikats-ingestion') {
      echo "\u27A1 Pulling Ikats Ingestion module"

      dir('ikats-ingestion') {

        git url: "https://thor.si.c-s.fr/git/ikats-ingestion", branch: params.BRANCH_TO_USE, credentialsId: credentials

        withMaven(
          // Maven installation declared in the Jenkins "Global Tool Configuration"
          maven: 'Maven 3.3.9',
          // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
          // Maven settings and global settings can also be defined in Jenkins Global Tools Configuration
          //mavenSettingsConfig: 'my-maven-settings',
          mavenLocalRepo: "${WORKSPACE}/.repository") {
            sh "mvn -P${targetName}-target -DskipTests -Dtarget=${targetName} clean package tomcat7:deploy"
          }
      }
    }

    stage('tag') {
      echo "\u27A1 Tagging Deployed version"

      def repos = ['ikats-ingestion']
      def builders = [:]
      for (x in repos) {
        def repo = x // Need to bind the label variable before the closure - can't do 'for (repo in repos)'
        builders[repo] = {
          dir("${repo}") {
            withCredentials([usernamePassword(credentialsId: credentials, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
              // Delete local tag
              sh ("git tag -d DEPLOY_${CLUSTER} || true")
              // Apply new tag
              sh ("git tag DEPLOY_${CLUSTER} -m 'Deployed by jenkins build id ${BUILD_DISPLAY_NAME}'")
              // Delete remote tag
              sh ("git push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@thor.si.c-s.fr/git/${repo} :refs/tags/DEPLOY_${CLUSTER}")
              // Push new local tag to remote
              sh ("git push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@thor.si.c-s.fr/git/${repo} --tags")
            }
          }
        }
      }
      parallel builders
    }
  }

  catch (err) {
    echo "\u2717 Error during build"

    currentBuild.result = "FAILURE"

    emailext(
      subject: 'Ikats INGESTION Build Failed',
      body: "Project build error is here: ${env.BUILD_URL}",
      recipientProviders: [[$class: 'CulpritsRecipientProvider']])

    throw err
  }
}
