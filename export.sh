set -e
mvn -pl . clean install
mvn -pl new-admin-admin-plugin clean install -Pexport,\!test
ezy.sh package
ezy.sh export
