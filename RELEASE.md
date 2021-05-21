# How to Deploy Flow Logix Library 

# To deploy a release:
mvn release:prepare # -DdryRun=true
mvn release:perform

# To deploy a snapshot
mvn clean deploy
