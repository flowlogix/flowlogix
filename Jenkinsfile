pipeline {
    agent any

    stages {
        stage('Another Hello') {
            steps {
                echo 'Hello World from master'
            }
        }
        stage('Hello') {
            steps {
                echo 'Hello World old style new branch'
                echo 'Hello World from new branch 3.0'
            }
        }
    }
}

