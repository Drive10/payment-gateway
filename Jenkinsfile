pipeline {
    agent any
    
    environment {
        REGISTRY = 'localhost:5000'
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7)}"
        DOCKER_BUILDKIT = '1'
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo "Checking out source code..."
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                echo "Building application with Maven..."
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                echo "Running unit tests..."
                sh 'mvn test'
            }
        }
        
        stage('Security Scan') {
            steps {
                echo "Running Trivy container scan..."
                sh 'trivy image --severity HIGH,CRITICAL localhost:5000/payment-gateway:test || true'
            }
        }
        
        stage('Build Docker Images') {
            steps {
                echo "Building Docker images..."
                
                script {
                    def services = [
                        'api-gateway',
                        'auth-service', 
                        'order-service',
                        'payment-service',
                        'notification-service',
                        'analytics-service',
                        'simulator-service'
                    ]
                    
                    services.each { service ->
                        sh """
                            docker build \
                                --build-arg SERVICE_PATH=services/${service} \
                                -t ${REGISTRY}/${service}:${IMAGE_TAG} \
                                -t ${REGISTRY}/${service}:latest \
                                -f Dockerfile.build .
                        """
                    }
                }
            }
        }
        
        stage('Push to Local Registry') {
            steps {
                echo "Pushing images to local Docker registry..."
                sh '''
                    for service in api-gateway auth-service order-service payment-service notification-service analytics-service simulator-service; do
                        docker push ${REGISTRY}/${service}:${IMAGE_TAG}
                        docker push ${REGISTRY}/${service}:latest
                    done
                '''
            }
        }
        
        stage('Deploy to K3s') {
            when {
                anyOf {
                    branch 'main'
                    branch pattern: 'dev/.*', comparator: 'REGEXP'
                }
            }
            steps {
                echo "Deploying to Kubernetes..."
                sh '''
                    # Update image tags in deployment
                    kubectl set image deployment/auth-service auth-service=${REGISTRY}/auth-service:${IMAGE_TAG} -n payment-gateway || echo "Deployment not found"
                    kubectl set image deployment/api-gateway api-gateway=${REGISTRY}/api-gateway:${IMAGE_TAG} -n payment-gateway || echo "Deployment not found"
                    kubectl set image deployment/payment-service payment-service=${REGISTRY}/payment-service:${IMAGE_TAG} -n payment-gateway || echo "Deployment not found"
                '''
            }
        }
        
        stage('Health Check') {
            steps {
                echo "Waiting for pods to be ready..."
                sh '''
                    sleep 30
                    kubectl get pods -n payment-gateway
                    kubectl get svc -n payment-gateway
                '''
            }
        }
        
        stage('Smoke Test') {
            steps {
                echo "Running smoke tests..."
                sh '''
                    # Test API Gateway health
                    curl -f http://localhost:8080/actuator/health || exit 1
                    
                    # Test Auth service
                    curl -f http://localhost:8081/actuator/health || exit 1
                    
                    echo "Smoke tests passed!"
                '''
            }
        }
    }
    
    post {
        success {
            echo "Pipeline completed successfully!"
            emailext(
                subject: "SUCCESS: Build ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build ${env.BUILD_NUMBER} completed successfully.",
                to: env.EMAIL_TO
            )
        }
        failure {
            echo "Pipeline failed!"
            emailext(
                subject: "FAILURE: Build ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build ${env.BUILD_NUMBER} failed. Check logs at ${env.BUILD_URL}",
                to: env.EMAIL_TO
            )
        }
        always {
            echo "Cleaning up..."
            sh 'docker system prune -f || true'
        }
    }
}
