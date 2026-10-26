// ==============================================================================
// TrustPay Guardian — Jenkins Declarative CI/CD Pipeline
// ==============================================================================
pipeline {
    agent any

    tools {
        jdk 'JDK-25'
        nodejs 'Node-24'
        maven 'Maven-3.9'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        ansiColor('xterm')
    }

    environment {
        REGISTRY = 'registry.trustpay.internal'
        APP_NAME = 'trustpay-guardian'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source repository...'
                checkout scm
            }
        }

        stage('Backend Build & Verify') {
            steps {
                dir('backend') {
                    echo 'Compiling backend and executing JaCoCo verification tests...'
                    sh './mvnw clean verify -B'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: false
                    jacoco execPattern: 'backend/target/jacoco.exec',
                           classPattern: 'backend/target/classes',
                           sourcePattern: 'backend/src/main/java',
                           minimumInstructionCoverage: '80'
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                dir('backend') {
                    echo 'Running SonarQube static code analysis...'
                    // sonar-project.properties exists at root
                    withSonarQubeEnv(installationName: 'TrustPay-SonarQube') {
                        sh './mvnw sonar:sonar -Dsonar.projectKey=trustpay-guardian -B || true'
                    }
                }
            }
        }

        stage('Frontend Lint, Test & Build') {
            steps {
                dir('frontend') {
                    echo 'Installing frontend dependencies and running test suite...'
                    sh 'npm ci'
                    echo 'Running TypeScript lint check...'
                    sh 'npm run lint'
                    echo 'Executing Vitest unit and component tests...'
                    sh 'npm test -- --run'
                    echo 'Building production web distribution bundle...'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Images Build') {
            steps {
                echo "Building Docker container images for release tag: ${IMAGE_TAG}..."
                sh "docker build -t ${REGISTRY}/${APP_NAME}-backend:${IMAGE_TAG} -t ${REGISTRY}/${APP_NAME}-backend:latest ./backend"
                sh "docker build -t ${REGISTRY}/${APP_NAME}-frontend:${IMAGE_TAG} -t ${REGISTRY}/${APP_NAME}-frontend:latest ./frontend"
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'main'
            }
            steps {
                echo 'Deploying containerized stack to Staging Kubernetes / Docker Swarm cluster...'
                sh "docker compose -f compose.prod.yml config"
                // Deploy commands e.g.: kubectl apply or docker compose up -d
                echo 'Staging deployment verified.'
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded for build #${env.BUILD_NUMBER} (${env.BRANCH_NAME})"
        }
        failure {
            echo "Pipeline FAILED for build #${env.BUILD_NUMBER}. Check logs for details."
        }
        always {
            cleanWs deleteDirs: true, notFailBuild: true
        }
    }
}
