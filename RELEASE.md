# Deploy Flow Logix Library 

# To deploy a release:
mvn release:prepare # -DdryRun=true

mvn release:perform # -DdryRun=true

# To deploy a snapshot
mvn clean deploy 
