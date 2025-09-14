set -e
mvn -pl . clean install
mvn -pl enhancement-admin-plugin clean install -Pexport,\!test
ezy.sh package
ezy.sh export
