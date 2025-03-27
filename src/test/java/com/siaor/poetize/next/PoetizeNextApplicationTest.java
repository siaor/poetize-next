package com.siaor.poetize.next;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Random;

class PoetizeNextApplicationTest {

    @Test
    void main() {
        System.out.println(genPayNum());
        System.out.println(genPayNum());
        System.out.println(genPayNum());
        System.out.println(genPayNum());
        System.out.println(genPayNum());
    }

    private String genPayNum(){
        //PN20250326152620704
        //PN20250326152621059
        //PN20250326152621059
        //PN2025 03 26 15 26 21 060
        //PN2025 03 26 15 26 53 622 09159
        //PN20250326152621060
        return "PN" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_MS_FORMAT) + String.format("%05d", new Random().nextInt(10000));
    }
}