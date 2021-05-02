pipeline {
    agent any

    stages {
        stage('Another Hello') {
            steps {
                githubNotify description: 'Quick Build', context: 'CI/master',  status: 'PENDING'
                echo 'Hello World from master'
            }
        }
        stage('Hello') {
            steps {
                echo 'Hello World old style new branch'
                echo 'Hello World from new branch'
            }
        }
    }

    post {
        success {
            githubNotify description: 'Quick Build', context: 'CI/master',  status: 'SUCCESS'
        }
    }
}
