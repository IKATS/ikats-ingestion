pipeline {
    agent any
    tools {
        maven 'Maven 3.5.2'
        jdk 'JDK 1.8'
    }
    parameters {
        string(name: 'DB_HOST', defaultValue: 'pgsql.dev', description: 'Hôte PostgreSQL')
        string(name: 'DB_PORT', defaultValue: '5432', description: 'Port PostgreSQL')
        string(name: 'OPENTSDB_READ_HOST', defaultValue: 'opentsdb-read', description: 'Hôte OpenTSDB')
        string(name: 'OPENTSDB_READ_PORT', defaultValue: '4242', description: 'Port OpenTSDB')
        string(name: 'OPENTSDB_WRITE_HOST', defaultValue: 'opentsdb-write', description: 'Hôte OpenTSDB')
        string(name: 'OPENTSDB_WRITE_PORT', defaultValue: '4243', description: 'Port OpenTSDB')
    }
    stages {
        stage('Fetch SCM') {
            steps {
                checkout scm
                sh 'git submodule update --init'
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
            environment {
                DB_HOST = "${params.DB_HOST}"
                DB_PORT = "${params.DB_PORT}"
                OPENTSDB_READ_HOST = "${params.OPENTSDB_READ_HOST}"
                OPENTSDB_READ_PORT = "${params.OPENTSDB_READ_PORT}"
                OPENTSDB_WRITE_HOST = "${params.OPENTSDB_WRITE_HOST}"
                OPENTSDB_WRITE_PORT = "${params.OPENTSDB_WRITE_PORT}"
                C3P0_ACQUIRE_INCREMENT = "2"
                C3P0_MAX_SIZE = "20"
                C3P0_IDLE_TEST_PERIOD = "50"
                C3P0_MAX_STATEMENTS = "15"
                C3P0_MIN_SIZE = "5"
                C3P0_TIMEOUT = "90"
            }

            steps {
                script {
                    datamodelImage = docker.build("ikats-ingestion",
                                                    "--build-arg DB_HOST=${DB_HOST} "
                                                    + "--build-arg DB_PORT=${DB_PORT} "
                                                    + "--build-arg OPENTSDB_READ_HOST=${OPENTSDB_READ_HOST} "
                                                    + "--build-arg OPENTSDB_READ_PORT=${OPENTSDB_READ_PORT} "
                                                    + "--build-arg OPENTSDB_WRITE_HOST=${OPENTSDB_WRITE_HOST} "
                                                    + "--build-arg OPENTSDB_WRITE_PORT=${OPENTSDB_WRITE_PORT} "
                                                    + "--build-arg C3P0_ACQUIRE_INCREMENT=${C3P0_ACQUIRE_INCREMENT} "
                                                    + "--build-arg C3P0_MAX_SIZE=${C3P0_MAX_SIZE} "
                                                    + "--build-arg C3P0_IDLE_TEST_PERIOD=${C3P0_IDLE_TEST_PERIOD} "
                                                    + "--build-arg C3P0_MAX_STATEMENTS=${C3P0_MAX_STATEMENTS} "
                                                    + "--build-arg C3P0_MIN_SIZE=${C3P0_MIN_SIZE} "
                                                    + "--build-arg C3P0_TIMEOUT=${C3P0_TIMEOUT} "
                                                    + " .")

                    fullBranchName = "${env.BRANCH_NAME}"
                    branchName = fullBranchName.substring(fullBranchName.lastIndexOf("/") + 1)
                    shortCommit = "${GIT_COMMIT}".substring(0, 7)

                    docker.withRegistry("${env.REGISTRY_ADDRESS}", 'DOCKER_REGISTRY') {
                        /* Push the container to the custom Registry */
                        datamodelImage.push(branchName + "_${GIT_COMMIT}")
                        datamodelImage.push(branchName + "_latest")
                          if (branchName == "master") {
                            datamodelImage.push("latest")
                          }
                    }
                }
            }
        }
    }
}