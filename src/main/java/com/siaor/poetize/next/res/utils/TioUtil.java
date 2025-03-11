package com.siaor.poetize.next.res.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.siaor.poetize.next.res.oper.im.TioWebsocketStarter;

public class TioUtil {

    private static TioWebsocketStarter tioWebsocketStarter;

    public static void buildTio() {
        TioWebsocketStarter websocketStarter = null;
        try {
            websocketStarter = SpringUtil.getBean(TioWebsocketStarter.class);
        } catch (Exception e) {
        }
        TioUtil.tioWebsocketStarter = websocketStarter;
    }

    public static TioWebsocketStarter getTio() {
        return TioUtil.tioWebsocketStarter;
    }
}
