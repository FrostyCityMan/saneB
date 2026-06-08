package com.saneb.domain.applog.service;

import com.saneb.domain.applog.dto.AppLogResponse;

public interface AppLogService {

    AppLogResponse selectAppLog(String levelCode, String keyword, int lines);
}
