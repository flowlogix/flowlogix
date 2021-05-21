# Deploy Flow Logix Library 

# To deploy a release:
mvn release:prepare # -DdryRun=true
mvn release:perform # -DdryRun=truex

# To deploy a snapshot
mvn clean deploy
