package com.lyticscorps.playlistpicker;

import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RefreshTokenService {
    @Value("${SPOTIFY_CLIENT_ID}")
    private String clientId;
    @Value("${SPOTIFY_CLIENT_SECRET}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> refreshTokens(String refreshToken) {
        String creds = clientId + ":" + clientSecret;
        String encodedCreds = Base64.getEncoder().encodeToString(creds.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCreds);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> rq = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "https://accounts.spotify.com/api/token",
                HttpMethod.POST,
                rq,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        Map<String, Object> res_ = res.getBody();
        res_.put("refresh_token", refreshToken);
        return res_;
    }

}