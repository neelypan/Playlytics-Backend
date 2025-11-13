package com.lyticscorps.playlistpicker;

import java.util.Map;
import java.util.UUID;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/auth/url")
	public Map<String, String> getAuthUrl() {
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

		return Map.of("url", url,
				"state", state);
		/*
		 * {
		 * "url": "https://accounts.spotify.com/authorize?<params>",
		 * "state": "<state>"
		 * }
		 * ⬆️ is what this endpoint returns
		 */
	}

	private static String encode(String val) {
		return URLEncoder.encode(val, StandardCharsets.UTF_8);
	}
}
