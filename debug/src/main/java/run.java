import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

import java.io.IOException;

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

        final var local = ElementsLocalBuilder.getDefault()
                .withSourceRoot()
                .withDeployment(builder -> builder
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
