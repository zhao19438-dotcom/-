package com.cainiao.forum.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 图形验证码工具：纯 JDK AWT 生成，不引入第三方依赖。
 * 验证码文本存放在 Session 中（一次性校验，用后即清）。
 */
public class CaptchaUtil {

    // Session 中保存验证码的 key。
    public static final String SESSION_KEY = "CAPTCHA_CODE";

    // 去掉了 0/O、1/I/L 等易混淆字符，减少用户输错。
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int WIDTH = 100;
    private static final int HEIGHT = 36;
    private static final int LENGTH = 4;

    private static final Random RANDOM = new Random();

    private CaptchaUtil() {
    }

    // 生成 4 位验证码文本。
    public static String generateCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // 根据验证码文本绘制图片（带干扰线，增加机器识别难度）。
    public static BufferedImage generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 白底
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 验证码字符，逐个随机颜色绘制
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(120), RANDOM.nextInt(120), RANDOM.nextInt(120)));
            g.drawString(String.valueOf(code.charAt(i)), 12 + i * 22, 26);
        }

        g.dispose();
        return image;
    }
}
