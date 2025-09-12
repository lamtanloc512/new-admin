package org.ltl.new_admin.admin.test;

import org.youngmonkeys.devtools.swagger.SwaggerGenerator;

public class NewAdminAdminPluginSwaggerGenerator {

    public static void main(String[] args) throws Exception {
        SwaggerGenerator swaggerGenerator = new SwaggerGenerator(
            "org.ltl.new_admin.admin.controller"
        );
        swaggerGenerator.generateToDefaultFile();
    }
}
