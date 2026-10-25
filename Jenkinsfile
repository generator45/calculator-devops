// Declarative CI/CD pipeline for the Calculator REST API.
//
// Prerequisites on the Jenkins controller/agent:
//   * Tools        : JDK named "JDK21" (Manage Jenkins > Tools). Maven comes from the
//                    repo's ./mvnw wrapper, so no Jenkins Maven install is needed.
//   * Plugins      : Pipeline, Git, GitHub, JUnit, Docker Pipeline, Email Extension,
//                    Timestamper
//   * Credentials  : Username/Password credential with ID "dockerhub-creds"
//   * Trigger      : GitHub webhook -> http://<jenkins>/github-webhook/
//                    (Poll SCM fallback is configured below)

pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    environment {
        DOCKER_HUB_USER = 'jayeshpandit'
        IMAGE_NAME      = 'calculator-api'
        IMAGE_REPO      = "${DOCKER_HUB_USER}/${IMAGE_NAME}"
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        REGISTRY_CREDS  = 'dockerhub-creds'
        NOTIFY_EMAIL    = 'jai4924p@gmail.com'
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
                sh './mvnw -B clean package -DskipTests'
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
                // the publisher in post{always} still runs, so the JUnit report
                // is available for the failed build too.
                sh './mvnw -B test'
            }
            post {
                always {
                    // allowEmptyResults: a test-compilation failure produces no XML,
                    // and the publisher's own error would otherwise bury the real cause.
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: true,
                          skipPublishingChecks: true
                    // Coverage is produced by the JaCoCo Maven plugin into
                    // target/site/jacoco and archived as a build artifact. It is
                    // deliberately not published through the Jenkins JaCoCo plugin:
                    // that plugin is deprecated, and the brief only requires JUnit
                    // results to be published here.
                    archiveArtifacts artifacts: 'target/site/jacoco/**',
                                     allowEmptyArchive: true,
                                     fingerprint: false
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
                            # Error paths must keep their status codes through the proxy;
                            # a 500 here would mean the catch-all handler has regressed.
                            assert_status() {
                                got=$(curl -s -X "${3:-GET}" -o /dev/null -w '%{http_code}' "$1")
                                test "$got" = "$2" || { echo "$1 -> expected $2, got $got"; exit 1; }
                            }
                            base="http://localhost:${PROXY_PORT}/api/v1"
                            assert_status "$base/divide?a=1&b=0"          400
                            assert_status "$base/add?a=NaN&b=1"           400
                            assert_status "$base/divide?a=1e308&b=1e-308" 400
                            assert_status "$base/add?a=2"                 400
                            assert_status "$base/modulo?a=7&b=3"          404
                            assert_status "$base/add?a=2&b=3"             405 POST
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
            // Disk hygiene on the agent. Deliberately a time-bounded prune rather than
            // `docker image rm` of this build's tag: the running stack is pinned to that
            // tag, so removing it would strand the deployment on a missing image.
            sh 'docker image prune -f --filter "until=168h" || true'
        }
        success {
            emailext(
                to: env.NOTIFY_EMAIL,
                subject: "SUCCESS - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: false,
                body: """
                    <h3>Build succeeded</h3>
                    <ul>
                      <li><b>Job</b>: ${env.JOB_NAME} #${env.BUILD_NUMBER}</li>
                      <li><b>Commit</b>: ${env.GIT_SHORT} - ${env.GIT_MSG}</li>
                      <li><b>Image</b>: ${IMAGE_REPO}:${IMAGE_TAG}</li>
                      <li><b>Deployed at</b>: http://localhost:${PROXY_PORT}/api/v1/add?a=2&amp;b=3</li>
                      <li><b>Console</b>: <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></li>
                    </ul>
                """
            )
        }
        failure {
            emailext(
                to: env.NOTIFY_EMAIL,
                subject: "FAILURE - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                attachLog: true,
                compressLog: true,
                body: """
                    <h3>Build failed</h3>
                    <ul>
                      <li><b>Job</b>: ${env.JOB_NAME} #${env.BUILD_NUMBER}</li>
                      <li><b>Commit</b>: ${env.GIT_SHORT} - ${env.GIT_MSG}</li>
                      <li><b>Failed stage</b>: see the console log attached</li>
                      <li><b>Console</b>: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></li>
                    </ul>
                """
            )
        }
        unstable {
            emailext(
                to: env.NOTIFY_EMAIL,
                subject: "UNSTABLE - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Tests failed in ${env.JOB_NAME} #${env.BUILD_NUMBER}: ${env.BUILD_URL}testReport/"
            )
        }
    }
}
