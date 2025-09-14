package org.ltl.enhancement.admin.test;

import org.youngmonkeys.devtools.swagger.SwaggerGenerator;

public class AdminPluginSwaggerGenerator {

    public static void main(String[] args) throws Exception {
        SwaggerGenerator swaggerGenerator = new SwaggerGenerator(
            "org.ltl.enhancement.admin.controller"
        );
        swaggerGenerator.generateToDefaultFile();
    }
}
