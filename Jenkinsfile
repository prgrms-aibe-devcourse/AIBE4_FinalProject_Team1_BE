pipeline {
    agent any

    tools {
        jdk 'jdk-17'
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the application...'
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build -x test --no-daemon'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                // 테스트 실패해도 파이프라인은 계속 진행, 결과만 수집
                sh './gradlew test --no-daemon || true'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                         testResults: '**/build/test-results/test/*.xml'

                    // Jacoco coverage report
                    jacoco(
                        execPattern: '**/build/jacoco/*.exec',
                        classPattern: '**/build/classes/java/main',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/generated/**,**/Q*.class,**/*Application*.class,**/*Config*.class,**/*Exception*.class,**/*ErrorCode*.class,**/*Request*.class,**/*Response*.class,**/*Dto*.class,**/*Enum*.class,**/*Interceptor*.class,**/*Filter*.class,**/*Resolver*.class'
                    )
                }
            }
        }

        stage('Code Quality') {
            when {
                branch 'main'
            }
            steps {
                echo 'Running code quality analysis...'
                withSonarQubeEnv('SonarCloud') {
                    sh './gradlew sonar --no-daemon'
                }
            }
        }
    }

    post {
        success {
            echo '✅ CI 완료: 빌드 성공'
        }
        failure {
            echo '❌ CI 실패: 빌드 실패 (컴파일 에러 확인 필요)'
        }
        always {
            echo 'Cleaning workspace...'
            cleanWs()
        }
    }
}
