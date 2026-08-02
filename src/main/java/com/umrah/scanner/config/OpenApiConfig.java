package com.umrah.scanner.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the bearer-token scheme once and applies it globally, so every operation in the Swagger
 * UI can be tried with a real access token instead of only the handful that are annotated.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI umrahScannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Umrah Scanner API")
                        .version("v1")
                        .description("""
                                Customer, company and admin APIs for browsing Umrah programs and running the
                                booking lifecycle.

                                **Lead lifecycle**

                                `INTERESTED` → `PENDING_DEPOSIT_CONFIRMATION` → `DEPOSIT_PAID` →
                                `PENDING_FULL_PAYMENT_CONFIRMATION` → `FULLY_PAID` →
                                `PENDING_COMMISSION_CONFIRMATION` → `COMMISSION_PAID` → `CASHBACK_PAID`

                                Whoever reports a payment first decides the path: a customer report parks the
                                lead in the matching `PENDING_*` state until the other side confirms, while a
                                company (or, for commission, an admin) recording the payment itself goes
                                straight to the settled state.

                                **Money**

                                A lead's commission (`commissionPerTraveler` x billable travelers) and its
                                cashback are calculated once, at creation, and stored on the lead. Changing a
                                company's rate later affects new leads only. Commission is never exposed to
                                customers — they see cashback only.

                                Clients should drive their UI from each lead's `availableActions` rather than
                                inferring which steps are legal from the status.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token issued by /api/v1/auth/google or /api/v1/auth/refresh")));
    }
}
