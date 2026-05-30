package com.saneb.domain.auth.service;

public interface AdminBootstrapService {

    void saveBootstrapAdmin(String loginId, String rawPassword, String name);
}
