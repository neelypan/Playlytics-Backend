package com.lyticscorps.playlistpicker;

import java.util.Map;
import java.util.UUID;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PlaylistController {
	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}

	@Value("${SPOTIFY_CLIENT_ID}")
	private String clientId;

	@Value("${SPOTIFY_REDIRECT_URL}")
	private String redirectUrl;

	@Value("${FRONTEND_API_KEY:}")
	private String frontendApiKey;

	@GetMapping("/auth/url")
	public Map<String, String> getAuthUrl(@RequestHeader("X-Frontend-Api-Key") String apiKey) {
		// check if api key was given / is correct
		final String expectedKey = resolveKey(frontendApiKey, "FRONTEND_API_KEY");
		if (!expectedKey.equals(apiKey)) { // if it isn't correct give unauthorized response instead of the url
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
		}

		// so this goes straight from the .env file if clientId isn't set using
		// Springboots thing
		String cid = ((clientId != null && !clientId.isEmpty()) ? clientId : System.getenv("SPOTIFY_CLIENT_ID"));
		String rdu = ((redirectUrl != null && !redirectUrl.isEmpty()) ? redirectUrl
				: System.getenv("SPOTIFY_REDIRECT_URL")); // same thing as above but for the redirect url

		final String state = UUID.randomUUID().toString();
		final String url = "https://accounts.spotify.com/authorize?client_id="
				+ encode(cid)
				+ "&response_type=code&redirect_uri="
				+ encode(rdu)
				+ "&scope=" + encode("playlist-read-private playlist-read-collaborative") +
				"&state=" + encode(state);

		/*
		 * {
		 * "url": "https://accounts.spotify.com/authorize?<params>",
		 * "state": "<state>"
		 * }
		 * ⬆️ is what this endpoint returns ⬇️
		 */

		// @formatter:off | it was being dumb and formatting this weirdly
		return Map.of(
			"url", url,
			"state", state
		);
		// @formatter:on
	}

	/*
	 * TODO: send POST request to https://accounts.spotify.com/api/token and sending
	 * clientid, clientSecret, redirecturi, the code we get from /authorize, and
	 * grant_type=authorization_code to get access and refresh tokens
	 * create a POST endpoint /auth/exchange that recieves a code and then sends the
	 * POST request that is above
	 * 
	 * return:
	 * access token
	 * token type (Bearer)
	 * time until expiration (seconds)
	 * refresh token
	 * Secure the endpoint like how /auth/url is secured
	 */

	private static String encode(String val) {
		return URLEncoder.encode(val, StandardCharsets.UTF_8);
	}

	private static String resolveKey(String configuredValue, String envKey) {
		final String value = (configuredValue != null && !configuredValue.isEmpty())
				? configuredValue
				: System.getenv(envKey);

		if (value == null || value.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Frontend API key not configured");
		}

		return value;
	}
}
