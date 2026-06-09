pipeline {
  agent any

  environment {
    REGISTRY = "${env.CONTAINER_REGISTRY ?: 'ghcr.io/citizens'}"
    BACKEND_IMAGE = "${REGISTRY}/digital-twin-fraud-backend"
    FRONTEND_IMAGE = "${REGISTRY}/digital-twin-fraud-frontend"
    IMAGE_TAG = "${env.BUILD_NUMBER ?: 'local'}"
    HELM_RELEASE = "${env.HELM_RELEASE ?: 'digital-twin-fraud'}"
    HELM_NAMESPACE = "${env.HELM_NAMESPACE ?: 'fraud-platform'}"
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build and Test') {
      parallel {
        stage('Backend') {
          steps {
            sh 'cd backend && mvn -B -DskipTests=false test package'
          }
        }
        stage('Frontend') {
          steps {
            sh 'cd frontend && npm ci && npm run build'
          }
        }
      }
    }

    stage('Docker Build') {
      when {
        anyOf {
          branch 'main'
          expression { return params.DEPLOY == true }
        }
      }
      parallel {
        stage('Backend Image') {
          steps {
            sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} ./backend"
            sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${BACKEND_IMAGE}:latest"
          }
        }
        stage('Frontend Image') {
          steps {
            sh """
              docker build \
                --build-arg VITE_API_BASE_URL=/api/v1 \
                --build-arg VITE_WS_URL=/ws/decisions \
                --build-arg VITE_SECURITY_ENABLED=true \
                -t ${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend
            """
            sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${FRONTEND_IMAGE}:latest"
          }
        }
      }
    }

    stage('Push Images') {
      when {
        anyOf {
          branch 'main'
          expression { return params.DEPLOY == true }
        }
      }
      steps {
        sh "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
        sh "docker push ${BACKEND_IMAGE}:latest"
        sh "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
        sh "docker push ${FRONTEND_IMAGE}:latest"
      }
    }

    stage('Helm Lint') {
      steps {
        sh 'helm lint deploy/helm/digital-twin-fraud'
        sh 'helm template digital-twin-fraud deploy/helm/digital-twin-fraud --debug > /dev/null'
      }
    }

    stage('Deploy to Kubernetes') {
      when {
        expression { return params.DEPLOY == true }
      }
      steps {
        sh """
          helm upgrade --install ${HELM_RELEASE} deploy/helm/digital-twin-fraud \
            --namespace ${HELM_NAMESPACE} \
            --create-namespace \
            --set backend.image.tag=${IMAGE_TAG} \
            --set frontend.image.tag=${IMAGE_TAG} \
            -f deploy/helm/digital-twin-fraud/values-prod.yaml
        """
        sh "kubectl rollout status deployment/${HELM_RELEASE}-backend -n ${HELM_NAMESPACE} --timeout=300s"
        sh "kubectl rollout status deployment/${HELM_RELEASE}-frontend -n ${HELM_NAMESPACE} --timeout=300s"
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
    }
    success {
      echo 'Pipeline completed successfully'
    }
    failure {
      echo 'Pipeline failed'
    }
  }
}
