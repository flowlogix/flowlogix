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
                echo 'Hello World'
                sleep 20
                echo 'Hello World from new branch'
            }
        }
    }
}

