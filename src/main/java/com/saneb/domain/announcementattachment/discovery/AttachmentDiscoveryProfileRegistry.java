package com.saneb.domain.announcementattachment.discovery;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class AttachmentDiscoveryProfileRegistry {
    private final List<AttachmentDiscoveryProfile> profiles;
    public AttachmentDiscoveryProfileRegistry(List<AttachmentDiscoveryProfile> profiles) { this.profiles = List.copyOf(profiles); }
    public Optional<AttachmentDiscoveryProfile> selectProfileDetails(String provider, String code, String hash) {
        var matches = profiles.stream().filter(profile -> profile.selectProviderCode().equals(provider)
                && profile.selectProfileCode().equals(code) && profile.selectProfileHash().equals(hash)).toList();
        if (matches.size() > 1) throw new IllegalStateException("동일 첨부 프로필이 중복 등록되었습니다.");
        return matches.stream().findFirst();
    }
    public List<AttachmentDiscoveryProfile> selectProfileList() { return profiles; }
}
