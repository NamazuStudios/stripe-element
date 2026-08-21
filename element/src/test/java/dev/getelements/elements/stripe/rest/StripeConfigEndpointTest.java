package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeDualConfig;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.StripeConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConfigEndpointTest {

    @Mock private StripeConfigService configService;

    private StripeConfigEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeConfigEndpoint(configService);
    }

    @Test
    void getConfig_returnsMaskedProductionAndSandbox() {
        when(configService.getConfig(StripeMode.PRODUCTION)).thenReturn(new StripeConfig("sk_live_abcd1234", "whsec_livesecret1234"));
        when(configService.getConfig(StripeMode.SANDBOX)).thenReturn(new StripeConfig("sk_test_wxyz9876", "whsec_testsecret9876"));

        final var result = endpoint.getConfig();

        assertEquals("••••1234", result.production().apiKey());
        assertEquals("••••1234", result.production().webhookSecret());
        assertEquals("••••9876", result.sandbox().apiKey());
        assertEquals("••••9876", result.sandbox().webhookSecret());
    }

    @Test
    void saveConfig_unchangedMaskedField_isNotOverwritten() {
        when(configService.getRawConfig(StripeMode.PRODUCTION)).thenReturn(new StripeConfig("sk_live_abcd1234", "whsec_livesecret1234"));

        // Simulate the UI submitting the field back unedited: it only shows the mask.
        final var submitted = new StripeDualConfig(new StripeConfig("••••1234", "••••1234"), null);
        endpoint.saveConfig(submitted);

        verify(configService).saveConfig(new StripeConfig("sk_live_abcd1234", "whsec_livesecret1234"), StripeMode.PRODUCTION);
        verify(configService, never()).saveConfig(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(StripeMode.SANDBOX));
    }

    @Test
    void saveConfig_realEdit_isPersistedVerbatim() {
        when(configService.getRawConfig(StripeMode.SANDBOX)).thenReturn(new StripeConfig("sk_test_old", "whsec_test_old"));

        final var submitted = new StripeDualConfig(null, new StripeConfig("sk_test_new", "whsec_test_old"));
        endpoint.saveConfig(submitted);

        verify(configService).saveConfig(new StripeConfig("sk_test_new", "whsec_test_old"), StripeMode.SANDBOX);
    }

    @Test
    void saveConfig_emptyString_clearsField() {
        when(configService.getRawConfig(StripeMode.PRODUCTION)).thenReturn(new StripeConfig("sk_live_abcd1234", "whsec_livesecret1234"));

        final var submitted = new StripeDualConfig(new StripeConfig("", "••••1234"), null);
        endpoint.saveConfig(submitted);

        verify(configService).saveConfig(new StripeConfig("", "whsec_livesecret1234"), StripeMode.PRODUCTION);
    }

}
