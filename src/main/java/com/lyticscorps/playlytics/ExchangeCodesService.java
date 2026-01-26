package com.lyticscorps.playlytics;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

@Service
public class ExchangeCodesService {
    @Value("${SPOTIFY_CLIENT_ID}")
    private String clientId;
    @Value("${SPOTIFY_CLIENT_SECRET}")
    private String clientSecret;
    @Value("${SPOTIFY_REDIRECT_URL}")
    private String redirectUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchanges a Spotify authorization code for access and refresh tokens.
     * Makes a POST request to the Spotify token endpoint with Base64-encoded
     * credentials.
     *
     * @param code the authorization code received from Spotify
     * @return a map containing the token response with keys like "access_token",
     *         "refresh_token", etc.
     */
    @SuppressWarnings("null")
    public Map<String, Object> exchangeCodeForTokens(String code) {
        String creds = clientId + ":" + clientSecret;
        String encodedCreds = Base64.getEncoder().encodeToString(creds.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCreds);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUrl);

        HttpEntity<MultiValueMap<String, String>> rq = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                rq,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        System.out.println(res.getBody());
        return res.getBody();
    }
}
