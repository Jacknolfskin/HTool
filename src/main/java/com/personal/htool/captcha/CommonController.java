package com.personal.htool.captcha;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class CommonController{

        /**
         * 获取验证码（Gif版本）
         *
         * @param response
         */
        public void getGifCode(HttpServletResponse response, HttpServletRequest request) {
            try {
                response.setHeader("Pragma", "No-cache");
                response.setHeader("Cache-Control", "no-cache");
                response.setDateHeader("Expires", 0);
                response.setContentType("image/gif");
                /**
                 * gif格式动画验证码
                 * 宽，高，位数。
                 */
                Captcha captcha = new GifCaptcha(146, 42, 4);
                //输出
                ServletOutputStream out = response.getOutputStream();
                captcha.out(out);
                out.flush();
                //存入Shiro会话session
                System.out.println(captcha.text().toLowerCase());
            } catch (Exception e) {
            }
        }

        /**
         * 跳转到其他网站
         *
         * @param url
         * @return
         */
        @RequestMapping(value = "www/open/goto", method = RequestMethod.GET)
        public ModelAndView _goto(String url) {

            return new ModelAndView("www/go_to", "url", url);
        }
}
