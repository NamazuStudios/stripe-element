package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.dao.StripeConfigDao;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeConfigDocument;
import dev.getelements.elements.stripe.model.StripeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConfigServiceImplTest {

    @Mock private StripeConfigDao configDao;

    private StripeConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StripeConfigServiceImpl(
                configDao,
                "sk_live_default", "whsec_live_default",
                "sk_test_default", "whsec_test_default");
    }

    @Test
    void getConfig_mode_noDocument_returnsAttributeDefaults() {
        when(configDao.findConfig()).thenReturn(Optional.empty());

        assertEquals(new StripeConfig("sk_live_default", "whsec_live_default"), service.getConfig(StripeMode.PRODUCTION));
        assertEquals(new StripeConfig("sk_test_default", "whsec_test_default"), service.getConfig(StripeMode.SANDBOX));
    }

    @Test
    void getConfig_mode_dbValuePresent_overridesAttributeDefault() {
        final var doc = new StripeConfigDocument();
        doc.setApiKey("sk_live_db");
        doc.setWebhookSecret("whsec_live_db");
        when(configDao.findConfig()).thenReturn(Optional.of(doc));

        assertEquals(new StripeConfig("sk_live_db", "whsec_live_db"), service.getConfig(StripeMode.PRODUCTION));
    }

    @Test
    void getConfig_mode_perFieldFallback_blankDbFieldFallsBackIndependently() {
        final var doc = new StripeConfigDocument();
        doc.setApiKey("sk_live_db");
        doc.setWebhookSecret("");
        when(configDao.findConfig()).thenReturn(Optional.of(doc));

        final var config = service.getConfig(StripeMode.PRODUCTION);
        assertEquals("sk_live_db", config.apiKey());
        assertEquals("whsec_live_default", config.webhookSecret());
    }

    @Test
    void getConfig_mode_sandboxAndProductionAreIndependent() {
        final var doc = new StripeConfigDocument();
        doc.setApiKey("sk_live_db");
        doc.setWebhookSecret("whsec_live_db");
        doc.setSandboxApiKey("sk_test_db");
        doc.setSandboxWebhookSecret("whsec_test_db");
        when(configDao.findConfig()).thenReturn(Optional.of(doc));

        assertEquals(new StripeConfig("sk_live_db", "whsec_live_db"), service.getConfig(StripeMode.PRODUCTION));
        assertEquals(new StripeConfig("sk_test_db", "whsec_test_db"), service.getConfig(StripeMode.SANDBOX));
    }

    @Test
    void getRawConfig_noAttributeFallback_returnsNullWhenUnset() {
        when(configDao.findConfig()).thenReturn(Optional.empty());

        final var raw = service.getRawConfig(StripeMode.SANDBOX);
        assertNull(raw.apiKey());
        assertNull(raw.webhookSecret());
    }

    @Test
    void resolveDefaultMode_productionConfigured_returnsProduction() {
        when(configDao.findConfig()).thenReturn(Optional.empty());
        assertEquals(StripeMode.PRODUCTION, service.resolveDefaultMode());
    }

    @Test
    void resolveDefaultMode_productionNotConfigured_returnsSandbox() {
        final var service = new StripeConfigServiceImpl(configDao, "", "", "sk_test_default", "whsec_test_default");
        when(configDao.findConfig()).thenReturn(Optional.empty());

        assertEquals(StripeMode.SANDBOX, service.resolveDefaultMode());
    }

    @Test
    void getConfig_noArg_delegatesToResolvedDefaultMode() {
        final var service = new StripeConfigServiceImpl(configDao, "", "", "sk_test_default", "whsec_test_default");
        when(configDao.findConfig()).thenReturn(Optional.empty());

        assertEquals(new StripeConfig("sk_test_default", "whsec_test_default"), service.getConfig());
    }

    @Test
    void saveConfig_mode_preservesOtherModesFields() {
        final var existing = new StripeConfigDocument();
        existing.setApiKey("sk_live_old");
        existing.setSandboxApiKey("sk_test_untouched");
        existing.setSandboxWebhookSecret("whsec_test_untouched");
        when(configDao.findConfig()).thenReturn(Optional.of(existing));

        service.saveConfig(new StripeConfig("sk_live_new", "whsec_live_new"), StripeMode.PRODUCTION);

        final var captor = ArgumentCaptor.forClass(StripeConfigDocument.class);
        verify(configDao).saveConfig(captor.capture());
        final var saved = captor.getValue();
        assertEquals("sk_live_new", saved.getApiKey());
        assertEquals("whsec_live_new", saved.getWebhookSecret());
        assertEquals("sk_test_untouched", saved.getSandboxApiKey());
        assertEquals("whsec_test_untouched", saved.getSandboxWebhookSecret());
    }

    @Test
    void saveConfig_legacyNoModeArg_writesProductionFields() {
        when(configDao.findConfig()).thenReturn(Optional.empty());

        service.saveConfig(new StripeConfig("sk_live_new", "whsec_live_new"));

        final var captor = ArgumentCaptor.forClass(StripeConfigDocument.class);
        verify(configDao).saveConfig(captor.capture());
        assertEquals("sk_live_new", captor.getValue().getApiKey());
        assertNull(captor.getValue().getSandboxApiKey());
    }

}
