package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeMode;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultStripeGatewayTest {

    @Mock private StripeConfigService configService;

    private DefaultStripeGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DefaultStripeGateway(configService);
    }

    @Test
    void neitherModeConfigured_throwsInternalServerError() {
        when(configService.getConfig(StripeMode.SANDBOX)).thenReturn(new StripeConfig("", ""));

        assertThrows(InternalServerErrorException.class,
                () -> gateway.listProducts(com.stripe.param.ProductListParams.builder().build(), StripeMode.SANDBOX));
    }

    @Test
    void modeConfiguredWithBlankApiKey_throwsInternalServerError() {
        when(configService.getConfig(StripeMode.PRODUCTION)).thenReturn(new StripeConfig(null, "whsec_x"));

        assertThrows(InternalServerErrorException.class,
                () -> gateway.retrievePrice("price_test", StripeMode.PRODUCTION));
    }

}
