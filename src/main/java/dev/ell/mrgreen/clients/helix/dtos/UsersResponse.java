package dev.ell.mrgreen.clients.helix.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsersResponse(List<User> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            String id,
            String login,
            @JsonProperty("display_name") String displayName,
            String type,
            @JsonProperty("broadcaster_type") String broadcasterType,
            String description,
            @JsonProperty("profile_image_url") String profileImageUrl,
            @JsonProperty("offline_image_url") String offlineImageUrl,
            @JsonProperty("created_at") String createdAt
    ) {}
}
