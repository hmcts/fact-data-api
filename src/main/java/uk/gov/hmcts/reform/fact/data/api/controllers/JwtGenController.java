package uk.gov.hmcts.reform.fact.data.api.controllers;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientAssertionCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.OnBehalfOfCredentialBuilder;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.ManagedIdentityApplication;
import com.microsoft.aad.msal4j.ManagedIdentityId;
import com.microsoft.aad.msal4j.ManagedIdentityParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jwt/")
@RequiredArgsConstructor
@Slf4j
public class JwtGenController {

    public enum Scope {
        USE_SERVER_APP_ID_AS_SCOPE,
        USE_CLIENT_APP_ID_AS_SCOPE
    }

    @Value("#{environment.AZURE_TENANT_ID}")
    String azureTenantId;
    @Value("#{environment.AZURE_CLIENT_ID}")
    String azureClientId;

    @Value("#{environment.POC_CLIENT_APP_ID}")
    String pocClientAppId;
    @Value("#{environment.POC_CLIENT_SECRET}")
    String pocClientSecret;
    @Value("#{environment.POC_SERVICE_APP_ID}")
    String pocServiceAppId;

    @GetMapping("/miv2")
    public ResponseEntity<String> genMIJwtAlt(
        @RequestParam(value = "set_authority", defaultValue = "true") boolean setAuthority) {
        try {
            ManagedIdentityCredential mic = new ManagedIdentityCredentialBuilder().clientId(azureClientId).build();
            var tokenRequestContext = new TokenRequestContext();
            tokenRequestContext.addScopes("api://AzureADTokenExchange");
            var caToken = mic.getTokenSync(tokenRequestContext);

            log.info("ca token: {}", caToken.getToken());

            var builder = ConfidentialClientApplication.builder(
                pocClientAppId,
                ClientCredentialFactory.createFromClientAssertion(caToken.getToken())
            );
            if (setAuthority) {
                builder.authority("https://login.microsoftonline.com/" + azureTenantId);
            }
            var cca = builder.build();

            ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                    Collections.singleton(String.format("api://%s/.default", pocServiceAppId)))
                .build();

            CompletableFuture<IAuthenticationResult> future = cca.acquireToken(parameters);
            IAuthenticationResult result = future.join();
            return ResponseEntity.ok(result.accessToken());
        } catch (MalformedURLException e) {
            log.error("Malformed URL", e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("/mi")
    public ResponseEntity<String> genMIJwt() {

        try {
            var mia = ManagedIdentityApplication.builder(ManagedIdentityId.userAssignedClientId(
                azureClientId)).build();

            var caTokenResult = mia.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(
                "api://AzureADTokenExchange").build());
            var caToken = caTokenResult.get();

            var assertionCredential = new ClientAssertionCredentialBuilder().clientId(pocClientAppId).authorityHost(
                "https://login.microsoftonline.com/" + azureTenantId).clientAssertion(caToken::accessToken).build();

            var tokenRequestContext = new TokenRequestContext();
            tokenRequestContext.addScopes(String.format("api://%s/.default", pocServiceAppId));

            var token = assertionCredential.getTokenSync(tokenRequestContext);
            return ResponseEntity.ok(token.getToken());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("/mi-ca")
    public ResponseEntity<String> genMIClientAssertionJwt() {

        try {
            var mia = ManagedIdentityApplication.builder(ManagedIdentityId.userAssignedClientId(
                azureClientId)).build();

            var caTokenResult = mia.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(
                "api://AzureADTokenExchange").build());
            var caToken = caTokenResult.get();

            return ResponseEntity.ok(caToken.accessToken());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.internalServerError().build();
    }


    @GetMapping("/cs/{scope}")
    public ResponseEntity<String> genCSJwt(@PathVariable Scope scope) {

        var credential = new ClientSecretCredentialBuilder()
            .tenantId(azureTenantId) // the tenant
            .clientId(pocClientAppId) // our client id
            .clientSecret(pocClientSecret) // our client secret
            .build();

        var jwt = requestJwt(scope, credential);

        return ResponseEntity.ok(jwt);
    }


    @GetMapping("/obo")
    public ResponseEntity<String> getOBOJwt(@RequestParam(value = "jwt", required = true) String jwt) {

        // this one is in just to see what it does :)

        var credential = new OnBehalfOfCredentialBuilder()
            .tenantId(azureTenantId)
            .clientId(azureClientId) // the mi?
            .clientAssertion(jwt::toString)
            .build();

        var tokenRequestContext = new TokenRequestContext();
        tokenRequestContext.addScopes(String.format("api://%s/.default", pocServiceAppId));

        var accessToken = credential.getTokenSync(tokenRequestContext);
        var oboJwt = accessToken.getToken();

        return ResponseEntity.ok(oboJwt);
    }

    // sets the scope and requests the jwt
    private String requestJwt(final Scope scope, final TokenCredential credential) {
        var tokenRequestContext = new TokenRequestContext();
        // scope is "api://<some-app-reg-id>/.default" where service id is the
        // id of the service that we are connecting to.
        tokenRequestContext.addScopes(String.format(
            "api://%s/.default", switch (scope) {
                // This is the one that should work
                case USE_SERVER_APP_ID_AS_SCOPE -> pocServiceAppId;
                // this will be the app id of the calling app - almost certainly won't work as aud will be duff
                case USE_CLIENT_APP_ID_AS_SCOPE -> azureClientId;
            }
        ));

        var accessToken = credential.getTokenSync(tokenRequestContext);
        return accessToken.getToken();
    }
}
