pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                setBuildStatus("Build Started", "Pending");
                echo 'Hello World'
            }
        }
    }
/*  post {
    success {
        setBuildStatus("Build succeeded", "SUCCESS");
    }
    failure {
        setBuildStatus("Build failed", "FAILURE");
    }
  }
*/
}

