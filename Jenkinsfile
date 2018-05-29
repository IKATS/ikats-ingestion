pipeline {
    agent any
    tools {
        maven 'Maven 3.5.2'
        jdk 'JDK 1.8'
    }
    parameters {
        string(name: 'DB_HOST', defaultValue: 'pgsql.dev', description: 'Hôte PostgreSQL')
        string(name: 'DB_PORT', defaultValue: '5432', description: 'Port PostgreSQL')
        string(name: 'OPENTSDB_WRITE_HOST', defaultValue: 'opentsdb-write', description: 'Hôte OpenTSDB')
        string(name: 'OPENTSDB_WRITE_PORT', defaultValue: '4243', description: 'Port OpenTSDB')
    }
    stages {
        stage('Fetch SCM') {
            steps {
                checkout scm
            }
        }

        //stage('Unit Tests') {
        //    steps {
        //        withMaven(
        //            maven: 'Maven 3.5.2',
        //            mavenSettingsConfig: 'e924d227-1005-4fcb-92ef-3d382c066f09'
        //        ) {
        //            sh 'mvn clean install -DskipTests'
        //            sh 'mvn test'
        //        }
        //    }
        //    post {
        //        success {
        //            junit '**/target/surefire-reports/**/*.xml'
        //            step( [ $class: 'JacocoPublisher' ] )
        //        }
        //    }
        //}

        stage('Build the image') {
            agent { node { label 'docker' } }
            steps {
                script {
                  
                    // Replacing docker registry to private one. See [#172302]
                    sh "sed -i 's/FROM ikats/FROM hub.ops.ikats.org/' Dockerfile"

                    ingestModuleImage = docker.build("ingestion", "--pull .")

                    fullBranchName = "${env.BRANCH_NAME}"
                    branchName = fullBranchName.substring(fullBranchName.lastIndexOf("/") + 1)
                    shortCommit = "${GIT_COMMIT}".substring(0, 7)

                    docker.withRegistry("${env.REGISTRY_ADDRESS}", 'DOCKER_REGISTRY') {
                        /* Push the container to the custom Registry */
                        ingestModuleImage.push(branchName + "_" + shortCommit)
                        ingestModuleImage.push(branchName + "_latest")
                          if (branchName == "master") {
                            ingestModuleImage.push("master")
                          }
                    }
                }
            }
        }
    }
}
