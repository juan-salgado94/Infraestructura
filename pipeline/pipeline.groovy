pipeline {

    environment {
        BASE_GIT_URL = 'https://github.com/juan-salgado94'
        APP_REPO_URL = "${env.BASE_GIT_URL}/${nombre_repo}.git"
        INFRA_REPO_URL = "${env.BASE_GIT_URL}/Infraestructura.git"
        DOCKER_IMAGE = "juansalgado94/${nombre_repo}"
        DEPLOY_FOLDER = "deploy/kubernete/${nombre_repo}"
    }

    agent any
    stages {
        stage("Checkout app-code") {
            steps {
            //se esta crando una carpeta....
               dir('app') {
                    git url:"${env.APP_REPO_URL}" , branch: "${version}"
                } 
            }
        }
         
         stage("Checkout deploy-code") {
            steps {
               dir('deploy') {
                    git url:"${env.INFRA_REPO_URL}" , branch: "master"
                } 
            }
        }
        
         stage("Build image") {
            steps {
                dir('app') {
                    script {
                        dockerImage = docker.build("${env.DOCKER_IMAGE}:${tag}")
                    }
                }
            }
        }
        
        stage("Push image") {
            steps {
                script {
                    docker.withRegistry('', 'salgadodocker') {
                    dockerImage.push()
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps{
                sh "sed -i 's:DOCKER_IMAGE:${env.DOCKER_IMAGE}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                sh "sed -i 's:TAG:${tag}:g' ${DEPLOY_FOLDER}/deployment.yaml"
                
                step([$class: 'KubernetesEngineBuilder', 
                        projectId: "strong-imagery-288623",
                        clusterName: "cluster-salgado",
                        zone: "us-central1-c",
                        manifestPattern: "${DEPLOY_FOLDER}/",
                        credentialsId: "My First Project",
                        verifyDeployments: true])
            }
        }
    }
}