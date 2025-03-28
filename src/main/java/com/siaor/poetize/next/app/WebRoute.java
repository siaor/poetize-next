package com.siaor.poetize.next.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 网页端路由支持
 *
 * @author Siaor
 * @since 2025-03-28 10:15:03
 */
@Controller
public class WebRoute {

    /**
     * 微聊路由
     */
    @RequestMapping({"/im", "/im/"})
    public ModelAndView imRoute() {
        return new ModelAndView("forward:/im/index.html");
    }

}
