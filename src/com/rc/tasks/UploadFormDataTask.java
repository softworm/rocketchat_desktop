package com.rc.tasks;

import com.rc.utils.HttpUtil;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by song on 15/06/2017.
 */
public class UploadFormDataTask extends HttpTask {

    String mediaType;
    File file;
    String identifier;

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public void execute(String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ret = HttpUtil.uploadFormData(url, headers, requestParams, mediaType, file);
                    System.out.println("[Net Upload response]" + ret);
                    JSONObject retJson = new JSONObject(ret);
                    if (listener != null) {
                        listener.onSuccess(retJson);
                    }
                } catch (IOException e) {
                    //TODO 大文件 消息重复
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailed();
                    }
                }
            }
        }).start();
    }
}
