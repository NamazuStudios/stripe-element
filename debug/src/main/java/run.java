import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Runs your local Element in the SDK.
 */
public class run {
    public static void main(final String[] args ) throws IOException, InterruptedException {

        try {
            new ProcessBuilder("docker", "compose", "up", "-d")
                    .directory(new java.io.File("services-dev"))
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (final InterruptedException e) {
            // Mongo is likely already running
        }

        // Load local overrides from debug/local.properties if present.
        var builder = ElementsLocalBuilder.getDefault().withSourceRoot();
        final var localPropertiesFile = new File("debug/local.properties");

        if (localPropertiesFile.exists()) {
            final var props = new Properties();

            try (final var in = new FileInputStream(localPropertiesFile)) {
                props.load(in);
            }

            builder = builder.withProperties(props);
        }

        final var local = builder
                .withDeployment(b -> b
                        .useDefaultRepositories(true)
                        .elementPackage()
                        .elmArtifact("dev.getelements.elements.stripe:element:elm:1.0-SNAPSHOT")
                        .endElementPackage()
                        .build()
                )
                .build();

        local.start();
        local.run();
    }

}
