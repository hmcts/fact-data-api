package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.identity.OnBehalfOfCredentialBuilder;
import lombok.RequiredArgsConstructor;
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
public class JwtGenController {

    public enum Scope {
        USER_SERVER_APP_ID_AS_SCOPE,
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

    @GetMapping("/mi/{scope}")
    public ResponseEntity<String> genMIJwt(@PathVariable Scope scope) {

        var credential = new ManagedIdentityCredentialBuilder()
            .clientId(azureClientId) // our client id is assumed to be the MI in this case
            .build();

        var jwt = requestJwt(scope, credential);

        return ResponseEntity.ok(jwt);
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
        // scope is "api://<serviceId>/.default" where service id is the id of
        // the service that we are connecting to. This is the bit I don't like
        // we shouldn't need this bit
        tokenRequestContext.addScopes(
            "api://AzureADTokenExchange");

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
                case USER_SERVER_APP_ID_AS_SCOPE -> pocServiceAppId;
                // this will be the app id of the calling app - almost certainly won't work as aud will be duff
                case USE_CLIENT_APP_ID_AS_SCOPE -> azureClientId;
            }
        ));

        var accessToken = credential.getTokenSync(tokenRequestContext);
        var jwt = accessToken.getToken();
        return jwt;
    }
}
