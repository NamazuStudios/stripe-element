import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Runs your local Element in the SDK.
 *
 * The element version is read from the build's pom.xml, so it tracks whatever {@code mvn
 * versions:set} (the release workflow, or a manual bump) has set — no separate version to
 * keep in sync. A specific version can still be pinned with the
 * {@code stripe.element.version} system property.
 */
public class run {

    private static final String ELEMENT_VERSION_PROPERTY = "stripe.element.version";

    /**
     * Poms to consult for the element version, in order: the debug module's own pom (first
     * {@code <version>} is its parent — the release version), then the root pom (first
     * {@code <version>} is the project version). Both are rewritten by {@code mvn versions:set}.
     */
    private static final List<String> POMS = List.of("debug/pom.xml", "pom.xml");

    private static final Pattern FIRST_VERSION_TAG = Pattern.compile("<version>([^<]+)</version>");

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

        final var version = resolveElementVersion();
        final var artifact = "dev.getelements.elements.stripe:element:elm:" + version;
        System.getLogger(run.class.getName()).log(System.Logger.Level.INFO, "Deploying element " + artifact);

        final var local = builder
                .withDeployment(b -> b
                        .useDefaultRepositories(true)
                        .elementPackage()
                        .elmArtifact(artifact)
                        .endElementPackage()
                        .build()
                )
                .build();

        local.start();
        local.run();
    }

    /**
     * Resolves the element version to deploy: the {@code stripe.element.version} system property
     * if set, otherwise the version declared in the build's pom.xml. Because the release
     * workflow bumps the poms with {@code mvn versions:set} and commits them, the debug runner
     * automatically tracks release and post-release snapshot versions with no manual edit.
     */
    private static String resolveElementVersion() throws IOException {

        final var override = System.getProperty(ELEMENT_VERSION_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }

        for (final var pom : POMS) {
            final var file = new File(pom);
            if (!file.exists()) {
                continue;
            }
            final var matcher = FIRST_VERSION_TAG.matcher(Files.readString(file.toPath()));
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        throw new IllegalStateException(
                "Could not determine the element version from " + POMS +
                        " — run this class from the project root, or set the " +
                        ELEMENT_VERSION_PROPERTY + " system property.");
    }

}
