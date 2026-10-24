// Declarative CI/CD pipeline for the Calculator REST API.
//
// Prerequisites on the Jenkins controller/agent:
//   * Tools        : Maven named "Maven3", JDK named "JDK21" (Manage Jenkins > Tools)
//   * Plugins      : Pipeline, Git, JUnit, JaCoCo, Docker Pipeline, Email Extension
//   * Credentials  : Username/Password credential with ID "dockerhub-credentials"
//   * Trigger      : GitHub webhook -> http://<jenkins>/github-webhook/
//                    (Poll SCM fallback is configured below)

pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk   'JDK21'
    }

    environment {
        DOCKER_HUB_USER = 'jayeshpandit'
        IMAGE_NAME      = 'calculator-api'
        IMAGE_REPO      = "${DOCKER_HUB_USER}/${IMAGE_NAME}"
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        DOCKER_IMAGE    = "${IMAGE_REPO}:${IMAGE_TAG}"
        REGISTRY_CREDS  = 'dockerhub-credentials'
        NOTIFY_EMAIL    = 'iiitbachelors@gmail.com'
        PROXY_PORT      = '8080'
    }

    triggers {
        // Webhook is the primary trigger; polling is the documented fallback.
        githubPush()
        pollSCM('H/5 * * * *')
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
        disableConcurrentBuilds()   // keeps the single shared deployment consistent
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_MSG   = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
                }
                echo "Building ${env.GIT_SHORT} on ${env.BRANCH_NAME ?: 'main'}: ${env.GIT_MSG}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/calculator.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                // A test failure fails this stage (and the build) straight away;
                // the publishers in post{always} still run, so the JUnit report
                // and coverage are available for the failed build too.
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          skipPublishingChecks: true
                    jacoco execPattern: 'target/jacoco.exec',
                           classPattern: 'target/classes',
                           sourcePattern: 'src/main/java'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def image = docker.build("${IMAGE_REPO}:${IMAGE_TAG}", '--pull .')
                    image.tag('latest')
                }
                sh "docker image inspect ${IMAGE_REPO}:${IMAGE_TAG} --format '{{.Size}} bytes'"
            }
        }

        stage('Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', env.REGISTRY_CREDS) {
                        docker.image("${IMAGE_REPO}:${IMAGE_TAG}").push()
                        docker.image("${IMAGE_REPO}:${IMAGE_TAG}").push('latest')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                // Idempotent: `up -d` reconciles the running stack in place instead of
                // tearing it down, so a re-run of the same build is a no-op.
                withEnv(["DOCKER_IMAGE=${IMAGE_REPO}", "IMAGE_TAG=${IMAGE_TAG}"]) {
                    sh '''
                        set -eu
                        docker compose pull --quiet calculator || true
                        docker compose up -d --remove-orphans
                        docker compose ps
                    '''
                }
            }
        }

        stage('Verify Health') {
            steps {
                sh '''
                    set -eu
                    echo "Waiting for the stack to report healthy..."
                    for attempt in $(seq 1 30); do
                        if curl -fsS "http://localhost:${PROXY_PORT}/actuator/health" | grep -q '"status":"UP"'; then
                            echo "Health check passed on attempt ${attempt}"
                            curl -fsS "http://localhost:${PROXY_PORT}/api/v1/add?a=2&b=3"; echo
                            curl -fsS "http://localhost:${PROXY_PORT}/api/v1/multiply?a=6&b=7"; echo
                            # divide-by-zero must answer 400, not 500
                            code=$(curl -s -o /dev/null -w '%{http_code}' \
                                   "http://localhost:${PROXY_PORT}/api/v1/divide?a=1&b=0")
                            test "$code" = "400" || { echo "expected 400, got $code"; exit 1; }
                            echo "Smoke tests passed."
                            exit 0
                        fi
                        sleep 5
                    done
                    echo "Deployment did not become healthy in time"
                    docker compose logs --tail=100
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            // Keep the agent's disk from filling up with per-build image tags.
            sh "docker image rm -f ${IMAGE_REPO}:${IMAGE_TAG} || true"
        }
    }
}
