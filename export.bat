mvn -pl . clean install & ^
mvn -pl enhancement clean install -Pexport,\!test & ^
ezy.bat package & ^
ezy.bat export
