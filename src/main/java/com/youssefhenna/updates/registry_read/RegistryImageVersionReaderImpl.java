package com.youssefhenna.updates.registry_read;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.updates.model.DockerConfigJson;
import com.youssefhenna.spec.CommonRegistrySpec;
import com.youssefhenna.updates.model.TagsListResponse;
import com.youssefhenna.updates.model.TokenResponse;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistryImageVersionReaderImpl implements RegistryImageVersionReader {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final Pattern BEARER_REALM = Pattern.compile("realm=\"([^\"]+)\"");
    private static final Pattern BEARER_SERVICE = Pattern.compile("service=\"([^\"]+)\"");

    private final KubernetesClient kubernetesClient;
    private final String namespace;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RegistryImageVersionReaderImpl(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = namespace;
    }

    @Override
    public List<String> getAvailableVersions(CommonRegistrySpec registrySpec, String imageName) throws Exception {
        String rawUrl = registrySpec.getRegistryUrl().replaceAll("/$", "");
        String registryUrl = rawUrl.contains("://") ? rawUrl : "https://" + rawUrl;

        String registryHost = URI.create(registryUrl).getHost();
        String basicCredentials = resolveBasicCredentials(registrySpec.getRegistryCredentials(), registryHost);
        String tagsUrl = registryUrl + "/v2/" + imageName + "/tags/list";

        HttpResponse<String> response = httpClient.send(
            buildRequest(tagsUrl, null),
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 200) {
            return parseTags(response.body());
        }

        // initial 401 expected for authed registry, use header to find and setup proper auth mode
        // https://mirilittleme.medium.com/how-docker-login-works-under-the-hood-42225601843c
        if (response.statusCode() == 401) {
            String wwwAuthenticate = response.headers().firstValue("WWW-Authenticate").orElse("");
            String authorization = resolveAuthorization(wwwAuthenticate, imageName, basicCredentials);
            HttpResponse<String> authedResponse = httpClient.send(
                buildRequest(tagsUrl, authorization),
                HttpResponse.BodyHandlers.ofString()
            );
            if (authedResponse.statusCode() != 200) {
                throw new Exception("Failed to fetch tags for " + imageName + ": HTTP " + authedResponse.statusCode());
            }
            return parseTags(authedResponse.body());
        }

        return List.of();

    }

    private String resolveBasicCredentials(RegistryCredentials credentials, String registryHost) {
        if (credentials == null || credentials.getSecretRef() == null) return null;

        Map<String, String> data = kubernetesClient.secrets()
            .inNamespace(namespace)
            .withName(credentials.getSecretRef().getName())
            .get()
            .getData();

        if (data.containsKey("username") && data.containsKey("password")) {
            String username = decodeBase64SecretValue(data.get("username"));
            String password = decodeBase64SecretValue(data.get("password"));
            if (username == null || password == null) return null;
            return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        }

        String dockerConfigJson = data.get(".dockerconfigjson");
        if (dockerConfigJson != null) {
            return resolveFromDockerConfig(dockerConfigJson, registryHost);
        }

        return null;
    }

    private String decodeBase64SecretValue(String base64Value) {
        if (base64Value == null) return null;
        return new String(Base64.getDecoder().decode(base64Value));
    }

    private String resolveFromDockerConfig(String base64ConfigJson, String registryHost) {
        try {
            String configJson = new String(Base64.getDecoder().decode(base64ConfigJson));
            DockerConfigJson config = jsonMapper.readValue(configJson, DockerConfigJson.class);
            if (config.getAuths() == null) return null;

            DockerConfigJson.DockerRegistryAuth registryAuth = config.getAuths().get(registryHost);
            if (registryAuth == null) return null;

            if (registryAuth.getAuth() != null) return registryAuth.getAuth();

            if (registryAuth.getUsername() != null && registryAuth.getPassword() != null) {
                return Base64.getEncoder().encodeToString((registryAuth.getUsername() + ":" + registryAuth.getPassword()).getBytes());
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }



    private String resolveAuthorization(String wwwAuthenticate, String imageName, String basicCredentials) throws Exception {
        if (!wwwAuthenticate.startsWith("Bearer ")) {
            return basicCredentials != null ? "Basic " + basicCredentials : null;
        }

        Matcher realmMatcher = BEARER_REALM.matcher(wwwAuthenticate);
        if (!realmMatcher.find()) return null;

        String realm = realmMatcher.group(1);
        Matcher serviceMatcher = BEARER_SERVICE.matcher(wwwAuthenticate);
        String service = serviceMatcher.find() ? serviceMatcher.group(1) : "";

        String tokenUrl = realm + "?service=" + service + "&scope=repository:" + imageName + ":pull";
        HttpRequest.Builder tokenRequest = HttpRequest.newBuilder().uri(URI.create(tokenUrl)).GET();
        if (basicCredentials != null) {
            tokenRequest.header("Authorization", "Basic " + basicCredentials);
        }

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest.build(), HttpResponse.BodyHandlers.ofString());
        TokenResponse tokenBody = jsonMapper.readValue(tokenResponse.body(), TokenResponse.class);
        String token = tokenBody.resolveToken();
        return token != null ? "Bearer " + token : null;
    }

    private HttpRequest buildRequest(String url, String authorization) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder.build();
    }

    private List<String> parseTags(String body) throws Exception {
        TagsListResponse response = jsonMapper.readValue(body, TagsListResponse.class);
        return response.getTags() != null ? response.getTags() : List.of();
    }
}
