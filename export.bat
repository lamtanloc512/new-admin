mvn -pl . clean install & ^
mvn -pl new-admin-admin-plugin clean install -Pexport,\!test & ^
ezy.bat package & ^
ezy.bat export
