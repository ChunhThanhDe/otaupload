package com.app.myapplication.service;

import com.vnptt.ota.service.IResponseQueryNewVersion;

interface IServiceManagement {
    void autoQueryNewVersion(IResponseQueryNewVersion responseCb);
    void userQueryNewVersion(IResponseQueryNewVersion responseCb);
    void configureOtaSetting(int configure);
}
