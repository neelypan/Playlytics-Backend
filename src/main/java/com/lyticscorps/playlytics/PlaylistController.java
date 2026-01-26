package com.lyticscorps.playlytics;

import java.util.Map;
import java.util.UUID;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PlaylistController {
	private final ExchangeCodesService spotifyAuthService;
	private final RefreshTokenService refreshTokenService;

	/**
	 * Constructs a PlaylistController with dependency injection.
	 *
	 * @param spotifyAuthService  the service for exchanging authorization codes
	 * @param refreshTokenService the service for refreshing tokens
	 */
	public PlaylistController(ExchangeCodesService spotifyAuthService, RefreshTokenService refreshTokenService) {
		this.spotifyAuthService = spotifyAuthService;
		this.refreshTokenService = refreshTokenService;
	}

	/**
	 * Returns the health status of the application.
	 *
	 * @return a map with status "ok" if the service is running
	 */
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

	private String tempState;

	/**
	 * Generates the Spotify OAuth authorization URL.
	 * Validates the frontend API key before returning the URL.
	 * Stores the state value for later validation in the exchange endpoint.
	 *
	 * @param apiKey the frontend API key from request headers
	 * @return a map containing the authorization URL and state value
	 * @throws ResponseStatusException with 401 status if API key is invalid or
	 *                                 missing
	 * @throws ResponseStatusException with 500 status if required configuration is
	 *                                 missing
	 */
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

		System.out.println("Redirect URI: '" + rdu + "'");

		final String state = UUID.randomUUID().toString();
		this.tempState = state;

		final String url = "https://accounts.spotify.com/authorize"
				+ "?client_id=" + cid
				+ "&response_type=code"
				+ "&redirect_uri=" + rdu // 👈 NO encode here
				+ "&scope=" + encode("playlist-read-private playlist-read-collaborative")
				+ "&state=" + encode(state);

		/*
		 * {
		 * "url": "https://accounts.spotify.com/authorize?<params>",
		 * "state": "<state>"
		 * }
		 * ⬆️ is what this endpoint returns ⬇️
		 */

		return Map.of(
				"url", url,
				"state", state);
	}

	/**
	 * Exchanges the Spotify authorization code for tokens.
	 * Validates the API key, code, state, and verifies state matches the previously
	 * generated value.
	 *
	 * @param apiKey the frontend API key from request headers
	 * @param body   a map containing "code" and "state" from the OAuth callback
	 * @return a map containing access and refresh tokens
	 * @throws ResponseStatusException with 401 status if API key or state is
	 *                                 invalid
	 * @throws ResponseStatusException with 400 status if code or state is missing
	 *                                 or empty
	 */
	@PostMapping("/auth/exchange")
	public Map<String, Object> exchangeCodes(@RequestHeader("X-Frontend-Api-Key") String apiKey,
			@RequestBody Map<String, String> body) {
		checkApiKey(frontendApiKey, apiKey);

		String code = body.get("code");
		String state = body.get("state");

		if (code == null || code.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required");
		}

		if (state == null || state.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State is required");
		}

		if (!state.equals(this.tempState)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid state");
		}

		System.out.println("Recieved code: " + code);
		System.out.println("Recieved State: " + state);

		// update with act values after we get them
		final Map<String, Object> tokens = spotifyAuthService.exchangeCodeForTokens(code);
		this.tempState = null;

		return tokens;
	}

	/**
	 * Refreshes Spotify access tokens using a refresh token.
	 * Validates the API key before processing the request.
	 *
	 * @param apiKey the frontend API key from request headers
	 * @param body   a map containing "refresh_token"
	 * @return a map containing the new access token and other token details
	 * @throws ResponseStatusException with 401 status if API key is invalid
	 * @throws ResponseStatusException with 400 status if refresh token is missing
	 *                                 or empty
	 */
	@PostMapping("/auth/refresh")
	public Map<String, Object> refreshCodes(@RequestHeader("X-Frontend-Api-Key") String apiKey,
			@RequestBody Map<String, String> body) {
		checkApiKey(frontendApiKey, apiKey);

		final String refreshToken = body.get("refresh_token");

		if (refreshToken == null || refreshToken.isEmpty())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh token is required");

		System.out.println("Recieved refresh token: " + refreshToken);

		final Map<String, Object> tokens = refreshTokenService.refreshTokens(refreshToken);

		return tokens;
	}

	/**
	 * Validates that the provided API key matches the expected key.
	 *
	 * @param frontendApiKey the configured frontend API key (may be empty if using
	 *                       environment variable)
	 * @param apiKey         the API key from the request header
	 * @throws ResponseStatusException with 401 status if the API key is invalid or
	 *                                 missing
	 * @throws ResponseStatusException with 500 status if required configuration is
	 *                                 missing
	 */
	private static void checkApiKey(String frontendApiKey, String apiKey) {
		final String expectedKey = resolveKey(frontendApiKey, "FRONTEND_API_KEY");
		if (!expectedKey.equals(apiKey)) { // if it isn't correct give unauthorized response instead of the url
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
		}
	}

	/**
	 * URL-encodes a string using UTF-8 encoding.
	 *
	 * @param val the string to encode
	 * @return the URL-encoded string
	 */
	private static String encode(String val) {
		return URLEncoder.encode(val, StandardCharsets.UTF_8);
	}

	/**
	 * Resolves a configuration key by checking the configured value first,
	 * then falling back to the environment variable if not configured.
	 *
	 * @param configuredValue the value from application configuration (may be null
	 *                        or empty)
	 * @param envKey          the environment variable name to use as fallback
	 * @return the resolved configuration value
	 * @throws ResponseStatusException with 500 status if both configured value and
	 *                                 environment variable are empty or null
	 */
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
