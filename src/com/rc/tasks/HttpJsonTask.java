package com.rc.tasks;

import com.rc.utils.HttpUtil;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by song on 08/06/2017.
 */
public class HttpJsonTask extends HttpTask
{

    String jsonStr;
    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    @Override
    public void execute(String url)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String ret = HttpUtil.postJson(url, headers, jsonStr);
                    System.out.println("[Net POST response]" + ret);
                    JSONObject retJson = new JSONObject(ret);
                    if (listener != null)
                    {
                        listener.onSuccess(retJson);
                    }
                }
                catch (IOException e)
                {
                    if (listener != null)
                    {
                        listener.onFailed();
                    }
                }

            }
        }).start();

    }
}
