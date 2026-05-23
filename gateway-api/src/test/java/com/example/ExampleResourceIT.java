package com.example;

import com.example.support.GatewayDownstreamMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
@QuarkusTestResource(value = GatewayDownstreamMockResource.class, restrictToAnnotatedClass = true)
class ExampleResourceIT extends ExampleResourceTest {
    // Execute the same tests but in packaged mode.
}
