package top.rymc.race.phira;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;

@Shiro
@Component
public class RaceBot {

    private static final VerifyClient verifyClient = new VerifyClient("localhost", 18080);

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED, cmd = "hi")
    public void fun2(GroupMessageEvent event, Bot bot) {

        String sendMsg = MsgUtils.builder()
                .at(event.getUserId())
                .text(" ")
                .text("hi, 我是澈宸杯qq机器人\n")
                .text("加入Phira多人游戏服务器 cc-cup.cc:1 获取绑定码\n")
                .text("使用指令 bind <绑定码> 来绑定你的Phira账号\n")
                .build();

        bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
    }

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.BOTH, cmd = "^bind ([A-Za-z0-9]+)$")
    public void fun4(GroupMessageEvent event, Bot bot, Matcher matcher) {
        doVerify(event, bot, matcher);
    }

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.BOTH, cmd = "^/bind ([A-Za-z0-9]+)$")
    public void fun3(GroupMessageEvent event, Bot bot, Matcher matcher) {
        doVerify(event, bot, matcher);
    }

    private void doVerify(GroupMessageEvent event, Bot bot, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        try {
            Optional<VerifyResult> check = verifyClient.check(qq);
            if (check.isPresent()) {
                VerifyResult verifyResult = check.get();
                String sendMsg = MsgUtils.builder()
                        .at(event.getUserId())
                        .text(" ")
                        .text(String.format("你已经绑定过了！你是: [%d] %s", verifyResult.user(), verifyResult.name()))
                        .build();

                bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
                return;
            }

            String code = matcher.group(1);
            if (code == null || !code.matches("\\d{6}")) {
                String sendMsg = MsgUtils.builder()
                        .at(event.getUserId())
                        .text(" ")
                        .text("验证码格式错误，请输入6位数字验证码")
                        .build();

                bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
                return;
            }


            Optional<VerifyResult> verify = verifyClient.verify(qq, code);

            if (verify.isPresent()) {
                VerifyResult verifyResult = verify.get();
                String sendMsg = MsgUtils.builder()
                        .at(event.getUserId())
                        .text(" ")
                        .text(String.format("绑定Phira账号成功！你是: [%d] %s", verifyResult.user(), verifyResult.name()))
                        .build();

                bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
            } else {
                String sendMsg = MsgUtils.builder()
                        .at(event.getUserId())
                        .text(" ")
                        .text("验证码无效或已过期，请重新获取验证码")
                        .build();

                bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
            }
        } catch (Exception e) {
            String sendMsg = MsgUtils.builder()
                    .at(event.getUserId())
                    .text(" ")
                    .text("验证过程中发生错误，请稍后再试")
                    .build();

            bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
            e.printStackTrace();
        }
    }

}
